package com.reactlibrary;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaCodec;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.axet.androidlibrary.sound.AudioTrack;
import com.reactlibrary.recorder.AudioRecorder;
import com.reactlibrary.recorder.MediaDecoder;
import com.reactlibrary.recorder.RawSamples;
import com.reactlibrary.recorder.RecordConfig;
import com.reactlibrary.recorder.Sound;
import com.reactlibrary.recorder.SoundFile;
import com.reactlibrary.recorder.WaveformView;
import com.reactlibrary.recorder.base.Storage;

import java.io.File;
import java.io.IOException;
import java.nio.ShortBuffer;

import static android.content.ContentValues.TAG;

public class RNAudioRecorderView extends RelativeLayout {

    TextView mTvStatus;
    WaveformView mWaveForm;

    RecordConfig config;
    AudioRecorder recording;

    private long editSample;
    private AudioTrack play;
    private boolean mTouchDragging = false;
    private float mTouchStart = 0;
    private int mTouchInitialOffset = 0;
    private float mFlingVelocity = 0;
    private long mPlayStart = 0;

    private boolean mInitialized = false;

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
        mWaveForm.setListener(new WaveformView.WaveformListener() {
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
        });

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
        config = new RecordConfig();

        if (recording != null)
            destroy();

        mWaveForm.clearData();

        filename ="/sdcard/Android/media/com.google.android.talk/Ringtones/hangouts_incoming_call.ogg";

        try {
            SoundFile sf = SoundFile.create(filename, new SoundFile.ProgressListener() {
                @Override
                public boolean reportProgress(double fractionComplete) {
                    return true;
                }
            });
            config.sampleRate = sf.getSampleRate();
            config.channels = sf.getChannels();
            recording = new AudioRecorder(getContext(), config);
            recording.storage.getTempRecording().delete();
            RawSamples rs = new RawSamples(recording.storage.getTempRecording());
            sf.writeRawToTemp(rs);
        } catch (Exception e) {
            e.printStackTrace();
            config.sampleRate = Sound.getSampleRate(getContext());
            config.channels = Sound.getChannels(getContext());
            recording = new AudioRecorder(getContext(), config);
            recording.storage.getTempRecording().delete();
        }


//        MediaDecoder decoder = new MediaDecoder(filename);
//        if (decoder.isInitialized) {
//            config.sampleRate = decoder.getSampleRate();
//            recording = new AudioRecorder(getContext(), config);
//            recording.storage.getTempRecording().delete();
//
//            short[] data;
//            RawSamples rs = new RawSamples(recording.storage.getTempRecording());
//            decoder.writeRawToTemp(rs);
//
//            rs.close();
//        }else{
//            config.sampleRate = Sound.getSampleRate(getContext());
//            recording = new AudioRecorder(getContext(), config);
//            recording.storage.getTempRecording().delete();
//        }

        synchronized (recording.handlers) {
            recording.handlers.add(handler);
        }

        recording.updateBufferSize(false);

        mWaveForm.setConfig(recording.sampleRate, 100);

