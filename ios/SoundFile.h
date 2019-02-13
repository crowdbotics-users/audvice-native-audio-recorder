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

// Buffer Size
#define kNumberRecordBuffers    3

// Update Time Interval for playing and recording
#define kBufferDurationSeconds .1

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
@public BOOL        mIsDone;
@public SInt64      mCurrentReadPacket;
@public UInt32      mNumPacketsToRead;    
}

@property(nonatomic)        BOOL isInitialized;
@property(atomic, strong)   NSMutableArray *plotArray;
@property(nonatomic)        NSInteger samplesPerPixel;
@property(nonatomic) AudioStreamBasicDescription audioFormat;

@property(nonatomic) FileStatus fileStatus;

- (instancetype)initWithFilePath:(NSString *) filename
                    pixelsPerSec:(NSInteger) pixelsPerSec
                        fromInMs:(NSInteger) fromInMs
                          toInMs:(NSInteger) toInMs;

- (void) buildPlotFromBuffer:(AudioQueueBufferRef) bufferRef;
- (void) bulidPlotFromBytes:(void*) bytes size:(UInt32) size;
- (void) setCurrentPlayPacket:(SInt64) currentPacket;
- (SInt64) currentPlayPacket;

- (void) startRecord:(NSInteger) startTime;
- (void) stopRecord;
- (void) play:(NSInteger) startSamples;
- (void) stopPlay;

- (NSString*) soundFilePath;
- (NSInteger) duration;

@end
