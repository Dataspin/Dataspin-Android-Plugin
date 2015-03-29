package io.dataspin.analyticsSDK;

/**
 * Created by rafal on 14.03.15.
 */
public enum HttpMethod {
    POST (0),
    GET (1);

    public final int methodCode;

    private HttpMethod(int methodCode) {
        this.methodCode = methodCode;
    }
}
