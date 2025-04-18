package com.example.racingsensor;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class GameModeActivity extends AppCompatActivity {

    private int selectedCarIndex; // Thêm biến lưu xe được chọn

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_mode);

        // Nhận cả 2 tham số từ CarShopActivity
        boolean isRandom = getIntent().getBooleanExtra("isRandomMode", false);
        selectedCarIndex = getIntent().getIntExtra(CarShopActivity.EXTRA_SELECTED_CAR, 1);

        Button btnScoreMode = findViewById(R.id.btnScoreMode);
        Button btnTimedMode = findViewById(R.id.btnTimedMode);
        Button btnSinglePlayer = findViewById(R.id.btnSinglePlayer);

        btnScoreMode.setOnClickListener(v -> {
            Intent intent = new Intent(this, ScoreSelectionActivity.class);
            intent.putExtra("isRandomMode", isRandom);
            intent.putExtra(CarShopActivity.EXTRA_SELECTED_CAR, selectedCarIndex); // Thêm xe đã chọn
            startActivity(intent);
        });

        btnTimedMode.setOnClickListener(v -> {
            Intent intent = new Intent(this, TimedSelectionActivity.class);
            intent.putExtra("isRandomMode", isRandom);
            intent.putExtra(CarShopActivity.EXTRA_SELECTED_CAR, selectedCarIndex); // Thêm xe đã chọn
            startActivity(intent);
        });

        btnSinglePlayer.setOnClickListener(v -> startGame(isRandom, "SINGLE"));
    }

    private void startGame(boolean isRandom, String mode) {
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("isRandomMode", isRandom);
        intent.putExtra("gameMode", mode);
        intent.putExtra(CarShopActivity.EXTRA_SELECTED_CAR, selectedCarIndex); // Thêm xe đã chọn
        startActivity(intent);
    }
}