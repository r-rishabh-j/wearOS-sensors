package com.example.android.weardatacollector;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataRequest;

import java.util.Set;

public class DeleteThread extends Thread {
    SyncCallbacks syncCallbacks;
    DataClient dataClient;
    String STREAM_CAPABILITY_ID;
    CapabilityClient capabilityClient;
    String TAG = "DeleteThread";
    String[] paths = {"/ppg", "/acc", "gyro"};

    public interface SyncCallbacks {
        public void onSyncComplete();
    }

    public void setListener(SyncCallbacks syncCallbacks) {
        this.syncCallbacks = syncCallbacks;
    }

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void run() {
        try {
            delete(paths);
        } catch (Exception e) {
            Log.d("DeleteThread:", "failed");
        } finally {
            this.handler.post(new Runnable() {
                @Override
                public void run() {
                    Log.d("DeleteThread: ", "Executing Main thread callback");
                    syncCallbacks.onSyncComplete();
                }
            });
        }
    }

    private void delete(String[] paths) {
        try {
            CapabilityInfo capabilityInfo = Tasks.await(capabilityClient.getCapability(STREAM_CAPABILITY_ID, CapabilityClient.FILTER_REACHABLE));
            Set<Node> nodes = capabilityInfo.getNodes();
            String node_id = "";
            Log.d(TAG, "delete: Node Reachable:");
            for (Node node : nodes) {
                Log.d("NODE_REACHABLE", "" + node.isNearby());
                if (node.isNearby()) {
                    node_id = node.getId();
                    break;
                }
            }
            if (node_id.equals("")) {
                Log.d("Watch:", "Not found");
                return;
            }
            Log.d("NODE_ID", node_id);

            for (String path : paths) {
                Uri uri = new Uri.Builder().scheme(PutDataRequest.WEAR_URI_SCHEME).path(path).authority("*").build();
                if (uri == null) {
                    Log.d("Main", "URI null");
                    continue;
                }
                Log.d("Main", "URI " + uri);
                int total_deleted = Tasks.await(dataClient.deleteDataItems(uri, DataClient.FILTER_PREFIX));
                Log.d("Delete Thread", "Deleted " + total_deleted + " from " + path);
            }

        } catch (Exception e) {
            Log.d("Deleted Thread", "delete failed");
        }

    }
}
