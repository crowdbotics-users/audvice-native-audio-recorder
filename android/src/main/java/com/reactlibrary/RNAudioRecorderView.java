package com.reactlibrary;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
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
import com.reactlibrary.recorder.PitchView;
import com.reactlibrary.recorder.RawSamples;
import com.reactlibrary.recorder.RecordConfig;
import com.reactlibrary.recorder.Sound;
import com.reactlibrary.recorder.WaveformView;

import java.io.File;
import java.nio.ShortBuffer;

import static android.content.ContentValues.TAG;

public class RNAudioRecorderView extends RelativeLayout {

    TextView mTvStatus;
    RecordConfig config;
    AudioRecorder recording;
    PitchView pitch;
    private long editSample;
    private AudioTrack play;
    WaveformView mWaveForm;

    public RNAudioRecorderView(Context context) {
        super(context);
        setBackgroundColor(Color.RED);
        addSubViews();

        // init
    }

    void addSubViews() {
        mTvStatus = new TextView(getContext());
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        mTvStatus.setGravity(Gravity.CENTER);
        mTvStatus.setBackgroundColor(Color.WHITE);
        mTvStatus.setVisibility(View.GONE);
        this.addView(mTvStatus, params);

        pitch = new PitchView(getContext());
        LayoutParams params1 = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
//        this.addView(pitch, params1);

        mWaveForm = new WaveformView(getContext());
        this.addView(mWaveForm, params1);
    }

    // External Reference Methods
    public void initialize() {
        // start initialize for record
        // check the audio record permission
        if (!hasPermissions()) {
            mTvStatus.setText("Has No Enough Permission, Please enable permission and try again.");
            mTvStatus.setVisibility(View.VISIBLE);
            pitch.setVisibility(View.GONE);
            return;
        }
        // init
        config = new RecordConfig();
        // TODO: set config
        // get colors
        recording = new AudioRecorder(getContext(), pitch.getPitchTime(), config);
        synchronized (recording.handlers) {
            recording.handlers.add(handler);
        }

        recording.updateBufferSize(false);

        // init waveform
        DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
        mWaveForm.setDensity(metrics.density);

        loadSamples();
    }

    void loadSamples() {
//        recording.storage.getTempRecording().delete();
        File f = recording.storage.getTempRecording();
        if (!f.exists()) {
            recording.samplesTime = 0;
            updateSamples(recording.samplesTime);
            return;
        }

        RawSamples rs = new RawSamples(f);
        recording.samplesTime = rs.getSamples() / Sound.getChannels(getContext());

        DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();

        int count = pitch.getMaxPitchCount(metrics.widthPixels);

        short[] buf = new short[count * recording.samplesUpdateStereo];
        long cut = recording.samplesTime * Sound.getChannels(getContext()) - buf.length;

        if (cut < 0)
            cut = 0;

        rs.open(cut, buf.length);
        int len = rs.read(buf);
        rs.close();
//
//        pitch.clear(cut / recording.samplesUpdateStereo);
        int lenUpdate = len / recording.samplesUpdateStereo * recording.samplesUpdateStereo; // cut right overs (leftovers from right)
//        for (int i = 0; i < lenUpdate; i += recording.samplesUpdateStereo) {
//            double dB = RawSamples.getDB(buf, i, recording.samplesUpdateStereo);
//            pitch.add(dB);
//        }

        mWaveForm.setSoundFile(rs, recording.sampleRate, 100);
        mWaveForm.invalidate();

        updateSamples(recording.samplesTime);

        int diff = len - lenUpdate;
        if (diff > 0) {
            recording.dbBuffer = ShortBuffer.allocate(recording.samplesUpdateStereo);
            recording.dbBuffer.put(buf, lenUpdate, diff);
        }
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

    public void startRecording() {
        try {
            pitch.setOnTouchListener(null);

            pitch.record();
            recording.startRecording();

        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    void stopRecording(String status) {

        stopRecording();

        pitch.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
//                float x = event.getX();
//                if (x < 0)
//                    x = 0;
//                long edit = pitch.edit(x);
//                else
//                    editSample = pitch.edit(x) * recording.samplesUpdate;
                return true;
            }
        });
    }

    void stopRecording() {
        if (recording != null) // not possible, but some devices do not call onCreate
            recording.stopRecording();
        pitch.stop();
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

            int playUpdate = PitchView.UPDATE_SPEED * recording.sampleRate / 1000;

            RawSamples rs = new RawSamples(recording.storage.getTempRecording());
            editSample = 0;
            int len = (int) (rs.getSamples() - editSample * Sound.getChannels(getContext())); // in samples

            final AudioTrack.OnPlaybackPositionUpdateListener listener = new AudioTrack.OnPlaybackPositionUpdateListener() {
                @Override
                public void onMarkerReached(android.media.AudioTrack track) {
                    editPlay(false);
                }

                @Override
                public void onPeriodicNotification(android.media.AudioTrack track) {
                    if (play != null) {
                        long now = System.currentTimeMillis();
                        long playIndex = editSample + (now - play.playStart) * recording.sampleRate / 1000;
                        pitch.play(playIndex / (float) recording.samplesUpdate);
                    }
                }
            };

            AudioTrack.AudioBuffer buf = new AudioTrack.AudioBuffer(recording.sampleRate, Sound.getOutMode(getContext()), Sound.DEFAULT_AUDIOFORMAT, len);
            rs.open(editSample * Sound.getChannels(getContext()), buf.len); // len in samples
            int r = rs.read(buf.buffer); // r in samples
            if (r != buf.len)
                throw new RuntimeException("unable to read data");
            int last = buf.len / buf.getChannels() - 1;
            if (play != null)
                play.release();
            play = AudioTrack.create(Sound.SOUND_STREAM, Sound.SOUND_CHANNEL, Sound.SOUND_TYPE, buf);
            play.setNotificationMarkerPosition(last);
            play.setPositionNotificationPeriod(playUpdate);
            play.setPlaybackPositionUpdateListener(listener, handler);
            play.play();
        } else {
            if (play != null) {
                play.release();
                play = null;
            }
            pitch.play(-1);
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
                pitch.drawEnd();
                if (!recording.interrupt.get()) {
                    stopRecording("pause");
                }
            }
            if (msg.what == AudioRecorder.ERROR)
                Error((Exception) msg.obj);
        }
    };
}
