package com.app.meditec.ui.maps

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.app.meditec.models.PlaceInfo
import com.app.meditec.repository.PlacesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MapsViewModel : ViewModel() {
    private val viewModelJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)
    private val _placesLiveData = MutableLiveData<List<PlaceInfo>>()
    private val _placesResponseStatus = MutableLiveData<String>()

    val placesLiveData: LiveData<List<PlaceInfo>>
        get() = _placesLiveData

    val placesResponseStatus: LiveData<String>
        get() = _placesResponseStatus

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    fun getPlaces(lat: Double, lng:Double) {
        uiScope.launch {
            try {
                _placesLiveData.postValue(PlacesRepository.getPlaces(lat, lng))
            } catch (t: Throwable){
                _placesResponseStatus.postValue(t.message)
            }
        }
    }

}