package com.app.meditec.repository

import com.app.meditec.helpers.DownloadPlacesFromUrl
import com.app.meditec.models.PlaceInfo

object PlacesRepository {
    private val downloadPlacesFromUrl = DownloadPlacesFromUrl()

    fun getPlaces(url : String) : List<PlaceInfo>{
        val response = downloadPlacesFromUrl.readUrl(url)
        return response.results
    }
}