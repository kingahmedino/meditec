package com.app.meditec.ui

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
    val placesLiveData = MutableLiveData<List<PlaceInfo>>()

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    fun getPlaces(lat: Double, lng:Double) {
        uiScope.launch(Dispatchers.IO) {
            placesLiveData.postValue(PlacesRepository.getPlaces(lat, lng))
        }
    }

}