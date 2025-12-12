package com.example.waypointv2.ui.home

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Waypoint(
    val id: String = "",
    val userId: String = "",
    val photoUrl: String = "",
    val frontPhotoUrl: String = "",
    val audioUrl: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val locationName: String = "",
    val title: String = "",
    val tags: List<String> = emptyList(),
    @ServerTimestamp
    val timestamp: Date? = null
)
