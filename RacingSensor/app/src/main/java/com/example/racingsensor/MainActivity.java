package com.example.racingsensor;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import java.util.Random;
public class MainActivity extends AppCompatActivity {

    private TextView tvFuelValue;
    private Button btnPlay, btnInvitations, btnRandomPlay;
    private ImageView iconShop, iconNotification, iconSettings, iconLeaderboard;
    private int fuelAmount = 120; // Số lượng nhiên liệu ban đầu

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Ánh xạ các view từ layout
        initViews();

        // Cập nhật hiển thị nhiên liệu
        updateFuelDisplay();

        // Thiết lập sự kiện click cho các nút
        setupClickListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Kiểm tra lại xem người dùng đã đăng nhập hay chưa. Nếu chưa thì chuyển tới LoginActivity
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish(); // Đảm bảo MainActivity không tiếp tục chạy
        }
    }

    private void initViews() {
        tvFuelValue = findViewById(R.id.tvFuelValue);
        btnPlay = findViewById(R.id.btnPlay);
        btnInvitations = findViewById(R.id.btnInvitations);
        btnRandomPlay = findViewById(R.id.btnRandomPlay);

        iconShop = findViewById(R.id.btnShop);
        iconNotification = findViewById(R.id.btnNotification);
        iconSettings = findViewById(R.id.btnSettings);
        iconLeaderboard = findViewById(R.id.btnLeaderboard);
    }

    private void setupClickListeners() {
        // Sự kiện cho các nút chơi
        btnPlay.setOnClickListener(v -> startGameMode(false));
        btnRandomPlay.setOnClickListener(v -> startGameMode(true));
        btnInvitations.setOnClickListener(v -> showInvitationsDialog());

        // Sự kiện cho các nút điều hướng
        iconShop.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, CarShopActivity.class)));
        iconLeaderboard.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, LeaderboardActivity.class)));
        iconSettings.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        });
        iconNotification.setOnClickListener(v -> showNotifications());
    }

    private void startGameMode(boolean isRandom) {
        if (fuelAmount > 0) {
            if (isRandom) {
                startRandomGameMode();
            } else {
                Intent intent = new Intent(this, GameModeActivity.class);
                intent.putExtra("isRandomMode", false);
                startActivity(intent);
            }
            // Giảm nhiên liệu khi bắt đầu chơi
            fuelAmount = Math.max(fuelAmount - 10, 0); // Đảm bảo nhiên liệu không bị âm
            updateFuelDisplay();
        } else {
            showAlert("Không đủ nhiên liệu", "Vui lòng tiếp nhiên liệu để tiếp tục chơi");
        }
    }

    private void startRandomGameMode() {
        Random random = new Random();
        int randomMode = random.nextInt(3);

        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("isRandomMode", true);

        // Thiết lập chế độ chơi ngẫu nhiên
        switch (randomMode) {
            case 0: // SINGLE
                intent.putExtra("gameMode", "SINGLE");
                break;
            case 1: // SCORE
                intent.putExtra("gameMode", "SCORE");
                intent.putExtra("selectedScore", 1000); // Điểm mặc định
                break;
            case 2: // TIMED
                intent.putExtra("gameMode", "TIMED");
                intent.putExtra("selectedTime", 60000); // 60 giây mặc định
                break;
        }

        startActivity(intent);
    }

    private void updateFuelDisplay() {
        tvFuelValue.setText(String.valueOf(fuelAmount));
    }

    private void showInvitationsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Mời bạn bè")
                .setMessage("Gửi lời mời cho bạn bè tham gia chơi cùng")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showNotifications() {
        showAlert("Thông báo", "Không có thông báo mới");
    }

    private void showAlert(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
}
