package com.example.android.weardatacollector;

import android.content.Context;
import android.net.Uri;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.os.Handler;

public class SyncThread extends Thread {
    Context context;
    String timestamp;
    String activity_name;
    String STREAM_CAPABILITY_ID;
    String PPG_DATA_PATH;
    String ACC_DATA_PATH;
    String GYRO_DATA_PATH;
    SyncCallbacks syncCallbacks;
    int result = 0;

    public interface SyncCallbacks {
        public void onSyncComplete(int result);
    }

    public void setListener(SyncCallbacks syncCallbacks) {
        this.syncCallbacks = syncCallbacks;
    }

    private final Handler handler = new Handler(Looper.getMainLooper());

    private String findNode(CapabilityInfo capabilityInfo) {
        Set<Node> nodes = capabilityInfo.getNodes();
        String node_id = "";
        Log.d("MainActivity.findNode", "Checking Connected nodes");
        for (Node node : nodes) {
            if (node.isNearby()) {
                return node.getId();
            }
        }
        return node_id;
    }

    @Override
    public void run() {
        try {
            Log.d("CALLBACK_IN", Thread.currentThread().toString());
            result = syncData(this.context, new DataHandle(this.STREAM_CAPABILITY_ID, this.PPG_DATA_PATH, this.ACC_DATA_PATH, this.GYRO_DATA_PATH, this.timestamp, this.activity_name));
        } catch (Exception e) {
            result = -2;
            Log.d("Thread:", "Error: " + e);
        } finally {
            this.handler.post(new Runnable() {
                @Override
                public void run() {
                    Log.d("Thread: ", "Executing Main thread callback");
                    syncCallbacks.onSyncComplete(result);
                }
            });
        }
    }

    List<int[]> ppgByte2Array(InputStream inputStream) {
        if (inputStream == null) {
            return null;
        }
        try {
            int total_bytes = inputStream.available();
            if (total_bytes % 8 != 0) {
                throw new Exception("Invalid byte string");
            }
            byte[] arr = new byte[total_bytes];
            int total = inputStream.read(arr);
            if (total != total_bytes) {
                throw new Exception("Invalid byte string");
            }
            List<int[]> result = new ArrayList<>();
            ByteBuffer wrapped = ByteBuffer.wrap(arr);
            for (int i = 0; i < total_bytes / 8; i++) {
                int[] data_point = new int[2];
                data_point[0] = wrapped.getInt();
                data_point[1] = wrapped.getInt();
                result.add(data_point);
            }
            inputStream.close();
            return result;
        } catch (Exception e) {
            Log.e("SyncThread4", "" + e);
        }
        return null;
    }

    List<float[]> accByte2Array(InputStream inputStream) {
        if (inputStream == null) {
            return null;
        }
        try {
            int total_bytes = inputStream.available();
            if (total_bytes % 28 != 0) {
                throw new Exception("Invalid byte string");
            }
            byte[] arr = new byte[total_bytes];
            int total = inputStream.read(arr);
            if (total != total_bytes) {
                throw new Exception("Invalid byte string");
            }
            List<float[]> result = new ArrayList<>();
            ByteBuffer wrapped = ByteBuffer.wrap(arr);
            for (int i = 0; i < total_bytes / 28; i++) {
                float[] data_point = new float[7];
                data_point[0] = wrapped.getFloat();
                data_point[1] = wrapped.getFloat();
                data_point[2] = wrapped.getFloat();
                data_point[3] = wrapped.getFloat();
                data_point[4] = wrapped.getFloat();
                data_point[5] = wrapped.getFloat();
                data_point[6] = wrapped.getFloat();
                result.add(data_point);
            }
            inputStream.close();
            return result;
        } catch (Exception e) {
            Log.e("SyncThread1", "" + e);
        }
        return null;
    }

