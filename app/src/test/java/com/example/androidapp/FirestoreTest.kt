package com.example.androidapp

import com.example.androidapp.data.remote.model.QuestionPoolItemDto
import com.google.firebase.firestore.FirebaseFirestore
import org.junit.Test
import org.junit.Assert.*

class FirestoreTest {
    @Test
    fun testDtoField() {
        val dto = QuestionPoolItemDto()
        println(dto)
    }
}
