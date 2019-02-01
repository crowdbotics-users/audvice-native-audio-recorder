//
//  RNAudioRecorderView.m
//  RNAudioRecorder
//
//  Created by Dev on 2019/2/1.
//  Copyright © 2019 Facebook. All rights reserved.
//

#import "RNAudioRecorderView.h"

@interface RNAudioRecorderView()

@property (nonatomic, weak) RCTBridge *bridge;
@end

@implementation RNAudioRecorderView

- (id)initWithBridge:(RCTBridge *)bridge {
    if ((self = [super init])) {
        self.bridge = bridge;
    }
    return self;
}

@end
