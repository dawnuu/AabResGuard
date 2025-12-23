package com.bytedance.android.plugin

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.ApplicationVariant
import com.bytedance.android.plugin.extensions.AabResGuardExtension
import com.bytedance.android.plugin.tasks.AabResGuardTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

class AabResGuardPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        checkApplicationPlugin(project)
        project.extensions.create("aabResGuard", AabResGuardExtension::class.java)

        // 使用新的 AndroidComponentsExtension，这是 AGP 7/8/9 通用的 API
        val androidComponents = project.extensions.findByType(ApplicationAndroidComponentsExtension::class.java)
        if (androidComponents != null) {
            androidComponents.onVariants { variant ->
                // 只处理 Release 变体（或根据需求调整）
                if (variant.name.contains("Release", ignoreCase = true)) {
                    createAabResGuardTask(project, variant)
                }
            }
        } else {
            throw GradleException("Android Application Components Extension not found. Please ensure 'com.android.application' plugin is applied.")
        }
    }

    private fun createAabResGuardTask(project: Project, variant: ApplicationVariant) {
        val variantName = variant.name.replaceFirstChar { it.uppercase() }
        val aabResGuardTaskName = "aabresguard$variantName"
        
        // 使用 register 代替 create 以利用 Configuration Avoidance API
        val aabResGuardTask = project.tasks.register(aabResGuardTaskName, AabResGuardTask::class.java) { task ->
            task.setVariantData(variant)
        }

        // 建立任务依赖关系
        val bundleTaskName = "bundle$variantName"
        project.tasks.configureEach { task ->
            if (task.name == bundleTaskName) {
                task.dependsOn(aabResGuardTask)
            }
        }
        
        // 依赖于打包 AAB 的任务
        val bundlePackageTaskName = "package${variantName}Bundle"
        aabResGuardTask.configure { task ->
            project.tasks.findByName(bundlePackageTaskName)?.let { packageTask ->
                task.dependsOn(packageTask)
            }
            
            // 依赖于签名任务
            val signBundleTaskName = "sign${variantName}Bundle"
            project.tasks.findByName(signBundleTaskName)?.let { signTask ->
                task.dependsOn(signTask)
            }
        }
    }

    private fun checkApplicationPlugin(project: Project) {
        if (!project.plugins.hasPlugin("com.android.application")) {
            throw GradleException("Android Application plugin required")
        }
    }
}