        loadSamples();
    }

    public void destroy() {
        recording = null;
    }

    void loadSamples() {
        File f = recording.storage.getTempRecording();
        if (!f.exists()) {
            recording.samplesTime = 0;
            updateSamples(recording.samplesTime);
            return;
        }

        RawSamples rs = new RawSamples(f);
        recording.samplesTime = rs.getSamples() / Sound.getChannels(getContext());

        mWaveForm.setSoundFile(rs);
        mWaveForm.invalidate();
    }

    public void startRecording() {

        if (recording.thread != null) {
            stopRecording();
            return;
        }

        try {
            // edit cut
            RawSamples rs = new RawSamples(recording.storage.getTempRecording());
            editSample = mWaveForm.getCurrentPosInMs() * recording.sampleRate / 1000;
            if (rs.getSamples() > editSample && editSample >= 0) {
                if (editSample == 0) {
                    rs.close();
                    recording.storage.getTempRecording().delete();
                }else {
                    rs.trunk((editSample + recording.samplesUpdate) * Sound.getChannels(getContext()));
                    rs.close();
                }
                loadSamples();
            }

            recording.startRecording();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    void stopRecording() {
        if (recording != null) // not possible, but some devices do not call onCreate
            recording.stopRecording();
    }

    public void play() {
        if (play != null) {
            editPlay(false);
        } else {
            editPlay(true);
        }
    }

    void editPlay(boolean show) {

        if (show) {

            RawSamples rs = new RawSamples(recording.storage.getTempRecording());
            editSample = mWaveForm.getCurrentPosInMs() * recording.sampleRate / 1000;
            int len = (int) (rs.getSamples() - editSample * Sound.getChannels(getContext())); // in samples
            if (len <= 0) return;

            final AudioTrack.OnPlaybackPositionUpdateListener listener = new AudioTrack.OnPlaybackPositionUpdateListener() {
                @Override
                public void onMarkerReached(android.media.AudioTrack track) {
                    editPlay(false);
                    mWaveForm.seekLastPos();
                }

                @Override
                public void onPeriodicNotification(android.media.AudioTrack track) {
                    if (play != null) {
                        long now = System.currentTimeMillis() + editSample * 1000 / recording.sampleRate;
                        mWaveForm.setPlaySeek(now - mPlayStart);
                    }
                }
            };

            AudioTrack.AudioBuffer buf = new AudioTrack.AudioBuffer(recording.sampleRate, Sound.getOutMode(getContext()), Sound.DEFAULT_AUDIOFORMAT, len);
            rs.open(editSample * Sound.getChannels(getContext()), buf.len); // len in samples
            int r = rs.read(buf.buffer); // r in samples
            if (r != buf.len)
                throw new RuntimeException("unable to read data");
            mWaveForm.setAutoSeeking(true);
            int last = buf.len / buf.getChannels() - 1;
            if (play != null)
                play.release();

            play = AudioTrack.create(Sound.SOUND_STREAM, Sound.SOUND_CHANNEL, Sound.SOUND_TYPE, buf);
            play.setNotificationMarkerPosition(last);
            play.setPositionNotificationPeriod(100);
            play.setPlaybackPositionUpdateListener(listener, handler);
            mPlayStart = System.currentTimeMillis();
            play.play();

        } else {
            if (play != null) {
                play.release();
                play = null;
                mWaveForm.setAutoSeeking(false);
            }
        }
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

    public void Error(Throwable e) {
        Log.e(TAG, "error", e);
        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
    }

    void updateSamples(long samplesTime) {
        long ms = samplesTime / recording.sampleRate * 1000;
        String duration = formatDuration(ms);

        // TODO: duration string
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == AudioRecorder.PINCH)
                mWaveForm.addNewBuffer((short[]) msg.obj);
            if (msg.what == AudioRecorder.UPDATESAMPLES)
                updateSamples((Long) msg.obj);
//            if (msg.what == AudioRecorder.PAUSED) {
//                muted = RecordingActivity.startActivity(RecordingActivity.this, "Error", getString(R.string.mic_paused));
//                if (muted != null) {
//                    AutoClose ac = new AutoClose(muted, 10);
//                    ac.run();
//                }
//            }
//            if (msg.what == AudioRecorder.MUTED) {
//                if (Build.VERSION.SDK_INT >= 28)
//                    muted = RecordingActivity.startActivity(RecordingActivity.this, getString(R.string.mic_muted_error), getString(R.string.mic_muted_pie));
//                else
//                    muted = RecordingActivity.startActivity(RecordingActivity.this, "Error", getString(R.string.mic_muted_error));
//            }
//            if (msg.what == AudioRecorder.UNMUTED) {
//                if (muted != null) {
//                    AutoClose run = new AutoClose(muted);
//                    run.run();
//                    muted = null;
//                }
//            }
            if (msg.what == AudioRecorder.END) {
                if (!recording.interrupt.get()) {
                    stopRecording();
                }
            }
            if (msg.what == AudioRecorder.ERROR)
                Error((Exception) msg.obj);
        }
    };
}
