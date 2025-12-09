package com.example.waypointv2.ui.waypoint

import android.app.Application
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.waypointv2.data.WaypointRepository
import com.example.waypointv2.ui.home.Waypoint
import com.example.waypointv2.utils.AudioRecorder
import com.example.waypointv2.utils.LocationHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.Date

/**
 * Estados del flujo de captura
 */
sealed class CaptureState {
    object Idle : CaptureState()
    object CapturingPhoto : CaptureState()
    object RecordingAudio : CaptureState()
    object ProcessingLocation : CaptureState()
    data class UploadingData(val progress: String) : CaptureState()
    object Success : CaptureState()
    data class Error(val message: String) : CaptureState()
}

class CreateWaypointViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = WaypointRepository()
    private val audioRecorder = AudioRecorder(application)
    private val locationHandler = LocationHandler(application)
    
    // Estado del flujo de captura
    private val _captureState = MutableStateFlow<CaptureState>(CaptureState.Idle)
    val captureState = _captureState.asStateFlow()
    
    // URI de la foto capturada
    private val _photoUri = MutableStateFlow<Uri?>(null)
    val photoUri = _photoUri.asStateFlow()
    
    // Archivo de audio grabado
    private var audioFile: File? = null
    
    // Duración de la grabación en segundos
    private val _recordingDuration = MutableStateFlow(0L)
    val recordingDuration = _recordingDuration.asStateFlow()
    
    // Job para actualizar el contador de duración
    private var durationJob: Job? = null
    
    // Mensaje de error
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()
    
    /**
     * Inicia el flujo de captura (se llama cuando se abre la pantalla)
     */
    fun startCaptureFlow() {
        _captureState.value = CaptureState.CapturingPhoto
    }
    
    /**
     * Callback cuando se toma una foto
     */
    fun onPhotoTaken(uri: Uri) {
        _photoUri.value = uri
        // Transición automática a grabación de audio
        _captureState.value = CaptureState.RecordingAudio
    }
    
    /**
     * Callback cuando se cancela la foto
     */
    fun onPhotoCancelled() {
        cancelCapture()
    }
    
    /**
     * Inicia la grabación de audio
     */
    fun startAudioRecording() {
        val file = audioRecorder.startRecording()
        if (file != null) {
            audioFile = file
            startDurationCounter()
        } else {
            _captureState.value = CaptureState.Error("Error al iniciar la grabación de audio")
        }
    }
    
    /**
     * Detiene la grabación de audio y procede con la ubicación y subida
     */
    fun stopAudioRecording() {
        durationJob?.cancel()
        val recordedFile = audioRecorder.stopRecording()
        
        if (recordedFile != null && recordedFile.exists()) {
            audioFile = recordedFile
            // Proceder con ubicación y subida
            processLocationAndUpload()
        } else {
            _captureState.value = CaptureState.Error("Error al detener la grabación de audio")
        }
    }
    
    /**
     * Inicia el contador de duración de grabación
     */
    private fun startDurationCounter() {
        durationJob = viewModelScope.launch {
            while (isActive && audioRecorder.isRecording()) {
                _recordingDuration.value = audioRecorder.getRecordingDuration() / 1000
                delay(100)
            }
        }
    }
    
    /**
     * Procesa la ubicación y sube todos los datos
     */
    private fun processLocationAndUpload() {
        viewModelScope.launch {
            try {
                // Paso 1: Obtener ubicación
                _captureState.value = CaptureState.ProcessingLocation
                
                val locationResult = locationHandler.getCurrentLocation()
                if (locationResult.isFailure) {
                    _captureState.value = CaptureState.Error(
                        locationResult.exceptionOrNull()?.message ?: "Error al obtener ubicación"
                    )
                    return@launch
                }
                
                val location = locationResult.getOrThrow()
                val locationName = locationHandler.getLocationName(location.latitude, location.longitude)
                
                // Paso 2: Subir foto
                _captureState.value = CaptureState.UploadingData("Subiendo foto...")
                
                val photoUri = _photoUri.value
                if (photoUri == null) {
                    _captureState.value = CaptureState.Error("No se encontró la foto")
                    return@launch
                }
                
                val photoUrlResult = repository.uploadPhoto(photoUri)
                if (photoUrlResult.isFailure) {
                    _captureState.value = CaptureState.Error(
                        "Error al subir la foto: ${photoUrlResult.exceptionOrNull()?.message}"
                    )
                    return@launch
                }
                
                val photoUrl = photoUrlResult.getOrThrow()
                
                // Paso 3: Subir audio
                _captureState.value = CaptureState.UploadingData("Subiendo audio...")
                
                val audioFileLocal = audioFile
                if (audioFileLocal == null || !audioFileLocal.exists()) {
                    _captureState.value = CaptureState.Error("No se encontró el archivo de audio")
                    // Limpiar foto subida
                    repository.deleteUploadedFiles(photoUrl, null)
                    return@launch
                }
                
                val audioUrlResult = repository.uploadAudio(audioFileLocal.toUri())
                if (audioUrlResult.isFailure) {
                    _captureState.value = CaptureState.Error(
                        "Error al subir el audio: ${audioUrlResult.exceptionOrNull()?.message}"
                    )
                    // Limpiar foto subida
                    repository.deleteUploadedFiles(photoUrl, null)
                    return@launch
                }
                
                val audioUrl = audioUrlResult.getOrThrow()
                
                // Paso 4: Guardar waypoint en Firestore
                _captureState.value = CaptureState.UploadingData("Guardando waypoint...")
                
                val waypoint = Waypoint(
                    userId = repository.getCurrentUserId(),
                    photoUrl = photoUrl,
                    audioUrl = audioUrl,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    locationName = locationName,
                    timestamp = Date()
                )
                
                val saveResult = repository.saveWaypoint(waypoint)
                if (saveResult.isFailure) {
                    _captureState.value = CaptureState.Error(
                        "Error al guardar el waypoint: ${saveResult.exceptionOrNull()?.message}"
                    )
                    // Limpiar archivos subidos
                    repository.deleteUploadedFiles(photoUrl, audioUrl)
                    return@launch
                }
                
                // ¡Éxito!
                _captureState.value = CaptureState.Success
                
                // Limpiar archivos locales
                cleanupLocalFiles()
                
            } catch (e: Exception) {
                _captureState.value = CaptureState.Error(
                    "Error inesperado: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Reintenta la subida después de un error
     */
    fun retryUpload() {
        if (_photoUri.value != null && audioFile != null) {
            processLocationAndUpload()
        } else {
            _captureState.value = CaptureState.Error("No hay datos para reintentar")
        }
    }
    
    /**
     * Cancela el flujo de captura
     */
    fun cancelCapture() {
        durationJob?.cancel()
        audioRecorder.cancelRecording()
        cleanupLocalFiles()
        _captureState.value = CaptureState.Idle
        _photoUri.value = null
        _recordingDuration.value = 0
        audioFile = null
    }
    
    /**
     * Limpia archivos locales temporales
     */
    private fun cleanupLocalFiles() {
        try {
            audioFile?.delete()
            audioFile = null
        } catch (e: Exception) {
            // Ignorar errores de limpieza
        }
    }
    
    /**
     * Limpieza al destruir el ViewModel
     */
    override fun onCleared() {
        super.onCleared()
        audioRecorder.cleanup()
        cleanupLocalFiles()
    }
}
