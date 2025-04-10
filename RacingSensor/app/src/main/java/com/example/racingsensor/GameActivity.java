package com.example.racingsensor;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
public class GameActivity extends AppCompatActivity {

    private TextView tvGameMode, tvScore, tvTime;
    private int score = 0;
    private boolean isRandomMode;
    private String gameMode;

    // Biến cho chế độ tính giờ
    private CountDownTimer countDownTimer;
    private long timeLeftInMillis = 60000; // 1 phút

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // Nhận tham số từ Intent
        isRandomMode = getIntent().getBooleanExtra("isRandomMode", false);
        gameMode = getIntent().getStringExtra("gameMode");

        // Ánh xạ view
        tvGameMode = findViewById(R.id.tvGameMode);
        tvScore = findViewById(R.id.tvScore);
        tvTime = findViewById(R.id.tvTime);

        // Thiết lập giao diện ban đầu
        setupGameMode();
    }

    private void setupGameMode() {
        switch(gameMode) {
            case "SCORE":
                tvGameMode.setText("SCORE MODE");
                tvTime.setVisibility(View.GONE);
                startScoreMode();
                break;

            case "TIMED":
                tvGameMode.setText("TIMED MODE");
                tvTime.setVisibility(View.VISIBLE);
                startTimedMode();
                break;

            case "SINGLE":
                tvGameMode.setText("SINGLE PLAYER");
                tvTime.setVisibility(View.GONE);
                startSinglePlayerMode();
                break;
        }
    }

    private void startScoreMode() {
        // Logic cho chế độ chơi theo điểm
        // Ví dụ: Tăng điểm khi vượt qua chướng ngại vật
        // score++;
        // tvScore.setText("Score: " + score);
    }

    private void startTimedMode() {
        // Hiển thị thời gian ban đầu
        updateTimerDisplay();

        // Khởi tạo bộ đếm ngược
        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateTimerDisplay();
            }

            @Override
            public void onFinish() {
                // Kết thúc game khi hết giờ
                tvTime.setText("TIME'S UP!");
            }
        }.start();
    }

    private void updateTimerDisplay() {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;
        String timeLeft = String.format("%02d:%02d", minutes, seconds);
        tvTime.setText(timeLeft);
    }

    private void startSinglePlayerMode() {
        // Logic cho chế độ chơi đơn
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}