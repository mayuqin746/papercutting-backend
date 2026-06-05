package com.example.kpappercutting.controller

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

private const val MIN_ADMIN_PAGE_SIZE = 1
private const val MAX_ADMIN_PAGE_SIZE = 100

data class AdminPageResponse<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasMore: Boolean
)

fun adminPageRequest(page: Int, size: Int, sort: Sort): PageRequest {
    return PageRequest.of(
        page.coerceAtLeast(0),
        size.coerceIn(MIN_ADMIN_PAGE_SIZE, MAX_ADMIN_PAGE_SIZE),
        sort
    )
}

fun <T, R> Page<T>.toAdminPageResponse(transform: (T) -> R): AdminPageResponse<R> {
    return AdminPageResponse(
        items = content.map(transform),
        page = number,
        size = size,
        totalElements = totalElements,
        totalPages = totalPages,
        hasMore = number + 1 < totalPages
    )
}
