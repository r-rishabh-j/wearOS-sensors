package com.example.android.weardatacollector;

import android.util.Log;

import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;

import java.nio.charset.StandardCharsets;

public class MessageListener implements MessageClient.OnMessageReceivedListener {

    MessageCallback listener;
    String message_path;

    public void setListener(MessageCallback listener) {
        this.listener = listener;
    }

    public interface MessageCallback {
        public void messageNotify(String timestamp, String activity);
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d("Message: ", "" + messageEvent.getPath());
        if (messageEvent.getPath().equals(message_path)) {
            String activity;
            String timestamp;
            // parse message data
            String msg = new String(messageEvent.getData(), StandardCharsets.UTF_8);
            Log.d("Message: ", msg);
            int act_index = msg.lastIndexOf('|');
            activity = msg.substring(act_index + 1);
            msg = msg.substring(0, act_index);
            int timestamp_index = msg.lastIndexOf('|');
            timestamp = msg.substring(timestamp_index + 1);
            listener.messageNotify(timestamp, activity);
        }
    }
}
