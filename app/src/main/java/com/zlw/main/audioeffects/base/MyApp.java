package com.zlw.main.audioeffects.base;

import android.app.Application;

/**
 * @author zhaolewei on 2018/8/17.
 */
public class MyApp extends Application {
    private static MyApp instance;

    public static MyApp getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }
}
