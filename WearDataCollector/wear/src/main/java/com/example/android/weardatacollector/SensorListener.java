package com.example.android.weardatacollector;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.VibrationEffect;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.os.Vibrator;

import androidx.annotation.NonNull;

import com.example.android.weardatacollector.databinding.SensorPageBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;

public class SensorListener extends Activity implements SensorEventListener {
    private Sensor accelerometer;
    private Sensor ppgHandle;
    private Sensor gyroscope;
    private SensorManager sensorManager;
    private SensorPageBinding binding;
    private Vibrator vibrator;
    DataClient dataClient;

    private TextView statusText;
    private Button cancelButton;
    private Button restButton;

    private int PPG_SENSOR = -2; // -2 if ppg not found
    private Long ppgDataCount = 0L;
    private Long ppgDataItemsCount = 0L;
    private Long accDataCount = 0L;
    private Long accDataItemsCount = 0L;
    private Long gyroDataCount = 0L;
    private Long gyroDataItemsCount = 0L;
    private Integer EXERCISE_INTERVAL = 5;
    private Integer REST_INTERVAL = 5;

    Long LIST_SIZE_LIMIT = 800L;
    private final long[] vibrationPattern = {0, 200, 200};
    int[] PPG_FLAG2 = {-2, -2}; // separator for activity->alarm transition or rest->alarm
    int[] PPG_FLAG1 = {-1, -1}; // separator for alarm->rest or rest->alarm
    float[] ACC_FLAG2 = {-2, -2, -2, -2, -2, -2, -2};
    float[] ACC_FLAG1 = {-1, -1, -1, -1, -1, -1, -1};
    private Integer ROUND_LIMIT = 5;
    private int CURRENT_ROUNDS = 0;

    String USER_STATE = "STALE";
    String activity = "RUN";
    String timestamp = "";
    String PPG_PATH = "/ppg/";
    String ACC_PATH = "/acc/";
    String GYRO_PATH = "/gyro/";
    Boolean started = null;

    TimerThread timerThread;
    HandlerThread handlerThread;
    Handler syncHandler;

    private List<int[]> ppgDataCache = new ArrayList<>();
    private List<float[]> accDataCache = new ArrayList<>();
    private List<float[]> gyroDataCache = new ArrayList<>();

    PutDataMapRequest dataMapRequestPPG;
    PutDataMapRequest dataMapRequestACC;
    PutDataMapRequest dataMapRequestGYRO;

    private Asset createAsset(byte[] arr) {
        return Asset.createFromBytes(arr);
    }

    private String ppgKey() {
        return "ppg_" + activity + "_" + timestamp + "_" + ppgDataItemsCount;
    }

    private String accKey() {
        return "acc_" + activity + "_" + timestamp + "_" + accDataItemsCount;
    }

