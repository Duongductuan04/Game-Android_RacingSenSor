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
    // Biến xe
    private Bitmap playerCarBitmap;
    private final int[] carResources = {R.drawable.car, R.drawable.car2, R.drawable.car3};
    private int selectedCarIndex = 1; // Mặc định là car2

    // Biến game
    private int playerCarX, playerCarY;
    private int playerCarWidth, playerCarHeight;
    private int currentLane = 1;
    private int screenWidth, screenHeight;
    private int laneWidth;
    private final int[] lanePositions = new int[4];

    // Chướng ngại vật
    private final int[] obstacleResources = {
            R.drawable.obstacle,

            R.drawable.obstacle2,
            R.drawable.obstacle3
    };
    private final List<Bitmap> obstacleBitmaps = new ArrayList<>();
    private final List<Obstacle> obstacles = new ArrayList<>();
    private final Random random = new Random();
    private long lastObstacleSpawnTime;
    private static final long OBSTACLE_SPAWN_INTERVAL = 2000;
    // Giao diện
    private int score = 0;
    private final Paint scorePaint = new Paint();
    private boolean isGameOver = false;
    private final Paint gameOverPaint = new Paint();
    private float dashOffset = 0f;
    private final Paint lanePaint = new Paint();
    private final Paint roadBorderPaint = new Paint();
    private final Paint laneBackgroundPaint = new Paint();
    private final int[] laneColors = {
            Color.rgb(169, 169, 169), // Làn 1
            Color.rgb(105, 105, 105), // Làn 2
            Color.rgb(169, 169, 169), // Làn 3
            Color.rgb(105, 105, 105)  // Làn 4
    };
   // Trạng thái game
    private boolean isPaused = false;
    private String gameMode;
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
        // Thiết lập Paint cho điểm số
        scorePaint.setColor(Color.WHITE);
        scorePaint.setTextSize(50);
        scorePaint.setAntiAlias(true);

        // Paint cho game over
        gameOverPaint.setColor(Color.RED);
        gameOverPaint.setTextSize(80);
        gameOverPaint.setAntiAlias(true);

        // Paint cho làn đường
        lanePaint.setColor(Color.YELLOW);
        lanePaint.setStrokeWidth(5);
        lanePaint.setStyle(Paint.Style.STROKE);
        lanePaint.setAntiAlias(true);

        // Paint cho viền đường
        roadBorderPaint.setColor(Color.WHITE);
        roadBorderPaint.setStrokeWidth(10);
        roadBorderPaint.setStyle(Paint.Style.STROKE);
        roadBorderPaint.setAntiAlias(true);

        // Paint cho nền làn đường
        laneBackgroundPaint.setStyle(Paint.Style.FILL);

        // Tải hình ảnh xe
        loadCarImage();
        loadObstacleImages();}

    private void loadObstacleImages() {
        for (int resId : obstacleResources) {
            try {
                Bitmap bitmap = Bitmap.createScaledBitmap(
                        BitmapFactory.decodeResource(getResources(), resId),
                        60, 60, true);
                obstacleBitmaps.add(bitmap);
            } catch (Exception e) {
                Log.e("GameView", "Error loading obstacle image: " + resId, e);
                // Fallback to default obstacle
                Bitmap defaultBitmap = Bitmap.createScaledBitmap(
                        BitmapFactory.decodeResource(getResources(), R.drawable.obstacle),
                        90, 90, true);
                obstacleBitmaps.add(defaultBitmap);
            }
        }
    }

    // Phương thức tải hình ảnh xe
    private void loadCarImage() {
        try {
            playerCarBitmap = Bitmap.createScaledBitmap(
                    BitmapFactory.decodeResource(getResources(), carResources[selectedCarIndex]),
                    220, 240, true);
        } catch (Exception e) {
            Log.e("GameView", "Error loading car image", e);
            // Fallback to default car (car2)
            playerCarBitmap = Bitmap.createScaledBitmap(
                    BitmapFactory.decodeResource(getResources(), R.drawable.car2),
                    220, 240, true);
        }

        updateCarDimensions();
    }

    // Phương thức công khai để thay đổi xe từ bên ngoài
    public void setSelectedCar(int carIndex) {
        if (carIndex >= 0 && carIndex < carResources.length) {
            this.selectedCarIndex = carIndex;
            loadCarImage();
            invalidate(); // Vẽ lại view
        }
    }

    private void updateCarDimensions() {
        if (playerCarBitmap != null) {
            playerCarWidth = playerCarBitmap.getWidth();
            playerCarHeight = playerCarBitmap.getHeight();
            if (screenHeight > 0) {
                playerCarY = screenHeight - playerCarHeight - 70;
                playerCarX = lanePositions[currentLane];
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        screenWidth = w;
        screenHeight = h;

        // Tính toán vị trí các làn đường
        laneWidth = screenWidth / 4;
        for (int i = 0; i < 4; i++) {
            lanePositions[i] = i * laneWidth + laneWidth / 2 - playerCarWidth / 2;
        }

        updateCarDimensions();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (isPaused) {
            drawPauseScreen(canvas);
            return;
        }

        drawRoad(canvas);
        drawCar(canvas);
        drawObstacles(canvas);
        drawScore(canvas);

        if (!isGameOver) {
            updateGame();
            invalidate();
        }
    }

    private void drawPauseScreen(Canvas canvas) {
        Paint overlay = new Paint();
        overlay.setColor(Color.argb(180, 0, 0, 0));
        canvas.drawRect(0, 0, screenWidth, screenHeight, overlay);

        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(80);
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("PAUSED", screenWidth/2, screenHeight/2, textPaint);
    }

    private void drawRoad(Canvas canvas) {
        // Vẽ nền các làn đường
        for (int i = 0; i < 4; i++) {
            laneBackgroundPaint.setColor(laneColors[i]);
            canvas.drawRect(i * laneWidth, 0, (i + 1) * laneWidth, screenHeight, laneBackgroundPaint);
        }

        // Vẽ vạch kẻ đường
        lanePaint.setPathEffect(new DashPathEffect(new float[]{20, 20}, dashOffset));
        Path path = new Path();
        for (int i = 1; i < 4; i++) {
            path.moveTo(i * laneWidth, 0);
            path.lineTo(i * laneWidth, screenHeight);
            canvas.drawPath(path, lanePaint);
        }

        // Vẽ viền đường
        canvas.drawLine(5, 0, 5, screenHeight, roadBorderPaint);
        canvas.drawLine(screenWidth-5, 0, screenWidth-5, screenHeight, roadBorderPaint);

        // Cập nhật hiệu ứng vạch kẻ
        dashOffset -= 5;
        if (dashOffset < -40) dashOffset = 0;
    }

    private void drawCar(Canvas canvas) {
        if (playerCarBitmap != null) {
            canvas.drawBitmap(playerCarBitmap, playerCarX, playerCarY, null);
        }
    }

    private void drawObstacles(Canvas canvas) {
        for (Obstacle obstacle : obstacles) {
            if (obstacle.bitmap != null) {
                canvas.drawBitmap(obstacle.bitmap, obstacle.x, obstacle.y, null);
            }
        }
    }

    private void drawScore(Canvas canvas) {
        canvas.drawText("Score: " + score, 20, 60, scorePaint);
    }

    private void updateGame() {
        if (isPaused) return;

        spawnObstacles();
        moveObstacles();
    }

    private void spawnObstacles() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastObstacleSpawnTime > OBSTACLE_SPAWN_INTERVAL) {
            int lane = random.nextInt(4);
            int obstacleType = random.nextInt(obstacleBitmaps.size());
            Bitmap obstacleBitmap = obstacleBitmaps.get(obstacleType);

            obstacles.add(new Obstacle(
                    lanePositions[lane] + laneWidth/2 - obstacleBitmap.getWidth()/2,
                    -obstacleBitmap.getHeight(),
                    obstacleBitmap
            ));
            lastObstacleSpawnTime = currentTime;
        }
    }

    private void moveObstacles() {
        for (int i = obstacles.size()-1; i >= 0; i--) {
            Obstacle obstacle = obstacles.get(i);
            obstacle.y += 10;

            if (checkCollision(obstacle)) {
                gameOver();
                return;
            }

            if (obstacle.y > screenHeight) {
                obstacles.remove(i);
                increaseScore();
            }
        }
    }

    private boolean checkCollision(Obstacle obstacle) {
        Rect carRect = new Rect(playerCarX, playerCarY,
                playerCarX + playerCarWidth, playerCarY + playerCarHeight);
        Rect obstacleRect = new Rect(obstacle.x, obstacle.y,
                obstacle.x + obstacle.bitmap.getWidth(), obstacle.y + obstacle.bitmap.getHeight());
        return carRect.intersect(obstacleRect);
    }

    private void gameOver() {
        isGameOver = true;
        if (gameListener != null) {
            gameListener.onGameOver(score);
        }
    }

    private void increaseScore() {
        score += 50;
        if (gameListener != null) {
            gameListener.onScoreUpdated(score);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (isGameOver) {
                resetGame();
            } else {
                handleLaneChange(event.getX());
            }
        }
        return true;
    }

    private void handleLaneChange(float touchX) {
        if (touchX < screenWidth/2 && currentLane > 0) {
            currentLane--;
        } else if (touchX >= screenWidth/2 && currentLane < 3) {
            currentLane++;
        }
        playerCarX = lanePositions[currentLane];
    }

    void resetGame() {
        isGameOver = false;
        score = 0;
        obstacles.clear();
        currentLane = 1;
        playerCarX = lanePositions[currentLane];
        if (gameListener != null) {
            gameListener.onScoreUpdated(score);
        }
    }

    public void updateCarPosition(float tiltX) {
        if (isGameOver || isPaused) return;

        if (tiltX > 1.5 && currentLane < 3) {
            currentLane++;
        } else if (tiltX < -1.5 && currentLane > 0) {
            currentLane--;
        }
        playerCarX = lanePositions[currentLane];
    }

    public void setPaused(boolean paused) {
        isPaused = paused;
        if (!paused) invalidate();
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
        Bitmap bitmap;

        Obstacle(int x, int y, Bitmap bitmap) {
            this.x = x;
            this.y = y;
            this.bitmap = bitmap;
        }
    }
}