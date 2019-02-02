//
//  SoundFile.h
//  RNAudioRecorder
//
//  Created by Dev on 2019/2/2.
//  Copyright © 2019 Facebook. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <AVFoundation/AVFoundation.h>
#import <AudioToolbox/AudioToolbox.h>

#define kNumberRecordBuffers    3
#define kBufferDurationSeconds .5

#define kNotificationRecordingUpdate @"notificationrecording"
#define kNotificationPlayingUpdate @"notificationplaying"

typedef enum : NSUInteger {
    IsNone = 0,
    IsReading,
    IsRecording,
    IsPlaying,
    IsWriting
} FileStatus;

@interface SoundFile : NSObject {    
    @public AudioFileID mRecordFile;
    @public SInt64      mRecordPacket;
}

@property(nonatomic)        BOOL isInitialized;
@property(atomic, strong)   NSMutableArray *plotArray;
@property(nonatomic)        NSInteger samplesPerPixel;
@property(nonatomic, strong) AudioStreamBasicDescription audioFormat;

@property(nonatomic) FileStatus fileStatus;

- (instancetype)initWithFilePath:(NSString *) filename
                    pixelsPerSec:(NSInteger) pixelsPerSec
                        fromInMs:(NSInteger) fromInMs
                          toInMs:(NSInteger) toInMs;

- (void) buildPlotFromBuffer:(AudioQueueBufferRef) bufferRef;
- (void) startRecord:(NSInteger) startTime;
- (void) stopRecord;
- (void) play;
- (void) stopPlay;

@end
