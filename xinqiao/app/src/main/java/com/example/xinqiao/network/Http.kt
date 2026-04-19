package com.example.xinqiao.network

import android.content.Context
import android.util.Log
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object Http {
    private const val TAG = "Http"
    @Volatile private var retrofit: Retrofit? = null
    @Volatile private var apiInst: Api? = null

    @JvmStatic
    @Synchronized fun init(context: Context) {
        val base = NetworkConfig.getBaseUrl(context)
        Log.d(TAG, "初始化HTTP客户端，Base URL: $base")
        
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(context))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor { chain ->
                val request = chain.request()
                Log.d(TAG, "请求: ${request.method} ${request.url}")
                try {
                    val response = chain.proceed(request)
                    Log.d(TAG, "响应: ${response.code} ${request.url}")
                    response
                } catch (e: Exception) {
                    Log.e(TAG, "请求失败: ${request.url}", e)
                    throw e
                }
            }
            .build()
            
        retrofit = Retrofit.Builder()
            .baseUrl(base)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
        apiInst = retrofit!!.create(Api::class.java)
        
        Log.d(TAG, "HTTP客户端初始化完成")
    }

    @JvmStatic
    fun api(): Api {
        val a = apiInst
        require(a != null) { "Http not initialized: call Http.init(context) in Application.onCreate()" }
        return a
    }
}
