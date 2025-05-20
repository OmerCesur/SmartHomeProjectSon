package com.example.smarthome.api

import retrofit2.http.*
import com.google.gson.annotations.SerializedName

/**
 * Retrofit API interface for SmartHome backend
 */
interface SmartHomeApiService {

    // ===== SENSOR ENDPOINTS =====

    /**
     * Get all sensors from all rooms
     * @return Response containing all rooms with their sensors
     */
    @GET("sensors")
    suspend fun getAllSensors(): RoomSensorsResponse

    /**
     * Get all sensors for a specific room
     * @param room Room name (e.g., "yatak_odasi", "salon", "garaj", "banyo", "giris")
     * @return Response containing sensors for the specified room
     */
    @GET("sensors/{room}")
    suspend fun getRoomSensors(@Path("room") room: String): RoomSensorResponse

    /**
     * Get data for a specific sensor in a room
     * @param room Room name
     * @param sensorType Sensor type (e.g., "light", "curtain", "door", "temperature", "gas", "face_id")
     * @return Response containing sensor data
     */
    @GET("sensor-data/{room}/{sensorType}")
    suspend fun getSensorData(
        @Path("room") room: String,
        @Path("sensorType") sensorType: String
    ): SensorDataResponse

    /**
     * Update sensor data for a specific sensor in a room
     * @param room Room name
     * @param sensorType Sensor type
     * @param request Request body containing the new sensor value
     * @return Response containing the update result
     */
    @POST("sensor-data/{room}/{sensorType}")
    suspend fun updateSensorData(
        @Path("room") room: String,
        @Path("sensorType") sensorType: String,
        @Body request: SensorUpdateRequest
    ): SensorUpdateResponse

    /**
     * Bulk update multiple sensors at once
     * @param request Request body containing updates for multiple sensors
     * @return Response containing the bulk update result
     */
    @POST("sensors/bulk-update")
    suspend fun bulkUpdateSensors(@Body request: BulkSensorUpdateRequest): BulkSensorUpdateResponse

    /**
     * Get all notifications
     * @return Response containing all notifications
     */
    @GET("notifications")
    suspend fun getNotifications(): NotificationsResponse

    /**
     * Delete a specific notification
     * @param notificationId ID of the notification to delete
     * @return Response containing the deletion result
     */
    @DELETE("notification/{notificationId}")
    suspend fun deleteNotification(@Path("notificationId") notificationId: String): BaseResponse

    // ===== COMMAND ENDPOINTS =====

    /**
     * Get status of a specific command in a room
     * @param room Room name
     * @param commandType Command type (e.g., "light", "curtain", "door")
     * @return Response containing the command status
     */
    @GET("command/{room}/{commandType}")
    suspend fun getCommandStatus(
        @Path("room") room: String,
        @Path("commandType") commandType: String
    ): CommandStatusResponse

    /**
     * Send a command to a specific device in a room
     * @param room Room name
     * @param commandType Command type
     * @param request Request body containing the command value
     * @return Response containing the command result
     */
    @POST("command/{room}/{commandType}")
    suspend fun sendCommand(
        @Path("room") room: String,
        @Path("commandType") commandType: String,
        @Body request: CommandRequest
    ): CommandResponse

    // ===== STATUS ENDPOINTS =====

    /**
     * Get status of all devices in a room
     * @param room Room name
     * @return Response containing the status of all devices in the room
     */
    @GET("status/{room}")
    suspend fun getRoomStatus(@Path("room") room: String): Map<String, CommandStatus>

    // ===== AUTH ENDPOINTS =====

