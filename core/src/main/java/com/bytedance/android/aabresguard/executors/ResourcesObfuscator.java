package com.bytedance.android.aabresguard.executors;

import static com.bytedance.android.aabresguard.bundle.AppBundleUtils.getEntryNameByResourceName;
import static com.bytedance.android.aabresguard.bundle.AppBundleUtils.getTypeNameByResourceName;
import static com.bytedance.android.aabresguard.bundle.ResourcesTableOperation.updateEntryConfigValueList;
import static com.bytedance.android.aabresguard.utils.FileOperation.getFilePrefixByFileName;
import static com.bytedance.android.aabresguard.utils.FileOperation.getNameFromZipFilePath;
import static com.bytedance.android.aabresguard.utils.FileOperation.getParentFromZipFilePath;

import com.android.aapt.Resources;
import com.android.tools.build.bundletool.model.AppBundle;
import com.android.tools.build.bundletool.model.BundleModule;
import com.android.tools.build.bundletool.model.BundleModuleName;
import com.android.tools.build.bundletool.model.InMemoryModuleEntry;
import com.android.tools.build.bundletool.model.ModuleEntry;
import com.android.tools.build.bundletool.model.ZipPath;
import com.android.tools.build.bundletool.model.utils.ResourcesUtils;
import com.bytedance.android.aabresguard.bundle.AppBundleUtils;
import com.bytedance.android.aabresguard.bundle.ResourcesTableBuilder;
import com.bytedance.android.aabresguard.bundle.ResourcesTableOperation;
import com.bytedance.android.aabresguard.model.ResourcesMapping;
import com.bytedance.android.aabresguard.obfuscation.ResGuardStringBuilder;
import com.bytedance.android.aabresguard.parser.ResourcesMappingParser;
import com.bytedance.android.aabresguard.utils.FileOperation;
import com.bytedance.android.aabresguard.utils.TimeClock;
import com.bytedance.android.aabresguard.utils.Utils;
import com.google.common.collect.ImmutableMap;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

/**
 * Created by YangJing on 2019/10/14 .
 * Email: yangjing.yeoh@bytedance.com
 */
public class ResourcesObfuscator {
    public static final String RESOURCE_ANDROID_PREFIX = "android:";
    public static final String FILE_MAPPING_NAME = "resources-mapping.txt";
    private static final Logger logger = Logger.getLogger(ResourcesObfuscator.class.getName());

    private final AppBundle rawAppBundle;
    private final Set<String> whiteListRules;
    private final Path outputMappingPath;
    private final ZipFile bundleZipFile;
    private final boolean useRandomName;
    private final boolean enableMutateMd5;
    private final ResGuardStringBuilder mResGuardStringBuilder;
    private ResourcesMapping resourcesMapping;
    private final Random random = new Random();

    // 允许修改 MD5 的媒体文件后缀
    private static final Set<String> MEDIA_EXTENSIONS = new HashSet<>();

    static {
        // 图片
        MEDIA_EXTENSIONS.add("png");
        MEDIA_EXTENSIONS.add("jpg");
        MEDIA_EXTENSIONS.add("jpeg");
        MEDIA_EXTENSIONS.add("webp");
        MEDIA_EXTENSIONS.add("gif");
        MEDIA_EXTENSIONS.add("bmp");
        // 音频
        MEDIA_EXTENSIONS.add("mp3");
        MEDIA_EXTENSIONS.add("wav");
        MEDIA_EXTENSIONS.add("ogg");
        MEDIA_EXTENSIONS.add("m4a");
        MEDIA_EXTENSIONS.add("aac");
        // 视频
        MEDIA_EXTENSIONS.add("mp4");
        MEDIA_EXTENSIONS.add("webm");
        MEDIA_EXTENSIONS.add("mkv");
        MEDIA_EXTENSIONS.add("avi");
    }

    public ResourcesObfuscator(Path bundlePath, AppBundle rawAppBundle, Set<String> whiteListRules, Path outputLogLocationDir, Path mappingPath, boolean useRandomName, boolean enableMutateMd5) throws IOException {
        if (mappingPath != null && mappingPath.toFile().exists()) {
            resourcesMapping = new ResourcesMappingParser(mappingPath).parse();
        } else {
            resourcesMapping = new ResourcesMapping();
        }

        this.bundleZipFile = new ZipFile(bundlePath.toFile());

        outputMappingPath = new File(outputLogLocationDir.toFile(), FILE_MAPPING_NAME).toPath();
        if (Files.exists(outputMappingPath, new LinkOption[0])) {
            logger.warning("Mapping file: " + outputMappingPath + " already existing! Deleting...");
            Files.delete(outputMappingPath);
        }

        this.rawAppBundle = rawAppBundle;
        this.whiteListRules = whiteListRules;
        this.useRandomName = useRandomName;
        this.enableMutateMd5 = enableMutateMd5;

        // 全局唯一字典生成器
        this.mResGuardStringBuilder = new ResGuardStringBuilder();
        this.mResGuardStringBuilder.reset(null, useRandomName);
    }

