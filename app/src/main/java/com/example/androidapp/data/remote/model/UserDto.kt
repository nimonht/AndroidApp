package com.example.androidapp.data.remote.model

import com.google.firebase.firestore.DocumentId

data class UserDto(
    @DocumentId val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String? = null
)