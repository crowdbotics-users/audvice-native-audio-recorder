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

- (void)dealloc {
    [[NSNotificationCenter defaultCenter] removeObserver:self name:UIApplicationDidEnterBackgroundNotification object:nil];
}

- (void) initView {
    waveform = [[WaveFormView alloc] initWithFrame:self.bounds];
    waveform.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
    [self addSubview:waveform];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(appMoveToBackground:) name:UIApplicationDidEnterBackgroundNotification object:nil];
}

- (void) appMoveToBackground:(NSNotification *) notification {
    if (soundFile) {
        if (soundFile.fileStatus == IsPlaying) {
            [soundFile stopPlay];
        } else if (soundFile.fileStatus == IsRecording) {
            [soundFile stopRecord];
        }
    }
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

- (void) initialize:(NSString *) filepath offset:(NSInteger) offsetInMs {
    [self destroy];
    soundFile = [[SoundFile alloc] initWithFilePath:filepath pixelsPerSec:_pixelsPerSecond fromInMs:-1 toInMs:-1];
    waveform.soundFile = soundFile;
    [waveform setNeedsDisplay];
    NSInteger offset = offsetInMs / 1000.f * soundFile.audioFormat.mSampleRate / soundFile.samplesPerPixel;
    [waveform setOffset:offset];
}
- (NSString*) renderByFile:(NSString*) filepath {
    [self destroy];
    soundFile = [[SoundFile alloc] initWithFilePath:filepath pixelsPerSec:_pixelsPerSecond fromInMs:-1 toInMs:-1];
    if (![soundFile isInitialized]) {
        soundFile = nil;
        waveform.soundFile = nil;
        [waveform setNeedsDisplay];
        return nil;
    }
    waveform.soundFile = soundFile;
    [waveform setNeedsDisplay];
    return [soundFile soundFilePath];
}
- (NSString*) cut:(NSString*) filepath fromTimeInMs:(long) fromTime toTimeInMs:(long) toTime {
    [self destroy];
    soundFile = [[SoundFile alloc] initWithFilePath:filepath pixelsPerSec:_pixelsPerSecond fromInMs:fromTime toInMs:toTime];
    if (![soundFile isInitialized]) {
        soundFile = nil;
        waveform.soundFile = nil;
        [waveform setNeedsDisplay];
        return nil;
    }
    waveform.soundFile = soundFile;
    [waveform setNeedsDisplay];
    return [soundFile soundFilePath];
}
- (void) destroy {
    
}
- (void) startRecording {
    if (soundFile == nil)
        return;
    if ([soundFile fileStatus] == IsRecording) {
        [soundFile stopRecord];
    } else if ([soundFile fileStatus] == IsNone) {
        NSInteger offset = [waveform offset];
        NSInteger samples = offset * soundFile.samplesPerPixel;
        [soundFile startRecord:samples];
    }
}
- (NSString*) stopRecording {
    if (soundFile) {
        return [soundFile soundFilePath];
    }
    return @"";
}
- (void) play {
    if (soundFile == nil)
        return;
    if ([soundFile fileStatus] == IsPlaying) {
        [soundFile stopPlay];
    } else if ([soundFile fileStatus] == IsNone) {
        NSInteger offset = [waveform offset];
        NSInteger samples = offset * soundFile.samplesPerPixel;
        [soundFile play:samples];
    }
    
}
- (long) getDuration {
    return 0;
}

@end
