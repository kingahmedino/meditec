package com.app.meditec.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Url

interface DirectionsBackend {
    companion object {
        const val DIRECTIONS_BASE_URL = "https://maps.googleapis.com/maps/api/directions/"

        operator fun invoke(): DirectionsBackend {
            return Retrofit.Builder()
                    .baseUrl(DIRECTIONS_BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(DirectionsBackend::class.java)
        }
    }

    @GET
    suspend fun getDirections(@Url url: String): Response<ResponseBody>
}