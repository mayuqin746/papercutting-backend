package com.example.kpappercutting.controller

import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/app")
class AppVersionController(
    @Value("\${app.update.latest-version-code:1}")
    private val latestVersionCode: Long,
    @Value("\${app.update.latest-version-name:1.0}")
    private val latestVersionName: String,
    @Value("\${app.update.apk-url:http://49.232.151.194/download/app-release.apk}")
    private val apkUrl: String,
    @Value("\${app.update.release-notes:修复已知问题，优化使用体验。}")
    private val releaseNotes: String,
    @Value("\${app.update.force-update:false}")
    private val forceUpdate: Boolean
) {
    @GetMapping("/latest-version")
    fun latestVersion(): AppVersionResponse {
        return AppVersionResponse(
            latestVersionCode = latestVersionCode,
            latestVersionName = latestVersionName,
            apkUrl = apkUrl,
            releaseNotes = releaseNotes,
            forceUpdate = forceUpdate
        )
    }
}

data class AppVersionResponse(
    val latestVersionCode: Long,
    val latestVersionName: String,
    val apkUrl: String,
    val releaseNotes: String,
    val forceUpdate: Boolean
)
