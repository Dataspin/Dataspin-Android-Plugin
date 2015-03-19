package io.dataspin.analyticsSDK;

import android.util.Base64;
import android.util.Log;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;

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

    public DataspinConnection(DataspinMethod dataspinMethod, HttpMethod httpMethod, JSONObject json) {
        this.dataspinMethod = dataspinMethod;
        this.httpMethod = httpMethod;
        this.json = json;
        this.url = DataspinManager.Instance().GetCorrespondingURL(dataspinMethod);

        if(httpMethod == HttpMethod.POST || json.length() < 2) {
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
            // TODO: Parse JSON into ?param=value&param2=value
            this.get = new HttpGet(url);
            this.post.addHeader("Authorization", "Token " + DataspinManager.Instance().APIKey);
        }
    }
}
