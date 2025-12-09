package com.example.waypointv2.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.io.IOException
import java.util.Locale

class LocationHandler(private val context: Context) {
    
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private val geocoder = Geocoder(context, Locale.getDefault())
    
    /**
     * Obtiene la ubicación actual del dispositivo
     * @return Result con Location o error
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Result<Location> {
        return try {
            // Verificar permisos
            if (!hasLocationPermission()) {
                return Result.failure(SecurityException("Permiso de ubicación no concedido"))
            }
            
            // Intentar obtener ubicación con timeout de 10 segundos
            val location = withTimeout(10000L) {
                val cancellationToken = CancellationTokenSource()
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationToken.token
                ).await()
            }
            
            if (location != null) {
                Result.success(location)
            } else {
                Result.failure(Exception("No se pudo obtener la ubicación. Verifica que el GPS esté activado."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Obtiene el nombre de la ubicación mediante geocodificación inversa
     * @param latitude Latitud
     * @param longitude Longitud
     * @return Nombre de la ubicación o coordenadas si falla
     */
    suspend fun getLocationName(latitude: Double, longitude: Double): String {
        return try {
            @Suppress("DEPRECATION")
            val addresses: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)
            
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                buildLocationName(address)
            } else {
                // Fallback a coordenadas
                formatCoordinates(latitude, longitude)
            }
        } catch (e: IOException) {
            // Si falla la geocodificación, usar coordenadas
            formatCoordinates(latitude, longitude)
        }
    }
    
    /**
     * Construye un nombre legible de la ubicación
     */
    private fun buildLocationName(address: Address): String {
        return when {
            // Si hay nombre de lugar específico (ej: "Starbucks")
            address.featureName != null && address.featureName != address.thoroughfare -> {
                address.featureName
            }
            // Si hay calle y ciudad
            address.thoroughfare != null && address.locality != null -> {
                "${address.thoroughfare}, ${address.locality}"
            }
            // Si solo hay ciudad
            address.locality != null -> {
                address.locality
            }
            // Si solo hay estado/provincia
            address.adminArea != null -> {
                address.adminArea
            }
            // Fallback a país
            address.countryName != null -> {
                address.countryName
            }
            // Último recurso: coordenadas
            else -> {
                formatCoordinates(address.latitude, address.longitude)
            }
        }
    }
    
    /**
     * Formatea coordenadas como string
     */
    private fun formatCoordinates(latitude: Double, longitude: Double): String {
        return String.format(Locale.US, "%.4f, %.4f", latitude, longitude)
    }
    
    /**
     * Verifica si tiene permisos de ubicación
     */
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}
