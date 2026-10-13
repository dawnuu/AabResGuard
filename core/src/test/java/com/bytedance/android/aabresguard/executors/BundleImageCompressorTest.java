package com.bytedance.android.aabresguard.executors;

import com.android.aapt.Resources;
import com.android.bundle.Config;
import com.android.tools.build.bundletool.model.AppBundle;
import com.android.tools.build.bundletool.model.ModuleEntry;
import com.bytedance.android.aabresguard.commands.ObfuscateBundleCommand;
import org.junit.Test;
import org.junit.Assume;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class BundleImageCompressorTest {
    @Test
    public void compressedBytesSurviveObfuscationMutationAndPackaging() throws Exception {
        verifyBundlePipeline("", true);
    }

    @Test
    public void missingToolsWarnOnceAndStillProduceBundleWithOriginalImages() throws Exception {
        Path tools = Files.createTempDirectory("image-tools-empty-");
        try {
            verifyFailureWarning(tools, "Cannot run image compression tool");
        } finally {
            delete(tools);
        }
    }

    @Test
    public void killedToolWarnsOnceAndStillProducesBundleWithOriginalImages() throws Exception {
        Assume.assumeFalse(System.getProperty("os.name").startsWith("Windows"));
        Path tools = Files.createTempDirectory("image-tools-killed-");
        try {
            Path executable = tools.resolve("optipng");
            Files.write(executable, "#!/bin/sh\nexit 137\n".getBytes(StandardCharsets.UTF_8));
            assertTrue(executable.toFile().setExecutable(true));
            verifyFailureWarning(tools, "exit 137");
        } finally {
            delete(tools);
        }
    }

    private void verifyFailureWarning(Path tools, String reason) throws Exception {
        ByteArrayOutputStream warnings = new ByteArrayOutputStream();
        java.io.PrintStream previous = System.err;
        try (java.io.PrintStream capture = new java.io.PrintStream(warnings, true, "UTF-8")) {
            System.setErr(capture);
            verifyBundlePipeline(tools.toString(), false);
        } finally {
            System.setErr(previous);
        }
        String message = warnings.toString("UTF-8");
        assertTrue(message.contains("[AabResGuard WARNING]"));
        assertTrue(message.contains("base/res/drawable/hero.png") || message.contains("base/assets/hero.png"));
        assertTrue(message.contains("continuing packaging"));
        assertTrue(message.contains(reason));
        assertEquals("Repeated images must not retry the failed tool", 1,
                message.split("\\[AabResGuard WARNING\\]", -1).length - 1);
    }

    private void verifyBundlePipeline(String tools, boolean expectCompression) throws Exception {
        Path work = Files.createTempDirectory("image-bundle-test-");
        try {
            BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            javax.imageio.ImageWriter writer = ImageIO.getImageWritersByFormatName("png").next();
            try (javax.imageio.stream.ImageOutputStream stream = ImageIO.createImageOutputStream(bytes)) {
                writer.setOutput(stream);
                javax.imageio.ImageWriteParam param = writer.getDefaultWriteParam();
                param.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(1.0f);
                writer.write(null, new javax.imageio.IIOImage(image, null, null), param);
            } finally {
                writer.dispose();
            }
            byte[] original = bytes.toByteArray();
            Resources.ResourceTable table = Resources.ResourceTable.newBuilder()
                    .addPackage(Resources.Package.newBuilder().setPackageName("com.example.images")
                            .setPackageId(Resources.PackageId.newBuilder().setId(127))
                            .addType(Resources.Type.newBuilder().setName("drawable")
                                    .setTypeId(Resources.TypeId.newBuilder().setId(1))
                                    .addEntry(Resources.Entry.newBuilder().setName("hero")
                                            .setEntryId(Resources.EntryId.newBuilder().setId(0))
                                            .addConfigValue(Resources.ConfigValue.newBuilder()
                                                    .setValue(Resources.Value.newBuilder().setItem(Resources.Item.newBuilder()
                                                            .setFile(Resources.FileReference.newBuilder()
                                                                    .setPath("res/drawable/hero.png")
                                                                    .setType(Resources.FileReference.Type.PNG))))))))
                    .build();
            Resources.XmlNode manifest = Resources.XmlNode.newBuilder().setElement(Resources.XmlElement.newBuilder()
                    .setName("manifest").addAttribute(Resources.XmlAttribute.newBuilder()
                            .setName("package").setValue("com.example.images"))
                    .addChild(Resources.XmlNode.newBuilder().setElement(Resources.XmlElement.newBuilder().setName("application"))))
                    .build();
            Path input = work.resolve("input.aab");
            try (java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(Files.newOutputStream(input))) {
                zipEntry(zip, "BundleConfig.pb", Config.BundleConfig.newBuilder()
                        .setBundletool(Config.Bundletool.newBuilder().setVersion("1.18.3")).build().toByteArray());
                zipEntry(zip, "base/manifest/AndroidManifest.xml", manifest.toByteArray());
                zipEntry(zip, "base/resources.pb", table.toByteArray());
                zipEntry(zip, "base/res/drawable/hero.png", original);
                zipEntry(zip, "base/assets/hero.png", original);
            }
            Path output = work.resolve("output.aab");
            ObfuscateBundleCommand command = ObfuscateBundleCommand.builder().setEnableObfuscate(true).setBundlePath(input).setOutputPath(output)
                    .setWhiteList(java.util.Collections.emptySet()).setBundleMetaDataWhiteList(java.util.Collections.emptySet())
                    .setEnableCompressImages(true).setImageCompressionToolsDir(tools)
                    .setImageCompressionCacheDir(work.resolve("cache"))
                    .setEnableMutateMd5(expectCompression).setMutateMd5Seed("test").setDisableSign(true).build();
            command.execute();
            if (expectCompression) {
                ByteArrayOutputStream logs = new ByteArrayOutputStream();
                java.io.PrintStream previous = System.out;
                try (java.io.PrintStream capture = new java.io.PrintStream(logs, true, "UTF-8")) {
                    System.setOut(capture);
                    command.execute();
                } finally { System.setOut(previous); }
                assertTrue("The second bundle must reuse both image results", logs.toString("UTF-8").contains("cached=2"));
            }
            try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(output.toFile())) {
                AppBundle bundle = AppBundle.buildFromZip(zip);
                int images = 0;
                for (ModuleEntry entry : bundle.getBaseModule().getEntries()) {
                    if (!entry.getPath().toString().endsWith(".png")) continue;
                    byte[] optimized = entry.getContent().read();
                    if (expectCompression) {
                        assertTrue("Compression must survive obfuscation", optimized.length < original.length);
                    } else {
                        assertArrayEquals("Failed compression must retain the original image bytes", original, optimized);
                    }
                    assertPixelsEqual(image, ImageIO.read(new ByteArrayInputStream(optimized)));
                    images++;
                }
                assertEquals(2, images);
            }
        } finally {
            delete(work);
        }
    }

    @Test
    public void recognizesFormatsAndSpecialImages() {
        assertEquals("jpg", BundleImageCompressor.format("assets/PHOTO.JPG"));
        assertNull(BundleImageCompressor.format("assets/data.bin"));
        assertTrue(BundleImageCompressor.shouldSkip("res/drawable/button.9.png", new byte[0]));
        assertTrue(BundleImageCompressor.shouldSkip("res/drawable/a.png", chunk("npTc", false)));
        assertTrue(BundleImageCompressor.shouldSkip("assets/a.png", chunk("acTL", false)));
        assertTrue(BundleImageCompressor.shouldSkip("assets/a.webp", chunk("ANIM", true)));
        assertFalse(BundleImageCompressor.shouldSkip("assets/a.webp", chunk("VP8L", true)));
    }

    @Test
    public void usesLosslessFlagsAndPreservesMetadata() {
        BundleImageCompressor compressor = new BundleImageCompressor("/tools with spaces");
        Path input = Paths.get("input with spaces.webp");
        Path output = Paths.get("output.webp");
        List<String> webp = compressor.command("webp", input, output);
        assertTrue(webp.containsAll(Arrays.asList("-lossless", "-exact", "-metadata", "all")));
        assertTrue(webp.contains(input.toString()));
        assertTrue(compressor.command("jpg", input, output).containsAll(Arrays.asList("-copy", "all", "-optimize")));
        assertTrue(compressor.command("png", input, output).containsAll(Arrays.asList("-nb", "-nc", "-np")));
        assertFalse(compressor.command("gif", input, output).contains("--lossy"));
    }

    @Test
    public void missingToolFailsClearly() throws Exception {
        Path work = Files.createTempDirectory("image-tool-missing-");
        try {
            try {
                new BundleImageCompressor(work.resolve("missing").toString()).optimize(new byte[20], "png", work);
                fail("Missing compressor must fail rather than silently skip");
            } catch (IOException e) {
                assertTrue(e.getMessage().contains("imageCompressionToolsDir"));
            }
        } finally {
            delete(work);
        }
    }

    @Test
    public void retainsOriginalWhenToolMakesFileLarger() throws Exception {
        Assume.assumeFalse(System.getProperty("os.name").startsWith("Windows"));
        Path work = Files.createTempDirectory("image-tool-larger-");
        try {
            Path tool = work.resolve("optipng");
            // optipng arguments: -o2 -nb -nc -np -out OUTPUT INPUT
            Files.write(tool, "#!/bin/sh\ncp \"$7\" \"$6\"\nprintf extra >> \"$6\"\n".getBytes(StandardCharsets.UTF_8));
            assertTrue(tool.toFile().setExecutable(true));
            byte[] original = new byte[40];
            assertArrayEquals(original, new BundleImageCompressor(work.toString()).optimize(original, "png", work));
        } finally {
            delete(work);
        }
    }

    @Test
    public void realToolsPreservePixelsForPngJpegGifAndWebp() throws Exception {
        Path work = Files.createTempDirectory("image-native-test-");
        try {
            String tools = BundledImageTools.extract(work).toString();
            BundleImageCompressor compressor = new BundleImageCompressor("");
            BufferedImage image = new BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) image.setRGB(x, y, x < 64 ? 0xff123456 : 0xffabcdef);
            }
            for (String format : Arrays.asList("png", "jpg", "gif")) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                assertTrue(ImageIO.write(image, format, bytes));
                byte[] original = bytes.toByteArray();
                byte[] compressed = compressor.optimize(original, format, work);
                assertTrue(compressed.length <= original.length);
                assertPixelsEqual(ImageIO.read(new ByteArrayInputStream(original)), ImageIO.read(new ByteArrayInputStream(compressed)));
            }
            // Exercise transparent pixels too, including their invisible RGB values.
            BufferedImage rgba = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < 128; y++) {
                for (int x = 0; x < 128; x++) rgba.setRGB(x, y, x < 64 ? 0x00123456 : 0x80abcdef);
            }
            Path png = work.resolve("rgba.png");
            ImageIO.write(rgba, "png", png.toFile());
            byte[] optimizedPng = compressor.optimize(Files.readAllBytes(png), "png", work);
            assertPixelsEqual(rgba, ImageIO.read(new ByteArrayInputStream(optimizedPng)));
            Path sourceWebp = work.resolve("source.webp");
            run(tools, "cwebp", "-lossless", "-exact", "-z", "0", png.toString(), "-o", sourceWebp.toString());
            byte[] originalWebp = Files.readAllBytes(sourceWebp);
            byte[] compressedWebp = compressor.optimize(originalWebp, "webp", work);
            assertTrue(compressedWebp.length <= originalWebp.length);
            Files.write(sourceWebp, compressedWebp);
            Path decoded = work.resolve("decoded.png");
            run(tools, "dwebp", sourceWebp.toString(), "-o", decoded.toString());
            assertPixelsEqual(rgba, ImageIO.read(decoded.toFile()));
        } finally {
            delete(work);
        }
    }

    private static byte[] chunk(String type, boolean webp) {
        byte[] data = new byte[20];
        byte[] name = type.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(name, 0, data, 12, 4);
        return data;
    }

    private static void run(String tools, String executable, String... args) throws Exception {
        java.util.ArrayList<String> command = new java.util.ArrayList<>();
        command.add(Paths.get(tools, executable + (System.getProperty("os.name").startsWith("Windows") ? ".exe" : "")).toString());
        command.addAll(Arrays.asList(args));
        Process process = new ProcessBuilder(command).redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD).start();
        try {
            assertTrue(process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS));
            assertEquals(0, process.exitValue());
        } finally {
            process.destroyForcibly();
        }
    }

    private static void zipEntry(java.util.zip.ZipOutputStream zip, String name, byte[] data) throws IOException {
        zip.putNextEntry(new java.util.zip.ZipEntry(name));
        zip.write(data);
        zip.closeEntry();
    }

    private static void assertPixelsEqual(BufferedImage expected, BufferedImage actual) {
        assertNotNull(actual);
        assertEquals(expected.getWidth(), actual.getWidth());
        assertEquals(expected.getHeight(), actual.getHeight());
        assertArrayEquals(expected.getRGB(0, 0, expected.getWidth(), expected.getHeight(), null, 0, expected.getWidth()),
                actual.getRGB(0, 0, actual.getWidth(), actual.getHeight(), null, 0, actual.getWidth()));
    }

    private static void delete(Path work) throws IOException {
        try (java.util.stream.Stream<Path> paths = Files.walk(work)) {
            for (Path path : (Iterable<Path>) paths.sorted(java.util.Comparator.reverseOrder())::iterator) Files.deleteIfExists(path);
        }
    }
}
