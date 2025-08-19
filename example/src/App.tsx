import { useEffect } from 'react';
import { View, StyleSheet } from 'react-native';
import { getCurrentPosition } from 'react-native-geolocation';

export default function App() {
  useEffect(() => {
    getCurrentPosition().then((position) => {
      console.log(position);
    });
  }, []);

  return <View style={styles.container} />;
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
