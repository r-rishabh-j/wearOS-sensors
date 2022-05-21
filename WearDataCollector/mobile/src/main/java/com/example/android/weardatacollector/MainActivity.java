package com.example.android.weardatacollector;

import androidx.activity.result.ActivityResultLauncher;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.weardatacollector.databinding.ActivityMainBinding;


import com.google.android.gms.wearable.DataClient;

import com.google.android.gms.wearable.MessageClient;

import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;

public class MainActivity extends Activity {
    public DataClient dataClient;
    MessageClient messageClient;
    MessageListener messageListener;
    ActivityMainBinding binding;
    ActivityResultLauncher<Intent> activityResultLauncher;

    String STREAM_CAPABILITY_ID = "ppg_data_stream";
    String PPG_DATA_PATH = "/ppg/";
    String ACC_DATA_PATH = "/acc/";
    String GYRO_DATA_PATH = "/gyro/";
    String message_path = "/wear_message";
    ArrayList<Exercise> activityList;
    CustomListAdapter customAdapter;
    ListView listView;
    TextView text;
    Button button2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getActionBar();
        messageClient = Wearable.getMessageClient(this);
        messageListener = new MessageListener();
        messageListener.message_path = message_path;
        listView = findViewById(R.id.ListView);
        text = findViewById(R.id.t1);
        button2 = findViewById(R.id.btn3);

        //Permissions

        if (checkPermission()) {
            Log.d("MainActivity", "Permission Already Granted");
        } else {
            requestPermission();
        }

    }

    public void addItem(String activity, String timestamp) {
        Exercise exercise = new Exercise(activity, timestamp);
        activityList.add(exercise);
        PrefConfig.writeListInPref(getApplicationContext(), activityList);
        listView.setAdapter(customAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        messageClient.addListener(messageListener);
        dataClient = Wearable.getDataClient(this);
        activityList = PrefConfig.readListFromPref(this);
        if (activityList == null)
            activityList = new ArrayList<Exercise>();
        customAdapter = new CustomListAdapter(this, R.layout.card_layout_main, activityList);
        listView.setAdapter(customAdapter);
        messageListener.setListener(new MessageListener.MessageCallback() {
            @Override
            public void messageNotify(String timestamp, String activity) {
                // add code for appending to list View. Add arguments accordingly
                activityList = PrefConfig.readListFromPref(getApplicationContext());
                if (activityList == null)
                    activityList = new ArrayList<Exercise>();
                customAdapter = new CustomListAdapter(getApplicationContext(), R.layout.card_layout_main, activityList);
                listView.setAdapter(customAdapter);
                String element = timestamp + "|" + activity;
                addItem(activity, timestamp);
            }
        });
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("MainAc", "del triggered");
                delete();
            }
        });

        //list view listner and alert dialog box
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int j, long l) {
                final AlertDialog.Builder alertDialog;
                alertDialog = new AlertDialog.Builder(MainActivity.this);
                alertDialog.setTitle("Save Data");
                alertDialog.setMessage("Do you want to sync data for this activity ?");
                alertDialog.setPositiveButton("SYNC", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Log.d("INDEX", "" + j);
                        Exercise element = activityList.get(j);
                        String timestamp = element.timestamp;
                        String activity = element.name;
                        text.setText("Syncing....");
                        syncData(activity, timestamp);
                    }

                });
                alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Toast.makeText(getApplicationContext(), "Cancelled", Toast.LENGTH_SHORT).show();
                    }
                });
                AlertDialog alert = alertDialog.create();
                alert.show();
            }
        });

    }

    public void delete() {
        DeleteThread deleteThread = new DeleteThread();
        deleteThread.capabilityClient = Wearable.getCapabilityClient(this);
        deleteThread.dataClient = Wearable.getDataClient(this);
        deleteThread.STREAM_CAPABILITY_ID = STREAM_CAPABILITY_ID;
        deleteThread.setListener(new DeleteThread.SyncCallbacks() {
            @Override
            public void onSyncComplete() {
                PrefConfig.removeData(getApplicationContext());
                recreate();
            }
        });
        deleteThread.start();
    }

    //Permission Functions
    void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(Uri.parse(String.format("package:%s", getApplicationContext().getPackageName())));
                //activityResultLauncher.launch(intent);
                startActivity(intent);
            } catch (Exception e) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
            }
        }
    }

    boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        return false;
    }
    //Permission over

    void syncData(String activity, String timestamp) {
        SyncThread syncThread = new SyncThread();
        syncThread.context = getApplicationContext();
        syncThread.STREAM_CAPABILITY_ID = STREAM_CAPABILITY_ID;
        syncThread.PPG_DATA_PATH = PPG_DATA_PATH + timestamp;
        syncThread.ACC_DATA_PATH = ACC_DATA_PATH + timestamp;
        syncThread.GYRO_DATA_PATH = GYRO_DATA_PATH + timestamp;
        syncThread.timestamp = timestamp;
        syncThread.activity_name = activity;
        syncThread.setListener(new SyncThread.SyncCallbacks() {
            @Override
            public void onSyncComplete(int result) {
                text.setText("Data Synced");
                Toast.makeText(getApplicationContext(), "Synced", Toast.LENGTH_SHORT).show();
            }
        });
        syncThread.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        messageClient.removeListener(messageListener);
    }
}