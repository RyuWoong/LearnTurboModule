import GeolocationModule from './NativeGeolocation';
import type {
  GeolocationOptions,
  GeolocationPosition,
} from './NativeGeolocation';

export type { GeolocationOptions, GeolocationPosition };

// Geolocation API
export async function getCurrentPosition(
  options?: GeolocationOptions
): Promise<GeolocationPosition> {
  return GeolocationModule.getCurrentPosition(options);
}

export async function watchPosition(
  options?: GeolocationOptions
): Promise<number> {
  return GeolocationModule.watchPosition(options);
}

export function clearWatch(watchId: number): void {
  GeolocationModule.clearWatch(watchId);
}

export async function requestLocationPermission(): Promise<string> {
  return GeolocationModule.requestAuthorization();
}

export function stopLocationUpdates(): void {
  GeolocationModule.stopObserving();
}

// Default export for convenience
export default {
  getCurrentPosition,
  watchPosition,
  clearWatch,
  requestLocationPermission,
  stopLocationUpdates,
};
