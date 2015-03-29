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
        DataspinManager.Instance().LogInfo("Executing request: "+params[0].toString());
        try {
            if(params[0].httpMethod == HttpMethod.POST) { //POST Request
                HttpResponse response = httpClient.execute(params[0].post);
                params[0].response = EntityUtils.toString(response.getEntity());
                if (response.getStatusLine().getStatusCode() != 200) {
                    //TODO: print error and put on backlog
                    DataspinManager.Instance().AddError(new DataspinError(ErrorType.CONNECTION_ERROR, params[0].response, response.getStatusLine().getStatusCode()));
                    return null;
                }
            }
            else {
                HttpResponse response = httpClient.execute(params[0].get);
                params[0].response = EntityUtils.toString(response.getEntity());
                if (response.getStatusLine().getStatusCode() != 200) {
                    DataspinManager.Instance().AddError(new DataspinError(ErrorType.CONNECTION_ERROR, params[0].response, response.getStatusLine().getStatusCode()));
                    if(DataspinManager.IsBacklogMethod(params[0].dataspinMethod)) DataspinManager.Instance()._backlog.AddTask(params[0]);
                    if(params[0].dataspinMethod == DataspinMethod.START_SESSION) DataspinManager.Instance().StartOfflineSession();
                    return null;
                }
            }
            Log.w("DataspinAsyncTask", "Request completed: "+params[0].response);
            DataspinManager.Instance().OnRequestExecuted(params[0]);
        }
        catch(Exception e) {
            //TODO: print error and put on backlog
            DataspinManager.Instance().AddError(new DataspinError(ErrorType.CONNECTION_ERROR, "Couldn't execute HTTP Request", e));
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
