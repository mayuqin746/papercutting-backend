package com.example.kpappercutting.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController


@RestController
class TestController {
    @GetMapping("/")
    fun index(): String {
        return "backend is running"
    }

    @GetMapping("/test")
    fun test(): String {
        return "test ok"
    }
}