//
//  RNAudioRecorderViewManager.h
//  RNAudioRecorder
//
//  Created by Dev on 2019/2/1.
//  Copyright © 2019 Facebook. All rights reserved.
//

#if __has_include("RCTBridgeModule.h")
#import "RCTBridgeModule.h"
#else
#import <React/RCTBridgeModule.h>
#endif
#import <Foundation/Foundation.h>
#import <React/RCTViewManager.h>

@interface RNAudioRecorderViewManager : RCTViewManager <RCTBridgeModule>

@end
