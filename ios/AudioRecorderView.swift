//
//  AudioRecorderView.swift
//  reactnativeaudiorecorder
//

import Foundation
import UIKit

import AudioKit
import AudioKitUI

// Represents the our native ui (view) component
public class AudioRecorderView: EZAudioPlot {
    // The width of the component received from React Native
    public var componentWidth: Double = 0.00
    
    // The height of the component received from React Native
    public var componentHeight: Double = 0.00
    
    // The background color of the view calculated from the received color
    public var bgColor: UIColor = UIColor(white: 0, alpha: 0.0)
    
    // Constructor
    private override init(frame: CGRect) {
        // Call super constructor
        super.init(frame: frame)

        // Assign frame
        self.frame = frame

        // Set width to use 100% (relative)
        self.autoresizingMask = [.flexibleWidth]
    }

    // Propagate view update
    public override func layoutSubviews() {
        self.backgroundColor = self.bgColor
    }

    required public init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}
