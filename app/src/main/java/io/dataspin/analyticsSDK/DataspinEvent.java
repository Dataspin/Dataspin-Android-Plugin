package io.dataspin.analyticsSDK;

import java.util.Dictionary;

/**
 * Created by rafal on 08.03.15.
 */
public class DataspinEvent {
    public String name;
    public String id;

    public DataspinEvent(Dictionary<String, Object> eventDict) {
        try {
            this.id = (String) eventDict.get("slug");
            this.name = (String) eventDict.get("name");
        }
        catch(Exception e) {
            //DataspinManager.Instance.AddError(DataspinError.ErrorTypeEnum.JSON_PROCESSING_ERROR, "Failed to create new DataspinCustomEvent. ", e.StackTrace);
        }
    }

    public String ToString() {
        return "ID: "+id+", Name: "+name;
    }
}
