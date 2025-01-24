package com.bytedance.android.aabresguard.executors;

import static com.android.tools.build.bundletool.model.utils.files.FilePreconditions.checkFileExistsAndReadable;

import com.android.tools.build.bundletool.model.AppBundle;
import com.android.tools.build.bundletool.model.BundleMetadata;
import com.android.tools.build.bundletool.model.InputStreamSupplier;
import com.android.tools.build.bundletool.model.ZipPath;
import com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.nio.file.Path;
import java.util.zip.ZipFile;

/**
 * @author chancey
 * @date 2025/1/24
 **/
public class BundleMetadataRemove {
    private ZipFile bundleZipFile;
    private AppBundle rawAppBundle;


    public BundleMetadataRemove(Path bundlePath, AppBundle rawAppBundle) throws IOException {
        checkFileExistsAndReadable(bundlePath);
        this.bundleZipFile = new ZipFile(bundlePath.toFile());
        this.rawAppBundle = rawAppBundle;
    }

    public AppBundle remove() {
        //构建一个空的BUNDLE-METADATA从而达到移除的效果
        BundleMetadata bundleMetadata = new BundleMetadata() {
            @Override
            public ImmutableMap<ZipPath, InputStreamSupplier> getFileDataMap() {
                return ImmutableMap.of();
            }
        };
        return this.rawAppBundle.toBuilder().setBundleMetadata(bundleMetadata).build();
    }
}
