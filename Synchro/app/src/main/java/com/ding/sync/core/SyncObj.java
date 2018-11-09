package com.ding.sync.core;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

public abstract class SyncObj implements SyncBundle {
    volatile boolean mSyncRegistered;
    protected boolean mIsMaster = true;

    @Syncable(name = "sync_id")
    protected String mSyncId = UUID.randomUUID().toString();

    public void fromBundle(@NonNull Bundle bundle) {
        Map<String, Field> syncables = SyncUtil.getFields(getClass());
        for (String key : bundle.keySet()) {
            Field syncable = syncables.get(key);
            if (syncable != null) {
                try {
                    syncable.set(this, bundle.get(key));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @NonNull
    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        Map<Field, Syncable> syncables = SyncUtil.getSyncables(getClass());
        for (Map.Entry<Field, Syncable> entry : syncables.entrySet()) {
            Field field = entry.getKey();
            Syncable syncable = entry.getValue();
            if (field != null && syncable != null) {
                try {
                    SyncUtil.toBundle(bundle, field.getType(), syncable.name(), field.get(this));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return bundle;
    }

    public void register() {
        SyncManager.getInstance().register(this);
        mSyncRegistered = true;
    }

    public void unregister() {
        SyncManager.getInstance().unregister(this);
        mSyncRegistered = false;
    }

    public void notifyFieldChanged(@NonNull String syncFieldName) {
        if (!isSyncRegistered()) {
            return;
        }
        Map<String, Field> fields = SyncUtil.getFields(getClass());
        if (fields == null) {
            return;
        }
        Field field = fields.get(syncFieldName);
        if (field == null) {
            return;
        }

        try {
            SyncManager.getInstance().syncFiled(new SyncField(this, syncFieldName, field.getType(), field.get(this)));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public boolean isMaster() {
        return mIsMaster;
    }

    public void setMaster(boolean isMaster) {
        mIsMaster = isMaster;
    }

    @NonNull
    public String getSyncId() {
        return mSyncId;
    }

    public void setSyncId(@NonNull String syncId) {
        this.mSyncId = syncId;
    }

    public boolean isSyncRegistered() {
        return mSyncRegistered;
    }

    @Nullable
    public <T> T getValue(@NonNull String syncField) {
        Map<String, Field> fields = SyncUtil.getFields(getClass());
        if (fields != null) {
            Field field = fields.get(syncField);
            if (field != null) {
                try {
                    return (T) field.get(this);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        return null;
    }

}
