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
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.Date

/**
 * Estados del flujo de captura
 */
sealed class CaptureState {
    object Idle : CaptureState()
    object CapturingBackPhoto : CaptureState()
    object CapturingFrontPhoto : CaptureState()
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
    
    // URI de las fotos capturadas
    private val _backPhotoUri = MutableStateFlow<Uri?>(null)
    val backPhotoUri = _backPhotoUri.asStateFlow()
    
    private val _frontPhotoUri = MutableStateFlow<Uri?>(null)
    val frontPhotoUri = _frontPhotoUri.asStateFlow()
    
    // Archivo de audio grabado
    private var audioFile: File? = null
    
    // Duración de la grabación en segundos
    private val _recordingDuration = MutableStateFlow(0L)
    val recordingDuration = _recordingDuration.asStateFlow()
    
    // Título de la nota
    private val _title = MutableStateFlow("")
    val title = _title.asStateFlow()
    
    // Etiquetas del waypoint
    private val _tags = MutableStateFlow<List<String>>(emptyList())
    val tags = _tags.asStateFlow()
    
    // Etiquetas disponibles (de waypoints anteriores)
    private val _availableTags = MutableStateFlow<List<String>>(emptyList())
    val availableTags = _availableTags.asStateFlow()
    
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    
    // Job para actualizar el contador de duración
    private var durationJob: Job? = null
    
    // Mensaje de error
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()
    
    /**
     * Inicia el flujo de captura (se llama cuando se abre la pantalla)
     */
    fun startCaptureFlow() {
        _captureState.value = CaptureState.CapturingBackPhoto
        loadAvailableTags()
    }
    
