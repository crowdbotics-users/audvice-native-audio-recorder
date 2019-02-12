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
@synthesize onScroll = _onScroll;

- (id)initWithBridge:(RCTBridge *)bridge {
    if ((self = [super init])) {
        self.bridge = bridge;
    }
    [self initView];
    return self;
}

- (void)dealloc {
    // remove notification observer
    [[NSNotificationCenter defaultCenter] removeObserver:self name:UIApplicationDidEnterBackgroundNotification object:nil];
}

- (void) initView {
    // initialize subviews
    waveform = [[WaveFormView alloc] initWithFrame:self.bounds];
    waveform.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
    [self addSubview:waveform];
    
    // add notification observer to stop action when the app move to background.
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(appMoveToBackground:) name:UIApplicationDidEnterBackgroundNotification object:nil];
}

// called when thee app move to background
- (void) appMoveToBackground:(NSNotification *) notification {
    if (soundFile) {
        if (soundFile.fileStatus == IsPlaying) {
            [soundFile stopPlay];
        } else if (soundFile.fileStatus == IsRecording) {
            [soundFile stopRecord];
        }
    }
}

// set properties from js module
// plotlinecolor
// timetextsize
// teimtextcolor
// onscroll

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

- (void)setOnScroll:(BOOL)onScroll {
    _onScroll = onScroll;
    waveform.onScroll = _onScroll;
}

- (void)layoutSubviews {
    [super layoutSubviews];
}

// methods, called from js module
// initialize mothods: initialize, renderByFile, cut
//
- (void) initialize:(NSString *) filepath offset:(NSInteger) offsetInMs {
    [self destroy];
    soundFile = [[SoundFile alloc] initWithFilePath:filepath pixelsPerSec:_pixelsPerSecond fromInMs:offsetInMs toInMs:-1];
    waveform.soundFile = soundFile;
    [waveform setNeedsDisplay];
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
    soundFile = nil;
}

// start/pause recording
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

// stop recordign and then return audio path
- (NSString*) stopRecording {
    if (soundFile) {
        if ([soundFile fileStatus] == IsRecording) {
            [soundFile stopRecord];
        } else if ([soundFile fileStatus] == IsPlaying) {
            [soundFile stopPlay];
        }
        return [soundFile soundFilePath];
    }
    return @"";
}

// play/pause audio file
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
    if (soundFile) {
        return [soundFile duration];
    }
    return 0;
}

@end
