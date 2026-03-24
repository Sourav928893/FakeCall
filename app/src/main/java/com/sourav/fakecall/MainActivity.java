package com.sourav.fakecall;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.sourav.fakecall.adapters.ScheduledCallAdapter;
import com.sourav.fakecall.models.ScheduledCall;
import com.sourav.fakecall.receiver.AlarmReceiver;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "MainActivity";
    private static final String BANNER_AD_UNIT_ID = "ca-app-pub-4041401840560784/7220566079";
    private static final String INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-4041401840560784/1539363552";

    private TextInputEditText etCallerName;
    private ChipGroup chipGroupDelay;
    private TextView tvSelectedDelay, tvNoSchedules;
    private MaterialButton btnInstantCall, btnScheduleCall;
    private RecyclerView rvScheduledCalls;
    private ScheduledCallAdapter adapter;
    private List<ScheduledCall> scheduledCallsList = new ArrayList<>();
    private SharedPreferences sharedPreferences;

    private SensorManager sensorManager;
    private float acceleration;
    private float currentAcceleration;
    private float lastAcceleration;

    private static final String PREFS_NAME = "FakeCallPrefs";
    private static final String KEY_CALLER_NAME = "caller_name";
    private static final String KEY_SHAKE = "shake_enabled";
    private static final String KEY_SCHEDULES = "scheduled_calls";

    private long selectedDelayMillis = 5000;

    // AdMob members
    private FrameLayout adContainerView;
    private AdView adView;
    public static InterstitialAd mInterstitialAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        initViews();
        setupDelayChips();
        setupRecyclerView();
        setupShakeDetector();
        loadData();

        // Initialize Mobile Ads SDK
        MobileAds.initialize(this, initializationStatus -> {
            loadBanner();
            loadInterstitial();
        });

        btnInstantCall.setOnClickListener(v -> startCallWithDelay());
        btnScheduleCall.setOnClickListener(v -> showDateTimePicker());
        
        findViewById(R.id.fabSettings).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        });
    }

    private void initViews() {
        etCallerName = findViewById(R.id.etCallerName);
        chipGroupDelay = findViewById(R.id.chipGroupDelay);
        tvSelectedDelay = findViewById(R.id.tvSelectedDelay);
        tvNoSchedules = findViewById(R.id.tvNoSchedules);
        btnInstantCall = findViewById(R.id.btnInstantCall);
        btnScheduleCall = findViewById(R.id.btnScheduleCall);
        rvScheduledCalls = findViewById(R.id.rvScheduledCalls);
        adContainerView = findViewById(R.id.adContainerView);
    }

    private void loadBanner() {
        adView = new AdView(this);
        adView.setAdUnitId(BANNER_AD_UNIT_ID);
        adContainerView.removeAllViews();
        adContainerView.addView(adView);

        AdSize adSize = getAdSize();
        adView.setAdSize(adSize);

        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
        
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e(TAG, "Banner load failed: " + loadAdError.getMessage());
            }
        });
    }

    private AdSize getAdSize() {
        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        float density = outMetrics.density;
        float adWidthPixels = adContainerView.getWidth();

        if (adWidthPixels == 0) {
            adWidthPixels = outMetrics.widthPixels;
        }

        int adWidth = (int) (adWidthPixels / density);
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth);
    }

    public void loadInterstitial() {
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(this, INTERSTITIAL_AD_UNIT_ID, adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        mInterstitialAd = interstitialAd;
                        Log.i(TAG, "Interstitial loaded successfully");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.e(TAG, "Interstitial load failed: " + loadAdError.getMessage());
                        mInterstitialAd = null;
                    }
                });
    }

    private void setupDelayChips() {
        // Set default selection to 5s
        chipGroupDelay.check(R.id.chip5s);
        updateDelay(5, "5 seconds");

        chipGroupDelay.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chip5s) updateDelay(5, "5 seconds");
            else if (id == R.id.chip10s) updateDelay(10, "10 seconds");
            else if (id == R.id.chip30s) updateDelay(30, "30 seconds");
            else if (id == R.id.chip1m) updateDelay(60, "1 minute");
            else if (id == R.id.chip5m) updateDelay(300, "5 minutes");
            else if (id == R.id.chipCustom) showCustomTimePicker();
        });
    }

    private void updateDelay(int seconds, String label) {
        selectedDelayMillis = seconds * 1000L;
        tvSelectedDelay.setText("Selected: " + label);
    }

    private void showCustomTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            int totalSeconds = (hourOfDay * 3600) + (minute * 60);
            if (totalSeconds == 0) totalSeconds = 5;
            updateDelay(totalSeconds, hourOfDay + "m " + minute + "s");
        }, 0, 0, true);
        timePickerDialog.setTitle("Set Custom Delay (Min:Sec)");
        timePickerDialog.show();
    }

    private void setupRecyclerView() {
        adapter = new ScheduledCallAdapter(scheduledCallsList, this::cancelScheduledCall);
        rvScheduledCalls.setLayoutManager(new LinearLayoutManager(this));
        rvScheduledCalls.setAdapter(adapter);
    }

    private void startCallWithDelay() {
        String name = etCallerName.getText().toString().trim();
        if (name.isEmpty()) name = "The Future";
        saveData();

        // If 5s is selected, trigger immediately as requested
        long delay = selectedDelayMillis;
        if (chipGroupDelay.getCheckedChipId() == R.id.chip5s) {
            delay = 0;
            Toast.makeText(this, "Calling now...", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Call in " + (delay / 1000) + " seconds", Toast.LENGTH_SHORT).show();
        }
        
        long triggerTime = System.currentTimeMillis() + delay;
        scheduleAlarm(triggerTime, name, (int) (System.currentTimeMillis() % 10000));
    }

    private void showDateTimePicker() {
        Calendar current = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(Calendar.YEAR, year);
            selected.set(Calendar.MONTH, month);
            selected.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            new TimePickerDialog(this, (view1, hourOfDay, minute) -> {
                selected.set(Calendar.HOUR_OF_DAY, hourOfDay);
                selected.set(Calendar.MINUTE, minute);
                selected.set(Calendar.SECOND, 0);

                if (selected.getTimeInMillis() <= System.currentTimeMillis()) {
                    Toast.makeText(this, "Please select a future time", Toast.LENGTH_SHORT).show();
                    return;
                }

                addScheduledCall(etCallerName.getText().toString(), selected.getTimeInMillis());
            }, current.get(Calendar.HOUR_OF_DAY), current.get(Calendar.MINUTE), false).show();
        }, current.get(Calendar.YEAR), current.get(Calendar.MONTH), current.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void addScheduledCall(String name, long time) {
        if (name.isEmpty()) name = "The Future";
        int id = (int) (System.currentTimeMillis() / 1000);
        ScheduledCall call = new ScheduledCall(id, name, time, false);
        scheduledCallsList.add(call);
        saveSchedules();
        scheduleAlarm(time, name, id);
        adapter.notifyItemInserted(scheduledCallsList.size() - 1);
        updateUI();
    }

    private void scheduleAlarm(long triggerTime, String name, int id) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("CALLER_NAME", name);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, id, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (alarmManager != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            }
        }
    }

    private void cancelScheduledCall(ScheduledCall call) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, call.getId(), intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
        
        scheduledCallsList.remove(call);
        saveSchedules();
        adapter.notifyDataSetChanged();
        updateUI();
    }

    private void setupShakeDetector() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            sensorManager.registerListener(this,
                    sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
        acceleration = 10f;
        currentAcceleration = SensorManager.GRAVITY_EARTH;
        lastAcceleration = SensorManager.GRAVITY_EARTH;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (sharedPreferences.getBoolean(KEY_SHAKE, true)) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            lastAcceleration = currentAcceleration;
            currentAcceleration = (float) Math.sqrt(x * x + y * y + z * z);
            float delta = currentAcceleration - lastAcceleration;
            acceleration = acceleration * 0.9f + delta;

            if (acceleration > 12) {
                startCallWithDelay();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void loadData() {
        etCallerName.setText(sharedPreferences.getString(KEY_CALLER_NAME, "The Future"));
        loadSchedules();
    }

    private void saveData() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_CALLER_NAME, etCallerName.getText().toString());
        editor.apply();
    }

    private void saveSchedules() {
        JSONArray jsonArray = new JSONArray();
        for (ScheduledCall call : scheduledCallsList) {
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("id", call.getId());
                jsonObject.put("callerName", call.getCallerName());
                jsonObject.put("time", call.getTriggerTime());
                jsonObject.put("isCompleted", call.isRepeatDaily());
                jsonArray.put(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        sharedPreferences.edit().putString(KEY_SCHEDULES, jsonArray.toString()).apply();
    }

    private void loadSchedules() {
        String json = sharedPreferences.getString(KEY_SCHEDULES, null);
        if (json != null) {
            try {
                JSONArray jsonArray = new JSONArray(json);
                scheduledCallsList.clear();
                long currentTime = System.currentTimeMillis();
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    ScheduledCall call = new ScheduledCall(
                            obj.getInt("id"),
                            obj.getString("callerName"),
                            obj.getLong("time"),
                            obj.getBoolean("isCompleted")
                    );
                    // Only keep future schedules
                    if (call.getTriggerTime() > currentTime) {
                        scheduledCallsList.add(call);
                    }
                }
                adapter.notifyDataSetChanged();
                updateUI();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateUI() {
        if (scheduledCallsList.isEmpty()) {
            tvNoSchedules.setVisibility(View.VISIBLE);
            rvScheduledCalls.setVisibility(View.GONE);
        } else {
            tvNoSchedules.setVisibility(View.GONE);
            rvScheduledCalls.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }
}
