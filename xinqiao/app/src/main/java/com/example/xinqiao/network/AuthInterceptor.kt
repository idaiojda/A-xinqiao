package com.example.xinqiao.network

import android.content.Context
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val sp = context.getSharedPreferences("loginInfo", Context.MODE_PRIVATE)
        val token = sp.getString("auth_token", null)
        val req = if (!token.isNullOrEmpty()) {
            chain.request().newBuilder().addHeader("Authorization", "Bearer $token").build()
        } else chain.request()
        return chain.proceed(req)
    }
}