    public Path getOutputMappingPath() {
        return outputMappingPath;
    }

    public AppBundle obfuscate() throws IOException {
        TimeClock timeClock = new TimeClock();

        checkResMappingRules();
        Map<BundleModuleName, BundleModule> obfuscatedModules = new HashMap<>();
        Map<String, Set<String>> typeEntryMapping = generateObfuscatedEntryFilesFromMapping();

        for (Map.Entry<BundleModuleName, BundleModule> entry : rawAppBundle.getModules().entrySet()) {
            BundleModule bundleModule = entry.getValue();
            BundleModuleName bundleModuleName = entry.getKey();
            generateResourceMappingRule(bundleModule, typeEntryMapping);
            Map<String, String> obfuscateModuleEntriesMap = obfuscateModuleEntries(bundleModule, typeEntryMapping);
            BundleModule obfuscatedModule = obfuscateBundleModule(bundleModule, obfuscateModuleEntriesMap);
            obfuscatedModules.put(bundleModuleName, obfuscatedModule);
        }

        AppBundle appBundle = rawAppBundle.toBuilder()
                .setModules(ImmutableMap.copyOf(obfuscatedModules))
                .build();

        System.out.println(String.format("obfuscate resources done, cost %s", timeClock.getCost()));
        resourcesMapping.writeMappingToFile(outputMappingPath);
        return appBundle;
    }

    private Map<String, Set<String>> generateObfuscatedEntryFilesFromMapping() {
        Map<String, Set<String>> typeEntryMapping = new HashMap<>();
        for (String path : resourcesMapping.getEntryFilesMapping().values()) {
            String parentPath = getParentFromZipFilePath(path);
            String name = getFilePrefixByFileName(getNameFromZipFilePath(path));
            typeEntryMapping.computeIfAbsent(parentPath, k -> new HashSet<>()).add(name);
        }
        for (String entry : resourcesMapping.getResourceMapping().values()) {
            String name = getEntryNameByResourceName(entry);
            String type = getTypeNameByResourceName(entry);
            typeEntryMapping.computeIfAbsent(type, k -> new HashSet<>()).add(name);
        }
        return typeEntryMapping;
    }

    private void generateResourceMappingRule(BundleModule bundleModule, Map<String, Set<String>> typeEntryMapping) {
        if (!bundleModule.getResourceTable().isPresent()) return;

        Resources.ResourceTable table = bundleModule.getResourceTable().get();
        ResourcesUtils.getAllFileReferences(table)
                .stream()
                .map(ZipPath::getParent)
                .filter(Objects::nonNull)
                .filter(path -> !resourcesMapping.getDirMapping().containsKey(path.toString()))
                .forEach(path -> {
                    String name = mResGuardStringBuilder.getReplaceString(resourcesMapping.getPathMappingNameList());
                    resourcesMapping.putDirMapping(path.toString(), BundleModule.RESOURCES_DIRECTORY.toString() + "/" + name);
                });

        ResourcesUtils.entries(table).forEach(entry -> {
            String resourceId = entry.getResourceId().toString();
            String resourceName = AppBundleUtils.getResourceFullName(entry);
            Set<String> obfuscationList = typeEntryMapping.computeIfAbsent(entry.getType().getName(), k -> new HashSet<>());

            if (resourcesMapping.getResourceMapping().containsKey(resourceName)) {
                if (!shouldBeObfuscated(resourceName)) {
                    resourcesMapping.getResourceMapping().remove(resourceName);
                } else {
                    String obfuscateResourceName = resourcesMapping.getResourceMapping().get(resourceName);
                    obfuscationList.add(AppBundleUtils.getEntryNameByResourceName(obfuscateResourceName));
                }
            } else {
                if (shouldBeObfuscated(resourceName)) {
                    String name = mResGuardStringBuilder.getReplaceString(obfuscationList);
                    obfuscationList.add(name);
                    String obfuscatedResourceName = AppBundleUtils.getResourceFullName(entry.getPackage().getPackageName(), entry.getType().getName(), name);
                    resourcesMapping.putResourceMapping(resourceName, obfuscatedResourceName);
                }
            }
        });
    }

    private Map<String, String> obfuscateModuleEntries(BundleModule bundleModule, Map<String, Set<String>> typeMappingMap) {
        Map<String, String> obfuscateEntries = new HashMap<>();
        bundleModule.getEntries().stream()
                .filter(entry -> entry.getPath().startsWith(BundleModule.RESOURCES_DIRECTORY))
                .forEach(entry -> {
                    String entryDir = entry.getPath().getParent().toString();
                    String obfuscateDir = resourcesMapping.getDirMapping().get(entryDir);
                    if (obfuscateDir == null) return;

                    Set<String> mapping = typeMappingMap.computeIfAbsent(obfuscateDir, k -> new HashSet<>());
                    String bundleRawPath = bundleModule.getName().getName() + "/" + entry.getPath().toString();
                    String bundleObfuscatedPath = resourcesMapping.getEntryFilesMapping().get(bundleRawPath);

                    if (bundleObfuscatedPath == null && shouldBeObfuscated(bundleRawPath)) {
                        String fileSuffix = FileOperation.getFileSuffix(entry.getPath());
                        String obfuscatedName = mResGuardStringBuilder.getReplaceString(mapping);
                        mapping.add(obfuscatedName);
                        bundleObfuscatedPath = obfuscateDir + "/" + obfuscatedName + fileSuffix;
                        resourcesMapping.putEntryFileMapping(bundleRawPath, bundleObfuscatedPath);
                    }
                    if (bundleObfuscatedPath != null) {
                        obfuscateEntries.put(bundleRawPath, bundleObfuscatedPath);
                    }
                });
        return obfuscateEntries;
    }

