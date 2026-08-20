package com.yx.mdmlauncher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final String[] ALLOWED_PACKAGES = {
        "com.edu.k12.hippo",
        "com.xiaoluxue.ai.student",
        "com.yx.appstore"
    };

    private static final String[] APP_NAMES = {
        "学习中心",
        "小鹿AI",
        "应用商店"
    };

    private static final int EXIT_TAP_COUNT = 7;
    private int cornerTapCount = 0;
    private long lastCornerTapTime = 0;

    private int[] volumeSequence = new int[8];
    private int volumeIndex = 0;
    private static final int[] KONAMI_VOLUME = {
        KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_UP,
        KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN,
        KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN,
        KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN
    };

    private Handler handler = new Handler();
    private boolean isUserCenterShown = false;
    private FrameLayout rootLayout;
    private LinearLayout appGridLayout;
    private View userCenterView;

    private static final int RESTORE_TAP_COUNT = 7;
    private int restoreTapCount = 0;
    private long lastRestoreTapTime = 0;

    private BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                cornerTapCount = 0;
                volumeIndex = 0;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        rootLayout = new FrameLayout(this);
        rootLayout.setBackgroundResource(R.drawable.ic_main_bg);

        appGridLayout = new LinearLayout(this);
        appGridLayout.setOrientation(LinearLayout.HORIZONTAL);
        appGridLayout.setGravity(android.view.Gravity.CENTER);
        FrameLayout.LayoutParams gridParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        );
        gridParams.gravity = android.view.Gravity.TOP | android.view.Gravity.LEFT;
        appGridLayout.setLayoutParams(gridParams);

        for (int i = 0; i < ALLOWED_PACKAGES.length; i++) {
            appGridLayout.addView(createAppIconView(i));
            if (i < ALLOWED_PACKAGES.length - 1) {
                View spacer = new View(this);
                LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(dpToPx(30), 1);
                spacer.setLayoutParams(sp);
                appGridLayout.addView(spacer);
            }
        }
        rootLayout.addView(appGridLayout);

        ImageView userBtn = new ImageView(this);
        userBtn.setImageResource(R.drawable.ic_user);
        userBtn.setScaleType(ImageView.ScaleType.FIT_CENTER);
        FrameLayout.LayoutParams btnParams = new FrameLayout.LayoutParams(dpToPx(72), dpToPx(72));
        btnParams.gravity = android.view.Gravity.TOP | android.view.Gravity.CENTER_HORIZONTAL;
        userBtn.setLayoutParams(btnParams);
        userBtn.setOnClickListener(v -> toggleUserCenter());
        rootLayout.addView(userBtn);

        userCenterView = createUserCenterView();
        userCenterView.setVisibility(View.GONE);
        rootLayout.addView(userCenterView);

        setContentView(rootLayout);

        rootLayout.post(new Runnable() {
            @Override
            public void run() {
                int screenHeight = rootLayout.getHeight();
                int screenWidth = rootLayout.getWidth();

                FrameLayout.LayoutParams gridParams = (FrameLayout.LayoutParams) appGridLayout.getLayoutParams();
                gridParams.topMargin = (int) (screenHeight * 0.58);
                gridParams.leftMargin = (int) (screenWidth * 0.08);
                appGridLayout.setLayoutParams(gridParams);

                FrameLayout.LayoutParams btnParams = (FrameLayout.LayoutParams) userBtn.getLayoutParams();
                btnParams.topMargin = (int) (screenHeight * 0.22);
                userBtn.setLayoutParams(btnParams);
            }
        });

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(screenReceiver, filter);
        }

        startService(new Intent(this, LockService.class));
    }

    private View createAppIconView(int index) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(dpToPx(90), LinearLayout.LayoutParams.WRAP_CONTENT);
        item.setLayoutParams(itemParams);

        ImageView icon = new ImageView(this);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dpToPx(64), dpToPx(64));
        icon.setLayoutParams(iconParams);

        try {
            Drawable appIcon = getPackageManager().getApplicationIcon(ALLOWED_PACKAGES[index]);
            icon.setImageDrawable(appIcon);
        } catch (PackageManager.NameNotFoundException e) {
            icon.setImageResource(android.R.drawable.sym_def_app_icon);
        }

        TextView name = new TextView(this);
        name.setText(APP_NAMES[index]);
        name.setTextColor(Color.WHITE);
        name.setTextSize(14);
        name.setGravity(android.view.Gravity.CENTER);
        name.setShadowLayer(2, 1, 1, Color.BLACK);

        item.addView(icon);
        item.addView(name);

        final int idx = index;
        item.setOnClickListener(v -> launchApp(ALLOWED_PACKAGES[idx]));

        return item;
    }

    private View createUserCenterView() {
        FrameLayout container = new FrameLayout(this);
        container.setBackgroundResource(R.drawable.ic_user_center);

        View restoreBtn = new View(this);
        restoreBtn.setBackgroundColor(Color.TRANSPARENT);
        FrameLayout.LayoutParams restoreParams = new FrameLayout.LayoutParams(dpToPx(90), dpToPx(90));
        restoreParams.gravity = android.view.Gravity.TOP | android.view.Gravity.RIGHT;
        restoreParams.topMargin = dpToPx(20);
        restoreParams.rightMargin = dpToPx(20);
        restoreBtn.setLayoutParams(restoreParams);
        restoreBtn.setOnClickListener(v -> handleRestoreTap());
        container.addView(restoreBtn);

        container.setOnClickListener(v -> toggleUserCenter());

        return container;
    }

    private void toggleUserCenter() {
        if (isUserCenterShown) {
            isUserCenterShown = false;
            userCenterView.animate()
                .alpha(0f)
                .setDuration(250)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        userCenterView.setVisibility(View.GONE);
                        restoreTapCount = 0;
                    }
                })
                .start();
        } else {
            isUserCenterShown = true;
            restoreTapCount = 0;
            userCenterView.setAlpha(0f);
            userCenterView.setVisibility(View.VISIBLE);
            userCenterView.animate()
                .alpha(1f)
                .setDuration(250)
                .start();
        }
    }

    private void handleRestoreTap() {
        long now = System.currentTimeMillis();
        if (now - lastRestoreTapTime > 2000) {
            restoreTapCount = 0;
        }
        restoreTapCount++;
        lastRestoreTapTime = now;

        if (restoreTapCount >= RESTORE_TAP_COUNT) {
            restoreTapCount = 0;
            toggleUserCenter();
            showExitPasswordDialog();
        } else if (restoreTapCount >= 5) {
            Toast.makeText(this, "继续点击 " + (RESTORE_TAP_COUNT - restoreTapCount) + " 次进入管理", Toast.LENGTH_SHORT).show();
        }
    }

    private void launchApp(String packageName) {
        try {
            Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } else {
                Toast.makeText(this, "应用未安装: " + packageName, Toast.LENGTH_SHORT).show();
            }
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "无法启动应用", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleCornerTap() {
        long now = System.currentTimeMillis();
        if (now - lastCornerTapTime > 2000) {
            cornerTapCount = 0;
        }
        cornerTapCount++;
        lastCornerTapTime = now;

        if (cornerTapCount >= EXIT_TAP_COUNT) {
            cornerTapCount = 0;
            showExitPasswordDialog();
        } else if (cornerTapCount >= 3) {
            Toast.makeText(this, "继续点击 " + (EXIT_TAP_COUNT - cornerTapCount) + " 次进入管理", Toast.LENGTH_SHORT).show();
        }
    }

    private void showExitPasswordDialog() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint("请输入管理密码");

        new AlertDialog.Builder(this)
            .setTitle("管理模式")
            .setMessage("请输入密码以退出管控（重新启动应用可恢复）")
            .setView(input)
            .setPositiveButton("确认", (dialog, which) -> {
                String pwd = input.getText().toString();
                if (App.EXIT_PASSWORD.equals(pwd)) {
                    exitLockMode();
                } else {
                    Toast.makeText(MainActivity.this, "密码错误", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("取消", null)
            .setCancelable(false)
            .show();
    }

    private void exitLockMode() {
        App.getInstance().setLocked(false);
        Toast.makeText(this, "已退出管控模式，重新启动应用可恢复", Toast.LENGTH_LONG).show();

        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(Intent.createChooser(homeIntent, "选择桌面"));
        } catch (Exception e) {
            // ignore
        }

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, 1500);
    }

    @Override
    public void onBackPressed() {
        if (isUserCenterShown) {
            toggleUserCenter();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            checkVolumeCombo(keyCode);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_HOME) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    private void checkVolumeCombo(int keyCode) {
        volumeSequence[volumeIndex] = keyCode;
        volumeIndex++;

        boolean match = true;
        for (int i = 0; i < volumeIndex; i++) {
            if (volumeSequence[i] != KONAMI_VOLUME[i]) {
                match = false;
                break;
            }
        }

        if (!match) {
            volumeIndex = 0;
            if (keyCode == KONAMI_VOLUME[0]) {
                volumeSequence[0] = keyCode;
                volumeIndex = 1;
            }
        } else if (volumeIndex == KONAMI_VOLUME.length) {
            volumeIndex = 0;
            showExitPasswordDialog();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (App.getInstance().isLocked()) {
            handler.postDelayed(() -> {
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
            }, 100);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (App.getInstance().isLocked()) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(screenReceiver);
        } catch (Exception e) {
            // ignore
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }
}
