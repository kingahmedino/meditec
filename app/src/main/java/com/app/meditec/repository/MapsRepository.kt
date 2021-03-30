package com.app.meditec.repository

import com.app.meditec.BuildConfig
import com.app.meditec.R
import com.app.meditec.api.DirectionsBackend
import com.app.meditec.api.PlaceInfoResponse
import com.app.meditec.api.PlacesBackend
import com.app.meditec.api.RoutesResponse
import com.app.meditec.models.OnBoardingItem
import com.app.meditec.models.PlaceInfo
import com.app.meditec.models.Route
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import okhttp3.ResponseBody
import retrofit2.Response

object MapsRepository {
    private lateinit var mOnBoardingItems: MutableList<OnBoardingItem>

    suspend fun getPlaces(lat: Double, lng: Double): List<PlaceInfo>? {
        val url = PlacesBackend.BASE_URL +
                "json?location=${lat},${lng}" +
                "&radius=1000" +
                "&type=hospital" +
                "&key=${BuildConfig.MAPS_API_KEY}"
        val response = PlacesBackend.invoke().searchPlaces(url)
        val jsonString = getJSONStringFrom(response)
        val placeInfoResponse = parse("place info", jsonString) as PlaceInfoResponse?
        return placeInfoResponse?.results
    }

    suspend fun getDirections(currentLocation: LatLng, endPlaceId: String, mode: String = "driving"): List<Route>? {
        val url = DirectionsBackend.DIRECTIONS_BASE_URL +
                "json?" +
                "origin=${currentLocation.latitude},${currentLocation.longitude}" +
                "&destination=place_id:$endPlaceId" +
                "&mode=$mode" +
                "&key=${BuildConfig.MAPS_API_KEY}"
        val response = DirectionsBackend.invoke().getDirections(url)
        val jsonString = getJSONStringFrom(response)
        val routesResponse = parse("route", jsonString) as RoutesResponse?
        return routesResponse?.routes
    }

    private fun getJSONStringFrom(response: Response<ResponseBody>): String? {
        if (response.body() != null) {
            val inputStream = response.body()!!.byteStream()
            return inputStream.bufferedReader().use { it.readText() }
        }
        return null
    }

    private fun parse(type: String, s: String?): Any? {
        if (s != null) {
            val gson = Gson()
            when (type) {
                "place info" -> {
                    return gson.fromJson(s, PlaceInfoResponse::class.java)
                }
                "route" -> {
                    return gson.fromJson(s, RoutesResponse::class.java)
                }
            }
        }
        return null
    }

    fun getOnBoardingItems(): List<OnBoardingItem> {
        mOnBoardingItems = mutableListOf()
        mOnBoardingItems.add(OnBoardingItem("Find hospitals", "Navigate your way to hospitals nearest to you", R.drawable.ic_map_locations))
        mOnBoardingItems.add(OnBoardingItem("Get directions", "Find hospitals through step by step directions\nbased on your mode of travelling", R.drawable.ic_lady_locations))
        mOnBoardingItems.add(OnBoardingItem("Meditec", "Get qualified doctors to attend to you", R.drawable.ic_doctors))
        return mOnBoardingItems
    }
}