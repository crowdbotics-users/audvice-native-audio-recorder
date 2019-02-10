package com.reactlibrary;

import android.graphics.Color;
import com.facebook.react.uimanager.*;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;

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

    // Properties from Java script.
    @ReactProp(name = "pixelsPerSecond")
    public void setPixelsPerSecond(RNAudioRecorderView view, int pixelsPerSecond) {
        view.setPixelsPerSecond(pixelsPerSecond);
    }

    @ReactProp(name = "plotLineColor")
    public void setPlotLineColor(RNAudioRecorderView view, String plotLineColor) {
        int color = Color.parseColor(plotLineColor);
        view.setPlotLineColor(color);
    }

    @ReactProp(name = "timeTextColor")
    public void setTimeTextColor(RNAudioRecorderView view, String timeTextColor) {
        int color = Color.parseColor(timeTextColor);
        view.setTimeTextColor(color);
    }

    @ReactProp(name = "timeTextSize")
    public void setTimeTextSize(RNAudioRecorderView view, int timeTextSize) {
        view.setTimeTextSize(timeTextSize);
    }

    @ReactProp(name = "onScroll")
    public void setOnScroll(RNAudioRecorderView view, boolean onScroll) {
        view.setOnScroll(onScroll);
    }
}
