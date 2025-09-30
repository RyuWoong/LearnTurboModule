import Foundation
import CoreLocation

@objc(GeolocationManager)
public class GeolocationManager: NSObject {
    
    private let locationManager = CLLocationManager()
    private var watchCallbacks: [Int: (CLLocation) -> Void] = [:]
    private var currentWatchId: Int = 0
    
    private var authorizationResolve: ((String) -> Void)?
    private var authorizationReject: ((String, String, Error?) -> Void)?
    
    private var getCurrentPositionResolve: (([String: Any]) -> Void)?
    private var getCurrentPositionReject: ((String, String, Error?) -> Void)?
    private var getCurrentPositionOptions: [String: Any]?
    
    // Event emitter callback
    private var sendEventCallback: ((_ eventName: String, _ params: [String: Any]) -> Void)?
    
    public override init() {
        super.init()
        locationManager.delegate = self
    }
    
    // MARK: - Public Methods
    
    @objc
    public func setSendEventCallback(_ callback: @escaping (_ eventName: String, _ params: [String: Any]) -> Void) {
        self.sendEventCallback = callback
    }
    
    @objc
    public func requestAuthorization(
        resolve: @escaping (String) -> Void,
        reject: @escaping (String, String, Error?) -> Void
    ) {
        let status = locationManager.authorizationStatus
        
        switch status {
        case .notDetermined:
            // 권한이 아직 결정되지 않은 경우에만 콜백 저장
            self.authorizationResolve = resolve
            self.authorizationReject = reject
            locationManager.requestWhenInUseAuthorization()
        case .authorizedWhenInUse, .authorizedAlways, .denied, .restricted:
            // 이미 결정된 경우 바로 resolve (delegate 메서드 호출 안 됨)
            resolve(authorizationStatusToString(status))
        @unknown default:
            resolve("unknown")
        }
    }
    
    @objc
    public func getCurrentPosition(
        options: [String: Any]?,
        resolve: @escaping ([String: Any]) -> Void,
        reject: @escaping (String, String, Error?) -> Void
    ) {
        self.getCurrentPositionResolve = resolve
        self.getCurrentPositionReject = reject
        self.getCurrentPositionOptions = options
        
        // 권한 체크
        let status = locationManager.authorizationStatus
        guard status == .authorizedWhenInUse || status == .authorizedAlways else {
            reject("PERMISSION_DENIED", "Location permission not granted", nil)
            return
        }
        
        // 옵션 설정
        configureLocationManager(with: options)
        
        // 위치 요청
        locationManager.requestLocation()
    }
    
    @objc
    public func watchPosition(
        options: [String: Any]?,
        resolve: @escaping (NSNumber) -> Void,
        reject: @escaping (String, String, Error?) -> Void
    ) {
        print("👁️ [GeolocationManager] watchPosition 시작")
        
        // 권한 체크
        let status = locationManager.authorizationStatus
        guard status == .authorizedWhenInUse || status == .authorizedAlways else {
            print("   ❌ 권한 없음: \(status)")
            reject("PERMISSION_DENIED", "Location permission not granted", nil)
            return
        }
        
        print("   ✅ 권한 확인됨")
        
        // 옵션 설정
        configureLocationManager(with: options)
        
        // Watch ID 생성
        currentWatchId += 1
        let watchId = currentWatchId
        
        print("   📝 Watch ID: \(watchId)")
        
        // 콜백 저장 - 이벤트 전송
        watchCallbacks[watchId] = { [weak self] location in
            guard let self = self else { return }
            print("   📡 이벤트 전송: onLocationChanged")
            let locationDict = self.locationToDict(location)
            self.sendEventCallback?("onLocationChanged", locationDict)
        }
        
        // 위치 업데이트 시작
        locationManager.startUpdatingLocation()
        print("   ▶️ CLLocationManager.startUpdatingLocation() 호출됨")
        
        resolve(NSNumber(value: watchId))
    }
    
    @objc
    public func clearWatch(watchId: NSNumber) {
        let id = watchId.intValue
        watchCallbacks.removeValue(forKey: id)
        
        // 모든 watch가 제거되면 위치 업데이트 중지
        if watchCallbacks.isEmpty {
            locationManager.stopUpdatingLocation()
        }
    }
    
    @objc
    public func stopObserving() {
        locationManager.stopUpdatingLocation()
        watchCallbacks.removeAll()
    }
    
    // MARK: - Private Methods
    
