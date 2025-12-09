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
        val parts = mutableListOf<String>()
        
        // Agregar nombre de lugar específico si existe y no es solo un número
        val featureName = address.featureName
        if (featureName != null && featureName != address.thoroughfare) {
            // Si el featureName es solo un número, combinarlo con la calle
            if (featureName.matches(Regex("^\\d+$"))) {
                // Es solo un número, lo ignoramos y usaremos la calle completa
            } else {
                parts.add(featureName)
            }
        }
        
        // Agregar calle (thoroughfare)
        if (address.thoroughfare != null) {
            parts.add(address.thoroughfare)
        }
        
        // Agregar ciudad
        if (address.locality != null) {
            parts.add(address.locality)
        }
        
        // Si tenemos al menos una parte, construir el nombre
        if (parts.isNotEmpty()) {
            return parts.joinToString(", ")
        }
        
        // Si no hay partes útiles, intentar con estado/provincia
        if (address.adminArea != null) {
            return address.adminArea
        }
        
        // Fallback a país
        if (address.countryName != null) {
            return address.countryName
        }
        
        // Último recurso: coordenadas
        return formatCoordinates(address.latitude, address.longitude)
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
