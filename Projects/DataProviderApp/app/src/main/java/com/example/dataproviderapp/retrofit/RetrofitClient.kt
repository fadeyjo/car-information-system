package com.example.dataproviderapp.retrofit

import com.example.dataproviderapp.BuildConfig
import com.example.dataproviderapp.api.RefreshTokenApi
import com.example.dataproviderapp.apiutils.LocalDateAdapter
import com.example.dataproviderapp.apiutils.LocalDateTimeAdapter
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import java.time.LocalDateTime

object RetrofitClient {
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val gson = GsonBuilder()
        .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
        .registerTypeAdapter(LocalDate::class.java, LocalDateAdapter())
        .create()

    private val refreshRetrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val refreshApi: RefreshTokenApi = refreshRetrofit.create(RefreshTokenApi::class.java)

    private val public_client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    val auth_client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(AuthInterceptor())
        .authenticator(TokenAuthenticator(refreshApi))
        .build()

    val public_retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(public_client)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val auth_retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(auth_client)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
}