    // to be used with sync button
    public int syncData(Context context, DataHandle dataHandle) throws Exception {
        DataClient dataClient = Wearable.getDataClient(context);
        CapabilityInfo capabilityInfo = Tasks.await(Wearable.getCapabilityClient(context).getCapability(dataHandle.STREAM_CAPABILITY_ID, CapabilityClient.FILTER_REACHABLE));

        String ppg_root = "ppg_" + dataHandle.activity_name + "_" + dataHandle.timestamp + "_";
        String acc_root = "acc_" + dataHandle.activity_name + "_" + dataHandle.timestamp + "_";
        String gyro_root = "gyro_" + dataHandle.activity_name + "_" + dataHandle.timestamp + "_";

        String node_id = findNode(capabilityInfo);
        if (node_id.equals("")) {
            return -1;
            /*
             * check null condition and perform appropriate error messages on frontend
             * */
        }
        Uri uri_ppg = new Uri.Builder().scheme(PutDataRequest.WEAR_URI_SCHEME).path(dataHandle.PPG_DATA_PATH).authority(node_id).build();
        Uri uri_acc = new Uri.Builder().scheme(PutDataRequest.WEAR_URI_SCHEME).path(dataHandle.ACC_DATA_PATH).authority(node_id).build();
        Uri uri_gyro = new Uri.Builder().scheme(PutDataRequest.WEAR_URI_SCHEME).path(dataHandle.GYRO_DATA_PATH).authority(node_id).build();
        Log.d("Thread.syncData", "Wear found at: " + uri_ppg.toString());
        Log.d("Thread.syncData", "Wear found at: " + uri_acc.toString());
        Log.d("Thread.syncData", "Wear found at: " + uri_gyro.toString());

        try {
            DataItemBuffer ppgDataBuffer;
            DataItemBuffer accDataBuffer;
            DataItemBuffer gyroDataBuffer;
            try {
                ppgDataBuffer = Tasks.await(dataClient.getDataItems(uri_ppg));
                accDataBuffer = Tasks.await(dataClient.getDataItems(uri_acc));
                gyroDataBuffer = Tasks.await(dataClient.getDataItems(uri_gyro));

            } catch (Exception e) {
                Log.e("SyncThread databuff", "abort");
                return -1;
            }
            CsvManager csvManager = new CsvManager(context, dataHandle.timestamp, dataHandle.activity_name);

            Map<String, Asset> ppgMap = new HashMap<>();
            Map<String, Asset> accMap = new HashMap<>();
            Map<String, Asset> gyroMap = new HashMap<>();

            Log.d("Thread", "Awaited");
            for (DataItem dataItem : ppgDataBuffer) {
                Log.d("PPG", "buff");
                DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItem);
                Log.d("Keys:", "List of keys");
                for (String key : dataMapItem.getDataMap().keySet()) {
                    Log.d("Keys:", key);
                    if (key.toUpperCase().contains(ppg_root.toUpperCase())) {
                        Asset profileAsset = dataMapItem.getDataMap().getAsset(key);
                        ppgMap.put(key, profileAsset);
                    }
                }
            }
            for (DataItem dataItem : accDataBuffer) {
                DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItem);
                Log.d("Keys:", "List of keys");
                for (String key : dataMapItem.getDataMap().keySet()) {
                    Log.d("Keys:", key);
                    if (key.toUpperCase().contains(acc_root.toUpperCase())) {
                        Asset profileAsset = dataMapItem.getDataMap().getAsset(key);
                        accMap.put(key, profileAsset);
                    }
                }
            }
            for (DataItem dataItem : gyroDataBuffer) {
                Log.d("GYRO", "buff");
                DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItem);
                Log.d("Keys:", "List of keys");
                for (String key : dataMapItem.getDataMap().keySet()) {
                    Log.d("Keys:", key);
                    if (key.toUpperCase().contains(gyro_root.toUpperCase())) {
                        Asset profileAsset = dataMapItem.getDataMap().getAsset(key);
                        gyroMap.put(key, profileAsset);
                    }
                }
            }

            for (int k = 0; ; k++) {
                String key = ppg_root + k;
                if (!ppgMap.containsKey(key)) {
                    break;
                }
                Log.d("SyncThread", "syncing " + key);
                Asset profileAsset = ppgMap.get(key);
                if (profileAsset != null) {
                    try {
                        DataClient.GetFdForAssetResponse ppgFD = Tasks.await(dataClient.getFdForAsset(profileAsset));
                        try {
                            InputStream inputStream = ppgFD.getInputStream();
                            List<int[]> ppg = ppgByte2Array(inputStream);
                            Log.d("PPG LEN", "" + ppg.size());
                            csvManager.savePPG(ppg);
                        } catch (Exception e) {
                            Log.d("Thread:", e + "");
                        }
                    } catch (Exception e) {
                        Log.d("Thread", "input stream not opened " + e);
                    }
                }
            }
            for (int k = 0; ; k++) {
                String key = acc_root + k;
                if (!accMap.containsKey(key)) {
                    break;
                }
                Log.d("SyncThread", "syncing " + key);
                Asset profileAsset = accMap.get(key);
                if (profileAsset != null) {
                    try {
                        DataClient.GetFdForAssetResponse accFD = Tasks.await(dataClient.getFdForAsset(profileAsset));
                        try {
                            InputStream inputStream = accFD.getInputStream();
                            List<float[]> acc = accByte2Array(inputStream);
                            Log.d("ACC LEN", "" + acc.size());
                            csvManager.saveAccelerometer(acc);
                        } catch (Exception e) {
                            Log.e("SyncThread2", e + "");
                        }
                    } catch (Exception e) {
                        Log.e("SyncThread", "input stream not opened " + e);
                    }
                }
            }
            for (int k = 0; ; k++) {
                String key = gyro_root + k;
                if (!gyroMap.containsKey(key)) {
                    break;
                }
                Log.d("SyncThread", "syncing " + key);
                Asset profileAsset = gyroMap.get(key);
                if (profileAsset != null) {
                    try {
                        DataClient.GetFdForAssetResponse gyroFD = Tasks.await(dataClient.getFdForAsset(profileAsset));
                        try {
                            InputStream inputStream = gyroFD.getInputStream();
                            List<float[]> gyro = accByte2Array(inputStream);
                            Log.d("GYRO LEN", "" + gyro.size());
                            csvManager.saveGyroScope(gyro);
                        } catch (Exception e) {
                            Log.e("SyncThread2", e + "");
                        }
                    } catch (Exception e) {
                        Log.e("SyncThread", "input stream not opened " + e);
                    }
                }
            }
            ppgDataBuffer.release();
            accDataBuffer.release();
            gyroDataBuffer.release();
        } catch (Exception e) {
            Log.e("SyncThread3", "" + e);
        }
        return 0;
    }
}
