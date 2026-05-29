package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.dao.RoxouDao
import com.example.data.entities.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlin.math.max

class RoxouRepository(
    private val dao: RoxouDao,
    private val context: Context
) {
    private val TAG = "RoxouRepository"
    private val repoScope = CoroutineScope(Dispatchers.IO)

    val allRequests: Flow<List<RideRequest>> = dao.getAllRequests()
    val driverSettings: Flow<DriverSettings?> = dao.getSettings()
    val driverStatus: Flow<DriverStatus?> = dao.getStatus()
    val partners: Flow<List<DriverPartner>> = dao.getPartners()

    fun getProfile(id: String): Flow<Profile?> = dao.getProfile(id)

    fun getRequestsForPassenger(passengerEmail: String): Flow<List<RideRequest>> =
        dao.getRequestsForPassenger(passengerEmail)

    fun getRequestsForDriver(driverId: String): Flow<List<RideRequest>> =
        dao.getRequestsForDriver(driverId)

    fun getRequestById(id: String): Flow<RideRequest?> = dao.getRequestById(id)

    fun getMessagesForRide(requestId: String): Flow<List<RideMessage>> = dao.getMessagesForRide(requestId)

    fun getLiveLocationForRide(requestId: String): Flow<DriverLiveLocation?> = dao.getLiveLocationForRide(requestId)

    suspend fun insertProfile(profile: Profile) {
        dao.insertProfile(profile)
    }

    suspend fun updateSettings(settings: DriverSettings) {
        dao.insertSettings(settings)
    }

    // ONLINE SUPABASE DATABASE SYNC
    suspend fun syncWithSupabase() {
        if (!SupabaseService.isConfigured) return
        try {
            // 1. Sync All Requests
            val remoteRequests = SupabaseService.fetchAllRideRequests()
            if (remoteRequests.isNotEmpty()) {
                remoteRequests.forEach { req ->
                    dao.insertRideRequest(req)
                }
            }

            // 2. Sync Driver Status
            val remoteDriverStatus = SupabaseService.fetchLatestDriverStatus()
            dao.insertStatus(DriverStatus(status = remoteDriverStatus, lastUpdated = System.currentTimeMillis()))
        } catch (e: Exception) {
            Log.e(TAG, "syncWithSupabase generic error: ${e.message}")
        }
    }

    // ONLINE CHAT SYNC
    suspend fun syncChatMessages(rideId: String) {
        if (!SupabaseService.isConfigured) return
        try {
            val remoteChats = SupabaseService.fetchRideMessages(rideId)
            if (remoteChats.isNotEmpty()) {
                remoteChats.forEach { msg ->
                    dao.insertMessage(msg)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "syncChatMessages error for ride $rideId: ${e.message}")
        }
    }

    suspend fun updateStatus(status: String) {
        dao.insertStatus(DriverStatus(status = status, lastUpdated = System.currentTimeMillis()))
        repoScope.launch {
            if (SupabaseService.isConfigured) {
                SupabaseService.updateDriverStatus(status)
            }
        }
    }

    // Live tracking updates & notifications
    suspend fun updateDriverLiveLocation(location: DriverLiveLocation) {
        // Save to Room Database local cache
        dao.insertLiveLocation(location)

        // Upload/Synchronize to Supabase if configured
        repoScope.launch {
            val success = SupabaseService.updateDriverLiveLocation(location)
            Log.d(TAG, "Driver Live location uploaded to Supabase: $success")
        }

        // Fire notifications based on live status updates
        when (location.status) {
            "a_caminho" -> RoxouNotificationManager.sendPushNotification(
                context,
                "Motorista a caminho!",
                "Seu motorista premium, ${location.driverName}, iniciou o percurso para seu ponto de origem!"
            )
            "chegou" -> RoxouNotificationManager.sendPushNotification(
                context,
                "Motorista chegou!",
                "${location.driverName} acaba de chegar na sua localização de embarque. Aguardando você."
            )
            "em_viagem" -> Log.i(TAG, "Ride is actively drawing route to destination.")
        }
    }

    suspend fun deleteLiveLocation(requestId: String) {
        dao.deleteLiveLocation(requestId)
        repoScope.launch {
            SupabaseService.deleteDriverLiveLocation(requestId)
        }
    }

    suspend fun requestQuote(
        passengerName: String,
        passengerEmail: String,
        origin: String,
        destination: String,
        dateTime: String,
        tripType: String,
        passengerCount: Int,
        notes: String,
        estimatedDistance: Double
    ): String {
        // Fetch active settings or use defaults
        val activeSettings = dao.getSettings().firstOrNull() ?: DriverSettings()
        
        // Calculate the pricing estimate
        val estimateValue = calculateEstimate(
            km = estimatedDistance,
            settings = activeSettings,
            tripType = tripType,
            passengerCount = passengerCount
        )

        val newId = java.util.UUID.randomUUID().toString()

        val newRequest = RideRequest(
            id = newId,
            passengerName = passengerName,
            passengerEmail = passengerEmail,
            origin = origin,
            destination = destination,
            dateTime = dateTime,
            tripType = tripType,
            passengerCount = passengerCount,
            notes = notes,
            status = "pendente",
            priceEstimate = estimateValue,
            finalPrice = estimateValue, // Initially final price equals estimate
            estimatedKm = estimatedDistance
        )

        dao.insertRideRequest(newRequest)

        // Dynamic Supabase insertion & Push notification
        repoScope.launch {
            SupabaseService.insertRideRequest(newRequest)
        }

        RoxouNotificationManager.sendPushNotification(
            context,
            "Novo Orçamento Recebido!",
            "Há uma nova solicitação pendente de $passengerName de $origin para $destination."
        )

        return newId
    }

    suspend fun updateRequestStatus(id: String, status: String) {
        dao.updateRequestStatus(id, status)
        
        repoScope.launch {
            SupabaseService.updateRideRequestStatus(id, status)
            
            // Send corresponding user notification
            when (status) {
                "confirmada" -> RoxouNotificationManager.sendPushNotification(
                    context,
                    "Reserva Confirmada!",
                    "Excelente! Sua viagem foi confirmada de forma definitiva. O condutor encontra-se a postos!"
                )
                "concluída" -> {
                    // Automatically stop location tracking
                    deleteLiveLocation(id)
                    RoxouNotificationManager.sendPushNotification(
                        context,
                        "Obrigado por viajar conosco!",
                        "Sua viagem VIP Reserva Roxou foi concluída. Avalie seu motorista!"
                    )
                }
                "cancelada" -> {
                    deleteLiveLocation(id)
                    RoxouNotificationManager.sendPushNotification(
                        context,
                        "Viagem Cancelada",
                        "Informativo: Esta agenda de viagem foi cancelada."
                    )
                }
            }
        }
    }

    suspend fun approveQuote(id: String, finalPrice: Double) {
        dao.approveRequest(id, "aprovada", finalPrice)
        
        repoScope.launch {
            SupabaseService.approveRideRequest(id, "aprovada", finalPrice)
            RoxouNotificationManager.sendPushNotification(
                context,
                "Seu Orçamento foi Aprovado!",
                "Seu orçamento foi precificado em R$ %.2f. Efetue o sinal Pix para garantir sua agenda.".format(finalPrice)
            )
        }
    }

    suspend fun rejectQuote(id: String, reason: String) {
        dao.rejectRequest(id, "recusada", reason)
        
        repoScope.launch {
            SupabaseService.rejectRideRequest(id, "recusada", reason)
            RoxouNotificationManager.sendPushNotification(
                context,
                "Solicitação de Orçamento Recusada",
                "Seu orçamento foi recusado. Motivo: $reason"
            )
        }
    }

    suspend fun confirmPayment(id: String, confirmed: Boolean) {
        dao.updatePaymentStatus(id, confirmed)
        repoScope.launch {
            SupabaseService.updateRidePaymentStatus(id, confirmed)
            if (confirmed) {
                RoxouNotificationManager.sendPushNotification(
                    context,
                    "Pagamento Confirmado!",
                    "O sinal de pagamento de 50% foi identificado com sucesso."
                )
            }
        }
    }

    suspend fun assignDriver(id: String, driverId: String?, driverName: String?) {
        dao.assignDriver(id, driverId, driverName)
        repoScope.launch {
            SupabaseService.assignDriverToRide(id, driverId, driverName)
        }
    }

    suspend fun sendMessage(requestId: String, senderId: String, senderName: String, senderRole: String, messageText: String) {
        val msg = RideMessage(
            requestId = requestId,
            senderId = senderId,
            senderName = senderName,
            senderRole = senderRole,
            message = messageText
        )
        dao.insertMessage(msg)

        repoScope.launch {
            SupabaseService.insertRideMessage(msg)
        }

        RoxouNotificationManager.sendPushNotification(
            context,
            "Nova mensagem de $senderName",
            messageText
        )
    }

    suspend fun insertPartner(partner: DriverPartner) = dao.insertPartner(partner)

    suspend fun deletePartner(id: String) = dao.deletePartner(id)

    // Pricing estimation algorithm strictly matching specified client preferences
    fun calculateEstimate(km: Double, settings: DriverSettings, tripType: String, passengerCount: Int): Double {
        val factor = if (tripType == "ida_volta") 2.0 else 1.0
        val effectiveKm = km * factor
        val pricePerKm = 2.50
        val bookingFee = 20.00
        val minPrice = 30.00
        
        val rawValue = (effectiveKm * pricePerKm) + bookingFee
        return if (rawValue < minPrice) minPrice else rawValue
    }

    // High fidelity data bootstrapping
    suspend fun ensurePrepopulatedData() {
        val currentSettings = dao.getSettings().firstOrNull()
        if (currentSettings == null) {
            // Seed Settings
            dao.insertSettings(DriverSettings())
            // Seed Status
            dao.insertStatus(DriverStatus(status = "online"))
            
            // Seed Only Requested Admin Profile
            dao.insertProfile(Profile("admin_id", "Administrador Roxou", "contato.fh3@gmail.com", "admin"))
        }
    }
}
