package com.example.racingsensor;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class CarShopActivity extends AppCompatActivity {

    public static final String EXTRA_SELECTED_CAR = "selected_car";

    private ImageView imgCar;
    private ImageButton btnLeft, btnRight, btnBack, btnSelectCar;
    private TextView tvCarName, tvSpeed, tvHandling, tvAcceleration;
    private int currentIndex = 0;

    // Array of car details (names, stats, and images)
    private String[] carNames = {"Street", "Race", "Sport"};
    private String[] speeds = {"Speed: 237 mph", "Speed: 180 mph", "Speed: 220 mph"};
    private String[] handling = {"Handling: 1.27 g/s", "Handling: 1.15 g/s", "Handling: 1.33 g/s"};
    private String[] acceleration = {"Acceleration: 4.49 s", "Acceleration: 5.0 s", "Acceleration: 4.2 s"};
    private int[] carImages = {R.drawable.car, R.drawable.car2, R.drawable.car3};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_shop);

        // Map views to UI elements
        imgCar = findViewById(R.id.imgCar);
        btnLeft = findViewById(R.id.btnLeft);
        btnRight = findViewById(R.id.btnRight);
        btnBack = findViewById(R.id.btnBack);
        btnSelectCar = findViewById(R.id.btnSelectCar);
        tvCarName = findViewById(R.id.tvCarName);
        tvSpeed = findViewById(R.id.tvSpeed);
        tvHandling = findViewById(R.id.tvHandling);
        tvAcceleration = findViewById(R.id.tvAcceleration);

        // Update car info for the initial view
        updateCarInfo();

        // Handle 'previous car' button click
        btnLeft.setOnClickListener(v -> {
            if (currentIndex > 0) {
                currentIndex--;
                updateCarInfo();
            }
        });

        // Handle 'next car' button click
        btnRight.setOnClickListener(v -> {
            if (currentIndex < carNames.length - 1) {
                currentIndex++;
                updateCarInfo();
            }
        });

        // Handle 'back' button click
        btnBack.setOnClickListener(v -> finish());

        // Handle 'select car' button click
        btnSelectCar.setOnClickListener(v -> {
            Intent resultIntent = new Intent(CarShopActivity.this, GameActivity.class);
            resultIntent.putExtra(EXTRA_SELECTED_CAR, currentIndex);
            startActivity(resultIntent);
        });
    }

    // Method to update car information based on the current index
    private void updateCarInfo() {
        // Update text views and image based on the selected car
        tvCarName.setText(carNames[currentIndex]);
        tvSpeed.setText(speeds[currentIndex]);
        tvHandling.setText(handling[currentIndex]);
        tvAcceleration.setText(acceleration[currentIndex]);
        imgCar.setImageResource(carImages[currentIndex]);
    }
}
