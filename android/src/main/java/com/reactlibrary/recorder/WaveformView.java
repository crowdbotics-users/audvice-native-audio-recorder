package com.reactlibrary.recorder;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.util.List;

/**
 * WaveformView is an Android view that displays a visual representation
 * of an audio waveform.  It retrieves the frame gains from a CheapSoundFile
 * object and recomputes the shape contour at several zoom levels.
 *
 * This class doesn't handle selection or any of the touch interactions
 * directly, so it exposes a listener interface.  The class that embeds
 * this view should add itself as a listener and make the view scroll
 * and respond to other events appropriately.
 *
 * WaveformView doesn't actually handle selection, but it will just display
 * the selected part of the waveform in a different color.
 */
public class WaveformView extends View {

    public interface WaveformListener {
        public void waveformTouchStart(float x);
        public void waveformTouchMove(float x);
        public void waveformTouchEnd();
        public void waveformFling(float x);
        public void waveformDraw();
    }

    private float HEIGHT_FACTOR = 0.8f;

    // Colors
    private Paint mGridPaint;
    private Paint mPlaybackLinePaint;
    private Paint mLinePaint;
    private Paint mTimecodePaint;

    private SoundFile mSoundFile;
    private int mLength;
    private int mSampleRate;
    private int mSamplesPerPixel;
    private int mOffset;
    private int mPlaybackPos;
    private float mDensity = 1;
    private float mInitialScaleSpan;
    private WaveformListener mListener;
    private GestureDetector mGestureDetector;
    private boolean mInitialized;
    private short mMaxValue = 12000;
    private boolean mAutoSeeking = false;
    private int mTimeTextSize = 12;

    public void setPlotLineColor(int color) {
        mLinePaint.setColor(color);
        mPlaybackLinePaint.setColor(color);
        invalidate();
    }

    public void setTimeTextColor(int color) {
        mTimecodePaint.setColor(color);
        invalidate();
    }

    public void setTimeTextSize(int size) {
        mTimeTextSize = size;
        mTimecodePaint.setTextSize(mTimeTextSize * mDensity);
        invalidate();
    }

    public WaveformView(Context context) {
        super(context);

        // We don't want keys, the markers get these
        setFocusable(false);

        mGridPaint = new Paint();
        mGridPaint.setAntiAlias(false);
        mGridPaint.setColor(Color.TRANSPARENT);
        mPlaybackLinePaint = new Paint();
        mPlaybackLinePaint.setAntiAlias(false);
        mPlaybackLinePaint.setColor(Color.WHITE);
        mLinePaint = new Paint();
        mLinePaint.setAntiAlias(false);
        mLinePaint.setColor(Color.YELLOW);
        mTimecodePaint = new Paint();
        mTimecodePaint.setTextSize(mTimeTextSize * mDensity);
        mTimecodePaint.setAntiAlias(true);
        mTimecodePaint.setColor(Color.GREEN);

        mGestureDetector = new GestureDetector(
                context,
                new GestureDetector.SimpleOnGestureListener() {
                    public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy) {
                        if (mAutoSeeking) return false;
                        mListener.waveformFling(vx);
                        return true;
                    }
                }
        );

