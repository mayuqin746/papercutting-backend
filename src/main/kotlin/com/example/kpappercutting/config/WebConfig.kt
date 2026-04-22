package com.example.kpappercutting.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.io.File

@Configuration
class WebConfig : WebMvcConfigurer {
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        // 让服务器去 C:/kp_uploads 下找图片
        registry.addResourceHandler("/images/**")
            .addResourceLocations("file:D:/kp_uploads/")
    }
}