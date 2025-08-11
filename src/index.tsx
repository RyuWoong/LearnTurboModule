import Geolocation from './NativeGeolocation';

export function getCurrentPosition(): Promise<any> {
  return Geolocation.getCurrentPosition();
}
