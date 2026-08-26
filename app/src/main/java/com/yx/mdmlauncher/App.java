package com.yx.mdmlauncher;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class App extends Application {
    private static final String TAG = "App";
    private static App instance;
    private SharedPreferences prefs;
    private Handler mainHandler;

    // 密码不再硬编码，改用 SHA-256 加盐哈希存储在 SharedPreferences 中
    // 首次运行时使用默认密码，用户可通过管理界面修改
    private static final String DEFAULT_PASSWORD = "yxmdm2024";
    private static final String PASSWORD_SALT = "yx_mdm_launcher_secure_salt_2024";

    public static final String PREFS_NAME = "mdm_launcher_prefs";
    public static final String KEY_LOCKED = "is_locked";
    public static final String KEY_PASSWORD_HASH = "password_hash";

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

        // 首次运行：初始化默认密码哈希
        if (!prefs.contains(KEY_PASSWORD_HASH)) {
            String hash = hashPassword(DEFAULT_PASSWORD);
            prefs.edit().putString(KEY_PASSWORD_HASH, hash).apply();
            Log.i(TAG, "首次运行，已初始化默认密码哈希");
        }
    }

    public static App getInstance() { return instance; }
    public SharedPreferences getPrefs() { return prefs; }
    public boolean isLocked() { return prefs.getBoolean(KEY_LOCKED, true); }
    public void setLocked(boolean locked) { prefs.edit().putBoolean(KEY_LOCKED, locked).apply(); }
    public Handler getMainHandler() { return mainHandler; }

    /**
     * 验证管理密码是否正确。
     * @param input 用户输入的明文密码
     * @return true 表示密码正确
     */
    public boolean verifyPassword(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        String storedHash = prefs.getString(KEY_PASSWORD_HASH, "");
        if (storedHash.isEmpty()) {
            return false;
        }
        String inputHash = hashPassword(input);
        return storedHash.equals(inputHash);
    }

    /**
     * 修改管理密码。
     * @param newPassword 新的明文密码
     * @return true 表示修改成功
     */
    public boolean changePassword(String newPassword) {
        if (newPassword == null || newPassword.length() < 4) {
            return false;
        }
        String hash = hashPassword(newPassword);
        prefs.edit().putString(KEY_PASSWORD_HASH, hash).apply();
        return true;
    }

    /**
     * 使用 SHA-256 + 固定盐对密码进行哈希。
     * 注意：对于更高安全要求，应使用 Android Keystore 或 EncryptedSharedPreferences。
     */
    private static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String salted = PASSWORD_SALT + password;
            byte[] hash = md.digest(salted.getBytes());
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "SHA-256 not available", e);
            return "";
        }
    }

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
