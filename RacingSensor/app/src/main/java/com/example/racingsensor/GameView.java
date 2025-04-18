package com.example.racingsensor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameView extends View {
    // Giữ nguyên các biến về xe và chướng ngại vật từ code gốc
    private Bitmap playerCarBitmap;
    private Bitmap obstacleBitmap;
    private int playerCarX, playerCarY;
    private int playerCarWidth, playerCarHeight;
    private int currentLane = 1;

    private int screenWidth, screenHeight;
    private int laneWidth;
    private int[] lanePositions = new int[4];

    private List<Obstacle> obstacles = new ArrayList<>();
    private Random random = new Random();
    private long lastObstacleSpawnTime;
    private static final long OBSTACLE_SPAWN_INTERVAL = 2000;

    private int score = 0;
    private Paint scorePaint;
    private boolean isGameOver = false;
    private Paint gameOverPaint;
    private String gameMode;

    // Các biến mới cho giao diện 4 làn đường
    private float dashOffset = 0f;
    private Paint lanePaint;
    private Paint roadBorderPaint;
    private Paint laneBackgroundPaint;
    private int[] laneColors;
    private GameListener gameListener;

    public interface GameListener {
        void onScoreUpdated(int score);

        void onGameOver(int finalScore);

        long getRemainingTime();
    }

    public GameView(Context context) {
        super(context);
        init();
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GameView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Khởi tạo các Paint cho giao diện mới
        scorePaint = new Paint();
        scorePaint.setColor(Color.WHITE);
        scorePaint.setTextSize(50);
        scorePaint.setAntiAlias(true);

        gameOverPaint = new Paint();
        gameOverPaint.setColor(Color.RED);
        gameOverPaint.setTextSize(80);
        gameOverPaint.setAntiAlias(true);

        // Paint cho đường kẻ làn
        lanePaint = new Paint();
        lanePaint.setColor(Color.YELLOW);
        lanePaint.setStrokeWidth(5);
        lanePaint.setStyle(Paint.Style.STROKE);
        lanePaint.setAntiAlias(true);

        // Paint cho viền đường
        roadBorderPaint = new Paint();
        roadBorderPaint.setColor(Color.WHITE);
        roadBorderPaint.setStrokeWidth(10);
        roadBorderPaint.setStyle(Paint.Style.STROKE);
        roadBorderPaint.setAntiAlias(true);

        // Paint cho nền các làn đường
        laneBackgroundPaint = new Paint();
        laneBackgroundPaint.setStyle(Paint.Style.FILL);


// Màu sắc cho các làn đường (chỉ có 2 màu xám)
        laneColors = new int[]{
                Color.rgb(169, 169, 169),    // Làn 1: Màu xám sáng
                Color.rgb(105, 105, 105),    // Làn 2: Màu xám tối
                Color.rgb(169, 169, 169),    // Làn 3: Màu xám sáng
                Color.rgb(105, 105, 105)     // Làn 4: Màu xám tối
        };
// Giữ nguyên cách tải hình ảnh xe từ code gốc, nhưng thay đổi kích thước
        try {
            playerCarBitmap = Bitmap.createScaledBitmap(
                    BitmapFactory.decodeResource(getResources(), R.drawable.car2),
                    220, 240, true); // Đã tăng kích thước lên 120x160
        } catch (Exception e) {
            Log.e("GameView", "Error loading car.png: " + e.getMessage());
            playerCarBitmap = Bitmap.createScaledBitmap(
                    BitmapFactory.decodeResource(getResources(), android.R.drawable.ic_menu_directions),
                    60, 80, true); // Đã tăng kích thước lên 120x160
        }

// Giữ nguyên cách tải hình ảnh chướng ngại vật từ code gốc, nhưng thay đổi kích thước
        try {
            obstacleBitmap = Bitmap.createScaledBitmap(
                    BitmapFactory.decodeResource(getResources(), R.drawable.obstacle),
                    60, 60, true); // Đã tăng kích thước lên 60x60
        } catch (Exception e) {
            Log.e("GameView", "Error loading obstacle.png: " + e.getMessage());
            obstacleBitmap = Bitmap.createScaledBitmap(
                    BitmapFactory.decodeResource(getResources(), android.R.drawable.ic_menu_close_clear_cancel),
                    60, 60, true); // Đã tăng kích thước lên 60x60
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        screenWidth = w;
        screenHeight = h;

        // Thiết lập vị trí xe (giữ nguyên từ code gốc)
        playerCarWidth = playerCarBitmap.getWidth();
        playerCarHeight = playerCarBitmap.getHeight();
        playerCarY = screenHeight - playerCarHeight - 70;

        // Tính toán các làn đường (4 làn)
        laneWidth = screenWidth / 4;
        for (int i = 0; i < 4; i++) {
            lanePositions[i] = i * laneWidth + laneWidth / 2 - playerCarWidth / 2;
        }
        currentLane = 1;
        playerCarX = lanePositions[currentLane];
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (isPaused) {
            // Draw "Paused" overlay
            Paint pausedPaint = new Paint();
            pausedPaint.setColor(Color.argb(180, 0, 0, 0)); // semi-transparent black
            canvas.drawRect(0, 0, screenWidth, screenHeight, pausedPaint);

            // Draw "Paused" text in the center
            Paint textPaint = new Paint();
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(80);
            textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("PAUSED", screenWidth / 2, screenHeight / 2, textPaint);
            return; // Do not proceed with the rest of the drawing
        }
        // Vẽ nền các làn đường
        for (int i = 0; i < 4; i++) {
            laneBackgroundPaint.setColor(laneColors[i]);
            canvas.drawRect(i * laneWidth, 0, (i + 1) * laneWidth, screenHeight, laneBackgroundPaint);
        }

        // Vẽ đường kẻ làn (nét đứt)
        lanePaint.setPathEffect(new DashPathEffect(new float[]{20, 20}, dashOffset));
        Path path = new Path();
        for (int i = 1; i < 4; i++) {
            float x = i * laneWidth;
            path.reset();
            path.moveTo(x, 0);
            path.lineTo(x, screenHeight);
            canvas.drawPath(path, lanePaint);
        }

        // Vẽ viền đường
        canvas.drawLine(5, 0, 5, screenHeight, roadBorderPaint);
        canvas.drawLine(screenWidth - 5, 0, screenWidth - 5, screenHeight, roadBorderPaint);

        // Vẽ xe (giữ nguyên từ code gốc)
        canvas.drawBitmap(playerCarBitmap, playerCarX, playerCarY, null);

        // Vẽ chướng ngại vật (giữ nguyên từ code gốc)
        for (Obstacle obstacle : obstacles) {
            canvas.drawBitmap(obstacleBitmap, obstacle.x, obstacle.y, null);
        }

        // Vẽ điểm số (giữ nguyên từ code gốc)
        canvas.drawText("Score: " + score, 20, 60, scorePaint);




        // Cập nhật hiệu ứng đường kẻ
        dashOffset -= 5;
        if (dashOffset < -40) {
            dashOffset = 0;
        }

        // Tiếp tục vòng lặp game nếu chưa kết thúc
        if (!isGameOver) {
            updateGame();
            invalidate();
        }
    }

    private void updateGame() {
        if (isPaused) return; // Do nothing if the game is paused
        long currentTime = System.currentTimeMillis();

        // Tạo chướng ngại vật mới (giữ nguyên logic từ code gốc)
        if (currentTime - lastObstacleSpawnTime > OBSTACLE_SPAWN_INTERVAL) {
            spawnObstacle();
            lastObstacleSpawnTime = currentTime;
        }

        // Di chuyển và kiểm tra chướng ngại vật (giữ nguyên logic từ code gốc)
        for (int i = obstacles.size() - 1; i >= 0; i--) {
            Obstacle obstacle = obstacles.get(i);
            obstacle.y += 10;

            if (isCollision(obstacle)) {
                isGameOver = true;
                if (gameListener != null) {
                    gameListener.onGameOver(score);
                }
                return;
            }

            if (obstacle.y > screenHeight + obstacleBitmap.getHeight()) {
                obstacles.remove(i);
                score += 50;
                if (gameListener != null) {
                    gameListener.onScoreUpdated(score);
                }
            }
        }
    }

    private void spawnObstacle() {
        int lane = random.nextInt(4); // Chọn ngẫu nhiên 1 làn đường
        int x = lanePositions[lane] + laneWidth / 2 - obstacleBitmap.getWidth() / 2; // Căn giữa chướng ngại vật trong làn
        obstacles.add(new Obstacle(x, -obstacleBitmap.getHeight())); // Tạo chướng ngại vật với vị trí căn giữa
    }


    private boolean isCollision(Obstacle obstacle) {
        Rect playerRect = new Rect(playerCarX, playerCarY,
                playerCarX + playerCarWidth,
                playerCarY + playerCarHeight);
        Rect obstacleRect = new Rect(obstacle.x, obstacle.y,
                obstacle.x + obstacleBitmap.getWidth(),
                obstacle.y + obstacleBitmap.getHeight());
        return playerRect.intersect(obstacleRect);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (isGameOver) {
                // Reset game (giữ nguyên từ code gốc)
                isGameOver = false;
                score = 0;
                obstacles.clear();
                currentLane = 1;
                playerCarX = lanePositions[currentLane];
                if (gameListener != null) {
                    gameListener.onScoreUpdated(score);
                }
                invalidate();
                return true;
            }

            // Chuyển làn khi chạm (giữ nguyên từ code gốc)
            float touchX = event.getX();
            if (touchX < screenWidth / 2 && currentLane > 0) {
                currentLane--;
            } else if (touchX >= screenWidth / 2 && currentLane < 3) {
                currentLane++;
            }
            playerCarX = lanePositions[currentLane];
        }
        return true;
    }

    public void updateCarPosition(float tiltX) {
        if (isGameOver) return;

        // Điều khiển xe bằng cảm biến (giữ nguyên logic từ code gốc)
        if (tiltX > 1.5 && currentLane < 3) {
            currentLane++;
        } else if (tiltX < -1.5 && currentLane > 0) {
            currentLane--;
        }
        playerCarX = lanePositions[currentLane];
    }

    private boolean isPaused = false;

    public void setPaused(boolean paused) {
        isPaused = paused;
        if (!paused) invalidate();  // Tiếp tục vẽ khi bỏ pause
    }

    public void setGameListener(GameListener listener) {
        this.gameListener = listener;
    }

    public void setGameMode(String gameMode) {
        this.gameMode = gameMode;
    }

    public void setGameOver(boolean gameOver) {
        this.isGameOver = gameOver;
    }

    private static class Obstacle {
        int x, y;

        Obstacle(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}