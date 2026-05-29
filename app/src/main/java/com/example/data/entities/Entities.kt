package com.example.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class Profile(
    @PrimaryKey val id: String, // e.g. "passageiro_demo", "motorista_admin", "motorista_parceiro"
    val name: String,
    val email: String,
    val role: String, // "passageiro", "admin", "parceiro"
    val avatarUrl: String? = null
)

@Entity(tableName = "driver_settings")
data class DriverSettings(
    @PrimaryKey val id: Int = 1, // Global single setting
    val minPrice: Double = 25.0,
    val pricePerKm: Double = 4.5,
    val roundTripFee: Double = 15.0,
    val waitFee: Double = 10.0,
    val nightFee: Double = 12.0,
    val demandFee: Double = 20.0,
    val maxPassengers: Int = 4,
    val waitToleranceMinutes: Int = 15,
    val cancellationRules: String = "Cancelamentos com menos de 2h de antecedência terão taxa de 50%. Para viagens noturnas, reembolsos apenas com 12h de antecedência.",
    val prepaymentRules: String = "É necessário um sinal antecipado de 50% via Pix para reserva formal de datas."
)

@Entity(tableName = "driver_status")
data class DriverStatus(
    @PrimaryKey val id: Int = 1, // Single global status
    val status: String = "online", // "online", "offline", "ocupado"
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "ride_requests")
data class RideRequest(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val passengerName: String,
    val passengerEmail: String,
    val origin: String,
    val destination: String,
    val dateTime: String,
    val tripType: String, // "ida", "volta", "ida_volta"
    val passengerCount: Int,
    val notes: String = "",
    val status: String = "pendente", // "pendente", "enviada", "aprovada", "recusada", "confirmada", "concluída", "cancelada"
    val assignedDriverId: String? = null,
    val assignedDriverName: String? = null,
    val cancelReason: String? = null,
    val priceEstimate: Double = 0.0,
    val finalPrice: Double = 0.0,
    val estimatedKm: Double = 12.0,
    val paymentConfirmed: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "ride_messages")
data class RideMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val requestId: Int,
    val senderId: String,
    val senderName: String,
    val senderRole: String, // "passageiro", "admin", "parceiro"
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "driver_partners")
data class DriverPartner(
    @PrimaryKey val id: String,
    val name: String,
    val status: String = "ativo", // "ativo", "inativo"
    val rating: Double = 5.0,
    val email: String,
    val phone: String = ""
)

@Entity(tableName = "driver_live_locations")
data class DriverLiveLocation(
    @PrimaryKey val driverId: String,
    val driverName: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val requestId: Int,
    val status: String // "a_caminho", "chegou", "em_viagem"
)

