package com.reactlibrary;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.text.Layout;
import android.util.Log;
import android.view.Gravity;
import android.widget.RelativeLayout;
import android.widget.TextView;

import static android.content.ContentValues.TAG;

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
        mTvStatus.setGravity(Gravity.CENTER);
        mTvStatus.setBackgroundColor(Color.WHITE);
        this.addView(mTvStatus, params);
    }

    // External Reference Methods
    public void initialize() {
        // start initialize for record
        // check the audio record permission
        if (!hasPermissions()) {
            mTvStatus.setText("Has No Enough Permission, Please enable permission and try again.");
            return;
        }

        // init
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
}
