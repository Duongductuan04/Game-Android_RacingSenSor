package com.example.racingsensor;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    // Phiên bản database
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "racing_game.db";

    // Tên bảng và các cột
    private static final String TABLE_HIGH_SCORES = "high_scores";
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_SCORE = "score";
    private static final String COLUMN_GAME_MODE = "game_mode";
    private static final String COLUMN_TIMESTAMP = "timestamp";

    // Câu lệnh tạo bảng
    private static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE_HIGH_SCORES + "(" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_SCORE + " INTEGER," +
                    COLUMN_GAME_MODE + " TEXT," +
                    COLUMN_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP" +
                    ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HIGH_SCORES);
        onCreate(db);
    }

    // Thêm điểm cao mới
    public void addHighScore(int score, String gameMode) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_SCORE, score);
        values.put(COLUMN_GAME_MODE, gameMode);

        db.insert(TABLE_HIGH_SCORES, null, values);

        // Giữ chỉ 10 điểm cao nhất
        db.execSQL("DELETE FROM " + TABLE_HIGH_SCORES + " WHERE " + COLUMN_ID +
                " NOT IN (SELECT " + COLUMN_ID + " FROM " + TABLE_HIGH_SCORES +
                " ORDER BY " + COLUMN_SCORE + " DESC LIMIT 10)");

        db.close();
    }

    public List<HighScore> getAllHighScores() {
        List<HighScore> highScores = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_HIGH_SCORES +
                " ORDER BY " + COLUMN_SCORE + " DESC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.rawQuery(selectQuery, null);

            // Lấy chỉ số cột một lần duy nhất
            int scoreIndex = cursor.getColumnIndexOrThrow(COLUMN_SCORE);
            int modeIndex = cursor.getColumnIndexOrThrow(COLUMN_GAME_MODE);
            int timeIndex = cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP);

            if (cursor.moveToFirst()) {
                do {
                    HighScore score = new HighScore(
                            cursor.getInt(scoreIndex),
                            cursor.getString(modeIndex),
                            cursor.getString(timeIndex)
                    );
                    highScores.add(score);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {

        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }

        return highScores;
    }

    // Model class cho điểm cao
    public static class HighScore {
        private final int score;
        private final String gameMode;
        private final String timestamp;

        public HighScore(int score, String gameMode, String timestamp) {
            this.score = score;
            this.gameMode = gameMode;
            this.timestamp = formatTimestamp(timestamp);
        }

        private static String formatTimestamp(String dbTimestamp) {
            try {
                SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
                Date date = dbFormat.parse(dbTimestamp);
                return displayFormat.format(date);
            } catch (Exception e) {
                return dbTimestamp;
            }
        }

        // Getter methods
        public int getScore() { return score; }
        public String getGameMode() { return gameMode; }
        public String getTimestamp() { return timestamp; }
    }
}