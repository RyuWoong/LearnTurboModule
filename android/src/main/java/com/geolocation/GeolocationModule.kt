package com.geolocation

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.facebook.react.bridge.*
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.google.android.gms.location.*

@ReactModule(name = GeolocationModule.NAME)
class GeolocationModule(reactContext: ReactApplicationContext) :
  NativeGeolocationSpec(reactContext) {

  private val fusedLocationClient: FusedLocationProviderClient =
    LocationServices.getFusedLocationProviderClient(reactContext)
  
  private var watchCallbacks = mutableMapOf<Int, LocationCallback>()
  private var currentWatchId = 0
  private var authorizationPromise: Promise? = null

  override fun getName(): String {
    return NAME
  }

  // MARK: - Geolocation Methods

  override fun requestAuthorization(promise: Promise) {
    Log.d(TAG, "🔐 requestAuthorization 호출")
    
    val status = getAuthorizationStatus()
    
    when (status) {
      "granted" -> {
        Log.d(TAG, "   ✅ 이미 권한 있음")
        promise.resolve(status)
      }
      "denied" -> {
        Log.d(TAG, "   ❌ 권한 거부됨")
        promise.resolve(status)
      }
      "notDetermined" -> {
        Log.d(TAG, "   📝 권한 요청 시작")
        authorizationPromise = promise
        requestLocationPermission()
      }
      else -> {
        promise.resolve("unknown")
      }
    }
  }

  override fun getCurrentPosition(options: ReadableMap?, promise: Promise) {
    Log.d(TAG, "📍 getCurrentPosition 호출")
    
    if (!hasLocationPermission()) {
      Log.d(TAG, "   ❌ 권한 없음")
      promise.reject("PERMISSION_DENIED", "Location permission not granted")
      return
    }

    Log.d(TAG, "   ✅ 권한 확인됨")
    
    try {
      val locationRequest = createLocationRequest(options)
      
      fusedLocationClient.getCurrentLocation(
        LocationRequest.PRIORITY_HIGH_ACCURACY,
        null
      ).addOnSuccessListener { location: Location? ->
        if (location != null) {
          Log.d(TAG, "   ✅ 위치 가져오기 성공")
          val locationMap = locationToMap(location)
          promise.resolve(locationMap)
        } else {
          Log.d(TAG, "   ⚠️ 위치가 null")
          promise.reject("LOCATION_ERROR", "Unable to get location")
        }
      }.addOnFailureListener { e ->
        Log.e(TAG, "   ❌ 위치 가져오기 실패: ${e.message}")
        promise.reject("LOCATION_ERROR", e.message, e)
      }
    } catch (e: SecurityException) {
      Log.e(TAG, "   ❌ SecurityException: ${e.message}")
      promise.reject("PERMISSION_DENIED", e.message, e)
    }
  }

  override fun watchPosition(options: ReadableMap?, promise: Promise) {
    Log.d(TAG, "👁️ watchPosition 시작")
    
    if (!hasLocationPermission()) {
      Log.d(TAG, "   ❌ 권한 없음")
      promise.reject("PERMISSION_DENIED", "Location permission not granted")
      return
    }

    Log.d(TAG, "   ✅ 권한 확인됨")
    
    try {
      val locationRequest = createLocationRequest(options)
      currentWatchId++
      val watchId = currentWatchId
      
      Log.d(TAG, "   📝 Watch ID: $watchId")
      Log.d(TAG, "   📏 간격: ${locationRequest.interval}ms")
      
      val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
          Log.d(TAG, "🗺️ [watchPosition] 위치 업데이트!")
          
          locationResult.lastLocation?.let { location ->
            Log.d(TAG, "   📍 위도: ${location.latitude}")
            Log.d(TAG, "   📍 경도: ${location.longitude}")
            Log.d(TAG, "   ⏰ 시간: ${System.currentTimeMillis()}")
            Log.d(TAG, "   📡 이벤트 전송: onLocationChanged")
            
            val locationMap = locationToMap(location)
            sendEvent("onLocationChanged", locationMap)
          }
        }

        override fun onLocationAvailability(availability: LocationAvailability) {
          if (!availability.isLocationAvailable) {
            Log.w(TAG, "   ⚠️ 위치 사용 불가")
            val errorMap = Arguments.createMap().apply {
              putInt("code", 2)
              putString("message", "Location not available")
            }
            sendEvent("onLocationError", errorMap)
          }
        }
      }
      
      watchCallbacks[watchId] = locationCallback
      
      fusedLocationClient.requestLocationUpdates(
        locationRequest,
        locationCallback,
        Looper.getMainLooper()
      ).addOnSuccessListener {
        Log.d(TAG, "   ▶️ requestLocationUpdates 성공")
        promise.resolve(watchId)
      }.addOnFailureListener { e ->
        Log.e(TAG, "   ❌ requestLocationUpdates 실패: ${e.message}")
        watchCallbacks.remove(watchId)
        promise.reject("LOCATION_ERROR", e.message, e)
      }
      
    } catch (e: SecurityException) {
      Log.e(TAG, "   ❌ SecurityException: ${e.message}")
      promise.reject("PERMISSION_DENIED", e.message, e)
    }
  }

  override fun clearWatch(watchId: Double) {
    val id = watchId.toInt()
    Log.d(TAG, "🛑 clearWatch: $id")
    
    watchCallbacks[id]?.let { callback ->
      fusedLocationClient.removeLocationUpdates(callback)
      watchCallbacks.remove(id)
      Log.d(TAG, "   ✅ Watch $id 제거됨")
    }
  }

  override fun stopObserving() {
    Log.d(TAG, "🛑 stopObserving")
    
    watchCallbacks.values.forEach { callback ->
      fusedLocationClient.removeLocationUpdates(callback)
    }
    watchCallbacks.clear()
    
    Log.d(TAG, "   ✅ 모든 watch 제거됨")
  }

  // MARK: - Event Emitter

  override fun addListener(eventName: String) {
    Log.d(TAG, "👂 addListener: $eventName")
  }

  override fun removeListeners(count: Double) {
    Log.d(TAG, "🔇 removeListeners: $count")
  }

  // MARK: - Private Methods

  private fun hasLocationPermission(): Boolean {
    val finePermission = ContextCompat.checkSelfPermission(
      reactApplicationContext,
      Manifest.permission.ACCESS_FINE_LOCATION
    )
    val coarsePermission = ContextCompat.checkSelfPermission(
      reactApplicationContext,
      Manifest.permission.ACCESS_COARSE_LOCATION
    )
    return finePermission == PackageManager.PERMISSION_GRANTED ||
           coarsePermission == PackageManager.PERMISSION_GRANTED
  }

  private fun getAuthorizationStatus(): String {
    return when {
      hasLocationPermission() -> "granted"
      shouldShowRequestPermissionRationale() -> "denied"
      else -> "notDetermined"
    }
  }

  private fun shouldShowRequestPermissionRationale(): Boolean {
    val activity = currentActivity ?: return false
    return ActivityCompat.shouldShowRequestPermissionRationale(
      activity,
      Manifest.permission.ACCESS_FINE_LOCATION
    )
  }

  private fun requestLocationPermission() {
    val activity = currentActivity
    if (activity == null) {
      authorizationPromise?.reject("ERROR", "Activity is null")
      authorizationPromise = null
      return
    }

    ActivityCompat.requestPermissions(
      activity,
      arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
      ),
      PERMISSION_REQUEST_CODE
    )
  }

  private fun createLocationRequest(options: ReadableMap?): LocationRequest {
    val priority = if (options?.hasKey("enableHighAccuracy") == true &&
                      options.getBoolean("enableHighAccuracy")) {
      LocationRequest.PRIORITY_HIGH_ACCURACY
    } else {
      LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
    }

    val interval = if (options?.hasKey("timeout") == true) {
      options.getDouble("timeout").toLong()
    } else {
      10000L // 기본 10초
    }

    val distanceFilter = if (options?.hasKey("distanceFilter") == true) {
      options.getDouble("distanceFilter").toFloat()
    } else {
      0f // 모든 업데이트 받기
    }

    Log.d(TAG, "   🎯 priority: $priority")
    Log.d(TAG, "   ⏱️ interval: ${interval}ms")
    Log.d(TAG, "   📏 distanceFilter: ${distanceFilter}m")

    return LocationRequest.create().apply {
      this.priority = priority
      this.interval = interval
      this.fastestInterval = interval / 2
      this.smallestDisplacement = distanceFilter
    }
  }

  private fun locationToMap(location: Location): WritableMap {
    return Arguments.createMap().apply {
      putMap("coords", Arguments.createMap().apply {
        putDouble("latitude", location.latitude)
        putDouble("longitude", location.longitude)
        putDouble("altitude", location.altitude)
        putDouble("accuracy", location.accuracy.toDouble())
        if (location.hasVerticalAccuracy()) {
          putDouble("altitudeAccuracy", location.verticalAccuracyMeters.toDouble())
        }
        if (location.hasBearing()) {
          putDouble("heading", location.bearing.toDouble())
        }
        if (location.hasSpeed()) {
          putDouble("speed", location.speed.toDouble())
        }
      })
      putDouble("timestamp", location.time.toDouble())
    }
  }

  private fun sendEvent(eventName: String, params: WritableMap?) {
    reactApplicationContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      ?.emit(eventName, params)
  }

  companion object {
    const val NAME = "Geolocation"
    private const val TAG = "GeolocationModule"
    private const val PERMISSION_REQUEST_CODE = 1001
  }
}