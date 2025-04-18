package com.example.racingsensor;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class TimedSelectionActivity extends AppCompatActivity {

    private long selectedTimeInMillis = 45000; // Mặc định là 45 giây
    private int selectedCarIndex; // Thêm biến lưu xe được chọn

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timed_selection);

        // Nhận tham số từ GameModeActivity
        boolean isRandom = getIntent().getBooleanExtra("isRandomMode", false);
        selectedCarIndex = getIntent().getIntExtra(CarShopActivity.EXTRA_SELECTED_CAR, 1);
        // Ánh xạ các nút TIMED
        Button btnTimed45s = findViewById(R.id.btnTimed45s);
        Button btnTimed60s = findViewById(R.id.btnTimed60s);
        Button btnTimed90s = findViewById(R.id.btnTimed90s);

        // Ánh xạ các nút Invite Friends và Single Player
        Button btnInviteFriends = findViewById(R.id.btnInviteFriends);
        Button btnSinglePlayer = findViewById(R.id.btnSinglePlayer);

        // Xử lý chọn thời gian
        btnTimed45s.setOnClickListener(v -> {
            selectedTimeInMillis = 45000; // 45 giây
            startGame(isRandom, "TIMED");
        });
        btnTimed60s.setOnClickListener(v -> {
            selectedTimeInMillis = 60000; // 60 giây
            startGame(isRandom, "TIMED");
        });
        btnTimed90s.setOnClickListener(v -> {
            selectedTimeInMillis = 90000; // 90 giây
            startGame(isRandom, "TIMED");
        });

        // Xử lý nút Invite Friends
        btnInviteFriends.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Invite Friends")
                    .setMessage("Send invitations to your friends to play together")
                    .setPositiveButton("OK", null)
                    .show();
        });

        // Xử lý nút Single Player
        btnSinglePlayer.setOnClickListener(v -> {
            selectedTimeInMillis = 60000;
            // Giá trị mặc định cho chế độ SINGLE (60 giây)
            startGame(isRandom, "SINGLE");
        });
    }

    private void startGame(boolean isRandom, String mode) {
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("isRandomMode", isRandom);
        intent.putExtra("gameMode", mode);
        intent.putExtra("selectedTime", selectedTimeInMillis);
        intent.putExtra(CarShopActivity.EXTRA_SELECTED_CAR, selectedCarIndex); // Truyền xe đã chọn

        startActivity(intent);
    }
}