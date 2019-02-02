//
//  WaveFormView.h
//  RNAudioRecorder
//
//  Created by Dev on 2019/2/2.
//  Copyright © 2019 Facebook. All rights reserved.
//

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

@interface WaveFormView : UIView

@property (nonatomic) NSInteger pixelsPerSecond;
@property (strong, nonatomic) UIColor *timeTextColor;
@property (nonatomic) NSInteger timeTextSize;
@property (strong, nonatomic) UIColor *plotLineColor;
@property (atomic) NSInteger offset;

@end

NS_ASSUME_NONNULL_END
