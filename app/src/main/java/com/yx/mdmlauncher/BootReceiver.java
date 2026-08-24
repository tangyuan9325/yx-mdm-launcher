package com.yx.mdmlauncher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
            || Intent.ACTION_REBOOT.equals(action)
            || "android.intent.action.QUICKBOOT_POWERON".equals(action)) {

            final PendingResult pendingResult = goAsync();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    PowerManager.WakeLock wakeLock = null;
                    try {
                        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                        if (pm != null) {
                            wakeLock = pm.newWakeLock(
                                PowerManager.PARTIAL_WAKE_LOCK,
                                "YXMDM:BootReceiver"
                            );
                            wakeLock.setReferenceCounted(false);
                            wakeLock.acquire(10000);
                        }

                        try {
                            Intent launchIntent = new Intent(context, MainActivity.class);
                            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                                | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                            context.startActivity(launchIntent);
                        } catch (Exception e) {
                            try {
                                Intent serviceIntent = new Intent(context, LockService.class);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    context.startForegroundService(serviceIntent);
                                } else {
                                    context.startService(serviceIntent);
                                }
                            } catch (Exception ex) {
                                // ignore
                            }
                        }
                    } finally {
                        if (wakeLock != null && wakeLock.isHeld()) {
                            try { wakeLock.release(); } catch (Exception e) { /* ignore */ }
                        }
                        pendingResult.finish();
                    }
                }
            }).start();
        }
    }
}
