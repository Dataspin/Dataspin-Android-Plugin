package io.dataspin.analyticsSDK;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Created by rafal@datapsin.io
 */

public class DataspinManager {

    public final String PluginVersion = "0.01";
    public final String ApiVersion = "v1";
    public final String BaseURL = "http://%s.dataspin.io/api/%s/%s/";

    public static String logTag = "DataspinManager";

    // Singleton
    private static DataspinManager _instance;
    public DataspinBacklog _backlog;

    public DataspinManager() {
        this._instance = this;
    }

    public DataspinManager(IDataspinListener ml) {
        this._instance = this;
        _instance.listener = ml;
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
    public boolean IsDebug;

    public void SetApiKey(String clientName, String APIKey, String appVersion, boolean isDebug, final Context context) {
        this.ClientName = clientName;
        this.APIKey = APIKey;
        this.AppVersion = appVersion;
        this.context = context;
        this.isApiKeyProvided = true;
        this.IsDebug = isDebug;

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
    public boolean isSessionStarted;
    private String user_uuid;
    private String device_uuid;
    public String session_id;
    private String ad_id;
    private Context context;

    public ArrayList<DataspinError> Errors;
    public LinkedList<DataspinConnection> BacklogTasks;
    public ArrayList<DataspinWebRequest> OnGoingTasks;

    public ArrayList<DataspinItem> AvailableItems;
    public ArrayList<DataspinEvent> AvailableEvents;

    //Temporary Variables
    private String stringParams;

    //Offline session
    public String offline_session_id;
    private int start_timestamp;

    //Event Listener
    private IDataspinListener listener;
    public void setOnEventListener(IDataspinListener listener) {
        this.listener = listener;
    }

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

                ExecuteConnection(new DataspinConnection(DataspinMethod.REGISTER_USER, HttpMethod.POST, paramsJson));
            }
            else {
                this.user_uuid = GetUserUUID();
                isUserRegistered = true;

                Log.i(logTag,"User already registered!");

                if (listener != null)
                    listener.OnUserRegistered(this.user_uuid);
                else Log.i(logTag, "Listener is not set!");
            }
        }
        else {
            AddError(new DataspinError(ErrorType.API_KEY_NOT_PROVIDED, "Couldn't register user because Api Key wsa not provided! Call SetApiKey first!"));
        }
    }

    public void RegisterDevice(String notification_id) {
        if(isUserRegistered) {
            if (GetDeviceUUID() == null) {
                JSONObject paramsJson = new JSONObject();
                try {
                    paramsJson.put("end_user", this.user_uuid);
                    paramsJson.put("platform", 1);
                    paramsJson.put("device", GetDevice());
                    paramsJson.put("uuid", GetDeviceID());
                    paramsJson.put("ads_id", GetAdvertisingID());
                    if (notification_id != null) paramsJson.put("notification_id", notification_id);
                } catch (Exception e) {
                    AddError(new DataspinError(ErrorType.REQUEST_CREATION_ERROR, "Couldn't create paramsJson while creating RegisterDevice query.", e));
                }

                ExecuteConnection(new DataspinConnection(DataspinMethod.REGISTER_DEVICE, HttpMethod.POST, paramsJson));
            }
            else {
                this.device_uuid = GetDeviceUUID();
                isDeviceRegistered = true;

                Log.i(logTag,"Device already registered!");

                if (listener != null)
                    listener.OnDeviceRegistered();
                else Log.i(logTag, "Listener is not set!");
            }
        }
        else {
            AddError(new DataspinError(ErrorType.USER_NOT_REGISTERED, "Couldn't register device because user wasn't registered! Call RegisterUser first!"));
        }
    }

    public void StartSession() {
        JSONObject paramsJson = new JSONObject();
        try {
            paramsJson.put("end_user_device", this.device_uuid);
            paramsJson.put("app_version", this.AppVersion);
            paramsJson.put("connectivity_type", GetConnectivityType());
            paramsJson.put("carrier_name", GetCarrier());
        } catch (Exception e) {
            AddError(new DataspinError(ErrorType.REQUEST_CREATION_ERROR, "Couldn't create paramsJson while creating RegisterDevice query.", e));
        }


        if(isDeviceRegistered) {
            if(!isSessionStarted) {
                ExecuteConnection(new DataspinConnection(DataspinMethod.START_SESSION, HttpMethod.POST, paramsJson));
            }
            else {
                Log.w(logTag, "Session already started! No need to start a new one.");
            }
        }
        else {
            StartOfflineSession();
            AddError(new DataspinError(ErrorType.DEVICE_NOT_REGISTERED, "Couldn't start session because device wasn't registered! Call RegisterDevice first!"));
        }
    }

    public void StartOfflineSession() {
        JSONObject paramsJson = new JSONObject();
        Random r = new Random();
        start_timestamp = (int) System.currentTimeMillis() / 1000;
        offline_session_id = String.valueOf(r.nextInt() * -1);
        Log.i(logTag, "Starting offline session with id: " + this.offline_session_id+", Start: "+String.valueOf(start_timestamp));

        try {
            paramsJson.put("end_user_device", this.device_uuid);
            paramsJson.put("app_version", this.AppVersion);
            paramsJson.put("connectivity_type", GetConnectivityType());
            paramsJson.put("carrier_name", GetCarrier());
            paramsJson.put("session_id", offline_session_id);
            paramsJson.put("start_timestamp", start_timestamp);
            paramsJson.put("end_timestamp", start_timestamp + 60);
        }
        catch (Exception e) {

        }
        if(_backlog == null) _backlog = new DataspinBacklog(context);
        _backlog.AddTask(new DataspinConnection(DataspinMethod.REGISTER_OLD_SESSION, HttpMethod.POST, paramsJson));

        isSessionStarted = true;

        Handler mainHandler = new Handler(context.getMainLooper());

        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                if(listener != null) listener.OnSessionStarted();
            }};

        mainHandler.post(myRunnable);
    }

    public void EndSession() {
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

            ExecuteConnection(new DataspinConnection(DataspinMethod.END_SESSION, HttpMethod.POST, paramsJson));
        }
        else {
            Log.w(logTag,"Cannot End Session, there is no session active!");
        }
    }

    public void RegisterCustomEvent(String custom_event, String extraData) {
        RegisterCustomEvent(custom_event, extraData, -1);
    }

    public void RegisterCustomEvent(String custom_event, String extraData, int forced_sess_id) {
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

        DataspinConnection conn = new DataspinConnection(DataspinMethod.REGISTER_EVENT, HttpMethod.POST, paramsJson);

        if(isSessionStarted) {
            ExecuteConnection(conn);
        }
        else {
            AddError(new DataspinError(ErrorType.SESSION_NOT_STARTED, "Couldn't Register event because session wasn't started! Call StartSession first!"));
        }
    }

    public void PurchaseItem(String internal_id, int amount) {
        PurchaseItem(internal_id, amount, -1);
    }

    public void PurchaseItem(String internal_id, int amount, int forced_sess_id) {
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

        DataspinConnection conn = new DataspinConnection(DataspinMethod.PURCHASE_ITEM, HttpMethod.POST, paramsJson);

        if(isSessionStarted) {
            ExecuteConnection(conn);
        }
        else {
            AddError(new DataspinError(ErrorType.SESSION_NOT_STARTED, "Couldn't Purchase item because session wasn't started! Call StartSession first!"));
        }
    }

    public void GetItems() {

        if(isSessionStarted) {
            JSONObject paramsJson = new JSONObject();
            try {
                paramsJson.put("app_version", this.AppVersion);
            }
            catch(Exception e) {
            }

            ExecuteConnection(new DataspinConnection(DataspinMethod.GET_ITEMS, HttpMethod.GET, paramsJson));
        }
        else {
            AddError(new DataspinError(ErrorType.SESSION_NOT_STARTED, "Couldn't get items because session wasn't started! Call StartSession first!"));
        }
    }

    public void GetEvents() {

        if(isSessionStarted) {
            JSONObject paramsJson = new JSONObject();
            try {
                paramsJson.put("app_version", this.AppVersion);
            }
            catch(Exception e) {
            }

            ExecuteConnection(new DataspinConnection(DataspinMethod.GET_EVENTS, HttpMethod.GET, paramsJson));
        }
        else {
            AddError(new DataspinError(ErrorType.SESSION_NOT_STARTED, "Couldn't get events because session wasn't started! Call StartSession first!"));
        }
    }

    public void GetAllTasks() {
        if(_backlog == null) _backlog = new DataspinBacklog(context);
        BacklogTasks = _backlog.GetAllTasks();
        Log.i(logTag, "BacklogTasks length: "+BacklogTasks.size());
        for(int i = 0; i < BacklogTasks.size(); i++) {
            Log.i(logTag, "Backlog task: "+BacklogTasks.get(i));
        }
    }


    // Listeners
    public void OnRequestExecuted(final DataspinConnection connection) {
        Handler mainHandler = new Handler(context.getMainLooper());

        Runnable myRunnable = new Runnable() {
        @Override
        public void run() {
            ExecuteConnectionOnUIThread(connection);
        }};

        mainHandler.post(myRunnable);
        Log.i(logTag, "Processing request: "+connection.toString());
    }

    public void ExecuteConnectionOnUIThread(DataspinConnection connection) {
        if(connection.backlogTaskId != 0) {
            Log.i(logTag, "Detected task from backlog! Executing next...");
            ExecuteNextTaskFromBacklog();
        }
        try {
            JSONObject responseJson = new JSONObject(connection.response);
            switch(connection.dataspinMethod) {
                case REGISTER_USER:
                    this.user_uuid = (String) responseJson.get("uuid");
                    SetUserUUID(this.user_uuid);
                    isUserRegistered = true;
                    if (listener != null)
                        listener.OnUserRegistered(this.user_uuid);
                    else Log.i(logTag, "Listener is not set!");

                    Log.d(logTag, "User Registered! UUID: "+this.user_uuid);
                    break;

                case REGISTER_DEVICE:
                    this.device_uuid = (String) responseJson.get("uuid");
                    SetDeviceUUID(this.device_uuid);
                    isDeviceRegistered = true;
                    if(listener != null)
                        listener.OnDeviceRegistered();

                    Log.d(logTag, "Device Registered! UUID: "+this.device_uuid);
                    break;

                case START_SESSION:
                    this.session_id = String.valueOf((int) responseJson.get("id"));
                    isSessionStarted = true;
                    if (listener != null)
                        listener.OnSessionStarted();

                    Log.d(logTag, "Session started! ID: " + this.session_id);
                    if(_backlog == null)_backlog = new DataspinBacklog(context);
                    BacklogTasks = _backlog.GetAllTasks();
                    ExecuteNextTaskFromBacklog();
                    break;

                case REGISTER_OLD_SESSION:
                    _backlog.UpdateTasksWithSessionId(connection.json.getString("session_id"), (int) responseJson.get("id"));
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
                    Log.d(logTag, "Items list processed! Length: "+itemsJson.length());
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
                    Log.d(logTag, "Events list processed! Length: "+eventsJson.length());
                    break;
            }
        }
        catch(Exception e) {
            AddError(new DataspinError(ErrorType.JSON_PROCESSING_ERROR, "Couldn't parse json probably. Details: "+e.getMessage(), e));
        }
    }


    // Helper functions
    public void LogInfo(String msg) {
        if(this.IsDebug == true) {
            Log.i(logTag, msg);
        }
    }
    public void AddError(final DataspinError error) {
        Log.e(logTag, error.toString());

        if(Errors == null) Errors = new ArrayList<DataspinError>();
        Errors.add(error);

        Handler mainHandler = new Handler(context.getMainLooper());
        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                if(listener != null) listener.OnError(error);
                else Log.i(logTag, "Listener is null!");
         }};

        mainHandler.post(myRunnable);
    }

    private void ExecuteNextTaskFromBacklog() {
        boolean isSessionFound;
        Log.i(logTag,"Executing next task from queue...");
        for(DataspinConnection conn : BacklogTasks) {
            if(conn.dataspinMethod == DataspinMethod.REGISTER_OLD_SESSION) {
                try {
                    conn.json.put("dt", (int) System.currentTimeMillis() / 1000 - conn.json.getInt("start_timestamp"));
                    conn.json.put("length", conn.json.getInt("end_timestamp") - conn.json.getInt("start_timestamp"));
                }
                catch (Exception e) {
                    Log.e(logTag, "Unable to calculate OfflineSession dt and length");
                }
                ExecuteConnection(conn);
                _backlog.DeleteTask(conn.backlogTaskId);
                BacklogTasks.remove(conn);
                return;
            }
        }

        for(DataspinConnection conn : BacklogTasks) {
            ExecuteConnection(conn);
            _backlog.DeleteTask(conn.backlogTaskId);
            BacklogTasks.remove(conn);
            return;
        }
    }

    private void ExecuteConnection(DataspinConnection connection) {
        Runnable r = new DataspinAsyncThread(connection);
        Thread th = new Thread(r);
        th.setPriority(Thread.MAX_PRIORITY);
        th.run();
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
        if(!uuid.equalsIgnoreCase("")) {
            this.user_uuid = uuid;
            return uuid;
        }
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
        if(!uuid.equalsIgnoreCase("")) {
            this.device_uuid = uuid;
            return uuid;
        }
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
        if(tm != null) return md5(tm.getDeviceId());

        String android_id = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        if(android_id != null && !android_id.equals("9774d56d682e549c")) return md5(android_id);

        try {
            return md5(android.os.Build.class.getField("SERIAL").get(null).toString());
        }
        catch (Exception e) {
            //Just fuck this device, this guy is useless
            return null;
        }
    }

    private JSONObject GetDevice() {
        JSONObject json = new JSONObject();
        try {
            DisplayMetrics metrics = new DisplayMetrics();
            WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            windowManager.getDefaultDisplay().getMetrics(metrics);

            json.put("manufacturer", Build.MANUFACTURER);
            json.put("model", Build.MODEL);
            json.put("screen_width", metrics.widthPixels);
            json.put("screen_height", metrics.heightPixels);
            json.put("dpi", metrics.densityDpi);
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

    private int GetConnectivityType() {
        ConnectivityManager mConnectivity = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = mConnectivity.getActiveNetworkInfo();
        if (info == null || !mConnectivity.getBackgroundDataSetting()) {
            return 0;
        }

        int netType = info.getType();
        int netSubtype = info.getSubtype();
        if (netType == ConnectivityManager.TYPE_WIFI) {
            return 1;
        } else if (netType == ConnectivityManager.TYPE_MOBILE
                && netSubtype == TelephonyManager.NETWORK_TYPE_UMTS) {
            return 2;
        } else {
            return 2;
        }
    }

    public static boolean IsBacklogMethod(DataspinMethod method) {
        if(method == DataspinMethod.PURCHASE_ITEM || method == DataspinMethod.REGISTER_EVENT) return true;
        return false;
    }

    public String GetCorrespondingURL(DataspinMethod method) {
        switch(method) {
            case REGISTER_USER:
                return String.format(BaseURL, ClientName, ApiVersion, "register_user");
            case REGISTER_DEVICE:
                return String.format(BaseURL, ClientName, ApiVersion, "register_user_device");
            case START_SESSION:
                return String.format(BaseURL, ClientName, ApiVersion, "start_session");
            case REGISTER_EVENT:
                return String.format(BaseURL, ClientName, ApiVersion, "register_event");
            case PURCHASE_ITEM:
                return String.format(BaseURL, ClientName, ApiVersion, "purchase");
            case GET_ITEMS:
                return String.format(BaseURL, ClientName, ApiVersion, "items");
            case GET_EVENTS:
                return String.format(BaseURL, ClientName, ApiVersion, "custom_events");
            case END_SESSION:
                return String.format(BaseURL, ClientName, ApiVersion, "end_session");
            case REGISTER_OLD_SESSION:
                return String.format(BaseURL, ClientName, ApiVersion, "register_old_session");

            default:
                return null;
        }
    }
}