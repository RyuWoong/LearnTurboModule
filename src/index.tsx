import { NativeEventEmitter, NativeModules } from 'react-native';
import NativeGeolocation from './NativeGeolocation';
import type {
  GeolocationOptions,
  GeolocationPosition,
  GeolocationError,
} from './NativeGeolocation';

// Export types
export type { GeolocationOptions, GeolocationPosition, GeolocationError };

// Event emitter
const eventEmitter = new NativeEventEmitter(NativeModules.Geolocation as any);

/**
 * 위치 권한을 요청합니다
 * @returns Promise<string> - 권한 상태 ('authorizedWhenInUse', 'denied', etc.)
 */
export function requestAuthorization(): Promise<string> {
  return NativeGeolocation.requestAuthorization();
}

/**
 * 현재 위치를 한 번 가져옵니다
 * @param options - 위치 옵션 (timeout, accuracy 등)
 * @returns Promise<GeolocationPosition> - 현재 위치 정보
 */
export function getCurrentPosition(
  options?: GeolocationOptions
): Promise<GeolocationPosition> {
  return NativeGeolocation.getCurrentPosition(options);
}

/**
 * 위치 변화를 계속 감시합니다
 * @param options - 위치 옵션
 * @returns Promise<number> - watch ID (clearWatch에 사용)
 */
export function watchPosition(options?: GeolocationOptions): Promise<number> {
  console.log('👁️ [index.tsx] watchPosition 호출, options:', options);
  const result = NativeGeolocation.watchPosition(options);
  result
    .then((watchId) => {
      console.log('✅ [index.tsx] watchPosition 성공, watchId:', watchId);
    })
    .catch((err) => {
      console.error('❌ [index.tsx] watchPosition 에러:', err);
    });
  return result;
}

/**
 * 특정 위치 감시를 중지합니다
 * @param watchId - watchPosition으로부터 받은 ID
 */
export function clearWatch(watchId: number): void {
  return NativeGeolocation.clearWatch(watchId);
}

/**
 * 모든 위치 감시를 중지합니다
 */
export function stopObserving(): void {
  return NativeGeolocation.stopObserving();
}

/**
 * 위치 변경 이벤트 리스너를 추가합니다
 * @param callback - 위치가 변경될 때 호출될 콜백 함수
 * @returns 리스너를 제거할 수 있는 subscription 객체
 */
export function addLocationListener(
  callback: (position: GeolocationPosition) => void
) {
  console.log('👂 [index.tsx] addLocationListener 호출');
  NativeGeolocation.addListener('onLocationChanged');
  const subscription = eventEmitter.addListener(
    'onLocationChanged',
    (position) => {
      console.log('📍 [index.tsx] onLocationChanged 이벤트 받음:', position);
      callback(position);
    }
  );
  console.log('✅ [index.tsx] 이벤트 리스너 등록 완료');
  return subscription;
}

/**
 * 위치 에러 이벤트 리스너를 추가합니다
 * @param callback - 에러가 발생했을 때 호출될 콜백 함수
 * @returns 리스너를 제거할 수 있는 subscription 객체
 */
export function addErrorListener(callback: (error: GeolocationError) => void) {
  NativeGeolocation.addListener('onLocationError');
  const subscription = eventEmitter.addListener('onLocationError', callback);
  return subscription;
}

/**
 * 모든 이벤트 리스너를 제거합니다
 */
export function removeAllListeners(): void {
  eventEmitter.removeAllListeners('onLocationChanged');
  eventEmitter.removeAllListeners('onLocationError');
  NativeGeolocation.removeListeners(2); // 2개의 이벤트 타입
}

// Default export로 모든 함수를 포함한 객체도 제공
export default {
  requestAuthorization,
  getCurrentPosition,
  watchPosition,
  clearWatch,
  stopObserving,
  addLocationListener,
  addErrorListener,
  removeAllListeners,
};
