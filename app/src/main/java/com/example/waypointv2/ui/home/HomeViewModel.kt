package com.example.waypointv2.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    companion object {
        private const val TAG = "HomeViewModel"
    }

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    private val _waypoints = MutableStateFlow<List<Waypoint>>(emptyList())
    val waypoints: StateFlow<List<Waypoint>> = _waypoints

    init {
        fetchWaypoints()
    }

    private fun fetchWaypoints() {
        val userId = auth.currentUser?.uid
        Log.d(TAG, "fetchWaypoints: userId = $userId")
        
        if (userId == null) {
            Log.w(TAG, "fetchWaypoints: No hay usuario autenticado")
            _waypoints.value = emptyList()
            return
        }

        viewModelScope.launch {
            Log.d(TAG, "fetchWaypoints: Iniciando listener de Firestore")
            
            // Escuchar cambios en la colección waypoints filtrada por userId
            db.collection("waypoints")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "fetchWaypoints: Error al escuchar cambios", error)
                        Log.e(TAG, "fetchWaypoints: Mensaje de error: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        Log.d(TAG, "fetchWaypoints: Recibidos ${snapshot.documents.size} documentos")
                        
                        val waypointsList = snapshot.documents.mapNotNull { doc ->
                            Log.d(TAG, "fetchWaypoints: Procesando documento ${doc.id}")
                            doc.toObject(Waypoint::class.java)?.copy(id = doc.id)
                        }
                        
                        Log.d(TAG, "fetchWaypoints: Total waypoints procesados: ${waypointsList.size}")
                        _waypoints.value = waypointsList
                    } else {
                        Log.w(TAG, "fetchWaypoints: Snapshot es null")
                    }
                }
        }
    }
}
