package com.sourav.fakecall;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.camera2.CameraManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.FullScreenContentCallback;

public class FakeCallActivity extends AppCompatActivity {

    private static final String TAG = "FakeCallActivity";
    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private TextView tvCallerName;
    private View btnAccept, btnDecline, btnRemindMe, btnMessage;
    private View pulseAccept;
    
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "FakeCallPrefs";
    private static final String KEY_VIBRATION = "vibration_enabled";
    private static final String KEY_FLASH = "flash_enabled";
    
    private boolean isFlashOn = false;
    private Handler flashHandler = new Handler();
    private String cameraId;
    private CameraManager cameraManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Full screen immersive and show over lock screen
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        setContentView(R.layout.activity_fake_call);

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        tvCallerName = findViewById(R.id.tvFakeCallerName);
        btnAccept = findViewById(R.id.btnAccept);
        btnDecline = findViewById(R.id.btnDecline);
        btnRemindMe = findViewById(R.id.btnRemindMe);
        btnMessage = findViewById(R.id.btnMessage);
        pulseAccept = findViewById(R.id.pulseAccept);

        String name = getIntent().getStringExtra("CALLER_NAME");
        if (name != null && !name.isEmpty()) {
            tvCallerName.setText(name);
        }

        startRingtone();
        
        if (sharedPreferences.getBoolean(KEY_VIBRATION, true)) {
            startVibration();
        }
        
        if (sharedPreferences.getBoolean(KEY_FLASH, false)) {
            startFlashBlink();
        }

        startAnimations();

        btnDecline.setOnClickListener(v -> showAdAndFinish());
        btnAccept.setOnClickListener(v -> {
            // Simulate call accepted state
            Toast.makeText(this, "Call Accepted", Toast.LENGTH_SHORT).show();
            showAdAndFinish();
        });

        btnRemindMe.setOnClickListener(v -> {
            Toast.makeText(this, "Reminder set", Toast.LENGTH_SHORT).show();
        });

        btnMessage.setOnClickListener(v -> {
            Toast.makeText(this, "Can't talk right now. I'll call you later.", Toast.LENGTH_LONG).show();
        });
    }

    private void showAdAndFinish() {
        stopMedia();
        
        if (MainActivity.mInterstitialAd != null) {
            MainActivity.mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Ad dismissed.");
                    MainActivity.mInterstitialAd = null;
                    finish();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    Log.e(TAG, "Ad failed to show: " + adError.getMessage());
                    MainActivity.mInterstitialAd = null;
                    finish();
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    Log.d(TAG, "Ad showed.");
                }
            });
            
            // Show ad with a slight delay for better UX
            new Handler().postDelayed(() -> {
                if (MainActivity.mInterstitialAd != null) {
                    MainActivity.mInterstitialAd.show(FakeCallActivity.this);
                } else {
                    finish();
                }
            }, 500);
        } else {
            finish();
        }
    }

    private void startAnimations() {
        Animation pulseAnim = AnimationUtils.loadAnimation(this, R.anim.pulse);
        if (pulseAccept != null) {
            pulseAccept.startAnimation(pulseAnim);
        }
        
        // Fade in entire screen content
        View root = findViewById(android.R.id.content);
        root.setAlpha(0f);
        root.animate().alpha(1f).setDuration(1000).start();
    }

    private void startRingtone() {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            mediaPlayer = MediaPlayer.create(this, notification);
            if (mediaPlayer != null) {
                mediaPlayer.setLooping(true);
                mediaPlayer.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startVibration() {
        try {
            vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                long[] pattern = {0, 1000, 1000};
                vibrator.vibrate(pattern, 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startFlashBlink() {
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraId = cameraManager.getCameraIdList()[0];
            flashHandler.post(flashRunnable);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Runnable flashRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                isFlashOn = !isFlashOn;
                cameraManager.setTorchMode(cameraId, isFlashOn);
                flashHandler.postDelayed(this, 500);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private void stopMedia() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception e) { }
            mediaPlayer = null;
        }
        if (vibrator != null) {
            try {
                vibrator.cancel();
            } catch (Exception e) { }
            vibrator = null;
        }
        
        flashHandler.removeCallbacks(flashRunnable);
        try {
            if (cameraId != null) {
                cameraManager.setTorchMode(cameraId, false);
            }
        } catch (Exception e) { }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        stopMedia();
        super.onDestroy();
    }
}
