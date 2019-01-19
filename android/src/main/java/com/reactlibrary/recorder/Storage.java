package com.reactlibrary.recorder;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;

import java.io.File;
import java.util.Date;

public class Storage extends com.reactlibrary.recorder.base.Storage {

    public static String getFormatted(String format, Date date) {
        format = format.replaceAll("%s", SIMPLE.format(date));
        format = format.replaceAll("%I", ISO8601.format(date));
        format = format.replaceAll("%T", "" + System.currentTimeMillis() / 1000);
        return format;
    }

    public Storage(Context context) {
        super(context);
    }

    @Override
    public Uri getNewFile() {
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
        String ext = "wav";

        String format = "%s";

        format = getFormatted(format, new Date());

        Uri path = getStoragePath();
        String s = path.getScheme();

        if (Build.VERSION.SDK_INT >= 21 && s.equals(ContentResolver.SCHEME_CONTENT)) {
            return getNextFile(path, format, ext);
        } else if (s.equals(ContentResolver.SCHEME_FILE)) {
            File f = getFile(path);
            if (!f.exists() && !f.mkdirs())
                throw new RuntimeException("Unable to create: " + path);
            return Uri.fromFile(getNextFile(f, format, ext));
        } else {
            throw new com.github.axet.androidlibrary.app.Storage.UnknownUri();
        }
    }

    public File getNewFile(File f, String ext) {
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);

        String format = "%s";

        format = getFormatted(format, new Date());

        if (!f.exists() && !f.mkdirs())
            throw new RuntimeException("Unable to create: " + f);
        return getNextFile(f, format, ext);
    }

}
