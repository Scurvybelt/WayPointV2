package com.example.waypointv2

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.waypointv2.ui.auth.AuthViewModel
import com.example.waypointv2.ui.auth.LoginScreen
import com.example.waypointv2.ui.auth.RegisterScreen
import com.example.waypointv2.ui.home.HomeViewModel
import com.example.waypointv2.ui.home.MainScreen
import com.example.waypointv2.ui.home.Waypoint
import com.example.waypointv2.ui.home.WaypointDetailScreen
import com.example.waypointv2.ui.map.WaypointsMapScreen
import com.example.waypointv2.ui.navigation.BottomNavigationBar
import com.example.waypointv2.ui.profile.ProfileScreen
import com.example.waypointv2.ui.theme.WayPointV2Theme
import com.example.waypointv2.ui.waypoint.CreateWaypointScreen

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WayPointV2Theme {
                WaypointApp(authViewModel)
            }
        }
    }
}

@Composable
fun WaypointApp(authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val error by authViewModel.error.collectAsState()
    val context = LocalContext.current

    // Mostrar Toast con el error
    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            authViewModel.clearError() // Limpiar el error para que no se muestre de nuevo
        }
    }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (isLoggedIn) "main" else "login",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("login") {
                LoginScreen(
                    onLoginClick = { email, password ->
                        authViewModel.login(email, password)
                    },
                    onNavigateToRegister = {
                        navController.navigate("register")
                    }
                )
            }
            composable("register") {
                RegisterScreen(
                    onRegisterClick = { email, password ->
                        authViewModel.register(email, password)
                    },
                    onNavigateToLogin = {
                        navController.popBackStack()
                    }
                )
            }
            composable("main") {
                val homeViewModel: HomeViewModel = viewModel()
                MainScreen(
                    homeViewModel = homeViewModel,
                    onFabClick = { navController.navigate("create_waypoint") },
                    onLogoutClick = { authViewModel.logout() },
                    onWaypointClick = { waypoint ->
                        // Pasar el ID del waypoint como argumento
                        navController.navigate("waypoint_detail/${waypoint.id}")
                    },
                    onMapClick = { navController.navigate("waypoints_map") }
                )
            }
            composable("create_waypoint") {
                CreateWaypointScreen(onBackClick = { navController.navigate("main") })
            }
            composable("profile") {
                ProfileScreen(
                    authViewModel = authViewModel,
                    onLogoutClick = { authViewModel.logout() }
                )
            }
            composable("waypoints_map") {
                WaypointsMapScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(
                route = "waypoint_detail/{waypointId}",
                arguments = listOf(navArgument("waypointId") { type = NavType.StringType })
            ) { backStackEntry ->
                val waypointId = backStackEntry.arguments?.getString("waypointId") ?: ""
                val homeViewModel: HomeViewModel = viewModel()
                val waypoints by homeViewModel.waypoints.collectAsState()
                
                // Buscar el waypoint por ID
                val waypoint = waypoints.find { it.id == waypointId }
                
                if (waypoint != null) {
                    WaypointDetailScreen(
                        waypoint = waypoint,
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }
        }
    }
    
    // Este efecto se encargará de la navegación cuando cambie el estado de login
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            if (navController.currentBackStackEntry?.destination?.route != "main") {
                navController.navigate("main") {
                    // Limpia el backstack para que el usuario no pueda volver a la pantalla de login
                    popUpTo("login") { inclusive = true }
                }
            }
        } else {
             // Si el usuario cierra sesión, volvemos al login
            if (navController.currentBackStackEntry?.destination?.route != "login") {
                 navController.navigate("login") {
                    popUpTo("main") { inclusive = true }
                }
            }
        }
    }
}
