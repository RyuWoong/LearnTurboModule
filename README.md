# react-native-geolocation

⚡️ React Native Geolocation library using TurboModule (New Architecture)

고성능 위치 정보 라이브러리로 iOS와 Android에서 완벽하게 동작합니다.

## Features

- ⚡️ **TurboModule** - React Native New Architecture 지원
- 📍 **현재 위치** - 한 번의 호출로 현재 위치 가져오기
- 👁️ **실시간 추적** - 위치 변화를 실시간으로 감지
- 🎯 **정확도 제어** - High accuracy, distance filter 등 세밀한 제어
- 🔐 **권한 관리** - 쉬운 권한 요청 및 상태 확인
- 📱 **Cross Platform** - iOS & Android 완벽 지원
- 🎉 **TypeScript** - 완전한 타입 지원
- 🚀 **Event Emitter** - 실시간 이벤트 기반 업데이트

## Installation

```sh
npm install react-native-geolocation
# or
yarn add react-native-geolocation
```

### iOS

```sh
cd ios && pod install
```

`Info.plist`에 권한 설명 추가:

```xml
<key>NSLocationWhenInUseUsageDescription</key>
<string>앱이 위치 정보를 사용하기 위해 권한이 필요합니다.</string>
<key>NSLocationAlwaysAndWhenInUseUsageDescription</key>
<string>백그라운드에서도 위치 정보를 사용하기 위해 권한이 필요합니다.</string>
```

### Android

`AndroidManifest.xml`에 권한 추가 (자동으로 포함됨):

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

## Usage

### 기본 사용법

```typescript
import {
  requestAuthorization,
  getCurrentPosition,
  watchPosition,
  clearWatch,
  addLocationListener,
  type GeolocationPosition,
} from 'react-native-geolocation';

// 1. 권한 요청
const status = await requestAuthorization();
console.log('권한 상태:', status); // 'granted', 'denied', etc.

// 2. 현재 위치 가져오기
const position = await getCurrentPosition({
  enableHighAccuracy: true,
  timeout: 15000,
  maximumAge: 10000,
});

console.log('위도:', position.coords.latitude);
console.log('경도:', position.coords.longitude);
console.log('정확도:', position.coords.accuracy);
```

### 실시간 위치 추적

```typescript
import { useEffect, useState } from 'react';
import {
  watchPosition,
  clearWatch,
  addLocationListener,
  type GeolocationPosition,
} from 'react-native-geolocation';

function App() {
  const [position, setPosition] = useState<GeolocationPosition | null>(null);
  const [watchId, setWatchId] = useState<number | null>(null);

  useEffect(() => {
    // 위치 추적 시작
    const startWatching = async () => {
      const id = await watchPosition({
        enableHighAccuracy: true,
        distanceFilter: 10, // 10m 이동마다 업데이트
      });
      setWatchId(id);
    };

    // 이벤트 리스너 등록
    const subscription = addLocationListener((newPosition) => {
      console.log('위치 업데이트:', newPosition);
      setPosition(newPosition);
    });

    startWatching();

    // 클린업
    return () => {
      if (watchId !== null) {
        clearWatch(watchId);
      }
      subscription.remove();
    };
  }, []);

  return (
    <View>
      <Text>위도: {position?.coords.latitude}</Text>
      <Text>경도: {position?.coords.longitude}</Text>
    </View>
  );
}
```

## API Reference

### Methods

#### `requestAuthorization()`

위치 권한을 요청합니다.

```typescript
requestAuthorization(): Promise<string>
```

**Returns:**

- `'granted'` - 권한 허용됨 (Android)
- `'authorizedWhenInUse'` - 앱 사용 중 권한 허용 (iOS)
- `'authorizedAlways'` - 항상 권한 허용 (iOS)
- `'denied'` - 권한 거부됨
- `'notDetermined'` - 아직 권한 요청 안 함 (iOS)

**Example:**

```typescript
const status = await requestAuthorization();
if (status === 'granted' || status === 'authorizedWhenInUse') {
  // 위치 정보 사용 가능
}
```

---

#### `getCurrentPosition(options?)`

현재 위치를 한 번 가져옵니다.

```typescript
getCurrentPosition(options?: GeolocationOptions): Promise<GeolocationPosition>
```

**Parameters:**

