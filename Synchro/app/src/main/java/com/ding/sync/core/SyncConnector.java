package com.ding.sync.core;

import android.support.annotation.NonNull;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by xwding on 5/2/2018.
 */

public abstract class SyncConnector {
    private ExecutorService mThreadPool;

    public void setExecutor(@NonNull ExecutorService executor) {
        mThreadPool = executor;
    }

    @NonNull
    public ExecutorService getExecutor() {
        if (mThreadPool == null) {
            mThreadPool = Executors.newSingleThreadExecutor();
        }
        return mThreadPool;
    }

    public void onSyncMsg(final SyncMsg syncMsg) {
        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                syncMsgInBackground(syncMsg);
            }
        });
    }

    protected void syncMsgInBackground(SyncMsg syncMsg) {
        if (syncMsg == null || syncMsg.syncObj == null) {
            Log.e(SyncUtil.TAG, syncMsg == null ? "syncMsg is null" : "syncObj is null");
            return;
        }
        syncMsg.syncObj.setClassLoader(getClass().getClassLoader());
        String syncClassName = syncMsg.syncClass;
        Class<? extends SyncObj> syncClass = null;
        try {
            syncClass = (Class<? extends SyncObj>) Class.forName(syncClassName, true, getClass().getClassLoader());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        if (syncClass == null) {
            return;
        }
        switch (syncMsg.syncType) {
            case SyncMsg.Type.TYPE_FIELD:
                SyncField syncField = new SyncField();
                syncField.fromBundle(syncMsg.syncObj);
                SyncManager.getInstance().syncFiled(syncField);
                break;
            case SyncMsg.Type.TYPE_REGISTER:
                try {
                    SyncObj syncObj = syncClass.newInstance();
                    syncObj.setMaster(false);
                    syncObj.fromBundle(syncMsg.syncObj);
                    syncObj.register();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                break;
            case SyncMsg.Type.TYPE_UNREGISTER:
                try {
                    SyncObj syncObj = syncClass.newInstance();
                    syncObj.setMaster(false);
                    syncObj.fromBundle(syncMsg.syncObj);
                    syncObj.unregister();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    protected void sendSyncMsgInBackground(final SyncMsg syncMsg) {
        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                sendSyncMsg(syncMsg);
            }
        });
    }

    public abstract void sendSyncMsg(SyncMsg syncMsg);

}
