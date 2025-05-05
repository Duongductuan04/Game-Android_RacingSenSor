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

    // Game constants
    private static final long POWERUP_SPAWN_INTERVAL = 8000;
    private static final long BASE_OBSTACLE_SPAWN_INTERVAL = 3000;
    private static final long MIN_OBSTACLE_SPAWN_INTERVAL = 1500;
    private long currentObstacleSpawnInterval = BASE_OBSTACLE_SPAWN_INTERVAL;
    private static final int BASE_OBSTACLE_SPEED = 5;
    private static final int MAX_OBSTACLE_SPEED = 15;
    private int obstacleSpeed = BASE_OBSTACLE_SPEED;
    private static final int BASE_POWERUP_SPEED = 4;
    private int currentPowerUpSpeed = BASE_POWERUP_SPEED;
    private static final float LANE_CHANGE_SPEED = 0.08f;

    // Game state
    private boolean isGameOver = false;
    private boolean isPaused = false;
    private boolean isUsingGyro = false;
    private boolean isNightMode = false;
    private int score = 0;
    private int scoreMultiplier = 1;
    private String gameMode;

    // Player car
    private Bitmap playerCarBitmap;
    private final int[] carResources = {R.drawable.car, R.drawable.car2, R.drawable.car3};
    private int selectedCarIndex = 1;
    private int playerCarX, playerCarY;
    private int playerCarWidth, playerCarHeight;
    private int currentLane = 1;

    // Road properties
    private int screenWidth, screenHeight;
    private int laneWidth;
    private final int[] lanePositions = new int[4];
    private final int[] dayLaneColors = {
            Color.rgb(220, 220, 220),
            Color.rgb(180, 180, 180),
            Color.rgb(220, 220, 220),
            Color.rgb(180, 180, 180)
    };
    private final int[] nightLaneColors = {
            Color.rgb(100, 100, 100),
            Color.rgb(70, 70, 70),
            Color.rgb(100, 100, 100),
            Color.rgb(70, 70, 70)
    };
    private int[] currentLaneColors = dayLaneColors;

    // Obstacles
    private final int[] obstacleResources = {
            R.drawable.obstacle,
            R.drawable.obstacle2,
            R.drawable.obstacle3
    };
    private final List<Bitmap> obstacleBitmaps = new ArrayList<>();
    private final List<Obstacle> obstacles = new ArrayList<>();

    // Power-ups
    private final List<PowerUp> powerUps = new ArrayList<>();
    private boolean isShieldActive = false;
    private boolean isDoubleScoreActive = false;
    private boolean isSlowTimeActive = false;

    // Drawing tools
    private final Paint scorePaint = new Paint();
    private final Paint gameOverPaint = new Paint();
    private final Paint lanePaint = new Paint();
    private final Paint roadBorderPaint = new Paint();
    private final Paint laneBackgroundPaint = new Paint();
    private final Paint gyroEffectPaint = new Paint();
    private float dashOffset = 0f;

    // Game mechanics
    private final Random random = new Random();
    private long lastObstacleSpawnTime;
    private long lastPowerUpSpawnTime;
    private float currentTilt = 0f;
    private float targetLanePosition;
    private GameListener gameListener;
    private long gameStartTime;

    // Power-up types
    private enum PowerUpType {
        SHIELD, SLOW_TIME, DOUBLE_SCORE
    }

    // Obstacle class
    private static class Obstacle {
        int x, y;
        Bitmap bitmap;

        Obstacle(int x, int y, Bitmap bitmap) {
            this.x = x;
            this.y = y;
            this.bitmap = bitmap;
        }
    }

    // Power-up class
    private class PowerUp {
        int x, y;
        int size = 100;
        Bitmap bitmap;
        PowerUpType type;

        PowerUp(int x, int y, PowerUpType type) {
            this.x = x;
            this.y = y;
            this.type = type;
            this.bitmap = createPowerUpBitmap(type);
        }
    }

    // Game listener interface
    public interface GameListener {
        void onScoreUpdated(int score);
        void onGameOver(int finalScore);
        long getRemainingTime();
    }

    // Constructors
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
        initPaints();
        loadCarImage();
        loadObstacleImages();
    }

    private void initPaints() {
        scorePaint.setColor(Color.BLACK);
        scorePaint.setTextSize(50);
        scorePaint.setAntiAlias(true);

        gameOverPaint.setColor(Color.RED);
        gameOverPaint.setTextSize(80);
        gameOverPaint.setAntiAlias(true);
        gameOverPaint.setTextAlign(Paint.Align.CENTER);

        lanePaint.setColor(Color.YELLOW);
        lanePaint.setStrokeWidth(5);
        lanePaint.setStyle(Paint.Style.STROKE);
        lanePaint.setAntiAlias(true);

        roadBorderPaint.setColor(Color.WHITE);
        roadBorderPaint.setStrokeWidth(10);
        roadBorderPaint.setStyle(Paint.Style.STROKE);
        roadBorderPaint.setAntiAlias(true);

        laneBackgroundPaint.setStyle(Paint.Style.FILL);

        gyroEffectPaint.setColor(Color.argb(100, 0, 150, 255));
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

        laneWidth = screenWidth / 4;
        for (int i = 0; i < 4; i++) {
            lanePositions[i] = i * laneWidth + laneWidth / 2 - playerCarWidth / 2;
        }

        targetLanePosition = lanePositions[currentLane];
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
        playerCarX = lanePositions[currentLane];
        playerCarY = screenHeight - playerCarHeight - 70;
    }

    public void setNightMode(boolean isNightMode) {
        this.isNightMode = isNightMode;
        this.currentLaneColors = isNightMode ? nightLaneColors : dayLaneColors;
        invalidate();
    }

    public boolean isNightMode() {
        return isNightMode;
    }

    public float getCurrentTilt() {
        return currentTilt;
    }

    public void updateCarPosition(float tiltX) {
        if (isGameOver || isPaused) return;

        currentTilt = tiltX;
        isUsingGyro = Math.abs(tiltX) > 1.5f;

        int targetLane = currentLane;
        if (tiltX > 0.5f) {
            targetLane = Math.min(3, currentLane + 1);
        } else if (tiltX < -0.5f) {
            targetLane = Math.max(0, currentLane - 1);
        }

        if (targetLane != currentLane) {
            currentLane = targetLane;
            targetLanePosition = lanePositions[currentLane];
        }

        if (Math.abs(playerCarX - targetLanePosition) > 5) {
            playerCarX += (targetLanePosition - playerCarX) * LANE_CHANGE_SPEED;
        } else {
            playerCarX = (int) targetLanePosition;
        }
    }

    public void updateCarPosition(int lane) {
        if (lane >= 0 && lane <= 3) {
            currentLane = lane;
            targetLanePosition = lanePositions[currentLane];
            playerCarX = (int) targetLanePosition;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawRoad(canvas);
        drawObstacles(canvas);
        drawPowerUps(canvas);
        drawCar(canvas);
        drawScore(canvas);
        drawActivePowerUps(canvas);

        if (isGameOver) {
            drawGameOver(canvas);
        } else if (isPaused) {
            drawPauseScreen(canvas);
        }

        if (!isPaused && !isGameOver) {
            updateGame();
            invalidate();
        }
    }

    private void drawRoad(Canvas canvas) {
        // Draw background
        Paint backgroundPaint = new Paint();
        backgroundPaint.setColor(isNightMode ? Color.BLACK : Color.WHITE);
        canvas.drawRect(0, 0, screenWidth, screenHeight, backgroundPaint);

        // Draw lanes
        for (int i = 0; i < 4; i++) {
            laneBackgroundPaint.setColor(currentLaneColors[i]);
            canvas.drawRect(i * laneWidth, 0, (i + 1) * laneWidth, screenHeight, laneBackgroundPaint);
        }

        // Draw lane markers
        lanePaint.setColor(isNightMode ? Color.rgb(200, 200, 0) : Color.YELLOW);
        lanePaint.setPathEffect(new DashPathEffect(new float[]{20, 20}, dashOffset));
        Path path = new Path();
        for (int i = 1; i < 4; i++) {
            path.moveTo(i * laneWidth, 0);
            path.lineTo(i * laneWidth, screenHeight);
            canvas.drawPath(path, lanePaint);
        }

        // Draw road borders
        roadBorderPaint.setColor(isNightMode ? Color.GRAY : Color.WHITE);
        canvas.drawLine(5, 0, 5, screenHeight, roadBorderPaint);
        canvas.drawLine(screenWidth-5, 0, screenWidth-5, screenHeight, roadBorderPaint);

        dashOffset -= 5;
        if (dashOffset < -40) dashOffset = 0;
    }

    private void drawObstacles(Canvas canvas) {
        for (Obstacle obstacle : obstacles) {
            if (obstacle.bitmap != null) {
                canvas.drawBitmap(obstacle.bitmap, obstacle.x, obstacle.y, null);
            }
        }
    }

    private void drawPowerUps(Canvas canvas) {
        long currentTime = System.currentTimeMillis();
        for (PowerUp pu : powerUps) {
            if (currentTime % 600 < 450) {
                Paint glowPaint = new Paint();
                glowPaint.setColor(Color.argb(50, 255, 255, 255));
                canvas.drawCircle(pu.x + pu.size / 2, pu.y + pu.size / 2, pu.size / 2 + 5, glowPaint);
                canvas.drawBitmap(pu.bitmap, pu.x, pu.y, null);
            }
        }
    }

    private void drawCar(Canvas canvas) {
        if (playerCarBitmap != null) {
            canvas.drawBitmap(playerCarBitmap, playerCarX, playerCarY, null);
        }
    }

    private void drawScore(Canvas canvas) {
        scorePaint.setColor(isNightMode ? Color.WHITE : Color.BLACK);
        canvas.drawText("Score: " + score, 30, 60, scorePaint);
    }

    private void drawActivePowerUps(Canvas canvas) {
        int startY = 100;
        int iconSize = 50;
        int padding = 10;

        if (isShieldActive) {
            Bitmap shield = createPowerUpBitmap(PowerUpType.SHIELD);
            canvas.drawBitmap(Bitmap.createScaledBitmap(shield, iconSize, iconSize, true),
                    padding, startY, null);
            startY += iconSize + padding;
        }

        if (isDoubleScoreActive) {
            Bitmap doubleScore = createPowerUpBitmap(PowerUpType.DOUBLE_SCORE);
            canvas.drawBitmap(Bitmap.createScaledBitmap(doubleScore, iconSize, iconSize, true),
                    padding, startY, null);
            startY += iconSize + padding;
        }

        if (isSlowTimeActive) {
            Bitmap slowTime = createPowerUpBitmap(PowerUpType.SLOW_TIME);
            canvas.drawBitmap(Bitmap.createScaledBitmap(slowTime, iconSize, iconSize, true),
                    padding, startY, null);
        }
    }

    private void drawGameOver(Canvas canvas) {
        gameOverPaint.setColor(isNightMode ? Color.rgb(255, 100, 100) : Color.RED);
        canvas.drawText("GAME OVER", screenWidth/2, screenHeight/2, gameOverPaint);
    }

    private void drawPauseScreen(Canvas canvas) {
        Paint overlayPaint = new Paint();
        overlayPaint.setColor(Color.argb(180, 0, 0, 0));
        canvas.drawRect(0, 0, screenWidth, screenHeight, overlayPaint);

        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(80);
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("PAUSED", screenWidth/2, screenHeight/2, textPaint);
    }

    private Bitmap createPowerUpBitmap(PowerUpType type) {
        Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        // Draw glow background
        int glowColor = 0;
        switch (type) {
            case SHIELD: glowColor = Color.argb(80, 0, 100, 255); break;
            case SLOW_TIME: glowColor = Color.argb(80, 0, 255, 100); break;
            case DOUBLE_SCORE: glowColor = Color.argb(80, 255, 255, 0); break;
        }
        paint.setColor(glowColor);
        canvas.drawCircle(50, 50, 45, paint);

        // Draw border and icon
        paint.setStyle(Paint.Style.FILL);
        switch (type) {
            case SHIELD:
                paint.setColor(Color.BLUE);
                canvas.drawCircle(50, 50, 35, paint);
                paint.setColor(Color.WHITE);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(3);
                canvas.drawCircle(50, 50, 35, paint);
                paint.setStyle(Paint.Style.FILL);
                // Draw shield icon
                paint.setColor(Color.WHITE);
                Path shieldPath = new Path();
                shieldPath.moveTo(50, 30);
                shieldPath.lineTo(65, 45);
                shieldPath.lineTo(50, 70);
                shieldPath.lineTo(35, 45);
                shieldPath.close();
                canvas.drawPath(shieldPath, paint);
                break;

            case SLOW_TIME:
                paint.setColor(Color.GREEN);
                canvas.drawCircle(50, 50, 35, paint);
                paint.setColor(Color.WHITE);
                // Draw clock icon
                paint.setStrokeWidth(5);
                canvas.drawCircle(50, 50, 25, paint);
                canvas.drawLine(50, 50, 50, 35, paint); // Minute hand
                canvas.drawLine(50, 50, 65, 50, paint); // Hour hand
                break;

            case DOUBLE_SCORE:
                paint.setColor(Color.YELLOW);
                canvas.drawCircle(50, 50, 35, paint);
                paint.setColor(Color.BLACK);
                paint.setTextSize(40);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setFakeBoldText(true);
                canvas.drawText("2X", 50, 60, paint);
                break;
        }
        return bitmap;
    }

    private void updateGame() {
        if (isPaused || isGameOver) return;

        updateDifficulty();
        spawnObstacles();
        spawnPowerUps();
        moveObstacles();
        movePowerUps();
        checkCollisions();
    }

    private void updateDifficulty() {
        long elapsedTime = System.currentTimeMillis() - gameStartTime;
        float timeFactor = Math.min(1.0f, elapsedTime / 300000f);
        float scoreFactor = Math.min(1.0f, score / 10000f);
        float difficulty = Math.min(1.0f, timeFactor * 0.7f + scoreFactor * 0.3f);

        if (!isSlowTimeActive) {
            obstacleSpeed = BASE_OBSTACLE_SPEED + (int)((MAX_OBSTACLE_SPEED - BASE_OBSTACLE_SPEED) * difficulty);
        }

        currentObstacleSpawnInterval = (long)(
                BASE_OBSTACLE_SPAWN_INTERVAL -
                        (BASE_OBSTACLE_SPAWN_INTERVAL - MIN_OBSTACLE_SPAWN_INTERVAL) * difficulty
        );

        currentPowerUpSpeed = BASE_POWERUP_SPEED + (int)(3 * difficulty);
    }

    private void spawnObstacles() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastObstacleSpawnTime > currentObstacleSpawnInterval) {
            int lane = random.nextInt(4);
            int obstacleType = random.nextInt(obstacleBitmaps.size());
            Bitmap obstacleBitmap = obstacleBitmaps.get(obstacleType);

            int x = lanePositions[lane] + laneWidth/2 - obstacleBitmap.getWidth()/2;
            int y = -obstacleBitmap.getHeight();

            if (!isPositionOccupied(x, y, obstacleBitmap.getWidth(), obstacleBitmap.getHeight())) {
                obstacles.add(new Obstacle(x, y, obstacleBitmap));
                lastObstacleSpawnTime = currentTime;
            }
        }
    }

    private void spawnPowerUps() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastPowerUpSpawnTime > POWERUP_SPAWN_INTERVAL) {
            if (random.nextInt(100) < 30) {
                int lane = random.nextInt(4);
                PowerUpType type = PowerUpType.values()[random.nextInt(PowerUpType.values().length)];

                int x = lanePositions[lane] + laneWidth/2 - 50;
                int y = -100;

                if (!isPositionOccupied(x, y, 100, 100)) {
                    powerUps.add(new PowerUp(x, y, type));
                    lastPowerUpSpawnTime = currentTime;
                }
            }
        }
    }

    private boolean isPositionOccupied(int x, int y, int width, int height) {
        Rect newRect = new Rect(x, y, x + width, y + height);

        for (Obstacle obstacle : obstacles) {
            Rect obstacleRect = new Rect(
                    obstacle.x,
                    obstacle.y,
                    obstacle.x + obstacle.bitmap.getWidth(),
                    obstacle.y + obstacle.bitmap.getHeight()
            );
            if (Rect.intersects(newRect, obstacleRect)) {
                return true;
            }
        }

        for (PowerUp pu : powerUps) {
            Rect puRect = new Rect(pu.x, pu.y, pu.x + pu.size, pu.y + pu.size);
            if (Rect.intersects(newRect, puRect)) {
                return true;
            }
        }

        return false;
    }

    private void moveObstacles() {
        for (int i = obstacles.size() - 1; i >= 0; i--) {
            Obstacle obstacle = obstacles.get(i);
            obstacle.y += obstacleSpeed;

            if (obstacle.y > screenHeight) {
                obstacles.remove(i);
                increaseScore();
            }
        }
    }

    private void movePowerUps() {
        for (int i = powerUps.size() - 1; i >= 0; i--) {
            PowerUp pu = powerUps.get(i);
            pu.y += isSlowTimeActive ? currentPowerUpSpeed / 2 : currentPowerUpSpeed;

            if (pu.y > screenHeight) {
                powerUps.remove(i);
            }
        }
    }

    private void checkCollisions() {
        Rect carRect = new Rect(playerCarX, playerCarY,
                playerCarX + playerCarWidth, playerCarY + playerCarHeight);

        for (int i = powerUps.size() - 1; i >= 0; i--) {
            PowerUp pu = powerUps.get(i);
            Rect puRect = new Rect(pu.x, pu.y, pu.x + pu.size, pu.y + pu.size);

            if (Rect.intersects(carRect, puRect)) {
                applyPowerUp(pu.type);
                powerUps.remove(i);
            }
        }

        if (!isShieldActive) {
            for (Obstacle obstacle : obstacles) {
                if (checkCollision(obstacle)) {
                    gameOver();
                    return;
                }
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

    private void applyPowerUp(PowerUpType type) {
        switch (type) {
            case SHIELD:
                isShieldActive = true;
                postDelayed(() -> isShieldActive = false, 5000);
                break;
            case SLOW_TIME:
                isSlowTimeActive = true;
                int originalSpeed = obstacleSpeed;
                obstacleSpeed = Math.max(3, originalSpeed / 2);

                postDelayed(() -> {
                    isSlowTimeActive = false;
                    updateDifficulty();
                }, 4000);
                break;
            case DOUBLE_SCORE:
                isDoubleScoreActive = true;
                scoreMultiplier = 2;
                postDelayed(() -> {
                    isDoubleScoreActive = false;
                    scoreMultiplier = 1;
                }, 10000);
                break;
        }
    }

    private void increaseScore() {
        score += 50 * scoreMultiplier;
        if (gameListener != null) {
            gameListener.onScoreUpdated(score);
        }
    }

    private void gameOver() {
        isGameOver = true;
        if (gameListener != null) {
            gameListener.onGameOver(score);
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

    public boolean isDoubleScoreActive() {
        return isDoubleScoreActive;
    }

    public void resetGame() {
        isGameOver = false;
        score = 0;
        obstacles.clear();
        powerUps.clear();
        currentLane = 1;
        playerCarX = lanePositions[currentLane];
        gameStartTime = System.currentTimeMillis();

        isShieldActive = false;
        isDoubleScoreActive = false;
        isSlowTimeActive = false;
        scoreMultiplier = 1;
        obstacleSpeed = BASE_OBSTACLE_SPEED;
        currentPowerUpSpeed = BASE_POWERUP_SPEED;
        currentObstacleSpawnInterval = BASE_OBSTACLE_SPAWN_INTERVAL;

        if (gameListener != null) {
            gameListener.onScoreUpdated(score);
        }

        invalidate();
    }

    // Public methods
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
}