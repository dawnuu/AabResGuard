package com.bytedance.android.plugin.internal

import com.android.build.api.variant.ApplicationVariant
import com.bytedance.android.plugin.model.SigningConfig
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import java.io.File

internal fun getSigningConfig(project: Project, variant: ApplicationVariant): SigningConfig {
    return try {
        // 1. 获取 variant.signingConfig 对象 (通常是一个 Provider)
        val scProvider = variant.signingConfig
        val sc = if (scProvider is Provider<*>) scProvider.orNull else scProvider

        if (sc == null) {
            return SigningConfig(null, null, null, null)
        }

        // 2. 使用反射获取属性
        val storeFile = invokeMethod(sc, "getStoreFile")?.let { result ->
            val fileObj = if (result is Provider<*>) result.orNull else result
            when {
                fileObj == null -> null
                fileObj is File -> fileObj
                else -> {
                    // 尝试调用 getAsFile() (针对 RegularFile 或 Directory)
                    try {
                        fileObj.javaClass.getMethod("getAsFile").invoke(fileObj) as? File
                    } catch (e: Exception) {
                        fileObj as? File
                    }
                }
            }
        }

        val storePassword = unwrapString(invokeMethod(sc, "getStorePassword"))
        val keyAlias = unwrapString(invokeMethod(sc, "getKeyAlias"))
        val keyPassword = unwrapString(invokeMethod(sc, "getKeyPassword"))

        SigningConfig(storeFile, storePassword, keyAlias, keyPassword)
    } catch (e: Exception) {
        SigningConfig(null, null, null, null)
    }
}

private fun invokeMethod(obj: Any, methodName: String): Any? {
    return try {
        val method = obj.javaClass.methods.find { it.name == methodName && it.parameterCount == 0 }
        method?.invoke(obj)
    } catch (e: Exception) {
        null
    }
}

private fun unwrapString(result: Any?): String? {
    return if (result is Provider<*>) result.orNull as? String else result as? String
}
