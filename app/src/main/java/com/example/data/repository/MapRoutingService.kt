package com.example.data.repository

import kotlin.math.*

data class GeoPoint(val latitude: Double, val longitude: Double, val name: String)

object MapRoutingService {

    // Predefined Roxou VIP Events coordinate database
    val ROXOU_EVENTS = listOf(
        GeoPoint(-23.5651, -46.6621, "Roxou Club Jardins (Al. Lorena 1500)"),
        GeoPoint(-23.5264, -46.6782, "Arena Roxou Metrópole (Av. Francisco Matarazzo 1705)"),
        GeoPoint(-23.7121, -46.8512, "Roxou Sunset Lounge (Estrada de Itapecerica, km 25)"),
        GeoPoint(-23.7012, -46.6978, "Roxou Festival Interlagos (Autódromo de Interlagos)")
    )

    // Famous SP locations geo registry
    private val KNOWN_LOCATIONS = mapOf(
        "congonhas" to GeoPoint(-23.6273, -46.6565, "Aeroporto de Congonhas (CGH), SP"),
        "guarulhos" to GeoPoint(-23.4356, -46.4730, "Aeroporto de Guarulhos (GRU), Terminal 2"),
        "paulista" to GeoPoint(-23.5615, -46.6554, "Av. Paulista, Jardins, SP"),
        "augusta" to GeoPoint(-23.5592, -46.6583, "Rua Augusta 1500, Consolação, SP"),
        "brooklin" to GeoPoint(-23.6094, -46.6853, "Berrini, Brooklin, SP"),
        "barra funda" to GeoPoint(-23.5260, -46.6668, "Barra Funda, SP"),
        "alphaville" to GeoPoint(-23.4975, -46.8322, "Alphaville, Barueri, SP"),
        "hilton" to GeoPoint(-23.6090, -46.6971, "Hotel Hilton Blue, Av. Nações Unidas")
    )

    // Current simulated user geolocation (geolocalização atual)
    val CURRENT_USER_LOCATION = GeoPoint(-23.5505, -46.6333, "Sua Localização Atual (Centro, SP)")

    fun geocodeLocation(input: String): GeoPoint {
        val normalized = input.lowercase().trim()
        
        // 1. Check if matches pre-defined Roxou Events
        for (event in ROXOU_EVENTS) {
            if (event.name.lowercase().contains(normalized) || normalized.contains(event.name.lowercase())) {
                return event
            }
        }

        // 2. Check famous known locations
        for ((key, geo) in KNOWN_LOCATIONS) {
            if (normalized.contains(key)) {
                return geo
            }
        }

        // 3. Deterministic hash resolver for ANY inputted string
        // This ensures random user input resolves to beautiful, stable coordinates within Greater São Paulo.
        val hash = input.hashCode().toDouble()
        // Center of São Paulo coordinates: Lat -23.5505, Lng -46.6333
        // Displace coordinate based on hash to within 15km radius
        val latDisplacement = (sin(hash) * 0.12)
        val lngDisplacement = (cos(hash) * 0.15)
        
        val resolvedLat = -23.5505 + latDisplacement
        val resolvedLng = -46.6333 + lngDisplacement

        return GeoPoint(resolvedLat, resolvedLng, input)
    }

    // Great-Circle (Haversine) Distance in Kilometers
    fun calculateDistanceKm(p1: GeoPoint, p2: GeoPoint): Double {
        val earhRadius = 6371.0
        val dLat = Math.toRadians(p2.latitude - p1.latitude)
        val dLng = Math.toRadians(p2.longitude - p1.longitude)
        
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(p1.latitude)) * cos(Math.toRadians(p2.latitude)) *
                sin(dLng / 2).pow(2)
                
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val directDistance = earhRadius * c
        
        // Apply city routing coefficient (real route is typically ~25% to 35% longer than direct path)
        return max(1.5, round(directDistance * 1.28 * 10) / 10.0)
    }

    // Estima tempo em minutos baseado na velocidade média do tráfego urbano de SP
    fun estimateTimeMinutes(distanceKm: Double): Int {
        // Average speed: ~32 km/h
        val rawTime = (distanceKm / 32.0) * 60.0
        // Add 6 minutes offset for traffic lights/stops
        return max(4, (rawTime + 6.0).toInt())
    }
}
