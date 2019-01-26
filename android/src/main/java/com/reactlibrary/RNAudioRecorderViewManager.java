package com.reactlibrary;

import android.graphics.Color;
import android.widget.Toast;

import com.facebook.infer.annotation.Assertions;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.*;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;

import java.util.Map;

import javax.annotation.Nullable;

public class RNAudioRecorderViewManager extends ViewGroupManager<RNAudioRecorderView> {
    private static final String REACT_CLASS = "RNAudioRecorderView";

    // Commands
    public static final int COMMAND_INITIALIZE = 1;
    public static final int COMMAND_START_RECORDING = 2;

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Nullable
    @Override
    public Map<String, Integer> getCommandsMap() {
        return MapBuilder.of(
                "initialize",
                COMMAND_INITIALIZE,
                "startRecording",
                COMMAND_START_RECORDING
        );
    }

    @Override
    public void receiveCommand(RNAudioRecorderView root, int commandId, @Nullable ReadableArray args) {
        Assertions.assertNotNull(root);
        switch (commandId) {
            case COMMAND_INITIALIZE:
                root.setStatus("Native Initialize");
                break;
            case COMMAND_START_RECORDING:
                String filename = "";
                int offset = 0;
                if (args.size() == 2) {
                    filename = args.getString(0);
                    offset = args.getInt(1);
                }
                root.setStatus("Native Start Recording: " + filename + " - " + offset);
                break;
            default:
                throw new IllegalArgumentException(
                    String.format("Unsupported command %d received by %s.", commandId, getClass().getSimpleName()));
        }
    }

    @Override
    public RNAudioRecorderView createViewInstance(ThemedReactContext context) {
        return new RNAudioRecorderView(context);
    }

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
