package com.example.android.weardatacollector;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.example.android.weardatacollector.databinding.ConnectionConfirmBinding;

public class ConnectionConfirmPage extends Activity {
    ConnectionConfirmBinding binding;
    Button cancelButton;
    Button continueButton;
    String activitySelected = "";
    String timestamp = "";
    private int EXERCISE_INTERVAL;
    private int REST_INTERVAL;
    private int CYCLES;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            activitySelected = extras.getString("ACTIVITY");
            timestamp = extras.getString("TIMESTAMP");
            EXERCISE_INTERVAL = extras.getInt("EXERCISE_INTERVAL");
            REST_INTERVAL = extras.getInt("REST_INTERVAL");
            CYCLES = extras.getInt("CYCLES");

        }
        binding = ConnectionConfirmBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        cancelButton = findViewById(R.id.backButton);
        continueButton = findViewById(R.id.continueButton);

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // redirect to
                Intent intent = new Intent(getApplicationContext(), SensorListener.class);
                intent.putExtra("ACTIVITY", activitySelected);
                intent.putExtra("EXERCISE_INTERVAL", EXERCISE_INTERVAL);
                intent.putExtra("REST_INTERVAL", REST_INTERVAL);
                intent.putExtra("CYCLES", CYCLES);
                intent.putExtra("TIMESTAMP", timestamp);
                startActivity(intent);
                finish();
            }
        });

    }
}
