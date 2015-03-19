package io.dataspin.analyticsSDK;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Created by rafal on 08.03.15.
 */
public class DataspinError {

    public ErrorType ErrorType;
    public int ErrorCode;
    public String Message;
    public Throwable Exception;
    public DataspinConnection Connection;

    public DataspinError(ErrorType errorType, String message, int errorCode, Throwable exception) {
        this.ErrorType = errorType;
        this.ErrorCode = errorCode;
        this.Message = message;
        this.Exception = exception;
    }

    public DataspinError(ErrorType errorType, String message, int errorCode, DataspinConnection connection) {
        this.Connection = connection;
        this.ErrorType = errorType;
        this.ErrorCode = errorCode;
        this.Message = message;
    }

    public DataspinError(ErrorType errorType, String message, int errorCode) {
        this.ErrorType = errorType;
        this.ErrorCode = errorCode;
        this.Message = message;
    }

    public DataspinError(ErrorType errorType, String message, Throwable exception) {
        this.ErrorType = errorType;
        this.Message = message;
        this.Exception = exception;
    }

    public DataspinError(ErrorType errorType, String message) {
        this.ErrorType = errorType;
        this.Message = message;
    }

    public String toString() {
        String s = "[Error] Type: " + this.ErrorType.toString() +
                ", Message: " + this.Message;
        s += (this.Connection == null) ? "" : "While executing connection "+this.Connection.dataspinMethod.toString() + ", Data: "+this.Connection.json;
        s += (this.ErrorCode == 0 ? "" : (", Error Code: "+this.ErrorCode));
        if(this.Exception != null) {
            StringWriter errors = new StringWriter();
            Exception.printStackTrace(new PrintWriter(errors));
            s += errors.toString();
        }
        return s;
    }
}
