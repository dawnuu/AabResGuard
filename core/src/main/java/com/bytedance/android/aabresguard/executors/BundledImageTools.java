package com.bytedance.android.aabresguard.executors;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Properties;

/** Extracts the tools packaged in the JAR, without using PATH or network access. */
final class BundledImageTools {
    private BundledImageTools() {}

    static String platform(String osName, String archName) throws IOException {
        String os = osName.toLowerCase(Locale.ROOT);
        String arch = archName.toLowerCase(Locale.ROOT);
        String family;
        if (os.startsWith("windows")) family = "windows";
        else if (os.startsWith("mac") || os.equals("darwin")) family = "macos";
        else if (os.startsWith("linux")) family = "linux";
        else throw unsupported(osName, archName);
        if (arch.equals("amd64") || arch.equals("x86_64") || arch.equals("x64")) arch = "x86_64";
        else if (arch.equals("aarch64") || arch.equals("arm64")) arch = "arm64";
        else throw unsupported(osName, archName);
        if (!arch.equals("x86_64") && !family.equals("macos")) throw unsupported(osName, archName);
        return family + "-" + arch;
    }

    private static IOException unsupported(String os, String arch) {
        return new IOException("No bundled image tools for " + os + "/" + arch
                + ". Supported: macOS ARM64/x86_64, Windows x86_64, Linux x86_64 (glibc)."
                + " Set imageCompressionToolsDir to use tools for another platform.");
    }

    static Path extract(Path work) throws IOException {
        String platform = platform(System.getProperty("os.name"), System.getProperty("os.arch"));
        Path directory = work.resolve("native").resolve(platform);
        Path ready = directory.resolve(".complete");
        if (Files.isRegularFile(ready)) return directory;
        Files.createDirectories(directory);
        String base = "/image-tools/" + platform + "/";
        Properties files = new Properties();
        try (InputStream input = resource(base + "files.properties")) {
            files.load(input);
        }
        if (files.isEmpty()) throw new IOException("Empty bundled image tools manifest: " + platform);
        for (String name : files.stringPropertyNames()) {
            if (!name.matches("[A-Za-z0-9_.-]+") || name.equals(".") || name.equals("..")) {
                throw new IOException("Invalid bundled image tool name: " + name);
            }
            Path output = directory.resolve(name);
            MessageDigest digest;
            try {
                digest = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e) {
                throw new IOException("SHA-256 unavailable", e);
            }
            try (InputStream input = new DigestInputStream(resource(base + name), digest)) {
                Files.copy(input, output, StandardCopyOption.REPLACE_EXISTING);
            }
            verifyChecksum(name, files.getProperty(name), digest.digest());
            if (!name.endsWith(".dll") && !output.toFile().setExecutable(true, true)) {
                throw new IOException("Cannot make bundled image tool executable: " + output);
            }
        }
        Files.createFile(ready);
        return directory;
    }

    static void verifyChecksum(String name, String expected, byte[] actual) throws IOException {
        StringBuilder hex = new StringBuilder();
        for (byte value : actual) hex.append(String.format(Locale.ROOT, "%02x", value & 255));
        if (!hex.toString().equals(expected)) throw new IOException("Bundled image tool checksum mismatch: " + name);
    }

    private static InputStream resource(String path) throws IOException {
        InputStream input = BundledImageTools.class.getResourceAsStream(path);
        if (input == null) throw new IOException("Missing bundled image tool resource: " + path
                + ". Rebuild the complete JAR or set imageCompressionToolsDir.");
        return input;
    }
}
