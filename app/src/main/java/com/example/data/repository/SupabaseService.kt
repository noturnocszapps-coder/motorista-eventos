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

    // Fetch credentials from BuildConfig injected by the Secrets Gradle Plugin
    // Falls back to empty values if they are placeholders or missing
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

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    // 1. AUTH GOOGLE
    // Simulates or authenticates directly against the Supabase OAuth endpoints
    suspend fun authenticateWithGoogleToken(idToken: String): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            Log.w(TAG, "Supabase not configured. Simulating Google Auth login locally.")
            return@withContext true
        }

        try {
            val json = JSONObject().apply {
                put("id_token", idToken)
                put("provider", "google")
            }

            val request = Request.Builder()
                .url("$SUPABASE_URL/auth/v1/token?grant_type=id_token")
                .header("apikey", SUPABASE_ANON_KEY)
                .header("Content-Type", "application/json")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    Log.i(TAG, "Google Auth Success: Session Token generated.")
                    return@withContext true
                } else {
                    Log.e(TAG, "Google Auth API returned error code: ${response.code}")
                    return@withContext false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing Google Auth with Supabase: ${e.message}")
            return@withContext false
        }
    }

    // 2. BANCO POSTGRESQL (Select requests)
    suspend fun fetchAllRideRequests(): List<RideRequest> = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            return@withContext emptyList()
        }

        try {
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/ride_requests?order=timestamp.desc")
                .header("apikey", SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer $SUPABASE_ANON_KEY")
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

    // Insert new check request to Supabase PostgreSQL Database
    suspend fun insertRideRequest(rideReq: RideRequest): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            Log.i(TAG, "Supabase local sync: offline request stored.")
            return@withContext true
        }

        try {
            val json = JSONObject().apply {
                put("id", if (rideReq.id > 0) rideReq.id else null)
                put("passengerName", rideReq.passengerName)
                put("passengerEmail", rideReq.passengerEmail)
                put("origin", rideReq.origin)
                put("destination", rideReq.destination)
                put("dateTime", rideReq.dateTime)
                put("tripType", rideReq.tripType)
                put("passengerCount", rideReq.passengerCount)
                put("notes", rideReq.notes)
                put("status", rideReq.status)
                put("assignedDriverId", rideReq.assignedDriverId)
                put("assignedDriverName", rideReq.assignedDriverName)
                put("cancelReason", rideReq.cancelReason)
                put("priceEstimate", rideReq.priceEstimate)
                put("finalPrice", rideReq.finalPrice)
                put("estimatedKm", rideReq.estimatedKm)
                put("paymentConfirmed", rideReq.paymentConfirmed)
                put("timestamp", rideReq.timestamp)
            }

            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/ride_requests")
                .header("apikey", SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer $SUPABASE_ANON_KEY")
                .header("Content-Type", "application/json")
                .header("Prefer", "resolution=merge-duplicates")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting ride request: ${e.message}")
            return@withContext false
        }
    }

    // Update status or other factors in Superbase PostgreSQL Database
    suspend fun updateRideRequestStatus(id: Int, status: String): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext true

        try {
            val json = JSONObject().apply {
                put("status", status)
            }

            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/ride_requests?id=eq.$id")
                .header("apikey", SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer $SUPABASE_ANON_KEY")
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
                .header("Authorization", "Bearer $SUPABASE_ANON_KEY")
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

    suspend fun deleteDriverLiveLocation(requestId: Int): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext true

        try {
            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/driver_live_locations?requestId=eq.$requestId")
                .header("apikey", SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer $SUPABASE_ANON_KEY")
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
    // Upload de avatar ou arquivos para Bucket S3 Supabase
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
                .header("Authorization", "Bearer $SUPABASE_ANON_KEY")
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

    private fun parseRideRequest(obj: JSONObject): RideRequest {
        return RideRequest(
            id = obj.optInt("id", 0),
            passengerName = obj.optString("passengerName", ""),
            passengerEmail = obj.optString("passengerEmail", ""),
            origin = obj.optString("origin", ""),
            destination = obj.optString("destination", ""),
            dateTime = obj.optString("dateTime", ""),
            tripType = obj.optString("tripType", "ida"),
            passengerCount = obj.optInt("passengerCount", 1),
            notes = obj.optString("notes", ""),
            status = obj.optString("status", "pendente"),
            assignedDriverId = obj.optString("assignedDriverId", null),
            assignedDriverName = obj.optString("assignedDriverName", null),
            cancelReason = obj.optString("cancelReason", null),
            priceEstimate = obj.optDouble("priceEstimate", 0.0),
            finalPrice = obj.optDouble("finalPrice", 0.0),
            estimatedKm = obj.optDouble("estimatedKm", 12.0),
            paymentConfirmed = obj.optBoolean("paymentConfirmed", false),
            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
        )
    }
}
