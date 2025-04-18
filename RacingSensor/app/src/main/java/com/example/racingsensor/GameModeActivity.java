package com.example.racingsensor;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class GameModeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_mode);

        // Nhận tham số từ MainActivity
        boolean isRandom = getIntent().getBooleanExtra("isRandomMode", false);

        Button btnScoreMode = findViewById(R.id.btnScoreMode);
        Button btnTimedMode = findViewById(R.id.btnTimedMode);
        Button btnSinglePlayer = findViewById(R.id.btnSinglePlayer);

        btnScoreMode.setOnClickListener(v -> {
            Intent intent = new Intent(this, ScoreSelectionActivity.class);
            intent.putExtra("isRandomMode", isRandom);
            startActivity(intent);
        });

        btnTimedMode.setOnClickListener(v -> {
            Intent intent = new Intent(this, TimedSelectionActivity.class);
            intent.putExtra("isRandomMode", isRandom);
            startActivity(intent);
        });

        btnSinglePlayer.setOnClickListener(v -> startGame(isRandom, "SINGLE"));
    }

    private void startGame(boolean isRandom, String mode) {
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("isRandomMode", isRandom);
        intent.putExtra("gameMode", mode);
        startActivity(intent);
    }
}