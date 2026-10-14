> [!IMPORTANT]
> **迁移提示：请使用新版 [AndResGuard](https://github.com/dawnuu/AndResGuard)。**
> 新版统一支持 AAB / APK 资源处理，接入与配置说明见 [迁移指南](https://github.com/dawnuu/AndResGuard/blob/codex/apk-support/README.zh-CN.md#迁移教程)。

# AabResGuard

[English](README.md) | [简体中文](README.zh-CN.md)

[![Maven Central](https://img.shields.io/maven-central/v/io.github.dawnuu/aabresguard-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.dawnuu/aabresguard-plugin)

## 迁移教程

> **Maven Central 迁移说明：** 从自定义 Maven 仓库迁移到 Maven Central 时，需要更新仓库、插件 ID 和旧版依赖，具体如下。源代码中的 Java/Kotlin 包名无需修改。
>
> | 项目 | 自定义 Maven 接入 | Maven Central 接入 |
> | --- | --- | --- |
> | 仓库 | 自定义 Maven 仓库 | mavenCentral() |
> | Plugins DSL ID | com.bytedance.android.aabResGuard | io.github.dawnuu.aabresguard |
> | 旧版依赖 | com.bytedance.android:aabresguard-plugin:0.1.x | io.github.dawnuu:aabresguard-plugin:1.0.1 |
> | 旧版 apply ID | com.bytedance.android.aabResGuard | io.github.dawnuu.aabresguard |

## 接入教程

AabResGuard 提供两种接入方式：

- **Gradle Plugin：** 推荐方式，集成到打包流程，执行原始打包命令即可完成混淆。
- **命令行工具：** 适用于 CI/CD 或独立使用场景，通过 jar 包命令行调用。

---

### 一、Gradle Plugin

#### 1. 接入插件

推荐使用 [Gradle plugins DSL](https://docs.gradle.org/current/userguide/plugins.html#sec:plugins_block) 接入。

在 `gradle/libs.versions.toml` 中声明版本：

```toml
[versions]
aabresguard = "1.0.1"

[plugins]
aabresguard = { id = "io.github.dawnuu.aabresguard", version.ref = "aabresguard" }
```

`libs.versions.toml` 不区分 Groovy/Kotlin DSL，两者共用。

**Groovy DSL：**

在 `build.gradle(root project)` 中声明插件版本并配置仓库：

```gradle
plugins {
  alias(libs.plugins.aabresguard) apply false
}

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}
```

在 `build.gradle(application)` 中启用插件：

```gradle
plugins {
  alias(libs.plugins.aabresguard)
}
```

**Kotlin DSL（.kts）：**

在 `build.gradle.kts(root project)` 中声明插件版本并配置仓库：

```kotlin
plugins {
  alias(libs.plugins.aabresguard) apply false
}

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}
```

在 `build.gradle.kts(application)` 中启用插件：

```kotlin
plugins {
  alias(libs.plugins.aabresguard)
}
```

> 也可以继续使用旧版 `buildscript` 方式接入：
>
> **Groovy DSL：**
>
> ```gradle
> buildscript {
>   repositories {
>     mavenCentral()
>     google()
>   }
>   dependencies {
>     classpath "io.github.dawnuu:aabresguard-plugin:1.0.1"
>   }
> }
> ```
>
> 在 application module 中启用插件：
>
> ```gradle
> apply plugin: "io.github.dawnuu.aabresguard"
> ```
>
> **Kotlin DSL（.kts）：**
>
> ```kotlin
> buildscript {
>   repositories {
>     mavenCentral()
>     google()
>   }
>   dependencies {
>     classpath("io.github.dawnuu:aabresguard-plugin:1.0.1")
>   }
> }
> ```
>
> 在 application module 中启用插件：
>
> ```kotlin
> apply(plugin = "io.github.dawnuu.aabresguard")
> ```

#### 2. 配置参数

在 `build.gradle(application)` 中添加 `aabResGuard` 配置块：

**Groovy DSL：**

```gradle
aabResGuard {
    // 是否开启资源混淆，默认 true
    enableObfuscate = true
    // 是否生成随机资源名称，默认 false
    useRandomName = false
    // 是否给媒体文件追加随机字节以修改 MD5，默认 false
    enableMutateMd5 = false
    // 默认稳定修改（相同输入得到相同结果）；改变种子可得到另一份稳定结果
    mutateMd5Seed = ""
    // 找不到签名配置时是否回退到 debug 签名，默认 false（直接构建失败）
    allowDebugSignFallback = false
    // 是否允许去除重复资源，默认 false
    mergeDuplicatedRes = true
    // 无损压缩 PNG/JPEG/GIF/静态 WebP，默认 false
    enableCompressImages = false
    // 可选：覆盖内置压缩工具；留空使用随 JAR 打包的工具
    imageCompressionToolsDir = ""
    // 是否允许过滤文件，默认 false
    enableFilterFiles = true
    // 是否允许过滤文案，默认 false
    enableFilterStrings = false
    // 是否移除 BUNDLE-METADATA，默认 true
    removeBundleMetadata = true
    // 是否移除 root 目录，默认 false
    // 开启后需自行测试稳定性
    removeRootFiles = false
    // removeBundleMetadata 开启时，保留匹配的 BUNDLE-METADATA 文件
    bundleMetaDataWhiteList = [
        "com.example.metadata"
    ]

    // 用于增量混淆的 mapping 文件（可选）
    mappingFile = file("mapping.txt").toPath()

    // 白名单规则，自定义的白名单会与内置白名单合并
    whiteList = [
        "*.R.raw.*",
        "*.R.drawable.icon"
    ]

    // 混淆后的文件名称，必须以 .aab 结尾
    // 支持变量: {applicationId} {versionName} {versionCode} {variantName} {buildTime}
    // 例如 "{applicationId}_{versionName}_{buildTime}.aab"
    // 外部覆盖: -PaabResGuard.fileName="{applicationId}_{channel}.aab"
    // 以及 -PaabResGuard.var.channel=google（命令行或 gradle.properties，优先于本文件）
    // 自定义变量，例如 fileNameVariables = [channel: "google"] -> "{applicationId}_{channel}.aab"
    // 默认文件名为: {applicationId}_{versionName}_{versionCode}.aab
    obfuscatedBundleFileName = "duplicated-app.aab"

    // 文件过滤规则
    filterList = [
        "*/arm64-v8a/*",
        "META-INF/*"
    ]

    // 过滤文案列表路径，相对于当前工作目录（可选）
    unusedStringPath = "unused.txt"

    // 保留 en,en-xx,zh,zh-xx 等语言，其余均删除（可选）
    languageWhiteList = ["en", "zh"]

    // 默认关闭 VirusTotal 上传，不要将 API key 硬编码到仓库
    enableVirusTotalUpload = false
    virusTotalApiKey = System.getenv("VIRUSTOTAL_API_KEY") ?: ""
}
```

**Kotlin DSL（.kts）：**

```kotlin
configure<AabResGuardExtension> {
    // 是否开启资源混淆，默认 true
    enableObfuscate = true
    // 是否生成随机资源名称，默认 false
    useRandomName = false
    // 是否给媒体文件追加随机字节以修改 MD5，默认 false
    enableMutateMd5 = false
    // 默认稳定修改（相同输入得到相同结果）；改变种子可得到另一份稳定结果
    mutateMd5Seed = ""
    // 是否允许去除重复资源，默认 false
    mergeDuplicatedRes = true
    enableCompressImages = false
    imageCompressionToolsDir = ""
    // 是否允许过滤文件，默认 false
    enableFilterFiles = true
    // 是否允许过滤文案，默认 false
    enableFilterStrings = false
    // 是否移除 BUNDLE-METADATA，默认 true
    removeBundleMetadata = true
    // 是否移除 root 目录，默认 false
    // 开启后需自行测试稳定性
    removeRootFiles = false
    // removeBundleMetadata 开启时，保留匹配的 BUNDLE-METADATA 文件
    bundleMetaDataWhiteList = setOf(
        "com.example.metadata"
    )

    // 用于增量混淆的 mapping 文件（可选）
    mappingFile = file("mapping.txt").toPath()

    // 白名单规则，自定义的白名单会与内置白名单合并
    whiteList = setOf(
        "*.R.raw.*",
        "*.R.drawable.icon"
    )

    // 混淆后的文件名称，必须以 .aab 结尾
    // 支持变量: {applicationId} {versionName} {versionCode} {variantName} {buildTime}
    // 例如 "{applicationId}_{versionName}_{buildTime}.aab"
    // 外部覆盖: -PaabResGuard.fileName="{applicationId}_{channel}.aab"
    // 以及 -PaabResGuard.var.channel=google（命令行或 gradle.properties，优先于本文件）
    // 自定义变量，例如 fileNameVariables = [channel: "google"] -> "{applicationId}_{channel}.aab"
    // 默认文件名为: {applicationId}_{versionName}_{versionCode}.aab
    obfuscatedBundleFileName = "duplicated-app.aab"

    // 文件过滤规则
    filterList = setOf(
        "*/arm64-v8a/*",
        "META-INF/*"
    )

    // 过滤文案列表路径，相对于当前工作目录（可选）
    unusedStringPath = "unused.txt"

    // 保留 en,en-xx,zh,zh-xx 等语言，其余均删除（可选）
    languageWhiteList = setOf("en", "zh")

    // 默认关闭 VirusTotal 上传，不要将 API key 硬编码到仓库
    enableVirusTotalUpload = false
    virusTotalApiKey = System.getenv("VIRUSTOTAL_API_KEY") ?: ""
}
```

#### 3. 执行混淆

`aabResGuard plugin` 侵入了 `bundle` 打包流程，可以直接执行原始打包命令进行混淆（默认仅处理 Release 变体）：

```cmd
./gradlew clean :app:bundleRelease --stacktrace
```

混淆完成后会输出混淆前后的包体积对比。

#### 4. 获取混淆产物路径

通过 Gradle Task API 获取混淆后的 bundle 文件路径：

**Groovy DSL：**

```groovy
def aabResGuardPlugin = project.tasks.getByName("aabresguardRelease")
Path bundlePath = aabResGuardPlugin.getObfuscatedBundlePath()
```

**Kotlin DSL（.kts）：**

```kotlin
val aabResGuardPlugin = project.tasks.getByName("aabresguardRelease")
val bundlePath: Path = aabResGuardPlugin.obfuscatedBundlePath()
```

---

### 图片无损压缩

可选配置 `imageCompressionCacheDir = rootProject.file("custom-cache/images").toPath()` 来指定其他缓存目录；默认不需要设置。若希望 `clean` 后仍可复用，请选择 `build/` 之外的目录。

Gradle 插件自动将成功的压缩结果缓存在项目根目录的 `.gradle/aabresguard/image-cache/`，`gradlew clean` 不会清除它。每次打包仍会读取原图；内容相同且压缩参数、工具内容和平台未改变时，直接将缓存图片写入新的 AAB，不修改源文件。不再缩小的结果也会缓存，压缩失败则不缓存。日志中的 `cached` 表示命中次数。缓存损坏时重新压缩，缓存读写失败时警告并继续；需要强制重新压缩时删除该目录即可。命令行默认不使用持久缓存。

设置 `enableCompressImages = true`，即可处理所有模块 `res/` 和 `assets/` 下的 PNG、JPG/JPEG、GIF（含动画）和静态 WebP。保持原格式和路径，不降低分辨率或画质，保留工具支持的元数据；仅当文件变小时替换。已优化的图片可能不再缩小，解码后的内存占用通常不变。

压缩工具已随 JAR 打包，无需额外安装、下载或配置目录。支持 macOS ARM64（Apple Silicon）/x86_64（Intel）、Windows x86_64 和 Linux x86_64（glibc，非 Alpine/musl）。运行时自动选择平台，在本次处理的临时目录解压并校验 SHA-256，处理结束后清理。关闭图片压缩时不会解压工具。

`imageCompressionToolsDir` 默认为空，表示使用内置工具；仅在需要覆盖工具版本或使用其他平台时设置，例如 `imageCompressionToolsDir = "/opt/homebrew/bin"`。压缩工具被系统阻止、缺失、失败或超时时，输出 `[AabResGuard WARNING]` 警告，保留原图并继续混淆和打包。同一格式失败后，本次处理跳过该格式后续压缩，避免重复启动和警告。临时目录创建或清理失败也只输出警告；主动取消任务仍会中断。

macOS：程序会设置当前用户的执行权限，已在 Apple Silicon Mac 上验证内置工具运行，但工具尚未使用 Developer ID 签名并通过 Apple 公证，不能保证所有 Gatekeeper/企业策略环境都允许执行。无法启动或被系统终止时，会输出 macOS 检查提示。公开分发前可按 [Apple 的签名与公证流程](https://developer.apple.com/documentation/security/notarizing-macos-software-before-distribution)处理工具；受限构建机也可用 `imageCompressionToolsDir` 指向已获准运行的工具。

Nine-patch（包括带 Android `np*` 块的编译图片）、APNG 和动画 WebP 会跳过并记录。其他格式（如 BMP、SVG）保持原样。日志输出优化数量、未缩小数量、跳过数量、因失败保留原图的数量和节省字节数；该字节数是图片文件大小差值，最终 AAB 大小还受 ZIP 压缩影响。压缩在资源混淆及 MD5 修改之前执行。

命令行只需增加 `--compress-images=true`；`--image-compression-tools-dir` 是可选覆盖参数。此功能通过命令行参数或 Gradle 配置启用，XML 配置暂不支持。工具版本、来源、许可证及重新生成方法见 [内置工具说明](core/src/main/resources/image-tools/NOTICE.md)。

### 二、命令行工具

#### 1. 下载

从 [Releases](https://github.com/dawnuu/AabResGuard/releases) 下载最新的 `AabResGuard-x.x.x.jar`。

#### 2. 准备配置文件

命令行工具需要一个 XML 配置文件来定义白名单和过滤规则，示例如下：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<resguard>
    <!-- 白名单规则 -->
    <issue id="whitelist" isactive="true">
        <path value="*.R.raw.*"/>
        <path value="*.R.drawable.icon"/>
    </issue>

    <!-- 文件过滤规则（可选） -->
    <filter isactive="true">
        <rule value="*/arm64-v8a/*"/>
        <rule value="META-INF/*"/>
    </filter>

    <!-- 文案过滤配置（可选） -->
    <filter-str isactive="true">
        <path value="unused.txt"/>
        <language value="en"/>
        <language value="zh"/>
    </filter-str>
</resguard>
```

XML 解析器只识别上面示例中的 `issue`、`filter` 和 `filter-str` 元素。`useRandomName`、`enableMutateMd5`、VirusTotal 上传和 BUNDLE-METADATA 白名单目前仅支持 Gradle 插件配置，命令行没有对应参数。

#### 3. 执行混淆

```cmd
java -jar AabResGuard.jar obfuscate-bundle \
  --bundle=app.aab \
  --output=obfuscated-app.aab \
  --config=config.xml \
  --mapping=mapping.txt \
  --merge-duplicated-res=true \
  --remove-bundle-metadata=true \
  --remove-root-files=true
```

**必选参数：**

| 参数 | 说明 |
|------|------|
| `--bundle` | 输入的 AAB 文件路径 |
| `--output` | 输出的混淆后 AAB 文件路径 |
| `--config` | 配置文件路径 |

**可选参数：**

| 参数 | 说明 |
|------|------|
| `--mapping` | 增量混淆的 mapping 文件路径 |
| `--merge-duplicated-res=true` | 去除重复资源 |
| `--remove-bundle-metadata=true` | 移除 BUNDLE-METADATA |
| `--remove-root-files=true` | 移除 root 目录文件 |
| `--disable-sign=true` | 禁用签名 |
| `--storeFile` | 签名 keystore 文件路径 |
| `--storePassword` | keystore 密码 |
| `--keyAlias` | key alias |
| `--keyPassword` | key 密码 |

#### 4. CI/CD 集成示例

```yaml
# GitHub Actions 示例
- name: Obfuscate AAB
  run: |
    java -jar AabResGuard.jar obfuscate-bundle \
      --bundle=app/build/outputs/bundle/release/app-release.aab \
      --output=app-obfuscated.aab \
      --config=config.xml \
      --merge-duplicated-res=true
```

## 推荐项目

- **[StringBlur](https://github.com/dawnuu/StringBlur)** — Android 字符串加密工具，防止字符串被轻易逆向提取。
