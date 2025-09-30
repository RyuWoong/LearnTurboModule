import { useState, useEffect } from 'react';
import {
  Text,
  View,
  StyleSheet,
  Button,
  ScrollView,
  Alert,
  SafeAreaView,
} from 'react-native';
import type { EmitterSubscription } from 'react-native';
import {
  requestAuthorization,
  getCurrentPosition,
  watchPosition,
  clearWatch,
  stopObserving,
  addLocationListener,
  addErrorListener,
  type GeolocationPosition,
  type GeolocationError,
} from 'react-native-geolocation';

export default function App() {
  const [authStatus, setAuthStatus] = useState<string>('');
  const [position, setPosition] = useState<GeolocationPosition | null>(null);
  const [watchId, setWatchId] = useState<number | null>(null);
  const [isWatching, setIsWatching] = useState(false);
  const [error, setError] = useState<string>('');
  const [updateCount, setUpdateCount] = useState<number>(0);
  const [lastUpdateTime, setLastUpdateTime] = useState<string>('');

  // 이벤트 리스너 등록
  useEffect(() => {
    console.log('🔄 [App.tsx] useEffect 실행, isWatching:', isWatching);

    let locationSubscription: EmitterSubscription | null = null;
    let errorSubscription: EmitterSubscription | null = null;

    if (isWatching) {
      console.log('👀 [App.tsx] 이벤트 리스너 등록 시작');

      // 위치 변경 이벤트 리스너
      locationSubscription = addLocationListener((newPosition) => {
        console.log('📍 [App.tsx] 위치 업데이트:', newPosition);
        setPosition(newPosition);
        setError('');
        setUpdateCount((prev) => prev + 1);
        setLastUpdateTime(new Date().toLocaleTimeString('ko-KR'));
      });

      // 에러 이벤트 리스너
      errorSubscription = addErrorListener((err: GeolocationError) => {
        console.log('❌ [App.tsx] 에러 발생:', err);
        setError(`${err.message} (Code: ${err.code})`);
      });

      console.log('✅ [App.tsx] 이벤트 리스너 등록 완료');
    }

    // 클린업: subscription만 제거 (removeAllListeners 제거)
    return () => {
      console.log('🧹 [App.tsx] useEffect cleanup');
      if (locationSubscription) {
        console.log('   🗑️ locationSubscription.remove()');
        locationSubscription.remove();
      }
      if (errorSubscription) {
        console.log('   🗑️ errorSubscription.remove()');
        errorSubscription.remove();
      }
    };
  }, [isWatching]);

  // 권한 요청
  const handleRequestAuth = async () => {
    try {
      setError('');
      const status = await requestAuthorization();
      setAuthStatus(status);
      Alert.alert('권한 상태', `현재 권한: ${status}`);
    } catch (err) {
      const errorMessage =
        err instanceof Error ? err.message : '알 수 없는 에러';
      setError(errorMessage);
      Alert.alert('에러', errorMessage);
    }
  };

  // 현재 위치 가져오기
  const handleGetCurrentPosition = async () => {
    try {
      setError('');
      const pos = await getCurrentPosition({
        enableHighAccuracy: true,
        timeout: 15000,
        maximumAge: 10000,
      });
      setPosition(pos);
    } catch (err) {
      const errorMessage =
        err instanceof Error ? err.message : '위치를 가져올 수 없습니다';
      setError(errorMessage);
      Alert.alert('에러', errorMessage);
    }
  };

  // 위치 추적 시작
  const handleStartWatching = async () => {
    console.log('🚀 [App.tsx] handleStartWatching 시작');
    try {
      setError('');
      setUpdateCount(0);
      setLastUpdateTime('');

      console.log('📞 [App.tsx] watchPosition 호출 중...');
      const id = await watchPosition({
        enableHighAccuracy: true,
        // distanceFilter 제거 또는 0으로 설정하면 모든 위치 업데이트를 받음
        // distanceFilter: 0,  // 모든 업데이트 받기
      });

      console.log('✅ [App.tsx] watchPosition 완료, watchId:', id);
      setWatchId(id);
      setIsWatching(true);
      Alert.alert(
        '추적 시작',
        `Watch ID: ${id}\n실시간으로 위치가 업데이트됩니다!`
      );
    } catch (err) {
      console.error('❌ [App.tsx] watchPosition 에러:', err);
      const errorMessage =
        err instanceof Error ? err.message : '추적을 시작할 수 없습니다';
      setError(errorMessage);
      Alert.alert('에러', errorMessage);
    }
  };

  // 위치 추적 중지
  const handleStopWatching = () => {
    if (watchId !== null) {
      clearWatch(watchId);
      setWatchId(null);
      setIsWatching(false);
      Alert.alert('추적 중지', `총 ${updateCount}번 업데이트되었습니다`);
    }
  };

  // 모든 감시 중지
  const handleStopObserving = () => {
    stopObserving();
    setWatchId(null);
    setIsWatching(false);
    Alert.alert('완전 중지', '모든 위치 감시가 중지되었습니다');
  };

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView contentContainerStyle={styles.scrollContent}>
        <Text style={styles.title}>🗺️ Geolocation Example</Text>

        {/* 권한 상태 */}
        {authStatus && (
          <View style={styles.infoBox}>
            <Text style={styles.label}>권한 상태:</Text>
            <Text style={styles.value}>{authStatus}</Text>
          </View>
        )}

        {/* 위치 정보 */}
        {position && (
          <View style={[styles.infoBox, isWatching && styles.infoBoxActive]}>
            <Text style={styles.label}>
              📍 현재 위치 {isWatching && '(실시간 업데이트 중)'}
            </Text>
            <Text style={styles.coordinate}>
              위도: {position.coords.latitude.toFixed(6)}
            </Text>
            <Text style={styles.coordinate}>
              경도: {position.coords.longitude.toFixed(6)}
            </Text>
            <Text style={styles.coordinate}>
              정확도: {position.coords.accuracy.toFixed(2)}m
            </Text>
            {!!position.coords.altitude && (
              <Text style={styles.coordinate}>
                고도: {position.coords.altitude.toFixed(2)}m
              </Text>
            )}
            {position.coords.speed && position.coords.speed >= 0 && (
              <Text style={styles.coordinate}>
                속도: {(position.coords.speed * 3.6).toFixed(2)} km/h
              </Text>
            )}
            <Text style={styles.timestamp}>
              {new Date(position.timestamp).toLocaleString('ko-KR')}
            </Text>
          </View>
        )}

        {/* Watch 상태 */}
        {isWatching && (
          <View style={styles.watchingBox}>
            <Text style={styles.watchingText}>
              👁️ 위치 추적 중 (Watch ID: {watchId})
            </Text>
            <Text style={styles.watchingSubText}>
              📊 업데이트 횟수: {updateCount}회
            </Text>
            {lastUpdateTime && (
              <Text style={styles.watchingSubText}>
                ⏰ 마지막 업데이트: {lastUpdateTime}
              </Text>
            )}
          </View>
        )}

        {/* 에러 메시지 */}
        {!!error && (
          <View style={styles.errorBox}>
            <Text style={styles.errorText}>❌ {error}</Text>
          </View>
        )}

        {/* 버튼들 */}
        <View style={styles.buttonContainer}>
          <View style={styles.button}>
            <Button
              title="🔐 권한 요청"
              onPress={handleRequestAuth}
              color="#007AFF"
            />
          </View>

          <View style={styles.button}>
            <Button
              title="📍 현재 위치 가져오기"
              onPress={handleGetCurrentPosition}
              color="#34C759"
            />
          </View>

          <View style={styles.button}>
            {!isWatching ? (
              <Button
                title="👁️ 위치 추적 시작"
                onPress={handleStartWatching}
                color="#FF9500"
              />
            ) : (
              <Button
                title="⏸️ 위치 추적 중지"
                onPress={handleStopWatching}
                color="#FF3B30"
              />
            )}
          </View>

          <View style={styles.button}>
            <Button
              title="🛑 모든 감시 중지"
              onPress={handleStopObserving}
              color="#8E8E93"
            />
          </View>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F2F2F7',
  },
  scrollContent: {
    padding: 20,
  },
  title: {
    fontSize: 28,
    fontWeight: 'bold',
    textAlign: 'center',
    marginBottom: 24,
    color: '#000',
  },
  infoBox: {
    backgroundColor: '#fff',
    padding: 16,
    borderRadius: 12,
    marginBottom: 16,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  infoBoxActive: {
    borderWidth: 2,
    borderColor: '#34C759',
    backgroundColor: '#F0FFF4',
  },
  label: {
    fontSize: 16,
    fontWeight: '600',
    marginBottom: 8,
    color: '#000',
  },
  value: {
    fontSize: 18,
    color: '#007AFF',
    fontWeight: '500',
  },
  coordinate: {
    fontSize: 16,
    marginVertical: 4,
    color: '#333',
    fontFamily: 'Menlo',
  },
  timestamp: {
    fontSize: 12,
    color: '#8E8E93',
    marginTop: 8,
  },
  watchingBox: {
    backgroundColor: '#FF9500',
    padding: 12,
    borderRadius: 8,
    marginBottom: 16,
  },
  watchingText: {
    color: '#fff',
    fontWeight: '600',
    textAlign: 'center',
    fontSize: 14,
    marginBottom: 8,
  },
  watchingSubText: {
    color: '#fff',
    fontSize: 12,
    textAlign: 'center',
    marginVertical: 2,
    opacity: 0.9,
  },
  errorBox: {
    backgroundColor: '#FF3B30',
    padding: 12,
    borderRadius: 8,
    marginBottom: 16,
  },
  errorText: {
    color: '#fff',
    fontWeight: '600',
    textAlign: 'center',
  },
  buttonContainer: {
    marginTop: 8,
  },
  button: {
    marginVertical: 8,
    borderRadius: 8,
    overflow: 'hidden',
  },
});
