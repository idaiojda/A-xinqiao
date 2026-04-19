package com.example.xinqiao.network

import android.content.Context
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val sp = context.getSharedPreferences("loginInfo", Context.MODE_PRIVATE)
        val token = sp.getString("auth_token", null)
        // Debug logs removed to reduce logcat spam
        val req = if (!token.isNullOrEmpty()) {
            chain.request().newBuilder().addHeader("Authorization", "Bearer $token").build()
        } else {
            // Only log warning when token is missing (less frequent)
            android.util.Log.w("AuthInterceptor", "警告: Token为空，请求将不带认证信息: ${chain.request().url}")
            chain.request()
        }
        return chain.proceed(req)
    }
}