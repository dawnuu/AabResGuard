package com.bytedance.android.aabresguard.bundle;

import com.android.tools.build.bundletool.io.ZipBuilder;
import com.android.tools.build.bundletool.io.ZipBuilder.EntryOption;
import com.android.tools.build.bundletool.model.AppBundle;
import com.android.tools.build.bundletool.model.BundleModule;
import com.android.tools.build.bundletool.model.BundleModule.SpecialModuleEntry;
import com.android.tools.build.bundletool.model.ModuleEntry;
import com.android.tools.build.bundletool.model.ZipPath;
import com.google.common.collect.UnmodifiableIterator;

import java.io.IOException;
import java.nio.file.Path;

public class CustomAppBundleSerializer {
    private final boolean allEntriesUncompressed;

    public CustomAppBundleSerializer(boolean allEntriesUncompressed) {
        this.allEntriesUncompressed = allEntriesUncompressed;
    }

    public CustomAppBundleSerializer() {
        this(false);
    }

    public void writeToDisk(AppBundle bundle, Path pathOnDisk) throws IOException {
        ZipBuilder zipBuilder = new ZipBuilder();
        ZipBuilder.EntryOption[] compression = this.allEntriesUncompressed ? new ZipBuilder.EntryOption[]{EntryOption.UNCOMPRESSED} : new ZipBuilder.EntryOption[0];
        zipBuilder.addFileWithProtoContent(ZipPath.create("BundleConfig.pb"), bundle.getBundleConfig(), compression);
        UnmodifiableIterator var5;
        //移除构建BUNDLE-METADATA目录代码
        var5 = bundle.getModules().values().iterator();

        while (var5.hasNext()) {
            BundleModule module = (BundleModule) var5.next();
            ZipPath moduleDir = ZipPath.create(module.getName().toString());
            UnmodifiableIterator var8 = module.getEntries().iterator();

            while (var8.hasNext()) {
                ModuleEntry entry = (ModuleEntry) var8.next();
                ZipPath entryPath = moduleDir.resolve(entry.getPath());
                if (entry.isDirectory()) {
                    zipBuilder.addDirectory(entryPath);
                } else {
                    zipBuilder.addFile(entryPath, entry.getContentSupplier(), compression);
                }
            }

            zipBuilder.addFileWithProtoContent(moduleDir.resolve(SpecialModuleEntry.ANDROID_MANIFEST.getPath()), module.getAndroidManifest().getManifestRoot().getProto(), compression);
            module.getAssetsConfig().ifPresent((assetsConfig) -> {
                zipBuilder.addFileWithProtoContent(moduleDir.resolve(SpecialModuleEntry.ASSETS_TABLE.getPath()), assetsConfig, compression);
            });
            module.getNativeConfig().ifPresent((nativeConfig) -> {
                zipBuilder.addFileWithProtoContent(moduleDir.resolve(SpecialModuleEntry.NATIVE_LIBS_TABLE.getPath()), nativeConfig, compression);
            });
            module.getResourceTable().ifPresent((resourceTable) -> {
                zipBuilder.addFileWithProtoContent(moduleDir.resolve(SpecialModuleEntry.RESOURCE_TABLE.getPath()), resourceTable, compression);
            });
            module.getApexConfig().ifPresent((apexConfig) -> {
                zipBuilder.addFileWithProtoContent(moduleDir.resolve(SpecialModuleEntry.APEX_TABLE.getPath()), apexConfig, compression);
            });
        }

        zipBuilder.writeTo(pathOnDisk);
    }
}
