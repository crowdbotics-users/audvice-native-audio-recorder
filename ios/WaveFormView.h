//
//  WaveFormView.h
//  RNAudioRecorder
//
//  Created by Dev on 2019/2/2.
//  Copyright © 2019 Facebook. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "SoundFile.h"

NS_ASSUME_NONNULL_BEGIN

@interface WaveFormView : UIView

@property (weak, nonatomic)     SoundFile *soundFile;
@property (strong, nonatomic)   UIColor *timeTextColor;
@property (nonatomic)           NSInteger timeTextSize;
@property (strong, nonatomic)   UIColor *plotLineColor;
@property (nonatomic)           NSInteger offset;
@property (nonatomic)           BOOL onScroll;

@end

NS_ASSUME_NONNULL_END
