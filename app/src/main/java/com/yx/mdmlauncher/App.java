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

    // 用户主动启动白名单应用后，允许使用的最长时间（毫秒）
    public static final long ALLOWED_APP_USE_DURATION = 120000; // 2分钟

    private boolean userLaunchedAllowedApp = false;
    private long userLaunchTimestamp = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        mainHandler = new Handler(Looper.getMainLooper());
        prefs.edit().putBoolean(KEY_LOCKED, true).apply();
    }

    public static App getInstance() { return instance; }
    public SharedPreferences getPrefs() { return prefs; }
    public boolean isLocked() { return prefs.getBoolean(KEY_LOCKED, true); }
    public void setLocked(boolean locked) { prefs.edit().putBoolean(KEY_LOCKED, locked).apply(); }
    public Handler getMainHandler() { return mainHandler; }

    public void setUserLaunchedAllowedApp(boolean launched) {
        this.userLaunchedAllowedApp = launched;
        this.userLaunchTimestamp = launched ? System.currentTimeMillis() : 0;
    }

    public boolean isInAllowedAppGracePeriod() {
        if (!userLaunchedAllowedApp) return false;
        return (System.currentTimeMillis() - userLaunchTimestamp) < ALLOWED_APP_USE_DURATION;
    }

    public long getGracePeriodRemaining() {
        if (!userLaunchedAllowedApp) return 0;
        long remaining = ALLOWED_APP_USE_DURATION - (System.currentTimeMillis() - userLaunchTimestamp);
        return remaining > 0 ? remaining : 0;
    }
}
