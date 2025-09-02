import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableMap
import com.geolocation.GeolocationModule
import com.geolocation.NativeGeolocationSpec

class RNGeolocationModule(reactContext: ReactApplicationContext): NativeGeolocationSpec(reactContext) {

  val geolocationModule : GeolocationModule = GeolocationModule(reactContext)

  override fun setConfiguration(config: ReadableMap) {
    geolocationModule.setConfiguration(config)
  }

  override fun requestAuthorization(promise: Promise) {
    geolocationModule.requestAuthorization(promise)
  }

  override fun getCurrentPosition(options: ReadableMap, promise: Promise) {
    geolocationModule.getCurrentPosition(options,promise)
  }

  override fun startObserving(options: ReadableMap) {
    geolocationModule.startObserving(options)
  }

  override fun stopObserving() {
    geolocationModule.stopObserving()
  }


}
