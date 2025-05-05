package com.example.racingsensor;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SettingsActivity extends AppCompatActivity {

    private TextView tvUserEmail, tvUsername;
    private SeekBar sbMusicVolume, sbEffectsVolume;
    private SwitchMaterial switchVibration, switchNotifications;
    private MaterialButton btnLogout;
    private FirebaseAuth mAuth;
    private SharedPreferences sharedPreferences;
    private Button btnBack; // Thêm nút "Trở về"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();
        sharedPreferences = getSharedPreferences("GameSettings", MODE_PRIVATE);

        // Ánh xạ view
        initViews();

        // Hiển thị thông tin người dùng
        displayUserInfo();

        // Tải cài đặt đã lưu
        loadSettings();

        // Thiết lập sự kiện
        setupListeners();
    }

    private void initViews() {
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvUsername = findViewById(R.id.tvUsername);
        sbMusicVolume = findViewById(R.id.sbMusicVolume);
        sbEffectsVolume = findViewById(R.id.sbEffectsVolume);
        switchVibration = findViewById(R.id.switchVibration);
        switchNotifications = findViewById(R.id.switchNotifications);
        btnLogout = findViewById(R.id.btnLogout);
        btnBack = findViewById(R.id.btnBack); // Ánh xạ nút "Trở về"
    }

    private void displayUserInfo() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            tvUserEmail.setText(currentUser.getEmail());
            // Lấy tên hiển thị nếu có
            String displayName = currentUser.getDisplayName();
            tvUsername.setText(displayName != null ? displayName : "Racer");
        }
    }

    private void loadSettings() {
        // Âm lượng nhạc nền (mặc định 70)
        int musicVolume = sharedPreferences.getInt("music_volume", 70);
        sbMusicVolume.setProgress(musicVolume);

        // Âm lượng hiệu ứng (mặc định 80)
        int effectsVolume = sharedPreferences.getInt("effects_volume", 80);
        sbEffectsVolume.setProgress(effectsVolume);

        // Cài đặt rung (mặc định true)
        boolean vibrationEnabled = sharedPreferences.getBoolean("vibration", true);
        switchVibration.setChecked(vibrationEnabled);

        // Thông báo (mặc định true)
        boolean notificationsEnabled = sharedPreferences.getBoolean("notifications", true);
        switchNotifications.setChecked(notificationsEnabled);
    }

    private void setupListeners() {
        // Xử lý thay đổi âm lượng nhạc
        sbMusicVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Lưu cài đặt
                sharedPreferences.edit().putInt("music_volume", progress).apply();
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Xử lý thay đổi âm lượng hiệu ứng
        sbEffectsVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                sharedPreferences.edit().putInt("effects_volume", progress).apply();
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Xử lý chuyển đổi rung
        switchVibration.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("vibration", isChecked).apply();
        });

        // Xử lý chuyển đổi thông báo
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("notifications", isChecked).apply();
            if (isChecked) {
                // Bật thông báo
            } else {
                // Tắt thông báo
            }
        });

        // Sự kiện đăng xuất
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(SettingsActivity.this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(SettingsActivity.this, LoginActivity.class));
            finishAffinity(); // Đóng tất cả activity
        });

        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // Optionally call finish() to close SettingsActivity
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Lưu tất cả cài đặt khi activity tạm dừng
        saveAllSettings();
    }

    private void saveAllSettings() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("music_volume", sbMusicVolume.getProgress());
        editor.putInt("effects_volume", sbEffectsVolume.getProgress());
        editor.putBoolean("vibration", switchVibration.isChecked());
        editor.putBoolean("notifications", switchNotifications.isChecked());
        editor.apply();
    }
}
