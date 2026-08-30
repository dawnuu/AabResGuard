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
    // 是否允许去除重复资源，默认 false
    mergeDuplicatedRes = true
    // 是否允许过滤文件，默认 false
    enableFilterFiles = true
    // 是否允许过滤文案，默认 false
    enableFilterStrings = false
    // 是否移除 BUNDLE-METADATA，默认 true
    removeBundleMetadata = true
    // 是否移除 root 目录，默认 false
    removeRootFiles = true
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
    // 默认文件名为: {packageName}_{versionName}_{versionCode}.aab
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
    // 是否允许去除重复资源，默认 false
    mergeDuplicatedRes = true
    // 是否允许过滤文件，默认 false
    enableFilterFiles = true
    // 是否允许过滤文案，默认 false
    enableFilterStrings = false
    // 是否移除 BUNDLE-METADATA，默认 true
    removeBundleMetadata = true
    // 是否移除 root 目录，默认 false
    removeRootFiles = true
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
    // 默认文件名为: {packageName}_{versionName}_{versionCode}.aab
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
