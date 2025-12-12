package com.example.waypointv2.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ViewType {
    CARDS,    // Vista de cards (actual)
    GRID,     // Vista de mosaico
    LIST      // Vista de listado compacto
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    homeViewModel: HomeViewModel = viewModel(),
    onFabClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onWaypointClick: (Waypoint) -> Unit = {},
    onMapClick: () -> Unit = {}
) {
    val waypoints by homeViewModel.waypoints.collectAsState()
    
    // Estado para el tipo de vista
    var viewType by remember { mutableStateOf(ViewType.CARDS) }
    
    // Estados para búsqueda y filtro
    var searchText by remember { mutableStateOf("") }
    var filterDate by remember { mutableStateOf<Date?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedTags by remember { mutableStateOf<Set<String>>(emptySet()) }
    
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = filterDate?.time
    )
    
    // Obtener todas las etiquetas únicas de los waypoints
    val allTags = remember(waypoints) {
        waypoints.flatMap { it.tags }.distinct().sorted()
    }
    
    // Filtrar waypoints
    val filteredWaypoints = remember(waypoints, searchText, filterDate, selectedTags) {
        waypoints.filter { waypoint ->
            // Filtro por nombre (título)
            val matchesSearch = searchText.isEmpty() || 
                waypoint.title.contains(searchText, ignoreCase = true) ||
                waypoint.locationName.contains(searchText, ignoreCase = true)
            
            // Filtro por fecha
            val matchesDate = if (filterDate == null) {
                true
            } else {
                waypoint.timestamp?.let { date ->
                    val calendarFilter = Calendar.getInstance().apply {
                        time = filterDate!!
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val calendarWaypoint = Calendar.getInstance().apply {
                        time = date
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    calendarFilter.timeInMillis == calendarWaypoint.timeInMillis
                } ?: false
            }
            
            // Filtro por etiquetas
            val matchesTags = if (selectedTags.isEmpty()) {
                true
            } else {
                waypoint.tags.any { it in selectedTags }
            }
            
            matchesSearch && matchesDate && matchesTags
        }
    }
    
    // Formatear fecha para mostrar
    val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale("es", "ES"))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Waypoints",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Light,
                            fontSize = 20.sp,
                            color = Color.Black
                        )
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black,
                    actionIconContentColor = Color.Black
                ),
                actions = {
                    IconButton(onClick = onMapClick) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = "Ver Mapa",
                            tint = Color.Black
                        )
                    }
                    IconButton(onClick = onLogoutClick) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Cerrar Sesión",
                            tint = Color.Black
                        )
                    }
                }
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color.White)
        ) {
            // Barra de búsqueda y filtro
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Campo de búsqueda y filtro por fecha en la misma fila
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Campo de búsqueda por nombre
                    TextField(
                        value = searchText,
                        onValueChange = { newValue -> searchText = newValue },
                        placeholder = {
                            Text(
                                "Buscar",
                                color = Color.Gray.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Buscar",
                                tint = Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchText.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchText = "" },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Limpiar",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minWidth = 200.dp),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF000000),
                            unfocusedTextColor = Color(0xFF000000),
                            focusedLabelColor = Color.Gray,
                            unfocusedLabelColor = Color.Gray,
                            focusedIndicatorColor = Color.Black,
                            unfocusedIndicatorColor = Color.Gray.copy(alpha = 0.3f),
                            cursorColor = Color.Black,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            disabledTextColor = Color.Black,
                            disabledLabelColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 14.sp,
                            color = Color(0xFF000000),
                            fontWeight = FontWeight.Normal
                        )
                    )
                    
                    // Icono de calendario (más compacto)
                    IconButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = if (filterDate != null) Color.Black else Color.Gray.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Seleccionar fecha",
                            tint = if (filterDate != null) Color.White else Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                
                // Selector de vista (debajo del input de búsqueda)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TextButton(
                        onClick = { viewType = ViewType.CARDS },
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (viewType == ViewType.CARDS) Color.Black else Color.Gray.copy(alpha = 0.6f),
                            containerColor = if (viewType == ViewType.CARDS) Color.Gray.copy(alpha = 0.1f) else Color.Transparent
                        ),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Cards",
                            fontSize = 11.sp,
                            fontWeight = if (viewType == ViewType.CARDS) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                    TextButton(
                        onClick = { viewType = ViewType.GRID },
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (viewType == ViewType.GRID) Color.Black else Color.Gray.copy(alpha = 0.6f),
                            containerColor = if (viewType == ViewType.GRID) Color.Gray.copy(alpha = 0.1f) else Color.Transparent
                        ),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Grid",
                            fontSize = 11.sp,
                            fontWeight = if (viewType == ViewType.GRID) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                    TextButton(
                        onClick = { viewType = ViewType.LIST },
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (viewType == ViewType.LIST) Color.Black else Color.Gray.copy(alpha = 0.6f),
                            containerColor = if (viewType == ViewType.LIST) Color.Gray.copy(alpha = 0.1f) else Color.Transparent
                        ),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "List",
                            fontSize = 11.sp,
                            fontWeight = if (viewType == ViewType.LIST) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                }
                
                // Mostrar fecha seleccionada y etiquetas
                if (filterDate != null || allTags.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mostrar fecha seleccionada como chip
                        if (filterDate != null) {
                            AssistChip(
                                onClick = { filterDate = null },
                                label = {
                                    Text(
                                        dateFormatter.format(filterDate!!),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 11.sp,
                                            color = Color.Black
                                        )
                                    )
                                },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Limpiar",
                                        modifier = Modifier.size(14.dp),
                                        tint = Color.Black
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = Color.Gray.copy(alpha = 0.1f),
                                    labelColor = Color.Black
                                ),
                                modifier = Modifier.height(28.dp)
                            )
                        }
                        
                        // Mostrar etiquetas disponibles para filtrar
                        if (allTags.isNotEmpty()) {
                            LazyRow(
                                modifier = if (filterDate != null) Modifier.weight(1f) else Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(allTags.size) { index ->
                                    val tag = allTags[index]
                                    val isSelected = tag in selectedTags
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedTags = if (isSelected) {
                                                selectedTags - tag
                                            } else {
                                                selectedTags + tag
                                            }
                                        },
                                        label = {
                                            Text(
                                                tag,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 11.sp
                                                )
                                            )
                                        },
                                        modifier = Modifier.height(28.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color.Black,
                                            selectedLabelColor = Color.White,
                                            containerColor = Color.Gray.copy(alpha = 0.1f),
                                            labelColor = Color.Black
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                
                // DatePicker Dialog
                if (showDatePicker) {
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    datePickerState.selectedDateMillis?.let { millis ->
                                        filterDate = Date(millis)
                                    }
                                    showDatePicker = false
                                }
                            ) {
                                Text(
                                    "Seleccionar",
                                    color = Color.Black
                                )
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showDatePicker = false }
                            ) {
                                Text(
                                    "Cancelar",
                                    color = Color.Black
                                )
                            }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }
            }
            
            // Lista de waypoints
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                if (waypoints.isEmpty()) {
                    Text(
                        text = "Aún no tienes Waypoints, ¡crea uno!",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = Color.Gray,
                            fontWeight = FontWeight.Normal
                        )
                    )
                } else if (filteredWaypoints.isEmpty()) {
                    Text(
                        text = "No se encontraron waypoints",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = Color.Gray,
                            fontWeight = FontWeight.Normal
                        )
                    )
                } else {
                    when (viewType) {
                        ViewType.CARDS -> {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(filteredWaypoints) { waypoint ->
                                    WaypointCard(
                                        waypoint = waypoint,
                                        onClick = { onWaypointClick(waypoint) }
                                    )
                                }
                            }
                        }
                        ViewType.GRID -> {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(filteredWaypoints) { waypoint ->
                                    WaypointGridItem(
                                        waypoint = waypoint,
                                        onClick = { onWaypointClick(waypoint) }
                                    )
                                }
                            }
                        }
                        ViewType.LIST -> {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                items(filteredWaypoints) { waypoint ->
                                    WaypointListItem(
                                        waypoint = waypoint,
                                        onClick = { onWaypointClick(waypoint) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WaypointCard(
    waypoint: Waypoint,
    onClick: () -> Unit = {}
) {
    var isPlaying by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val mediaPlayer = remember { 
        android.media.MediaPlayer().apply {
            setOnCompletionListener {
                isPlaying = false
            }
        }
    }
    
    DisposableEffect(waypoint.audioUrl) {
        onDispose {
            mediaPlayer.release()
        }
    }
    
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color.Gray.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column {
            // Imágenes del waypoint (trasera y frontal)
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Foto trasera
                AsyncImage(
                    model = waypoint.photoUrl,
                    contentDescription = "Foto trasera del waypoint",
                    modifier = Modifier
                        .weight(1f)
                        .height(220.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 0.dp)),
                    contentScale = ContentScale.Crop
                )
                // Foto frontal
                if (waypoint.frontPhotoUrl.isNotEmpty()) {
                    AsyncImage(
                        model = waypoint.frontPhotoUrl,
                        contentDescription = "Foto frontal del waypoint",
                        modifier = Modifier
                            .weight(1f)
                            .height(220.dp)
                            .clip(RoundedCornerShape(topStart = 0.dp, topEnd = 12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            
            // Información del waypoint
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Título de la nota
                if (waypoint.title.isNotEmpty()) {
                    Text(
                        text = waypoint.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 18.sp,
                            color = Color.Black
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
                
                // Ubicación
                Text(
                    text = waypoint.locationName.ifEmpty { "Ubicación desconocida" },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Coordenadas
                Text(
                    text = "${String.format("%.4f", waypoint.latitude)}, ${String.format("%.4f", waypoint.longitude)}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                // Fecha
                waypoint.timestamp?.let { date ->
                    val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("es", "ES"))
                    Text(
                        text = formatter.format(date),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    )
                }
                
                // Etiquetas
                if (waypoint.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(waypoint.tags.size) { index ->
                            AssistChip(
                                onClick = { },
                                label = {
                                    Text(
                                        waypoint.tags[index],
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 11.sp,
                                            color = Color.Black
                                        )
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = Color.Gray.copy(alpha = 0.1f),
                                    labelColor = Color.Black
                                )
                            )
                        }
                    }
                }
                
                // Botón de reproducir audio
                if (waypoint.audioUrl.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = {
                            if (isPlaying) {
                                mediaPlayer.pause()
                                isPlaying = false
                            } else {
                                try {
                                    if (!mediaPlayer.isPlaying) {
                                        mediaPlayer.reset()
                                        mediaPlayer.setDataSource(waypoint.audioUrl)
                                        mediaPlayer.prepare()
                                        mediaPlayer.start()
                                        isPlaying = true
                                    }
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(
                                        context,
                                        "Error al reproducir audio",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Check else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isPlaying) "Pausar Audio" else "Reproducir Audio",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// Vista de Mosaico (Grid)
@Composable
fun WaypointGridItem(
    waypoint: Waypoint,
    onClick: () -> Unit = {}
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .border(
                width = 1.dp,
                color = Color.Gray.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Box {
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                AsyncImage(
                    model = waypoint.photoUrl,
                    contentDescription = "Foto trasera del waypoint",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp, topEnd = 0.dp, bottomEnd = 0.dp)),
                    contentScale = ContentScale.Crop
                )
                if (waypoint.frontPhotoUrl.isNotEmpty()) {
                    AsyncImage(
                        model = waypoint.frontPhotoUrl,
                        contentDescription = "Foto frontal del waypoint",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 12.dp, bottomEnd = 12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            // Overlay con información
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Color.Black.copy(alpha = 0.3f)
                    )
                    .padding(12.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                if (waypoint.title.isNotEmpty()) {
                    Text(
                        text = waypoint.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = Color.White
                        ),
                        maxLines = 1
                    )
                }
                Text(
                    text = waypoint.locationName.ifEmpty { "Ubicación desconocida" },
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    ),
                    maxLines = 1
                )
                // Etiquetas
                if (waypoint.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(waypoint.tags.size) { index ->
                            AssistChip(
                                onClick = { },
                                label = {
                                    Text(
                                        waypoint.tags[index],
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 10.sp,
                                            color = Color.White
                                        )
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
        }
    }
}

// Vista de Lista Compacta
@Composable
fun WaypointListItem(
    waypoint: Waypoint,
    onClick: () -> Unit = {}
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .border(
                width = 0.5.dp,
                color = Color.Gray.copy(alpha = 0.1f),
                shape = RoundedCornerShape(0.dp)
            ),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Imágenes pequeñas (trasera y frontal)
            Row(
                modifier = Modifier.size(64.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                AsyncImage(
                    model = waypoint.photoUrl,
                    contentDescription = "Foto trasera del waypoint",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 0.dp, bottomEnd = 0.dp)),
                    contentScale = ContentScale.Crop
                )
                if (waypoint.frontPhotoUrl.isNotEmpty()) {
                    AsyncImage(
                        model = waypoint.frontPhotoUrl,
                        contentDescription = "Foto frontal del waypoint",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 8.dp, bottomEnd = 8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            
            // Información
            Column(
                modifier = Modifier
                    .weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                if (waypoint.title.isNotEmpty()) {
                    Text(
                        text = waypoint.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            color = Color.Black
                        ),
                        maxLines = 1
                    )
                } else {
                    Text(
                        text = "Waypoint",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            color = Color.Black
                        ),
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = waypoint.locationName.ifEmpty { 
                        "${String.format("%.2f", waypoint.latitude)}, ${String.format("%.2f", waypoint.longitude)}"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        color = Color.Gray
                    ),
                    maxLines = 1
                )
                waypoint.timestamp?.let { date ->
                    val formatter = SimpleDateFormat("dd MMM yyyy", Locale("es", "ES"))
                    Text(
                        text = formatter.format(date),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            color = Color.Gray.copy(alpha = 0.7f)
                        ),
                        maxLines = 1
                    )
                }
                // Etiquetas
                if (waypoint.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(waypoint.tags.size) { index ->
                            AssistChip(
                                onClick = { },
                                label = {
                                    Text(
                                        waypoint.tags[index],
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 10.sp,
                                            color = Color.Black
                                        )
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = Color.Gray.copy(alpha = 0.1f),
                                    labelColor = Color.Black
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
