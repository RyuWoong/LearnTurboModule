package com.geolocation

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationListener
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.facebook.react.bridge.Callback
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableMap

@SuppressLint("MissingPermission")
class AndroidLocationManager(context: ReactApplicationContext): BaseLocationManager(context) {
  private var watchProvider = null

  override fun getCurrentLocationData(
    options: ReadableMap,
  ) {
    val locationOptions = LocationOptions.fromReactMap(options)

    try {
      val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
      val provider = getValidProvider(locationManager, locationOptions.highAccuracy)

      if(provider == null) {
        val error = PositionError.buildError(PositionErrorCode.POSITION_UNAVAILABLE, "No Location provider available.")

      }
    } catch (e: SecurityException) {
      throw e
    }
  }

  override fun startObserving(options: ReadableMap) {
    TODO("Not yet implemented")
  }

  override fun stopObserving() {
    TODO("Not yet implemented")
  }

  /**
   *
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

}
