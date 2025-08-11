package com.geolocation

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.module.annotations.ReactModule

@ReactModule(name = GeolocationModule.NAME)
class GeolocationModule(reactContext: ReactApplicationContext) :
  NativeGeolocationSpec(reactContext) {
    private lateinit var locationManager: BaseLocationManager  // LocationManager를 사용할지, Google Play Service를 사용할지 선택

  override fun getName(): String {
    return NAME
  }

  // Example method
  // See https://reactnative.dev/docs/native-modules-android
  override fun setConfiguration(config: ReadableMap?) {
    TODO("Not yet implemented")
  }

  override fun requestAuthorization() {
    TODO("Not yet implemented")
  }

  companion object {
    const val NAME = "Geolocation"
  }
}
