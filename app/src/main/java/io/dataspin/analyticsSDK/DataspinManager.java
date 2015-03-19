package io.dataspin.analyticsSDK;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by rafal@datapsin.io
 */

public class DataspinManager {

    public final String PluginVersion = "0.01";
    public final String ApiVersion = "v1";
    public final String BaseURL = "http://54.247.78.173:8888/api/%s/%s/";

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
    public String ClientName = "";
    public String AppVersion = "";

    public void SetApiKey(String clientName, String APIKey, String appVersion, final Context context) {
        this.ClientName = clientName;
        this.APIKey = APIKey;
        this.AppVersion = appVersion;
        this.context = context;
        this.isApiKeyProvided = true;

        new Thread(new Runnable() {
            public void run() {
                try {
                    AdvertisingIdClient.AdInfo adInfo = AdvertisingIdClient.getAdvertisingIdInfo(context);
                    ad_id = adInfo.getId();

                } catch (Exception e) {
                    //UnityPlayer.UnitySendMessage("DataspinManager", "OnAdIdReceived", null);
                    e.printStackTrace();
                }
            }
        }).start();
    }


    //Session specific variables
    private boolean isApiKeyProvided;
    private boolean isUserRegistered;
    private boolean isDeviceRegistered;
    private boolean isSessionStarted;
    private String user_uuid;
    private String device_uuid;
    private String session_id;
    private String ad_id;
    private Context context;

    public ArrayList<DataspinError> Errors;
    public ArrayList<DataspinWebRequest> OnGoingTasks;

    public ArrayList<DataspinItem> AvailableItems;
    public ArrayList<DataspinEvent> AvailableEvents;

    //Temporary Variables
    private String stringParams;

    //Event Listener
    private IDataspinListener listener;
    public void setOnEventListener(IDataspinListener listener) {
        this.listener = listener;
    }


    // TODO: Put requests onto goingtasks list
    // Requests
    public void RegisterUser(String name, String surname, String email, String google_plus_id, String facebook_id) {
        if(isApiKeyProvided) {
            if(GetUserUUID() == null) {
                Log.i(logTag,"New user detected, registering!");

                JSONObject paramsJson = new JSONObject();
                try {
                    paramsJson.put("name", name);
                    paramsJson.put("surname", surname);
                    paramsJson.put("email", email == null ? GetMail() : email);
                    paramsJson.put("google_plus", google_plus_id);
                    paramsJson.put("facebook", facebook_id);

                    stringParams = paramsJson.toString();
                    stringParams = stringParams.replaceAll("\\\\","");
                }
                catch(Exception e) {
                    AddError(new DataspinError(ErrorType.REQUEST_CREATION_ERROR, "Couldn't create paramsJson while creating RegisterUser query.", e));
                }

                DataspinConnection dc = new DataspinConnection(DataspinMethod.REGISTER_USER, HttpMethod.POST, paramsJson);
                new DataspinWebRequest().execute(dc);
            }
            else {
                Log.i(logTag,"User already registered!");
            }
        }
        else {
            AddError(new DataspinError(ErrorType.API_KEY_NOT_PROVIDED, "Couldn't register user because Api Key wsa not provided! Call SetApiKey first!"));
        }
    }

    public void RegisterDevice(String notification_id) {
        if(isUserRegistered) {
            JSONObject paramsJson = new JSONObject();
            try {
                paramsJson.put("end_user", this.user_uuid);
                paramsJson.put("platform", 1);
                paramsJson.put("device", GetDevice());
                paramsJson.put("uuid", GetDeviceID());
                paramsJson.put("ads_id", GetAdvertisingID());
                if(notification_id != null) paramsJson.put("notification_id", notification_id);
            }
            catch(Exception e) {
                AddError(new DataspinError(ErrorType.REQUEST_CREATION_ERROR, "Couldn't create paramsJson while creating RegisterDevice query.", e));
            }

            DataspinConnection dc = new DataspinConnection(DataspinMethod.REGISTER_DEVICE, HttpMethod.POST, paramsJson);
            new DataspinWebRequest().execute(dc);
        }
        else {
            AddError(new DataspinError(ErrorType.USER_NOT_REGISTERED, "Couldn't register device because user wasn't registered! Call RegisterUser first!"));
        }
    }

