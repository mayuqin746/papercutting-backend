package com.example.kpappercutting.config

import com.example.kpappercutting.security.AuthInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    private val authInterceptor: AuthInterceptor,
    private val uploadStorage: UploadStorageProperties
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(authInterceptor)
            .addPathPatterns("/api/**")
    }

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/images/**")
            .addResourceLocations(uploadStorage.resourceLocation(uploadStorage.imageDir))

        registry.addResourceHandler("/videos/**")
            .addResourceLocations(uploadStorage.resourceLocation(uploadStorage.videoDir))

        registry.addResourceHandler("/drafts/**")
            .addResourceLocations(uploadStorage.resourceLocation(uploadStorage.postDraftDir))

        registry.addResourceHandler("/user-drafts/**")
            .addResourceLocations(uploadStorage.resourceLocation(uploadStorage.userDraftDir))

        registry.addResourceHandler("/custom-patterns/**")
            .addResourceLocations(uploadStorage.resourceLocation(uploadStorage.customPatternDir))
    }
}
