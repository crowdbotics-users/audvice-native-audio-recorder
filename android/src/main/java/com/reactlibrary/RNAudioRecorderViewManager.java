package com.reactlibrary;

import com.facebook.react.uimanager.*;
import com.facebook.react.uimanager.ThemedReactContext;

public class RNAudioRecorderViewManager extends ViewGroupManager<RNAudioRecorderView> {
    private static final String REACT_CLASS = "RNAudioRecorderView";

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    public RNAudioRecorderView createViewInstance(ThemedReactContext context) {
        return new RNAudioRecorderView(context);
    }
}
