package com.app.meditec.ui.maps

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.app.meditec.models.PlaceInfo
import com.app.meditec.models.Route
import com.app.meditec.repository.MapsRepository
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MapsViewModel : ViewModel() {
    private val viewModelJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)
    private val _placesLiveData = MutableLiveData<List<PlaceInfo>>()
    private val _routesLiveData = MutableLiveData<List<Route>>()
    private val _responseStatus = MutableLiveData<String>()

    val placesLiveData: LiveData<List<PlaceInfo>>
        get() = _placesLiveData

    val routeLiveData: LiveData<List<Route>>
        get() = _routesLiveData

    val responseStatus: LiveData<String>
        get() = _responseStatus

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    fun getPlaces(lat: Double, lng:Double) {
        uiScope.launch {
            try {
                _placesLiveData.postValue(MapsRepository.getPlaces(lat, lng))
            } catch (t: Throwable){
                _responseStatus.postValue(t.message)
            }
        }
    }

    fun getDirections(currentLocation: LatLng, endPlaceId: String, mode: String = "driving"){
        uiScope.launch {
            try {
                _routesLiveData.postValue(MapsRepository.getDirections(currentLocation, endPlaceId, mode))
            } catch (t: Throwable){
                _responseStatus.postValue("An error occurred.\nCheck your internet connection.")
            }
        }
    }

}