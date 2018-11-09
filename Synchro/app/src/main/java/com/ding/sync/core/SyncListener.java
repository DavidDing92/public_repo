package com.ding.sync.core;

public interface SyncListener<T extends SyncObj> {
    void onChanged(T syncObj, String syncField);
}
