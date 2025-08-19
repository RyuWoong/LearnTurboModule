package com.geolocation

import android.Manifest
import com.facebook.react.bridge.Callback
import com.facebook.react.bridge.JavaOnlyArray
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.PromiseImpl
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableNativeMap
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.modules.permissions.PermissionsModule


@ReactModule(name = GeolocationModule.NAME)
class GeolocationModule(reactContext: ReactApplicationContext) :
  NativeGeolocationSpec(reactContext) {
    private lateinit var locationManager: BaseLocationManager  // LocationManager를 사용할지, Google Play Service를 사용할지 선택
    private lateinit var configuration: Configuration

  override fun getName(): String {
    return NAME
  }

  override fun setConfiguration(config: ReadableMap?) {
    configuration = if(config == null) {
      Configuration("auto")
    } else {
      Configuration.fromReactMap(config)
    }
  }

  override fun requestAuthorization(promise: Promise) {
    val perms = getReactApplicationContext().getNativeModule<PermissionsModule?>(PermissionsModule::class.java)
    val permissions = arrayListOf<String>();
    permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
    permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);

    val onPermissionGranted = Callback { args ->
      val result = args[0] as WritableNativeMap
      if (result.getString(Manifest.permission.ACCESS_COARSE_LOCATION) == "granted") {
        promise.resolve("grated")
      } else {
        promise.reject(GeolocationError.PermissionDenied("Location permission was not granted." ))
      }
    }

    val onPermissionDenied = Callback { args ->
      promise.reject(GeolocationError.PermissionDenied("Failed to request location permission." ))
    }

    val onPermissionCheckFailed = Callback { args ->
      promise.reject(GeolocationError.PermissionDenied("Failed to check location permission." ))
    }

    val onPermissionChecked = Callback { args ->
      val hasPermission = args[0] as Boolean
      if (!hasPermission) {
        val permissionsArray = JavaOnlyArray.from(permissions)
        perms?.requestMultiplePermissions(permissionsArray, PromiseImpl(onPermissionGranted, onPermissionDenied))
      } else {
        promise.resolve("grated")
      }
    }

    perms?.checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, PromiseImpl(
      onPermissionChecked,
      Callback {
        perms.checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, PromiseImpl(onPermissionChecked, onPermissionCheckFailed))
      }
    ))
    return
  }

  override fun getCurrentPosition(promise: Promise?) {
    TODO("Not yet implemented")
  }

  override fun startObserving() {
    TODO("Not yet implemented")
  }

  override fun stopObserving() {
    TODO("Not yet implemented")
  }

  companion object {
    const val NAME = "Geolocation"
  }

  private class Configuration(val locationProvider: String) {

    companion object {
      fun fromReactMap(map: ReadableMap): Configuration {
        val locationProvider = if (map.hasKey("locationProvider")) {
          map.getString("locationProvider") ?: "auto"
        } else {
          "auto"
        }

        return Configuration(locationProvider)
      }
    }
  }
}
