package com.example.racingsensor;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class CarShopActivity extends AppCompatActivity {

    private ImageView imgCar;
    private TextView tvCarName, tvSpeed, tvHandling, tvAcceleration;
    private int currentIndex = 0;

    private int[] carImages = {
            R.drawable.car_street,
            R.drawable.car_racer,
            R.drawable.car_truck
    };

    private String[] carNames = {
            "Street",
            "Racer",
            "Truck"
    };

    private String[] carSpeeds = {
            "237 mph",
            "260 mph",
            "210 mph"
    };

    private String[] carHandlings = {
            "1.27 g/s",
            "1.35 g/s",
            "1.10 g/s"
    };

    private String[] carAccelerations = {
            "4.49 s",
            "3.90 s",
            "5.20 s"
    };

    private int selectedColor = 0; // 0 = White, 1 = Blue, 2 = Black
    private View colorWhite, colorBlue, colorBlack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_shop);

        imgCar = findViewById(R.id.imgCar);
        tvCarName = findViewById(R.id.tvCarName);
        tvSpeed = findViewById(R.id.tvSpeed);
        tvHandling = findViewById(R.id.tvHandling);
        tvAcceleration = findViewById(R.id.tvAcceleration);

        colorWhite = findViewById(R.id.colorWhite);
        colorBlue = findViewById(R.id.colorBlue);
        colorBlack = findViewById(R.id.colorBlack);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnLeft).setOnClickListener(v -> {
            currentIndex = (currentIndex - 1 + carImages.length) % carImages.length;
            updateCarDisplay();
        });

        findViewById(R.id.btnRight).setOnClickListener(v -> {
            currentIndex = (currentIndex + 1) % carImages.length;
            updateCarDisplay();
        });

        findViewById(R.id.btnSelectCar).setOnClickListener(v -> {
            String colorName = getColorName(selectedColor);
            String message = "Selected: " + carNames[currentIndex] + " (" + colorName + ")";
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });

        colorWhite.setOnClickListener(v -> selectColor(0));
        colorBlue.setOnClickListener(v -> selectColor(1));
        colorBlack.setOnClickListener(v -> selectColor(2));

        updateCarDisplay();
        selectColor(0);
    }

    private void updateCarDisplay() {
        imgCar.setImageResource(carImages[currentIndex]);
        tvCarName.setText(carNames[currentIndex]);
        tvSpeed.setText("Speed: " + carSpeeds[currentIndex]);
        tvHandling.setText("Handling: " + carHandlings[currentIndex]);
        tvAcceleration.setText("Acceleration: " + carAccelerations[currentIndex]);
    }

    private void selectColor(int colorIndex) {
        selectedColor = colorIndex;

        // Đặt background để làm nổi bật màu đã chọn
        colorWhite.setAlpha(colorIndex == 0 ? 1f : 0.3f);
        colorBlue.setAlpha(colorIndex == 1 ? 1f : 0.3f);
        colorBlack.setAlpha(colorIndex == 2 ? 1f : 0.3f);
    }

    private String getColorName(int colorIndex) {
        switch (colorIndex) {
            case 0: return "White";
            case 1: return "Blue";
            case 2: return "Black";
            default: return "Unknown";
        }
    }
}
