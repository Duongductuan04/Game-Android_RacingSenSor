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
import java.util.Random;
import android.widget.Button;
import androidx.core.content.ContextCompat;
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
    private int selectedCarIndex = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        btnPause = findViewById(R.id.btnPause);
        btnPause.setOnClickListener(v -> togglePause());
        int carIndex = getIntent().getIntExtra(CarShopActivity.EXTRA_SELECTED_CAR, 1);

        isRandomMode = getIntent().getBooleanExtra("isRandomMode", false);
        gameMode = getIntent().getStringExtra("gameMode");
        selectedScore = getIntent().getIntExtra("selectedScore", 1000);
        selectedTimeInMillis = getIntent().getLongExtra("selectedTime", 60000);
        timeRemaining = selectedTimeInMillis;

        tvGameMode = findViewById(R.id.tvGameMode);
        tvScore = findViewById(R.id.tvScore);
        tvTime = findViewById(R.id.tvTime);
        gameView = findViewById(R.id.gameView);
        gameView.setSelectedCar(carIndex); // Đảm bảo xe được chọn được áp dụng
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
        // Nếu là chế độ random, chọn ngẫu nhiên gameMode
        if (isRandomMode && (gameMode == null || gameMode.equals("RANDOM"))) {
            String[] modes = {"SINGLE", "SCORE", "TIMED"};
            gameMode = modes[new Random().nextInt(modes.length)];

            // Thiết lập giá trị mặc định nếu cần
            if ("SCORE".equals(gameMode)) {
                selectedScore = getIntent().getIntExtra("selectedScore", 1000);
            } else if ("TIMED".equals(gameMode)) {
                selectedTimeInMillis = getIntent().getLongExtra("selectedTime", 60000);
                timeRemaining = selectedTimeInMillis;
            }
        }

        // Hiển thị thông tin chế độ chơi
        switch (gameMode) {
            case "SCORE":
                tvGameMode.setText(isRandomMode ? "RANDOM: SCORE MODE - Target: " + selectedScore
                        : "SCORE MODE - Target: " + selectedScore);
                tvTime.setVisibility(View.GONE);
                startScoreMode();
                break;
            case "TIMED":
                tvGameMode.setText(isRandomMode ? "RANDOM: TIMED MODE" : "TIMED MODE");
                tvTime.setVisibility(View.VISIBLE);
                startTimedMode();
                break;
            case "SINGLE":
                tvGameMode.setText(isRandomMode ? "RANDOM: SINGLE PLAYER" : "SINGLE PLAYER");
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
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_result, null);

        TextView tvResult = dialogView.findViewById(R.id.tvResult);
        TextView tvScore = dialogView.findViewById(R.id.tvScoreValue); // Đổi ID
        TextView tvTime = dialogView.findViewById(R.id.tvTimeValue); // Đổi ID
        Button btnHome = dialogView.findViewById(R.id.btnReturnHome); // Đổi ID
        Button btnRematch = dialogView.findViewById(R.id.btnPlayAgain); // Đổi ID

        // Thiết lập nội dung
        tvResult.setText("YOU LOSE!");
        tvResult.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
        tvScore.setText("Score: " + finalScore);

        if (gameMode.equals("TIMED")) {
            long timePlayed = selectedTimeInMillis - timeRemaining;
            String timeText = formatTime(timePlayed);
            tvTime.setText("Time: " + timeText);
            tvTime.setVisibility(View.VISIBLE);
        }

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
            startActivity(getIntent());

        });

        dialog.show();
    }
    private void showWinDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_result, null);

        TextView tvResult = dialogView.findViewById(R.id.tvResult);
        TextView tvScore = dialogView.findViewById(R.id.tvScoreValue);
        TextView tvTime = dialogView.findViewById(R.id.tvTimeValue);
        Button btnHome = dialogView.findViewById(R.id.btnReturnHome);
        Button btnRematch = dialogView.findViewById(R.id.btnPlayAgain);

        // Thiết lập nội dung
        tvResult.setText("YOU WIN!");
        tvResult.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        tvScore.setText("Score: " + score);

        // Hiển thị thời gian nếu là chế độ TIMED
        if (gameMode.equals("TIMED")) {
            long timePlayed = selectedTimeInMillis - timeRemaining;
            String timeText = formatTime(timePlayed);
            tvTime.setText("Time: " + timeText);
            tvTime.setVisibility(View.VISIBLE);
        } else {
            tvTime.setVisibility(View.GONE);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);

        btnHome.setOnClickListener(v -> {
            dialog.dismiss();
            finish(); // Quay về màn hình chính
        });

        btnRematch.setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(getIntent());

        });

        dialog.show();
    }

    private String formatTime(long millis) {
        int seconds = (int) (millis / 1000) % 60;
        int minutes = (int) ((millis / (1000 * 60)) % 60);
        return String.format("%02d:%02d", minutes, seconds);
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