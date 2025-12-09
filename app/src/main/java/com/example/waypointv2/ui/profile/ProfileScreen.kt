package com.example.waypointv2.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.waypointv2.ui.auth.AuthViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel = viewModel(),
    onLogoutClick: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Perfil",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Light,
                            fontSize = 20.sp,
                            color = Color.Black
                        )
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black
                )
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            // Avatar circular
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        color = Color.Black,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currentUser?.email?.firstOrNull()?.uppercaseChar()?.toString() ?: "U",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Light,
                        color = Color.White
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Email del usuario
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Correo Electrónico",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currentUser?.email ?: "No disponible",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.Black
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // ID del usuario
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "ID de Usuario",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currentUser?.uid ?: "No disponible",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.Gray
                    )
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Botón de cerrar sesión
            Button(
                onClick = onLogoutClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "Cerrar Sesión",
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Cerrar Sesión",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

