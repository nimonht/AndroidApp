
package com.example.androidapp.ui.screens.search

/**
 * Data class đại diện cho dữ liệu của một bài kiểm tra hiển thị trên UI.
 * Phục vụ riêng cho tầng UI, tách biệt với tầng Domain.
 */
data class QuizCardDraft(
    val id: String,
    val title: String,
    val authorName: String,
    val questionCount: Int,
    val attemptCount: Int,
    val coverImageUrl: String? = null,
    val tags: List<String> = emptyList()
)