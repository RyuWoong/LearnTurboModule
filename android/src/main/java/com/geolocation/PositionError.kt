package com.geolocation

import android.os.Message
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap

enum class PositionErrorCode(val code: Int) {
  UNKNOWN_ERROR(0),
  /**
   * 위치 정보 획득 권한이 거부된 경우
   * 페이지에 위치 정보에 대한 권한이 없어서 실패
   */
  PERMISSION_DENIED(1),
  /**
   * 위치 정보 획득이 불가능한 경우
   * 최소 하나 이상의 내부 위치 소스에서 내부 오류가 발생
   */
  POSITION_UNAVAILABLE(2),
  /**
   * 위치 정보 획득 시간 초과
   * PositionOptions.timeout에 정의된 시간 내에 정보를 얻지 못함
   */
  TIMEOUT(3),
  /**
   * 현재 Activity가 null인 경우
   * 로직에서 non-null Activity가 필요하지만 getCurrentActivity()가 null을 반환
   * 이 에러로 사용자에게 Android 내부 오류를 알릴 수 있음
   */
  ACTIVITY_NULL(4);

  companion object {
    fun fromInt(code: Int): PositionErrorCode = values().firstOrNull { it.code == code } ?: UNKNOWN_ERROR
  }
}

class PositionError {
  companion object {
    /**
     * 에러 객체 생성 함수
     * React Native JavaScript로 전달할 표준 에러 객체 생성
     *
     * @param code 에러 코드 (PERMISSION_DENIED, POSITION_UNAVAILABLE, TIMEOUT, ACTIVITY_NULL)
     * @param message 에러 메시지 (nullable)
     * @return WritableMap 형태의 에러 객체
     */
    fun buildError(code: PositionErrorCode, message: String): WritableMap {
      val error: WritableMap = Arguments.createMap().apply {
        putInt("code",code.code)
        putString("message", message)

        putInt("PERMISSION_DENIED", PositionErrorCode.PERMISSION_DENIED.code)
        putInt("POSITION_UNAVAILABLE", PositionErrorCode.POSITION_UNAVAILABLE.code)
        putInt("TIMEOUT", PositionErrorCode.TIMEOUT.code)
        putInt("ACTIVITY_NULL", PositionErrorCode.ACTIVITY_NULL.code)
      }

      return error;
    }
  }
}
