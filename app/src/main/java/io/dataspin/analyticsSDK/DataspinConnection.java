package io.dataspin.analyticsSDK;

import android.util.Base64;
import android.util.Log;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HTTP;
/**
 * Created by rafal@dataspin.io on 10.03.15.
 */
public class DataspinConnection {
    public HttpPost post;
    public HttpGet get;

    public DataspinMethod dataspinMethod;
    public HttpMethod httpMethod;

    public String json;
    public String response;
    public String error;

    public DataspinConnection(String url, DataspinMethod dataspinMethod, HttpMethod httpMethod, String json) {
        this.dataspinMethod = dataspinMethod;
        this.httpMethod = httpMethod;
        this.json = json;

        if(httpMethod == HttpMethod.POST || json.length() < 2) {
            this.post = new HttpPost(url);
            try {
                StringEntity entity = new StringEntity(json, HTTP.UTF_8);
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
            this.get = new HttpGet(url);
            this.post.addHeader("Authorization", "Token " + DataspinManager.Instance().APIKey);
        }
    }
}
