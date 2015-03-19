package io.dataspin.analyticsSDK;

import org.json.JSONObject;

import java.util.Dictionary;

/**
 * Created by rafal on 08.03.15.
 */
public class DataspinEvent {
    public String name;
    public String id;

    public DataspinEvent(JSONObject eventDict) {
        try {
            this.id = (String) eventDict.get("slug");
            this.name = (String) eventDict.get("name");
        }
        catch(Exception e) {
            DataspinManager.Instance().AddError(new DataspinError(ErrorType.JSON_PROCESSING_ERROR, "Failed to create new DataspinCustomEvent. ", e));
        }
    }

    public String ToString() {
        return "ID: "+id+", Name: "+name;
    }
}
