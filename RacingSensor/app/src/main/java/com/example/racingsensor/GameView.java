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
    // Car properties
    private Bitmap playerCarBitmap;
    private final int[] carResources = {R.drawable.car, R.drawable.car2, R.drawable.car3};
    private int selectedCarIndex = 1;
    private int playerCarX, playerCarY;
    private int playerCarWidth, playerCarHeight;

    // Game properties
    private int currentLane = 1;
    private int screenWidth, screenHeight;
    private int laneWidth;
    private final int[] lanePositions = new int[4];
    private int score = 0;
    private boolean isGameOver = false;
    private boolean isPaused = false;
    private String gameMode;

    // Obstacles
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

    // Drawing tools
    private final Paint scorePaint = new Paint();
    private final Paint gameOverPaint = new Paint();
    private final Paint lanePaint = new Paint();
    private final Paint roadBorderPaint = new Paint();
    private final Paint laneBackgroundPaint = new Paint();
    private final Paint gyroEffectPaint = new Paint();
    private float dashOffset = 0f;

    // Lane colors
    private final int[] laneColors = {
            Color.rgb(169, 169, 169), // Lane 1 (light gray)
            Color.rgb(105, 105, 105), // Lane 2 (dark gray)
            Color.rgb(169, 169, 169), // Lane 3 (light gray)
            Color.rgb(105, 105, 105)  // Lane 4 (dark gray)
    };

    // Game state
    private boolean isUsingGyro = false;
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
        // Initialize paints
        initPaints();

        // Load car and obstacles
        loadCarImage();
        loadObstacleImages();
    }

    private void initPaints() {
        // Score paint
        scorePaint.setColor(Color.WHITE);
        scorePaint.setTextSize(50);
        scorePaint.setAntiAlias(true);

        // Game over paint
        gameOverPaint.setColor(Color.RED);
        gameOverPaint.setTextSize(80);
        gameOverPaint.setAntiAlias(true);
        gameOverPaint.setTextAlign(Paint.Align.CENTER);

        // Lane markings paint
        lanePaint.setColor(Color.YELLOW);
        lanePaint.setStrokeWidth(5);
        lanePaint.setStyle(Paint.Style.STROKE);
        lanePaint.setAntiAlias(true);

        // Road border paint
        roadBorderPaint.setColor(Color.WHITE);
        roadBorderPaint.setStrokeWidth(10);
        roadBorderPaint.setStyle(Paint.Style.STROKE);
        roadBorderPaint.setAntiAlias(true);

        // Lane background paint
        laneBackgroundPaint.setStyle(Paint.Style.FILL);

        // Gyro effect paint
        gyroEffectPaint.setColor(Color.argb(100, 0, 150, 255)); // Semi-transparent blue
        gyroEffectPaint.setStyle(Paint.Style.FILL);
        gyroEffectPaint.setAntiAlias(true);
    }

    private void loadCarImage() {
        try {
            playerCarBitmap = Bitmap.createScaledBitmap(
                    BitmapFactory.decodeResource(getResources(), carResources[selectedCarIndex]),
                    220, 240, true);
            updateCarDimensions();
        } catch (Exception e) {
            Log.e("GameView", "Error loading car image", e);
            // Fallback to default car
            playerCarBitmap = Bitmap.createScaledBitmap(
                    BitmapFactory.decodeResource(getResources(), R.drawable.car2),
                    220, 240, true);
            updateCarDimensions();
        }
    }

    private void loadObstacleImages() {
        for (int resId : obstacleResources) {
            try {
                Bitmap bitmap = Bitmap.createScaledBitmap(
                        BitmapFactory.decodeResource(getResources(), resId),
                        60, 60, true);
                obstacleBitmaps.add(bitmap);
            } catch (Exception e) {
                Log.e("GameView", "Error loading obstacle image", e);
                // Fallback to default obstacle
                Bitmap defaultBitmap = Bitmap.createScaledBitmap(
                        BitmapFactory.decodeResource(getResources(), R.drawable.obstacle),
                        90, 90, true);
                obstacleBitmaps.add(defaultBitmap);
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        screenWidth = w;
        screenHeight = h;

        // Calculate lane positions
        laneWidth = screenWidth / 4;
        for (int i = 0; i < 4; i++) {
            lanePositions[i] = i * laneWidth + laneWidth / 2 - playerCarWidth / 2;
        }

        updateCarPosition();
    }

    private void updateCarDimensions() {
        if (playerCarBitmap != null) {
            playerCarWidth = playerCarBitmap.getWidth();
            playerCarHeight = playerCarBitmap.getHeight();
            updateCarPosition();
        }
    }

    private void updateCarPosition() {
        if (screenHeight > 0) {
            playerCarY = screenHeight - playerCarHeight - 70; // 70px from bottom
            playerCarX = lanePositions[currentLane];
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (isPaused) {
            drawPauseScreen(canvas);
            return;
        }

        drawRoad(canvas);

        // Draw gyro effect if active
        if (isUsingGyro) {
            drawGyroEffect(canvas);
        }

        drawCar(canvas);
        drawObstacles(canvas);
        drawScore(canvas);

        if (isGameOver) {
            drawGameOver(canvas);
        } else {
            updateGame();
            invalidate(); // Continue the game loop
        }
    }

    private void drawPauseScreen(Canvas canvas) {
        Paint overlayPaint = new Paint();
        overlayPaint.setColor(Color.argb(180, 0, 0, 0)); // Semi-transparent black
        canvas.drawRect(0, 0, screenWidth, screenHeight, overlayPaint);

        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(80);
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("PAUSED", screenWidth/2, screenHeight/2, textPaint);
    }

    private void drawRoad(Canvas canvas) {
        // Draw lane backgrounds
        for (int i = 0; i < 4; i++) {
            laneBackgroundPaint.setColor(laneColors[i]);
            canvas.drawRect(i * laneWidth, 0, (i + 1) * laneWidth, screenHeight, laneBackgroundPaint);
        }

        // Draw lane markings with dashed effect
        lanePaint.setPathEffect(new DashPathEffect(new float[]{20, 20}, dashOffset));
        Path path = new Path();
        for (int i = 1; i < 4; i++) {
            path.moveTo(i * laneWidth, 0);
            path.lineTo(i * laneWidth, screenHeight);
            canvas.drawPath(path, lanePaint);
        }

        // Draw road borders
        canvas.drawLine(5, 0, 5, screenHeight, roadBorderPaint);
        canvas.drawLine(screenWidth-5, 0, screenWidth-5, screenHeight, roadBorderPaint);

        // Update dash offset for moving effect
        dashOffset -= 5;
        if (dashOffset < -40) dashOffset = 0;
    }

    private void drawGyroEffect(Canvas canvas) {
        float centerX = playerCarX + playerCarWidth / 2f;
        float centerY = playerCarY + playerCarHeight / 2f;
        float radius = playerCarWidth * 0.8f;
        canvas.drawCircle(centerX, centerY, radius, gyroEffectPaint);
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
        canvas.drawText("Score: " + score, 30, 60, scorePaint);
    }

    private void drawGameOver(Canvas canvas) {
        canvas.drawText("GAME OVER", screenWidth/2, screenHeight/2, gameOverPaint);
    }

    private void updateGame() {
        if (isPaused || isGameOver) return;

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
        for (int i = obstacles.size() - 1; i >= 0; i--) {
            Obstacle obstacle = obstacles.get(i);
            obstacle.y += 10; // Move obstacle down

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
                obstacle.x + obstacle.bitmap.getWidth(),
                obstacle.y + obstacle.bitmap.getHeight());
        return Rect.intersects(carRect, obstacleRect);
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
            return true;
        }
        return super.onTouchEvent(event);
    }

    private void handleLaneChange(float touchX) {
        if (touchX < screenWidth / 2 && currentLane > 0) {
            currentLane--;
        } else if (touchX >= screenWidth / 2 && currentLane < 3) {
            currentLane++;
        }
        playerCarX = lanePositions[currentLane];
    }

    public void updateCarPosition(float tiltX) {
        if (isGameOver || isPaused) return;

        // Detect if gyro is being used (strong tilt)
        isUsingGyro = Math.abs(tiltX) > 2.5f;

        if (tiltX > 1.5f && currentLane < 3) {
            currentLane++;
        } else if (tiltX < -1.5f && currentLane > 0) {
            currentLane--;
        }
        playerCarX = lanePositions[currentLane];
    }

    public void resetGame() {
        isGameOver = false;
        score = 0;
        obstacles.clear();
        currentLane = 1;
        playerCarX = lanePositions[currentLane];
        isUsingGyro = false;

        if (gameListener != null) {
            gameListener.onScoreUpdated(score);
        }

        invalidate();
    }

    // Public methods to control game state
    public void setSelectedCar(int carIndex) {
        if (carIndex >= 0 && carIndex < carResources.length) {
            selectedCarIndex = carIndex;
            loadCarImage();
            invalidate();
        }
    }

    public void setPaused(boolean paused) {
        isPaused = paused;
        if (!paused) {
            invalidate();
        }
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