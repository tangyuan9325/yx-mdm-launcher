package com.yx.mdmlauncher;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import java.util.Timer;
import java.util.TimerTask;

public class LockService extends Service {

    private static final String CHANNEL_ID = "mdm_lock_channel";
    private static final String FULLSCREEN_CHANNEL_ID = "mdm_fullscreen_channel";
    private static final int NOTIFICATION_ID = 1001;
    private static final int FULLSCREEN_NOTIFICATION_ID = 1002;

    private Timer watchdogTimer;
    private Handler handler;
    private long lastFullScreenTime = 0;
    private static final long FULLSCREEN_INTERVAL = 15000;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        createNotificationChannels();
        startForeground(NOTIFICATION_ID, buildNotification());
        startWatchdog();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "MDM管控服务", NotificationManager.IMPORTANCE_LOW);
                channel.setDescription("MDM设备管控保活服务");
                manager.createNotificationChannel(channel);

                NotificationChannel fullscreenChannel = new NotificationChannel(
                    FULLSCREEN_CHANNEL_ID, "MDM全屏唤起", NotificationManager.IMPORTANCE_HIGH);
                fullscreenChannel.setDescription("用于在后台拉起管控界面");
                manager.createNotificationChannel(fullscreenChannel);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private Notification buildNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("设备管控中")
                .setContentText("MDM管控服务正在运行")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setOngoing(true)
                .build();
        } else {
            return new Notification.Builder(this)
                .setContentTitle("设备管控中")
                .setContentText("MDM管控服务正在运行")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setOngoing(true)
                .build();
        }
    }

    private void startWatchdog() {
        watchdogTimer = new Timer();
        watchdogTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (App.getInstance().isLocked()) {
                            bringMainActivityToFront();
                        }
                    }
                });
            }
        }, 3000, 3000);
    }

    private void bringMainActivityToFront() {
        try {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        } catch (Exception e) {
            // ignore
        }

        long now = System.currentTimeMillis();
        if (now - lastFullScreenTime >= FULLSCREEN_INTERVAL) {
            lastFullScreenTime = now;
            showFullScreenIntent();
        }
    }

    private void showFullScreenIntent() {
        try {
            Intent fullScreenIntent = new Intent(this, MainActivity.class);
            fullScreenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                this, 0, fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            Notification.Builder builder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder = new Notification.Builder(this, FULLSCREEN_CHANNEL_ID);
            } else {
                builder = new Notification.Builder(this);
            }

            builder.setContentTitle("设备管控")
                .setContentText("管控界面正在恢复")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setPriority(Notification.PRIORITY_MAX)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setAutoCancel(true)
                .setOngoing(false);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setCategory(Notification.CATEGORY_ALARM);
            }

            Notification notification = builder.build();
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.notify(FULLSCREEN_NOTIFICATION_ID, notification);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        manager.cancel(FULLSCREEN_NOTIFICATION_ID);
                    }
                }, 2000);
            }
        } catch (Exception e) {
            // ignore
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (watchdogTimer != null) {
            watchdogTimer.cancel();
        }
        if (App.getInstance().isLocked()) {
            Intent restartIntent = new Intent(this, LockService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(restartIntent);
            } else {
                startService(restartIntent);
            }
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Intent restartIntent = new Intent(this, LockService.class);
        restartIntent.setPackage(getPackageName());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(restartIntent);
        } else {
            startService(restartIntent);
        }
    }
}
