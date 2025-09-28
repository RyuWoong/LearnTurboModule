import Foundation
import CoreLocation
import React

@objc(GeolocationModule)
class GeolocationModule: RCTEventEmitter, CLLocationManagerDelegate {
    
    private var locationManager: CLLocationManager
    private var watchCallbacks: [Int: (CLLocation?, Error?) -> Void] = [:]
    private var currentWatchId: Int = 0
    private var isObserving: Bool = false
    
    override init() {
        self.locationManager = CLLocationManager()
        super.init()
        self.locationManager.delegate = self
    }
    
    @objc
    override func supportedEvents() -> [String]! {
        return ["geolocationDidChange", "geolocationError"]
    }
    
    @objc
    override func startObserving() {
        isObserving = true
    }
    
    @objc
    override func stopObserving() {
        isObserving = false
        locationManager.stopUpdatingLocation()
        locationManager.stopMonitoringSignificantLocationChanges()
    }
    
    override class func requiresMainQueueSetup() -> Bool {
        return true
    }
}

// MARK: - TurboModule Implementation
extension GeolocationModule {
    
    @objc
    func getCurrentPosition(_ options: NSDictionary?, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
        let optionsDict = options as? [String: Any]
        self.requestLocationOnce(options: optionsDict, resolve: resolve, reject: reject)
    }
    
    @objc
    func watchPosition(_ options: NSDictionary?, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
        let optionsDict = options as? [String: Any]
        let watchId = self.addLocationWatch(options: optionsDict, resolve: resolve, reject: reject)
        resolve(NSNumber(value: watchId))
    }
    
    @objc
    func clearWatch(_ watchId: Double) {
        let id = Int(watchId)
        watchCallbacks.removeValue(forKey: id)
        
        if watchCallbacks.isEmpty {
            locationManager.stopUpdatingLocation()
            locationManager.stopMonitoringSignificantLocationChanges()
        }
    }
    
    @objc
    func requestAuthorization(_ resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
        let status = locationManager.authorizationStatus
        
        switch status {
        case .notDetermined:
            // Request authorization and wait for delegate callback
            self.authorizationResolve = resolve
            self.authorizationReject = reject
            locationManager.requestWhenInUseAuthorization()
            
        case .denied, .restricted:
            reject("PERMISSION_DENIED", "Location permission denied", nil)
            
        case .authorizedWhenInUse, .authorizedAlways:
            resolve("granted")
            
        @unknown default:
            reject("UNKNOWN_ERROR", "Unknown authorization status", nil)
        }
    }
}

// MARK: - Private Implementation
private extension GeolocationModule {
    
    private var authorizationResolve: RCTPromiseResolveBlock? {
        get { return objc_getAssociatedObject(self, &AssociatedKeys.authResolve) as? RCTPromiseResolveBlock }
        set { objc_setAssociatedObject(self, &AssociatedKeys.authResolve, newValue, .OBJC_ASSOCIATION_RETAIN_NONATOMIC) }
    }
    
    private var authorizationReject: RCTPromiseRejectBlock? {
        get { return objc_getAssociatedObject(self, &AssociatedKeys.authReject) as? RCTPromiseRejectBlock }
        set { objc_setAssociatedObject(self, &AssociatedKeys.authReject, newValue, .OBJC_ASSOCIATION_RETAIN_NONATOMIC) }
    }
    
    func requestLocationOnce(options: [String: Any]?, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
        guard hasLocationPermission() else {
            reject("PERMISSION_DENIED", "Location permission not granted", nil)
            return
        }
        
        configureLocationManager(with: options)
        
        let timeout = (options?["timeout"] as? Double) ?? 60.0
        let maximumAge = (options?["maximumAge"] as? Double) ?? 0.0
        
        // Check if we have a recent location
        if let lastLocation = locationManager.location,
           maximumAge > 0,
           Date().timeIntervalSince(lastLocation.timestamp) <= maximumAge / 1000.0 {
            resolve(locationToDict(lastLocation))
            return
        }
        
        // Request new location
        let timeoutTimer = DispatchSource.makeTimerSource(queue: DispatchQueue.main)
        timeoutTimer.schedule(deadline: .now() + timeout / 1000.0)
        
        var didResolve = false
        
        timeoutTimer.setEventHandler { [weak self] in
            guard !didResolve else { return }
            didResolve = true
            timeoutTimer.cancel()
            
            if let lastLocation = self?.locationManager.location {
                resolve(self?.locationToDict(lastLocation) ?? [:])
            } else {
                reject("TIMEOUT", "Location request timed out", nil)
            }
        }
        
        // Store callback for location updates
        let tempWatchId = currentWatchId
        currentWatchId += 1
        
        watchCallbacks[tempWatchId] = { location, error in
            guard !didResolve else { return }
            didResolve = true
            timeoutTimer.cancel()
            self.watchCallbacks.removeValue(forKey: tempWatchId)
            
            if let error = error {
                reject("LOCATION_ERROR", error.localizedDescription, error)
            } else if let location = location {
                resolve(self.locationToDict(location))
            } else {
                reject("UNKNOWN_ERROR", "Unknown location error", nil)
            }
        }
        
        timeoutTimer.resume()
        startLocationUpdates(with: options)
    }
    
