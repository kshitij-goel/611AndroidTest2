package com.kgoel.mycloud;

import org.json.JSONException;
import org.json.JSONObject;

public class TransmitObject {
    public String message;
    public String deviceType;

    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("message",this.message);
            jsonObject.put("deviceType",this.deviceType);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }
}
