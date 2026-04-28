package com.example.mycar.data.model

data class MaintenanceRecord(
    val id: String = "",
    val carId: String = "",
    val date: String = "",
    val mileage: String = "",
    val description: String = "",
    val cost: String = ""
)