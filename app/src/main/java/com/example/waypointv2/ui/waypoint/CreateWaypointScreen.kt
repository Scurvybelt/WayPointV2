package com.example.waypointv2.ui.waypoint

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import java.io.File

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CreateWaypointScreen(
    onBackClick: () -> Unit,
    viewModel: CreateWaypointViewModel = viewModel()
) {
    val context = LocalContext.current
    val captureState by viewModel.captureState.collectAsState()
    val backPhotoUri by viewModel.backPhotoUri.collectAsState()
    val frontPhotoUri by viewModel.frontPhotoUri.collectAsState()
    val recordingDuration by viewModel.recordingDuration.collectAsState()
    
    // Permisos necesarios
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    )
    
    // Archivos para las fotos
    val backPhotoFile = remember {
        File(context.cacheDir, "back_photo_${System.currentTimeMillis()}.jpg")
    }
    
    val frontPhotoFile = remember {
        File(context.cacheDir, "front_photo_${System.currentTimeMillis()}.jpg")
    }
    
    val backPhotoUriForCamera = remember {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            backPhotoFile
        )
    }
    
    val frontPhotoUriForCamera = remember {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            frontPhotoFile
        )
    }
    
    // Launcher para la cámara trasera
    val backCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            viewModel.onBackPhotoTaken(backPhotoUriForCamera)
        } else {
            viewModel.onPhotoCancelled()
            onBackClick()
        }
    }
    
    // Launcher para la cámara frontal usando Intent personalizado
    val frontCameraLauncher = rememberLauncherForActivityResult(
        contract = object : ActivityResultContract<Uri, Boolean>() {
            override fun createIntent(context: android.content.Context, input: Uri): Intent {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    putExtra(MediaStore.EXTRA_OUTPUT, input)
                    // Intentar especificar cámara frontal
                    // Nota: Esto puede no funcionar en todos los dispositivos
                    // Algunos dispositivos ignoran este parámetro
                    putExtra("android.intent.extras.CAMERA_FACING", 1) // 1 = frontal
                    addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                return intent
            }
            
            override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
                return resultCode == android.app.Activity.RESULT_OK
            }
        }
    ) { success ->
        if (success) {
            viewModel.onFrontPhotoTaken(frontPhotoUriForCamera)
        } else {
            viewModel.onPhotoCancelled()
            onBackClick()
        }
    }
    
    
    // Efecto para iniciar el flujo cuando se abre la pantalla o cuando se conceden permisos
    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted) {
            viewModel.startCaptureFlow()
        }
    }
    
    // Efecto para lanzar la cámara según el estado
    LaunchedEffect(captureState) {
        when (captureState) {
            is CaptureState.CapturingBackPhoto -> {
                if (permissionsState.allPermissionsGranted) {
                    backCameraLauncher.launch(backPhotoUriForCamera)
                }
            }
            is CaptureState.CapturingFrontPhoto -> {
                if (permissionsState.allPermissionsGranted) {
                    frontCameraLauncher.launch(frontPhotoUriForCamera)
                }
            }
            is CaptureState.Success -> {
                onBackClick()
            }
            else -> {}
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Solicitar permisos si no están concedidos
        if (!permissionsState.allPermissionsGranted) {
            PermissionRequestScreen(
                permissionsState = permissionsState,
                onBackClick = onBackClick
            )
        } else {
            // Mostrar UI según el estado
            when (val state = captureState) {
                is CaptureState.RecordingAudio -> {
                    val availableTags by viewModel.availableTags.collectAsState()
                    val currentTags by viewModel.tags.collectAsState()
                    AudioRecordingScreen(
                        backPhotoUri = backPhotoUri,
                        frontPhotoUri = frontPhotoUri,
                        recordingDuration = recordingDuration,
                        title = viewModel.title.value,
                        onTitleChange = { viewModel.updateTitle(it) },
                        tags = currentTags,
                        availableTags = availableTags,
                        onAddTag = { viewModel.addTag(it) },
                        onRemoveTag = { viewModel.removeTag(it) },
                        onStartRecording = { viewModel.startAudioRecording() },
                        onStopRecording = { viewModel.stopAudioRecording() },
                        onCancel = {
                            viewModel.cancelCapture()
                            onBackClick()
                        }
                    )
                }
                is CaptureState.ProcessingLocation,
                is CaptureState.UploadingData -> {
                    ProcessingScreen(
                        message = when (state) {
                            is CaptureState.ProcessingLocation -> "Obteniendo ubicación..."
                            is CaptureState.UploadingData -> state.progress
                            else -> "Procesando..."
                        }
                    )
                }
                is CaptureState.Error -> {
                    ErrorDialog(
                        message = state.message,
                        onRetry = { viewModel.retryUpload() },
                        onDismiss = {
                            viewModel.cancelCapture()
                            onBackClick()
                        }
                    )
                }
                else -> {
                    // Idle o CapturingPhoto - no mostrar nada (la cámara se abre automáticamente)
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionRequestScreen(
    permissionsState: com.google.accompanist.permissions.MultiplePermissionsState,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Permisos Necesarios",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Light,
                fontSize = 32.sp,
                color = Color.Black
            ),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Para crear waypoints necesitamos acceso a:",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp,
                color = Color.Gray
            ),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(40.dp))
        
        PermissionItem("📷", "Cámara", "Para tomar fotos")
        Spacer(modifier = Modifier.height(16.dp))
        PermissionItem("🎤", "Micrófono", "Para grabar notas de voz")
        Spacer(modifier = Modifier.height(16.dp))
        PermissionItem("📍", "Ubicación", "Para geolocalizar tus waypoints")
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = { permissionsState.launchMultiplePermissionRequest() },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                "Conceder Permisos",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(
            onClick = onBackClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Cancelar",
                color = Color.Black,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
fun PermissionItem(icon: String, title: String, description: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 32.sp
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                color = Color.Gray
            ),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AudioRecordingScreen(
    backPhotoUri: Uri?,
    frontPhotoUri: Uri?,
    recordingDuration: Long,
    title: String,
    onTitleChange: (String) -> Unit,
    tags: List<String>,
    availableTags: List<String>,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onCancel: () -> Unit
) {
    var isRecording by remember { mutableStateOf(false) }
    var localTitle by remember { mutableStateOf(title) }
    var tagInput by remember { mutableStateOf("") }
    var showTagInput by remember { mutableStateOf(false) }
    
    // Sincronizar el título local con el del ViewModel
    LaunchedEffect(title) {
        localTitle = title
    }
    
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
            // Botón de cancelar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onCancel) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancelar",
                        tint = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Preview de las fotos (trasera y frontal)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Foto trasera
                backPhotoUri?.let { uri ->
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(200.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 0.dp
                        )
                    ) {
                        Column {
                            Text(
                                text = "Trasera",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                ),
                                modifier = Modifier.padding(8.dp)
                            )
                            Image(
                                painter = rememberAsyncImagePainter(uri),
                                contentDescription = "Foto trasera",
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
                
                // Foto frontal
                frontPhotoUri?.let { uri ->
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(200.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 0.dp
                        )
                    ) {
                        Column {
                            Text(
                                text = "Frontal",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                ),
                                modifier = Modifier.padding(8.dp)
                            )
                            Image(
                                painter = rememberAsyncImagePainter(uri),
                                contentDescription = "Foto frontal",
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Campo de título
            TextField(
                value = localTitle,
                onValueChange = { newValue ->
                    localTitle = newValue
                    onTitleChange(newValue)
                },
                placeholder = {
                    Text(
                        "Título de la nota",
                        color = Color.Gray.copy(alpha = 0.7f)
                    )
                },
                label = { 
                    Text(
                        "Título de la nota",
                        color = Color.Gray
                    ) 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = Color.Gray,
                    unfocusedLabelColor = Color.Gray.copy(alpha = 0.7f),
                    focusedIndicatorColor = Color.White,
                    unfocusedIndicatorColor = Color.Gray.copy(alpha = 0.5f),
                    cursorColor = Color.White,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledTextColor = Color.Gray,
                    disabledLabelColor = Color.Gray.copy(alpha = 0.5f),
                    disabledIndicatorColor = Color.Gray.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(0.dp),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Campo de etiquetas
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Título con icono para crear nueva etiqueta
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Etiquetas",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 14.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    IconButton(
                        onClick = { showTagInput = !showTagInput },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (showTagInput) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = if (showTagInput) "Cerrar" else "Crear nueva etiqueta",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                // Mostrar etiquetas disponibles para seleccionar
                val unselectedTags = availableTags.filter { availableTag ->
                    !tags.any { selectedTag -> 
                        selectedTag.equals(availableTag, ignoreCase = true) 
                    }
                }
                
                // Debug: mostrar información
                LaunchedEffect(availableTags.size, tags.size) {
                    android.util.Log.d("CreateWaypointScreen", "Etiquetas disponibles: $availableTags")
                    android.util.Log.d("CreateWaypointScreen", "Etiquetas seleccionadas: $tags")
                    android.util.Log.d("CreateWaypointScreen", "Etiquetas no seleccionadas: $unselectedTags")
                }
                
                if (unselectedTags.isNotEmpty()) {
                    Text(
                        text = "Etiquetas disponibles (${unselectedTags.size})",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            color = Color.Gray.copy(alpha = 0.8f)
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = unselectedTags,
                            key = { tag -> tag }
                        ) { tag ->
                            Button(
                                onClick = { 
                                    android.util.Log.d("CreateWaypointScreen", "Click en etiqueta: $tag")
                                    onAddTag(tag)
                                },
                                modifier = Modifier.height(32.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.2f),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Agregar",
                                    modifier = Modifier.size(14.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    tag,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                } else if (availableTags.isNotEmpty()) {
                    // Todas las etiquetas están seleccionadas
                    Text(
                        text = "Todas las etiquetas disponibles están seleccionadas",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            color = Color.Gray.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                // Campo de texto para agregar nuevas etiquetas (solo visible si se activa)
                if (showTagInput) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = tagInput,
                            onValueChange = { tagInput = it },
                            placeholder = {
                                Text(
                                    "Escribe una nueva etiqueta y presiona +",
                                    color = Color.Gray.copy(alpha = 0.7f)
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedIndicatorColor = Color.White,
                                unfocusedIndicatorColor = Color.Gray.copy(alpha = 0.5f),
                                cursorColor = Color.White,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(0.dp),
                            singleLine = true
                        )
                        IconButton(
                            onClick = {
                                if (tagInput.isNotBlank()) {
                                    onAddTag(tagInput)
                                    tagInput = ""
                                    showTagInput = false
                                }
                            },
                            modifier = Modifier
                                .background(
                                    Color.White.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Agregar etiqueta",
                                tint = Color.White
                            )
                        }
                    }
                }
                
                // Mostrar etiquetas seleccionadas
                if (tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Etiquetas seleccionadas",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            color = Color.Gray.copy(alpha = 0.8f)
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = tags,
                            key = { tag -> tag }
                        ) { tag ->
                            AssistChip(
                                onClick = { onRemoveTag(tag) },
                                label = {
                                    Text(
                                        tag,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 12.sp,
                                            color = Color.White
                                        )
                                    )
                                },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Eliminar",
                                        modifier = Modifier.size(16.dp),
                                        tint = Color.White
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = Color.White.copy(alpha = 0.2f),
                                    labelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Indicador de grabación
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isRecording) {
                    RecordingIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = formatDuration(recordingDuration),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Light,
                            color = Color.White
                        )
                    )
                } else {
                    Text(
                        text = "Graba una nota de voz",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Light,
                            color = Color.White
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Botón de grabar/detener
            FloatingActionButton(
                onClick = {
                    if (isRecording) {
                        isRecording = false
                        onStopRecording()
                    } else {
                        isRecording = true
                        onStartRecording()
                    }
                },
                modifier = Modifier.size(80.dp),
                containerColor = if (isRecording) Color.Red else Color.Black,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Check else Icons.Default.PlayArrow,
                    contentDescription = if (isRecording) "Detener" else "Grabar",
                    modifier = Modifier.size(40.dp),
                    tint = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

@Composable
fun RecordingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "recording")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(Color.Red.copy(alpha = alpha))
    )
}

@Composable
fun ProcessingScreen(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                color = Color.Black,
                strokeWidth = 3.dp
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.Black
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ErrorDialog(
    message: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Text(
                "Error",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 20.sp,
                    color = Color.Black
                )
            )
        },
        text = {
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    color = Color.Gray
                )
            )
        },
        confirmButton = {
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    "Reintentar",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Cancelar",
                    color = Color.Black,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    )
}

fun formatDuration(seconds: Long): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", minutes, secs)
}
