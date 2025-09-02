package com.geolocation;

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.common.SystemClock
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient


@SuppressLint("MissingPermission")
class PlayServicesLocationManager(context: ReactApplicationContext) : BaseLocationManager(context) {
  // 기기의 GPS, Wi-Fi, 모바일 네트워크 등을 이용해서 가장 정확하고 배터리 효율적인 위치를 찾아줌\
  private var locationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

  // 이 클라이언트는 위치 설정이 적절한지 확인하고, 만약 설정이 잘못되었다면 사용자에게 바로 설정을 켜도록 유도하는 다이얼로그를 띄워줌
  private var settingsClient : SettingsClient = LocationServices.getSettingsClient(context)

  private var locationCallback: LocationCallback? = null

  private var singleLocationCallback: LocationCallback? = null



   override fun getCurrentLocation(
     options: ReadableMap,
     promise: Promise
   ) {
     val locationOptions = LocationOptions.fromReactMap(options)
     val currentActivity = context.currentActivity;

     if(currentActivity == null) {
       singleLocationCallback = createSingleLocationCallback(promise);
       singleLocationCallback?.let { checkLocationSettings(options, it,promise) }
       return;
     }

     try {
         locationClient.getLastLocation()
           .addOnSuccessListener(currentActivity) {  location ->
             if(location != null && (SystemClock.currentTimeMillis() - location.time) < locationOptions.maximumAge) {
                promise.resolve(locationToMap(location))
             } else {
               singleLocationCallback = createSingleLocationCallback(promise);
               singleLocationCallback?.let {   checkLocationSettings(options, it, promise);}
             }
         }
     } catch (e: SecurityException) {
        throw e
     }
   }

   override fun startObserving(options: ReadableMap) {
     locationCallback = object :LocationCallback() {
       public override fun onLocationResult(locationResult: LocationResult) {
         locationResult.lastLocation?.let {
           context.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java).emit("geolocationDidChange", locationToMap(it))
         }
       }
     }

     locationCallback?.let { checkLocationSettings(options,it,null) }
   }

   override fun stopObserving() {
     locationCallback?.let {
       locationClient.removeLocationUpdates(it)
     }
   }


  private fun checkLocationSettings(options: ReadableMap, locationCallback: LocationCallback, promise: Promise?) {
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
        requestLocationUpdates(locationRequest,locationCallback)
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

        promise?.reject(GeolocationError.PositionUnavailable("Location not available (FusedLocationProvider/settings)."))
      }
  }

  private fun requestLocationUpdates(locationRequest: LocationRequest, locationCallback: LocationCallback) {
     try {
       locationClient.requestLocationUpdates(locationRequest,locationCallback, Looper.getMainLooper())
     } catch (e: SecurityException) {
       throw e
     }
  }

  private fun isAnyProviderAvailable(): Boolean {
    if(context == null) {
      return false
    }

    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    return locationManager != null && (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
      LocationManager.NETWORK_PROVIDER))
  }

  private fun createSingleLocationCallback(promise: Promise): LocationCallback {
    return object : LocationCallback() {
      public override fun onLocationResult(locationResult: LocationResult) {
        val location: Location? = locationResult.getLastLocation()

        if (location == null) {
          promise.reject(
            "POSITION_UNAVAILABLE",
            "No location provided (FusedLocationProvider/lastLocation)."
          )
          return
        }

        promise.resolve(locationToMap(location))

        // null 체크를 추가하여 안전하게 처리
        singleLocationCallback?.let { callback ->
          locationClient.removeLocationUpdates(callback)
        }
        singleLocationCallback = null
      }

      public override fun onLocationAvailability(locationAvailability: LocationAvailability) {
        if (!locationAvailability.isLocationAvailable()) {
          promise.reject(
            "POSITION_UNAVAILABLE",
            "Location not available (FusedLocationProvider/lastLocation)."
          )
        }
      }
    }
  }



 }

