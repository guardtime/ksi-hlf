
package org.guardtime.ksi.hlf.ledgerapi;

import org.json.JSONObject;
import org.json.JSONPropertyName;

import static java.nio.charset.StandardCharsets.UTF_8;

public abstract class State {
    protected String key;

    @JSONPropertyName("key")
    public String getKey() {
        return key;
    }

    public String[] getSplitKey() {
        return key.split(".");
    }

    public abstract State parse(byte[] data) throws RuntimeException;
    
    public boolean isInit() {
        return !key.isEmpty();
    }

    public byte[] serialize() {
        System.out.println("Serializing: " + this.getClass().getName());
        String jsonStr = new JSONObject(this).toString();
        return jsonStr.getBytes(UTF_8);
    }

}

