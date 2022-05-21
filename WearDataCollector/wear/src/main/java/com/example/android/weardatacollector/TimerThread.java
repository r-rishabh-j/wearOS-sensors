package com.example.android.weardatacollector;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class TimerThread extends Thread {
    Integer TIME_LIMIT; // in seconds
    TimerListener timerListener;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public void setTimerListener(TimerListener timerListener) {
        this.timerListener = timerListener;
    }

    public void setParams(Integer TIME_LIMIT) {
        this.TIME_LIMIT = TIME_LIMIT;
    }

    public interface TimerListener {
        public void timerListener(int command);
    }

    @Override
    public void run() {
        Log.d("Timer:", "Thread started for " + TIME_LIMIT);
        try {
            Thread.sleep(TIME_LIMIT * 1000);
        } catch (Exception e) {
            return;
        }
        this.handler.post(new Runnable() {
            @Override
            public void run() {
                timerListener.timerListener(1);
            }
        });

        // call to UI thread
    }
}
