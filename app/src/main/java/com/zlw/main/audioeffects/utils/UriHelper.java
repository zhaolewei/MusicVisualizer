package com.zlw.main.audioeffects.utils;

import android.net.Uri;

import com.zlw.main.audioeffects.base.MyApp;

import java.io.File;
import java.util.Locale;

/**
 * Created by admin on 2018/8/27.
 */

public class UriHelper {

    public static Uri getResUri(int res) {
        return Uri.parse(String.format(Locale.getDefault(), "android.resource://%s/%s", MyApp.getInstance().getPackageName(), res));
    }

    public static Uri getFileUri(File file) {
        return Uri.fromFile(file);
    }

    public static Uri getFileUri(String file) {
        return Uri.fromFile(new File(file));
    }

}
