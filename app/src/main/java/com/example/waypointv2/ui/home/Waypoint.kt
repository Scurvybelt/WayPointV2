package com.example.waypointv2.ui.home

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Waypoint(
    val id: String = "",
    val title: String = "Waypoint", // Título por defecto
    val photoUrl: String? = null,
    val audioUrl: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    @ServerTimestamp
    val createdAt: Date? = null,
    val userId: String = ""
)
