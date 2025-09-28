#import "Geolocation.h"
#import <React/RCTEventEmitter.h>
#import <React/RCTBridgeModule.h>
#import <ReactCommon/RCTTurboModule.h>
#import <Foundation/Foundation.h>

@implementation Geolocation

RCT_EXPORT_MODULE()

static id _sharedGeolocationModule = nil;

+ (id)sharedGeolocationModule {
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        Class moduleClass = NSClassFromString(@"GeolocationModule");
        if (moduleClass) {
            _sharedGeolocationModule = [[moduleClass alloc] init];
        }
    });
    return _sharedGeolocationModule;
}

// 사용할 때 타입 안전성 확보
- (void)getCurrentPosition:(JS::NativeGeolocation::GeolocationOptions &)options
                   resolve:(RCTPromiseResolveBlock)resolve
                    reject:(RCTPromiseRejectBlock)reject {
    
    // C++ 구조체를 NSDictionary로 변환
    NSMutableDictionary *optionsDict = [NSMutableDictionary dictionary];
    
    if (options.timeout().has_value()) {
        optionsDict[@"timeout"] = @(options.timeout().value());
    }
    if (options.maximumAge().has_value()) {
        optionsDict[@"maximumAge"] = @(options.maximumAge().value());
    }
    if (options.enableHighAccuracy().has_value()) {
        optionsDict[@"enableHighAccuracy"] = @(options.enableHighAccuracy().value());
    }
    if (options.distanceFilter().has_value()) {
        optionsDict[@"distanceFilter"] = @(options.distanceFilter().value());
    }
    if (options.useSignificantChanges().has_value()) {
        optionsDict[@"useSignificantChanges"] = @(options.useSignificantChanges().value());
    }
    
    // GeolocationModule 인스턴스로 메서드 호출
    id module = [Geolocation sharedGeolocationModule];
    id<GeolocationModuleProtocol> typedModule = (id<GeolocationModuleProtocol>)module;
    [typedModule getCurrentPosition:(NSDictionary *)optionsDict resolve:resolve reject:reject];
}

- (void)watchPosition:(JS::NativeGeolocation::GeolocationOptions &)options
              resolve:(RCTPromiseResolveBlock)resolve
               reject:(RCTPromiseRejectBlock)reject {
    
    // C++ 구조체를 NSDictionary로 변환
    NSMutableDictionary *optionsDict = [NSMutableDictionary dictionary];
    
    if (options.timeout().has_value()) {
        optionsDict[@"timeout"] = @(options.timeout().value());
    }
    if (options.maximumAge().has_value()) {
        optionsDict[@"maximumAge"] = @(options.maximumAge().value());
    }
    if (options.enableHighAccuracy().has_value()) {
        optionsDict[@"enableHighAccuracy"] = @(options.enableHighAccuracy().value());
    }
    if (options.distanceFilter().has_value()) {
        optionsDict[@"distanceFilter"] = @(options.distanceFilter().value());
    }
    if (options.useSignificantChanges().has_value()) {
        optionsDict[@"useSignificantChanges"] = @(options.useSignificantChanges().value());
    }
    
    // GeolocationModule 인스턴스로 메서드 호출
    id module = [Geolocation sharedGeolocationModule];
    id<GeolocationModuleProtocol> typedModule = (id<GeolocationModuleProtocol>)module;
    [typedModule watchPosition:(NSDictionary *)optionsDict resolve:resolve reject:reject];
}

- (void)clearWatch:(double)watchId {
    id module = [Geolocation sharedGeolocationModule];
    id<GeolocationModuleProtocol> typedModule = (id<GeolocationModuleProtocol>)module;
    [typedModule clearWatch:watchId];
}

- (void)requestAuthorization:(RCTPromiseResolveBlock)resolve
                      reject:(RCTPromiseRejectBlock)reject {
    id module = [Geolocation sharedGeolocationModule];
    id<GeolocationModuleProtocol> typedModule = (id<GeolocationModuleProtocol>)module;
    [typedModule requestAuthorization:resolve reject:reject];
}

- (void)stopObserving {
    id module = [Geolocation sharedGeolocationModule];
    id<GeolocationModuleProtocol> typedModule = (id<GeolocationModuleProtocol>)module;
    [typedModule stopObserving];
}

- (void)startObserving {
    id module = [Geolocation sharedGeolocationModule];
    id<GeolocationModuleProtocol> typedModule = (id<GeolocationModuleProtocol>)module;
    [typedModule startObserving];
}

- (NSArray<NSString *> *)supportedEvents {
    id module = [Geolocation sharedGeolocationModule];
    id<GeolocationModuleProtocol> typedModule = (id<GeolocationModuleProtocol>)module;
    return [typedModule supportedEvents];
}

- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:
    (const facebook::react::ObjCTurboModule::InitParams &)params
{
    return std::make_shared<facebook::react::NativeGeolocationSpecJSI>(params);
}

@end
