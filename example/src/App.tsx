import { useState } from 'react';
import { Text, View, StyleSheet, Button, Alert } from 'react-native';
import {
  getCurrentPosition,
  watchPosition,
  clearWatch,
  requestLocationPermission,
  stopLocationUpdates,
  type GeolocationPosition,
} from 'react-native-geolocation';

export default function App() {
  const [location, setLocation] = useState<GeolocationPosition | null>(null);
  const [watchId, setWatchId] = useState<number | null>(null);
  const [isWatching, setIsWatching] = useState(false);

  const handleRequestPermission = async () => {
    try {
      const result = await requestLocationPermission();
      Alert.alert('Permission Result', result);
    } catch (error: any) {
      Alert.alert('Permission Error', error.message);
    }
  };

  const handleGetCurrentPosition = async () => {
    try {
      const position = await getCurrentPosition({
        enableHighAccuracy: true,
        timeout: 15000,
        maximumAge: 10000,
      });
      setLocation(position);
    } catch (error: any) {
      Alert.alert('Location Error', error.message);
    }
  };

  const handleStartWatching = async () => {
    try {
      const id = await watchPosition({
        enableHighAccuracy: true,
        distanceFilter: 10,
      });
      setWatchId(id);
      setIsWatching(true);
    } catch (error: any) {
      Alert.alert('Watch Error', error.message);
    }
  };

  const handleStopWatching = () => {
    if (watchId !== null) {
      clearWatch(watchId);
      setWatchId(null);
      setIsWatching(false);
    }
    stopLocationUpdates();
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>React Native Geolocation</Text>

      <View style={styles.buttonContainer}>
        <Button title="Request Permission" onPress={handleRequestPermission} />
      </View>

      <View style={styles.buttonContainer}>
        <Button
          title="Get Current Position"
          onPress={handleGetCurrentPosition}
        />
      </View>

      <View style={styles.buttonContainer}>
        <Button
          title={isWatching ? 'Stop Watching' : 'Start Watching'}
          onPress={isWatching ? handleStopWatching : handleStartWatching}
        />
      </View>

      {location && (
        <View style={styles.locationContainer}>
          <Text style={styles.locationTitle}>Location:</Text>
          <Text>Latitude: {location.coords.latitude.toFixed(6)}</Text>
          <Text>Longitude: {location.coords.longitude.toFixed(6)}</Text>
          <Text>Accuracy: {location.coords.accuracy.toFixed(2)}m</Text>
          <Text>
            Timestamp: {new Date(location.timestamp).toLocaleTimeString()}
          </Text>
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 20,
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    marginBottom: 20,
  },
  subtitle: {
    fontSize: 16,
    marginBottom: 20,
  },
  buttonContainer: {
    marginVertical: 10,
    width: '80%',
  },
  locationContainer: {
    marginTop: 20,
    padding: 15,
    backgroundColor: '#f0f0f0',
    borderRadius: 8,
    width: '90%',
  },
  locationTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    marginBottom: 10,
  },
});
