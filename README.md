# AabResGuard

[English](README.md) | [简体中文](README.zh-CN.md)

[![Maven Central](https://img.shields.io/maven-central/v/io.github.dawnuu/aabresguard-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.dawnuu/aabresguard-plugin)

> **Migration note for v1.0.0:** If you are upgrading from a pre-1.0.0 version, update the repository, plugin ID, and legacy dependency as shown below. Java/Kotlin package names in your source code do not need to change.
>
> | Item | Before v1.0.0 | v1.0.0 |
> | --- | --- | --- |
> | Repository | Custom Maven repository | mavenCentral() |
> | Plugins DSL ID | com.bytedance.android.aabResGuard | io.github.dawnuu.aabresguard |
> | Legacy dependency | com.bytedance.android:aabresguard-plugin:0.1.x | io.github.dawnuu:aabresguard-plugin:1.0.0 |
> | Legacy apply ID | com.bytedance.android.aabResGuard | io.github.dawnuu.aabresguard |

The current release is published to Maven Central. You can use the Gradle plugin directly with `io.github.dawnuu:aabresguard-plugin:1.0.0`.

## Maven Central Deployment

The project provides a `centralPortalUpload` task that creates and uploads the signed bundle to Maven Central. The default Deployment name is `aabresguard-1.0.0`; customize it with `-PcentralDeploymentName`.

```sh
./gradlew centralPortalUpload \
  -PcentralRelease=true \
  -PcentralDeploymentName="AabResGuard 1.0.0" \
  -PcentralPublishingType=USER_MANAGED
```

The task reads `mavenCentralUsername` and `mavenCentralPassword` from Gradle user properties, or `MAVEN_CENTRAL_USERNAME` and `MAVEN_CENTRAL_PASSWORD` from the environment. Use `-PcentralPublishingType=AUTOMATIC` when automatic publishing is desired.

> This tool was provided by ByteDance's Douyin Android team.

## Features

> A resource obfuscation tool for Android App Bundle files.

- **Resource deduplication:** merges duplicate resource files to reduce bundle size.
- **File filtering:** filters files in a `bundle`; currently supports the `META-INF/` and `lib/` paths.
- **Allowlist:** resource names matching the allowlist are not obfuscated.
- **Incremental obfuscation:** accepts a `mapping` file to support incremental obfuscation.
- **String removal:** accepts a line-separated string file to remove strings and translations.

## Quick start

AabResGuard provides two integration methods:

- **Gradle Plugin:** the recommended method; it integrates with the packaging workflow, so the original bundle command performs obfuscation.
- **Command-line tool:** suitable for CI/CD or standalone workflows through a JAR command.

---

### 1. Gradle Plugin

#### 1.1 Add the plugin

We recommend using the [Gradle plugins DSL](https://docs.gradle.org/current/userguide/plugins.html#sec:plugins_block).

Declare the version in `gradle/libs.versions.toml`:

```toml
[versions]
aabresguard = "1.0.0"

[plugins]
aabresguard = { id = "io.github.dawnuu.aabresguard", version.ref = "aabresguard" }
```

`libs.versions.toml` is shared by Groovy DSL and Kotlin DSL projects.

**Groovy DSL:**

Declare the plugin version and configure repositories in `build.gradle` for the root project:

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

Enable the plugin in `build.gradle` for the application module:

```gradle
plugins {
  alias(libs.plugins.aabresguard)
}
```

**Kotlin DSL (`.kts`):**

Declare the plugin version and configure repositories in `build.gradle.kts` for the root project:

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

Enable the plugin in `build.gradle.kts` for the application module:

```kotlin
plugins {
  alias(libs.plugins.aabresguard)
}
```

> You can also use the legacy `buildscript` method:
>
> **Groovy DSL:**
>
> Add the dependency in the root project:
>
> ```gradle
> buildscript {
>   repositories {
>     mavenCentral()
>     google()
>   }
>   dependencies {
>     classpath "io.github.dawnuu:aabresguard-plugin:1.0.0"
>   }
> }
> ```
>
> Apply the plugin in the application module:
>
> ```gradle
> apply plugin: "io.github.dawnuu.aabresguard"
> ```
>
> **Kotlin DSL (`.kts`):**
>
> Add the dependency in the root project:
>
> ```kotlin
> buildscript {
>   repositories {
>     mavenCentral()
>     google()
>   }
>   dependencies {
>     classpath("io.github.dawnuu:aabresguard-plugin:1.0.0")
>   }
> }
> ```
>
> Apply the plugin in the application module:
>
> ```kotlin
> apply(plugin = "io.github.dawnuu.aabresguard")
> ```

#### 1.2 Configuration options

Add an `aabResGuard` configuration block in `build.gradle` for the application module.

**Groovy DSL:**

```gradle
aabResGuard {
    // Enable resource obfuscation; default: true
    enableObfuscate = true
    // Remove duplicate resources; default: false
    mergeDuplicatedRes = true
    // Enable file filtering; default: false
    enableFilterFiles = true
    // Enable string filtering; default: false
    enableFilterStrings = false
    // Remove BUNDLE-METADATA; default: true
    removeBundleMetadata = true
    // Remove files in the root directory; default: false
    removeRootFiles = true

    // Mapping file for incremental obfuscation (optional)
    mappingFile = file("mapping.txt").toPath()

    // Custom allowlist rules are merged with the built-in allowlist
    whiteList = [
        "*.R.raw.*",
        "*.R.drawable.icon"
    ]

    // Obfuscated output name; it must end with .aab
    // Default: {packageName}_{versionName}_{versionCode}.aab
    obfuscatedBundleFileName = "duplicated-app.aab"

    // File filtering rules
    filterList = [
        "*/arm64-v8a/*",
        "META-INF/*"
    ]

    // String filtering list; defaults to the mapping file directory
    unusedStringPath = file("unused.txt").toPath()

    // Retain en, en-xx, zh, zh-xx, and other selected languages (optional)
    languageWhiteList = ["en", "zh"]
}
```

**Kotlin DSL (`.kts`):**

```kotlin
configure<AabResGuardExtension> {
    // Enable resource obfuscation; default: true
    enableObfuscate = true
    // Remove duplicate resources; default: false
    mergeDuplicatedRes = true
    // Enable file filtering; default: false
    enableFilterFiles = true
    // Enable string filtering; default: false
    enableFilterStrings = false
    // Remove BUNDLE-METADATA; default: true
    removeBundleMetadata = true
    // Remove files in the root directory; default: false
    removeRootFiles = true

    // Mapping file for incremental obfuscation (optional)
    mappingFile = file("mapping.txt").toPath()

    // Custom allowlist rules are merged with the built-in allowlist
    whiteList = setOf(
        "*.R.raw.*",
        "*.R.drawable.icon"
    )

    // Obfuscated output name; it must end with .aab
    // Default: {packageName}_{versionName}_{versionCode}.aab
    obfuscatedBundleFileName = "duplicated-app.aab"

    // File filtering rules
    filterList = setOf(
        "*/arm64-v8a/*",
        "META-INF/*"
    )

    // String filtering list; defaults to the mapping file directory
    unusedStringPath = file("unused.txt").toPath()

    // Retain en, en-xx, zh, zh-xx, and other selected languages (optional)
    languageWhiteList = listOf("en", "zh")
}
```

#### 1.3 Built-in allowlist

The plugin automatically applies the following built-in allowlist rules:

```kotlin
// Built-in allowlist; custom rules are merged with these rules
"*.R.mipmap.ic_*",                // App icons
"*.R.mipmap.logo*",               // Logo
"*.R.string.default_web_client_id",
"*.R.string.firebase_database_url",
"*.R.string.gcm_defaultSenderId",
"*.R.string.google_api_key",
"*.R.string.google_app_id",
"*.R.string.google_crash_reporting_api_key",
"*.R.string.google_storage_bucket",
"*.R.string.project_id",
"*.R.string.com.crashlytics.android.build_id",
"*.R.string.com.google.firebase.crashlytics.mapping_file_id",
"*.R.string.tt_*",                // Douyin-related resources
"*.R.layout.tt_*",
"*.R.drawable.tt_*",
"*.R.layout.notification_*",      // Notification layouts
"*.R.string.star_*",
"*.R.dimen.tt_*",
"*.R.integer.tt_*",
"*.R.anim.tt_*",
"*.R.xml.tt_*",
"*.R.color.tt_*",
"*.R.style.tt_*",
"*.R.raw.tt_*",
"*.R.mipmap.tt_*",
"*.R.menu.tt_*",
"*.R.attr.tt_*",
"*.R.style.Theme.Dialog.TT_*",
"*.R.style.quick_*",
"*.R.style.EditTextStyle*",
"*.R.id.tt_*"
```

> The built-in allowlist mainly covers **app icons**, **key third-party SDK configuration** such as Firebase and Crashlytics, and **Douyin-related resources**.

#### 1.4 Run obfuscation

The `aabResGuard plugin` is integrated into the `bundle` packaging workflow. Run the original bundle command to perform obfuscation; only Release variants are processed by default:

```cmd
./gradlew clean :app:bundleRelease --stacktrace
```

The task prints a before-and-after bundle-size comparison when obfuscation finishes.

#### 1.5 Get the obfuscated bundle path

Use the Gradle Task API to obtain the obfuscated bundle path.

**Groovy DSL:**

```groovy
def aabResGuardPlugin = project.tasks.getByName("aabresguardRelease")
Path bundlePath = aabResGuardPlugin.getObfuscatedBundlePath()
```

**Kotlin DSL (`.kts`):**

```kotlin
val aabResGuardPlugin = project.tasks.getByName("aabresguardRelease")
val bundlePath: Path = aabResGuardPlugin.obfuscatedBundlePath()
```

---

### 2. Command-line tool

#### 2.1 Download

Download the latest `AabResGuard-x.x.x.jar` from [Releases](https://github.com/dawnuu/AabResGuard/releases).

#### 2.2 Prepare the configuration file

The command-line tool uses an XML configuration file to define allowlist and filtering rules:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<resguard>
    <!-- Allowlist rules -->
    <issue>
        <white-list>
            *.R.raw.*
            *.R.drawable.icon
        </white-list>
    </issue>

    <!-- File filtering rules (optional) -->
    <filter>
        <rule>*/arm64-v8a/*</rule>
        <rule>META-INF/*</rule>
    </filter>

    <!-- String filtering configuration (optional) -->
    <string-filter>
        <unused-path>unused.txt</unused-path>
        <language-white-list>
            <language>en</language>
            <language>zh</language>
        </language-white-list>
    </string-filter>
</resguard>
```

#### 2.3 Run obfuscation

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

**Required arguments:**

| Argument | Description |
|------|------|
| `--bundle` | Input AAB file path |
| `--output` | Obfuscated output AAB file path |
| `--config` | Configuration file path |

**Optional arguments:**

| Argument | Description |
|------|------|
| `--mapping` | Mapping file path for incremental obfuscation |
| `--merge-duplicated-res=true` | Remove duplicate resources |
| `--remove-bundle-metadata=true` | Remove BUNDLE-METADATA |
| `--remove-root-files=true` | Remove files in the root directory |
| `--disable-sign=true` | Disable signing |
| `--storeFile` | Signing keystore file path |
| `--storePassword` | Keystore password |
| `--keyAlias` | Key alias |
| `--keyPassword` | Key password |

#### 2.4 CI/CD integration example

```yaml
# GitHub Actions example
- name: Obfuscate AAB
  run: |
    java -jar AabResGuard.jar obfuscate-bundle \
      --bundle=app/build/outputs/bundle/release/app-release.aab \
      --output=app-obfuscated.aab \
      --config=config.xml \
      --merge-duplicated-res=true
```

## Recommended project

- **[StringBlur](https://github.com/dawnuu/StringBlur)** — an Android string encryption tool that helps prevent strings from being easily extracted through reverse engineering.
