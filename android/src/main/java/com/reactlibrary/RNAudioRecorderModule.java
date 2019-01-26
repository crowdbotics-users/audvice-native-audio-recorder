
package com.reactlibrary;

import android.view.View;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.NativeViewHierarchyManager;
import com.facebook.react.uimanager.UIBlock;
import com.facebook.react.uimanager.UIManagerModule;
import com.reactlibrary.recorder.SoundFile;

import java.io.FileNotFoundException;
import java.io.IOException;

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
    public void initialize(final int viewId, final String filename, final int offset) {
        UIManagerModule uiManager = getReactApplicationContext().getNativeModule(UIManagerModule.class);
        uiManager.addUIBlock(new UIBlock() {
            @Override
            public void execute(NativeViewHierarchyManager nativeViewHierarchyManager) {
                View view = nativeViewHierarchyManager.resolveView(viewId);
                if (view instanceof RNAudioRecorderView) {
                    RNAudioRecorderView audioRecorderView = (RNAudioRecorderView)view;
                    audioRecorderView.initialize(filename, offset);
                }
            }
        });
    }

    @ReactMethod
    public void renderByFile(final int viewId, final String filename, final Promise promise) {
        UIManagerModule uiManager = getReactApplicationContext().getNativeModule(UIManagerModule.class);
        uiManager.addUIBlock(new UIBlock() {
            @Override
            public void execute(NativeViewHierarchyManager nativeViewHierarchyManager) {
                View view = nativeViewHierarchyManager.resolveView(viewId);
                if (view instanceof RNAudioRecorderView) {
                    RNAudioRecorderView audioRecorderView = (RNAudioRecorderView)view;
                    try {
                        audioRecorderView.renderByFile(filename);
                        promise.resolve("success");
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        promise.reject("FileNotFound", "Doesn't exist audio file");
                    } catch (IOException e) {
                        e.printStackTrace();
                        promise.reject("InvalidFile", e.getCause());
                    } catch (SoundFile.InvalidInputException e) {
                        e.printStackTrace();
                        promise.reject("InvalidFile", e.getCause());
                    }
                } else {
                    promise.reject("ViewNotFound", "Cannot Find View");
                }
            }
        });
    }

    @ReactMethod
    public void destroy(final int viewId, final Promise promise) {
        UIManagerModule uiManager = getReactApplicationContext().getNativeModule(UIManagerModule.class);
        uiManager.addUIBlock(new UIBlock() {
            @Override
            public void execute(NativeViewHierarchyManager nativeViewHierarchyManager) {
                View view = nativeViewHierarchyManager.resolveView(viewId);
                if (view instanceof RNAudioRecorderView) {
                    RNAudioRecorderView audioRecorderView = (RNAudioRecorderView)view;
                    audioRecorderView.destroy();
                    promise.resolve("success");
                }else {
                    promise.reject("ViewNotFound", "Cannot Find View");
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
                    String output = null;
                    try {
                        output = audioRecorderView.stopRecording();
                        long duration = audioRecorderView.getDuration();
                        if (output == null) {
                            promise.reject("SaveError", "File Not Found!");
                        }else {
                            WritableMap map = Arguments.createMap();
                            map.putString("filepath", output);
                            map.putDouble("duration", duration);
                            promise.resolve(map);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        promise.reject("SaveError", e.getCause());
                    }
                } else {
                    promise.reject("error", "Not found view");
                }
            }
        });
    }

    @ReactMethod
    public void startRecording(final int viewId) {
        UIManagerModule uiManager = getReactApplicationContext().getNativeModule(UIManagerModule.class);
        uiManager.addUIBlock(new UIBlock() {
            @Override
            public void execute(NativeViewHierarchyManager nativeViewHierarchyManager) {
                View view = nativeViewHierarchyManager.resolveView(viewId);
                if (view instanceof RNAudioRecorderView) {
                    RNAudioRecorderView audioRecorderView = (RNAudioRecorderView)view;
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
    public void cut(final int viewId, final String filename, final int fromTime, final int toTime, final Promise promise) {
        UIManagerModule uiManager = getReactApplicationContext().getNativeModule(UIManagerModule.class);
        uiManager.addUIBlock(new UIBlock() {
            @Override
            public void execute(NativeViewHierarchyManager nativeViewHierarchyManager) {
                View view = nativeViewHierarchyManager.resolveView(viewId);
                if (view instanceof RNAudioRecorderView) {
                    try {
                        RNAudioRecorderView audioRecorderView = (RNAudioRecorderView)view;
                        String output = audioRecorderView.cut(filename, fromTime, toTime);
                        long duration = audioRecorderView.getDuration();
                        if (output == null) {
                            promise.reject("SaveError", "File Not Found!");
                        }else {
                            WritableMap map = Arguments.createMap();
                            map.putString("filepath", output);
                            map.putDouble("duration", duration);
                            promise.resolve(map);
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        promise.reject("FileNotFound", "Doesn't exist audio file");
                    } catch (IOException e) {
                        e.printStackTrace();
                        promise.reject("InvalidFile", e.getCause());
                    } catch (SoundFile.InvalidInputException e) {
                        e.printStackTrace();
                        promise.reject("InvalidFile", e.getCause());
                    } catch (RNAudioRecorderView.InvalidParamException e) {
                        e.printStackTrace();
                        promise.reject("InvalidParam", e.getCause());
                    }
                } else {
                    promise.reject("ViewNotFound", "Cannot Find View");
                }
            }
        });
    }
}