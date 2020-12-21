package com.app.meditec.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Url

interface Backend {
    companion object {
        const val BASE_URL = "https://maps.googleapis.com/maps/api/place/nearbysearch/"

        operator fun invoke(): Backend {
            return Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(Backend::class.java)
        }
    }

    @GET
    suspend fun searchPlaces(@Url url: String): Response<ResponseBody>
}