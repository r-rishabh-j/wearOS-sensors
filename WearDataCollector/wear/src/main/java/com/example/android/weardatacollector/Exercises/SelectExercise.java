package com.example.android.weardatacollector.Exercises;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.Nullable;

import com.example.android.weardatacollector.R;
import com.example.android.weardatacollector.databinding.SelectExerciseBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;


public class SelectExercise extends Activity {
    private SelectExerciseBinding binding;

    // arr[0] is activity time in seconds
    // arr[1] is rest time in seconds
    // arr[2] is number of cycles
    HashMap<String, int[]> activityMap;
    private ListView lv;

    private void setActivityMap() {
        activityMap = new HashMap<>();
        /*
         * arr[0]-> exercise interval
         * arr[1]-> rest interval
         * arr[2]-> number of cycles
         * */
        activityMap.put("RUNNING", new int[]{60, 30, 5});
        activityMap.put("WALKING", new int[]{120, 30, 5});
        activityMap.put("JUMPING", new int[]{60, 30, 5});
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = SelectExerciseBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        lv = findViewById(R.id.activityListView);
        setActivityMap();
        List<String> activityViewList = new ArrayList<>(activityMap.keySet());
        Collections.sort(activityViewList);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, R.layout.text_row_item, activityViewList);

        lv.setAdapter(arrayAdapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String selectedActivity = activityViewList.get(i);
                try {
                    int EXERCISE_INTERVAL = activityMap.get(selectedActivity)[0];
                    int REST_INTERVAL = activityMap.get(selectedActivity)[1];
                    int CYCLES = activityMap.get(selectedActivity)[2];
                    Intent intent = new Intent();
                    intent.putExtra("ACTIVITY", selectedActivity);
                    intent.putExtra("EXERCISE_INTERVAL", EXERCISE_INTERVAL);
                    intent.putExtra("REST_INTERVAL", REST_INTERVAL);
                    intent.putExtra("CYCLES", CYCLES);
                    setResult(Activity.RESULT_OK, intent);
                    finish();
                } catch (Exception e) {
                    Log.d("SelectExercise:", "" + e);
                }

            }
        });
    }
}
