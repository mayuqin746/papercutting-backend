package com.example.kpappercutting.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig : WebMvcConfigurer {

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/images/**")
            .addResourceLocations("file:/home/ubuntu/kp_uploads/")
        registry.addResourceHandler("/drafts/**")
            .addResourceLocations("file:/home/ubuntu/kp_drafts/")
        registry.addResourceHandler("/user-drafts/**")
            .addResourceLocations("file:/home/ubuntu/kp_user_drafts/")
    }
}