    public void StartSession() {
        if(isDeviceRegistered) {
            if(isSessionStarted) {
                JSONObject paramsJson = new JSONObject();
                try {
                    paramsJson.put("end_user_device", this.device_uuid);
                    paramsJson.put("app_version", this.AppVersion);
                    paramsJson.put("connectivity_type", GetConnectivityType());
                    paramsJson.put("carrier_name", GetCarrier());
                } catch (Exception e) {
                    AddError(new DataspinError(ErrorType.REQUEST_CREATION_ERROR, "Couldn't create paramsJson while creating RegisterDevice query.", e));
                }

                DataspinConnection dc = new DataspinConnection(DataspinMethod.START_SESSION, HttpMethod.POST, paramsJson);
                new DataspinWebRequest().execute(dc);
            }
            else {
                Log.w(logTag, "Session already started! No need to start a new one.");
            }
        }
        else {
            AddError(new DataspinError(ErrorType.DEVICE_NOT_REGISTERED, "Couldn't start session because device wasn't registered! Call RegisterDevice first!"));
        }
    }

    public void EndSession(String carrier_name) {
        if(isSessionStarted) {
            JSONObject paramsJson = new JSONObject();
            try {
                paramsJson.put("end_user_device", this.device_uuid);
                paramsJson.put("app_version", this.AppVersion);
                paramsJson.put("connectivity_type", GetConnectivityType());
                paramsJson.put("carrier_name", GetCarrier());
            }
            catch (Exception e) {
                AddError(new DataspinError(ErrorType.REQUEST_CREATION_ERROR, "Couldn't create paramsJson while creating EndSession query.", e));
            }

            DataspinConnection dc = new DataspinConnection(DataspinMethod.END_SESSION, HttpMethod.POST, paramsJson);
            new DataspinWebRequest().execute(dc);
        }
        else {
            Log.w(logTag,"Cannot End Session, there is no session active!");
        }
    }

    public void RegisterCustomEvent(String custom_event, String extraData, int forced_sess_id) {
        if(isSessionStarted) {
            JSONObject paramsJson = new JSONObject();
            try {
                paramsJson.put("custom_event", custom_event);
                paramsJson.put("end_user_device", device_uuid);
                paramsJson.put("app_version", this.AppVersion);
                if (extraData != null) paramsJson.put("data", extraData);

                if (forced_sess_id == -1) paramsJson.put("session", this.session_id);
                else paramsJson.put("session", forced_sess_id);
            }
            catch (Exception e) {
                AddError(new DataspinError(ErrorType.REQUEST_CREATION_ERROR, "Couldn't create paramsJson while creating RegisterCustomEvent query.", e));
            }

            DataspinConnection dc = new DataspinConnection(DataspinMethod.REGISTER_EVENT, HttpMethod.POST, paramsJson);
            new DataspinWebRequest().execute(dc);
        }
        else {
            AddError(new DataspinError(ErrorType.SESSION_NOT_STARTED, "Couldn't Register event because session wasn't started! Call StartSession first!"));
        }
    }

    public void PurchaseItem(String internal_id, int amount, int forced_sess_id) {
        if(isSessionStarted) {
            JSONObject paramsJson = new JSONObject();
            try {
                paramsJson.put("item", internal_id); //FindItemByName(item_name).InternalId
                paramsJson.put("end_user_device", this.device_uuid);
                paramsJson.put("app_version", this.AppVersion);
                paramsJson.put("amount", amount);

                if (forced_sess_id == -1) paramsJson.put("session", this.session_id);
                else paramsJson.put("session", forced_sess_id);
            }

            catch (Exception e) {
                AddError(new DataspinError(ErrorType.REQUEST_CREATION_ERROR, "Couldn't create paramsJson while creating PurchaseItem query.", e));
            }

            DataspinConnection dc = new DataspinConnection(DataspinMethod.PURCHASE_ITEM, HttpMethod.POST, paramsJson);
            new DataspinWebRequest().execute(dc);
        }
        else {
            AddError(new DataspinError(ErrorType.SESSION_NOT_STARTED, "Couldn't Purchase item because session wasn't started! Call StartSession first!"));
        }
    }

