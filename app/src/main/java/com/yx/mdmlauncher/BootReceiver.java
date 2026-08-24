package com.yx.mdmlauncher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
            || Intent.ACTION_REBOOT.equals(action)
            || "android.intent.action.QUICKBOOT_POWERON".equals(action)) {

            // 1. 先启动前台服务（Android 10+ 有前台服务运行时可放宽后台启动 Activity 限制）
            Intent serviceIntent = new Intent(context, LockService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }

            // 2. 延迟 500ms 启动主界面，确保服务已进入前台状态
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent launchIntent = new Intent(context, MainActivity.class);
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    try {
                        context.startActivity(launchIntent);
                    } catch (Exception e) {
                        // 后台启动被阻止时，LockService 的 watchdog 会在 3 秒内拉回
                    }
                }
            }, 500);
        }
    }
}
