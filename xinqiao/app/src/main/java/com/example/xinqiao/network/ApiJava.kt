package com.example.xinqiao.network

import kotlinx.coroutines.runBlocking

object ApiJava {
    @JvmStatic
    fun login(username: String, password: String): LoginResp? = runBlocking {
        try { Http.api().login(username, password) } catch (t: Throwable) { null }
    }

    @JvmStatic
    fun me(): MeResp? = runBlocking {
        try { Http.api().me() } catch (t: Throwable) { null }
    }

    @JvmStatic
    fun myApplicationsRaw(): retrofit2.Response<okhttp3.ResponseBody>? = runBlocking {
        try { Http.api().myApplicationsRaw() } catch (t: Throwable) { null }
    }

    @JvmStatic
    fun counselorSchedule(from: String?, to: String?): retrofit2.Response<okhttp3.ResponseBody>? = runBlocking {
        try { Http.api().counselorSchedule(from, to) } catch (t: Throwable) { null }
    }

    @JvmStatic
    fun register(username: String, password: String): Boolean = runBlocking {
        try { Http.api().register(username, password, null).isSuccessful } catch (t: Throwable) { false }
    }
}
