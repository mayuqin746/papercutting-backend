package com.example.kpappercutting.security

import jakarta.servlet.http.HttpServletRequest

fun HttpServletRequest.currentUserId(): Long {
    return getAttribute(AUTH_USER_ID_ATTRIBUTE) as? Long
        ?: throw IllegalStateException("Missing authenticated user")
}

fun HttpServletRequest.currentUserIdOrNull(): Long? {
    return getAttribute(AUTH_USER_ID_ATTRIBUTE) as? Long
}
