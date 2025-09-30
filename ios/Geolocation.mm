#import "Geolocation.h"
#import <CoreLocation/CoreLocation.h>

#if __has_include(<Geolocation/Geolocation-Swift.h>)
#import <Geolocation/Geolocation-Swift.h>
#else
#import "Geolocation-Swift.h"
#endif

@implementation Geolocation {
    GeolocationManager *_manager;
}

RCT_EXPORT_MODULE()

- (instancetype)init {
    if (self = [super init]) {
        _manager = [[GeolocationManager alloc] init];
        
        NSLog(@"📦 [Geolocation.mm] init - GeolocationManager 생성됨");
        
        // Swift에서 이벤트를 보낼 수 있도록 콜백 설정
        __weak __typeof(self) weakSelf = self;
        [_manager setSendEventCallback:^(NSString *eventName, NSDictionary *params) {
            __strong __typeof(weakSelf) strongSelf = weakSelf;
            NSLog(@"📡 [Geolocation.mm] 이벤트 콜백 받음: %@", eventName);
            NSLog(@"   데이터: %@", params);
            
            if (strongSelf && strongSelf.bridge) {
                NSLog(@"   ✅ sendEventWithName 호출");
                [strongSelf sendEventWithName:eventName body:params];
            } else {
                NSLog(@"   ❌ strongSelf 또는 bridge가 nil");
                NSLog(@"   strongSelf: %@, bridge: %@", strongSelf, strongSelf.bridge);
            }
        }];
        
        NSLog(@"✅ [Geolocation.mm] 이벤트 콜백 설정 완료");
    }
    return self;
}

// MARK: - RCTEventEmitter

- (NSArray<NSString *> *)supportedEvents {
    return @[@"onLocationChanged", @"onLocationError"];
}

+ (BOOL)requiresMainQueueSetup {
    return NO;
}

// MARK: - Event Emitter Methods (TurboModule)

- (void)addListener:(NSString *)eventName {
    NSLog(@"👂 [Geolocation.mm] addListener: %@", eventName);
    [super addListener:eventName];
}

- (void)removeListeners:(double)count {
    NSLog(@"🔇 [Geolocation.mm] removeListeners: %.0f", count);
    [super removeListeners:count];
}

// MARK: - Geolocation Methods

- (void)requestAuthorization:(RCTPromiseResolveBlock)resolve
                      reject:(RCTPromiseRejectBlock)reject
{
    [_manager requestAuthorizationWithResolve:resolve reject:reject];
}

- (void)getCurrentPosition:(JS::NativeGeolocation::GeolocationOptions &)options
                   resolve:(RCTPromiseResolveBlock)resolve
                    reject:(RCTPromiseRejectBlock)reject
{
    // C++ struct를 NSDictionary로 변환
    NSMutableDictionary *optionsDict = [NSMutableDictionary new];
    
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
    
    [_manager getCurrentPositionWithOptions:optionsDict resolve:resolve reject:reject];
}

- (void)watchPosition:(JS::NativeGeolocation::GeolocationOptions &)options
              resolve:(RCTPromiseResolveBlock)resolve
               reject:(RCTPromiseRejectBlock)reject
{
    // C++ struct를 NSDictionary로 변환
    NSMutableDictionary *optionsDict = [NSMutableDictionary new];
    
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
    
    [_manager watchPositionWithOptions:optionsDict resolve:resolve reject:reject];
}

- (void)clearWatch:(double)watchId
{
    [_manager clearWatchWithWatchId:@(watchId)];
}

- (void)stopObserving
{
    [_manager stopObserving];
}

// MARK: - TurboModule

- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:
    (const facebook::react::ObjCTurboModule::InitParams &)params
{
    return std::make_shared<facebook::react::NativeGeolocationSpecJSI>(params);
}

@end
