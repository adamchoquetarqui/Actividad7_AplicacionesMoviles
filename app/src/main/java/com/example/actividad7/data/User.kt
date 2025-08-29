package com.example.actividad7.data

data class User(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

