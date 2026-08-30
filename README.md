# AabResGuard

[English](README.md) | [简体中文](README.zh-CN.md)

[![Maven Central](https://img.shields.io/maven-central/v/io.github.dawnuu/aabresguard-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.dawnuu/aabresguard-plugin)

## Migration guide

> **Maven Central migration:** When migrating from the custom Maven repository to Maven Central, update the repository, plugin ID, and legacy dependency as shown below. Java/Kotlin package names in your source code do not need to change.
>
> | Item | Custom Maven setup | Maven Central setup |
> | --- | --- | --- |
> | Repository | Custom Maven repository | mavenCentral() |
> | Plugins DSL ID | com.bytedance.android.aabResGuard | io.github.dawnuu.aabresguard |
> | Legacy dependency | com.bytedance.android:aabresguard-plugin:0.1.x | io.github.dawnuu:aabresguard-plugin:1.0.1 |
> | Legacy apply ID | com.bytedance.android.aabResGuard | io.github.dawnuu.aabresguard |

## Integration guide

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
aabresguard = "1.0.1"

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
>     classpath "io.github.dawnuu:aabresguard-plugin:1.0.1"
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
>     classpath("io.github.dawnuu:aabresguard-plugin:1.0.1")
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
    // Generate random resource names; default: false
    useRandomName = false
    // Append one random byte to media files to change their MD5; default: false
    enableMutateMd5 = false
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
    // Keep matching BUNDLE-METADATA files when removeBundleMetadata is true
    bundleMetaDataWhiteList = [
        "com.example.metadata"
    ]

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

    // String filtering list; path is relative to the current working directory (optional)
    unusedStringPath = "unused.txt"

    // Retain en, en-xx, zh, zh-xx, and other selected languages (optional)
    languageWhiteList = ["en", "zh"]

    // VirusTotal upload is disabled by default; do not hard-code the API key
    enableVirusTotalUpload = false
    virusTotalApiKey = System.getenv("VIRUSTOTAL_API_KEY") ?: ""
}
```

**Kotlin DSL (`.kts`):**

```kotlin
configure<AabResGuardExtension> {
    // Enable resource obfuscation; default: true
    enableObfuscate = true
    // Generate random resource names; default: false
    useRandomName = false
    // Append one random byte to media files to change their MD5; default: false
    enableMutateMd5 = false
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
    // Keep matching BUNDLE-METADATA files when removeBundleMetadata is true
    bundleMetaDataWhiteList = setOf(
        "com.example.metadata"
    )

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

    // String filtering list; path is relative to the current working directory (optional)
    unusedStringPath = "unused.txt"

    // Retain en, en-xx, zh, zh-xx, and other selected languages (optional)
    languageWhiteList = setOf("en", "zh")

    // VirusTotal upload is disabled by default; do not hard-code the API key
    enableVirusTotalUpload = false
    virusTotalApiKey = System.getenv("VIRUSTOTAL_API_KEY") ?: ""
}
```

#### 1.3 Run obfuscation

The `aabResGuard plugin` is integrated into the `bundle` packaging workflow. Run the original bundle command to perform obfuscation; only Release variants are processed by default:

```cmd
./gradlew clean :app:bundleRelease --stacktrace
```

The task prints a before-and-after bundle-size comparison when obfuscation finishes.

#### 1.4 Get the obfuscated bundle path

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
    <issue id="whitelist" isactive="true">
        <path value="*.R.raw.*"/>
        <path value="*.R.drawable.icon"/>
    </issue>

    <!-- File filtering rules (optional) -->
    <filter isactive="true">
        <rule value="*/arm64-v8a/*"/>
        <rule value="META-INF/*"/>
    </filter>

    <!-- String filtering configuration (optional) -->
    <filter-str isactive="true">
        <path value="unused.txt"/>
        <language value="en"/>
        <language value="zh"/>
    </filter-str>
</resguard>
```

The XML parser recognizes only the `issue`, `filter`, and `filter-str` elements shown above. `useRandomName`, `enableMutateMd5`, VirusTotal upload, and the BUNDLE-METADATA allowlist are currently Gradle-plugin options and have no command-line flags.

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
