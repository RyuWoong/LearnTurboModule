package com.geolocation

import android.Manifest
import com.facebook.react.bridge.Callback
import com.facebook.react.bridge.JavaOnlyArray
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.PromiseImpl
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableNativeMap
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.modules.permissions.PermissionsModule
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import java.util.Objects


@ReactModule(name = GeolocationModule.NAME)
class GeolocationModule(val reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {
    private var locationManager: BaseLocationManager = AndroidLocationManager(reactContext) // LocationManager를 사용할지, Google Play Service를 사용할지 선택
    private var configuration: Configuration = Configuration("auto")

  override fun getName(): String {
    return NAME
  }

  fun setConfiguration(config: ReadableMap?) {
    configuration = if(config == null) {
      Configuration("auto")
    } else {
      Configuration.fromReactMap(config)
    }
    onConfingurationChange(configuration)
  }

  private fun onConfingurationChange(config: Configuration) {
    if(Objects.equals(config.locationProvider, "android") && locationManager is PlayServicesLocationManager) {
      locationManager = AndroidLocationManager(reactContext)
    }

    if(Objects.equals(config.locationProvider, "playServices") && locationManager is AndroidLocationManager) {
      val availability = GoogleApiAvailability()
      if(availability.isGooglePlayServicesAvailable(reactContext.applicationContext) == ConnectionResult.RESULT_SUCCESS.errorCode){
        locationManager = PlayServicesLocationManager(reactContext)
      }
    }
  }

   fun requestAuthorization(promise: Promise) {
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

   fun getCurrentPosition(options: ReadableMap, promise: Promise) {
     try {
      locationManager.getCurrentLocation(options,promise)
     } catch (e: SecurityException) {
       locationPermissionMissing(e)
     }
  }

   fun startObserving(options: ReadableMap) {
     try {
       locationManager.startObserving(options)
     } catch (e: SecurityException) {
       locationPermissionMissing(e)
     }
  }

   fun stopObserving() {
     locationManager.stopObserving()

   }

  private fun locationPermissionMissing(e: SecurityException) {
    val message = "Looks like the app doesn't have the permission to access location.\n" +
      "Add the following line to your app's AndroidManifest.xml:\n" +
      "<uses-permission android:name=\"android.permission.ACCESS_FINE_LOCATION\" />\n" +
      e.message
    GeolocationError.PermissionDenied(message)
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
