import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export type GeolocationConfiguration = {
  locationProvider?: 'playServices' | 'android' | 'auto';
  enableBackgroundLocationUpdates?: boolean;
};

export type GeolocationOptions = {
  timeout?: number;
  maximumAge?: number;
  enableHighAccuracy?: boolean;
  distanceFilter?: number;
  useSignificantChanges?: boolean;
  interval?: number;
  fastestInterval?: number;
};

export type GeloocationData = {
  coords: {
    latitude: number;
    longitude: number;
    altitude: number | null;
    accuracy: number;
    altitudeAccuracy: number | null;
    heading: number | null;
    speed: number | null;
  };
  timestamp: number;
};

export type GeolocationError = {
  code: number;
  message: string;
};

export interface Spec extends TurboModule {
  setConfiguration(config: GeolocationConfiguration): void;

  requestAuthorization(): Promise<string>;

  getCurrentPosition(options: GeolocationOptions): Promise<GeloocationData>;

  startObserving(options: GeolocationOptions): void;

  stopObserving(): void;
}

export default TurboModuleRegistry.getEnforcing<Spec>('Geolocation');
