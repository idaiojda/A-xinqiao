package com.example.xinqiao.network

import android.content.Context
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object Http {
    @Volatile private var retrofit: Retrofit? = null
    @Volatile private var apiInst: Api? = null

    @JvmStatic
    @Synchronized fun init(context: Context) {
        val base = NetworkConfig.getBaseUrl(context)
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(context))
            .build()
        retrofit = Retrofit.Builder()
            .baseUrl(base)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
        apiInst = retrofit!!.create(Api::class.java)
    }

    @JvmStatic
    fun api(): Api {
        val a = apiInst
        require(a != null) { "Http not initialized: call Http.init(context) in Application.onCreate()" }
        return a
    }
}
