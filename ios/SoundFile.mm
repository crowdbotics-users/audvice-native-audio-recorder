//
//  SoundFile.m
//  RNAudioRecorder
//
//  Created by Dev on 2019/2/2.
//  Copyright © 2019 Facebook. All rights reserved.
//

#import "SoundFile.h"

void MyInputBufferHandler(void *                                inUserData,
                          AudioQueueRef                         inAQ,
                          AudioQueueBufferRef                   inBuffer,
                          const AudioTimeStamp *                inStartTime,
                          UInt32                                inNumPackets,
                          const AudioStreamPacketDescription*   inPacketDesc) {
    SoundFile *sf = (__bridge SoundFile*) inUserData;
    if (inNumPackets > 0) {
        OSStatus status = AudioFileWritePackets(sf->mRecordFile, FALSE, inBuffer->mAudioDataByteSize, inPacketDesc, sf->mRecordPacket, &inNumPackets, inBuffer->mAudioData);
        [sf buildPlotFromBuffer:inBuffer];
        sf->mRecordPacket += inNumPackets;
    }
    AudioQueueEnqueueBuffer(inAQ, inBuffer, 0, NULL);
}

@interface SoundFile ()
{
    AudioQueueRef       mQueue;
    AudioQueueBufferRef mBuffers[kNumberRecordBuffers];
    NSString *          mTempFilePath;
    NSInteger           mPixelsPerSec;
    
    int chunkIndex;
    int plotValue;
}

@end

@implementation SoundFile
@synthesize audioFormat = _audioFormat;

- (instancetype)initWithFilePath:(NSString *) filename
                    pixelsPerSec:(NSInteger) pixelsPerSec
                        fromInMs:(NSInteger) fromInMs
                          toInMs:(NSInteger) toInMs
{
    self = [super init];
    if (self) {
        if ([self openAudioFile:filename]) {
            _isInitialized = true;
        } else {
            [self setupAudioFormat];
        }
        _fileStatus = IsNone;
        _plotArray = [NSMutableArray new];
        chunkIndex = 0;
        plotValue = 0;
        mPixelsPerSec = pixelsPerSec;
        _samplesPerPixel = _audioFormat.mSampleRate / mPixelsPerSec;
    }
    return self;
}

- (void)dealloc {
    AudioQueueDispose(mQueue, YES);
    AudioFileClose(mRecordFile);
    [_plotArray removeAllObjects];
}

- (BOOL) openAudioFile:(NSString*) filepath {
    NSString *fullPath = [[SoundFile applicationDocumentsDirectory] stringByAppendingPathComponent:filepath];
    CFURLRef urlRef = (__bridge CFURLRef)[NSURL URLWithString:fullPath];
    OSStatus status = AudioFileOpenURL(urlRef, kAudioFileReadWritePermission, 0, &mRecordFile);
    if (status != 0) {
        return false;
    }
    UInt32 size = sizeof(_audioFormat);
    status = AudioFileGetProperty(mRecordFile, kAudioFilePropertyDataFormat, &size, &_audioFormat);
    
    return true;
}

- (void) prepareRecordingWithNewFile {
    mTempFilePath = [self getTempFile];
    CFURLRef urlRef = (__bridge CFURLRef)[NSURL URLWithString:mTempFilePath];
    OSStatus status = AudioFileCreateWithURL(urlRef, kAudioFileCAFType, &_audioFormat, kAudioFileFlags_EraseFile, &mRecordFile);
    _isInitialized = true;
}

- (void) buildPlotFromBuffer:(AudioQueueBufferRef) bufferRef {
    int16_t *audioData = (int16_t *)bufferRef->mAudioData;
    for (int i = 0; i < bufferRef->mAudioDataByteSize / 2; i++) {
        plotValue = MAX(plotValue, ABS(audioData[i]));
        chunkIndex++;
        if (chunkIndex >= mPixelsPerSec) {
            [_plotArray addObject:[NSNumber numberWithInt:plotValue]];
            chunkIndex = 0;
            plotValue = 0;
        }
    }
    [[NSNotificationCenter defaultCenter] postNotificationName:kNotificationRecordingUpdate object:nil];
}

