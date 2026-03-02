package com.example.androidapp.data.remote.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class ShareCodeDto(
    @DocumentId val code: String = "",
    val quizId: String = "",
    val expiresAt: Timestamp? = null
)