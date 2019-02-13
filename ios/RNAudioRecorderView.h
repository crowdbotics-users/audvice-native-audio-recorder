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
#import <React/RCTComponent.h>

@interface RNAudioRecorderView : UIView

@property (nonatomic) NSInteger pixelsPerSecond;
@property (strong, nonatomic) UIColor *timeTextColor;
@property (nonatomic) NSInteger timeTextSize;
@property (strong, nonatomic) UIColor *plotLineColor;
@property (nonatomic) BOOL onScroll;
@property (nonatomic, copy) RCTBubblingEventBlock onPlayFinished;

- (id)initWithBridge:(RCTBridge *)bridge;

// methods
- (void) initialize:(NSString *) filepath offset:(long) offset;
- (NSString*) renderByFile:(NSString*) filepath;
- (NSString*) cut:(NSString*) filepath fromTimeInMs:(long) fromTime toTimeInMs:(long) toTime;

- (void) destroy;

- (BOOL) startRecording;
- (NSString*) stopRecording;

- (BOOL) play;
- (BOOL) pause;

- (long) getDuration;

@end
