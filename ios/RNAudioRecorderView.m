//
//  RNAudioRecorderView.m
//  RNAudioRecorder
//
//  Created by Dev on 2019/2/1.
//  Copyright © 2019 Facebook. All rights reserved.
//

#import "RNAudioRecorderView.h"
#import "WaveFormView.h"
#import "SoundFile.h"

@interface RNAudioRecorderView() {
    WaveFormView *waveform;
    SoundFile *soundFile;
}

@property (nonatomic, weak) RCTBridge *bridge;

@end

@implementation RNAudioRecorderView
@synthesize plotLineColor = _plotLineColor;
@synthesize timeTextSize = _timeTextSize;
@synthesize timeTextColor = _timeTextColor;

- (id)initWithBridge:(RCTBridge *)bridge {
    if ((self = [super init])) {
        self.bridge = bridge;
    }
    [self initView];
    return self;
}

- (void) initView {
    waveform = [[WaveFormView alloc] initWithFrame:self.bounds];
    waveform.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
    [self addSubview:waveform];
}

- (void) setPlotLineColor:(UIColor *)plotLineColor {
    _plotLineColor = plotLineColor;
    waveform.plotLineColor = _plotLineColor;
}

- (void)setTimeTextSize:(NSInteger)timeTextSize {
    _timeTextSize = timeTextSize;
    waveform.timeTextSize = timeTextSize;
}

- (void)setTimeTextColor:(UIColor *)timeTextColor {
    _timeTextColor = timeTextColor;
    waveform.timeTextColor = timeTextColor;
}

- (void)layoutSubviews {
    [super layoutSubviews];
}

- (void) initialize:(NSString *) filepath offset:(NSInteger) offset {
    [self destroy];
    soundFile = [[SoundFile alloc] initWithFilePath:filepath pixelsPerSec:_pixelsPerSecond fromInMs:offset toInMs:-1];
    waveform.soundFile = soundFile;
}
- (NSString*) renderByFile:(NSString*) filepath {
    [self destroy];
    soundFile = [[SoundFile alloc] initWithFilePath:filepath pixelsPerSec:_pixelsPerSecond fromInMs:-1 toInMs:-1];
    if (![soundFile isInitialized]) {
        soundFile = nil;
        return nil;
    }
    waveform.soundFile = soundFile;
    return @"";
}
- (NSString*) cut:(NSString*) filepath fromTimeInMs:(long) fromTime toTimeInMs:(long) toTime {
    [self destroy];
    soundFile = [[SoundFile alloc] initWithFilePath:filepath pixelsPerSec:_pixelsPerSecond fromInMs:-1 toInMs:-1];
    if (![soundFile isInitialized]) {
        soundFile = nil;
        return nil;
    }
    waveform.soundFile = soundFile;
    return @"";
}
- (void) destroy {
    
}
- (void) startRecording {
    if (soundFile == nil)
        return;
    if ([soundFile fileStatus] == IsRecording) {
        [soundFile stopRecord];
    } else if ([soundFile fileStatus] == IsNone) {
        [soundFile startRecord:0];
    }
}
- (NSString*) stopRecording {
    
    return @"";
}
- (void) play {
    
}
- (long) getDuration {
    return 0;
}

@end