- (void) startRecord: (NSInteger) startTime {
    if (_fileStatus == IsPlaying) {
        [self stopPlay];
    } else if (_fileStatus != IsNone) {
        return;
    }
    // open audio file
    if (!_isInitialized) {
        [self prepareRecordingWithNewFile];
    }
    int i, bufferByteSize;
    mRecordPacket = 0;
    OSStatus status = AudioQueueNewInput(&_audioFormat, MyInputBufferHandler, (__bridge void*)self, NULL, NULL, 0, &mQueue);
    [self copyEncoderCookieToFile];
    bufferByteSize = [self computeRecordBufferSize:&_audioFormat seconds:kBufferDurationSeconds];
    for (i = 0; i < kNumberRecordBuffers; ++i) {
        AudioQueueAllocateBuffer(mQueue, bufferByteSize, &mBuffers[i]);
        AudioQueueEnqueueBuffer(mQueue, mBuffers[i], 0, NULL);
    }
    
    _fileStatus = IsRecording;
    status = AudioQueueStart(mQueue, NULL);
    if (status != 0) {
        
    }
}

- (void) stopRecord {
    // close audio file
    AudioQueueStop(mQueue, YES);
    [self copyEncoderCookieToFile];
    AudioQueueDispose(mQueue, YES);
    _fileStatus = IsNone;
}

- (void) play {
    _fileStatus = IsPlaying;
}

- (void) stopPlay {
    _fileStatus = IsNone;
}

- (int) computeRecordBufferSize:(AudioStreamBasicDescription *) format seconds:(float) seconds {
    int packets, frames, bytes = 0;
    frames = (int)ceil(seconds * format->mSampleRate);
    
    if (format->mBytesPerFrame > 0)
        bytes = frames * format->mBytesPerFrame;
    else {
        UInt32 maxPacketSize;
        if (format->mBytesPerPacket > 0)
            maxPacketSize = format->mBytesPerPacket;    // constant packet size
        else {
            UInt32 propertySize = sizeof(maxPacketSize);
            AudioQueueGetProperty(mQueue, kAudioQueueProperty_MaximumOutputPacketSize, &maxPacketSize,
                                                &propertySize);
        }
        if (format->mFramesPerPacket > 0)
            packets = frames / format->mFramesPerPacket;
        else
            packets = frames;    // worst-case scenario: 1 frame in a packet
        if (packets == 0)        // sanity check
            packets = 1;
        bytes = packets * maxPacketSize;
    }
    return bytes;
}

- (void) copyEncoderCookieToFile {
    UInt32 propertySize;
    // get the magic cookie, if any, from the converter
    OSStatus err = AudioQueueGetPropertySize(mQueue, kAudioQueueProperty_MagicCookie, &propertySize);
    
    // we can get a noErr result and also a propertySize == 0
    // -- if the file format does support magic cookies, but this file doesn't have one.
    if (err == noErr && propertySize > 0) {
        Byte *magicCookie = new Byte[propertySize];
        UInt32 magicCookieSize;
        AudioQueueGetProperty(mQueue, kAudioQueueProperty_MagicCookie, magicCookie, &propertySize);
        magicCookieSize = propertySize;    // the converter lies and tell us the wrong size
        
        // now set the magic cookie on the output file
        UInt32 willEatTheCookie = false;
        // the converter wants to give us one; will the file take it?
        err = AudioFileGetPropertyInfo(mRecordFile, kAudioFilePropertyMagicCookieData, NULL, &willEatTheCookie);
        if (err == noErr && willEatTheCookie) {
            err = AudioFileSetProperty(mRecordFile, kAudioFilePropertyMagicCookieData, magicCookieSize, magicCookie);
        }
        delete[] magicCookie;
    }
}

- (NSString*) getTempFile {
    NSDate *now = [NSDate date];
    NSDateFormatter *simpleFormat = [[NSDateFormatter alloc] init];
    simpleFormat.dateFormat = @"yyyy-mm-dd-HH-MM-SS-zzz";
    NSString *filename = [NSString stringWithFormat:@"%@.caf", [simpleFormat stringFromDate:now]];
    return [NSTemporaryDirectory() stringByAppendingPathComponent:filename];
}

- (void) setupAudioFormat {
    memset(&_audioFormat, 0, sizeof(_audioFormat));
    _audioFormat.mSampleRate = [AVAudioSession sharedInstance].sampleRate;
    _audioFormat.mChannelsPerFrame = 1;
    _audioFormat.mFormatID = kAudioFormatLinearPCM;
    _audioFormat.mFormatFlags = kLinearPCMFormatFlagIsSignedInteger | kLinearPCMFormatFlagIsPacked;
    _audioFormat.mBitsPerChannel = 16;
    _audioFormat.mBytesPerPacket = _audioFormat.mBytesPerFrame = (_audioFormat.mBitsPerChannel / 8) * _audioFormat.mChannelsPerFrame;
    _audioFormat.mFramesPerPacket = 1;
}

+ (NSString *) applicationDocumentsDirectory
{
    NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *basePath = paths.firstObject;
    return basePath;
}

@end
