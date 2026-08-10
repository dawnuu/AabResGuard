# AabResGuard

[English](README.md) | [简体中文](README.zh-CN.md)

> This tool was provided by ByteDance's Douyin Android team.

AabResGuard obfuscates resources in Android App Bundle (`.aab`) files.

## Features

- **Resource deduplication:** merges duplicate resource files to reduce bundle size.
- **File filtering:** filters files in a bundle; currently supports the `META-INF/` and `lib/` paths.
- **Allowlist:** resource names matching the allowlist are not obfuscated.
- **Incremental obfuscation:** reuses a mapping file between builds.
- **String removal:** removes specified strings and translations from a line-separated list.

## Quick start

Use either the Gradle plugin, which integrates with the bundle task, or the command-line JAR for CI/CD and standalone workflows.

### Gradle plugin

Declare the plugin in `gradle/libs.versions.toml`:

```toml
[versions]
aabresguard = "0.1.21"

[plugins]
aabresguard = { id = "com.bytedance.android.aabResGuard", version.ref = "aabresguard" }
```

Configure the repository and plugin in the root project:

```kotlin
plugins {
  alias(libs.plugins.aabresguard) apply false
}

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
    maven("https://raw.githubusercontent.com/dawnuu/maven/refs/heads/main/gradle/")
  }
}
```

Then apply it in the application module:

```kotlin
plugins {
  alias(libs.plugins.aabresguard)
}
```

Groovy DSL uses the same `plugins` blocks, with `maven { url "..." }`. The legacy alternative is `classpath "com.bytedance.android:aabresguard-plugin:0.1.21"` followed by applying the plugin in the application module.

### Plugin configuration

```kotlin
configure<AabResGuardExtension> {
    enableObfuscate = true
    mergeDuplicatedRes = true
    enableFilterFiles = true
    enableFilterStrings = false
    removeBundleMetadata = true
    removeRootFiles = true

    mappingFile = file("mapping.txt").toPath()
    whiteList = setOf("*.R.raw.*", "*.R.drawable.icon")
    obfuscatedBundleFileName = "obfuscated-app.aab"
    filterList = setOf("*/arm64-v8a/*", "META-INF/*")
    unusedStringPath = file("unused.txt").toPath()
    languageWhiteList = listOf("en", "zh")
}
```

In Groovy DSL, use an `aabResGuard { ... }` block and lists such as `whiteList = ["*.R.raw.*"]`.

| Option | Description | Default |
| --- | --- | --- |
| `enableObfuscate` | Enables resource obfuscation. | `true` |
| `mergeDuplicatedRes` | Merges duplicate resources. | `false` |
| `enableFilterFiles` | Enables file filtering. | `false` |
| `enableFilterStrings` | Enables removal of unused strings. | `false` |
| `removeBundleMetadata` | Removes `BUNDLE-METADATA`. | `true` |
| `removeRootFiles` | Removes files in the bundle root. | `false` |
| `mappingFile` | Optional mapping file for incremental obfuscation. | — |
| `whiteList` | Additional resource-name allowlist; merged with the built-in rules. | — |
| `obfuscatedBundleFileName` | Output name; must end in `.aab`. | `{packageName}_{versionName}_{versionCode}.aab` |
| `filterList` | File-filter rules. | — |
| `unusedStringPath` | Path to the unused-string list. | Next to the mapping file |
| `languageWhiteList` | Languages to retain, such as `en`, `en-XX`, `zh`, and `zh-XX`. | — |

The built-in allowlist protects app icons, important third-party SDK configuration (including Firebase and Crashlytics), and Douyin-related resources. See the [Chinese README](README.zh-CN.md) for the complete built-in-rule list.

### Run the plugin

The plugin hooks into the `bundle` task. Run the normal bundle command (only Release variants are processed by default):

```sh
./gradlew clean :app:bundleRelease --stacktrace
```

The task prints the before/after bundle-size comparison. To obtain the output path through the Gradle Task API:

```kotlin
val aabResGuardPlugin = project.tasks.getByName("aabresguardRelease")
val bundlePath: Path = aabResGuardPlugin.obfuscatedBundlePath()
```

### Command-line tool

Download the latest `AabResGuard-x.x.x.jar` from [Releases](https://github.com/bytedance/AabResGuard/releases). Create an XML configuration file:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<resguard>
    <issue>
        <white-list>
            *.R.raw.*
            *.R.drawable.icon
        </white-list>
    </issue>
    <filter>
        <rule>*/arm64-v8a/*</rule>
        <rule>META-INF/*</rule>
    </filter>
    <string-filter>
        <unused-path>unused.txt</unused-path>
        <language-white-list>
            <language>en</language>
            <language>zh</language>
        </language-white-list>
    </string-filter>
</resguard>
```

Run obfuscation:

```sh
java -jar AabResGuard.jar obfuscate-bundle \
  --bundle=app.aab \
  --output=obfuscated-app.aab \
  --config=config.xml \
  --mapping=mapping.txt \
  --merge-duplicated-res=true \
  --remove-bundle-metadata=true \
  --remove-root-files=true
```

Required arguments are `--bundle`, `--output`, and `--config`. Optional arguments include `--mapping`, `--merge-duplicated-res=true`, `--remove-bundle-metadata=true`, `--remove-root-files=true`, `--disable-sign=true`, `--storeFile`, `--storePassword`, `--keyAlias`, and `--keyPassword`.

Example GitHub Actions step:

```yaml
- name: Obfuscate AAB
  run: |
    java -jar AabResGuard.jar obfuscate-bundle \
      --bundle=app/build/outputs/bundle/release/app-release.aab \
      --output=app-obfuscated.aab \
      --config=config.xml \
      --merge-duplicated-res=true
```

## Related project

- [StringBlur](https://github.com/dawnuu/StringBlur) — Android string encryption tool.