    private String gyroKey() {
        return "gyro_" + activity + "_" + timestamp + "_" + gyroDataItemsCount;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            activity = extras.getString("ACTIVITY");
            timestamp = extras.getString("TIMESTAMP");
            EXERCISE_INTERVAL = extras.getInt("EXERCISE_INTERVAL");
            REST_INTERVAL = extras.getInt("REST_INTERVAL");
            ROUND_LIMIT = extras.getInt("CYCLES");
            Log.d("PARAMS: ", activity + " " + timestamp);
        } else {
            finish();
        }
        PPG_PATH = PPG_PATH + timestamp;
        ACC_PATH = ACC_PATH + timestamp;
        GYRO_PATH = GYRO_PATH + timestamp;
        dataMapRequestPPG = PutDataMapRequest.create(PPG_PATH);
        dataMapRequestACC = PutDataMapRequest.create(ACC_PATH);
        dataMapRequestGYRO = PutDataMapRequest.create(GYRO_PATH);
        dataClient = Wearable.getDataClient(this);
        binding = SensorPageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        cancelButton = findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                vibrator.cancel();
                timerThread.interrupt();
                endProcedure();
                finish();
            }
        });
        restButton = findViewById(R.id.restButton);
        statusText = findViewById(R.id.statusText);
        restButton.setClickable(false);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER_UNCALIBRATED);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        //To list all the available sensor
        List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL);

        for (Sensor sensor : sensorList) {
            String name = sensor.getStringType();
            if (name.contains("ppg")) {
                PPG_SENSOR = sensor.getType();
                ppgHandle = sensorManager.getDefaultSensor(PPG_SENSOR);
                Log.d("sensorFound: ", "name: " + name + " " + sensor.getType());
                break;
            }
        }
        started = false;
        restButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (USER_STATE.equals("REST")) {
                    // append -2 to queue
                    ppgDataCache.add(PPG_FLAG2);
                    accDataCache.add(ACC_FLAG2);
                    gyroDataCache.add(ACC_FLAG2);
                    // disable vibration
                    vibrator.cancel();
                    Log.d("Timer:", "Clicked button. Rest to work");
                    restButton.setClickable(false);
                    restButton.setText("");
                    USER_STATE = "WORKING";
                    statusText.setText(activity);
                    timerThread = new TimerThread();
                    timerThread.TIME_LIMIT = EXERCISE_INTERVAL;

                    timerThread.setTimerListener(new TimerThread.TimerListener() {
                        @Override
                        public void timerListener(int command) {
                            // append -2 to queue
                            ppgDataCache.add(PPG_FLAG2);
                            accDataCache.add(ACC_FLAG2);
                            gyroDataCache.add(ACC_FLAG2);
                            // vibrate
                            vibrator.vibrate(VibrationEffect.createWaveform(vibrationPattern, 0));

                            Log.d("Timer:", "Rest now");
                            String task = ("START RESTING");
                            statusText.setText(task);
                            restButton.setText("REST");
                            restButton.setClickable(true);
                        }
                    });
                    timerThread.start();
                } else if (USER_STATE.equals("WORKING")) {
                    // user needs to stop
                    // disable vibration
                    vibrator.cancel();
                    // append -1 to ppg and acc queue
                    ppgDataCache.add(PPG_FLAG1);
                    accDataCache.add(ACC_FLAG1);
                    gyroDataCache.add(ACC_FLAG1);

                    CURRENT_ROUNDS++;
                    Log.d("Timer:", "Clicked. Work to REST");
                    USER_STATE = "REST";
                    statusText.setText("RESTING");
                    restButton.setText("");
                    timerThread = new TimerThread();
                    timerThread.TIME_LIMIT = REST_INTERVAL;
                    restButton.setClickable(false);
                    timerThread.setTimerListener(new TimerThread.TimerListener() {
                        @Override
                        public void timerListener(int command) {
                            // append -1 to queue
                            ppgDataCache.add(PPG_FLAG1);
                            accDataCache.add(ACC_FLAG1);
                            gyroDataCache.add(ACC_FLAG1);
                            if (CURRENT_ROUNDS <= ROUND_LIMIT) {
                                Log.d("Timer:", "Start working");
                                String task = "START\n" + activity;
                                vibrator.vibrate(VibrationEffect.createWaveform(vibrationPattern, 0));
                                statusText.setText(task);
                                restButton.setText("CONTINUE");
                                restButton.setClickable(true);
                            } else {
                                Log.d("Timer:", "Finishing...");
                                statusText.setText("FINISHING...WAIT");
                                endProcedure();
                            }
                        }
                    });
                    timerThread.start();
                }
            }
        });
    }

    /**
     * Function to clear up resources, sync remaining data items
     */
    private void endProcedure() {
        // unregister listeners
        sensorManager.unregisterListener(this);
        vibrator.cancel();
        syncHandler.post(new Runnable() {
            @Override
            public void run() {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        // clean up code
                        // push remaining data to datalayer
                        if (ppgDataCache.size() > 0) {
                            String ppg_key = ppgKey();
                            ppgDataItemsCount++;
                            pushToDataLayer(convertPpgArrayToBytes(ppgDataCache), ppg_key, PPG_PATH);
                            ppgDataCache.clear();
                        }

                        // acc data
                        if (accDataCache.size() > 0) {
                            String acc_key = accKey();
                            accDataItemsCount++;
                            pushToDataLayer(convertAccArrayToBytes(accDataCache), acc_key, ACC_PATH);
                            accDataCache.clear();
                        }

                        if (gyroDataCache.size() > 0) {
                            String gyro_key = gyroKey();
                            gyroDataItemsCount++;
                            pushToDataLayer(convertAccArrayToBytes(gyroDataCache), gyro_key, GYRO_PATH);
                            gyroDataCache.clear();
                        }
                        finish();
                    }
                });
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // thread used for syncing data
        if (started)
            return;
        started = true;
        Log.d("SensorListener", "START");
        handlerThread = new HandlerThread("syncThread");
        handlerThread.start();
        syncHandler = new Handler(handlerThread.getLooper());

        // timing thread
        timerThread = new TimerThread();
        timerThread.TIME_LIMIT = EXERCISE_INTERVAL;
        USER_STATE = "WORKING";
        statusText.setText(activity);
        restButton.setText("");
        timerThread.setTimerListener(new TimerThread.TimerListener() {
            @Override
            public void timerListener(int command) {
                CURRENT_ROUNDS++;
                Log.d("Timer:", "START REST");
                restButton.setClickable(true);
                restButton.setText("REST");
                statusText.setText("START REST");
                // start vibration
                vibrator.vibrate(VibrationEffect.createWaveform(vibrationPattern, 0));
                // append -2 to acc and ppg queue
                ppgDataCache.add(PPG_FLAG2);
                accDataCache.add(ACC_FLAG2);
                gyroDataCache.add(ACC_FLAG2);
            }
        });
        restButton.setClickable(false);
        timerThread.start();
        sensorManager.registerListener(this, this.accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, this.ppgHandle, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, this.gyroscope, SensorManager.SENSOR_DELAY_FASTEST);
    }

    private byte[] convertPpgArrayToBytes(List<int[]> array) {
        // int[] is of dim 1x2
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        int dataSetSize = array.size();
        for (int i = 0; i < dataSetSize; i++) {
            try {
                dos.writeInt(array.get(i)[0]);
                dos.writeInt(array.get(i)[1]);
            } catch (Exception e) {
                Log.d("Converter Error:", "" + e);
            }
        }

        return baos.toByteArray();
    }

    private byte[] convertAccArrayToBytes(List<float[]> array) {
        // 1x4
        ByteArrayOutputStream bas = new ByteArrayOutputStream();
        DataOutputStream ds = new DataOutputStream(bas);
        for (int i = 0; i < array.size(); i++) {
            try {
                ds.writeFloat(array.get(i)[0]); //t
                ds.writeFloat(array.get(i)[1]); //x
                ds.writeFloat(array.get(i)[2]); //y
                ds.writeFloat(array.get(i)[3]); //z
                ds.writeFloat(array.get(i)[4]); //bx
                ds.writeFloat(array.get(i)[5]); //by
                ds.writeFloat(array.get(i)[6]); //bz
            } catch (Exception e) {
                Log.d("convertAccArrayToBytes", "ByteArrayOutput error");
            }
        }
        return bas.toByteArray();
    }

    private void pushToDataLayer(byte[] array, String key, String path) {
        if (path.equals(PPG_PATH)) {
            dataMapRequestPPG.getDataMap().putAsset(key, createAsset(array));
            PutDataRequest request = dataMapRequestPPG.asPutDataRequest();
            Task<DataItem> putTask = dataClient.putDataItem(request);
            putTask.addOnSuccessListener(new OnSuccessListener<DataItem>() {
                @Override
                public void onSuccess(DataItem dataItem) {
                    Log.d("pushToDataLayer", "DATA ITEM " + path + "/" + key + " pushed");
                }
            });
            putTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d("pushToDataLayer", "DATA ITEM " + "/" + key + " push FAIL");
                }
            });
        } else if (path.equals(ACC_PATH)) {
            dataMapRequestACC.getDataMap().putAsset(key, createAsset(array));
            PutDataRequest request = dataMapRequestACC.asPutDataRequest();
            Task<DataItem> putTask = dataClient.putDataItem(request);
            putTask.addOnSuccessListener(new OnSuccessListener<DataItem>() {
                @Override
                public void onSuccess(DataItem dataItem) {
                    Log.d("pushToDataLayer", "DATA ITEM " + path + "/" + key + " pushed");
                }
            });
            putTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d("pushToDataLayer", "DATA ITEM " + "/" + key + " push FAIL");
                }
            });
        } else {
            dataMapRequestGYRO.getDataMap().putAsset(key, createAsset(array));
            PutDataRequest request = dataMapRequestGYRO.asPutDataRequest();
            Task<DataItem> putTask = dataClient.putDataItem(request);
            putTask.addOnSuccessListener(new OnSuccessListener<DataItem>() {
                @Override
                public void onSuccess(DataItem dataItem) {
                    Log.d("pushToDataLayer", "DATA ITEM " + path + "/" + key + " pushed");
                }
            });
            putTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d("pushToDataLayer", "DATA ITEM " + "/" + key + " push FAIL");
                }
            });
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == PPG_SENSOR) {
            int[] data = {(int) (sensorEvent.timestamp / 1000000), (int) (sensorEvent.values[0])};
            ppgDataCache.add(data);
            ppgDataCount++;
            if (ppgDataCount > LIST_SIZE_LIMIT) {
                // put looper request
                String key = new String(ppgKey());
                ppgDataItemsCount++;
                List<int[]> data1 = new ArrayList<>(ppgDataCache);
                syncHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        pushToDataLayer(convertPpgArrayToBytes(data1), key, PPG_PATH);
                        data1.clear();
                    }
                });
                // clear local var
                ppgDataCache.clear();
                ppgDataCount = 0L;
            }
        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER_UNCALIBRATED) {
            float[] data = {(float) (sensorEvent.timestamp / 1000000), (float) sensorEvent.values[0], (float) sensorEvent.values[1], (float) sensorEvent.values[2],
                    (float) sensorEvent.values[3], (float) sensorEvent.values[4], (float) sensorEvent.values[5]};
            accDataCache.add(data);
            accDataCount++;
            if (accDataCount > LIST_SIZE_LIMIT) {
                // put looper request
                String key = new String(accKey());
                accDataItemsCount++;
                List<float[]> data1 = new ArrayList<>(accDataCache);
                syncHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("Accelerometer", "Pushing " + key);
                        pushToDataLayer(convertAccArrayToBytes(data1), key, ACC_PATH);
                        data1.clear();
                    }
                });
                // clear local var
                accDataCache.clear();
                accDataCount = 0L;
            }
        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE_UNCALIBRATED) {
            float[] data = {(float) (sensorEvent.timestamp / 1000000), (float) (sensorEvent.values[0]), (float) (sensorEvent.values[1]), (float) (sensorEvent.values[2]),
                    (float) sensorEvent.values[3], (float) sensorEvent.values[4], (float) sensorEvent.values[5]};
            gyroDataCache.add(data);
            gyroDataCount++;
            if (gyroDataCount > LIST_SIZE_LIMIT) {
                // put looper request
                String key = new String(gyroKey());
                gyroDataItemsCount++;
                List<float[]> data1 = new ArrayList<>(gyroDataCache);
                syncHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("Gyroscope", "Pushing " + key);
                        pushToDataLayer(convertAccArrayToBytes(data1), key, GYRO_PATH);
                        data1.clear();
                    }
                });
                // clear local var
                gyroDataCache.clear();
                gyroDataCount = 0L;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            Log.d("AccuracyChanged:", "accuracy changed for Accelerometer : " + i);
        else if (sensor.getType() == PPG_SENSOR)
            Log.d("AccuracyChanged:", "accuracy changed for PPG: " + i);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("SensorListener:", "DESTROY");
        timerThread.interrupt();
        handlerThread.quit();
        vibrator.cancel();
        sensorManager.unregisterListener(this);
    }
}
