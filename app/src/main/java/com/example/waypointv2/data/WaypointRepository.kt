package com.example.waypointv2.data

import android.net.Uri
import android.util.Log
import com.example.waypointv2.ui.home.Waypoint
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class WaypointRepository {
    
    companion object {
        private const val TAG = "WaypointRepository"
    }
    
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    
    /**
     * Obtiene el ID del usuario autenticado
     */
    fun getCurrentUserId(): String {
        val userId = auth.currentUser?.uid
        Log.d(TAG, "getCurrentUserId: userId = $userId")
        return userId ?: throw IllegalStateException("Usuario no autenticado")
    }
    
    /**
     * Sube una foto a Firebase Storage
     * @param photoUri URI local de la foto
     * @return Result con la URL de descarga o error
     */
    suspend fun uploadPhoto(photoUri: Uri): Result<String> {
        return try {
            Log.d(TAG, "uploadPhoto: Iniciando subida de foto")
            val userId = getCurrentUserId()
            val timestamp = System.currentTimeMillis()
            val fileName = "photo_${timestamp}.jpg"
            
            Log.d(TAG, "uploadPhoto: userId=$userId, fileName=$fileName")
            
            val storageRef = storage.reference
                .child("photos")
                .child(userId)
                .child(fileName)
            
            Log.d(TAG, "uploadPhoto: Subiendo archivo desde URI: $photoUri")
            
            // Agregar listener de progreso
            val uploadTask = storageRef.putFile(photoUri)
            uploadTask.addOnProgressListener { taskSnapshot ->
                val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount)
                Log.d(TAG, "uploadPhoto: Progreso: ${progress.toInt()}%")
            }
            
            // Esperar a que termine la subida
            uploadTask.await()
            
            Log.d(TAG, "uploadPhoto: Archivo subido, obteniendo URL de descarga")
            val downloadUrl = storageRef.downloadUrl.await()
            
            Log.d(TAG, "uploadPhoto: Éxito! URL: $downloadUrl")
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Log.e(TAG, "uploadPhoto: Error al subir foto", e)
            Log.e(TAG, "uploadPhoto: Tipo de error: ${e.javaClass.simpleName}")
            Log.e(TAG, "uploadPhoto: Mensaje: ${e.message}")
            Result.failure(Exception("Error al subir foto: ${e.message}", e))
        }
    }
    
    /**
     * Sube un audio a Firebase Storage
     * @param audioUri URI local del archivo de audio
     * @return Result con la URL de descarga o error
     */
    suspend fun uploadAudio(audioUri: Uri): Result<String> {
        return try {
            val userId = getCurrentUserId()
            val timestamp = System.currentTimeMillis()
            val fileName = "audio_${timestamp}.m4a"
            val storageRef = storage.reference
                .child("audios")
                .child(userId)
                .child(fileName)
            
            val uploadTask = storageRef.putFile(audioUri).await()
            val downloadUrl = storageRef.downloadUrl.await()
            
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Guarda un waypoint en Firestore
     * @param waypoint Objeto waypoint a guardar
     * @return Result con el ID del documento o error
     */
    suspend fun saveWaypoint(waypoint: Waypoint): Result<String> {
        return try {
            val waypointData = hashMapOf(
                "userId" to waypoint.userId,
                "timestamp" to waypoint.timestamp,
                "latitude" to waypoint.latitude,
                "longitude" to waypoint.longitude,
                "photoUrl" to waypoint.photoUrl,
                "audioUrl" to waypoint.audioUrl,
                "locationName" to waypoint.locationName
            )
            
            val documentRef = firestore.collection("waypoints")
                .add(waypointData)
                .await()
            
            Result.success(documentRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Elimina archivos de Storage si falla el guardado
     */
    suspend fun deleteUploadedFiles(photoUrl: String?, audioUrl: String?) {
        try {
            photoUrl?.let { 
                storage.getReferenceFromUrl(it).delete().await()
            }
            audioUrl?.let { 
                storage.getReferenceFromUrl(it).delete().await()
            }
        } catch (e: Exception) {
            // Ignorar errores de limpieza
        }
    }
}
