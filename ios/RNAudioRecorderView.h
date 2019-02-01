//
//  RNAudioRecorderView.h
//  RNAudioRecorder
//
//  Created by Dev on 2019/2/1.
//  Copyright © 2019 Facebook. All rights reserved.
//

#import <AVFoundation/AVFoundation.h>
#import <React/RCTBridge.h>
#import <React/RCTBridgeModule.h>
#import <UIKit/UIKit.h>

@interface RNAudioRecorderView : UIView

- (id)initWithBridge:(RCTBridge *)bridge;

@end
