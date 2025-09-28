#import <React/RCTEventEmitter.h>
#import <GeolocationSpec/GeolocationSpec.h>

@protocol GeolocationModuleProtocol <NSObject>
- (void)getCurrentPosition:(NSDictionary *)options
                   resolve:(RCTPromiseResolveBlock)resolve
                    reject:(RCTPromiseRejectBlock)reject;
- (void)watchPosition:(NSDictionary *)options
              resolve:(RCTPromiseResolveBlock)resolve
               reject:(RCTPromiseRejectBlock)reject;
- (void)clearWatch:(double)watchId;
- (void)requestAuthorization:(RCTPromiseResolveBlock)resolve
                      reject:(RCTPromiseRejectBlock)reject;
- (void)stopObserving;
- (void)startObserving;
- (NSArray<NSString *> *)supportedEvents;
@end

@interface Geolocation : RCTEventEmitter <NativeGeolocationSpec>

@end
