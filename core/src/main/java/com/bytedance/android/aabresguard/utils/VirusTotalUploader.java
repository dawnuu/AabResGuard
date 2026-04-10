package com.bytedance.android.aabresguard.utils;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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
    private static final long MAX_DIRECT_UPLOAD_SIZE = 32 * 1024 * 1024; // 32MB

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
            String uploadUrl = API_URL;
            if (file.length() > MAX_DIRECT_UPLOAD_SIZE) {
                System.out.println("[VirusTotal] File is larger than 32MB, requesting special upload URL...");
                uploadUrl = getUploadUrl(apiKey);
            }

            String boundary = "---" + System.currentTimeMillis();
            HttpURLConnection connection = (HttpURLConnection) new URL(uploadUrl).openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("x-apikey", apiKey);
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            try (OutputStream output = connection.getOutputStream();
                 PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, "UTF-8"), true)) {
                
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(file.getName()).append("\"").append("\r\n");
                writer.append("Content-Type: application/octet-stream").append("\r\n\r\n");
                writer.flush();

                Files.copy(file.toPath(), output);
                output.flush();
                
                writer.append("\r\n");
                writer.append("--").append(boundary).append("--").append("\r\n");
                writer.flush();
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                System.out.println("[VirusTotal] Upload successful!");
                System.out.println("[VirusTotal] Permanent Report URL: https://www.virustotal.com/gui/file/" + sha256);
            } else {
                System.err.println("[VirusTotal] Upload failed with response code: " + responseCode);
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        System.err.println("[VirusTotal] Error details: " + line);
                    }
                } catch (Exception ignore) {}
            }
        } catch (IOException e) {
            System.err.println("[VirusTotal] Error during upload: " + e.getMessage());
        }
    }

    private static String getUploadUrl(String apiKey) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(API_URL + "/upload_url").openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("x-apikey", apiKey);

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                String json = response.toString();
                // Extract URL from JSON response
                int start = json.indexOf("\"data\"");
                if (start != -1) {
                    int urlStart = json.indexOf("http", start);
                    if (urlStart != -1) {
                        int urlEnd = json.indexOf("\"", urlStart);
                        if (urlEnd != -1) {
                            return json.substring(urlStart, urlEnd);
                        }
                    }
                }
            }
        } else {
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
                StringBuilder errorResponse = new StringBuilder();
                String inputLine;
                while ((inputLine = errorReader.readLine()) != null) {
                    errorResponse.append(inputLine);
                }
                System.err.println("[VirusTotal] Error getting upload URL: " + errorResponse);
            } catch (Exception ignore) {}
        }
        throw new IOException("Failed to get upload URL: " + responseCode);
    }
}