    /**
     * Login to the system
     * @param request Request body containing username and password
     * @return Response containing login result
     */
    @POST("login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    /**
     * Logout from the system
     * @return Response containing logout result
     */
    @POST("logout")
    suspend fun logout(): BaseResponse
}

// ===== DATA MODELS =====

/**
 * Base response for all API calls
 */
open class BaseResponse(
    @SerializedName("message") val message: String? = null,
    @SerializedName("error") val error: String? = null,
    @SerializedName("details") val details: String? = null
)

/**
 * Response for getting all sensors from all rooms
 */
data class RoomSensorsResponse(
    @SerializedName("rooms") val rooms: Map<String, RoomData>
) : BaseResponse()

/**
 * Response for getting sensors for a specific room
 */
data class RoomSensorResponse(
    @SerializedName("room") val room: String,
    @SerializedName("sensors") val sensors: Map<String, SensorInfo>
) : BaseResponse()

/**
 * Room data containing sensors
 */
data class RoomData(
    @SerializedName("sensors") val sensors: Map<String, SensorInfo>
)

/**
 * Sensor information
 */
data class SensorInfo(
    @SerializedName("type") val type: String,
    @SerializedName("description") val description: String,
    @SerializedName("status") val status: SensorStatus? = null,
    @SerializedName("values") val values: List<String>? = null,
    @SerializedName("model") val model: String? = null,
    @SerializedName("range") val range: List<Int>? = null,
//    @SerializedName("severity") val severity: String? = null
)

/**
 * Sensor status
 */
data class SensorStatus(
    @SerializedName("value") val value: Any?,
    @SerializedName("timestamp") val timestamp: String?,
    @SerializedName("severity") val severity: String? = null  // sadece gaz için gelir.
)

/**
 * Response for getting data for a specific sensor
 */
data class SensorDataResponse(
    @SerializedName("room") val room: String,
    @SerializedName("sensor_type") val sensorType: String,
    @SerializedName("sensor_info") val sensorInfo: SensorInfo,
    @SerializedName("data") val status: SensorStatus
) : BaseResponse()

/**
 * Request for updating sensor data
 */
data class SensorUpdateRequest(
    @SerializedName("value") val value: Any
)

data class FloatSensorUpdateRequest(
    @SerializedName("value") val value: Float
)

/**
 * Response for updating sensor data
 */
data class SensorUpdateResponse(
    @SerializedName("room") val room: String,
    @SerializedName("sensor_type") val sensorType: String,
    @SerializedName("value") val value: Any,
    @SerializedName("timestamp") val timestamp: String
) : BaseResponse()

/**
 * Request for bulk updating multiple sensors
 */
data class BulkSensorUpdateRequest(
    @SerializedName("updates") val updates: List<SensorUpdate>
)

/**
 * Individual sensor update for bulk update
 */
data class SensorUpdate(
    @SerializedName("room") val room: String,
    @SerializedName("sensor_type") val sensorType: String,
    @SerializedName("value") val value: Any
)

/**
 * Response for bulk updating sensors
 */
data class BulkSensorUpdateResponse(
    @SerializedName("successful_updates") val successfulUpdates: List<SensorUpdateResponse>,
    @SerializedName("errors") val errors: List<Map<String, Any>>
) : BaseResponse()

/**
 * Response for getting notifications
 */
data class NotificationsResponse(
    @SerializedName("notifications") val notifications: List<Notification>
) : BaseResponse()

/**
 * Notification data
 */
data class Notification(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("message") val message: String,
    @SerializedName("type") val type: String,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("read") val read: Boolean
)

/**
 * Command status
 */
data class CommandStatus(
    @SerializedName("value") val value: String,
    @SerializedName("timestamp") val timestamp: String?
)

/**
 * Response for getting command status
 */
data class CommandStatusResponse(
    @SerializedName("room") val room: String,
    @SerializedName("command_type") val commandType: String,
    @SerializedName("command_info") val commandInfo: Map<String, Any>,
    @SerializedName("status") val status: CommandStatus
) : BaseResponse()

/**
 * Request for sending a command
 */
data class CommandRequest(
    @SerializedName("command") val command: String
)

/**
 * Response for sending a command
 */
data class CommandResponse(
    @SerializedName("room") val room: String,
    @SerializedName("command_type") val commandType: String,
    @SerializedName("command") val command: String,
    @SerializedName("timestamp") val timestamp: String
) : BaseResponse()

/**
 * Request for login
 */
data class LoginRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String
)

/**
 * Response for login
 */
data class LoginResponse(
    @SerializedName("user") val user: User,
    @SerializedName("token") val token: String
) : BaseResponse()

/**
 * User data
 */
data class User(
    @SerializedName("id") val id: String,
    @SerializedName("username") val username: String,
    @SerializedName("name") val name: String,
    @SerializedName("role") val role: String
)