    public void GetItems() {
        if(isSessionStarted) {
            DataspinConnection dc = new DataspinConnection(DataspinMethod.GET_ITEMS, HttpMethod.GET, null);
            new DataspinWebRequest().execute(dc);
        }
        else {
            AddError(new DataspinError(ErrorType.SESSION_NOT_STARTED, "Couldn't get events because session wasn't started! Call StartSession first!"));
        }
    }


    // Listeners
    public void OnRequestExecuted(DataspinConnection connection) {
        Log.i(logTag, "Processing request: "+connection.toString());

        try {
            JSONObject responseJson = new JSONObject(connection.response);
            switch(connection.dataspinMethod) {
                case REGISTER_USER:
                    this.user_uuid = (String) responseJson.get("uuid");
                    SetUserUUID(this.user_uuid);
                    isUserRegistered = true;
                    if (listener != null)
                        listener.OnUserRegistered(this.user_uuid);

                    Log.d(logTag, "User Registered! UUID: "+this.user_uuid);
                    break;

                case REGISTER_DEVICE:
                    this.device_uuid = GetDeviceID();
                    SetDeviceUUID(this.device_uuid);
                    isDeviceRegistered = true;
                    if(listener != null)
                        listener.OnDeviceRegistered();

                    Log.d(logTag, "Device Registered! UUID: "+this.device_uuid);
                    break;

                case START_SESSION:
                    this.session_id = String.valueOf((int) responseJson.get("id"));
                    isSessionStarted = true;
                    if(listener != null)
                        listener.OnSessionStarted();

                    Log.d(logTag, "Session started! ID: "+this.session_id);
                    break;

                case END_SESSION:
                    isSessionStarted = false;
                    if(listener != null)
                        listener.OnSessionEnded();
                    break;

                case PURCHASE_ITEM:
                    DataspinItem item = FindItemById((String) responseJson.get("item"));
                    if(listener != null) {
                        if(item != null) listener.OnItemPurchased(item);
                    }

                    Log.d(logTag, "Item Purchased! "+item.toString());
                    break;

                case GET_ITEMS:
                    JSONArray itemsJson = (JSONArray) responseJson.getJSONArray("results");
                    AvailableItems = new ArrayList<DataspinItem>();
                    for(int i = 0; i < itemsJson.length(); i++) {
                       AvailableItems.add(new DataspinItem(itemsJson.getJSONObject(i)));
                    }
                    if(listener != null) {
                        listener.OnItemsListReceived(AvailableItems);
                    }
                    break;

                case GET_EVENTS:
                    JSONArray eventsJson = (JSONArray) responseJson.getJSONArray("results");
                    AvailableEvents = new ArrayList<DataspinEvent>();
                    for(int i = 0; i < eventsJson.length(); i++) {
                        AvailableEvents.add(new DataspinEvent(eventsJson.getJSONObject(i)));
                    }
                    if(listener != null) {
                        listener.OnEventsListReceived(AvailableEvents);
                    }
                    break;


                case REGISTER_OLD_SESSION:
                    break;
            }
        }
        catch(Exception e) {
            AddError(new DataspinError(ErrorType.JSON_PROCESSING_ERROR, "Couldn't parse json probably. Details: "+e.getMessage(), e));
        }
    }


    // Helper functions
    public void AddError(DataspinError error) {
        if(Errors == null) Errors = new ArrayList<DataspinError>();
        Errors.add(error);
        Log.e(logTag, error.toString());
    }

    public DataspinItem FindItemByName(String name) {
        if(AvailableItems != null)
            for(DataspinItem item : AvailableItems) {
                if(item.long_name == name) return item;
            }
        return null;
    }

