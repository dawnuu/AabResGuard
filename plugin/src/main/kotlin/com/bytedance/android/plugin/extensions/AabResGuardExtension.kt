package com.bytedance.android.plugin.extensions

import java.nio.file.Path

/**
 * Created by YangJing on 2019/10/15 .
 * Email: yangjing.yeoh@bytedance.com
 */
open class AabResGuardExtension {
    var enableObfuscate: Boolean = true
    var mappingFile: Path? = null
    var whiteList: Set<String>? = HashSet()
    var bundleMetaDataWhiteList: Set<String>? = HashSet()
    var obfuscatedBundleFileName: String = ""
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
                "\twhiteList=${if (whiteList == null) null else whiteList}\n" +
                "\tbundleMetaDataWhiteList=${if (bundleMetaDataWhiteList == null) null else bundleMetaDataWhiteList}\n" +
                "\tobfuscatedBundleFileName=$obfuscatedBundleFileName\n" +
                "\tmergeDuplicatedRes=$mergeDuplicatedRes\n" +
                "\tenableFilterFiles=$enableFilterFiles\n" +
                "\tfilterList=${if (filterList == null) null else filterList}\n" +
                "\tenableFilterStrings=$enableFilterStrings\n" +
                "\tremoveBundleMetadata=$removeBundleMetadata\n" +
                "\tremoveRootFiles=$removeRootFiles\n" +
                "\tunusedStringPath=$unusedStringPath\n" +
                "\tlanguageWhiteoolean`List=${if (languageWhiteList == null) null else languageWhiteList}\n"
    }
}