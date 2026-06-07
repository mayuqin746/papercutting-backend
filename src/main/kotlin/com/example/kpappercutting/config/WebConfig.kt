package com.example.kpappercutting.config

import com.example.kpappercutting.security.AuthInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    private val authInterceptor: AuthInterceptor
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(authInterceptor)
            .addPathPatterns("/api/**")
    }

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/images/**")
            .addResourceLocations("file:/home/ubuntu/kp_uploads/")
        registry.addResourceHandler("/videos/**")
            .addResourceLocations("file:/home/ubuntu/kp_videos/")
        registry.addResourceHandler("/drafts/**")
            .addResourceLocations("file:/home/ubuntu/kp_drafts/")
        registry.addResourceHandler("/user-drafts/**")
            .addResourceLocations("file:/home/ubuntu/kp_user_drafts/")
        registry.addResourceHandler("/custom-patterns/**")
            .addResourceLocations("file:/home/ubuntu/kp_custom_patterns/")
    }
}
