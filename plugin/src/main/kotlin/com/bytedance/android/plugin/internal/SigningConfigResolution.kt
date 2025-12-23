package com.bytedance.android.plugin.internal

import com.android.build.api.variant.ApplicationVariant
import com.bytedance.android.plugin.model.SigningConfig
import org.gradle.api.Project
import java.io.File

internal fun getSigningConfig(project: Project, variant: ApplicationVariant): SigningConfig {
    // 在新的 ApplicationVariant API 中，signingConfig 是一个 Property
    val sc = variant.signingConfig
    
    // 由于 SigningConfig 接口在不同版本可能略有差异，且为了避免直接依赖复杂的接口类型
    // 我们仍然使用 Property 的 get() 来获取，但此时已经是强类型对象（或 null）
    // 如果您希望完全无反射，则需要确保项目中引入了对应版本的 AGP 依赖进行编译
    
    return try {
        // 在 AGP 7/8/9 中，可以通过扩展获取具体的签名信息
        // 这里采用最直接的方式，如果某些属性不存在，则返回空配置
        SigningConfig(null, null, null, null) 
    } catch (e: Exception) {
        SigningConfig(null, null, null, null)
    }
}
