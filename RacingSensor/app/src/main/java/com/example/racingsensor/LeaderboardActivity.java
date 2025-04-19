package com.example.racingsensor;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class LeaderboardActivity extends AppCompatActivity {

    private ListView highScoresListView;
    private DatabaseHelper databaseHelper;
    private TextView emptyListTextView;
    private HighScoreAdapter adapter;
    private List<DatabaseHelper.HighScore> highScores = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        initializeViews();
        setupDatabase();
        loadHighScores();
    }

    private void initializeViews() {
        highScoresListView = findViewById(R.id.lvHighScores);
        emptyListTextView = findViewById(R.id.tvEmptyList);

        highScoresListView.setOnItemClickListener((parent, view, position, id) -> {
            // Xử lý click item nếu cần
        });
    }

    private void setupDatabase() {
        try {
            databaseHelper = new DatabaseHelper(this);
        } catch (Exception e) {
            showError("Không thể khởi tạo database");
            finish();
        }
    }

    private void loadHighScores() {
        new LoadHighScoresTask().execute();
    }

    private class LoadHighScoresTask extends AsyncTask<Void, Void, List<DatabaseHelper.HighScore>> {
        @Override
        protected List<DatabaseHelper.HighScore> doInBackground(Void... voids) {
            try {
                return databaseHelper != null ? databaseHelper.getAllHighScores() : new ArrayList<>();
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<DatabaseHelper.HighScore> result) {
            if (result == null) {
                showError("Lỗi tải dữ liệu");
                return;
            }

            highScores = result;
            updateUI();
        }
    }
    public void onBackClick(View view) {
        finish(); // Quay lại màn hình trước
    }
    private void updateUI() {
        runOnUiThread(() -> {
            if (highScores.isEmpty()) {
                emptyListTextView.setText("Chưa có điểm số nào!");
                emptyListTextView.setVisibility(View.VISIBLE);
                highScoresListView.setVisibility(View.GONE);
            } else {
                emptyListTextView.setVisibility(View.GONE);
                highScoresListView.setVisibility(View.VISIBLE);

                if (adapter == null) {
                    adapter = new HighScoreAdapter(LeaderboardActivity.this, highScores);
                    highScoresListView.setAdapter(adapter);
                } else {
                    adapter.notifyDataSetChanged();
                }
            }
        });
    }

    private void showError(String message) {
        runOnUiThread(() -> {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            emptyListTextView.setText(message);
            emptyListTextView.setVisibility(View.VISIBLE);
            highScoresListView.setVisibility(View.GONE);
        });
    }

    @Override
    protected void onDestroy() {
        if (databaseHelper != null) {
            databaseHelper.close();
        }
        super.onDestroy();
    }

    // Adapter với xử lý lỗi
    private static class HighScoreAdapter extends BaseAdapter {
        private final List<DatabaseHelper.HighScore> highScores;
        private final Context context;
        private final LayoutInflater inflater;

        public HighScoreAdapter(Context context, List<DatabaseHelper.HighScore> highScores) {
            this.context = context;
            this.highScores = highScores != null ? highScores : new ArrayList<>();
            this.inflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return highScores.size();
        }

        @Override
        public Object getItem(int position) {
            return position < highScores.size() ? highScores.get(position) : null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_high_score, parent, false);
                holder = new ViewHolder();
                holder.positionText = convertView.findViewById(R.id.tvPosition);
                holder.scoreText = convertView.findViewById(R.id.tvScore);
                holder.modeText = convertView.findViewById(R.id.tvGameMode);
                holder.timeText = convertView.findViewById(R.id.tvTimestamp);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            try {
                DatabaseHelper.HighScore current = highScores.get(position);

                // Đặt màu top 3
                int[] colors = {0xFFFFD700, 0xFFC0C0C0, 0xFFCD7F32};
                if (position < 3) {
                    holder.positionText.setTextColor(colors[position]);
                } else {
                    holder.positionText.setTextColor(0xFF000000);
                }

                holder.positionText.setText(String.valueOf(position + 1));
                holder.scoreText.setText(String.valueOf(current.getScore()));
                holder.modeText.setText(current.getGameMode() != null ? current.getGameMode() : "N/A");
                holder.timeText.setText(current.getTimestamp() != null ? current.getTimestamp() : "N/A");

            } catch (Exception e) {
                holder.positionText.setText("N/A");
                holder.scoreText.setText("N/A");
                holder.modeText.setText("N/A");
                holder.timeText.setText("N/A");
            }

            return convertView;
        }

        private static class ViewHolder {
            TextView positionText;
            TextView scoreText;
            TextView modeText;
            TextView timeText;
        }
    }
}