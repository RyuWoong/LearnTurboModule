import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export type GeolocationConfiguration = {
  authorizationLevel?: 'always' | 'whenInUse' | 'auto';
  locationProvider?: 'playServices' | 'android' | 'auto';
  enableBackgroundLocationUpdates?: boolean;
};

export interface Spec extends TurboModule {
  setConfiguration(config: GeolocationConfiguration): void;

  requestAuthorization(): void;

  getCurrentPosition(): Promise<any>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('Geolocation');
