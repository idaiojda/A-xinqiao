package com.example.xinqiao.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

data class LoginResp(val ok: Boolean, val token: String?)
data class MeResp(
    val ok: Boolean, 
    val username: String?, 
    val roles: List<String>?, 
    val reviewStatus: String?,
    val nickname: String? = null,
    val avatarBase64: String? = null
)
data class ApplicationPayload(
    val realName: String?, val idCard: String?, val phone: String?,
    val qualificationType: String?, val certificateNo: String?, val years: Int?,
    val expertise: List<String>?, val materials: List<String>?, val intro: String?
)
data class ApiResp<T>(val ok: Boolean, val code: Int, val message: String?, val data: T?)
data class Overview(
    val pending: Int, 
    val approved: Int, 
    val completed: Int, 
    val cancelled: Int, 
    val rejected: Int, 
    val monthTotal: Int, 
    val approvalRate: Double,
    val monthIncome: Double? = null,
    val balance: Double? = null,
    val totalIncome: Double? = null,
    val platformFee: Double? = null
)

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

    @GET("/api/applications/auto-approval-rules")
    suspend fun getAutoApprovalRules(): retrofit2.Response<ApiResp<String>>

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
    
    // Counselor appointments management
    @GET("/api/counselor/appointments")
    suspend fun counselorAppointments(): retrofit2.Response<okhttp3.ResponseBody>
    
    @POST("/api/counselor/appointments/{id}/approve")
    suspend fun approveAppointment(@retrofit2.http.Path("id") id: Long): retrofit2.Response<okhttp3.ResponseBody>
    
    @POST("/api/counselor/appointments/{id}/reject")
    suspend fun rejectAppointment(@retrofit2.http.Path("id") id: Long): retrofit2.Response<okhttp3.ResponseBody>
    
    @POST("/api/counselor/appointments/{id}/complete")
    suspend fun completeAppointment(@retrofit2.http.Path("id") id: Long): retrofit2.Response<okhttp3.ResponseBody>

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
    suspend fun consultSubmit(@Body body: Map<String, @JvmSuppressWildcards Any>): retrofit2.Response<okhttp3.ResponseBody>

    @GET("/api/consult/pro/cities")
    suspend fun consultCities(): retrofit2.Response<okhttp3.ResponseBody>

    @GET("/api/consult/pro/cityDict")
    suspend fun consultCityDict(): retrofit2.Response<okhttp3.ResponseBody>
    
    @GET("/api/counselor/public/{id}")
    suspend fun counselorPublicProfile(@retrofit2.http.Path("id") id: Long): retrofit2.Response<okhttp3.ResponseBody>

    // Geo
    @GET("/api/geo/reverse")
    suspend fun reverseGeo(@Query("lat") lat: Double, @Query("lon") lon: Double): retrofit2.Response<okhttp3.ResponseBody>
    
    // File upload
    @retrofit2.http.Multipart
    @POST("/api/upload/common")
    suspend fun uploadCommon(@retrofit2.http.Part file: okhttp3.MultipartBody.Part): ApiResp<Map<String, String>>
    
    // Counselor Content Management - Courses
    @GET("/api/counselor/courses")
    suspend fun counselorCourses(): retrofit2.Response<okhttp3.ResponseBody>
    
    @POST("/api/counselor/courses")
    suspend fun createCounselorCourse(@Body payload: Map<String, Any?>): retrofit2.Response<okhttp3.ResponseBody>
    
    @retrofit2.http.PUT("/api/counselor/courses/{id}/publish")
    suspend fun publishCounselorCourse(@retrofit2.http.Path("id") id: Long): retrofit2.Response<okhttp3.ResponseBody>
    
    @retrofit2.http.PUT("/api/counselor/courses/{id}/archive")
    suspend fun archiveCounselorCourse(@retrofit2.http.Path("id") id: Long): retrofit2.Response<okhttp3.ResponseBody>
    
    @retrofit2.http.DELETE("/api/counselor/courses/{id}")
    suspend fun deleteCounselorCourse(@retrofit2.http.Path("id") id: Long): retrofit2.Response<okhttp3.ResponseBody>
    
    @GET("/api/counselor/courses/{id}")
    suspend fun courseDetail(@retrofit2.http.Path("id") id: Long): retrofit2.Response<okhttp3.ResponseBody>
    
    @GET("/api/counselor/courses/{courseId}/lessons")
    suspend fun counselorCourseLessons(@retrofit2.http.Path("courseId") courseId: Long): retrofit2.Response<okhttp3.ResponseBody>
    
    @POST("/api/counselor/courses/lessons")
    suspend fun addCounselorCourseLesson(@Body payload: Map<String, Any?>): retrofit2.Response<okhttp3.ResponseBody>
    
    @retrofit2.http.DELETE("/api/counselor/courses/lessons/{id}")
    suspend fun deleteCounselorCourseLesson(@retrofit2.http.Path("id") id: Long): retrofit2.Response<okhttp3.ResponseBody>
    
    // Counselor Content Management - Assessments
    @GET("/api/counselor/assessments")
    suspend fun counselorAssessments(): retrofit2.Response<okhttp3.ResponseBody>
    
    @POST("/api/counselor/assessments")
    suspend fun createCounselorAssessment(@Body payload: Map<String, Any?>): retrofit2.Response<okhttp3.ResponseBody>
    
    @retrofit2.http.PUT("/api/counselor/assessments/{id}/publish")
    suspend fun publishCounselorAssessment(@retrofit2.http.Path("id") id: Long): retrofit2.Response<okhttp3.ResponseBody>
    
    @retrofit2.http.PUT("/api/counselor/assessments/{id}/archive")
    suspend fun archiveCounselorAssessment(@retrofit2.http.Path("id") id: Long): retrofit2.Response<okhttp3.ResponseBody>
    
    @retrofit2.http.DELETE("/api/counselor/assessments/{id}")
    suspend fun deleteCounselorAssessment(@retrofit2.http.Path("id") id: Long): retrofit2.Response<okhttp3.ResponseBody>
    
    @GET("/api/counselor/assessments/{id}")
    suspend fun assessmentDetail(@retrofit2.http.Path("id") id: Long): retrofit2.Response<okhttp3.ResponseBody>
    
    @GET("/api/counselor/assessments/{assessmentId}/questions")
    suspend fun assessmentQuestions(@retrofit2.http.Path("assessmentId") assessmentId: Long): retrofit2.Response<okhttp3.ResponseBody>
    
    @POST("/api/counselor/assessments/questions")
    suspend fun addCounselorAssessmentQuestion(@Body payload: Map<String, Any?>): retrofit2.Response<okhttp3.ResponseBody>
    
    @retrofit2.http.DELETE("/api/counselor/assessments/questions/{id}")
    suspend fun deleteCounselorAssessmentQuestion(@retrofit2.http.Path("id") id: Long): retrofit2.Response<okhttp3.ResponseBody>
    
    // Counselor Content Management - Articles
    @GET("/api/counselor/articles")
    suspend fun counselorArticles(): retrofit2.Response<okhttp3.ResponseBody>
    
    @POST("/api/counselor/articles")
    suspend fun createCounselorArticle(@Body payload: Map<String, Any?>): retrofit2.Response<okhttp3.ResponseBody>
    
    @retrofit2.http.PUT("/api/counselor/articles/{id}/publish")
    suspend fun publishCounselorArticle(@retrofit2.http.Path("id") id: Long): retrofit2.Response<okhttp3.ResponseBody>
    
    @retrofit2.http.PUT("/api/counselor/articles/{id}/archive")
    suspend fun archiveCounselorArticle(@retrofit2.http.Path("id") id: Long): retrofit2.Response<okhttp3.ResponseBody>
    
    @retrofit2.http.DELETE("/api/counselor/articles/{id}")
    suspend fun deleteCounselorArticle(@retrofit2.http.Path("id") id: Long): retrofit2.Response<okhttp3.ResponseBody>
    
    @GET("/api/counselor/articles/{id}")
    suspend fun articleDetail(@retrofit2.http.Path("id") id: Long): retrofit2.Response<okhttp3.ResponseBody>
    
    // Public content APIs
    @GET("/api/courses")
    suspend fun courses(): retrofit2.Response<okhttp3.ResponseBody>
    
    @GET("/api/courses/{id}/lessons")
    suspend fun courseLessons(@retrofit2.http.Path("id") id: Long): retrofit2.Response<okhttp3.ResponseBody>
    
    @GET("/api/assessments")
    suspend fun assessments(): retrofit2.Response<okhttp3.ResponseBody>
    
    @GET("/api/articles")
    suspend fun articles(@Query("page") page: Int, @Query("size") size: Int): retrofit2.Response<okhttp3.ResponseBody>
    
    // Counselor Profile Management
    @GET("/api/counselor/profile")
    suspend fun counselorProfile(): retrofit2.Response<okhttp3.ResponseBody>
    
    @POST("/api/counselor/profile")
    suspend fun updateCounselorProfile(@Body payload: Map<String, @JvmSuppressWildcards Any>): retrofit2.Response<okhttp3.ResponseBody>
    
    @retrofit2.http.PUT("/api/counselor/profile")
    suspend fun putCounselorProfile(@Body payload: Map<String, Any?>): retrofit2.Response<okhttp3.ResponseBody>
    
    // User Medical Records
    @GET("/api/user-medical-records/my")
    suspend fun getMyMedicalRecord(): retrofit2.Response<okhttp3.ResponseBody>
    
    @GET("/api/user-medical-records/user/{username}")
    suspend fun getUserMedicalRecord(@retrofit2.http.Path("username") username: String): retrofit2.Response<okhttp3.ResponseBody>
    
    @GET("/api/user-medical-records/counselor/patients")
    suspend fun getCounselorPatients(): retrofit2.Response<okhttp3.ResponseBody>
    
    @GET("/api/user-medical-records/counselor/patient/{username}")
    suspend fun getCounselorPatientRecord(@retrofit2.http.Path("username") username: String): retrofit2.Response<okhttp3.ResponseBody>
}
