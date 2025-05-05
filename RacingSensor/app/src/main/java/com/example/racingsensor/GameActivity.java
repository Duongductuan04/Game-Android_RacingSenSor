package com.example.racingsensor;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.Random;

public class GameActivity extends AppCompatActivity implements SensorEventListener {

    // UI Components
    private TextView tvGameMode, tvScore, tvTime;
    private GameView gameView;
    private ImageButton btnPause;

    // Game state
    private int score = 0;
    private boolean isRandomMode;
    private String gameMode;
    private int selectedScore;
    private long selectedTimeInMillis;
    private long timeRemaining;
    private boolean isPaused = false;
    private boolean isGameOver = false;
    private CountDownTimer countDownTimer;

    // Sensors
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private Sensor lightSensor;
    private float[] accelerometerValues = new float[3];
    private float[] gyroscopeValues = new float[3];
    private float currentLightLevel = -1;
    private static final float FILTER_ALPHA = 0.15f;
    private static final float LIGHT_THRESHOLD = 20f;
    private long lastSensorUpdateTime = 0;
    private static final int SENSOR_DELAY_MICROS = 10000;

    // Game controls
    private static final float TILT_THRESHOLD = 1.5f;
    private static final float TILT_DEADZONE = 0.3f;
    private int currentLane = 1;
    private boolean isChangingLane = false;

