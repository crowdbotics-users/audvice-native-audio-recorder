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

void AQBufferCallback(void * inUserData,
                          AudioQueueRef inAQ,
                          AudioQueueBufferRef inBuffer) {
    SoundFile *sf = (__bridge SoundFile*) inUserData;
    
    if (sf->mIsDone) return;
    UInt32 numBytes;
    UInt32 nPackets = sf->mNumPacketsToRead;
    OSStatus result = AudioFileReadPacketData(sf->mRecordFile, false, &numBytes, inBuffer->mPacketDescriptions, sf->mCurrentReadPacket, &nPackets, inBuffer->mAudioData);
    if (result)
        printf("AudioFileReadPackets failed: %d", (int)result);
    if (nPackets > 0) {
        inBuffer->mAudioDataByteSize = numBytes;
        inBuffer->mPacketDescriptionCount = nPackets;
        AudioQueueEnqueueBuffer(inAQ, inBuffer, 0, NULL);
        [sf setCurrentPlayPacket:sf->mCurrentReadPacket + nPackets];
    }
    
    else
    {
        sf->mIsDone = true;
        [sf setFileStatus:IsNone];
        AudioQueueStop(inAQ, false);
    }
}

void isRunningProc (  void *              inUserData,
                              AudioQueueRef           inAQ,
                              AudioQueuePropertyID    inID)
{
    SoundFile *sf = (__bridge SoundFile*) inUserData;
    UInt32 isRunning = 0;
    UInt32 size = sizeof(isRunning);
    
    OSStatus result = AudioQueueGetProperty (inAQ, kAudioQueueProperty_IsRunning, &isRunning, &size);
    
    if (result == noErr)
        [[NSNotificationCenter defaultCenter] postNotificationName: kNotificationPlayingUpdate object: [NSNumber numberWithBool:isRunning]];
}

@interface SoundFile ()
{
    AudioQueueRef       mQueue;
    AudioQueueBufferRef mBuffers[kNumberRecordBuffers];
    NSString *          mTempFilePath;
    NSInteger           mPixelsPerSec;
    
    NSInteger chunkIndex;
    int plotValue;
    NSInteger plotIndex;
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
        [self setupAudioFormat];
        _fileStatus = IsNone;
        _plotArray = [NSMutableArray new];
        chunkIndex = 0;
        plotValue = 0;
        mPixelsPerSec = pixelsPerSec;
        mNumPacketsToRead = 0;
        mIsDone = false;
        _samplesPerPixel = _audioFormat.mSampleRate / mPixelsPerSec;
        if ([self openAudioFile:filename fromTimeInMs:fromInMs toTimeInMs:toInMs]) {
            _isInitialized = true;
        }
    }
    return self;
}

- (void)dealloc {
    AudioQueueDispose(mQueue, YES);
    AudioFileClose(mRecordFile);
    [_plotArray removeAllObjects];
}

