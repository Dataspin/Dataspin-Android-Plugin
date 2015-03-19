package io.dataspin.analyticsSDK;

import org.json.JSONObject;

import java.util.Date;
import java.util.Dictionary;
import java.util.Objects;

/**
 * Created by rafal on 08.03.15.
 */
public class DataspinItem {
    public String internal_id;
    public String long_name;
    public float price;
    public boolean is_coinpack;
    public String parameters;
    public JSONObject json;

    public DataspinItem(JSONObject dict) {
        this.json = dict;
        try {
            internal_id = (String) json.get("internal_id");
            long_name = (String) json.get("long_name");
            price = Float.parseFloat((String) json.get("price"));
            is_coinpack = (boolean) json.get("is_coinpack");
            parameters = (String) json.get("parameters");
            internal_id = (String) json.get("internal_id");
        }
        catch(Exception e) {
            DataspinManager.Instance().AddError(new DataspinError(ErrorType.JSON_PROCESSING_ERROR, "Couldn't create DataspinItem Details: " + e.getMessage(), e));
        }
    }

    public String toString() {
        String result = "Item: "+this.internal_id+", FullName: "+long_name+", price: "+price+", is coinpack? "+this.is_coinpack+", parameters: "+this.parameters;
        return result;
    }
}
