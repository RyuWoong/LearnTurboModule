enum GeolocationError: Int {
    case denied = 1
    case unavailable
    case timeout
}


func geolocationError(code: GeolocationError, message: String) -> NSError {
    return NSError(domain: "Geolocation", code: code.rawValue, userInfo: [NSLocalizedDescriptionKey: message])
}
