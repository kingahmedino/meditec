package com.app.meditec.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.app.meditec.BuildConfig
import com.app.meditec.models.PlaceInfo
import com.app.meditec.repository.PlacesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MapsViewModel : ViewModel() {
    private val viewModelJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)
    val placesLiveData = MutableLiveData<List<PlaceInfo>>()

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    fun getPlaces(lat: Double, lng:Double) {
        val url = "${NEAR_BY_SEARCH_URL}location=${lat},${lng}" +
                "&radius=1000" +
                "&type=hospital" +
                "&key=${BuildConfig.MAPS_API_KEY}"
        uiScope.launch(Dispatchers.IO) {
            placesLiveData.postValue(PlacesRepository.getPlaces(url))
        }
    }

    companion object {
        const val NEAR_BY_SEARCH_URL = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?"
    }

}