import Geolocation, { type GeloocationData } from './specs/NativeGeolocation';

export function getCurrentPosition(): Promise<GeloocationData> {
  return Geolocation.getCurrentPosition();
}