| Name    | Type                 | Description          |
| ------- | -------------------- | -------------------- |
| options | `GeolocationOptions` | 위치 옵션 (optional) |

**Returns:** `Promise<GeolocationPosition>`

**Example:**

```typescript
const position = await getCurrentPosition({
  enableHighAccuracy: true,
  timeout: 15000,
  maximumAge: 10000,
});

console.log(position.coords.latitude);
console.log(position.coords.longitude);
```

---

#### `watchPosition(options?)`

위치 변화를 실시간으로 추적합니다.

```typescript
watchPosition(options?: GeolocationOptions): Promise<number>
```

**Parameters:**

| Name    | Type                 | Description          |
| ------- | -------------------- | -------------------- |
| options | `GeolocationOptions` | 위치 옵션 (optional) |

**Returns:** `Promise<number>` - Watch ID (clearWatch에 사용)

**Example:**

```typescript
const watchId = await watchPosition({
  enableHighAccuracy: true,
  distanceFilter: 10, // 10m 이동마다 업데이트
});

// 나중에 중지할 때
clearWatch(watchId);
```

---

#### `clearWatch(watchId)`

특정 위치 추적을 중지합니다.

```typescript
clearWatch(watchId: number): void
```

**Parameters:**

| Name    | Type     | Description                 |
| ------- | -------- | --------------------------- |
| watchId | `number` | watchPosition에서 반환된 ID |

**Example:**

```typescript
const watchId = await watchPosition();
// ...
clearWatch(watchId);
```

---

#### `stopObserving()`

모든 위치 추적을 중지합니다.

```typescript
stopObserving(): void
```

**Example:**

```typescript
stopObserving();
```

---

#### `addLocationListener(callback)`

위치 변경 이벤트 리스너를 추가합니다.

```typescript
addLocationListener(
  callback: (position: GeolocationPosition) => void
): EmitterSubscription
```

**Parameters:**

| Name     | Type                                      | Description              |
| -------- | ----------------------------------------- | ------------------------ |
| callback | `(position: GeolocationPosition) => void` | 위치 변경 시 호출될 함수 |

**Returns:** `EmitterSubscription` - 리스너를 제거할 수 있는 subscription

**Example:**

```typescript
const subscription = addLocationListener((position) => {
  console.log('위치 업데이트:', position.coords);
});

// 나중에 제거
subscription.remove();
```

---

#### `addErrorListener(callback)`

위치 에러 이벤트 리스너를 추가합니다.

```typescript
addErrorListener(
  callback: (error: GeolocationError) => void
): EmitterSubscription
```

**Parameters:**

| Name     | Type                                | Description              |
| -------- | ----------------------------------- | ------------------------ |
| callback | `(error: GeolocationError) => void` | 에러 발생 시 호출될 함수 |

**Returns:** `EmitterSubscription`

**Example:**

```typescript
const subscription = addErrorListener((error) => {
  console.error('위치 에러:', error.message);
});
```

---

#### `removeAllListeners()`

모든 이벤트 리스너를 제거합니다.

```typescript
removeAllListeners(): void
```

**Example:**

```typescript
removeAllListeners();
```

---

### Types

#### `GeolocationOptions`

```typescript
interface GeolocationOptions {
  timeout?: number; // 타임아웃 (ms)
  maximumAge?: number; // 캐시된 위치의 최대 수명 (ms)
  enableHighAccuracy?: boolean; // 고정밀도 모드 사용
  distanceFilter?: number; // 최소 이동 거리 (m)
  useSignificantChanges?: boolean; // 중요한 변화만 감지 (iOS)
}
```

#### `GeolocationPosition`

```typescript
interface GeolocationPosition {
  coords: {
    latitude: number; // 위도
    longitude: number; // 경도
    altitude?: number; // 고도 (m)
    accuracy: number; // 수평 정확도 (m)
    altitudeAccuracy?: number; // 수직 정확도 (m)
    heading?: number; // 방향 (도, 0-360)
    speed?: number; // 속도 (m/s)
  };
  timestamp: number; // 타임스탬프 (ms)
}
```

#### `GeolocationError`

```typescript
interface GeolocationError {
  code: number; // 에러 코드
  message: string; // 에러 메시지
}
```

## Examples

### 1. 권한 체크 후 위치 가져오기

