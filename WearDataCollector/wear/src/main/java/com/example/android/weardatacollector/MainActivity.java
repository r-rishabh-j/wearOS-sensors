package com.example.android.weardatacollector;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.android.weardatacollector.Exercises.SelectExercise;
import com.example.android.weardatacollector.databinding.ActivityMainBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.text.SimpleDateFormat;
import java.util.Set;


public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getName();
    private ActivityMainBinding binding;
    private MessageClient messageClient;
    Button startButton;
    Button changeActivityButton;
    private String activitySelected = "";
    private int EXERCISE_INTERVAL;
    private int REST_INTERVAL;
    private int CYCLES;
    private String timestamp;
    private String PHONE_CAPABILITY = "DATA_RECEIVER";
    private final static int ACT_REQUEST_CODE = 10;
    private TextView selectActView;

    private String getPhoneID(Set<Node> nodes) {
        String bestNodeId = null;
        for (Node node : nodes) {
            if (node.isNearby()) {
                return node.getId();
            }
            bestNodeId = node.getId();
        }
        return bestNodeId;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        startButton = findViewById(R.id.startButton);
        selectActView = findViewById(R.id.selectedActivityView);
        MessageClient messageClient = Wearable.getMessageClient(this);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!activitySelected.equals("")) {
                    timestamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date());
                    String message_payload = "PPG_DATA_COLLECTOR_|" + timestamp + "|" + activitySelected;
                    byte[] message = message_payload.getBytes();
                    Task<CapabilityInfo> capabilityInfo = Wearable.getCapabilityClient(getApplicationContext()).getCapability(PHONE_CAPABILITY, CapabilityClient.FILTER_REACHABLE);
                    capabilityInfo.addOnCompleteListener(new OnCompleteListener<CapabilityInfo>() {
                        @Override
                        public void onComplete(@NonNull Task<CapabilityInfo> task) {
                            CapabilityInfo capabilityInfo1 = task.getResult();
                            String phone_id = getPhoneID(capabilityInfo1.getNodes());
                            if (phone_id != null) {
                                Log.d("Message", "" + phone_id);
                                Task<Integer> sendTask = messageClient.sendMessage(phone_id, "/wear_message", message);
                                sendTask.addOnCompleteListener(new OnCompleteListener<Integer>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Integer> task) {
                                        // also send timestamp and activity info
                                        Intent intent = new Intent(getApplicationContext(), ConnectionConfirmPage.class);
                                        intent.putExtra("ACTIVITY", activitySelected);
                                        intent.putExtra("EXERCISE_INTERVAL", EXERCISE_INTERVAL);
                                        intent.putExtra("REST_INTERVAL", REST_INTERVAL);
                                        intent.putExtra("CYCLES", CYCLES);
                                        intent.putExtra("TIMESTAMP", timestamp);
                                        startActivity(intent);
                                    }
                                });
                            }
                        }
                    });
                }
            }
        });
        changeActivityButton = findViewById(R.id.changeActivity);
        changeActivityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // launch an intent with list-view of activities
                // get activity
                startActivityForResult(new Intent(getApplicationContext(), SelectExercise.class), ACT_REQUEST_CODE);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == ACT_REQUEST_CODE) {
                if (data != null) {
                    activitySelected = data.getStringExtra("ACTIVITY");
                    EXERCISE_INTERVAL = data.getIntExtra("EXERCISE_INTERVAL", 5);
                    REST_INTERVAL = data.getIntExtra("REST_INTERVAL", 5);
                    CYCLES = data.getIntExtra("CYCLES", 2);
                    selectActView.setText(activitySelected);
                }
            }
        }
    }
}