//
//  SoundFile.h
//  RNAudioRecorder
//
//  Created by Dev on 2019/2/2.
//  Copyright © 2019 Facebook. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <AudioToolbox/AudioToolbox.h>

#define kNumberRecordBuffers    3

typedef enum : NSUInteger {
    IsNone = 0,
    IsReading,
    IsRecording,
    IsPlaying,
    IsWriting
} FileStatus;

@interface SoundFile : NSObject
{
    
    AudioQueueRef       mQueue;
    AudioQueueBufferRef mBuffers[kNumberRecordBuffers];
    AudioFileID         mRecordFile;
}

@property(nonatomic) BOOL isNew;
@property(nonatomic) FileStatus fileStatus;

- (instancetype)initWithFilePath:(NSString *) filename
                    pixelsPerSec:(NSInteger) pixelsPerSec
                        fromInMs:(NSInteger) fromInMs
                          toInMs:(NSInteger) toInMs;

- (void) startRecord(NSInteger startTime);
- (void) stopRecord();
- (void) play();
- (void) stopPlay();

@end
