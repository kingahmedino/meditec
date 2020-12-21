package com.app.meditec.repository

import com.app.meditec.BuildConfig
import com.app.meditec.api.Backend
import com.app.meditec.api.PlaceInfoResponse
import com.app.meditec.models.PlaceInfo
import com.google.gson.Gson
import okhttp3.ResponseBody
import retrofit2.Response

object PlacesRepository {

    suspend fun getPlaces(lat: Double, lng: Double): List<PlaceInfo>? {
        val url = Backend.BASE_URL +
                "json?location=${lat},${lng}" +
                "&radius=1000" +
                "&type=hospital" +
                "&key=${BuildConfig.MAPS_API_KEY}"
        val response = Backend.invoke().searchPlaces(url)
        val jsonString = getJSONStringFrom(response)
        val placeInfoResponse = parse(jsonString)
        return placeInfoResponse?.results
    }

    private fun getJSONStringFrom(response: Response<ResponseBody>): String? {
        if (response.body() != null) {
            val inputStream = response.body()!!.byteStream()
            return inputStream.bufferedReader().use { it.readText() }
        }
        return null
    }

    private fun parse(s: String?): PlaceInfoResponse? {
        if (s != null) {
            val gson = Gson()
            return gson.fromJson(s, PlaceInfoResponse::class.java)
        }
        return null
    }
}