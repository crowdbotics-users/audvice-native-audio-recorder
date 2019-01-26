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
    Handler mPlayerHandler = new Handler();
    String mOutputFile = null;

    private boolean mTouchDragging = false;
    private float mTouchStart = 0;
    private int mTouchInitialOffset = 0;
    private float mFlingVelocity = 0;

    private boolean mInitialized = false;
    private boolean mNeedProcessStop = false;

    private boolean onScrollWhenPlay = true;
    private int mPixelsPerSec = 100;

    Thread mRecordAudioThread;

    public RNAudioRecorderView(Context context) {
        super(context);
        addSubViews();
    }

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

    // property

    public void setOnScroll(boolean onScroll) {
        onScrollWhenPlay = onScroll;
    }

    public void setPixelsPerSecond(int pixelsPerSecond) {
        mPixelsPerSec = pixelsPerSecond;
    }

    public void setTimeTextColor(int color) {
        mWaveForm.setTimeTextColor(color);
    }

    public void setPlotLineColor(int color) {
        mWaveForm.setPlotLineColor(color);
    }

    public void setTimeTextSize(int size) {
        mWaveForm.setTimeTextSize(size);
    }

    public void initialize(String filename, int offsetInMs) {
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
        mOutputFile = filename;
        if (offsetInMs != 0) {
            try {
                mSoundFile = SoundFile.create(filename, mPixelsPerSec, offsetInMs, -1, soundProgressListener);
            } catch (Exception e) {
                e.printStackTrace();
                mSoundFile = null;
            }
        }

        if (mSoundFile == null) {
            mOutputFile = null;
            mSoundFile = SoundFile.createRecord(mPixelsPerSec, soundProgressListener);
        }

        mWaveForm.setSoundFile(mSoundFile);
        mWaveForm.invalidate();
    }

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
        mWaveForm.invalidate();
    }

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
        mWaveForm.invalidate();

        return stopRecording();
    }

    public void destroy() {
        releasePlayer();
        closeThread(mRecordAudioThread);
        mRecordAudioThread = null;
        mSoundFile = null;
    }

    private void closeThread(Thread thread) {
        if (thread != null && thread.isAlive()) {
            try {
                thread.join();
            } catch (InterruptedException e) {
            }
        }
    }

    public void startRecording() {
        // if mplayer, clear it.
        if (mSoundFile == null) return;
        releasePlayer();
        mNeedProcessStop = false;

        if (mRecordAudioThread != null) {
            mNeedProcessStop = true;
            return;
        }

        mSoundFile.truncateFile(mWaveForm.pixelsToMillisecs(mWaveForm.getOffset()));

        mRecordAudioThread = new Thread(){
            @Override
            public void run() {
                mSoundFile.RecordAudio(0);
                mNeedProcessStop = false;
                mRecordAudioThread = null;
            }
        };
        mRecordAudioThread.start();
    }

    String stopRecording() throws IOException {
        if (mSoundFile == null) return null;
        releasePlayer();
        mNeedProcessStop = true;

        mWaveForm.invalidate();

        String extension = mSoundFile.getFiletype();

        if (extension.equals("mp3") || extension.equals("m4a")) {
            File outFile = new File(mOutputFile);
            if (outFile.exists()) {
                outFile.delete();
            }
            mSoundFile.WriteFile(outFile);
            return mOutputFile;
        } else if (extension.equals("wav")) {
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

    public long getDuration() {
        if (mSoundFile == null) return 0;
        return (long)mSoundFile.getNumSamples() * 1000 / mSoundFile.getSampleRate();
    }

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

    public void play() {
        if (mSoundFile == null) return;
        if (mPlayer == null) {
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

    Runnable playRunnable = new Runnable() {
        @Override
        public void run() {
            if (mPlayer != null && mPlayer.isPlaying()) {
                int pixels = mWaveForm.millisecsToPixels(mPlayer.getCurrentPosition());
                if (pixels < mWaveForm.maxPos() - 5) {
                    if (onScrollWhenPlay)
                        mWaveForm.setOffset(pixels);
                    mPlayerHandler.postDelayed(playRunnable, 25);
                } else {
                    completedPlay();
                }
            }
        }
    };

    private synchronized void completedPlay() {
        if (onScrollWhenPlay)
            mWaveForm.setOffset(mWaveForm.maxPos());
        releasePlayer();
    }

    public String formatDuration(long diff) {
        int diffMilliseconds = (int) (diff % 1000);
        int diffSeconds = (int) (diff / 1000 % 60);
        int diffMinutes = (int) (diff / (60 * 1000) % 60);
        int diffHours = (int) (diff / (60 * 60 * 1000) % 24);
        int diffDays = (int) (diff / (24 * 60 * 60 * 1000));

        String str = "";

        if (diffDays > 0)
            str = diffDays + "days " + formatTime(diffHours) + ":" + formatTime(diffMinutes) + ":" + formatTime(diffSeconds);
        else if (diffHours > 0)
            str = formatTime(diffHours) + ":" + formatTime(diffMinutes) + ":" + formatTime(diffSeconds);
        else
            str = formatTime(diffMinutes) + ":" + formatTime(diffSeconds);

        return str;
    }

    public String formatTime(int tt) {
        return String.format("%02d", tt);
    }

    boolean hasPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
        } else {
            return true;
        }
    }

    public void setStatus(String status) {
        mTvStatus.setText(status);
    }

    private SoundFile.ProgressListener soundProgressListener = new SoundFile.ProgressListener() {
        @Override
        public boolean reportProgress(double fractionComplete) {
            if (mPlayer != null && mPlayer.isPlaying())
                messageHandler.obtainMessage(MessageHandler.MSG_UPDATE_WAVEFORM).sendToTarget();
            return !mNeedProcessStop;
        }
    };

    void updateWavrForm() {
        mWaveForm.updateRecording();
    }

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
                    updateWavrForm();
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