    /**
     * Carga las etiquetas disponibles de los waypoints existentes del usuario
     */
    private fun loadAvailableTags() {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid
                if (userId == null) {
                    android.util.Log.d("CreateWaypointViewModel", "loadAvailableTags: No hay usuario")
                    return@launch
                }
                
                android.util.Log.d("CreateWaypointViewModel", "loadAvailableTags: Cargando etiquetas para userId: $userId")
                
                val snapshot = db.collection("waypoints")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()
                
                android.util.Log.d("CreateWaypointViewModel", "loadAvailableTags: Encontrados ${snapshot.documents.size} waypoints")
                
                val allTags = snapshot.documents
                    .mapNotNull { doc ->
                        val waypoint = doc.toObject(Waypoint::class.java)
                        android.util.Log.d("CreateWaypointViewModel", "loadAvailableTags: Waypoint ${doc.id} tiene tags: ${waypoint?.tags}")
                        waypoint?.tags
                    }
                    .flatten()
                    .distinct()
                    .sorted()
                
                android.util.Log.d("CreateWaypointViewModel", "loadAvailableTags: Etiquetas únicas encontradas: $allTags")
                _availableTags.value = allTags
            } catch (e: Exception) {
                android.util.Log.e("CreateWaypointViewModel", "loadAvailableTags: Error al cargar etiquetas", e)
            }
        }
    }
    
    /**
     * Callback cuando se toma la foto trasera
     */
    fun onBackPhotoTaken(uri: Uri) {
        _backPhotoUri.value = uri
        // Transición automática a foto frontal
        _captureState.value = CaptureState.CapturingFrontPhoto
    }
    
    /**
     * Callback cuando se toma la foto frontal
     */
    fun onFrontPhotoTaken(uri: Uri) {
        _frontPhotoUri.value = uri
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
     * Actualiza el título de la nota
     */
    fun updateTitle(newTitle: String) {
        _title.value = newTitle
    }
    
    /**
     * Agrega una etiqueta
     */
    fun addTag(tag: String) {
        val trimmedTag = tag.trim()
        android.util.Log.d("CreateWaypointViewModel", "addTag: Intentando agregar etiqueta: '$trimmedTag'")
        if (trimmedTag.isNotEmpty()) {
            // Verificar si ya existe (comparación case-insensitive)
            val exists = _tags.value.any { it.equals(trimmedTag, ignoreCase = true) }
            android.util.Log.d("CreateWaypointViewModel", "addTag: Etiqueta ya existe: $exists, etiquetas actuales: ${_tags.value}")
            if (!exists) {
                _tags.value = _tags.value + trimmedTag
                android.util.Log.d("CreateWaypointViewModel", "addTag: Etiqueta agregada. Nuevas etiquetas: ${_tags.value}")
            }
        }
    }
    
    /**
     * Elimina una etiqueta
     */
    fun removeTag(tag: String) {
        _tags.value = _tags.value.filter { it != tag }
    }
    
    /**
     * Actualiza todas las etiquetas
     */
    fun updateTags(newTags: List<String>) {
        _tags.value = newTags
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
                
                // Paso 2: Subir foto trasera
                _captureState.value = CaptureState.UploadingData("Subiendo foto trasera...")
                
                val backPhotoUri = _backPhotoUri.value
                if (backPhotoUri == null) {
                    _captureState.value = CaptureState.Error("No se encontró la foto trasera")
                    return@launch
                }
                
                val backPhotoUrlResult = repository.uploadPhoto(backPhotoUri)
                if (backPhotoUrlResult.isFailure) {
                    _captureState.value = CaptureState.Error(
                        "Error al subir la foto trasera: ${backPhotoUrlResult.exceptionOrNull()?.message}"
                    )
                    return@launch
                }
                
                val backPhotoUrl = backPhotoUrlResult.getOrThrow()
                
                // Paso 3: Subir foto frontal
                _captureState.value = CaptureState.UploadingData("Subiendo foto frontal...")
                
                val frontPhotoUri = _frontPhotoUri.value
                if (frontPhotoUri == null) {
                    _captureState.value = CaptureState.Error("No se encontró la foto frontal")
                    // Limpiar foto trasera subida
                    repository.deleteUploadedFiles(backPhotoUrl, null)
                    return@launch
                }
                
                val frontPhotoUrlResult = repository.uploadPhoto(frontPhotoUri)
                if (frontPhotoUrlResult.isFailure) {
                    _captureState.value = CaptureState.Error(
                        "Error al subir la foto frontal: ${frontPhotoUrlResult.exceptionOrNull()?.message}"
                    )
                    // Limpiar foto trasera subida
                    repository.deleteUploadedFiles(backPhotoUrl, null)
                    return@launch
                }
                
                val frontPhotoUrl = frontPhotoUrlResult.getOrThrow()
                
                // Paso 4: Subir audio
                _captureState.value = CaptureState.UploadingData("Subiendo audio...")
                
                val audioFileLocal = audioFile
                if (audioFileLocal == null || !audioFileLocal.exists()) {
                    _captureState.value = CaptureState.Error("No se encontró el archivo de audio")
                    // Limpiar fotos subidas
                    repository.deleteUploadedFiles(backPhotoUrl, null)
                    repository.deleteUploadedFiles(frontPhotoUrl, null)
                    return@launch
                }
                
                val audioUrlResult = repository.uploadAudio(audioFileLocal.toUri())
                if (audioUrlResult.isFailure) {
                    _captureState.value = CaptureState.Error(
                        "Error al subir el audio: ${audioUrlResult.exceptionOrNull()?.message}"
                    )
                    // Limpiar fotos subidas
                    repository.deleteUploadedFiles(backPhotoUrl, null)
                    repository.deleteUploadedFiles(frontPhotoUrl, null)
                    return@launch
                }
                
                val audioUrl = audioUrlResult.getOrThrow()
                
                // Paso 5: Guardar waypoint en Firestore
                _captureState.value = CaptureState.UploadingData("Guardando waypoint...")
                
                val waypoint = Waypoint(
                    userId = repository.getCurrentUserId(),
                    photoUrl = backPhotoUrl,
                    frontPhotoUrl = frontPhotoUrl,
                    audioUrl = audioUrl,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    locationName = locationName,
                    title = _title.value,
                    tags = _tags.value,
                    timestamp = Date()
                )
                
                val saveResult = repository.saveWaypoint(waypoint)
                if (saveResult.isFailure) {
                    _captureState.value = CaptureState.Error(
                        "Error al guardar el waypoint: ${saveResult.exceptionOrNull()?.message}"
                    )
                    // Limpiar archivos subidos
                    repository.deleteUploadedFiles(backPhotoUrl, null)
                    repository.deleteUploadedFiles(frontPhotoUrl, null)
                    repository.deleteUploadedFiles(null, audioUrl)
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
        if (_backPhotoUri.value != null && _frontPhotoUri.value != null && audioFile != null) {
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
        _backPhotoUri.value = null
        _frontPhotoUri.value = null
        _recordingDuration.value = 0
        _title.value = ""
        _tags.value = emptyList()
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
