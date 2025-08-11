package com.geolocation

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext

import android.location.Location
import android.os.Build

import com.facebook.react.bridge.Callback
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.DeviceEventManagerModule

abstract class BaseLocationManager(val context: ReactApplicationContext) {
  private companion object {
    private const val DEFAULT_LOCATION_ACCURACY = 100f;

  }

  fun locationToMap(location: Location) {
    val map = Arguments.createMap();
    val coords = Arguments.createMap().apply {
        // 기본 좌표 정보
        putDouble("latitude", location.getLatitude());
        putDouble("longitude", location.getLongitude());
        putDouble("altitude", location.getAltitude());
        putDouble("accuracy", location.getAccuracy().toDouble());
        putDouble("heading", location.getBearing().toDouble());
        putDouble("speed", location.getSpeed().toDouble());
     }

    map.putMap("coords", coords);
    map.putLong("timestamp", location.getTime());

    location.extras?.let { bundle ->
      val extras = Arguments.createMap()
      bundle.keySet().forEach { key ->
        bundle.get(key)?.let { value ->
            putIntoMap(extras,key,value)
        }
      }
      map.putMap("extars",extras)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      map.putBoolean("mocked", location.isMock())
    }
  }

  private fun putIntoMap(map: WritableMap, key: String, value: Any) {
    when (value) {
      is Int -> map.putInt(key, value)
      is Long -> map.putInt(key, value.toInt()) // JS는 64bit Long 미지원
      is Float -> map.putDouble(key, value.toDouble())
      is Double -> map.putDouble(key, value)
      is String -> map.putString(key, value)
      is Boolean -> map.putBoolean(key, value)
      is IntArray, is LongArray, is DoubleArray,
      is Array<*>, is BooleanArray -> {
        map.putArray(key, Arguments.fromArray(value))
      }
      // 기타 타입은 무시 (안전성)
    }
  }

  protected fun emitError(code:Int, message: String) {
    this.context.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java).emit("geolocationError",PositionError.buildError(code,message))
  }

  // 추상 메서드들 - 서브클래스에서 구현 필수
  abstract fun getCurrentLocationData(options: ReadableMap)
  abstract fun startObserving(options: ReadableMap)
  abstract fun stopObserving()


  data class LocationOptions(
    val interval: Int, // 위치 업데이트 간격 (ms)
    val fastestInterval: Int, // 최소 업데이트 간격 (ms), -1이면 제한 없음
    val timeout: Long, // 타임아웃 (ms)
    val maximumAge: Double, // 캐시된 위치 최대 사용 시간
    val highAccuracy: Boolean, // 고정밀 모드 (GPS 우선)
    val distanceFilter: Float ) {

    companion object {

      /**
       * React Native ReadableMap을 LocationOptions로 변환
       *
       * @param map React Native에서 전달받은 옵션 맵
       * @return 변환된 LocationOptions 인스턴스
       */
      fun fromReactMap(map: ReadableMap): LocationOptions {
        return LocationOptions(
          interval = map.takeIf { it.hasKey("interval") }
            ?.getInt("interval") ?: 10_000,

          fastestInterval = map.takeIf { it.hasKey("fastestInterval") }
            ?.getInt("fastestInterval") ?: -1,

          // JavaScript에서는 Double로 오지만 Android는 Long 필요
          timeout = map.takeIf { it.hasKey("timeout") }
            ?.getDouble("timeout")?.toLong() ?: (10 * 60 * 1000L),

          maximumAge = map.takeIf { it.hasKey("maximumAge") }
            ?.getDouble("maximumAge") ?: Double.POSITIVE_INFINITY,

          // enableHighAccuracy가 있고 true일 때만 고정밀 모드
          highAccuracy = map.takeIf { it.hasKey("enableHighAccuracy") }
            ?.getBoolean("enableHighAccuracy") ?: false,

          distanceFilter = map.takeIf { it.hasKey("distanceFilter") }
            ?.getDouble("distanceFilter")?.toFloat()
            ?: DEFAULT_LOCATION_ACCURACY
        )
      }
    }
  }
}
