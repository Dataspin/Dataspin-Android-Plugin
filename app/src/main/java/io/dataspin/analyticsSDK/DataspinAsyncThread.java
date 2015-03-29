package io.dataspin.analyticsSDK;

/**
 * Created by rafal on 20.03.15.
 */
public class DataspinAsyncThread implements Runnable {
    public DataspinConnection task;

    public DataspinAsyncThread(DataspinConnection parameter) {
        this.task = parameter;
    }

    public void run() {
        new DataspinWebRequest().execute(task);
    }
}

