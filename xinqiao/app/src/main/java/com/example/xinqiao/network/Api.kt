package com.example.xinqiao.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

data class LoginResp(val ok: Boolean, val token: String?)
data class MeResp(val ok: Boolean, val username: String?, val roles: List<String>?, val reviewStatus: String?)
data class ApplicationPayload(
    val realName: String?, val idCard: String?, val phone: String?,
    val qualificationType: String?, val certificateNo: String?, val years: Int?,
    val expertise: List<String>?, val materials: List<String>?, val intro: String?
)
data class ApiResp<T>(val ok: Boolean, val code: Int, val message: String?, val data: T?)
data class Overview(val pending: Int, val approved: Int, val completed: Int, val cancelled: Int, val rejected: Int, val monthTotal: Int, val approvalRate: Double)

interface Api {
    @POST("/api/auth/login")
    suspend fun login(@Query("username") username: String, @Query("password") password: String): LoginResp

    @GET("/api/auth/me")
    suspend fun me(): MeResp

    @POST("/api/applications")
    suspend fun submitApplication(@Body payload: ApplicationPayload): retrofit2.Response<ApiResp<Any>>

    @POST("/api/auth/register")
    suspend fun register(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("role") role: String? = null
    ): retrofit2.Response<okhttp3.ResponseBody>

    @GET("/api/applications/me")
    suspend fun myApplications(): ApiResp<List<Any>>

    @GET("/api/applications/me")
    suspend fun myApplicationsRaw(): retrofit2.Response<okhttp3.ResponseBody>

    @GET("/api/counselor/dashboard/overview")
    suspend fun counselorOverview(): Map<String, Any>

    // Appointments (user side)
    @GET("/api/appointments/mine")
    suspend fun appointmentsMine(): retrofit2.Response<okhttp3.ResponseBody>

    @POST("/api/appointments")
    suspend fun requestAppointment(
        @Query("counselor") counselor: String,
        @Query("start") start: String,
        @Query("end") end: String
    ): retrofit2.Response<okhttp3.ResponseBody>

    @POST("/api/appointments/{id}/cancel")
    suspend fun cancelAppointment(@retrofit2.http.Path("id") id: Long): retrofit2.Response<okhttp3.ResponseBody>

    @POST("/api/appointments/{id}/reschedule")
    suspend fun rescheduleAppointment(
        @retrofit2.http.Path("id") id: Long,
        @Query("date") date: String,
        @Query("time") time: String
    ): retrofit2.Response<okhttp3.ResponseBody>

    // Counselor schedule
    @GET("/api/counselor/schedule")
    suspend fun counselorSchedule(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): retrofit2.Response<okhttp3.ResponseBody>

    @GET("/api/counselor/schedule/rules")
    suspend fun listScheduleRules(): retrofit2.Response<okhttp3.ResponseBody>

    @POST("/api/counselor/schedule/rules")
    suspend fun createScheduleRule(
        @Query("frequency") frequency: String,
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String,
        @Query("startTime") startTime: String,
        @Query("endTime") endTime: String,
        @Query("weekdays") weekdays: String
    ): retrofit2.Response<okhttp3.ResponseBody>

    @POST("/api/counselor/schedule/rules/{ruleId}/exceptions")
    suspend fun addScheduleException(
        @retrofit2.http.Path("ruleId") ruleId: Long,
        @Query("date") date: String
    ): retrofit2.Response<okhttp3.ResponseBody>

    @POST("/api/counselor/schedule/rules/generate")
    suspend fun generateSlots(
        @Query("from") from: String,
        @Query("to") to: String
    ): retrofit2.Response<okhttp3.ResponseBody>

    @POST("/api/counselor/schedule/{id}/open")
    suspend fun openSlot(@retrofit2.http.Path("id") id: Long): retrofit2.Response<okhttp3.ResponseBody>

    @POST("/api/counselor/schedule/{id}/close")
    suspend fun closeSlot(@retrofit2.http.Path("id") id: Long): retrofit2.Response<okhttp3.ResponseBody>

    @GET("/api/counselor/schedule/rules/{ruleId}/exceptions")
    suspend fun listScheduleExceptions(@retrofit2.http.Path("ruleId") ruleId: Long): retrofit2.Response<okhttp3.ResponseBody>

    @retrofit2.http.DELETE("/api/counselor/schedule/rules/{ruleId}")
    suspend fun deleteRule(@retrofit2.http.Path("ruleId") ruleId: Long): retrofit2.Response<okhttp3.ResponseBody>

    @retrofit2.http.DELETE("/api/counselor/schedule/rules/exceptions/{exId}")
    suspend fun deleteException(@retrofit2.http.Path("exId") exId: Long): retrofit2.Response<okhttp3.ResponseBody>

    // Consult Pro
    @GET("/api/consult/pro/slots")
    suspend fun consultSlots(
        @Query("consultantId") consultantId: String,
        @Query("date") date: String
    ): retrofit2.Response<okhttp3.ResponseBody>

    @POST("/api/consult/pro/appointments")
    suspend fun consultSubmit(@Body body: Map<String, Any?>): retrofit2.Response<okhttp3.ResponseBody>

    @GET("/api/consult/pro/cities")
    suspend fun consultCities(): retrofit2.Response<okhttp3.ResponseBody>

    @GET("/api/consult/pro/cityDict")
    suspend fun consultCityDict(): retrofit2.Response<okhttp3.ResponseBody>

    // Geo
    @GET("/api/geo/reverse")
    suspend fun reverseGeo(@Query("lat") lat: Double, @Query("lon") lon: Double): retrofit2.Response<okhttp3.ResponseBody>
}
