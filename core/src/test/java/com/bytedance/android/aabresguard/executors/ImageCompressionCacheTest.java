package com.bytedance.android.aabresguard.executors;

import org.junit.Assume;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class ImageCompressionCacheTest {
    @Test
    public void reusesResultsAcrossRunsAndCleanAndInvalidatesChangedInputs() throws Exception {
        Assume.assumeFalse(System.getProperty("os.name").startsWith("Windows"));
        Path root = Files.createTempDirectory("image-cache-test-");
        try {
            Path tools = Files.createDirectories(root.resolve("tools"));
            Path cache = root.resolve(".gradle/aabresguard/image-cache");
            Path counter = root.resolve("calls");
            Path build = Files.createDirectories(root.resolve("build"));
            Path tool = tools.resolve("optipng");
            String script = "#!/bin/sh\nprintf x >> '" + counter + "'\nprintf compressed > \"$6\"\n";
            Files.write(tool, script.getBytes(StandardCharsets.UTF_8));
            assertTrue(tool.toFile().setExecutable(true));
            byte[] original = new byte[100];
            byte[] expected = "compressed".getBytes(StandardCharsets.UTF_8);
            assertArrayEquals(expected, compressor(tools, cache).optimize(original, "png", build));
            assertEquals(1, Files.size(counter));
            // Simulate clean deleting build outputs, then use a fresh compressor instance.
            delete(build);
            Files.createDirectories(build);
            assertArrayEquals(expected, compressor(tools, cache).optimize(original, "png", build));
            assertEquals(1, Files.size(counter));
            byte[] changed = original.clone();
            changed[0] = 1;
            compressor(tools, cache).optimize(changed, "png", build);
            assertEquals(2, Files.size(counter));
            Files.write(tool, (script + "# new tool version\n").getBytes(StandardCharsets.UTF_8));
            compressor(tools, cache).optimize(original, "png", build);
            assertEquals(3, Files.size(counter));
            // All cached files are invalid, so the next run must repair its entry.
            try (java.util.stream.Stream<Path> files = Files.list(cache)) {
                for (Path file : (Iterable<Path>) files::iterator) Files.write(file, new byte[40]);
            }
            assertArrayEquals(expected, compressor(tools, cache).optimize(original, "png", build));
            assertEquals(4, Files.size(counter));
            assertArrayEquals(expected, compressor(tools, cache).optimize(original, "png", build));
            assertEquals(4, Files.size(counter));
        } finally { delete(root); }
    }

    @Test
    public void cachesUnchangedResultsButRetriesFailures() throws Exception {
        Assume.assumeFalse(System.getProperty("os.name").startsWith("Windows"));
        Path root = Files.createTempDirectory("image-cache-unchanged-");
        try {
            Path tools = Files.createDirectories(root.resolve("tools"));
            Path cache = root.resolve("cache");
            Path counter = root.resolve("calls");
            Path tool = tools.resolve("optipng");
            String script = "#!/bin/sh\nprintf x >> '" + counter + "'\ncp \"$7\" \"$6\"\n";
            Files.write(tool, script.getBytes(StandardCharsets.UTF_8));
            assertTrue(tool.toFile().setExecutable(true));
            byte[] original = new byte[100];
            assertArrayEquals(original, compressor(tools, cache).optimize(original, "png", root));
            assertArrayEquals(original, compressor(tools, cache).optimize(original, "png", root));
            assertEquals(1, Files.size(counter));
            Files.write(tool, ("#!/bin/sh\nprintf x >> '" + counter + "'\nexit 1\n").getBytes(StandardCharsets.UTF_8));
            for (int i = 0; i < 2; i++) {
                try {
                    compressor(tools, cache).optimize(original, "png", root);
                    fail("Failed tools must not reuse earlier results");
                } catch (IOException expected) { assertTrue(expected.getMessage().contains("exit 1")); }
            }
            assertEquals(3, Files.size(counter));
        } finally { delete(root); }
    }

    @Test
    public void cacheIoFailuresStillReturnCompressedImages() throws Exception {
        Path root = Files.createTempDirectory("image-cache-blocked-");
        try {
            Path blocked = root.resolve("blocked");
            Files.write(blocked, new byte[1]);
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(64, 64,
                    java.awt.image.BufferedImage.TYPE_INT_ARGB), "png", bytes);
            byte[] original = bytes.toByteArray();
            byte[] expected = new BundleImageCompressor("").optimize(original, "png", root);
            assertArrayEquals(expected, new BundleImageCompressor("", blocked.resolve("cache"))
                    .optimize(original, "png", root));
        } finally { delete(root); }
    }

    @Test
    public void keysIncludeParametersAndFormat() throws Exception {
        Path root = Files.createTempDirectory("image-cache-key-");
        try {
            ImageCompressionCache cache = new ImageCompressionCache(root, "");
            byte[] original = new byte[100];
            List<String> flags = Arrays.asList("optipng", "-o2");
            String key = cache.key(original, flags);
            assertNotEquals(key, cache.key(original, Arrays.asList("optipng", "-o3")));
            assertNotEquals(key, cache.key(original, Arrays.asList("jpegtran", "-optimize")));
        } finally { delete(root); }
    }

    private static BundleImageCompressor compressor(Path tools, Path cache) {
        return new BundleImageCompressor(tools.toString(), cache);
    }

    private static void delete(Path root) throws IOException {
        try (java.util.stream.Stream<Path> files = Files.walk(root)) {
            for (Path file : (Iterable<Path>) files.sorted(java.util.Comparator.reverseOrder())::iterator) Files.deleteIfExists(file);
        }
    }
}
