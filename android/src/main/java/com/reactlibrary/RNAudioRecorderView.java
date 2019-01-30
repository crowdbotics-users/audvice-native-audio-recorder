package com.reactlibrary;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.reactlibrary.recorder.SamplePlayer;
import com.reactlibrary.recorder.SoundFile;
import com.reactlibrary.recorder.WaveformView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RNAudioRecorderView extends RelativeLayout {

    public static final SimpleDateFormat ISO8601 = new SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.US);

    TextView mTvStatus;
    WaveformView mWaveForm;

    SoundFile mSoundFile;
    SamplePlayer mPlayer = null;
    String mOutputFile = null;
    Handler mPlayerHandler = new Handler();

    // Gesture related properties used on listener of WaveformView
    private float mTouchStart = 0;
    private int mTouchInitialOffset = 0;
    private boolean mNeedProcessStop = false;// The flag to cancel the action of sound file.
    private boolean mTouchDragging;
    private int mFlingVelocity;

    // Properties from JavaScript, other properties applied directly into WaveformView
    private boolean onScrollWhenPlay = true;
    private int mPixelsPerSec = 100;

    // The Thread for Audio Recording.
    // It should be existed only while recording.
    Thread mRecordAudioThread;

    public RNAudioRecorderView(Context context) {
        super(context);
        addSubViews();
    }

    // Create/Add Waveform and status view.
    void addSubViews() {
        LayoutParams params1 = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        mWaveForm = new WaveformView(getContext());
        this.addView(mWaveForm, params1);
        DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
        mWaveForm.setDensity(metrics.density);
        mWaveForm.setListener(waveformListener);

        mTvStatus = new TextView(getContext());
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        mTvStatus.setGravity(Gravity.CENTER);
        mTvStatus.setBackgroundColor(Color.WHITE);
        mTvStatus.setVisibility(View.GONE);
        this.addView(mTvStatus, params);
    }

    // setter onScroll property from Java Script
    public void setOnScroll(boolean onScroll) {
        onScrollWhenPlay = onScroll;
    }

    // setter pixelsPerSecond property from Java Script
    public void setPixelsPerSecond(int pixelsPerSecond) {
        mPixelsPerSec = pixelsPerSecond;
    }

    // setter timeTextColor property from Java Script
    public void setTimeTextColor(int color) {
        mWaveForm.setTimeTextColor(color);
    }

    // setter plotLineColor property from Java Script
    public void setPlotLineColor(int color) {
        mWaveForm.setPlotLineColor(color);
    }

    // setter timeTextSize property from Java Script
    public void setTimeTextSize(int size) {
        mWaveForm.setTimeTextSize(size);
    }

    // Some Initialize Functions.
    // There are 3 Initialize Functions, initialize(filepath, offsetInMs), renderByFile(filepath), cut(filepath, startTimeInMs, endTimeInMs)
    // open file is support following formats
    // "mp3", "wav", "3gpp", "3gp", "amr", "aac", "m4a", "ogg"
    // saved only "wav"
    //
    // initialize(filepath, offsetInMs)

    public void initialize(String filepath, int offsetInMs) {
        // start initialize for record
        // check the audio record permission
        if (!hasPermissions()) {
            mTvStatus.setText("Has No Enough Permission, Please enable permission and try again.");
            mTvStatus.setVisibility(View.VISIBLE);
            mWaveForm.setVisibility(View.GONE);
            return;
        }

        // init
        mNeedProcessStop = false;
        if (mSoundFile != null) {
            mSoundFile = null;
        }

        // create sound file
        mOutputFile = filepath;
        if (offsetInMs != 0) {
            try {
                mSoundFile = SoundFile.create(filepath, mPixelsPerSec, offsetInMs, -1, soundProgressListener);
            } catch (Exception e) {
                e.printStackTrace();
                mSoundFile = null;
            }
        }

        // if fail, create new file for recording.
        if (mSoundFile == null) {
            mOutputFile = null;
            mSoundFile = SoundFile.createRecord(mPixelsPerSec, soundProgressListener);
        }

        mWaveForm.setSoundFile(mSoundFile);
        mWaveForm.setPlaySeek(0);
    }

    // init with filename, if file not exist, return fail with exception.
    public void renderByFile(String filename) throws IOException,
            FileNotFoundException,
            SoundFile.InvalidInputException
    {
        // start initialize for record
        // check the audio record permission
        if (!hasPermissions()) {
            mTvStatus.setText("Has No Enough Permission, Please enable permission and try again.");
            mTvStatus.setVisibility(View.VISIBLE);
            mWaveForm.setVisibility(View.GONE);
            return;
        }

        mNeedProcessStop = false;
        mOutputFile = filename;
        // init
        if (mSoundFile != null) {
            mSoundFile = null;
        }

        // create sound file
        mSoundFile = SoundFile.create(filename, mPixelsPerSec, -1, -1, soundProgressListener);

        if (mSoundFile == null) {
            throw new FileNotFoundException();
        }

        mWaveForm.setSoundFile(mSoundFile);
        mWaveForm.setPlaySeek(0);
    }

    // init cut file with filename, if file not exist, return fail with exception.
    public String cut(String filename, final int fromTime, final int toTime) throws IOException,
            FileNotFoundException,
            SoundFile.InvalidInputException,
            InvalidParamException
    {
        // start initialize for record
        // check the audio record permission
        if (!hasPermissions()) {
            mTvStatus.setText("Has No Enough Permission, Please enable permission and try again.");
            mTvStatus.setVisibility(View.VISIBLE);
            mWaveForm.setVisibility(View.GONE);
            return null;
        }

        mNeedProcessStop = false;

        mOutputFile = filename;

        // init
        if (mSoundFile != null) {
            mSoundFile = null;
        }

        // create sound file
        if (fromTime == -1 &&
                toTime == -1) {
            throw new InvalidParamException("Invalid Parameter is used");
        }

        if (fromTime != -1 &&
                toTime != -1 &&
                fromTime >= toTime) {
            throw new InvalidParamException("Invalid Parameter is used");
        }

        mSoundFile = SoundFile.create(filename, mPixelsPerSec, fromTime, toTime, soundProgressListener);

        if (mSoundFile == null) {
            throw new FileNotFoundException();
        }

        if (mSoundFile.getNumSamples() == 0) {
            throw new InvalidParamException("Invalid Parameter is used");
        }

        mWaveForm.setSoundFile(mSoundFile);
        mWaveForm.setPlaySeek(0);

        return stopRecording();
    }

    // destroy function, invoked from JavaScript
    // release player and close recording
    public void destroy() {
        releasePlayer();
        closeThread(mRecordAudioThread);
        mRecordAudioThread = null;
        mSoundFile = null;
    }

    // Start Recording, invoked from JavaScript
    // Requirement: initialized. It should be checked on Javascript.    
    public void startRecording() {
        // if no Sound File, clear it.
        if (mSoundFile == null) return;

        // If play, release it.
        releasePlayer();
        mNeedProcessStop = false;

        // If Recording, pause it.
        if (mRecordAudioThread != null) {
            mNeedProcessStop = true;
            return;
        }

        // if the position isn't on end of file, the sound should be truncated and continue the recording.
        // mSoundFile.truncateFile(mWaveForm.pixelsToMillisecs(mWaveForm.getOffset()));
        final long offsetInMs = mWaveForm.pixelsToMillisecs(mWaveForm.getOffset());

        // Create Audio Recording Thread
        mRecordAudioThread = new Thread(){
            @Override
            public void run() {
                mSoundFile.RecordAudio(offsetInMs);
                mNeedProcessStop = false;
                mRecordAudioThread = null;
            }
        };
        mRecordAudioThread.start();
    }

    // Stop Recording and return the path of recorded audio file
    String stopRecording() throws IOException {
        // if no sound file, return null
        if (mSoundFile == null) return null;

        // release player
        releasePlayer();
        mNeedProcessStop = true;

        mWaveForm.invalidate();

        String extension = mSoundFile.getFiletype();

        // if source is wav file, overwrite it,
        // if not, create new wav file.
        if (extension.equals("wav")) {
            File outFile = new File(mOutputFile);
            if (outFile.exists()) {
                outFile.delete();
            }
            mSoundFile.WriteWAVFile(outFile);
            return mOutputFile;
        } else {
            extension = "wav";
            mOutputFile = makeRandomFilePath(extension);
            mSoundFile.WriteWAVFile(new File(mOutputFile));
            return mOutputFile;
        }
    }

    // return duration of audio file
    public long getDuration() {
        if (mSoundFile == null) return 0;
        return (long)mSoundFile.getNumSamples() * 1000 / mSoundFile.getSampleRate();
    }

    // play/pause audio file
    public void play() {
        if (mSoundFile == null) return;
        if (mPlayer == null) {

            // Create Player
            mPlayer = new SamplePlayer(mSoundFile);
            mPlayer.setOnCompletionListener(new SamplePlayer.OnCompletionListener() {

                @Override
                public void onCompletion() {
                    completedPlay();
                }
            });
        }

        if (mPlayer.isPlaying()) {
            mPlayer.pause();
            mPlayerHandler.removeCallbacks(playRunnable);
        }else{
            mPlayer.seekTo(mWaveForm.pixelsToMillisecs(mWaveForm.getOffset()));
            mPlayerHandler.postDelayed(playRunnable, 100);
            mPlayer.start();
        }
    }

    // Play Runnable to update waveform
    Runnable playRunnable = new Runnable() {
        @Override
        public void run() {
            if (mPlayer != null && mPlayer.isPlaying()) {
                int pixels = mWaveForm.millisecsToPixels(mPlayer.getCurrentPosition());
                if (pixels < mWaveForm.maxPos() - 5) {
                    if (onScrollWhenPlay)
                        mWaveForm.setOffset(pixels);

                    // Update state per 25 Ms
                    mPlayerHandler.postDelayed(playRunnable, 25);
                } else {
                    completedPlay();
                }
            }
        }
    };

    // close audio recording thread
    private void closeThread(Thread thread) {
        if (thread != null && thread.isAlive()) {
            try {
                thread.join();
            } catch (InterruptedException e) {
            }
        }
    }

    // release player
    private void releasePlayer() {
        if (mPlayer != null) {
            if (mPlayer.isPlaying()) {
                mPlayer.stop();
            }
            mPlayer.release();
            mPlayer = null;
            mPlayerHandler.removeCallbacks(playRunnable);
        }
    }

    // this function is called, when the playing is finished.
    private synchronized void completedPlay() {
        if (onScrollWhenPlay)
            mWaveForm.setOffset(mWaveForm.maxPos());
        releasePlayer();
    }

    // check permission
    boolean hasPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
        } else {
            return true;
        }
    }

    // while recording, this listener should be called, send the message to waveform to update itself.
    private SoundFile.ProgressListener soundProgressListener = new SoundFile.ProgressListener() {
        @Override
        public boolean reportProgress(double fractionComplete) {
            messageHandler.obtainMessage(MessageHandler.MSG_UPDATE_WAVEFORM, fractionComplete).sendToTarget();
            return !mNeedProcessStop;
        }
    };

    // update waveform
    void updateWaveForm(double posInS) {
        mWaveForm.setPlaySeek((long)(posInS * 1000));
    }

    // Gesture Listener from Wave Form
    private WaveformView.WaveformListener waveformListener = new WaveformView.WaveformListener() {
        @Override
        public void waveformTouchStart(float x) {
            mTouchDragging = true;
            mTouchStart = x;
            mTouchInitialOffset = mWaveForm.getOffset();
            mFlingVelocity = 0;
        }

        @Override
        public void waveformTouchMove(float x) {
            mWaveForm.setOffset(mTouchInitialOffset + (int)(mTouchStart - x));
        }

        @Override
        public void waveformTouchEnd() {
            mTouchDragging = false;
        }

        @Override
        public void waveformFling(float x) {
        }

        @Override
        public void waveformDraw() {

        }
    };

    // Create Random file on sdcar/audiorecorder/[DATE].wav
    public static String makeRandomFilePath(String extension) {
        String externalRootDir = Environment.getExternalStorageDirectory().getPath();
        if (!externalRootDir.endsWith("/")) {
            externalRootDir += "/";
        }
        String parentdir = externalRootDir + "audiorecorder/";

        // Create the parent directory
        File parentDirFile = new File(parentdir);
        parentDirFile.mkdirs();

        // If we can't write to that special path, try just writing
        // directly to the sdcard
        if (!parentDirFile.isDirectory()) {
            parentdir = externalRootDir;
        }

        String filename = ISO8601.format(new Date()) + "." + extension;
        return parentdir + filename;
    }

    private  MessageHandler messageHandler = new MessageHandler();

    @SuppressLint("HandlerLeak")
    class MessageHandler extends Handler {
        static final int MSG_UPDATE_WAVEFORM = 0;
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_WAVEFORM:
                {
                    updateWaveForm((double)msg.obj);
                }
            }
        }
    }

    public static class InvalidParamException extends Exception {
        // Serial version ID generated by Eclipse.
        public InvalidParamException(String message) {
            super(message);
        }
    }
}