    public DataspinItem FindItemById(String id) {
        if(AvailableItems != null)
            for(DataspinItem item : AvailableItems) {
                if(item.internal_id == id) return item;
            }
        return null;
    }

    public String GetAdvertisingID() {
        return null;
    }

    private String GetCarrier() {
        try {
            TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            String carrierName = manager.getNetworkOperatorName();
            return carrierName;
        }
        catch(Exception e) {
            Log.w(logTag, "Unable to get carrier!");
            return "";
        }
    }

    private String GetMail() {
        try {
            AccountManager manager = AccountManager.get(context);
            Account[] accounts = manager.getAccountsByType("com.google");
            List<String> possibleEmails = new LinkedList<String>();

            for (Account account : accounts) {
                Log.i(logTag, "Email Acc: " + account.toString());
                possibleEmails.add(account.name);
            }

            if (!possibleEmails.isEmpty() && possibleEmails.get(0) != null) {
                return possibleEmails.get(0);
            }
        }
        catch(Exception ex) {
            Log.w(logTag, "Failed to get user accounts! Probably GET_ACCOUNTS permission is missing. Details: "+ex.getMessage());
            ex.printStackTrace();
        }

        return null;
    }

    private String GetUserUUID() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String uuid = preferences.getString("USER_UUID","");
        if(!uuid.equalsIgnoreCase("")) return uuid;
        else return null;
    }

    private void SetUserUUID(String uuid) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("USER_UUID", uuid);
        editor.commit();
    }

    private String GetDeviceUUID() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String uuid = preferences.getString("DEVICE_UUID","");
        if(!uuid.equalsIgnoreCase("")) return uuid;
        else return null;
    }

    private void SetDeviceUUID(String uuid) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("DEVICE_UUID", uuid);
        editor.commit();
    }

    private String GetDeviceID() {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return md5(tm.getDeviceId());
    }

    public JSONObject GetDevice() {
        JSONObject json = new JSONObject();
        try {
            json.put("manufacturer", Build.MANUFACTURER);
            json.put("model", Build.MODEL);
            json.put("screen_width", 45);
            json.put("screen_height", Build.BRAND);
            json.put("dpi", 45);
        }
        catch (Exception e) {
            AddError(new DataspinError(ErrorType.JSON_PROCESSING_ERROR, "Couldn't create Device json. Details: "+e.getMessage(), e));
        }
        return json;
    }

    private static String md5(final String s) {
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

    public int GetConnectivityType() {
        ConnectivityManager mConnectivity = null;
        TelephonyManager mTelephony = null;
        NetworkInfo info = mConnectivity.getActiveNetworkInfo();
        if (info == null || !mConnectivity.getBackgroundDataSetting()) {
            return 0;
        }

        int netType = info.getType();
        int netSubtype = info.getSubtype();
        if (netType == ConnectivityManager.TYPE_WIFI) {
            return 1;
        } else if (netType == ConnectivityManager.TYPE_MOBILE
                && netSubtype == TelephonyManager.NETWORK_TYPE_UMTS
                && !mTelephony.isNetworkRoaming()) {
            return 2;
        } else {
            return 2;
        }
    }

    public String GetCorrespondingURL(DataspinMethod method) {
        switch(method) {
            case REGISTER_USER:
                return String.format(BaseURL, ApiVersion, "register_user");
            case REGISTER_DEVICE:
                return String.format(BaseURL, ApiVersion, "register_user_device");
            case START_SESSION:
                return String.format(BaseURL, ApiVersion, "start_session");
            case REGISTER_EVENT:
                return String.format(BaseURL, ApiVersion, "register_event");
            case PURCHASE_ITEM:
                return String.format(BaseURL, ApiVersion, "purchase");
            case GET_ITEMS:
                return String.format(BaseURL, ApiVersion, "get_items");
            case GET_EVENTS:
                return String.format(BaseURL, ApiVersion, "custom_events");
            case END_SESSION:
                return String.format(BaseURL, ApiVersion, "end_session");
            case REGISTER_OLD_SESSION:
                return String.format(BaseURL, ApiVersion, "register_old_session");

            default:
                return null;
        }
    }
}