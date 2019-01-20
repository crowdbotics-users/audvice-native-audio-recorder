package com.reactlibrary.recorder;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.util.LinkedList;
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

    private final static float HEIGHT_FACTOR = 0.8f;

    public interface WaveformListener {
        public void waveformTouchStart(float x);
        public void waveformTouchMove(float x);
        public void waveformTouchEnd();
        public void waveformFling(float x);
        public void waveformDraw();
    };

    // Colors
    private Paint mGridPaint;
    private Paint mPlaybackLinePaint;
    private Paint mLinePaint;
    private Paint mTimecodePaint;

    private RawSamples mSoundFile;
    private int mLength;
    private List<Double> mValues;
    public int mSampleRate;
    public int mSamplesPerPixel;
    private int mOffset;
    private float mDensity;
    private WaveformListener mListener;
    private GestureDetector mGestureDetector;
    private boolean mInitialized;
    private short mMaxValue = 255;
    private short mMinValue = 0;
    private long mChunkIndex = 0;

    public WaveformView(Context context) {
        super(context);

        // We don't want keys, the markers get these
        setFocusable(false);

        Resources res = getResources();
        mGridPaint = new Paint();
        mGridPaint.setAntiAlias(false);
        mGridPaint.setColor(Color.TRANSPARENT);// TODO
        mPlaybackLinePaint = new Paint();
        mPlaybackLinePaint.setAntiAlias(false);
        mPlaybackLinePaint.setColor(Color.WHITE);
        mLinePaint = new Paint();
        mLinePaint.setAntiAlias(false);
        mLinePaint.setColor(Color.YELLOW);
        mTimecodePaint = new Paint();
        mTimecodePaint.setTextSize(12);
        mTimecodePaint.setAntiAlias(true);
        mTimecodePaint.setColor(Color.GREEN);
        mTimecodePaint.setShadowLayer(2, 1, 1, Color.GREEN);

        mGestureDetector = new GestureDetector(
                context,
                new GestureDetector.SimpleOnGestureListener() {
                    public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy) {
                        mListener.waveformFling(vx);
                        return true;
                    }
                }
        );

        mSoundFile = null;
        mLength = 0;
        mValues = new LinkedList<>();
        mOffset = 0;
        mDensity = 1.0f;
        mInitialized = false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
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

    public void setSoundFile(RawSamples soundFile, int sampleRate, int pixelsPerSec) {
        mSoundFile = soundFile;
        mSampleRate = sampleRate;
        mSamplesPerPixel = sampleRate/ pixelsPerSec;
        computeDoublesForAllZoomLevels();
    }

    public boolean isInitialized() {
        return mInitialized;
    }

    public int maxPos() {
        return mLength;
    }

    public int secondsToPixels(double seconds) {
        return (int)(seconds * mSampleRate / mSamplesPerPixel + 0.5);
    }

    public double pixelsToSeconds(int pixels) {
        return (pixels * (double) mSamplesPerPixel / (mSampleRate));
    }

    public int millisecsToPixels(int msecs) {
        return (int)((msecs * mSampleRate) /
                (1000.0 * mSamplesPerPixel) + 0.5);
    }

    public int pixelsToMillisecs(int pixels) {
        return (int)(pixels * (1000.0 * mSamplesPerPixel) /
                (mSampleRate) + 0.5);
    }

    public void setOffset(int offset) {
        mOffset = offset;
        invalidate();
    }

    public int getOffset() {
        return mOffset;
    }

    public void setListener(WaveformListener listener) {
        mListener = listener;
    }

    public void setDensity(float density) {
        mDensity = density;
        mTimecodePaint.setTextSize((int)(12 * density));
    }

    protected void drawWaveformLine(Canvas canvas,
                                    int x, int y0, int y1,
                                    Paint paint) {
        canvas.drawLine(x, y0, x, y1, paint);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mSoundFile == null)
            return;

        // Draw waveform
        int measuredWidth = getMeasuredWidth();
        int measuredHeight = getMeasuredHeight();
        int mHalfHeight = (getMeasuredHeight() / 2) - 1;
        int offset = mOffset - measuredWidth / 2;
        int start = offset;
        int width = mValues.size() - start;
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
        for (i = 0; i < width; i++) {
            int height = 0;
            if (start + i >= 0) {
                height = (int) (mValues.get(start + i) * mHalfHeight * HEIGHT_FACTOR / mMaxValue);
            }
            drawWaveformLine(
                    canvas, i,
                    ctr - height ,
                    ctr + 1 + height,
                    mLinePaint);
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
        i = 0;
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
                        (int)(12 * mDensity),
                        mTimecodePaint);
            }
        }

        if (mListener != null) {
            mListener.waveformDraw();
        }
    }

    /**
     * Called once when a new sound file is added
     */
    private void computeDoublesForAllZoomLevels() {
        int numFrames = (int) mSoundFile.getSamples() / mSamplesPerPixel;

        mValues.clear();
        int value;
        double gain;
        short[] buffer = new short[mSamplesPerPixel];
        mSoundFile.open(0, mSamplesPerPixel);
        mMaxValue = 1;
        int lastLen = 0;
        for (int i = 0; i < numFrames; i++) {
            gain = -1;
            lastLen = mSoundFile.read(buffer);
            for (int j = 0; j < lastLen; j++) {
                value = buffer[j];
                gain = Math.max(gain, value);
            }
            mMaxValue = (short) Math.max(mMaxValue, (double) gain);
            mValues.add(gain);
        }

        mSoundFile.close();

        if (lastLen < mSamplesPerPixel) {
            mChunkIndex = lastLen;
        }

        mLength = numFrames;
        mOffset = mLength;

        mInitialized = true;
    }

    public void addNewBuffer(short[] buffer) {
        mChunkIndex = 0;
        double gain = 0;
        for (int i = 0; i< buffer.length; i++) {
            gain = Math.max(gain, (double)buffer[i]);
            if (mChunkIndex >= mSamplesPerPixel) {
                mValues.add(gain);
                mMaxValue = (short) Math.max(mMaxValue, (double) gain);
                gain = 0;
                mChunkIndex = 0;
                continue;
            }
            mChunkIndex++;
        }
        mLength = mValues.size();
        mOffset = mLength;
        invalidate();
    }
}
