package com.bytedance.android.aabresguard.executors;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Properties;

import static org.junit.Assert.*;

public class BundledImageToolsTest {
    @Test
    public void selectsMatchingOperatingSystemAndArchitecture() throws Exception {
        assertEquals("macos-arm64", BundledImageTools.platform("Mac OS X", "aarch64"));
        assertEquals("macos-x86_64", BundledImageTools.platform("Darwin", "x86_64"));
        assertEquals("windows-x86_64", BundledImageTools.platform("Windows 11", "amd64"));
        assertEquals("linux-x86_64", BundledImageTools.platform("Linux", "amd64"));
        try {
            BundledImageTools.platform("Linux", "aarch64");
            fail("Must not run binaries for a different architecture");
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("imageCompressionToolsDir"));
        }
    }

    @Test
    public void allPlatformBinariesArePackagedWithValidChecksums() throws Exception {
        for (String platform : Arrays.asList("macos-arm64", "macos-x86_64", "windows-x86_64", "linux-x86_64")) {
            String base = "/image-tools/" + platform + "/";
            Properties files = new Properties();
            try (InputStream input = getClass().getResourceAsStream(base + "files.properties")) {
                assertNotNull(platform, input);
                files.load(input);
            }
            for (String tool : Arrays.asList("optipng", "jpegtran", "gifsicle", "cwebp", "dwebp")) {
                assertTrue(platform + "/" + tool, files.containsKey(tool + (platform.startsWith("windows") ? ".exe" : "")));
            }
            if (platform.startsWith("windows")) assertTrue(files.containsKey("libjpeg-62.dll"));
            if (platform.startsWith("linux")) assertTrue(files.containsKey("libz.so.1"));
            for (String file : files.stringPropertyNames()) {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                try (InputStream input = new DigestInputStream(getClass().getResourceAsStream(base + file), digest)) {
                    byte[] buffer = new byte[8192];
                    while (input.read(buffer) != -1) { /* compute checksum */ }
                }
                BundledImageTools.verifyChecksum(file, files.getProperty(file), digest.digest());
            }
        }
    }

    @Test
    public void rejectsCorruptedTools() throws Exception {
        try {
            BundledImageTools.verifyChecksum("optipng", "corrupted", new byte[32]);
            fail("Checksum mismatch must prevent execution");
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("checksum mismatch"));
        }
    }
}