- (BOOL) openAudioFile:(NSString*) filepath
          fromTimeInMs:(NSInteger) fromTms toTimeInMs:(NSInteger) toTms
{
    // get information from input file
    CFURLRef urlRef = (__bridge CFURLRef)[NSURL URLWithString:filepath];
    ExtAudioFileRef inputFile;
    if (![self checkError:ExtAudioFileOpenURL(urlRef, &inputFile) withErrorString:@"Audio Source Error"])
    {
        return false;
    }
    AudioStreamBasicDescription inputFormat;
    UInt32 size = sizeof(inputFormat);
    
    if (![self checkError:ExtAudioFileGetProperty(inputFile, kExtAudioFileProperty_FileDataFormat, &size, &inputFormat) withErrorString:@"Audio Format Error"])
    {
        return false;
    }
    
    // Prepare Recording File
    [self prepareRecordingWithNewFile];
    
    size = sizeof(_audioFormat);
    
    if (![self checkError:ExtAudioFileSetProperty(inputFile, kExtAudioFileProperty_ClientDataFormat, size, &_audioFormat) withErrorString:@"Cannot set client format on the source"])
    {
        return false;
    }
    
    // Set Converter to input file
    AudioConverterRef converter = 0;
    size = sizeof(converter);
    
    if (![self checkError:ExtAudioFileGetProperty(inputFile, kExtAudioFileProperty_AudioConverter, &size, &converter) withErrorString:@"Failed to get converter"]) {
        return false;
    }
    
    // Set buffer for reading audio data from input file
    UInt32 bufferByteSize = 32768;
    char sourceBuffer[bufferByteSize];
    
    /*
     keep track of the source file offset so we know where to reset the source for
     reading if interrupted and input was not consumed by the audio converter
     */
    SInt64 sourceFrameOffset = 0;
    SInt64 destBytesOffset = 0;
    SInt64 sourceBytesOffset = 0;
    
    // check invalidation
    SInt64 fromBytes = fromTms / 1000.f * _audioFormat.mSampleRate * 2;
    SInt64 toBytes = toTms / 1000.f * _audioFormat.mSampleRate * 2;
    
    if (toTms != -1) {
        fromBytes = MAX(0, fromBytes);
        if (fromTms >= toTms) {
            fromBytes = INT64_MAX;
            toBytes = INT64_MAX;
        }
    } else if (fromTms != -1) {
        toBytes = INT64_MAX;
    } else {
        fromBytes = INT64_MAX;
        toBytes = INT64_MAX;
    }
    
    OSStatus error = 0;
    
    while (YES) {
        // Set up output buffer list
        AudioBufferList fillBufferList = {};
        fillBufferList.mNumberBuffers = 1;
        fillBufferList.mBuffers[0].mNumberChannels = _audioFormat.mChannelsPerFrame;
        fillBufferList.mBuffers[0].mDataByteSize = bufferByteSize;
        fillBufferList.mBuffers[0].mData = sourceBuffer;
        
        /*
         The client format is always linear PCM - so here we determine how many frames of lpcm
         we can read/write given our buffer size
         */
        UInt32 numberOfFrames = 0;
        if (_audioFormat.mBytesPerFrame > 0) {
            // Handles bogus analyzer divide by zero warning mBytesPerFrame can't be a 0 and is protected by an Assert.
            numberOfFrames = bufferByteSize / _audioFormat.mBytesPerFrame;
        }
        
        if (![self checkError:ExtAudioFileRead(inputFile, &numberOfFrames, &fillBufferList) withErrorString:@"ExtAudioFileRead failed!"]) {
            return false;
        }
        
        if (!numberOfFrames) {
            // This is our termination condition.
            error = noErr;
            break;
        }
        
        UInt32 numBytes = fillBufferList.mBuffers[0].mDataByteSize;
        UInt32 writeBytes = numBytes;
        // if chunk is fully in cut interval
        if (sourceBytesOffset < fromBytes) {
            if (sourceBytesOffset + numBytes > fromBytes) {
                writeBytes = (UInt32)(fromBytes - sourceBytesOffset);
                error = AudioFileWriteBytes(mRecordFile, false, destBytesOffset, &writeBytes, fillBufferList.mBuffers[0].mData);
                [self bulidPlotFromBytes:fillBufferList.mBuffers[0].mData size:writeBytes];
                destBytesOffset += writeBytes;
            } else {// chunk is befor fromBytes
                error = AudioFileWriteBytes(mRecordFile, false, destBytesOffset, &writeBytes, fillBufferList.mBuffers[0].mData);
                [self bulidPlotFromBytes:fillBufferList.mBuffers[0].mData size:writeBytes];
                destBytesOffset += writeBytes;
            }
        }
        
        if (sourceBytesOffset < toBytes) {
            if (sourceBytesOffset + numBytes > toBytes) {
                UInt8 *writeData = (UInt8*)fillBufferList.mBuffers[0].mData + (toBytes - sourceBytesOffset);
                writeBytes = (UInt32)(sourceBytesOffset + numBytes - toBytes);
                error = AudioFileWriteBytes(mRecordFile, false, destBytesOffset, &writeBytes, (void*)writeData);
                [self bulidPlotFromBytes:writeData size:writeBytes];
                destBytesOffset += writeBytes;
            }
        } else {// chunk is after toBytes
            error = AudioFileWriteBytes(mRecordFile, false, destBytesOffset, &writeBytes, fillBufferList.mBuffers[0].mData);
            [self bulidPlotFromBytes:fillBufferList.mBuffers[0].mData size:writeBytes];
            destBytesOffset += writeBytes;
        }
        
        sourceBytesOffset += numBytes;
        sourceFrameOffset += numberOfFrames;
    }
    if (inputFile) { ExtAudioFileDispose(inputFile); }
    if (converter) { AudioConverterDispose(converter); }
    return true;
}

- (void) prepareRecordingWithNewFile {
    // Create Temp file in Tmp Directory
    mTempFilePath = [self getTempFile];
    CFURLRef urlRef = (__bridge CFURLRef)[NSURL URLWithString:mTempFilePath];
    AudioFileCreateWithURL(urlRef, kAudioFileWAVEType, &_audioFormat, kAudioFileFlags_EraseFile, &mRecordFile);
    _isInitialized = true;
}

- (NSString *)soundFilePath {
    return mTempFilePath;
}

