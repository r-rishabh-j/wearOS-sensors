package com.example.android.weardatacollector;

public class DataHandle {
    String timestamp;
    String activity_name;
    String STREAM_CAPABILITY_ID;
    String PPG_DATA_PATH;
    String ACC_DATA_PATH;
    String GYRO_DATA_PATH;

    public DataHandle(String STREAM_CAPABILITY_ID, String PPG_DATA_PATH, String ACC_DATA_PATH, String GYRO_DATA_PATH, String timestamp, String activity_name) {
        this.STREAM_CAPABILITY_ID = STREAM_CAPABILITY_ID;
        this.PPG_DATA_PATH = PPG_DATA_PATH;
        this.GYRO_DATA_PATH = GYRO_DATA_PATH;
        this.ACC_DATA_PATH = ACC_DATA_PATH;
        this.activity_name = activity_name;
        this.timestamp = timestamp;
    }
}
