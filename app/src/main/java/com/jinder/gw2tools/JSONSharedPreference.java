package com.jinder.gw2tools;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Jinder on 14/12/16.
 */
public class JSONSharedPreference {
    private final static  String PREFIX = "json";

    public static void saveJsonObject(Context c, String prefName, String key, JSONObject object){
        SharedPreferences settings = c.getSharedPreferences(prefName, 0);
        SharedPreferences.Editor editor = settings.edit();
        String aryString = object.toString();
        editor.putString(PREFIX+key, object.toString());
        editor.commit();
    }

    public static void saveJSONArray(Context c, String prefName, String key, JSONArray array) {
        SharedPreferences settings = c.getSharedPreferences(prefName, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PREFIX+key, array.toString());
        editor.commit();
    }

    public static JSONObject loadJSONObject(Context c, String prefName, String key) throws JSONException {
        SharedPreferences settings = c.getSharedPreferences(prefName, 0);
        return new JSONObject(settings.getString(PREFIX+key, "{}"));
    }

    public static JSONArray loadJSONArray(Context c, String prefName, String key) throws JSONException {
        SharedPreferences settings = c.getSharedPreferences(prefName, 0);
        return new JSONArray(settings.getString(PREFIX+key, "[]"));
    }

    public static void remove(Context c, String prefName, String key) {
        SharedPreferences settings = c.getSharedPreferences(prefName, 0);
        if (settings.contains(PREFIX+key)) {
            SharedPreferences.Editor editor = settings.edit();
            editor.remove(PREFIX+key);
            editor.commit();
        }
    }
}
