package com.yx.mdmlauncher;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

public class App extends Application {

    private static App instance;
    private SharedPreferences prefs;
    private Handler mainHandler;

    public static final String EXIT_PASSWORD = "yxmdm2024";
    public static final String PREFS_NAME = "mdm_launcher_prefs";
    public static final String KEY_LOCKED = "is_locked";

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        mainHandler = new Handler(Looper.getMainLooper());

        if (!prefs.contains(KEY_LOCKED)) {
            prefs.edit().putBoolean(KEY_LOCKED, true).apply();
        }
    }

    public static App getInstance() {
        return instance;
    }

    public SharedPreferences getPrefs() {
        return prefs;
    }

    public boolean isLocked() {
        return prefs.getBoolean(KEY_LOCKED, true);
    }

    public void setLocked(boolean locked) {
        prefs.edit().putBoolean(KEY_LOCKED, locked).apply();
    }

    public Handler getMainHandler() {
        return mainHandler;
    }
}
