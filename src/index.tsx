import Geolocation, {
  type GeloocationData,
  type GeolocationOptions,
} from './specs/NativeGeolocation';

export function getCurrentPosition(
  options: GeolocationOptions
): Promise<GeloocationData> {
  return Geolocation.getCurrentPosition(options);
}
