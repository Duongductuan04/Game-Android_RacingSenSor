package com.example.racingsensor;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class ScoreSelectionActivity extends AppCompatActivity {

    private int selectedScore = 500; // Mặc định là 500

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_score_selection);

        // Nhận tham số từ GameModeActivity
        boolean isRandom = getIntent().getBooleanExtra("isRandomMode", false);

        // Ánh xạ các nút SCORE
        Button btnScore500 = findViewById(R.id.btnScore500);
        Button btnScore1000 = findViewById(R.id.btnScore1000);
        Button btnScore2000 = findViewById(R.id.btnScore2000);
        Button btnScore5000 = findViewById(R.id.btnScore5000);

        // Ánh xạ các nút Invite Friends và Single Player
        Button btnInviteFriends = findViewById(R.id.btnInviteFriends);
        Button btnSinglePlayer = findViewById(R.id.btnSinglePlayer);

        // Xử lý chọn điểm số
        btnScore500.setOnClickListener(v -> {
            selectedScore = 500;
            startGame(isRandom, "SCORE");
        });
        btnScore1000.setOnClickListener(v -> {
            selectedScore = 1000;
            startGame(isRandom, "SCORE");
        });
        btnScore2000.setOnClickListener(v -> {
            selectedScore = 2000;
            startGame(isRandom, "SCORE");
        });
        btnScore5000.setOnClickListener(v -> {
            selectedScore = 5000;
            startGame(isRandom, "SCORE");
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
            selectedScore = 1000; // Giá trị mặc định cho chế độ SINGLE
            startGame(isRandom, "SINGLE");
        });
    }

    private void startGame(boolean isRandom, String mode) {
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("isRandomMode", isRandom);
        intent.putExtra("gameMode", mode);
        intent.putExtra("selectedScore", selectedScore);
        startActivity(intent);
    }
}