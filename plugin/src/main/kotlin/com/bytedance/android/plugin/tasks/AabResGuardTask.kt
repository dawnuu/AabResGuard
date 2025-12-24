package com.bytedance.android.plugin.tasks

import com.android.build.api.variant.ApplicationVariant
import com.bytedance.android.aabresguard.commands.ObfuscateBundleCommand
import com.bytedance.android.plugin.extensions.AabResGuardExtension
import com.bytedance.android.plugin.internal.getBundleFilePath
import com.bytedance.android.plugin.internal.getSigningConfig
import com.bytedance.android.plugin.model.SigningConfig
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.logging.text.StyledTextOutput.Style
import org.gradle.internal.logging.text.StyledTextOutputFactory
import java.io.File
import java.nio.file.Path
import javax.inject.Inject

open class AabResGuardTask @Inject constructor(outputFactory: StyledTextOutputFactory) :
    DefaultTask() {

    @get:Internal
    var variant: ApplicationVariant? = null

    @get:Internal
    lateinit var signingConfig: SigningConfig

    @get:Internal
    val aabResGuard: AabResGuardExtension =
        project.extensions.getByName("aabResGuard") as AabResGuardExtension

    private val out = outputFactory.create("AabResGuardTask")

    init {
        description = "Assemble resource proguard for bundle file"
        group = "bundle"
        outputs.upToDateWhen { false }
    }

    fun setVariantData(variant: ApplicationVariant) {
        this.variant = variant
    }

    @TaskAction
    fun execute() {
        val currentVariant = variant ?: throw RuntimeException("Variant info is missing")
        val variantName = currentVariant.name

        out.style(Style.Info).println(aabResGuard.toString())

        // 获取签名配置
        signingConfig = getSigningConfig(project, currentVariant)

        // 获取 Bundle 文件路径
        val bundlePath = getBundleFilePath(project, currentVariant)

        val applicationId = currentVariant.applicationId.get()
        val versionName =
            currentVariant.outputs.firstOrNull()?.versionName?.getOrElse("unspecified")
                ?: "unspecified"
        val versionCode = currentVariant.outputs.firstOrNull()?.versionCode?.getOrElse(0) ?: 0

        val aabName = aabResGuard.obfuscatedBundleFileName.ifBlank {
            "${applicationId}_${versionName}_${versionCode}.aab"
        }
        val obfuscatedBundlePath = File(bundlePath.toFile().parentFile, aabName).toPath()

        printSignConfiguration()
        printOutputFileLocation(obfuscatedBundlePath)

        prepareUnusedFile(variantName)

        val command = ObfuscateBundleCommand.builder()
            .setEnableObfuscate(aabResGuard.enableObfuscate)
            .setBundlePath(bundlePath)
            .setOutputPath(obfuscatedBundlePath)
            .setMergeDuplicatedResources(aabResGuard.mergeDuplicatedRes)
            .setWhiteList(aabResGuard.whiteList)
            .setBundleMetaDataWhiteList(aabResGuard.bundleMetaDataWhiteList)
            .setFilterFile(aabResGuard.enableFilterFiles)
            .setRemoveBundleMetadata(aabResGuard.removeBundleMetadata)
            .setRemoveRootFiles(aabResGuard.removeRootFiles)
            .setFileFilterRules(aabResGuard.filterList)
            .setRemoveStr(aabResGuard.enableFilterStrings)
            .setUnusedStrPath(aabResGuard.unusedStringPath)
            .setLanguageWhiteList(aabResGuard.languageWhiteList)
            .setUseRandomName(aabResGuard.useRandomName)
            .setEnableMutateMd5(aabResGuard.enableMutateMd5)

        if (aabResGuard.mappingFile != null) {
            command.setMappingPath(aabResGuard.mappingFile)
        }

        if (signingConfig.storeFile != null && signingConfig.storeFile!!.exists()) {
            command.setStoreFile(signingConfig.storeFile!!.toPath())
                .setKeyAlias(signingConfig.keyAlias)
                .setKeyPassword(signingConfig.keyPassword)
                .setStorePassword(signingConfig.storePassword)
        }
        command.build().execute()
    }

    private fun prepareUnusedFile(name: String) {
        val simpleName = name.replace("Release", "", ignoreCase = true)
        if (simpleName.isEmpty()) return
        val lowerName = simpleName.replaceFirstChar { it.lowercase() }
        val resourcePath = "${project.buildDir}/outputs/mapping/$lowerName/release/unused.txt"
        val usedFile = File(resourcePath)
        if (usedFile.exists()) {
            println("find unused.txt : ${usedFile.absolutePath}")
            if (aabResGuard.enableFilterStrings) {
                if (aabResGuard.unusedStringPath == null || aabResGuard.unusedStringPath!!.isBlank()) {
                    aabResGuard.unusedStringPath = usedFile.absolutePath
                    out.style(Style.Error).println("replace unused.txt!")
                }
            }
        }
    }

    private fun printSignConfiguration() {
        println("-------------- Sign configuration --------------")
        println("\tStoreFile:\t\t${signingConfig.storeFile}")
        println("--------------------------------------------------")
    }

    private fun printOutputFileLocation(path: Path) {
        println("-------------- Output configuration --------------")
        println("\tFolder:\t\t${path.parent}")
        println("\tFile:\t\t${path.fileName}")
        println("--------------------------------------------------")
    }
}
