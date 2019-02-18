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
    [[NSNotificationCenter defaultCenter] removeObserver:self name:kNotificationPlayingUpdate object:nil];
}

- (void) initView {
    // initialize subviews
    waveform = [[WaveFormView alloc] initWithFrame:self.bounds];
    waveform.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
    waveform.delegate = self;
    [self addSubview:waveform];
    
    // add notification observer to stop action when the app move to background.
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(appMoveToBackground:) name:UIApplicationDidEnterBackgroundNotification object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(playUpdated:) name:kNotificationPlayingUpdate object:nil];
    
    
    // set audio session
    AVAudioSession *session = [AVAudioSession sharedInstance];    
    [session setCategory:AVAudioSessionCategoryPlayAndRecord withOptions:AVAudioSessionCategoryOptionDefaultToSpeaker error:nil];
    [session setActive:YES error:nil];
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

- (void) playUpdated:(NSNotification *) notification {
    if (![notification.object boolValue]) {
        self.onEventCallback(@{
                               @"type":@"PlayFinished"
                               });
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
- (BOOL) startRecording {
    if (soundFile == nil)
        return false;
    if ([soundFile fileStatus] == IsPlaying) {
        [soundFile stopPlay];
    }
    if ([soundFile fileStatus] == IsNone) {
        NSInteger offset = [waveform offset];
        NSInteger samples = offset * soundFile.samplesPerPixel;
        [soundFile startRecord:samples];
    }
    return true;
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

// play audio file
- (BOOL) play {
    if (soundFile == nil)
        return false;
    if ([soundFile fileStatus] == IsRecording) {
        [soundFile stopRecord];
    }
    if ([soundFile fileStatus] == IsNone) {
        NSInteger offset = [waveform offset];
        NSInteger samples = offset * soundFile.samplesPerPixel;
        [soundFile play:samples];
    }
    return true;
}

// play audio file
- (BOOL) pause {
    if (soundFile == nil)
        return false;
    if ([soundFile fileStatus] == IsPlaying) {
        [soundFile stopPlay];
    }
    return true;
}

- (long) getDuration {
    if (soundFile) {
        return [soundFile duration];
    }
    return 0;
}


// return current position by ms
- (long) getPosition {
    if (soundFile == nil)
        return 0;
    long offset = [waveform offset];
    return offset * soundFile.samplesPerPixel * 1000 / soundFile.audioFormat.mSampleRate;
}

#pragma mark - WaveFormDelegate
- (void)onScrolled:(WaveFormView *)view toOffset:(NSInteger)offset {
    if (soundFile == nil) {
        return;
    }
    long positionInMs = offset * 1000 * soundFile.samplesPerPixel / soundFile.audioFormat.mSampleRate;
    if (self.onEventCallback) {
        self.onEventCallback(@{
                               @"type":@"Scrolled",
                               @"position":[NSNumber numberWithLong:positionInMs]
                               });
    }
}

@end
