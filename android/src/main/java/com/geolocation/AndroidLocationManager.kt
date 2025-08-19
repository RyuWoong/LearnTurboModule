package com.geolocation

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.common.SystemClock
import com.facebook.react.modules.core.DeviceEventManagerModule
import kotlinx.coroutines.Runnable


@SuppressLint("MissingPermission")
class AndroidLocationManager(context: ReactApplicationContext): BaseLocationManager(context) {
  private var watchProvider: String? = null

  private val locationListener = object : LocationListener {
    override fun onLocationChanged(location: Location) {
      context.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java).emit("geolocationDidChange",locationToMap(location))
    }

    override fun onProviderDisabled(provider: String) {
      emitError(
        PositionErrorCode.POSITION_UNAVAILABLE.code,
        "Provider $provider is out of service."
      )
    }
  }

  override fun getCurrentLocation(
    options: ReadableMap,
    promise: Promise
  ) {
    val locationOptions = LocationOptions.fromReactMap(options)

    try {
      val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
      val provider = getValidProvider(locationManager, locationOptions.highAccuracy)

      if (provider == null) {
        throw GeolocationError.PermissionDenied("Provider Not Found")
      }

      val location = locationManager.getLastKnownLocation(provider)

      if (location != null && (SystemClock.currentTimeMillis() - location.getTime()) < locationOptions.maximumAge) {
        promise.resolve(locationToMap(location));
        return;
      }

      } catch (e: GeolocationError) {
      promise.reject(e)
    } catch (e: SecurityException) {
      promise.reject(GeolocationError.PermissionDenied())
    } catch (e: Exception) {
      promise.reject(GeolocationError.Unknown("getCurrentLocation Fail"))
    }
  }

  /**
   *  실시간 위치 추적 시작
   */
  override fun startObserving(options: ReadableMap) {
    if(LocationManager.GPS_PROVIDER.equals(watchProvider)) {
      return;
    }

    val locationOptions = LocationOptions.fromReactMap(options)

    try {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val provider = getValidProvider(locationManager, locationOptions.highAccuracy)
        if(provider == null) {
          emitError(PositionErrorCode.POSITION_UNAVAILABLE.code, "No Location provider available")
          return;
        }

      if(!provider.equals(watchProvider)) {
        locationManager.removeUpdates(locationListener)
        locationManager.requestLocationUpdates(provider,1000,locationOptions.distanceFilter,locationListener)
      }

      watchProvider = provider


    } catch (e: SecurityException) {
        throw e
    }
  }

  /**
   *  실시간 위치 추적 중지
   */
  override fun stopObserving() {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    locationManager.removeUpdates(locationListener)
    watchProvider = null
  }

  /**
   * 유효한 위치 제공자 찾기 + 권한 체크
   */
  private fun getValidProvider(locationManager: LocationManager, highAccuracy: Boolean): String? {
    val provider = checkProvider(locationManager,highAccuracy);

    val hasPermission = when (provider) {
      LocationManager.GPS_PROVIDER -> {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
      }
      LocationManager.NETWORK_PROVIDER -> {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
      }
      else -> false
    }

    if(!hasPermission) {
        return null;
    }

    return provider
  }

  private fun checkProvider(locationManager: LocationManager, highAccuracy: Boolean): String? {
    var provider =  if (highAccuracy) LocationManager.GPS_PROVIDER else LocationManager.NETWORK_PROVIDER
    if(!locationManager.isProviderEnabled(provider)) { // 해당 제공자를 사용할 수 있느냐 없으면 다른 제공자로,
      provider = if(provider.equals(LocationManager.GPS_PROVIDER)) LocationManager.NETWORK_PROVIDER else LocationManager.GPS_PROVIDER
      if(!locationManager.isProviderEnabled(provider)) {
        return null;
      }
    }
    return provider
  }


  /**
   * 일회성 위치 요청 처리 클래스
   */
  private inner class SingleUpdateRequest(
    private  val locationManager: LocationManager,
    private val provider: String,
    private val timeout: Long,
    private val promise: Promise,
  ) {
      private var oldLocation: Location? = null;
      private var isTriggered = false;
      private val handler = Handler(Looper.getMainLooper())
      private val timeoutRunnable: Runnable = Runnable {
        synchronized(this) {
          if(!isTriggered) {
            promise.reject(GeolocationError.TimeOut("Location request timed out"))
            locationManager.removeUpdates(singleLocationListener)
            isTriggered = true
          }
        }
      }

    private val singleLocationListener = object : LocationListener {
      override fun onLocationChanged(location: Location) {
        synchronized(this@SingleUpdateRequest) {
          if (!isTriggered && isBetterLocation(location, oldLocation)) {
            promise.resolve(locationToMap(location))
            handler.removeCallbacks(timeoutRunnable)
            isTriggered = true
            locationManager.removeUpdates(this)
          }
          oldLocation = location
        }
      }

      override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
      override fun onProviderEnabled(provider: String) {}
      override fun onProviderDisabled(provider: String) {}
    }

    fun invoke(location: Location?) {
      oldLocation = location
      locationManager.requestLocationUpdates(provider, 100, 1f, singleLocationListener)
      handler.postDelayed(timeoutRunnable, timeout)
    }

    private fun isBetterLocation(location: Location, currentBestLocation: Location?): Boolean {
      if (currentBestLocation == null) {
        // 새로운 위치는 항상 위치 없음보다 좋음
        return true
      }

      // 새로운 위치와 현재 최고 위치의 시간 차이 확인
      val timeDelta = location.time - currentBestLocation.time
      val isSignificantlyNewer = timeDelta > TWO_MINUTES
      val isSignificantlyOlder = timeDelta < -TWO_MINUTES
      val isNewer = timeDelta > 0

      // 2분 이상 지났으면 새로운 위치 사용 (사용자가 이동했을 가능성)
      if (isSignificantlyNewer) {
        return true
      } else if (isSignificantlyOlder) {
        // 2분 이상 오래된 위치는 더 나쁨
        return false
      }

      // 새로운 위치의 정확도 확인
      val accuracyDelta = location.accuracy - currentBestLocation.accuracy
      val isLessAccurate = accuracyDelta > 0
      val isMoreAccurate = accuracyDelta < 0
      val isSignificantlyLessAccurate = accuracyDelta > 200

      // 같은 제공자인지 확인
      val isFromSameProvider = isSameProvider(location.provider, currentBestLocation.provider)

      // 시간과 정확도를 조합하여 위치 품질 결정
      return when {
        isMoreAccurate -> true
        isNewer && !isLessAccurate -> true
        isNewer && !isSignificantlyLessAccurate && isFromSameProvider -> true
        else -> false
      }
    }

    /**
     * 두 제공자가 같은지 확인
     */
    private fun isSameProvider(provider1: String?, provider2: String?): Boolean {
      return when {
        provider1 == null -> provider2 == null
        else -> provider1 == provider2
      }
    }
  }

  companion object {
    private const val TWO_MINUTES = 1000 * 60 * 2
  }
}
