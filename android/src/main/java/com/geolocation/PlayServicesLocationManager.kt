package com.geolocation;

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableMap
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient

class PlayServicesLocationManager(context: ReactApplicationContext) : BaseLocationManager(context) {
  private var locationClient: FusedLocationProviderClient
  private var settingsClient : SettingsClient

  init {
    // 기기의 GPS, Wi-Fi, 모바일 네트워크 등을 이용해서 가장 정확하고 배터리 효율적인 위치를 찾아줌
      locationClient = LocationServices.getFusedLocationProviderClient(context)
    // 이 클라이언트는 위치 설정이 적절한지 확인하고, 만약 설정이 잘못되었다면 사용자에게 바로 설정을 켜도록 유도하는 다이얼로그를 띄워줌
      settingsClient = LocationServices.getSettingsClient(context)
  }

   override fun getCurrentLocation(
     options: ReadableMap,
     promise: Promise
   ) {
     val locationOptions = LocationOptions.fromReactMap(options)
     val currentActivity = context.currentActivity;

     if(currentActivity == null) {

     }

   }

   override fun startObserving(options: ReadableMap) {
     TODO("Not yet implemented")
   }

   override fun stopObserving() {
     TODO("Not yet implemented")
   }


  private fun checkLocationSettings(options: ReadableMap, locationCallback: LocationCallback, promise: Promise) {
    val locationOptions = LocationOptions.fromReactMap(options)
    val requestBuilder = LocationRequest.Builder(locationOptions.interval.toLong())
    requestBuilder.setPriority(if(locationOptions.highAccuracy) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_LOW_POWER)
    requestBuilder.setMaxUpdateAgeMillis(locationOptions.maximumAge.toLong())

    if (locationOptions.fastestInterval >= 0) {
      requestBuilder.setMinUpdateIntervalMillis(locationOptions.fastestInterval.toLong());
    }

    if (locationOptions.distanceFilter >= 0) {
      requestBuilder.setMinUpdateDistanceMeters(locationOptions.distanceFilter);
    }

    val locationRequest = requestBuilder.build()

    val settingsBuilder = LocationSettingsRequest.Builder()
    settingsBuilder.addLocationRequest(locationRequest)

    val locationSettingsRequest = settingsBuilder.build()
    settingsClient.checkLocationSettings(locationSettingsRequest)
      .addOnSuccessListener { response ->

    }
      .addOnFailureListener { err ->

        if(isAnyProviderAvailable()) {
          requestLocationUpdates(locationRequest, locationCallback)
          return@addOnFailureListener
        }

        if(promise != null) {
          promise.reject(GeolocationError.PositionUnavailable("Location not available (FusedLocationProvider/settings)."))
          return@addOnFailureListener
        }

        promise.reject(GeolocationError.PositionUnavailable("Location not available (FusedLocationProvider/settings)."))
      }




  }

  private fun requestLocationUpdates(locationRequest: LocationRequest, locationCallback: LocationCallback) {

  }

  private fun isAnyProviderAvailable(): Boolean {
    return false
  }

 }

