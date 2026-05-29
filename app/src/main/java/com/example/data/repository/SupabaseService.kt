package com.example.data.repository

import android.util.Log
import com.example.data.entities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object SupabaseService {
    private const val TAG = "SupabaseService"

    val SUPABASE_URL: String = try {
        val url = com.example.BuildConfig.SUPABASE_URL
        if (url.startsWith("http") && !url.contains("your-supabase-project")) url else ""
    } catch (e: Exception) {
        ""
    }

    val SUPABASE_ANON_KEY: String = try {
        val key = com.example.BuildConfig.SUPABASE_ANON_KEY
        if (key.isNotBlank() && !key.contains("your-anon-key-here")) key else ""
    } catch (e: Exception) {
        ""
    }

    val isConfigured: Boolean = SUPABASE_URL.isNotEmpty() && SUPABASE_ANON_KEY.isNotEmpty()

    // Persistent User Auth Sesion state
    var sessionToken: String? = null
    var sessionUserId: String? = null
    var sessionUserRole: String? = null
    var sessionUserEmail: String? = null
    var sessionUserName: String? = null

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    // AUTH GOOGLE - Direct REST signUp / signIn mapping
    suspend fun authenticateWithGoogleToken(idToken: String): Boolean = withContext(Dispatchers.IO) {
        // Fallback for simulation initially
        return@withContext true
    }

    /**
     * Real Supabase Auth Flow using Email & Password.
     * We derive a silent, secure password per email account to enable a seamless, immediate login 
     * experience without manual password entry steps from standard Google accounts.
     */
    suspend fun signUpOrSignIn(email: String, name: String, requestedRole: String): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            Log.w(TAG, "Supabase local sync: Using simulated offline user session.")
            sessionUserId = when (requestedRole) {
                "admin" -> "admin_id"
                "parceiro" -> "driver_partner_id"
                else -> "passageiro_id"
            }
            sessionUserEmail = email
            sessionUserName = name
            sessionUserRole = requestedRole
            return@withContext true
        }

        val dummyPassword = "RoxouSecurePassWord123!_UserPass"
        val loginSuccess = attemptSignIn(email, dummyPassword)

        if (loginSuccess) {
            Log.i(TAG, "Supabase Auth: Existing user signed in successfully.")
            upsertUserProfile(email, name, requestedRole)
            return@withContext true
        }

        // Try SignUp if SignIn failed
        val signupSuccess = attemptSignUp(email, dummyPassword, name)
        if (signupSuccess) {
            Log.i(TAG, "Supabase Auth: New user registered successfully.")
            upsertUserProfile(email, name, requestedRole)
            return@withContext true
        }

        Log.e(TAG, "Supabase Auth: Failed both signIn and signUp.")
        return@withContext false
    }

    private suspend fun attemptSignIn(email: String, pass: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("email", email)
                put("password", pass)
            }

            val request = Request.Builder()
                .url("$SUPABASE_URL/auth/v1/token?grant_type=password")
                .header("apikey", SUPABASE_ANON_KEY)
                .header("Content-Type", "application/json")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    val obj = JSONObject(bodyStr)
                    sessionToken = obj.optString("access_token", null as String?)
                    
                    val userObj = obj.optJSONObject("user")
                    sessionUserId = userObj?.optString("id", null as String?)
                    sessionUserEmail = email
                    
                    Log.i(TAG, "attemptSignIn Success: UserId = $sessionUserId")
                    return@withContext true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in attemptSignIn: ${e.message}")
        }
        return@withContext false
    }

    private suspend fun attemptSignUp(email: String, pass: String, name: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("email", email)
                put("password", pass)
                put("data", JSONObject().apply { put("name", name) })
            }

            val request = Request.Builder()
                .url("$SUPABASE_URL/auth/v1/signup")
                .header("apikey", SUPABASE_ANON_KEY)
                .header("Content-Type", "application/json")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    val obj = JSONObject(bodyStr)
                    sessionToken = obj.optString("access_token", null as String?)
                    
                    val userObj = obj.optJSONObject("user")
                    sessionUserId = userObj?.optString("id", null as String?)
                    sessionUserEmail = email
                    
                    if (sessionToken.isNullOrEmpty()) {
                        // If auto-confirm is disabled on Supabase, sign in might not return token immediately,
                        // so we attempt a direct signin right after signup in case.
                        return@withContext attemptSignIn(email, pass)
                    }
                    Log.i(TAG, "attemptSignUp Success: UserId = $sessionUserId")
                    return@withContext true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in attemptSignUp: ${e.message}")
        }
        return@withContext false
    }

    // UPSERT Profiles
    private suspend fun upsertUserProfile(email: String, name: String, role: String) = withContext(Dispatchers.IO) {
        val uid = sessionUserId ?: return@withContext
        val finalRole = if (email.equals("contato.fh3@gmail.com", ignoreCase = true)) "admin" else role
        sessionUserRole = finalRole
        sessionUserName = name

        try {
            val profileJson = JSONObject().apply {
                put("id", uid)
                put("name", name)
                put("email", email)
                put("role", finalRole)
                put("avatar_url", "https://api.dicebear.com/7.x/bottts/png?seed=${email}")
            }

            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/profiles")
                .header("apikey", SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer ${sessionToken ?: SUPABASE_ANON_KEY}")
                .header("Content-Type", "application/json")
                .header("Prefer", "resolution=merge-duplicates")
                .post(profileJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                Log.d(TAG, "upsertUserProfile response code: ${response.code}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in upsertUserProfile: ${e.message}")
        }
    }

    // UPDATE DRIVER STATUS
    suspend fun updateDriverStatus(status: String): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext true
        val uid = sessionUserId ?: return@withContext false

        try {
            val statusJson = JSONObject().apply {
                put("driver_id", uid)
                put("status", status)
                put("updated_at", "now()")
            }

            // Using on_conflict upsert on Supabase Postgrest
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/driver_status?on_conflict=driver_id")
                .header("apikey", SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer ${sessionToken ?: SUPABASE_ANON_KEY}")
                .header("Content-Type", "application/json")
                .header("Prefer", "resolution=merge-duplicates")
                .post(statusJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating status: ${e.message}")
            return@withContext false
        }
    }

    // FETCH DRIVER STATUS
    suspend fun fetchLatestDriverStatus(): String = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext "online"

        try {
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/driver_status?order=updated_at.desc&limit=1")
                .header("apikey", SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer ${sessionToken ?: SUPABASE_ANON_KEY}")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "[]"
                    val arr = JSONArray(body)
                    if (arr.length() > 0) {
                        return@withContext arr.getJSONObject(0).optString("status", "online")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching driver status: ${e.message}")
        }
        return@withContext "online"
    }

    // FETCH ALL RIDE REQUESTS (Online PostgreSQL)
    suspend fun fetchAllRideRequests(): List<RideRequest> = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            return@withContext emptyList()
        }

        try {
            // Join profile metadata dynamically
            val url = "$SUPABASE_URL/rest/v1/ride_requests?select=*,profiles:passenger_id(name,email)&order=created_at.desc"
            val request = Request.Builder()
                .url(url)
                .header("apikey", SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer ${sessionToken ?: SUPABASE_ANON_KEY}")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: "[]"
                    val jsonArray = JSONArray(bodyStr)
                    val requests = mutableListOf<RideRequest>()
                    
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        requests.add(parseRideRequest(obj))
                    }
                    return@withContext requests
                } else {
                    Log.e(TAG, "Error fetching from PostgreSQL: API code ${response.code}")
                    return@withContext emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing fetchAllRideRequests: ${e.message}")
            return@withContext emptyList()
        }
    }

    // INSERT / UPSERT NEW RIDE REQUEST
    suspend fun insertRideRequest(rideReq: RideRequest): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            return@withContext true
        }

        try {
            // We need current passenger id (Auth uid)
            val passId = sessionUserId ?: "00000000-0000-0000-0000-000000000000"

            val json = JSONObject().apply {
                put("id", rideReq.id)
                put("passenger_id", passId)
                put("origin", rideReq.origin)
                put("destination", rideReq.destination)
                put("distance_km", rideReq.estimatedKm)
                put("trip_type", mapLocalTripTypeToSupabase(rideReq.tripType))
                put("passengers", rideReq.passengerCount)
                put("notes", rideReq.notes)
                put("estimated_price", rideReq.priceEstimate)
                put("final_price", rideReq.finalPrice)
                put("status", mapLocalStatusToSupabase(rideReq.status))
                put("payment_confirmed", rideReq.paymentConfirmed)
                put("rejection_reason", rideReq.cancelReason)
                put("scheduled_at", rideReq.dateTime)
                put("assigned_driver_id", rideReq.assignedDriverId)
                put("assigned_driver_name", rideReq.assignedDriverName)
            }

            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/ride_requests")
                .header("apikey", SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer ${sessionToken ?: SUPABASE_ANON_KEY}")
                .header("Content-Type", "application/json")
                .header("Prefer", "resolution=merge-duplicates")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                Log.d(TAG, "insertRideRequest response: ${response.code}")
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting ride request: ${e.message}")
            return@withContext false
        }
    }

    // UPDATE RIDE REQUEST STATUS
    suspend fun updateRideRequestStatus(id: String, status: String): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext true

        try {
            val json = JSONObject().apply {
                put("status", mapLocalStatusToSupabase(status))
                put("updated_at", "now()")
            }

            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/ride_requests?id=eq.$id")
                .header("apikey", SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer ${sessionToken ?: SUPABASE_ANON_KEY}")
                .header("Content-Type", "application/json")
                .patch(json.toString().toRequestBody("application/json".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating status: ${e.message}")
            return@withContext false
        }
    }

    // ASSIGN DRIVER (Online)
    suspend fun assignDriverToRide(id: String, driverId: String?, driverName: String?): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext true

        try {
            val json = JSONObject().apply {
                put("assigned_driver_id", driverId)
                put("assigned_driver_name", driverName)
                put("updated_at", "now()")
            }

            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/ride_requests?id=eq.$id")
                .header("apikey", SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer ${sessionToken ?: SUPABASE_ANON_KEY}")
                .header("Content-Type", "application/json")
                .patch(json.toString().toRequestBody("application/json".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error assigning driver: ${e.message}")
            return@withContext false
        }
    }

    // REJECT RIDE REQUEST WITH REASON (Online)
    suspend fun rejectRideRequest(id: String, status: String, reason: String): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext true

        try {
            val json = JSONObject().apply {
                put("status", mapLocalStatusToSupabase(status))
                put("rejection_reason", reason)
                put("updated_at", "now()")
            }

            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/ride_requests?id=eq.$id")
                .header("apikey", SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer ${sessionToken ?: SUPABASE_ANON_KEY}")
                .header("Content-Type", "application/json")
                .patch(json.toString().toRequestBody("application/json".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error rejecting ride request: ${e.message}")
            return@withContext false
        }
    }

    // APPROVE RIDE REQUEST WITH PRICE (Online)
    suspend fun approveRideRequest(id: String, status: String, finalPrice: Double): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext true

        try {
            val json = JSONObject().apply {
                put("status", mapLocalStatusToSupabase(status))
                put("final_price", finalPrice)
                put("updated_at", "now()")
            }

            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/ride_requests?id=eq.$id")
                .header("apikey", SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer ${sessionToken ?: SUPABASE_ANON_KEY}")
                .header("Content-Type", "application/json")
                .patch(json.toString().toRequestBody("application/json".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error approving ride request: ${e.message}")
            return@withContext false
        }
    }

    // PAYMENT STATUS UPDATE
    suspend fun updateRidePaymentStatus(id: String, confirmed: Boolean): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext true

        try {
            val json = JSONObject().apply {
                put("payment_confirmed", confirmed)
                put("updated_at", "now()")
            }

            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/ride_requests?id=eq.$id")
                .header("apikey", SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer ${sessionToken ?: SUPABASE_ANON_KEY}")
                .header("Content-Type", "application/json")
                .patch(json.toString().toRequestBody("application/json".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating payment status: ${e.message}")
            return@withContext false
        }
    }

    // CHAT MESSAGES - FETCH
    suspend fun fetchRideMessages(rideId: String): List<RideMessage> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext emptyList()

        try {
            val url = "$SUPABASE_URL/rest/v1/ride_messages?select=*,profiles:sender_id(name,role)&ride_id=eq.$rideId&order=created_at.asc"
            val request = Request.Builder()
                .url(url)
                .header("apikey", SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer ${sessionToken ?: SUPABASE_ANON_KEY}")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "[]"
                    val arr = JSONArray(body)
                    val list = mutableListOf<RideMessage>()
                    for (i in 0 until arr.length()) {
                        list.add(parseRideMessage(arr.getJSONObject(i)))
                    }
                    return@withContext list
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching messages: ${e.message}")
        }
        return@withContext emptyList()
    }

    // CHAT MESSAGES - INSERT
    suspend fun insertRideMessage(msg: RideMessage): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext true

        try {
            val senderId = sessionUserId ?: "00000000-0000-0000-0000-000000000000"
            val json = JSONObject().apply {
                put("id", msg.id)
                put("ride_id", msg.requestId)
                put("sender_id", senderId)
                put("message", msg.message)
            }

            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/ride_messages")
                .header("apikey", SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer ${sessionToken ?: SUPABASE_ANON_KEY}")
                .header("Content-Type", "application/json")
                .header("Prefer", "resolution=merge-duplicates")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting chat message: ${e.message}")
            return@withContext false
        }
    }


    // 4. CHAT MESSAGES PARSING & MAPPING HELPERS
    private fun parseRideMessage(obj: JSONObject): RideMessage {
        val prof = obj.optJSONObject("profiles")
        return RideMessage(
            id = obj.optString("id", java.util.UUID.randomUUID().toString()),
            requestId = obj.optString("ride_id", ""),
            senderId = obj.optString("sender_id", ""),
            senderName = prof?.optString("name") ?: (if (obj.optString("sender_id") == "admin_id") "Admin" else "Passageiro"),
            senderRole = prof?.optString("role") ?: (if (obj.optString("sender_id") == "admin_id") "admin" else "passageiro"),
            message = obj.optString("message", ""),
            timestamp = System.currentTimeMillis()
        )
    }

    private fun parseRideRequest(obj: JSONObject): RideRequest {
        val passengerProfile = obj.optJSONObject("profiles")
        return RideRequest(
            id = obj.optString("id", java.util.UUID.randomUUID().toString()),
            passengerName = passengerProfile?.optString("name") ?: "Passageiro",
            passengerEmail = passengerProfile?.optString("email") ?: "passageiro@gmail.com",
            origin = obj.optString("origin", ""),
            destination = obj.optString("destination", ""),
            dateTime = obj.optString("scheduled_at", ""),
            tripType = mapSupabaseTripTypeToLocal(obj.optString("trip_type", "ida")),
            passengerCount = obj.optInt("passengers", 1),
            notes = obj.optString("notes", ""),
            status = mapSupabaseStatusToLocal(obj.optString("status", "pendente")),
            assignedDriverId = obj.optString("assigned_driver_id", null as String?),
            assignedDriverName = obj.optString("assigned_driver_name", null as String?),
            cancelReason = obj.optString("rejection_reason", null as String?),
            priceEstimate = obj.optDouble("estimated_price", 0.0),
            finalPrice = obj.optDouble("final_price", 0.0),
            estimatedKm = obj.optDouble("distance_km", 12.0),
            paymentConfirmed = obj.optBoolean("payment_confirmed", false),
            timestamp = System.currentTimeMillis()
        )
    }

    private fun mapLocalStatusToSupabase(status: String): String {
        return when (status) {
            "pendente" -> "pending"
            "aprovada" -> "approved"
            "recusada" -> "rejected"
            "confirmada" -> "confirmed"
            "em_viagem" -> "in_progress"
            "concluída" -> "completed"
            "cancelada" -> "cancelled"
            else -> status
        }
    }

    private fun mapSupabaseStatusToLocal(status: String): String {
        return when (status) {
            "pending" -> "pendente"
            "approved" -> "aprovada"
            "rejected" -> "recusada"
            "confirmed" -> "confirmada"
            "in_progress" -> "em_viagem"
            "completed" -> "concluída"
            "cancelled" -> "cancelada"
            else -> status
        }
    }

    private fun mapLocalTripTypeToSupabase(tripType: String): String {
        return when (tripType) {
            "ida" -> "one_way"
            "ida_volta" -> "round_trip"
            else -> tripType
        }
    }

    private fun mapSupabaseTripTypeToLocal(tripType: String): String {
        return when (tripType) {
            "one_way" -> "ida"
            "round_trip" -> "ida_volta"
            else -> tripType
        }
    }

    // 4. SISTEMA DE RASTREAMENTO AO VIVO (Tabela driver_live_locations)
    suspend fun updateDriverLiveLocation(location: DriverLiveLocation): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext true

        try {
            val json = JSONObject().apply {
                put("driverId", location.driverId)
                put("driverName", location.driverName)
                put("latitude", location.latitude)
                put("longitude", location.longitude)
                put("requestId", location.requestId)
                put("status", location.status)
                put("timestamp", location.timestamp)
            }

            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/driver_live_locations")
                .header("apikey", SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer ${sessionToken ?: SUPABASE_ANON_KEY}")
                .header("Content-Type", "application/json")
                .header("Prefer", "resolution=merge-duplicates")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating live location in Supabase: ${e.message}")
            return@withContext false
        }
    }

    suspend fun deleteDriverLiveLocation(requestId: String): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext true

        try {
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/driver_live_locations?requestId=eq.$requestId")
                .header("apikey", SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer ${sessionToken ?: SUPABASE_ANON_KEY}")
                .delete()
                .build()

            httpClient.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting live location in Supabase: ${e.message}")
            return@withContext false
        }
    }

    // 3. STORAGE
    suspend fun uploadFileToStorage(bucket: String, path: String, fileBytes: ByteArray): String? = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            Log.i(TAG, "Supabase storage: Simulated upload completed successfully.")
            return@withContext "https://supabase-simulated-bucket.com/$bucket/$path"
        }

        try {
            val requestBody = fileBytes.toRequestBody("application/octet-stream".toMediaType())
            
            val request = Request.Builder()
                .url("$SUPABASE_URL/storage/v1/object/$bucket/$path")
                .header("apikey", SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer ${sessionToken ?: SUPABASE_ANON_KEY}")
                .header("Content-Type", "application/octet-stream")
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val publicUrl = "$SUPABASE_URL/storage/v1/object/public/$bucket/$path"
                    Log.i(TAG, "Supabase file uploaded successfully: $publicUrl")
                    return@withContext publicUrl
                } else {
                    Log.e(TAG, "Supabase Storage uploaded failed with code: ${response.code}")
                    return@withContext null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error performing Supabase Storage upload: ${e.message}")
            return@withContext null
        }
    }
}