    // Vibration
    private Vibrator vibrator;
    private boolean hasVibrated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        initializeViews();
        setupSensors();
        loadGameSettings();
        setupGameMode();
    }

    private void initializeViews() {
        tvGameMode = findViewById(R.id.tvGameMode);
        tvScore = findViewById(R.id.tvScore);
        tvTime = findViewById(R.id.tvTime);
        gameView = findViewById(R.id.gameView);
        btnPause = findViewById(R.id.btnPause);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        btnPause.setOnClickListener(v -> togglePause());

        gameView.setGameListener(new GameView.GameListener() {
            @Override
            public void onScoreUpdated(int score) {
                GameActivity.this.score = score;
                updateScoreDisplay();

                if (gameMode.equals("SCORE") && score >= selectedScore) {
                    endGame();
                    showWinDialog();
                }
            }

            @Override
            public void onGameOver(int finalScore) {
                endGame();
                showGameOverDialog(finalScore);
            }

            @Override
            public long getRemainingTime() {
                return timeRemaining;
            }
        });
    }

    private void setupSensors() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        }
    }

    private void loadGameSettings() {
        int carIndex = getIntent().getIntExtra(CarShopActivity.EXTRA_SELECTED_CAR, 1);
        gameView.setSelectedCar(carIndex);

        isRandomMode = getIntent().getBooleanExtra("isRandomMode", false);
        gameMode = getIntent().getStringExtra("gameMode");
        selectedScore = getIntent().getIntExtra("selectedScore", 1000);
        selectedTimeInMillis = getIntent().getLongExtra("selectedTime", 60000);
        timeRemaining = selectedTimeInMillis;
    }

    private void setupGameMode() {
        if (isRandomMode && (gameMode == null || gameMode.equals("RANDOM"))) {
            String[] modes = {"SINGLE", "SCORE", "TIMED"};
            gameMode = modes[new Random().nextInt(modes.length)];
        }

        switch (gameMode) {
            case "SCORE":
                tvGameMode.setText(isRandomMode ?
                        "RANDOM: SCORE MODE - Target: " + selectedScore :
                        "SCORE MODE - Target: " + selectedScore);
                tvTime.setVisibility(View.GONE);
                startScoreMode();
                break;
            case "TIMED":
                tvGameMode.setText(isRandomMode ? "RANDOM: TIMED MODE" : "TIMED MODE");
                tvTime.setVisibility(View.VISIBLE);
                startTimedMode();
                break;
            default:
                tvGameMode.setText(isRandomMode ? "RANDOM: SINGLE PLAYER" : "SINGLE PLAYER");
                tvTime.setVisibility(View.GONE);
                startSinglePlayerMode();
                break;
        }

        gameView.setGameMode(gameMode);
    }

    private void startScoreMode() {
        updateScoreDisplay();
    }

    private void startTimedMode() {
        updateTimerDisplay();

        countDownTimer = new CountDownTimer(timeRemaining, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeRemaining = millisUntilFinished;
                updateTimerDisplay();
            }

            @Override
            public void onFinish() {
                runOnUiThread(() -> {
                    tvTime.setText("TIME'S UP!");
                    if (!isGameOver) {
                        endGame();
                        showGameOverDialog(score);
                    }
                });
            }
        }.start();
    }

    private void startSinglePlayerMode() {
        updateScoreDisplay();
    }

    private void updateScoreDisplay() {
        runOnUiThread(() -> {
            if (!isDestroyed()) {
                if (gameView.isDoubleScoreActive()) {
                    tvScore.setTextColor(Color.YELLOW);
                    tvScore.setScaleX(1.2f);
                    tvScore.setScaleY(1.2f);
                } else {
                    tvScore.setTextColor(gameView.isNightMode() ? Color.WHITE : Color.BLACK);
                    tvScore.setScaleX(1f);
                    tvScore.setScaleY(1f);
                }

                tvScore.setText("Score: " + score +
                        (gameMode.equals("SCORE") ? " / " + selectedScore : ""));
            }
        });
    }

    private void updateTimerDisplay() {
        runOnUiThread(() -> {
            if (!isDestroyed()) {
                int minutes = (int) (timeRemaining / 1000) / 60;
                int seconds = (int) (timeRemaining / 1000) % 60;
                tvTime.setText(String.format("%02d:%02d", minutes, seconds));

                if (timeRemaining <= 10000) {
                    tvTime.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light));
                    if (!hasVibrated && vibrator.hasVibrator()) {
                        vibrator.vibrate(200);
                        hasVibrated = true;
                    }
                } else {
                    tvTime.setTextColor(gameView.isNightMode() ?
                            Color.WHITE :
                            ContextCompat.getColor(this, android.R.color.holo_orange_dark));
                }
            }
        });
    }

    private void togglePause() {
        if (isPaused) {
            resumeGame();
        } else {
            pauseGame();
        }
    }

    private void pauseGame() {
        isPaused = true;
        gameView.setPaused(true);
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        sensorManager.unregisterListener(this);
        btnPause.setImageResource(R.drawable.ic_play);
    }

    private void resumeGame() {
        isPaused = false;
        gameView.setPaused(false);
        registerSensors();
        if (gameMode.equals("TIMED")) {
            startTimedMode();
        }
        btnPause.setImageResource(R.drawable.ic_pause);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isPaused) {
            registerSensors();
            if (gameMode.equals("TIMED") && !isGameOver) {
                startTimedMode();
            }
        }
    }

    private void registerSensors() {
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SENSOR_DELAY_MICROS);
        }
        if (gyroscope != null) {
            sensorManager.registerListener(this, gyroscope, SENSOR_DELAY_MICROS);
        }
        if (lightSensor != null) {
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        if (vibrator != null) {
            vibrator.cancel();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long now = System.currentTimeMillis();

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                accelerometerValues[0] = accelerometerValues[0] * (1 - FILTER_ALPHA) + event.values[0] * FILTER_ALPHA;
                accelerometerValues[1] = accelerometerValues[1] * (1 - FILTER_ALPHA) + event.values[1] * FILTER_ALPHA;
                accelerometerValues[2] = accelerometerValues[2] * (1 - FILTER_ALPHA) + event.values[2] * FILTER_ALPHA;
                break;

            case Sensor.TYPE_GYROSCOPE:
                gyroscopeValues[0] = gyroscopeValues[0] * (1 - FILTER_ALPHA) + event.values[0] * FILTER_ALPHA;
                gyroscopeValues[1] = gyroscopeValues[1] * (1 - FILTER_ALPHA) + event.values[1] * FILTER_ALPHA;
                gyroscopeValues[2] = gyroscopeValues[2] * (1 - FILTER_ALPHA) + event.values[2] * FILTER_ALPHA;
                break;

            case Sensor.TYPE_LIGHT:
                currentLightLevel = event.values[0];
                gameView.setNightMode(currentLightLevel < LIGHT_THRESHOLD);
                updateUIForNightMode();
                break;
        }

        if (!isChangingLane) {
            float tiltX = accelerometerValues[0];

            if (tiltX < -TILT_THRESHOLD && Math.abs(tiltX) > TILT_DEADZONE) {
                if (currentLane > 0) {
                    currentLane--;
                    isChangingLane = true;
                    startLaneChangeAnimation(currentLane);
                }
            } else if (tiltX > TILT_THRESHOLD && Math.abs(tiltX) > TILT_DEADZONE) {
                if (currentLane < 3) {
                    currentLane++;
                    isChangingLane = true;
                    startLaneChangeAnimation(currentLane);
                }
            }
        }

        if (now - lastSensorUpdateTime >= 16) {
            lastSensorUpdateTime = now;
        }
    }

    private void updateUIForNightMode() {
        runOnUiThread(() -> {
            int textColor = gameView.isNightMode() ? Color.WHITE : Color.BLACK;
            int bgColor = gameView.isNightMode() ? Color.argb(200, 0, 0, 0) : Color.argb(200, 255, 255, 255);

            tvScore.setTextColor(textColor);
            tvTime.setTextColor(gameView.isNightMode() ? Color.YELLOW : Color.BLUE);
            tvGameMode.setTextColor(textColor);

            // Cập nhật background header dựa trên chế độ
            findViewById(R.id.header).setBackgroundColor(bgColor);
        });
    }
    private void startLaneChangeAnimation(final int targetLane) {
        new CountDownTimer(300, 16) {
            public void onTick(long millisUntilFinished) {
                gameView.updateCarPosition(targetLane);
            }

            public void onFinish() {
                isChangingLane = false;
                gameView.updateCarPosition(targetLane);
            }
        }.start();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
    }

    private void endGame() {
        isGameOver = true;
        gameView.setGameOver(true);
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        sensorManager.unregisterListener(this);
        saveHighScore();
    }

    private void saveHighScore() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                DatabaseHelper dbHelper = new DatabaseHelper(GameActivity.this);
                dbHelper.addHighScore(score, gameMode);
                dbHelper.close();
                return null;
            }
        }.execute();
    }

    private void showGameOverDialog(int finalScore) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_result, null);
        setupResultDialog(dialogView, "GAME OVER",
                android.R.color.holo_red_dark, finalScore, true);
    }

    private void showWinDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_result, null);
        setupResultDialog(dialogView, "YOU WIN!",
                android.R.color.holo_green_dark, score, false);
    }

    private void setupResultDialog(View dialogView, String title, int colorRes, int score, boolean isGameOver) {
        TextView tvResult = dialogView.findViewById(R.id.tvResult);
        TextView tvScore = dialogView.findViewById(R.id.tvScoreValue);
        TextView tvTime = dialogView.findViewById(R.id.tvTimeValue);
        Button btnHome = dialogView.findViewById(R.id.btnReturnHome);
        Button btnRematch = dialogView.findViewById(R.id.btnPlayAgain);
        Button btnLeaderboard = dialogView.findViewById(R.id.btnLeaderboard);

        tvResult.setText(title);
        tvResult.setTextColor(ContextCompat.getColor(this, colorRes));
        tvScore.setText("Score: " + score);

        if (gameMode.equals("TIMED")) {
            long timePlayed = selectedTimeInMillis - timeRemaining;
            tvTime.setText("Time: " + formatTime(timePlayed));
            tvTime.setVisibility(View.VISIBLE);
        } else {
            tvTime.setVisibility(View.GONE);
        }

        btnLeaderboard.setVisibility(View.VISIBLE);
        btnLeaderboard.setOnClickListener(v -> {
            startActivity(new Intent(this, LeaderboardActivity.class));
            finish();
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);

        btnHome.setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });

        btnRematch.setOnClickListener(v -> {
            dialog.dismiss();
            recreate();
        });

        dialog.show();
    }

    private String formatTime(long millis) {
        int seconds = (int) (millis / 1000) % 60;
        int minutes = (int) ((millis / (1000 * 60)) % 60);
        return String.format("%02d:%02d", minutes, seconds);
    }
}