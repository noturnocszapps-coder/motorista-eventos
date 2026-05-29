package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RoxouDao {

    // Profiles
    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    fun getProfile(id: String): Flow<Profile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: Profile)

    // Driver Settings
    @Query("SELECT * FROM driver_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<DriverSettings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: DriverSettings)

    // Driver Status
    @Query("SELECT * FROM driver_status WHERE id = 1 LIMIT 1")
    fun getStatus(): Flow<DriverStatus?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatus(status: DriverStatus)

    // Ride Requests
    @Query("SELECT * FROM ride_requests ORDER BY timestamp DESC")
    fun getAllRequests(): Flow<List<RideRequest>>

    @Query("SELECT * FROM ride_requests WHERE passengerEmail = :passengerEmail ORDER BY timestamp DESC")
    fun getRequestsForPassenger(passengerEmail: String): Flow<List<RideRequest>>

    @Query("SELECT * FROM ride_requests WHERE assignedDriverId = :driverId ORDER BY timestamp DESC")
    fun getRequestsForDriver(driverId: String): Flow<List<RideRequest>>

    @Query("SELECT * FROM ride_requests WHERE id = :id LIMIT 1")
    fun getRequestById(id: String): Flow<RideRequest?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRideRequest(request: RideRequest)

    @Update
    suspend fun updateRideRequest(request: RideRequest)

    @Query("UPDATE ride_requests SET status = :status WHERE id = :id")
    suspend fun updateRequestStatus(id: String, status: String)

    @Query("UPDATE ride_requests SET status = :status, finalPrice = :price WHERE id = :id")
    suspend fun approveRequest(id: String, status: String, price: Double)

    @Query("UPDATE ride_requests SET status = :status, cancelReason = :reason WHERE id = :id")
    suspend fun rejectRequest(id: String, status: String, reason: String)

    @Query("UPDATE ride_requests SET paymentConfirmed = :confirmed WHERE id = :id")
    suspend fun updatePaymentStatus(id: String, confirmed: Boolean)

    @Query("UPDATE ride_requests SET assignedDriverId = :driverId, assignedDriverName = :driverName WHERE id = :id")
    suspend fun assignDriver(id: String, driverId: String?, driverName: String?)

    // Ride Messages (Chat)
    @Query("SELECT * FROM ride_messages WHERE requestId = :requestId ORDER BY timestamp ASC")
    fun getMessagesForRide(requestId: String): Flow<List<RideMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: RideMessage)

    // Driver Partners
    @Query("SELECT * FROM driver_partners ORDER BY name ASC")
    fun getPartners(): Flow<List<DriverPartner>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPartner(partner: DriverPartner)

    @Query("DELETE FROM driver_partners WHERE id = :id")
    suspend fun deletePartner(id: String)

    // Driver Live Locations
    @Query("SELECT * FROM driver_live_locations WHERE requestId = :requestId LIMIT 1")
    fun getLiveLocationForRide(requestId: String): Flow<DriverLiveLocation?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLiveLocation(location: DriverLiveLocation)

    @Query("DELETE FROM driver_live_locations WHERE requestId = :requestId")
    suspend fun deleteLiveLocation(requestId: String)

    @Query("DELETE FROM driver_live_locations WHERE driverId = :driverId")
    suspend fun deleteLiveLocationForDriver(driverId: String)
}
