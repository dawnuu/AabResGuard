package com.bytedance.android.aabresguard.executors;

import static com.android.tools.build.bundletool.model.utils.files.FilePreconditions.checkFileExistsAndReadable;

import com.android.tools.build.bundletool.model.AppBundle;
import com.android.tools.build.bundletool.model.BundleModule;
import com.android.tools.build.bundletool.model.BundleModuleName;
import com.android.tools.build.bundletool.model.ModuleEntry;
import com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

/**
 * @author chancey
 * @date 2025/1/24
 **/
public class BaseRootFileRemove {

    private ZipFile bundleZipFile;
    private AppBundle rawAppBundle;


    public BaseRootFileRemove(Path bundlePath, AppBundle rawAppBundle) throws IOException {
        checkFileExistsAndReadable(bundlePath);
        this.bundleZipFile = new ZipFile(bundlePath.toFile());
        this.rawAppBundle = rawAppBundle;
    }

    public AppBundle remove() throws IOException {
        Map<BundleModuleName, BundleModule> bundleModules = new HashMap<>();
        for (Map.Entry<BundleModuleName, BundleModule> entry : rawAppBundle.getModules().entrySet()) {
            BundleModule bundleModule = entry.getValue();
            BundleModule.Builder builder = bundleModule.toBuilder();
            List<ModuleEntry> entries = bundleModule.getEntries().stream()
                    .filter(moduleEntry -> !moduleEntry.getPath().startsWith("root"))
                    .collect(Collectors.toList());
            builder.setRawEntries(entries);
            bundleModules.put(entry.getKey(), builder.build());
        }
        return this.rawAppBundle.toBuilder().setModules(ImmutableMap.copyOf(bundleModules)).build();
    }
}