    func addLocationWatch(options: [String: Any]?, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) -> Int {
        guard hasLocationPermission() else {
            reject("PERMISSION_DENIED", "Location permission not granted", nil)
            return -1
        }
        
        let watchId = currentWatchId
        currentWatchId += 1
        
        watchCallbacks[watchId] = { [weak self] location, error in
            guard let self = self, self.isObserving else { return }
            
            if let error = error {
                self.sendEvent(withName: "geolocationError", body: [
                    "watchId": watchId,
                    "error": [
                        "code": (error as NSError).code,
                        "message": error.localizedDescription
                    ]
                ])
            } else if let location = location {
                self.sendEvent(withName: "geolocationDidChange", body: [
                    "watchId": watchId,
                    "position": self.locationToDict(location)
                ])
            }
        }
        
        configureLocationManager(with: options)
        startLocationUpdates(with: options)
        
        return watchId
    }
    
    func hasLocationPermission() -> Bool {
        let status = locationManager.authorizationStatus
        return status == .authorizedWhenInUse || status == .authorizedAlways
    }
    
    func configureLocationManager(with options: [String: Any]?) {
        if let enableHighAccuracy = options?["enableHighAccuracy"] as? Bool, enableHighAccuracy {
            locationManager.desiredAccuracy = kCLLocationAccuracyBest
        } else {
            locationManager.desiredAccuracy = kCLLocationAccuracyHundredMeters
        }
        
        if let distanceFilter = options?["distanceFilter"] as? Double {
            locationManager.distanceFilter = distanceFilter
        } else {
            locationManager.distanceFilter = kCLLocationAccuracyHundredMeters
        }
    }
    
    func startLocationUpdates(with options: [String: Any]?) {
        let useSignificantChanges = options?["useSignificantChanges"] as? Bool ?? false
        
        if useSignificantChanges {
            locationManager.startMonitoringSignificantLocationChanges()
        } else {
            locationManager.startUpdatingLocation()
        }
    }
    
    func locationToDict(_ location: CLLocation) -> [String: Any] {
        var coords: [String: Any] = [
            "latitude": location.coordinate.latitude,
            "longitude": location.coordinate.longitude,
            "accuracy": location.horizontalAccuracy
        ]
        
        if location.altitude != 0 {
            coords["altitude"] = location.altitude
        }
        
        if location.verticalAccuracy >= 0 {
            coords["altitudeAccuracy"] = location.verticalAccuracy
        }
        
        if location.course >= 0 {
            coords["heading"] = location.course
        }
        
        if location.speed >= 0 {
            coords["speed"] = location.speed
        }
        
        return [
            "coords": coords,
            "timestamp": location.timestamp.timeIntervalSince1970 * 1000
        ]
    }
}

// MARK: - CLLocationManagerDelegate
extension GeolocationModule {
    
    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let location = locations.last else { return }
        
        // Notify all active watches
        for (_, callback) in watchCallbacks {
            callback(location, nil)
        }
    }
    
    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        // Notify all active watches
        for (_, callback) in watchCallbacks {
            callback(nil, error)
        }
    }
    
    func locationManager(_ manager: CLLocationManager, didChangeAuthorization status: CLAuthorizationStatus) {
        switch status {
        case .authorizedWhenInUse, .authorizedAlways:
            authorizationResolve?("granted")
            
        case .denied, .restricted:
            authorizationReject?("PERMISSION_DENIED", "Location permission denied", nil)
            
        case .notDetermined:
            // Still waiting for user response
            break
            
        @unknown default:
            authorizationReject?("UNKNOWN_ERROR", "Unknown authorization status", nil)
        }
        
        // Clear authorization callbacks
        authorizationResolve = nil
        authorizationReject = nil
    }
}

// MARK: - Associated Keys for Promise callbacks
private struct AssociatedKeys {
    static var authResolve = "authResolve"
    static var authReject = "authReject"
}
