package com.fintech.currencyconverter.domain.model

/**
 * Framework-free pagination wrapper for domain layer.
 * Replaces Spring Data's Page<T> to enforce Zero-Framework Policy.
 */
data class PageResult<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
) {
    companion object {
        fun <T> of(content: List<T>, page: Int, size: Int, totalElements: Long): PageResult<T> {
            val totalPages = if (size == 0) 0 else ((totalElements + size - 1) / size).toInt()
            return PageResult(content, page, size, totalElements, totalPages)
        }
    }
}

