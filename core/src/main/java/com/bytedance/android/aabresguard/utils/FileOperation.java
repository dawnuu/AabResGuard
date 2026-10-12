package com.bytedance.android.aabresguard.utils;

import com.android.tools.build.bundletool.model.ZipPath;
import com.android.tools.build.bundletool.model.utils.files.BufferedIo;
import com.android.tools.build.bundletool.model.utils.files.FileUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.android.tools.build.bundletool.model.utils.files.FilePreconditions.checkFileExistsAndReadable;

/**
 * Created by YangJing on 2019/10/09 .
 * Email: yangjing.yeoh@bytedance.com
 */
public class FileOperation {

    private static final int BUFFER = 8192;

    public static boolean deleteDir(File file) {
        if (file == null || (!file.exists())) {
            return false;
        }
        if (file.isFile()) {
            file.delete();
        } else if (file.isDirectory()) {
            File files[] = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                deleteDir(files[i]);
            }
        }
        file.delete();
        return true;
    }

    /**
     * Extracts the zip into targetDir. An existing targetDir is deleted first,
     * so the result never contains stale files from a previous extraction.
     */
    public static void uncompress(Path uncompressedFile, Path targetDir) throws IOException {
        checkFileExistsAndReadable(uncompressedFile);
        if (Files.exists(targetDir)) {
            deleteDir(targetDir.toFile());
        }
        FileUtils.createDirectories(targetDir);
        Path targetRoot = targetDir.toAbsolutePath().normalize();
        try (ZipFile zipFile = new ZipFile(uncompressedFile.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            byte[] buf = new byte[BUFFER];
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                // guard against Zip Slip: the entry must stay inside targetDir
                Path entryPath = targetRoot.resolve(entry.getName()).normalize();
                if (!entryPath.startsWith(targetRoot)) {
                    throw new IOException("Illegal zip entry path: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    FileUtils.createDirectories(entryPath);
                    continue;
                }
                File file = entryPath.toFile();
                File parent = file.getParentFile();
                if (parent != null && !parent.exists()) {
                    FileUtils.createDirectories(parent.toPath());
                }
                try (BufferedInputStream bis = new BufferedInputStream(zipFile.getInputStream(entry));
                     BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file), BUFFER)) {
                    int len;
                    while ((len = bis.read(buf, 0, BUFFER)) != -1) {
                        bos.write(buf, 0, len);
                    }
                }
            }
        }
    }

    public static String getNetFileSizeDescription(long size) {
        StringBuilder bytes = new StringBuilder();
        DecimalFormat format = new DecimalFormat("###.0");
        if (size >= 1024 * 1024 * 1024) {
            double i = (size / (1024.0 * 1024.0 * 1024.0));
            bytes.append(format.format(i)).append("GB");
        } else if (size >= 1024 * 1024) {
            double i = (size / (1024.0 * 1024.0));
            bytes.append(format.format(i)).append("MB");
        } else if (size >= 1024) {
            double i = (size / (1024.0));
            bytes.append(format.format(i)).append("KB");
        } else {
            if (size <= 0) {
                bytes.append("0B");
            } else {
                bytes.append((int) size).append("B");
            }
        }
        return bytes.toString();
    }

    public static long getFileSizes(File f) {
        long size = 0;
        if (f.exists() && f.isFile()) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(f);
                size = fis.available();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (fis != null) {
                        fis.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return size;
    }

    public static long getZipPathFileSize(ZipFile zipFile, ZipEntry zipEntry) {
        long size = 0;
        try {
            InputStream is = new BufferedInputStream(zipFile.getInputStream(zipEntry));
            size = is.available();
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return size;
    }

    public static void copyFileUsingStream(File source, File dest) throws IOException {
        FileInputStream is = null;
        FileOutputStream os = null;
        File parent = dest.getParentFile();
        if (parent != null && (!parent.exists())) {
            parent.mkdirs();
        }
        try {
            is = new FileInputStream(source);
            os = new FileOutputStream(dest, false);

            byte[] buffer = new byte[BUFFER];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
            if (is != null) {
                is.close();
            }
            if (os != null) {
                os.close();
            }
        }
    }

    public static String getFileSimpleName(ZipPath zipPath) {
        return zipPath.getFileName().toString();
    }

    public static String getFileSuffix(ZipPath zipPath) {
        String fileName = zipPath.getName(zipPath.getNameCount() - 1).toString();
        if (!fileName.contains(".")) {
            return fileName;
        }
        String[] values = fileName.replace(".", "/").split("/");
        return fileName.substring(values[0].length());
    }

    public static String getParentFromZipFilePath(String zipPath) {
        if (!zipPath.contains("/")) {
            throw new IllegalArgumentException("invalid zipPath: " + zipPath);
        }
        String[] values = zipPath.split("/");
        return zipPath.substring(0, zipPath.indexOf(values[values.length - 1]) - 1);
    }

    public static String getNameFromZipFilePath(String zipPath) {
        if (!zipPath.contains("/")) {
            throw new IllegalArgumentException("invalid zipPath: " + zipPath);
        }
        String[] values = zipPath.split("/");
        return values[values.length - 1];
    }

    public static String getFilePrefixByFileName(String fileName) {
        if (!fileName.contains(".")) {
            throw new IllegalArgumentException("invalid file name: " + fileName);
        }
        String[] values = fileName.replace(".", "/").split("/");
        return values[0];
    }
}
