package io.dataspin.analyticsSDK;

import java.util.Random;

/**
 * Created by rafal on 23.03.15.
 */
public class DataspinBacklogTask {
    public int task_id;
    public String url;
    public String json_data;
    public int dataspin_method;
    public int http_method;

    public DataspinBacklogTask(int task_id, String url, String json_data, int dataspin_method, int http_method) {
        this.task_id = task_id;
        this.url = url;
        this.json_data = json_data;
        this.dataspin_method = dataspin_method;
        this.http_method = http_method;
    }

    public DataspinBacklogTask(String url, String json_data, int dataspin_method, int http_method) {
        this.url = url;
        this.json_data = json_data;
        this.dataspin_method = dataspin_method;
        this.http_method = http_method;
    }
}
