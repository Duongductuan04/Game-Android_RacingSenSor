package com.example.racingsensor;
import android.widget.ImageButton;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.os.Vibrator;

public class GameActivity extends AppCompatActivity implements GameView.GameListener, SensorEventListener {

    private TextView tvGameMode, tvScore, tvTime;
    private GameView gameView;
    private int score = 0;
    private boolean isRandomMode;
    private String gameMode;
    private int selectedScore;
    private long selectedTimeInMillis;
    private long timeRemaining;
    private long timerStartTime;
    private boolean isPaused = false; // Biến lưu trạng thái trò chơi (pause/resume)
    private ImageButton btnPause;
    private CountDownTimer countDownTimer;
    private boolean isGameOver = false;
    private Vibrator vibrator;
    private boolean hasVibrated = false;

    private SensorManager sensorManager;
    private Sensor accelerometer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        btnPause = findViewById(R.id.btnPause);
        btnPause.setOnClickListener(v -> togglePause());

        isRandomMode = getIntent().getBooleanExtra("isRandomMode", false);
        gameMode = getIntent().getStringExtra("gameMode");
        selectedScore = getIntent().getIntExtra("selectedScore", 1000);
        selectedTimeInMillis = getIntent().getLongExtra("selectedTime", 60000);
        timeRemaining = selectedTimeInMillis;

        tvGameMode = findViewById(R.id.tvGameMode);
        tvScore = findViewById(R.id.tvScore);
        tvTime = findViewById(R.id.tvTime);
        gameView = findViewById(R.id.gameView);

        gameView.setGameMode(gameMode);
        gameView.setGameListener(this);

        setupGameMode();
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
        gameView.setPaused(true);  // Tạm dừng trò chơi
        if (countDownTimer != null) {
            countDownTimer.cancel(); // Dừng bộ đếm thời gian
        }
        btnPause.setImageResource(R.drawable.ic_play);  // Chuyển nút Pause sang Resume
    }

    private void resumeGame() {
        isPaused = false;
        gameView.setPaused(false); // Tiếp tục trò chơi
        startTimedMode();  // Tiếp tục bộ đếm thời gian
        btnPause.setImageResource(R.drawable.ic_pause);  // Chuyển nút Resume sang Pause
    }

    private void setupGameMode() {
        switch (gameMode) {
            case "SCORE":
                tvGameMode.setText("SCORE MODE - Target: " + selectedScore);
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

            default:
                tvGameMode.setText("UNKNOWN MODE");
                tvTime.setVisibility(View.GONE);
                break;
        }
    }

    private void startScoreMode() {
        runOnUiThread(() -> tvScore.setText("Score: " + score + " / " + selectedScore));
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
                        isGameOver = true;
                        gameView.setGameOver(true);
                        showGameOverDialog(score);
                    }
                });
            }
        }.start();
        timerStartTime = System.currentTimeMillis();
    }

    private void updateTimerDisplay() {
        int minutes = (int) (timeRemaining / 1000) / 60;
        int seconds = (int) (timeRemaining / 1000) % 60;
        String timeLeft = String.format("%02d:%02d", minutes, seconds);
        runOnUiThread(() -> {
            tvTime.setText(timeLeft);
            if (timeRemaining <= 10000) {
                tvTime.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                if (!hasVibrated && vibrator.hasVibrator()) {
                    vibrator.vibrate(500);
                    hasVibrated = true;
                }
            } else {
                tvTime.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
            }
        });
    }

    private void startSinglePlayerMode() {
        runOnUiThread(() -> tvScore.setText("Score: " + score));
    }

    @Override
    public void onScoreUpdated(int score) {
        this.score = score;
        runOnUiThread(() -> {
            tvScore.setText("Score: " + score + (gameMode.equals("SCORE") ? " / " + selectedScore : ""));
            if (gameMode.equals("SCORE") && score >= selectedScore && !isGameOver) {
                isGameOver = true;
                gameView.setGameOver(true);
                showWinDialog();
            }
        });
    }

    @Override
    public void onGameOver(int finalScore) {
        isGameOver = true;
        runOnUiThread(() -> showGameOverDialog(finalScore));
    }

    private void showGameOverDialog(int finalScore) {
        new AlertDialog.Builder(this)
                .setTitle("Game Over")
                .setMessage("Your score: " + finalScore)
                .setPositiveButton("Play Again", (dialog, which) -> {
                    finish();
                    startActivity(getIntent());
                })
                .setNegativeButton("Exit", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    private void showWinDialog() {
        new AlertDialog.Builder(this)
                .setTitle("You Win!")
                .setMessage("You reached the target score: " + selectedScore)
                .setPositiveButton("Play Again", (dialog, which) -> {
                    finish();
                    startActivity(getIntent());
                })
                .setNegativeButton("Exit", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
        if (gameMode.equals("TIMED") && !isGameOver) {
            startTimedMode();
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
    public long getRemainingTime() {
        return timeRemaining;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float tiltX = -event.values[0]; // Đảo ngược tiltX để nghiêng trái di chuyển sang trái
            gameView.updateCarPosition(tiltX);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Không cần xử lý
    }
}