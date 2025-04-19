package com.example.racingsensor;
import java.util.Random;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView tvFuelValue;
    private Button btnPlay, btnInvitations, btnRandomPlay;
    private ImageView iconShop, iconNotification, iconSettings, iconLeaderboard;
    private int fuelAmount = 120;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Ánh xạ các view từ layout
        initViews();

        // Cập nhật hiển thị nhiên liệu
        updateFuelDisplay();

        // Thiết lập sự kiện click
        setupClickListeners();
    }

    private void initViews() {
        tvFuelValue = findViewById(R.id.tvFuelValue);
        btnPlay = findViewById(R.id.btnPlay);
        btnInvitations = findViewById(R.id.btnInvitations);
        btnRandomPlay = findViewById(R.id.btnRandomPlay);

        // Ánh xạ các icon điều hướng
        iconShop = findViewById(R.id.btnShop);
        iconNotification = findViewById(R.id.btnNotification);
        iconSettings = findViewById(R.id.btnSettings);
        iconLeaderboard = findViewById(R.id.btnLeaderboard);
    }

    private void setupClickListeners() {
        // Sự kiện cho các nút chính
        btnPlay.setOnClickListener(v -> startGameMode(false));
        btnRandomPlay.setOnClickListener(v -> startGameMode(true));
        btnInvitations.setOnClickListener(v -> showInvitationsDialog());

        // Sự kiện cho nút cửa hàng
        iconShop.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CarShopActivity.class);
            startActivity(intent);
        });
        // Sự kiện cho nút cửa hàng
        iconLeaderboard.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LeaderboardActivity.class);
            startActivity(intent);
        });

        // Các sự kiện khác có thể thêm ở đây
        iconNotification.setOnClickListener(v -> showNotifications());
    }

    private void startGameMode(boolean isRandom) {
        if (fuelAmount > 0) {
            if (isRandom) {
                // Chế độ random - chọn ngẫu nhiên chế độ chơi
                startRandomGameMode();
            } else {
                // Chế độ bình thường - vào màn hình chọn chế độ
                Intent intent = new Intent(this, GameModeActivity.class);
                intent.putExtra("isRandomMode", false);
                startActivity(intent);
            }

            // Giảm nhiên liệu khi bắt đầu chơi
            fuelAmount -= 10;
            updateFuelDisplay();
        } else {
            showAlert("Not enough fuel", "Please refuel to continue playing");
        }
    }

    private void startRandomGameMode() {
        // Khởi tạo đối tượng Random
        Random random = new Random();

        // Tạo số ngẫu nhiên từ 0-2
        int randomMode = random.nextInt(3);

        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("isRandomMode", true);

        // Thiết lập tham số theo chế độ ngẫu nhiên
        switch (randomMode) {
            case 0: // SINGLE
                intent.putExtra("gameMode", "SINGLE");
                break;
            case 1: // SCORE
                intent.putExtra("gameMode", "SCORE");
                intent.putExtra("selectedScore", 1000); // Giá trị mặc định
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
                .setTitle("Invite Friends")
                .setMessage("Send invitations to your friends to play together")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showNotifications() {
        // Hiển thị thông báo
        showAlert("Notifications", "No new notifications");
    }

    private void showAlert(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
}