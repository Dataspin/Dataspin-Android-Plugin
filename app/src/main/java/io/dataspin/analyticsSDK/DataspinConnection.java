package io.dataspin.analyticsSDK;

import android.util.Base64;
import android.util.Log;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * Created by rafal@dataspin.io on 10.03.15.
 */
public class DataspinConnection {
    public HttpPost post;
    public HttpGet get;

    public DataspinMethod dataspinMethod;
    public HttpMethod httpMethod;

    public String url;
    public JSONObject json;
    public String response;
    public int backlogTaskId;

    public DataspinConnection(DataspinMethod dataspinMethod, HttpMethod httpMethod, JSONObject json) {
        this.dataspinMethod = dataspinMethod;
        this.httpMethod = httpMethod;
        this.json = json;
        this.url = DataspinManager.Instance().GetCorrespondingURL(dataspinMethod);

        if(httpMethod == HttpMethod.POST) {
            this.post = new HttpPost(url);
            try {
                String stringParams = json.toString();
                stringParams = stringParams.replaceAll("\\\\","");

                StringEntity entity = new StringEntity(stringParams, HTTP.UTF_8);
                this.post.setEntity(entity);
                this.post.addHeader("Content-Type", "application/json");
                this.post.addHeader("Authorization", "Token " + DataspinManager.Instance().APIKey);
            }
            catch(Exception e) {
                Log.w("DataspinConnection", "Couldn't execute connection! Details: "+e.getMessage());
                e.printStackTrace();
            }
        }
        else {
            if(json != null) {
                int counter = 0;

                Iterator<String> iter = json.keys();
                while (iter.hasNext()) {
                    String key = iter.next();
                    try {
                        if (counter == 0) url += "?" + key + "=" + json.get(key);
                        else url += "&" + key + "=" + json.get(key);
                    } catch (JSONException e) {
                        // Something went wrong!
                        Log.e("DataspinConnection", "Failed to create URL!");
                    }
                }
            }

            this.get = new HttpGet(url);
            this.get.addHeader("Authorization", "Token " + DataspinManager.Instance().APIKey);
        }
    }

    public DataspinConnection(DataspinMethod dataspinMethod, HttpMethod httpMethod, JSONObject json, String url, int backlogTaskId) {
        this.dataspinMethod = dataspinMethod;
        this.httpMethod = httpMethod;
        this.json = json;
        this.url = url;
        this.backlogTaskId = backlogTaskId;

        if(httpMethod == HttpMethod.POST) {
            this.post = new HttpPost(url);
            try {
                String stringParams = json.toString();
                stringParams = stringParams.replaceAll("\\\\","");

                StringEntity entity = new StringEntity(stringParams, HTTP.UTF_8);
                this.post.setEntity(entity);
                this.post.addHeader("Content-Type", "application/json");
                this.post.addHeader("Authorization", "Token " + DataspinManager.Instance().APIKey);
            }
            catch(Exception e) {
                Log.w("DataspinConnection", "Couldn't create connection! Details: "+e.getMessage());
                e.printStackTrace();
            }
        }
        else {
            this.get = new HttpGet(url);
            this.get.addHeader("Authorization", "Token " + DataspinManager.Instance().APIKey);
        }
    }

    public void UpdatePost() {
        try {
            String stringParams = json.toString();
            stringParams = stringParams.replaceAll("\\\\", "");

            StringEntity entity = new StringEntity(stringParams, HTTP.UTF_8);
            this.post.setEntity(entity);
        }
        catch(Exception e) {

        }
    }

    public String toString() {
        String result = "[Connection] URL: "+this.url+", Method: "+dataspinMethod.toString()+", HTTP: "+httpMethod.toString();
        if(json != null) result += ", JSON: "+json.toString();
        result += ", Result: "+response;
        return result;
    }
}
