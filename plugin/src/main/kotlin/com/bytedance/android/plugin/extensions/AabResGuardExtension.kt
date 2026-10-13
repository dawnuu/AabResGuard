package com.bytedance.android.plugin.extensions

import java.nio.file.Path

/**
 * Created by YangJing on 2019/10/15 .
 * Email: yangjing.yeoh@bytedance.com
 */
open class AabResGuardExtension {
    var enableObfuscate: Boolean = true
    var mappingFile: Path? = null
    var useRandomName: Boolean = false
    var enableCompressImages: Boolean = false
    // Empty means use the bundled native tools; set only to override them.
    var imageCompressionToolsDir: String = ""
    // Null uses the root project's .gradle/aabresguard/image-cache directory.
    var imageCompressionCacheDir: Path? = null

    // MD5 mutation config
    var enableMutateMd5: Boolean = false

    // false: fail the build when no usable signing config is found.
    // true: fall back to the debug keystore (only for testing, never for release)
    var allowDebugSignFallback: Boolean = false
    // Seed for the deterministic mutation; change it to get a different (but again stable) result
    var mutateMd5Seed: String = ""

    // VirusTotal upload config
    var enableVirusTotalUpload: Boolean = false
    var virusTotalApiKey: String = ""

    private val defaultWhiteList = setOf(
        "*.R.mipmap.ic_*",
        "*.R.mipmap.logo*",
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
        "*.R.string.tt_*",
        "*.R.layout.tt_*",
        "*.R.drawable.tt_*",
        "*.R.layout.notification_*",
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
    )

    var whiteList: Set<String>? = HashSet(defaultWhiteList)
        set(value) {
            field = if (value != null) {
                HashSet(defaultWhiteList + value)
            } else {
                HashSet(defaultWhiteList)
            }
        }

    var bundleMetaDataWhiteList: Set<String>? = HashSet()
    var obfuscatedBundleFileName: String = ""

    // Custom variables usable in obfuscatedBundleFileName as {name}; they override built-in ones
    var fileNameVariables: Map<String, String> = emptyMap()
    var mergeDuplicatedRes: Boolean = false
    var enableFilterFiles: Boolean = false
    var filterList: Set<String>? = HashSet()
    var removeBundleMetadata: Boolean = true
    var removeRootFiles: Boolean = false
    var enableFilterStrings: Boolean = false
    var unusedStringPath: String? = ""
    var languageWhiteList: Set<String>? = HashSet()

    override fun toString(): String {
        return "-------------- AabResGuardExtension --------------\n" +
                "\tenableObfuscate=$enableObfuscate\n" +
                "\tmappingFile=$mappingFile\n" +
                "\tuseRandomName=$useRandomName\n" +
                "\tenableCompressImages=$enableCompressImages\n" +
                "\timageCompressionToolsDir=$imageCompressionToolsDir\n" +
                "\timageCompressionCacheDir=$imageCompressionCacheDir\n" +
                "\tenableMutateMd5=$enableMutateMd5\n" +
                "\tallowDebugSignFallback=$allowDebugSignFallback\n" +
                "\tmutateMd5Seed=$mutateMd5Seed\n" +
                "\tenableVirusTotalUpload=$enableVirusTotalUpload\n" +
                "\twhiteList=${if (whiteList == null) null else whiteList}\n" +
                "\tobfuscatedBundleFileName=$obfuscatedBundleFileName\n" +
                "\tfileNameVariables=${fileNameVariables.keys}\n" +
                "\tmergeDuplicatedRes=$mergeDuplicatedRes\n" +
                "\tenableFilterFiles=$enableFilterFiles\n" +
                "\tenableFilterStrings=$enableFilterStrings\n" +
                "\tunusedStringPath=$unusedStringPath\n" +
                "\tlanguageWhiteList=${if (languageWhiteList == null) null else languageWhiteList}\n"
    }
}
