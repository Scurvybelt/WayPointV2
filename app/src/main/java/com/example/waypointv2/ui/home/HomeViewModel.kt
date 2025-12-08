package com.example.waypointv2.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.waypointv2.ui.home.Waypoint
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    private val _waypoints = MutableStateFlow<List<Waypoint>>(emptyList())
    val waypoints: StateFlow<List<Waypoint>> = _waypoints

    init {
        fetchWaypoints()
    }

    private fun fetchWaypoints() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            // No hay usuario, no se pueden cargar waypoints
            _waypoints.value = emptyList()
            return
        }

        viewModelScope.launch {
            // He añadido el uid del usuario en la colección para asegurar que solo lee sus datos
            db.collection("users").document(userId).collection("waypoints")
                .orderBy("createdAt")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        // Manejar el error
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        // He corregido la forma de convertir los datos de Firestore
                        _waypoints.value = snapshot.toObjects(Waypoint::class.java)
                    }
                }
        }
    }
}
