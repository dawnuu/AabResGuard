package com.bytedance.android.aabresguard.executors;

import static com.android.tools.build.bundletool.model.utils.files.FilePreconditions.checkFileExistsAndReadable;

import com.android.tools.build.bundletool.model.AppBundle;
import com.android.tools.build.bundletool.model.BundleMetadata;
import com.android.tools.build.bundletool.model.ZipPath;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.annotation.Nullable;

/**
 * @author chancey
 * @date 2025/1/24
 **/
public class BundleMetadataRemove {
    private ZipFile bundleZipFile;
    private final AppBundle rawAppBundle;
    private final Set<String> whiteList;

    public BundleMetadataRemove(Path bundlePath, AppBundle rawAppBundle, Set<String> whiteList) throws IOException {
        checkFileExistsAndReadable(bundlePath);
        this.bundleZipFile = new ZipFile(bundlePath.toFile());
        this.rawAppBundle = rawAppBundle;
        this.whiteList = whiteList;
    }

    public AppBundle remove() throws IOException {
        BundleMetadata.Builder builder = BundleMetadata.builder();
        if (whiteList != null && !whiteList.isEmpty()) {
            Enumeration<? extends ZipEntry> entries = bundleZipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry zipEntry = entries.nextElement();
                String name = getName(zipEntry);
                if (name == null) {
                    continue;
                }
                ZipPath zipPath = ZipPath.create(name);
                if (needKeep(zipEntry.getName())) {
                    System.out.println("keep BUNDLE-METADATA file：" + zipEntry.getName());
                    builder.addFile(zipPath, () -> bundleZipFile.getInputStream(zipEntry));
                }
            }
        }
        return this.rawAppBundle.toBuilder().setBundleMetadata(builder.build()).build();
    }

    @Nullable
    private String getName(ZipEntry zipEntry) {
        String name = zipEntry.getName();
        String startStr = "BUNDLE-METADATA";
        int start = name.indexOf(startStr);
        if (start == -1) {
            return null;
        } else {
            return name.substring(start + startStr.length()).replaceFirst("/", ".");
        }
    }

    private Boolean needKeep(String resourceName) {
        if (!resourceName.startsWith("BUNDLE-METADATA")) {
            return false;
        }
        for (String name : whiteList) {
            if (resourceName.contains(name)) {
                return true;
            }
        }
        return false;
    }
}
