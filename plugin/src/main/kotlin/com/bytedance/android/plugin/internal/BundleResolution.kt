package com.bytedance.android.plugin.internal

import com.android.build.api.variant.ApplicationVariant
import org.gradle.api.Project
import java.io.File
import java.nio.file.Path

internal fun getBundleFilePath(project: Project, variant: ApplicationVariant): Path {
    val variantName = variant.name.replaceFirstChar { it.uppercase() }
    
    // 优先从 sign 任务获取，这是最准确的最终生成路径
    val signTask = project.tasks.findByName("sign${variantName}Bundle")
    if (signTask != null) {
        val propertyName = if (signTask.hasProperty("bundleFile")) "bundleFile" else "finalBundleFile"
        val bundleFile = signTask.property(propertyName)
        if (bundleFile != null && bundleFile::class.java.name.contains("Property")) {
            val regularFile = bundleFile::class.java.getMethod("get").invoke(bundleFile)
            return (regularFile::class.java.getMethod("getAsFile").invoke(regularFile) as File).toPath()
        }
    }
    
    // 备选：从 package 任务获取
    val packageTask = project.tasks.findByName("package${variantName}Bundle")
    if (packageTask != null) {
        val bundleFile = packageTask.property("bundleFile")
        if (bundleFile != null && bundleFile::class.java.name.contains("Property")) {
            val regularFile = bundleFile::class.java.getMethod("get").invoke(bundleFile)
            return (regularFile::class.java.getMethod("getAsFile").invoke(regularFile) as File).toPath()
        }
    }

    // 默认兜底路径
    return File(project.buildDir, "outputs/bundle/${variant.name}/${project.name}-${variant.name}.aab").toPath()
}
