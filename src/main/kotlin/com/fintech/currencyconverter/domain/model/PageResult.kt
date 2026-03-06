package com.fintech.currencyconverter.domain.model

data class PageResult<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
) {
    fun <R> map(transform: (T) -> R): PageResult<R> =
        PageResult(
            content = content.map(transform),
            page = page,
            size = size,
            totalElements = totalElements,
            totalPages = totalPages,
        )
}
