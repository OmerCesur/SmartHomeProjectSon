package com.example.smarthome.api

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    private const val BASE_URL = "http://192.168.240.242:5001/api/"

    private val retrofit by lazy {
        val ohttp = OkHttpClient.Builder()
            .addNetworkInterceptor(
                HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
            ).build()

        val gson = GsonBuilder()
            .setLenient()
            .create()

        Retrofit.Builder()
            .client(ohttp)
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory
                .create(gson))
            .build()
    }

    val smartHomeApiService by lazy {
        retrofit.create(SmartHomeApiService::class.java)
    }

}
