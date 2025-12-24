package com.bytedance.android.aabresguard.utils;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;

/**
 * Utility to upload files to VirusTotal using API v3 and return permanent report URL.
 */
public class VirusTotalUploader {
    private static final String API_URL = "https://www.virustotal.com/api/v3/files";

    public static void upload(String apiKey, File file) {
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("[VirusTotal] API Key is missing. Skipping upload.");
            return;
        }

        System.out.println("[VirusTotal] Calculating file hash...");
        String sha256 = "";
        try (FileInputStream fis = new FileInputStream(file)) {
            sha256 = DigestUtils.sha256Hex(fis);
        } catch (IOException e) {
            System.err.println("[VirusTotal] Hash calculation failed: " + e.getMessage());
            return;
        }

        System.out.println("[VirusTotal] Uploading " + file.getName() + " to VirusTotal...");

        try {
            String boundary = "---" + System.currentTimeMillis();
            HttpURLConnection connection = (HttpURLConnection) new URL(API_URL).openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("x-apikey", apiKey);
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            try (OutputStream output = connection.getOutputStream();
                 PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, "UTF-8"), true)) {
                
                writer.append("--" + boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"").append("\r\n");
                writer.append("Content-Type: application/octet-stream").append("\r\n\r\n");
                writer.flush();

                Files.copy(file.toPath(), output);
                output.flush();
                
                writer.append("\r\n");
                writer.append("--" + boundary + "--").append("\r\n");
                writer.flush();
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                System.out.println("[VirusTotal] Upload successful!");
                System.out.println("[VirusTotal] Permanent Report URL: https://www.virustotal.com/gui/file/" + sha256);
            } else {
                System.err.println("[VirusTotal] Upload failed with response code: " + responseCode);
            }
        } catch (IOException e) {
            System.err.println("[VirusTotal] Error during upload: " + e.getMessage());
        }
    }
}
