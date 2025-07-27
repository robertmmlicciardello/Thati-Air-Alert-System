package com.thati.airalert.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.math.*

/**
 * Location Utilities
 * Helper functions for GPS, location services, and geographic calculations
 */
object LocationUtils {
    
    /**
     * Check if location permissions are granted
     */
    fun hasLocationPermission(context: Context): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Check if GPS is enabled
     */
    fun isGpsEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }
    
    /**
     * Check if network location is enabled
     */
    fun isNetworkLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
    
    /**
     * Calculate distance between two points in meters
     */
    fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val earthRadius = 6371000.0 // Earth radius in meters
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return earthRadius * c
    }
    
    /**
     * Calculate distance using Location objects
     */
    fun calculateDistance(location1: Location, location2: Location): Float {
        return location1.distanceTo(location2)
    }
    
    /**
     * Check if location is within Myanmar boundaries
     */
    fun isLocationInMyanmar(latitude: Double, longitude: Double): Boolean {
        // Myanmar approximate boundaries
        val minLat = 9.5
        val maxLat = 28.5
        val minLng = 92.2
        val maxLng = 101.2
        
        return latitude in minLat..maxLat && longitude in minLng..maxLng
    }
    
    /**
     * Get Myanmar region based on coordinates
     */
    fun getMyanmarRegion(latitude: Double, longitude: Double): String? {
        // Simplified region detection based on coordinates
        // In production, this should use more accurate boundary data
        return when {
            // Yangon Region
            latitude in 16.0..17.5 && longitude in 95.5..97.0 -> "yangon"
            // Mandalay Region  
            latitude in 21.5..23.0 && longitude in 95.5..97.0 -> "mandalay"
            // Naypyitaw
            latitude in 19.5..20.5 && longitude in 96.0..97.0 -> "naypyitaw"
            // Bago Region
            latitude in 17.0..19.5 && longitude in 95.0..97.0 -> "bago"
            // Magway Region
            latitude in 19.5..22.5 && longitude in 94.0..96.0 -> "magway"
            // Sagaing Region
            latitude in 21.5..26.0 && longitude in 94.0..96.5 -> "sagaing"
            // Ayeyarwady Region
            latitude in 15.0..19.0 && longitude in 94.5..96.5 -> "ayeyarwady"
            // Mon State
            latitude in 15.0..17.0 && longitude in 97.0..98.5 -> "mon"
            // Kayin State
            latitude in 16.0..19.0 && longitude in 97.0..99.0 -> "kayin"
            // Tanintharyi Region
            latitude in 10.0..15.0 && longitude in 98.0..99.5 -> "tanintharyi"
            // Shan State
            latitude in 19.5..24.5 && longitude in 96.5..101.2 -> "shan"
            // Kayah State
            latitude in 18.5..20.0 && longitude in 96.5..98.0 -> "kayah"
            // Chin State
            latitude in 21.0..24.5 && longitude in 92.5..94.5 -> "chin"
            // Rakhine State
            latitude in 17.5..21.5 && longitude in 92.2..95.0 -> "rakhine"
            // Kachin State
            latitude in 23.5..28.5 && longitude in 96.0..98.5 -> "kachin"
            else -> null
        }
    }
    
    /**
     * Get address from coordinates using Geocoder
     */
    suspend fun getAddressFromCoordinates(
        context: Context,
        latitude: Double,
        longitude: Double
    ): String? = withContext(Dispatchers.IO) {
        try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                addresses?.firstOrNull()?.let { address ->
                    buildString {
                        address.thoroughfare?.let { append("$it, ") }
                        address.locality?.let { append("$it, ") }
                        address.adminArea?.let { append("$it, ") }
                        address.countryName?.let { append(it) }
                    }.trimEnd(',', ' ')
                }
            } else null
        } catch (e: Exception) {
            Logger.e("Failed to get address from coordinates", "LocationUtils", e)
            null
        }
    }
    
    /**
     * Get coordinates from address using Geocoder
     */
    suspend fun getCoordinatesFromAddress(
        context: Context,
        address: String
    ): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocationName(address, 1)
                addresses?.firstOrNull()?.let { 
                    Pair(it.latitude, it.longitude)
                }
            } else null
        } catch (e: Exception) {
            Logger.e("Failed to get coordinates from address", "LocationUtils", e)
            null
        }
    }
    
    /**
     * Format coordinates for display
     */
    fun formatCoordinates(latitude: Double, longitude: Double): String {
        val latDirection = if (latitude >= 0) "N" else "S"
        val lonDirection = if (longitude >= 0) "E" else "W"
        
        return "${abs(latitude).format(6)}°$latDirection, ${abs(longitude).format(6)}°$lonDirection"
    }
    
    /**
     * Check if location is within specified radius of target
     */
    fun isWithinRadius(
        currentLat: Double, currentLon: Double,
        targetLat: Double, targetLon: Double,
        radiusMeters: Double
    ): Boolean {
        val distance = calculateDistance(currentLat, currentLon, targetLat, targetLon)
        return distance <= radiusMeters
    }
    
    /**
     * Get bearing between two points
     */
    fun getBearing(
        startLat: Double, startLon: Double,
        endLat: Double, endLon: Double
    ): Double {
        val dLon = Math.toRadians(endLon - startLon)
        val startLatRad = Math.toRadians(startLat)
        val endLatRad = Math.toRadians(endLat)
        
        val y = sin(dLon) * cos(endLatRad)
        val x = cos(startLatRad) * sin(endLatRad) - sin(startLatRad) * cos(endLatRad) * cos(dLon)
        
        val bearing = Math.toDegrees(atan2(y, x))
        return (bearing + 360) % 360
    }
    
    /**
     * Get compass direction from bearing
     */
    fun getCompassDirection(bearing: Double): String {
        return when ((bearing + 22.5) % 360) {
            in 0.0..45.0 -> "N"
            in 45.0..90.0 -> "NE"
            in 90.0..135.0 -> "E"
            in 135.0..180.0 -> "SE"
            in 180.0..225.0 -> "S"
            in 225.0..270.0 -> "SW"
            in 270.0..315.0 -> "W"
            in 315.0..360.0 -> "NW"
            else -> "N"
        }
    }
    
    /**
     * Generate random location within Myanmar for testing
     */
    fun generateRandomMyanmarLocation(): Pair<Double, Double> {
        val minLat = 9.5
        val maxLat = 28.5
        val minLng = 92.2
        val maxLng = 101.2
        
        val latitude = minLat + (maxLat - minLat) * Math.random()
        val longitude = minLng + (maxLng - minLng) * Math.random()
        
        return Pair(latitude, longitude)
    }
    
    /**
     * Validate coordinate values
     */
    fun isValidCoordinate(latitude: Double, longitude: Double): Boolean {
        return latitude in -90.0..90.0 && longitude in -180.0..180.0
    }
    
    /**
     * Get location accuracy description
     */
    fun getAccuracyDescription(accuracy: Float): String {
        return when {
            accuracy <= 5 -> "Excellent"
            accuracy <= 10 -> "Good"
            accuracy <= 20 -> "Fair"
            accuracy <= 50 -> "Poor"
            else -> "Very Poor"
        }
    }
    
    /**
     * Extension function to format Double with specified decimal places
     */
    private fun Double.format(decimals: Int): String {
        return "%.${decimals}f".format(this)
    }
    
    /**
     * Create Location object from coordinates
     */
    fun createLocation(latitude: Double, longitude: Double, provider: String = "manual"): Location {
        return Location(provider).apply {
            this.latitude = latitude
            this.longitude = longitude
            time = System.currentTimeMillis()
        }
    }
    
    /**
     * Check if two locations are approximately equal
     */
    fun locationsEqual(loc1: Location, loc2: Location, toleranceMeters: Float = 10f): Boolean {
        return loc1.distanceTo(loc2) <= toleranceMeters
    }
}