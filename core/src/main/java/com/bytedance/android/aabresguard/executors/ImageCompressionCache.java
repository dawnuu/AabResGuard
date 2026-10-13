package com.bytedance.android.aabresguard.executors;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/** Content-addressed cache; each entry is published atomically and checksum verified. */
final class ImageCompressionCache {
    private final Path directory;
    private final String tools;
    private String fingerprint;

    ImageCompressionCache(Path directory, String tools) {
        this.directory = directory;
        this.tools = tools;
    }

    String key(byte[] original, List<String> command) throws IOException {
        if (fingerprint == null) fingerprint = fingerprint();
        MessageDigest digest = digest();
        // Bump this schema if optimization behavior changes outside the command flags.
        digest.update(("image-cache-v1\n" + fingerprint + "\n" + String.join("\n", command))
                .getBytes(StandardCharsets.UTF_8));
        digest.update(original);
        return hex(digest.digest());
    }

    byte[] read(String key, int originalSize) throws IOException {
        Path entry = directory.resolve(key + ".bin");
        if (!Files.isRegularFile(entry)) return null;
        long size = Files.size(entry);
        if (size <= 32 || size > (long) originalSize + 32) return null;
        byte[] bytes = Files.readAllBytes(entry);
        byte[] result = Arrays.copyOfRange(bytes, 32, bytes.length);
        return MessageDigest.isEqual(Arrays.copyOf(bytes, 32), digest().digest(result)) ? result : null;
    }

    void write(String key, byte[] result) throws IOException {
        Files.createDirectories(directory);
        Path temporary = Files.createTempFile(directory, key, ".tmp");
        try {
            try (OutputStream output = Files.newOutputStream(temporary)) {
                output.write(digest().digest(result));
                output.write(result);
            }
            try {
                Files.move(temporary, directory.resolve(key + ".bin"),
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                // Do not publish a partially written entry on unsupported file systems.
                throw new IOException("Image cache requires atomic file moves", e);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private String fingerprint() throws IOException {
        MessageDigest digest = digest();
        digest.update((System.getProperty("os.name") + "/" + System.getProperty("os.arch"))
                .getBytes(StandardCharsets.UTF_8));
        if (tools.isEmpty()) {
            String platform = BundledImageTools.platform(System.getProperty("os.name"), System.getProperty("os.arch"));
            try (InputStream input = BundledImageTools.class.getResourceAsStream("/image-tools/" + platform + "/files.properties")) {
                if (input == null) throw new IOException("Missing bundled tool manifest");
                update(digest, input);
            }
        } else {
            // Include sibling libraries as well as executables; versions may change in place.
            try (java.util.stream.Stream<Path> files = Files.list(Paths.get(tools))) {
                for (Path file : (Iterable<Path>) files.filter(Files::isRegularFile)
                        .filter(ImageCompressionCache::isToolFile).sorted()::iterator) {
                    digest.update(file.getFileName().toString().getBytes(StandardCharsets.UTF_8));
                    try (InputStream input = Files.newInputStream(file)) { update(digest, input); }
                }
            }
        }
        return hex(digest.digest());
    }

    private static boolean isToolFile(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        return Arrays.asList("optipng", "jpegtran", "cwebp", "gifsicle",
                "optipng.exe", "jpegtran.exe", "cwebp.exe", "gifsicle.exe").contains(name)
                || name.endsWith(".dll") || name.endsWith(".dylib") || name.contains(".so");
    }

    private static void update(MessageDigest digest, InputStream input) throws IOException {
        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
    }

    private static MessageDigest digest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    private static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte value : bytes) result.append(String.format(Locale.ROOT, "%02x", value & 255));
        return result.toString();
    }
}
