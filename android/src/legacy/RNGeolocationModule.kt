import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import com.geolocation.GeolocationModule

class RNGeolocationModule(reactContext: ReactApplicationContext): ReactContextBaseJavaModule(reactContext) {
  val geolocationModule : GeolocationModule = GeolocationModule(reactContext)

  override fun getName(): String  = geolocationModule.name

  @ReactMethod
   fun setConfiguration(config: ReadableMap) {
    geolocationModule.setConfiguration(config)
  }

  @ReactMethod
   fun requestAuthorization(promise: Promise) {
    geolocationModule.requestAuthorization(promise)
  }
@ReactMethod
   fun getCurrentPosition(options: ReadableMap, promise: Promise) {
    geolocationModule.getCurrentPosition(options,promise)
  }

  @ReactMethod
   fun startObserving(options: ReadableMap) {
    geolocationModule.startObserving(options)
  }

  @ReactMethod
   fun stopObserving() {
    geolocationModule.stopObserving()
  }
}
