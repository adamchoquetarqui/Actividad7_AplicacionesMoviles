package com.example.actividad7.data

data class Appointment(
    val id: String = "",
    val userId: String = "",
    val date: Long = 0,
    val time: String = "",
    val reason: String = "",
    val status: AppointmentStatus = AppointmentStatus.SCHEDULED,
    val createdAt: Long = System.currentTimeMillis()
)

enum class AppointmentStatus {
    SCHEDULED,
    COMPLETED,
    CANCELLED
}

