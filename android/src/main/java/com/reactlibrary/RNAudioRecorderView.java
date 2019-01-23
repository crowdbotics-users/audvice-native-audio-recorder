package com.reactlibrary;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
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

public class RNAudioRecorderView extends RelativeLayout {

    TextView mTvStatus;
    WaveformView mWaveForm;

    SoundFile mSoundFile;
    SamplePlayer mPlayer = null;
    Handler mPlayerHandler = new Handler();

    private boolean mTouchDragging = false;
    private float mTouchStart = 0;
    private int mTouchInitialOffset = 0;
    private float mFlingVelocity = 0;

    private boolean mInitialized = false;
    private boolean mNeedProcessStop = false;

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
        if (mSoundFile != null) {
            mSoundFile = null;
        }

        // create sound file
        try {
            mSoundFile = SoundFile.create(filename, 100, soundProgressListener);

        } catch (Exception e) {
            e.printStackTrace();
            mSoundFile = null;
        }

        if (mSoundFile == null) {
            mSoundFile = SoundFile.createRecord(100, soundProgressListener);
        }

        mWaveForm.setSoundFile(mSoundFile);
        mWaveForm.invalidate();
    }

    public void destroy() {
        releasePlayer();
        closeThread(mRecordAudioThread);
        mRecordAudioThread = null;
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
        releasePlayer();
        mNeedProcessStop = false;

        mRecordAudioThread = new Thread(){
            @Override
            public void run() {
                mSoundFile.RecordAudio(0);
                mNeedProcessStop = true;
            }
        };
        mRecordAudioThread.start();
    }

    void stopRecording() {
        releasePlayer();
        mNeedProcessStop = true;
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
            mPlayerHandler.postDelayed(playRunnable, 50);
            mPlayer.start();
        }
    }

    Runnable playRunnable = new Runnable() {
        @Override
        public void run() {
            if (mPlayer != null && mPlayer.isPlaying()) {
                int pixels = mWaveForm.millisecsToPixels(mPlayer.getCurrentPosition());
                if (pixels < mWaveForm.maxPos() && pixels > mWaveForm.getOffset()) {
                    mWaveForm.setOffset(pixels);
                    mPlayerHandler.postDelayed(playRunnable, 50);
                } else {
                    completedPlay();
                }
            }
        }
    };

    private synchronized void completedPlay() {
        releasePlayer();
        mWaveForm.setOffset(0);
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
}
