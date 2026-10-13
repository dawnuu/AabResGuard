package com.bytedance.android.plugin.internal

import org.gradle.api.GradleException

private val placeholderRegex = Regex("""\$?\{([^}]*)}""")
private val illegalFileNameChars = Regex("""[\\/:*?"<>|\s]""")

/**
 * Expands the `{name}` / `${name}` placeholders of the obfuscatedBundleFileName template.
 * The form without `$` avoids Groovy's own string interpolation in double-quoted strings.
 * Supported: applicationId, versionName, versionCode, variantName, buildTime.
 * Unknown placeholders and names that are not a plain file name fail the build.
 */
internal fun resolveBundleFileName(template: String, values: Map<String, String>): String {
    val name = placeholderRegex.replace(template) { match ->
        val key = match.groupValues[1]
        val value = values[key]
            ?: throw GradleException(
                "Unknown placeholder {$key} in obfuscatedBundleFileName. Supported: " +
                    values.keys.joinToString(", ") { "{$it}" }
            )
        value.replace(illegalFileNameChars, "_")
    }
    if (name.isBlank() || name.contains('/') || name.contains('\\') || name == "." || name == "..") {
        throw GradleException("obfuscatedBundleFileName must be a plain file name, but resolved to: '$name'")
    }
    return name
}
