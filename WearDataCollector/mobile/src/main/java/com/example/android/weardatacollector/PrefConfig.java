package com.example.android.weardatacollector;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class PrefConfig {
    private static final String LIST_KEY="list_key";
    public static void writeListInPref(Context context, ArrayList<Exercise> list)
    {
        Gson gson=new Gson();
        String jsonString= gson.toJson(list);
        SharedPreferences pref= PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor= pref.edit();
        editor.putString(LIST_KEY,jsonString);
        editor.apply();
    }
    public static ArrayList<Exercise> readListFromPref(Context context)
    {
        SharedPreferences pref=PreferenceManager.getDefaultSharedPreferences(context);
        String json = pref.getString(LIST_KEY, null);
        Gson gson = new Gson();
        Type type= new TypeToken<ArrayList<Exercise>>() {}.getType();
        ArrayList<Exercise> list=gson.fromJson(json,type);
        return list;
    }
    public static void removeData(Context context)
    {
        SharedPreferences pref=PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor= pref.edit();
        editor.remove(LIST_KEY);
        editor.apply();
    }
}