        mSoundFile = null;
        mOffset = 0;
        mPlaybackPos = -1;
        mDensity = 1.0f;
        mInitialized = false;
        mSamplesPerPixel = 100;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mAutoSeeking) return false;
        if (mGestureDetector.onTouchEvent(event)) {
            return true;
        }

        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mListener.waveformTouchStart(event.getX());
                break;
            case MotionEvent.ACTION_MOVE:
                mListener.waveformTouchMove(event.getX());
                break;
            case MotionEvent.ACTION_UP:
                mListener.waveformTouchEnd();
                break;
        }
        return true;
    }

    public boolean hasSoundFile() {
        return mSoundFile != null;
    }

    public void setSoundFile(SoundFile soundFile) {
        mSoundFile = soundFile;
        mSampleRate = mSoundFile.getSampleRate();
        mSamplesPerPixel = mSoundFile.getSamplesPerPixel();
        mInitialized = true;
    }

    public boolean isInitialized() {
        return mInitialized;
    }

    public int maxPos() {
        return mSoundFile.getNumPixels();
    }

    public int secondsToPixels(double seconds) {
        return (int)(seconds * mSampleRate / mSamplesPerPixel + 0.5);
    }

    public double pixelsToSeconds(int pixels) {
        return (pixels * (double) mSamplesPerPixel / (mSampleRate));
    }

    public int millisecsToPixels(long msecs) {
        return (int)((msecs * mSampleRate) /
                (1000.0 * mSamplesPerPixel) + 0.5);
    }

    public long pixelsToMillisecs(int pixels) {
        return (long) ((long)pixels * (1000.0 * mSamplesPerPixel) /
                (mSampleRate) + 0.5);
    }

    public void setPlaySeek(long msecs) {
        long offset = msecs * mSampleRate / 1000 / mSamplesPerPixel;
        setOffset((int)offset);
    }

    public long getCurrentPosInMs() {
        long msecs = mOffset * 1000 * mSamplesPerPixel / mSampleRate;
        return msecs;
    }

    public void setOffset(int offset) {
        if (mSoundFile == null) return;
        mOffset = Math.max(0, offset);
        mOffset = Math.min(mOffset, mSoundFile.getNumPixels());
        invalidate();
    }

    public int getOffset() {
        return mOffset;
    }

    public void setAutoSeeking(boolean audoSeeking) {
        mAutoSeeking = audoSeeking;
    }

    public boolean isAutoSeeking() {
        return mAutoSeeking;
    }

    public void updateRecording(float posInS) {
        setOffset(millisecsToPixels((long)(1000 * posInS)));
        // setOffset(mSoundFile.getNumPixels());
    }

    public void setDensity(float density) {
        mDensity = density;
        mTimecodePaint.setTextSize((int)(mTimeTextSize * density));
    }

    public void setListener(WaveformListener listener) {
        mListener = listener;
    }

    protected void drawWaveformLine(Canvas canvas,
                                    int x, int y0, int y1,
                                    Paint paint) {
        canvas.drawLine(x, y0, x, y1, paint);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw waveform
        int measuredWidth = getMeasuredWidth();
        int measuredHeight = getMeasuredHeight();
        int mHalfHeight = (getMeasuredHeight() / 2) - 1;
        int offset = mOffset - measuredWidth / 2;
        int start = offset;
        int width = mSoundFile != null ? mSoundFile.pixelGains.size() - start : -start;
        int ctr = measuredHeight / 2;

        if (width > measuredWidth)
            width = measuredWidth;

        // Draw grid
        double onePixelInSecs = pixelsToSeconds(1);
        boolean onlyEveryFiveSecs = (onePixelInSecs > 1.0 / 50.0);
        double fractionalSecs = offset * onePixelInSecs;
        int integerSecs = (int) fractionalSecs;
        int i = 0;
        while (i < width) {
            i++;
            fractionalSecs += onePixelInSecs;
            int integerSecsNew = (int) fractionalSecs;
            if (integerSecsNew != integerSecs) {
                integerSecs = integerSecsNew;
                if (!onlyEveryFiveSecs || 0 == (integerSecs % 5)) {
                    canvas.drawLine(i, 0, i, measuredHeight, mGridPaint);
                }
            }
        }

        // Draw waveform

        if (mSoundFile != null) {
            for (i = 0; i < width; i++) {
                int height = 0;
                if (start + i >= 0 && start + i < mSoundFile.pixelGains.size()) {
                    height = (int) (mSoundFile.pixelGains.get(start + i) * mHalfHeight * HEIGHT_FACTOR / mMaxValue);
                }
                drawWaveformLine(
                        canvas, i,
                        ctr - height,
                        ctr + 1 + height,
                        mLinePaint);
            }
        }

        // Draw current line
        mPlaybackLinePaint.setStrokeWidth(mDensity);
        canvas.drawLine(measuredWidth / 2, 0, measuredWidth / 2, measuredHeight, mPlaybackLinePaint);

        mLinePaint.setStrokeWidth(mDensity);
        // Draw Baseline
        canvas.drawLine(0, mHalfHeight, measuredWidth, mHalfHeight, mLinePaint);
        mLinePaint.setStrokeWidth(1);

        // Draw timecode
        double timecodeIntervalSecs = 1.0;
        if (timecodeIntervalSecs / onePixelInSecs < 50) {
            timecodeIntervalSecs = 5.0;
        }
        if (timecodeIntervalSecs / onePixelInSecs < 50) {
            timecodeIntervalSecs = 15.0;
        }

        // Draw grid
        fractionalSecs = offset * onePixelInSecs;
        int integerTimecode = (int) (fractionalSecs / timecodeIntervalSecs);

        if (fractionalSecs < 0 && integerTimecode == 0) {
            integerTimecode = -1;
        }

        i = 0;

        if (mSampleRate == 0) return;

        while (i < measuredWidth) {
            i++;
            fractionalSecs += onePixelInSecs;
            if (fractionalSecs < 0) continue;
            integerSecs = (int) fractionalSecs;
            int integerTimecodeNew = (int) (fractionalSecs /
                    timecodeIntervalSecs);
            if (integerTimecodeNew != integerTimecode) {
                integerTimecode = integerTimecodeNew;

                // Turn, e.g. 67 seconds into "1:07"
                String timecodeMinutes = "" + (integerSecs / 60);
                String timecodeSeconds = "" + (integerSecs % 60);
                if ((integerSecs % 60) < 10) {
                    timecodeSeconds = "0" + timecodeSeconds;
                }
                String timecodeStr = timecodeMinutes + ":" + timecodeSeconds;
                float diff = (float) (
                        0.5 * mTimecodePaint.measureText(timecodeStr));
                canvas.drawText(timecodeStr,
                        i - diff,
                        (int)(mTimeTextSize * mDensity),
                        mTimecodePaint);
            }
        }

        if (mListener != null) {
            mListener.waveformDraw();
        }
    }
}