    private func configureLocationManager(with options: [String: Any]?) {
        // 기본값 설정
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        locationManager.distanceFilter = kCLDistanceFilterNone  // 모든 업데이트 받기
        
        guard let options = options else {
            print("   ℹ️ 옵션 없음, 기본값 사용")
            return
        }
        
        // enableHighAccuracy
        if let enableHighAccuracy = options["enableHighAccuracy"] as? Bool {
            locationManager.desiredAccuracy = enableHighAccuracy 
                ? kCLLocationAccuracyBest 
                : kCLLocationAccuracyHundredMeters
            print("   🎯 desiredAccuracy: \(enableHighAccuracy ? "Best" : "100m")")
        }
        
        // distanceFilter
        if let distanceFilter = options["distanceFilter"] as? Double {
            locationManager.distanceFilter = distanceFilter
            print("   📏 distanceFilter: \(distanceFilter)m")
        } else {
            print("   📏 distanceFilter: None (모든 업데이트)")
        }
        
        // 추가 설정
        locationManager.pausesLocationUpdatesAutomatically = false
        print("   ⏸️ pausesLocationUpdatesAutomatically: false")
    }
    
    private func authorizationStatusToString(_ status: CLAuthorizationStatus) -> String {
        switch status {
        case .notDetermined:
            return "notDetermined"
        case .restricted:
            return "restricted"
        case .denied:
            return "denied"
        case .authorizedAlways:
            return "authorizedAlways"
        case .authorizedWhenInUse:
            return "authorizedWhenInUse"
        @unknown default:
            return "unknown"
        }
    }
    
    private func locationToDict(_ location: CLLocation) -> [String: Any] {
        return [
            "coords": [
                "latitude": location.coordinate.latitude,
                "longitude": location.coordinate.longitude,
                "altitude": location.altitude,
                "accuracy": location.horizontalAccuracy,
                "altitudeAccuracy": location.verticalAccuracy,
                "heading": location.course,
                "speed": location.speed
            ],
            "timestamp": location.timestamp.timeIntervalSince1970 * 1000 // milliseconds
        ]
    }
}

// MARK: - CLLocationManagerDelegate

extension GeolocationManager: CLLocationManagerDelegate {
    
    public func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let location = locations.last else { return }
        
        print("🗺️ [GeolocationManager] 위치 업데이트!")
        print("   📍 위도: \(location.coordinate.latitude)")
        print("   📍 경도: \(location.coordinate.longitude)")
        print("   ⏰ 시간: \(Date())")
        
        // getCurrentPosition 콜백 처리
        if let resolve = getCurrentPositionResolve {
            print("   ✅ getCurrentPosition resolve 호출")
            let locationDict = locationToDict(location)
            resolve(locationDict)
            getCurrentPositionResolve = nil
            getCurrentPositionReject = nil
        }
        
        // watchPosition 콜백 처리
        print("   🔍 watchCallbacks.count: \(watchCallbacks.count)")
        print("   🔍 watchCallbacks.keys: \(Array(watchCallbacks.keys))")
        
        if !watchCallbacks.isEmpty {
            print("   ✅ watchPosition 콜백 \(watchCallbacks.count)개 실행")
            for (watchId, callback) in watchCallbacks {
                print("   📡 Watch ID \(watchId) 콜백 실행")
                callback(location)
            }
        } else {
            print("   ⚠️ watchCallbacks가 비어있음!")
        }
    }
    
    public func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        // getCurrentPosition 에러 처리
        if let reject = getCurrentPositionReject {
            reject("LOCATION_ERROR", error.localizedDescription, error)
            getCurrentPositionResolve = nil
            getCurrentPositionReject = nil
        }
        
        // watchPosition 에러 이벤트 전송
        if !watchCallbacks.isEmpty {
            let errorDict: [String: Any] = [
                "code": (error as NSError).code,
                "message": error.localizedDescription
            ]
            sendEventCallback?("onLocationError", errorDict)
        }
    }
    
    public func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        let status = manager.authorizationStatus
        
        print("🔐 [GeolocationManager] 권한 상태 변경: \(authorizationStatusToString(status))")
        
        // authorizationResolve가 있을 때만 (requestAuthorization이 호출되었을 때만) resolve
        if let resolve = authorizationResolve {
            print("   ✅ resolve 호출")
            resolve(authorizationStatusToString(status))
            authorizationResolve = nil
            authorizationReject = nil
        } else {
            print("   ℹ️ resolve 없음 (이미 처리됨)")
        }
    }
}