    private BundleModule obfuscateBundleModule(BundleModule bundleModule, Map<String, String> obfuscatedEntryMap) throws IOException {
        BundleModule.Builder builder = bundleModule.toBuilder();
        List<ModuleEntry> obfuscateEntries = new ArrayList<>();
        for (ModuleEntry entry : bundleModule.getEntries()) {
            String bundleRawPath = bundleModule.getName().getName() + "/" + entry.getPath().toString();
            String obfuscatedPath = obfuscatedEntryMap.get(bundleRawPath);
            if (obfuscatedPath != null) {
                byte[] data = AppBundleUtils.readByte(bundleZipFile, entry, bundleModule);
                // 只有图片、音视频等媒体文件才修改 MD5
                if (enableMutateMd5 && isMediaFile(bundleRawPath)) {
                    data = mutateData(data);
                }
                obfuscateEntries.add(InMemoryModuleEntry.ofFile(obfuscatedPath, data));
            } else {
                obfuscateEntries.add(entry);
            }
        }
        builder.setRawEntries(obfuscateEntries);
        Resources.ResourceTable obfuscatedResTable = obfuscateResourceTable(bundleModule, obfuscatedEntryMap);
        if (obfuscatedResTable != null) builder.setResourceTable(obfuscatedResTable);
        return builder.build();
    }

    private boolean isMediaFile(String path) {
        if (path == null || !path.contains(".")) return false;
        String extension = path.substring(path.lastIndexOf(".") + 1).toLowerCase();
        return MEDIA_EXTENSIONS.contains(extension);
    }

    private byte[] mutateData(byte[] data) {
        if (data == null || data.length == 0) return data;
        byte[] newData = new byte[data.length + 1];
        System.arraycopy(data, 0, newData, 0, data.length);
        newData[data.length] = (byte) (random.nextInt(256));
        return newData;
    }

    private Resources.ResourceTable obfuscateResourceTable(BundleModule bundleModule, Map<String, String> obfuscatedEntryMap) {
        if (!bundleModule.getResourceTable().isPresent()) return null;
        Resources.ResourceTable resourceTable = bundleModule.getResourceTable().get();
        ResourcesTableBuilder resourcesTableBuilder = new ResourcesTableBuilder();
        ResourcesUtils.entries(resourceTable).forEach(entry -> {
            String resourceName = AppBundleUtils.getResourceFullName(entry);
            String resourceId = entry.getResourceId().toString();
            String obfuscatedResName = resourcesMapping.getResourceMapping().get(resourceName);
            Resources.Entry obfuscatedEntry = entry.getEntry();
            if (obfuscatedResName != null) {
                obfuscatedEntry = ResourcesTableOperation.updateEntryName(obfuscatedEntry, getEntryNameByResourceName(obfuscatedResName));
            }
            List<Resources.ConfigValue> configValues = Stream.of(obfuscatedEntry)
                    .map(Resources.Entry::getConfigValueList).flatMap(Collection::stream)
                    .map(configValue -> {
                        if (!configValue.getValue().getItem().hasFile()) return configValue;
                        String bundleRawPath = bundleModule.getName().getName() + "/" + configValue.getValue().getItem().getFile().getPath();
                        String obfuscatedPath = obfuscatedEntryMap.get(bundleRawPath);
                        return obfuscatedPath != null ? ResourcesTableOperation.replaceEntryPath(configValue, obfuscatedPath) : configValue;
                    }).collect(Collectors.toList());
            if (!configValues.isEmpty()) {
                obfuscatedEntry = updateEntryConfigValueList(obfuscatedEntry, configValues);
            }
            resourcesTableBuilder.addPackage(entry.getPackage()).addResource(entry.getType(), obfuscatedEntry);
        });
        return resourcesTableBuilder.build();
    }

    private void checkResMappingRules() {
        resourcesMapping.getDirMapping().values().stream().map(ZipPath::create).forEach(path -> {
            if (!path.startsWith(BundleModule.RESOURCES_DIRECTORY))
                throw new IllegalArgumentException("Invalid mapping obfuscation rule: " + path);
        });
    }

    private boolean shouldBeObfuscated(String resourceName) {
        if (resourceName.startsWith(RESOURCE_ANDROID_PREFIX)) return false;
        for (String rule : whiteListRules) {
            if (Pattern.compile(Utils.convertToPatternString(rule)).matcher(resourceName).matches())
                return false;
        }
        return true;
    }
}
