
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
    public void initialize(final int viewId, final String filename, final int offset, final Promise promise) {
        UIManagerModule uiManager = getReactApplicationContext().getNativeModule(UIManagerModule.class);
        uiManager.addUIBlock(new UIBlock() {
            @Override
            public void execute(NativeViewHierarchyManager nativeViewHierarchyManager) {
                View view = nativeViewHierarchyManager.resolveView(viewId);
                if (view instanceof RNAudioRecorderView) {
                    RNAudioRecorderView audioRecorderView = (RNAudioRecorderView)view;
                    audioRecorderView.initialize(filename, offset);
                    promise.resolve("success");
                } else {
                    promise.reject("ViewNotFound", "Cannot Find View");
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
                        promise.reject("InvalidFile", e.getMessage());
                    } catch (SoundFile.InvalidInputException e) {
                        e.printStackTrace();
                        promise.reject("InvalidFile", e.getMessage());
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
                            promise.reject("InitError", "Before stop recording, please call initialize");
                        }else {
                            WritableMap map = Arguments.createMap();
                            map.putString("filepath", output);
                            map.putDouble("duration", duration);
                            promise.resolve(map);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        promise.reject("SaveError", e.getMessage());
                    }
                } else {
                    promise.reject("error", "Not found view");
                }
            }
        });
    }

    @ReactMethod
    public void startRecording(final int viewId, final Promise promise) {
        UIManagerModule uiManager = getReactApplicationContext().getNativeModule(UIManagerModule.class);
        uiManager.addUIBlock(new UIBlock() {
            @Override
            public void execute(NativeViewHierarchyManager nativeViewHierarchyManager) {
                View view = nativeViewHierarchyManager.resolveView(viewId);
                if (view instanceof RNAudioRecorderView) {
                    RNAudioRecorderView audioRecorderView = (RNAudioRecorderView)view;
                    if (audioRecorderView.startRecording()) {
                        promise.resolve("success");
                    } else {
                        promise.reject("InitError", "Before start recording, please call initialize");
                    }
                } else {
                    promise.reject("ViewNotFound", "Cannot Find View");
                }
            }
        });
    }

    @ReactMethod
    public void play(final int viewId, final Promise promise) {
        UIManagerModule uiManager = getReactApplicationContext().getNativeModule(UIManagerModule.class);
        uiManager.addUIBlock(new UIBlock() {
            @Override
            public void execute(NativeViewHierarchyManager nativeViewHierarchyManager) {
                View view = nativeViewHierarchyManager.resolveView(viewId);
                if (view instanceof RNAudioRecorderView) {
                    RNAudioRecorderView audioRecorderView = (RNAudioRecorderView)view;
                    if (audioRecorderView.play()) {
                        promise.resolve("success");
                    } else {
                        promise.reject("InitError", "Before play recording, please call initialize");
                    }
                } else {
                    promise.reject("ViewNotFound", "Cannot Find View");
                }
            }
        });
    }

    @ReactMethod
    public void pause(final int viewId, final Promise promise) {
        UIManagerModule uiManager = getReactApplicationContext().getNativeModule(UIManagerModule.class);
        uiManager.addUIBlock(new UIBlock() {
            @Override
            public void execute(NativeViewHierarchyManager nativeViewHierarchyManager) {
                View view = nativeViewHierarchyManager.resolveView(viewId);
                if (view instanceof RNAudioRecorderView) {
                    RNAudioRecorderView audioRecorderView = (RNAudioRecorderView)view;
                    if (audioRecorderView.pause()) {
                        long positionInMs = audioRecorderView.getPosition();
                        WritableMap map = Arguments.createMap();
                        map.putDouble("position", positionInMs);
                        promise.resolve(map);
                    } else {
                        promise.reject("InitError", "Before pause recording, please call initialize");
                    }
                } else {
                    promise.reject("ViewNotFound", "Cannot Find View");
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
                        promise.reject("InvalidFile", e.getMessage());
                    } catch (SoundFile.InvalidInputException e) {
                        e.printStackTrace();
                        promise.reject("InvalidFile", e.getMessage());
                    } catch (RNAudioRecorderView.InvalidParamException e) {
                        e.printStackTrace();
                        promise.reject("InvalidParam", e.getMessage());
                    }
                } else {
                    promise.reject("ViewNotFound", "Cannot Find View");
                }
            }
        });
    }

    @ReactMethod
    public void compress(final int viewId, final String filename, final Promise promise) {
        UIManagerModule uiManager = getReactApplicationContext().getNativeModule(UIManagerModule.class);
        uiManager.addUIBlock(new UIBlock() {
            @Override
            public void execute(NativeViewHierarchyManager nativeViewHierarchyManager) {
                View view = nativeViewHierarchyManager.resolveView(viewId);
                if (view instanceof RNAudioRecorderView) {
                    try {
                        RNAudioRecorderView audioRecorderView = (RNAudioRecorderView)view;
                        String output = audioRecorderView.compress(filename);
                        if (output == null) {
                            promise.reject("InvalidFile", "Input file is invalid!");
                        }else {
                            WritableMap map = Arguments.createMap();
                            map.putString("filepath", output);
                            promise.resolve(map);
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        promise.reject("FileNotFound", "Doesn't exist audio file");
                    } catch (IOException e) {
                        e.printStackTrace();
                        promise.reject("InvalidFile", e.getMessage());
                    } catch (SoundFile.InvalidInputException e) {
                        e.printStackTrace();
                        promise.reject("InvalidFile", e.getMessage());
                    } catch (RNAudioRecorderView.InvalidParamException e) {
                        e.printStackTrace();
                        promise.reject("InvalidParam", e.getMessage());
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                        promise.reject("FailConversion", e.getMessage());
                    }
                } else {
                    promise.reject("ViewNotFound", "Cannot Find View");
                }
            }
        });
    }
}