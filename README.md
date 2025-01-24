# AabResGuard

> 本工具由字节跳动抖音 Android 团队提供。

## 特性

> 针对 aab 文件的资源混淆工具

- **资源去重：** 对重复资源文件进行合并，缩减包体积。
- **文件过滤：** 支持对 `bundle` 包中的文件进行过滤，目前只支持 `MATE-INFO/`、`lib/` 路径下的过滤。
- **白名单：** 白名单中的资源，名称不予混淆。
- **增量混淆：** 输入 `mapping` 文件，支持增量混淆。
- **文案删除：** 输入按行分割的字符串文件，移除文案及翻译。

## 快速开始

- **命令行工具：** 支持命令行一键输入输出。
- **Gradle plugin：** 支持 `gradle plugin`，使用原始打包命令执行混淆。

### Gradle plugin

在 `build.gradle(root project)` 中进行配置

```gradle
buildscript {
  repositories {
    mavenCentral()
    google()
   }
  dependencies {
    classpath "com.bytedance.android:aabresguard-plugin:0.1.13"
  }
}
```

在 `build.gradle(application)` 中配置

```gradle
apply plugin: "com.bytedance.android.aabResGuard"
aabResGuard {
    mappingFile = file("mapping.txt").toPath() // 用于增量混淆的 mapping 文件
    whiteList = [ // 白名单规则
        "*.R.raw.*",
        "*.R.drawable.icon"
    ]
    obfuscatedBundleFileName = "duplicated-app.aab" // 混淆后的文件名称，必须以 `.aab` 结尾，默认文件名为packageName_versioName_versionCode.aab
    mergeDuplicatedRes = true // 是否允许去除重复资源
    enableFilterFiles = true // 是否允许过滤文件
    filterList = [ // 文件过滤规则
        "*/arm64-v8a/*",
        "META-INF/*"
    ]
    enableFilterStrings = false // 过滤文案
    removeBundleMetadata = true // 是否移除BUNDLE-METADATA，默认开启
    removeRootFiles = true // 是否移除root目录，默认关闭
    unusedStringPath = file("unused.txt").toPath() // 过滤文案列表路径 默认在mapping同目录查找
    languageWhiteList = ["en", "zh"] // 保留en,en-xx,zh,zh-xx等语言，其余均删除
}
```

`aabResGuard plugin` 侵入了 `bundle` 打包流程，可以直接执行原始打包命令进行混淆。

```cmd
./gradlew clean :app:bundleDebug --stacktrace
```

通过 `gradle` 获取混淆后的 `bundle` 文件路径

```groovy
def aabResGuardPlugin = project.tasks.getByName("aabresguard${VARIANT_NAME}")
Path bundlePath = aabResGuardPlugin.getObfuscatedBundlePath()
```
