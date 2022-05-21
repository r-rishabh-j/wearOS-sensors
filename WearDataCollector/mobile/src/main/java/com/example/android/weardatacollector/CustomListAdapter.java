package com.example.android.weardatacollector;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public class CustomListAdapter extends ArrayAdapter<Exercise> {
    private static final String TAG = "CustomListAdapter";

    private Context mContext;
    private int mResource;
    private int lastPosition = -1;

    private static class ViewHolder {
        TextView Activity;
        TextView Timestamp;
        ImageView imageIcon;
    }

    public CustomListAdapter(Context context, int resource, ArrayList<Exercise> objects) {
        super(context, resource, objects);
        mContext = context;
        mResource = resource;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        String name = getItem(position).getName();
        String tmp = getItem(position).getTimestamp();

        //create the view result for showing the animation
        final View result;

        //ViewHolder object
        final ViewHolder holder;


        if (convertView == null) {

            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(mResource, parent, false);
            holder = new ViewHolder();
            holder.Activity = (TextView) convertView.findViewById(R.id.name1);

            holder.Timestamp = (TextView) convertView.findViewById(R.id.timestamp1);
            holder.imageIcon = (ImageView) convertView.findViewById(R.id.cardImage);
            result = convertView;

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
            result = convertView;
        }
        lastPosition = position;

        holder.Activity.setText(name);
        holder.Timestamp.setText(tmp);
        if (name.equals("RUNNING")) {
            int defaultImage = mContext.getResources().getIdentifier("@drawable/run", null, mContext.getPackageName());
            holder.imageIcon.setImageResource(defaultImage);
        } else if (name.equals("JUMPING")) {
            int cd = mContext.getResources().getIdentifier("@drawable/jump", null, mContext.getPackageName());
            holder.imageIcon.setImageResource(cd);
        } else if (name.equals("WALKING")) {
            int cd = mContext.getResources().getIdentifier("@drawable/pedestrian", null, mContext.getPackageName());
            holder.imageIcon.setImageResource(cd);
        } else {
            int cd = mContext.getResources().getIdentifier("@drawable/default_exercise", null, mContext.getPackageName());
            holder.imageIcon.setImageResource(cd);
        }

        return convertView;
    }
}
