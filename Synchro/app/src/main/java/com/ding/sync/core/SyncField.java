package com.ding.sync.core;

import android.os.Bundle;

import org.json.JSONException;
import org.json.JSONObject;

public class SyncField implements SyncBundle {
    private Class<? extends SyncObj> syncClass;
    private String syncId;
    private String syncField;
    private Class<?> fieldClass;
    private Object value;

    public SyncField() {}

    public SyncField(SyncObj syncObj, String fieldName, Class<?> fieldClass, Object value) {
        this(syncObj.getClass(), syncObj.getSyncId(), fieldName, fieldClass, value);
    }

    public SyncField(Class<? extends SyncObj> syncClass, String syncId, String fieldName, Class<?> fieldClass, Object value) {
        this.syncClass = syncClass;
        this.syncId = syncId;
        this.syncField = fieldName;
        this.fieldClass = fieldClass;
        this.value = value;
    }

    @Override
    public String toString() {
        JSONObject json = new JSONObject();
        try {
            json.put("sync_class", syncClass.getName());
            json.put("sync_id", syncId);
            json.put("sync_field", syncField);
            json.put("sync_value", value);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json.toString();
    }

    @Override
    public void fromBundle(Bundle bundle) {
        try {
            syncClass = (Class<? extends SyncObj>) Class.forName(bundle.getString("sync_class"));
            syncId = bundle.getString("sync_id");
            syncField = bundle.getString("sync_field");
            value = bundle.get("sync_value");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putString("sync_class", syncClass.getName());
        bundle.putString("sync_id", syncId);
        bundle.putString("sync_field", syncField);
        SyncUtil.toBundle(bundle, fieldClass, "sync_value", value);
        return bundle;
    }

    public Class<? extends SyncObj> getSyncClass() {
        return syncClass;
    }

    public String getSyncId() {
        return syncId;
    }

    public String getSyncField() {
        return syncField;
    }

    public Object getValue() {
        return value;
    }

}
