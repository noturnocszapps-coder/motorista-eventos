package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.entities.*
import com.example.data.repository.MapRoutingService
import com.example.data.repository.RoxouRepository
import com.example.data.repository.SupabaseService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RoxouViewModel(private val repository: RoxouRepository) : ViewModel() {

    private val TAG = "RoxouViewModel"

    // Manage active simulation user
    private val _currentUser = MutableStateFlow<Profile?>(null)
    val currentUser: StateFlow<Profile?> = _currentUser.asStateFlow()

    // Configuration pricing and driver online state
    val driverSettings: StateFlow<DriverSettings?> = repository.driverSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DriverSettings())

    val driverStatus: StateFlow<DriverStatus?> = repository.driverStatus
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DriverStatus(status = "online"))

    val allRequests: StateFlow<List<RideRequest>> = repository.allRequests
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val partners: StateFlow<List<DriverPartner>> = repository.partners
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active live tracking simulator jobs mapped by Ride ID
    private val activeLiveJobs = mutableMapOf<String, Job>()

    // Active single background chat sync job (for the currently opened conversation screen)
    private var activeChatJob: Pair<String, Job>? = null

    // Filter requests reactively based on active user
    val activeUserRequests: StateFlow<List<RideRequest>> = combine(
        allRequests,
        _currentUser
    ) { requests, user ->
        when {
            user == null -> emptyList()
            user.role == "admin" -> requests
            user.role == "parceiro" || user.role == "driver" -> requests // admin and drivers see all requests temporarily while there's only one driver
            else -> requests.filter { it.passengerEmail.trim().lowercase() == user.email.trim().lowercase() }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            // Boot values
            repository.ensurePrepopulatedData()
            
            // Set default login profile to passenger initially
            _currentUser.value = Profile("passageiro_id", "Maurício Souza", "mauricio@gmail.com", "passageiro")

            // Realtime / Sync looping
            launch {
                while (true) {
                    try {
                        repository.syncWithSupabase()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e(TAG, "Periodic sync error: ${e.message}")
                    }
                    delay(3000)
                }
            }
        }
    }

    // Swapping profile identities during simulation / real auth
    fun selectProfile(id: String, name: String, email: String, role: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val success = SupabaseService.signUpOrSignIn(email, name, role)
            if (success) {
                val finalId = SupabaseService.sessionUserId ?: id
                val finalRole = SupabaseService.sessionUserRole ?: role
                val finalName = SupabaseService.sessionUserName ?: name
                val profile = Profile(finalId, finalName, email, finalRole)
                repository.insertProfile(profile)
                _currentUser.value = profile
                onComplete(true)
            } else {
                onComplete(false)
            }
        }
    }

    fun updateDriverStatus(status: String) {
        viewModelScope.launch {
            repository.updateStatus(status)
        }
    }

    fun submitRequest(
        origin: String,
        destination: String,
        dateTime: String,
        tripType: String,
        passengerCount: Int,
        notes: String,
        estimatedDistance: Double
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.requestQuote(
                passengerName = user.name,
                passengerEmail = user.email,
                origin = origin,
                destination = destination,
                dateTime = dateTime,
                tripType = tripType,
                passengerCount = passengerCount,
                notes = notes,
                estimatedDistance = estimatedDistance
            )
        }
    }

    fun approveQuote(id: String, finalPrice: Double) {
        viewModelScope.launch {
            repository.approveQuote(id, finalPrice)
            // Add automated system messages to assist UX
            val text = "Orçamento APROVADO pelo motorista! Valor final de R$ %.2f confirmado.".format(finalPrice)
            repository.sendMessage(id, "admin_id", "Rax", "admin", text)
        }
    }

    fun rejectQuote(id: String, reason: String) {
        viewModelScope.launch {
            repository.rejectQuote(id, reason)
            val text = "Orçamento recusado pelo seguinte motivo: $reason"
            repository.sendMessage(id, "admin_id", "Rax", "admin", text)
        }
    }

    fun updateRequestStatus(id: String, status: String) {
        viewModelScope.launch {
            repository.updateRequestStatus(id, status)
            val text = when (status) {
                "confirmada" -> "Sua agenda está confirmada! Prepare-se para a partida."
                "concluída" -> "Viagem finalizada com sucesso! Agradecemos a preferência."
                "cancelada" -> "Esta reserva foi cancelada pelo motorista."
                else -> "O status da viagem foi alterado para: ${status.uppercase()}."
            }
            repository.sendMessage(id, "admin_id", "Rax", "admin", text)

            // Dynamic live tracking loop trigger
            manageLiveTrackingLoop(id, status)
        }
    }

    fun setPaymentConfirmed(id: String, confirmed: Boolean) {
        viewModelScope.launch {
            repository.confirmPayment(id, confirmed)
            if (confirmed) {
                repository.sendMessage(id, "admin_id", "Rax", "admin", "Sinal de pagamento identificado e confirmado!")
            }
        }
    }

    fun assignDriver(id: String, driver: DriverPartner?) {
        viewModelScope.launch {
            repository.assignDriver(id, driver?.id, driver?.name)
            val msg = if (driver != null) {
                "Motorista parceiro ${driver.name} designado para atender sua reserva."
            } else {
                "Viagem designada de volta ao motorista principal."
            }
            repository.sendMessage(id, "admin_id", "Rax", "admin", msg)
        }
    }

    fun sendChatMessage(requestId: String, message: String) {
        val user = _currentUser.value ?: return
        if (message.isBlank()) return
        viewModelScope.launch {
            repository.sendMessage(
                requestId = requestId,
                senderId = user.id,
                senderName = user.name,
                senderRole = user.role,
                messageText = message
            )
        }
    }

    fun updateDriverSettings(settings: DriverSettings) {
        viewModelScope.launch {
            repository.updateSettings(settings)
        }
    }

    fun addPartnerDriver(name: String, email: String, phone: String) {
        viewModelScope.launch {
            val id = "partner_" + System.currentTimeMillis()
            val partner = DriverPartner(
                id = id,
                name = name,
                email = email,
                phone = phone,
                status = "ativo",
                rating = 5.0
            )
            repository.insertPartner(partner)
        }
    }

    fun removePartnerDriver(id: String) {
        viewModelScope.launch {
            repository.deletePartner(id)
        }
    }

    // Expose message flow for active chat detail (with periodic remote synchronization)
    fun getMessagesForRide(requestId: String): Flow<List<RideMessage>> {
        val currentJob = activeChatJob
        if (currentJob == null || currentJob.first != requestId) {
            currentJob?.second?.cancel()
            val job = viewModelScope.launch {
                while (true) {
                    try {
                        repository.syncChatMessages(requestId)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e(TAG, "Chat sync loop error: ${e.message}")
                    }
                    delay(3000)
                }
            }
            activeChatJob = Pair(requestId, job)
        }
        return repository.getMessagesForRide(requestId)
    }

    // Stop active background chat message synchronization loop when conversation screen is closed/disposed
    fun stopChatSync(requestId: String) {
        val currentJob = activeChatJob
        if (currentJob != null && currentJob.first == requestId) {
            currentJob.second.cancel()
            activeChatJob = null
            Log.i(TAG, "Chat sync loop successfully stopped and cancelled for request: $requestId")
        }
    }

    fun getRequestById(id: String): Flow<RideRequest?> {
        return repository.getRequestById(id)
    }

    fun getLiveLocationForRide(requestId: String): Flow<DriverLiveLocation?> {
        return repository.getLiveLocationForRide(requestId)
    }

    // Live price estimative calculation helper
    fun calculateLiveEstimate(distance: Double, tripType: String, passengers: Int): Double {
        val settings = driverSettings.value ?: DriverSettings()
        return repository.calculateEstimate(distance, settings, tripType, passengers)
    }

    // 4. SISTEMA DE RASTREAMENTO AO VIVO (SIMULADOR DINÂMICO CENTRAL)
    private fun manageLiveTrackingLoop(requestId: String, status: String) {
        // Cancel existing job for this request if any
        activeLiveJobs[requestId]?.cancel()
        activeLiveJobs.remove(requestId)

        if (status == "confirmada") {
            // Trigger periodic location tracking (only during active booked flights/trips)
            val job = viewModelScope.launch {
                val requestModel = allRequests.value.firstOrNull { it.id == requestId } ?: return@launch
                val originGeo = MapRoutingService.geocodeLocation(requestModel.origin)
                val destGeo = MapRoutingService.geocodeLocation(requestModel.destination)
                
                val driverId = requestModel.assignedDriverId ?: "admin_id"
                val driverName = requestModel.assignedDriverName ?: "Rax (Você - Motorista)"

                // Step 1: Simulate driver a_caminho (moving to passenger)
                for (step in 0..5) {
                    val progress = step / 5.0
                    val interpolatedLat = MapRoutingService.CURRENT_USER_LOCATION.latitude + 
                            (originGeo.latitude - MapRoutingService.CURRENT_USER_LOCATION.latitude) * progress
                    val interpolatedLng = MapRoutingService.CURRENT_USER_LOCATION.longitude + 
                            (originGeo.longitude - MapRoutingService.CURRENT_USER_LOCATION.longitude) * progress
                    
                    val liveLoc = DriverLiveLocation(
                        driverId = driverId,
                        driverName = driverName,
                        latitude = interpolatedLat,
                        longitude = interpolatedLng,
                        timestamp = System.currentTimeMillis(),
                        requestId = requestId,
                        status = "a_caminho"
                    )
                    repository.updateDriverLiveLocation(liveLoc)
                    delay(5000)
                }

                // Step 2: Driver arrived (chegou) at passenger's starting point
                val arrivedLoc = DriverLiveLocation(
                    driverId = driverId,
                    driverName = driverName,
                    latitude = originGeo.latitude,
                    longitude = originGeo.longitude,
                    timestamp = System.currentTimeMillis(),
                    requestId = requestId,
                    status = "chegou"
                )
                repository.updateDriverLiveLocation(arrivedLoc)
                delay(6000)

                // Step 3: Driver em_viagem (conduzindo para o destino)
                for (step in 0..6) {
                    val progress = step / 6.0
                    val interpolatedLat = originGeo.latitude + (destGeo.latitude - originGeo.latitude) * progress
                    val interpolatedLng = originGeo.longitude + (destGeo.longitude - originGeo.longitude) * progress
                    
                    val liveLoc = DriverLiveLocation(
                        driverId = driverId,
                        driverName = driverName,
                        latitude = interpolatedLat,
                        longitude = interpolatedLng,
                        timestamp = System.currentTimeMillis(),
                        requestId = requestId,
                        status = "em_viagem"
                    )
                    repository.updateDriverLiveLocation(liveLoc)
                    delay(5000)
                }
            }
            activeLiveJobs[requestId] = job
        } else if (status == "concluída" || status == "cancelada" || status == "recusada") {
            // Clean up of location tracker strictly
            viewModelScope.launch {
                repository.deleteLiveLocation(requestId)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        activeLiveJobs.values.forEach { it.cancel() }
        activeLiveJobs.clear()
        activeChatJob?.second?.cancel()
        activeChatJob = null
    }
}
