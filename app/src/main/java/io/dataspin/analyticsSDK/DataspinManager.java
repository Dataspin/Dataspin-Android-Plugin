package io.dataspin.analyticsSDK;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * Created by rafal@datapsin.io
 */

public class DataspinManager {

    public static String logTag = "DataspinManager";

    // Singleton
    private static DataspinManager _instance;

    public DataspinManager() {
        this._instance = this;
    }

    public static DataspinManager Instance() {
        if(_instance == null) {
            _instance = new DataspinManager();
        }
        return _instance;
    }


    // Setup functions and variables
    public String APIKey = "";

    public void SetApiKey(String key, Context context) {
        APIKey = key;
        isApiKeyProvided = true;
    }

    //Session specific variables
    private boolean isApiKeyProvided;
    private boolean isUserRegistered;
    private String user_uuid;
    private String device_uuid;
    private String session_id;
    private Context context;

    public List<DataspinError> Errors;
    public List<DataspinWebRequest> OnGoingTasks;

    // Requests
    public void RegisterUser(String name, String surname, String email, String google_plus_id, String facebook_id) {
        if(isApiKeyProvided) {
            if(GetUserUUID() == null) {
                Log.i(logTag,"New user detected, registering!");

                JSONObject paramsJson = new JSONObject();
                try {
                    paramsJson.put("name", name);
                    paramsJson.put("surname", surname);
                    paramsJson.put("email", email);
                    paramsJson.put("google_plus", google_plus_id);
                    paramsJson.put("facebook", facebook_id);



                }
                catch(Exception e) {
                    //Log.e(logTag, "Cannot create/execute RegisterUser request! Details: "e.getMessage());
                }
            }
            else {
                Log.i(logTag,"User already registered!");
            }
        }
        else {
            //Api Key not provided
        }
    }

    public void RegisterDevice() {
        if(isUserRegistered) {

        }
    }


    // Helper functions
    private String GetUserUUID() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String name = preferences.getString("Name","");
        if(!name.equalsIgnoreCase("")) return name;
        else return null;
    }

    private String GetDeviceID() {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return md5(tm.getDeviceId());
    }

    public static final String md5(final String s) {
        final String MD5 = "MD5";
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest
                    .getInstance(MD5);
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                String h = Integer.toHexString(0xFF & aMessageDigest);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }
}