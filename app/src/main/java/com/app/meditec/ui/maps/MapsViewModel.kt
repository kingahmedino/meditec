package com.app.meditec.ui.maps

import android.location.Location
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
    var userCurrentLocation: Location? = null
        set(value) {
            field = value
            if (value != null) {
                getPlaces(value.latitude, value.longitude)
            }
        }
    var currentlySelectedPlace: PlaceInfo? = null
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

    private fun getPlaces(lat: Double, lng:Double) {
        uiScope.launch {
            try {
                _placesLiveData.postValue(MapsRepository.getPlaces(lat, lng))
            } catch (t: Throwable){
                _responseStatus.postValue(t.message)
            }
        }
    }

    private fun getDirections(currentLocation: LatLng, endPlaceId: String, mode: String = "driving"){
        uiScope.launch {
            try {
                _routesLiveData.postValue(MapsRepository.getDirections(currentLocation, endPlaceId, mode))
            } catch (t: Throwable){
                _responseStatus.postValue("An error occurred.\nCheck your internet connection.")
            }
        }
    }

    fun onClickGoFab(){
        Log.d("MapsViewModel", "onClickGoFab: Clicked")
        val latLng = LatLng(userCurrentLocation!!.latitude, userCurrentLocation!!.longitude)
        getDirections(latLng, currentlySelectedPlace!!.place_id)
    }

    fun onWalkImageButton(){
        val latLng = LatLng(userCurrentLocation!!.latitude, userCurrentLocation!!.longitude)
        getDirections(latLng, currentlySelectedPlace!!.place_id, "walking")
    }

    fun onTransitImageButton(){
        val latLng = LatLng(userCurrentLocation!!.latitude, userCurrentLocation!!.longitude)
        getDirections(latLng, currentlySelectedPlace!!.place_id, "transit")
    }

    fun onDriveImageButton(){
        val latLng = LatLng(userCurrentLocation!!.latitude, userCurrentLocation!!.longitude)
        getDirections(latLng, currentlySelectedPlace!!.place_id, "driving")
    }

}