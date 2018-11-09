package com.ding.sync.core;

public interface SyncLifecycleListener<T extends SyncObj> {
    void onLifecycleChange(T syncObj, @SyncMsg.Type int state);
}