```typescript
async function getLocation() {
  try {
    const status = await requestAuthorization();

    if (status === 'denied') {
      Alert.alert('권한 거부', '위치 권한이 필요합니다.');
      return;
    }

    const position = await getCurrentPosition({
      enableHighAccuracy: true,
      timeout: 15000,
    });

    console.log('현재 위치:', position.coords);
  } catch (error) {
    console.error('위치 가져오기 실패:', error);
  }
}
```

### 2. 실시간 위치 추적 (React Hook)

```typescript
import { useEffect, useState } from 'react';
import { watchPosition, clearWatch, addLocationListener } from 'react-native-geolocation';
import type { GeolocationPosition } from 'react-native-geolocation';

function useGeolocation() {
  const [position, setPosition] = useState<GeolocationPosition | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let watchId: number | null = null;

    const startTracking = async () => {
      try {
        watchId = await watchPosition({
          enableHighAccuracy: true,
          distanceFilter: 10,
        });

        const subscription = addLocationListener((newPosition) => {
          setPosition(newPosition);
          setError(null);
        });

        return () => {
          subscription.remove();
        };
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Unknown error');
      }
    };

    startTracking();

    return () => {
      if (watchId !== null) {
        clearWatch(watchId);
      }
    };
  }, []);

  return { position, error };
}

// 사용
function App() {
  const { position, error } = useGeolocation();

  if (error) {
    return <Text>Error: {error}</Text>;
  }

  return (
    <View>
      <Text>위도: {position?.coords.latitude}</Text>
      <Text>경도: {position?.coords.longitude}</Text>
      <Text>속도: {position?.coords.speed ? `${(position.coords.speed * 3.6).toFixed(2)} km/h` : 'N/A'}</Text>
    </View>
  );
}
```

### 3. 거리 계산

```typescript
import { getCurrentPosition } from 'react-native-geolocation';

// Haversine 공식으로 두 지점 간 거리 계산
function calculateDistance(
  lat1: number,
  lon1: number,
  lat2: number,
  lon2: number
): number {
  const R = 6371; // 지구 반지름 (km)
  const dLat = (lat2 - lat1) * (Math.PI / 180);
  const dLon = (lon2 - lon1) * (Math.PI / 180);
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(lat1 * (Math.PI / 180)) *
      Math.cos(lat2 * (Math.PI / 180)) *
      Math.sin(dLon / 2) *
      Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
}

async function getDistanceFromTarget() {
  const targetLat = 37.5665;
  const targetLon = 126.978;

  const position = await getCurrentPosition();
  const distance = calculateDistance(
    position.coords.latitude,
    position.coords.longitude,
    targetLat,
    targetLon
  );

  console.log(`목표 지점까지 거리: ${distance.toFixed(2)} km`);
}
```

## Platform-Specific Notes

### iOS

- **CLLocationManager** 사용
- `Info.plist`에 권한 설명 필수
- 시뮬레이터에서 테스트 시: `Debug > Simulate Location` 사용

### Android

- **FusedLocationProviderClient** 사용 (Google Play Services)
- 자동으로 최적의 위치 제공자 선택 (GPS, WiFi, Cell)
- 에뮬레이터에서 테스트 시: Extended Controls > Location 사용

## Performance Tips

1. **High Accuracy는 필요할 때만 사용**

   ```typescript
   // 배터리 절약
   getCurrentPosition({ enableHighAccuracy: false });
   ```

2. **distanceFilter 활용**

   ```typescript
   // 100m 이동할 때만 업데이트
   watchPosition({ distanceFilter: 100 });
   ```

3. **사용하지 않을 때는 추적 중지**
   ```typescript
   useEffect(() => {
     // 컴포넌트 언마운트 시 자동 중지
     return () => stopObserving();
   }, []);
   ```

## Troubleshooting

### iOS에서 권한 요청이 안 나타나요

`Info.plist`에 `NSLocationWhenInUseUsageDescription`이 있는지 확인하세요.

### Android에서 위치가 업데이트되지 않아요

1. Google Play Services가 설치되어 있는지 확인
2. 기기의 위치 서비스가 켜져 있는지 확인
3. 에뮬레이터에서는 Extended Controls로 위치 설정

### watchPosition이 작동하지 않아요

1. `requestAuthorization()`을 먼저 호출했는지 확인
2. `addLocationListener()`로 이벤트 리스너를 등록했는지 확인
3. 기기를 실제로 이동시키거나 시뮬레이션하고 있는지 확인

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)