- (NSInteger)duration {
    Float64 outDataSize = 0;
    UInt32 thePropSize = sizeof(Float64);
    OSStatus result = AudioFileGetProperty(mRecordFile, kAudioFilePropertyEstimatedDuration, &thePropSize, &outDataSize);
    if (result) {
        return 0;
    }
    return outDataSize * 1000;
}

// while recording, create waveform
- (void) buildPlotFromBuffer:(AudioQueueBufferRef) bufferRef {
    int16_t *audioData = (int16_t *)bufferRef->mAudioData;
    for (int i = 0; i < bufferRef->mAudioDataByteSize / 2; i++) {
        plotValue = MAX(plotValue, ABS(audioData[i]));
        chunkIndex++;
        if (chunkIndex >= _samplesPerPixel) {
            if (plotIndex < _plotArray.count) {
                [_plotArray replaceObjectAtIndex:plotIndex withObject:[NSNumber numberWithInt:plotValue]];
            } else {
                [_plotArray addObject:[NSNumber numberWithInt:plotValue]];
            }
            plotIndex++;
            chunkIndex = 0;
            plotValue = 0;
        }
    }
    [[NSNotificationCenter defaultCenter] postNotificationName:kNotificationRecordingUpdate object:[NSNumber numberWithInteger:plotIndex]];
}

// while reading audio file, create waveform
- (void) bulidPlotFromBytes:(void*) bytes size:(UInt32) size {
    int16_t *audioData = (int16_t *)bytes;
    for (int i = 0; i < size / 2; i++) {
        plotValue = MAX(plotValue, ABS(audioData[i]));
        chunkIndex++;
        if (chunkIndex >= _samplesPerPixel) {
            if (plotIndex < _plotArray.count) {
                [_plotArray replaceObjectAtIndex:plotIndex withObject:[NSNumber numberWithInt:plotValue]];
            } else {
                [_plotArray addObject:[NSNumber numberWithInt:plotValue]];
            }
            plotIndex++;
            chunkIndex = 0;
            plotValue = 0;
        }
    }
}

- (void) setCurrentPlayPacket:(SInt64) currentPacket {
    mCurrentReadPacket = currentPacket;
//    [[NSNotificationCenter defaultCenter] postNotificationName:kNotificationPlayingUpdate object:nil];
}

- (SInt64) currentPlayPacket {
    return mCurrentReadPacket;    
}

- (void) startRecord: (NSInteger) samples {
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
    mRecordPacket = samples;
    plotIndex = samples / _samplesPerPixel;
    chunkIndex = samples % _samplesPerPixel;
    if (plotIndex < _plotArray.count) {
        plotValue = [_plotArray[plotIndex] intValue];
    } else {
        plotValue = 0;
    }
    
    // set input queue
    OSStatus status = AudioQueueNewInput(&_audioFormat, MyInputBufferHandler, (__bridge void*)self, NULL, NULL, 0, &mQueue);
    [self copyEncoderCookieToFile];
    bufferByteSize = [self computeRecordBufferSize:&_audioFormat seconds:kBufferDurationSeconds];
    for (i = 0; i < kNumberRecordBuffers; ++i) {
        AudioQueueAllocateBuffer(mQueue, bufferByteSize, &mBuffers[i]);
        AudioQueueEnqueueBuffer(mQueue, mBuffers[i], 0, NULL);
    }
    
    _fileStatus = IsRecording;
    status = AudioQueueStart(mQueue, NULL);
}

- (void) stopRecord {
    // close audio file
    AudioQueueStop(mQueue, YES);
    [self copyEncoderCookieToFile];
    AudioQueueDispose(mQueue, YES);
    _fileStatus = IsNone;
}

