package io.dataspin.analyticsSDK;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

/**
 * Created by rafal on 08.03.15.
 */
public class DataspinWebRequest extends AsyncTask<DataspinConnection, Void, String> {
    @Override
    protected String doInBackground(DataspinConnection... params) {
        HttpClient httpClient = new DefaultHttpClient();
        try {
            if(params[0].get == null) { //POST Request
                HttpResponse response = httpClient.execute(params[0].post);
                params[0].response = EntityUtils.toString(response.getEntity());
                if (response.getStatusLine().getStatusCode() != 200) {
                    //TODO: print error and put on backlog
                    //params[0].error = Integer.toString(response.getStatusLine().getStatusCode()) + response.getStatusLine().getReasonPhrase();
                    Log.w("DataspinAsyncTask", "Couldn't execute request! Error: " + params[0]);
                }
            }
            else {
                HttpResponse response = httpClient.execute(params[0].get);
                params[0].response = EntityUtils.toString(response.getEntity());
                if (response.getStatusLine().getStatusCode() != 200) {
                    //params[0].error = Integer.toString(response.getStatusLine().getStatusCode()) + response.getStatusLine().getReasonPhrase();
                    Log.w("DataspinAsyncTask","Couldn't execute request! Error: "+params[0]);
                }
            }
            Log.w("DataspinAsyncTask", "Request success! "+params[0].response);
            DataspinManager.Instance().OnRequestExecuted(params[0]);
        }
        catch(Exception e) {
            //TODO: print error and put on backlog
            Log.w("DataspinAsyncTask", "Couldn't execute request! Error: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        //Do nothing here actually
    }
}
