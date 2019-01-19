
package com.reactlibrary;

import android.view.View;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.uimanager.NativeViewHierarchyManager;
import com.facebook.react.uimanager.UIBlock;
import com.facebook.react.uimanager.UIManagerModule;

public class RNAudioRecorderModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;

    public RNAudioRecorderModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "RNAudioRecorder";
    }

    @ReactMethod
    public void initialize(final int viewId) {
        UIManagerModule uiManager = getReactApplicationContext().getNativeModule(UIManagerModule.class);
        uiManager.addUIBlock(new UIBlock() {
            @Override
            public void execute(NativeViewHierarchyManager nativeViewHierarchyManager) {
                View view = nativeViewHierarchyManager.resolveView(viewId);
                if (view instanceof RNAudioRecorderView) {
                    RNAudioRecorderView audioRecorderView = (RNAudioRecorderView)view;
                    audioRecorderView.initialize();
                }
            }
        });
    }

    @ReactMethod
    public void destroy(final int viewId) {
        UIManagerModule uiManager = getReactApplicationContext().getNativeModule(UIManagerModule.class);
        uiManager.addUIBlock(new UIBlock() {
            @Override
            public void execute(NativeViewHierarchyManager nativeViewHierarchyManager) {
                View view = nativeViewHierarchyManager.resolveView(viewId);
                if (view instanceof RNAudioRecorderView) {
                    RNAudioRecorderView audioRecorderView = (RNAudioRecorderView)view;
                    audioRecorderView.setStatus("Native destroy");
                }
            }
        });
    }

    @ReactMethod
    public void stopRecording(final int viewId, final Promise promise) {
        UIManagerModule uiManager = getReactApplicationContext().getNativeModule(UIManagerModule.class);
        uiManager.addUIBlock(new UIBlock() {
            @Override
            public void execute(NativeViewHierarchyManager nativeViewHierarchyManager) {
                View view = nativeViewHierarchyManager.resolveView(viewId);
                if (view instanceof RNAudioRecorderView) {
                    RNAudioRecorderView audioRecorderView = (RNAudioRecorderView)view;
                    audioRecorderView.stopRecording("pause");
                    promise.resolve("filename");
                } else {
                    promise.reject("error", "Not found view");
                }
            }
        });
    }

    @ReactMethod
    public void startRecording(final int viewId, final String filename, final int offset) {
        UIManagerModule uiManager = getReactApplicationContext().getNativeModule(UIManagerModule.class);
        uiManager.addUIBlock(new UIBlock() {
            @Override
            public void execute(NativeViewHierarchyManager nativeViewHierarchyManager) {
                View view = nativeViewHierarchyManager.resolveView(viewId);
                if (view instanceof RNAudioRecorderView) {
                    RNAudioRecorderView audioRecorderView = (RNAudioRecorderView)view;
                    // TODO: add file and offset
                    audioRecorderView.startRecording();
                }
            }
        });
    }

    @ReactMethod
    public void play(final int viewId) {
        UIManagerModule uiManager = getReactApplicationContext().getNativeModule(UIManagerModule.class);
        uiManager.addUIBlock(new UIBlock() {
            @Override
            public void execute(NativeViewHierarchyManager nativeViewHierarchyManager) {
                View view = nativeViewHierarchyManager.resolveView(viewId);
                if (view instanceof RNAudioRecorderView) {
                    RNAudioRecorderView audioRecorderView = (RNAudioRecorderView)view;
                    audioRecorderView.play();
                }
            }
        });
    }

    @ReactMethod
    public void renderByFile(final int viewId, final String filename) {
        UIManagerModule uiManager = getReactApplicationContext().getNativeModule(UIManagerModule.class);
        uiManager.addUIBlock(new UIBlock() {
            @Override
            public void execute(NativeViewHierarchyManager nativeViewHierarchyManager) {
                View view = nativeViewHierarchyManager.resolveView(viewId);
                if (view instanceof RNAudioRecorderView) {
                    RNAudioRecorderView audioRecorderView = (RNAudioRecorderView)view;
                    audioRecorderView.setStatus("Native Render File: " + filename);
                }
            }
        });
    }

    @ReactMethod
    public void cut(final int viewId, final String filename, final int fromTime, final int toTime) {
        UIManagerModule uiManager = getReactApplicationContext().getNativeModule(UIManagerModule.class);
        uiManager.addUIBlock(new UIBlock() {
            @Override
            public void execute(NativeViewHierarchyManager nativeViewHierarchyManager) {
                View view = nativeViewHierarchyManager.resolveView(viewId);
                if (view instanceof RNAudioRecorderView) {
                    RNAudioRecorderView audioRecorderView = (RNAudioRecorderView)view;
                    audioRecorderView.setStatus("Native cut: " + filename + " - " + fromTime + "-" + toTime);
                }
            }
        });
    }
}