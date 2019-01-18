package com.reactlibrary;

import android.content.Context;
import android.graphics.Color;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class RNAudioRecorderView extends RelativeLayout {

    TextView mTvStatus;

    public RNAudioRecorderView(Context context) {
        super(context);
        setBackgroundColor(Color.RED);
        addSubViews();
    }

    void addSubViews() {
        mTvStatus = new TextView(getContext());
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        this.addView(mTvStatus, params);
    }

    public void setStatus(String status) {
        mTvStatus.setText(status);
    }
}
