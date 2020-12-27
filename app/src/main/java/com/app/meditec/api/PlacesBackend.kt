package com.app.meditec.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Url

interface PlacesBackend {
    companion object {
        const val BASE_URL = "https://maps.googleapis.com/maps/api/place/nearbysearch/"

        operator fun invoke(): PlacesBackend {
            return Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(PlacesBackend::class.java)
        }
    }

    @GET
    suspend fun searchPlaces(@Url url: String): Response<ResponseBody>
}