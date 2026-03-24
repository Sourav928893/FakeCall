package com.sourav.fakecall;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.materialswitch.MaterialSwitch;

public class SettingsActivity extends AppCompatActivity {

    private MaterialSwitch switchVibration, switchShake, switchFlash;
    private SharedPreferences sharedPreferences;
    
    private static final String PREFS_NAME = "FakeCallPrefs";
    private static final String KEY_VIBRATION = "vibration_enabled";
    private static final String KEY_SHAKE = "shake_enabled";
    private static final String KEY_FLASH = "flash_enabled";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        MaterialToolbar toolbar = findViewById(R.id.toolbarSettings);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Settings");
        }

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        switchVibration = findViewById(R.id.switchVibration);
        switchShake = findViewById(R.id.switchShake);
        switchFlash = findViewById(R.id.switchFlash);

        // Load saved settings
        switchVibration.setChecked(sharedPreferences.getBoolean(KEY_VIBRATION, true));
        switchShake.setChecked(sharedPreferences.getBoolean(KEY_SHAKE, false));
        switchFlash.setChecked(sharedPreferences.getBoolean(KEY_FLASH, false));

        // Listeners
        switchVibration.setOnCheckedChangeListener((buttonView, isChecked) -> 
            sharedPreferences.edit().putBoolean(KEY_VIBRATION, isChecked).apply());

        switchShake.setOnCheckedChangeListener((buttonView, isChecked) -> 
            sharedPreferences.edit().putBoolean(KEY_SHAKE, isChecked).apply());
            
        switchFlash.setOnCheckedChangeListener((buttonView, isChecked) -> 
            sharedPreferences.edit().putBoolean(KEY_FLASH, isChecked).apply());
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