- (void) play:(NSInteger) startSamples {
    _fileStatus = IsPlaying;
    mIsDone = false;
    AudioQueueNewOutput(&_audioFormat, AQBufferCallback, (__bridge void*)self, CFRunLoopGetCurrent(), kCFRunLoopCommonModes, 0, &mQueue);
    UInt32 bufferByteSize;
    // we need to calculate how many packets we read at a time, and how big a buffer we need
    // we base this on the size of the packets in the file and an approximate duration for each buffer
    // first check to see what the max size of a packet is - if it is bigger
    // than our allocation default size, that needs to become larger
    UInt32 maxPacketSize;
    UInt32 size = sizeof(maxPacketSize);
    AudioFileGetProperty(mRecordFile, kAudioFilePropertyPacketSizeUpperBound, &size, &maxPacketSize);
    
    // adjust buffer size to represent about a half second of audio based on this format
    [self calculateBytesForTime:_audioFormat inMaxPacketSize:maxPacketSize inSeconds:kBufferDurationSeconds outBufferSize:&bufferByteSize outNumPackets:&mNumPacketsToRead];
    
    //printf ("Buffer Byte Size: %d, Num Packets to Read: %d\n", (int)bufferByteSize, (int)mNumPacketsToRead);
    
    // (2) If the file has a cookie, we should get it and set it on the AQ
    size = sizeof(UInt32);
    OSStatus result = AudioFileGetPropertyInfo (mRecordFile, kAudioFilePropertyMagicCookieData, &size, NULL);
    
    if (!result && size) {
        char* cookie = new char [size];
        AudioFileGetProperty (mRecordFile, kAudioFilePropertyMagicCookieData, &size, cookie);
        AudioQueueSetProperty(mQueue, kAudioQueueProperty_MagicCookie, cookie, size);
        delete [] cookie;
    }
    
    // channel layout?
    result = AudioFileGetPropertyInfo(mRecordFile, kAudioFilePropertyChannelLayout, &size, NULL);
    if (result == noErr && size > 0) {
        AudioChannelLayout *acl = (AudioChannelLayout *)malloc(size);
        
        result = AudioFileGetProperty(mRecordFile, kAudioFilePropertyChannelLayout, &size, acl);
        
        result = AudioQueueSetProperty(mQueue, kAudioQueueProperty_ChannelLayout, acl, size);
        
        free(acl);
    }
    
    AudioQueueAddPropertyListener(mQueue, kAudioQueueProperty_IsRunning, isRunningProc, (__bridge void*)self);
    
    bool isFormatVBR = (_audioFormat.mBytesPerPacket == 0 || _audioFormat.mFramesPerPacket == 0);
    for (int i = 0; i < kNumberRecordBuffers; ++i) {
        AudioQueueAllocateBufferWithPacketDescriptions(mQueue, bufferByteSize, (isFormatVBR ? mNumPacketsToRead : 0), &mBuffers[i]);
    }
    
    // set the volume of the queue
    AudioQueueSetParameter(mQueue, kAudioQueueParam_Volume, 1.0);
    
    mCurrentReadPacket = startSamples;
    
    
    for (int i = 0; i < kNumberRecordBuffers; ++i) {
        AQBufferCallback((__bridge void*)self, mQueue, mBuffers[i]);
    }
    AudioQueueStart(mQueue, NULL);
}

- (void) stopPlay {
    _fileStatus = IsNone;
    AudioQueueStop(mQueue, true);
}

// computer buffersize for recordign based on record format
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

// computer buffersize for reading audio file based on input file format
- (void) calculateBytesForTime:(AudioStreamBasicDescription) inDesc inMaxPacketSize:(UInt32)inMaxPacketSize
                     inSeconds:(Float64) inSeconds outBufferSize:(UInt32*) outBufferSize outNumPackets:(UInt32*) outNumPackets {
    // we only use time here as a guideline
    // we're really trying to get somewhere between 16K and 64K buffers, but not allocate too much if we don't need it
    static const int maxBufferSize = 0x10000; // limit size to 64K
    static const int minBufferSize = 0x4000; // limit size to 16K
    
    if (inDesc.mFramesPerPacket) {
        Float64 numPacketsForTime = inDesc.mSampleRate / inDesc.mFramesPerPacket * inSeconds;
        *outBufferSize = numPacketsForTime * inMaxPacketSize;
    } else {
        // if frames per packet is zero, then the codec has no predictable packet == time
        // so we can't tailor this (we don't know how many Packets represent a time period
        // we'll just return a default buffer size
        *outBufferSize = maxBufferSize > inMaxPacketSize ? maxBufferSize : inMaxPacketSize;
    }
    
    // we're going to limit our size to our default
    if (*outBufferSize > maxBufferSize && *outBufferSize > inMaxPacketSize)
        *outBufferSize = maxBufferSize;
    else {
        // also make sure we're not too small - we don't want to go the disk for too small chunks
        if (*outBufferSize < minBufferSize)
            *outBufferSize = minBufferSize;
    }
    *outNumPackets = *outBufferSize / inMaxPacketSize;
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

- (BOOL)checkError:(OSStatus)error withErrorString:(NSString *)string {
    if (error == noErr) {
        return YES;
    }
    
    NSError *err = [NSError errorWithDomain:@"AudioOperation" code:error userInfo:@{NSLocalizedDescriptionKey : string}];
    NSLog(@"Error: %@", err);
    
    return NO;
}

- (NSString*) getTempFile {
    NSDate *now = [NSDate date];
    NSDateFormatter *simpleFormat = [[NSDateFormatter alloc] init];
    simpleFormat.dateFormat = @"yyyy-mm-dd-HH-MM-SS-zzz";
    NSString *filename = [NSString stringWithFormat:@"%@.wav", [simpleFormat stringFromDate:now]];
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
