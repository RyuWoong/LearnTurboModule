package com.geolocation

sealed class GeolocationError(
  val code: String,
  override val message: String,
  cause: Throwable? = null
): Exception(message,cause) {

  class PermissionDenied(message: String = "Location permission is required."): GeolocationError(code = "PERMISSION_DENIED", message = message)

  class PositionUnavailable(message: String): GeolocationError( code = "POSITION_UNAVAILABLE", message = message)

  class TimeOut(message: String):  GeolocationError(code = "TIMEOUT", message = message)

  class ActivityNull(message: String): GeolocationError(code = "ACTIVITY_NULL", message = message)

  class Unknown(message: String, cause: Throwable? = null): GeolocationError(code = "UNKNOWN_ERROR", message = message, cause = cause)
}
