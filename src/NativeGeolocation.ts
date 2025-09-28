import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export interface GeolocationOptions {
  timeout?: number;
  maximumAge?: number;
  enableHighAccuracy?: boolean;
  distanceFilter?: number;
  useSignificantChanges?: boolean;
}

export interface GeolocationPosition {
  coords: {
    latitude: number;
    longitude: number;
    altitude?: number;
    accuracy: number;
    altitudeAccuracy?: number;
    heading?: number;
    speed?: number;
  };
  timestamp: number;
}

export interface GeolocationError {
  code: number;
  message: string;
}

export interface Spec extends TurboModule {
  // Geolocation methods
  getCurrentPosition(
    options?: GeolocationOptions
  ): Promise<GeolocationPosition>;

  watchPosition(options?: GeolocationOptions): Promise<number>; // Returns watch ID

  clearWatch(watchId: number): void;

  requestAuthorization(): Promise<string>;

  stopObserving(): void;
}

export default TurboModuleRegistry.getEnforcing<Spec>('Geolocation');
