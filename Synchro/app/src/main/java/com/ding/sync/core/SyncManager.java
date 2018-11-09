package com.ding.sync.core;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SyncManager {
    private static SyncManager sManager = new SyncManager();

    private Map<String, SyncObj> mSyncObjs = new ConcurrentHashMap<>();
    private SyncConnector mSyncConnector;

    private Map<Class<? extends SyncObj>, List<SyncListener>> mSyncListeners = new ConcurrentHashMap<>();
    private Map<Class<? extends SyncObj>, Map<String, List<SyncListener>>> mSyncFieldListeners = new ConcurrentHashMap<>();
    private Map<Class<? extends SyncObj>, List<SyncLifecycleListener>> mSyncLifecycleListeners = new ConcurrentHashMap<>();
    private Map<Class<? extends SyncObj>, Map<String, List<SyncLifecycleListener>>> mNamedSyncLifecycleListeners = new ConcurrentHashMap<>();

    public static SyncManager getInstance() {
        return sManager;
    }

    void register(@NonNull SyncObj syncObj) {
        if (mSyncObjs.containsKey(syncObj.getSyncId())) {
            Log.w(SyncUtil.TAG, "sync obj has already registered: " + syncObj.getSyncId());
            return;
        }
        syncObj.mSyncRegistered = true;
        SyncUtil.registerClass(syncObj.getClass());
        mSyncObjs.put(syncObj.mSyncId, syncObj);
        Map<String, List<SyncLifecycleListener>> namedSyncLifecycleListeners = mNamedSyncLifecycleListeners.get(syncObj.getClass());
        if (namedSyncLifecycleListeners != null) {
            List<SyncLifecycleListener> syncLifecycleListeners = namedSyncLifecycleListeners.get(syncObj.getSyncId());
            if (syncLifecycleListeners != null) {
                for (SyncLifecycleListener lifecycleListener : syncLifecycleListeners) {
                    lifecycleListener.onLifecycleChange(syncObj, SyncMsg.Type.TYPE_REGISTER);
                }
            }
        }
        List<SyncLifecycleListener> syncLifecycleListeners = mSyncLifecycleListeners.get(syncObj.getClass());
        if (syncLifecycleListeners != null) {
            for (SyncLifecycleListener lifecycleListener : syncLifecycleListeners) {
                lifecycleListener.onLifecycleChange(syncObj, SyncMsg.Type.TYPE_REGISTER);
            }
        }

        if (syncObj.isMaster() && mSyncConnector != null) {
            mSyncConnector.sendSyncMsg(new SyncMsg(syncObj, SyncMsg.Type.TYPE_REGISTER));
        }
    }

    void unregister(@NonNull SyncObj syncObj) {
        syncObj.mSyncRegistered = false;
        SyncObj cachedSyncObj = mSyncObjs.remove(syncObj.mSyncId);
        cachedSyncObj.mSyncRegistered = false;
        if (cachedSyncObj == null) {
            Log.w(SyncUtil.TAG, "unregister sync obj but can't find the cached ojb: " + syncObj.getSyncId());
            return;
        }

        if (syncObj.isMaster() && mSyncConnector != null) {
            mSyncConnector.sendSyncMsg(new SyncMsg(cachedSyncObj, SyncMsg.Type.TYPE_UNREGISTER));
        }
        List<SyncLifecycleListener> syncLifecycleListeners = mSyncLifecycleListeners.get(cachedSyncObj.getClass());
        if (syncLifecycleListeners != null) {
            for (SyncLifecycleListener lifecycleListener : syncLifecycleListeners) {
                lifecycleListener.onLifecycleChange(cachedSyncObj, SyncMsg.Type.TYPE_UNREGISTER);
            }
        }
    }

    void syncFiled(@NonNull SyncField syncField) {
        String syncId = syncField.getSyncId();
        String fieldName = syncField.getSyncField();
        SyncObj syncObj = getSyncObj(syncId);
        if (syncObj == null) {
            Log.w(SyncUtil.TAG, "sync field failed, can't find sync cache, sync field: " + fieldName + ", sync name: : " + syncId);
            return;
        }
        if (!syncObj.isSyncRegistered()) {
            Log.w(SyncUtil.TAG, "sync field failed, not register, sync field: " + fieldName + ", sync name: : " + syncId);
            return;
        }

        if (syncObj.isMaster()) {
            if (mSyncConnector != null) {
                mSyncConnector.sendSyncMsg(new SyncMsg(syncField));
            }
        } else {
            Map<String, Field> fields = SyncUtil.getFields(syncObj.getClass());
            if (fields != null || fields.get(fieldName) != null) {
                Field field = fields.get(fieldName);
                try {
                    field.set(syncObj, syncField.getValue());
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        List<SyncListener> objListeners = mSyncListeners.get(syncObj.getClass());
        if (objListeners != null) {
            for (SyncListener objListener : objListeners) {
                objListener.onChanged(syncObj, fieldName);
            }
        }
        Map<String, List<SyncListener>> objFieldListeners = mSyncFieldListeners.get(syncObj.getClass());
        if (objFieldListeners != null) {
            List<SyncListener> fieldListeners = objFieldListeners.get(fieldName);
            if (fieldListeners != null) {
                for (SyncListener fieldListener : fieldListeners) {
                    fieldListener.onChanged(syncObj, fieldName);
                }
            }
        }
    }

    @Nullable
    SyncObj getSyncObj(@NonNull String syncId) {
        return mSyncObjs.get(syncId);
    }

    @NonNull
    public <T> List<T> getSyncObjs(@NonNull Class<? extends SyncObj> syncClass) {
        List<T> objs = new LinkedList<T>();
        for (SyncObj obj : getAllSyncObjs()) {
            if (obj.getClass().equals(syncClass) || SyncUtil.isExtend(obj.getClass(), syncClass)) {
                objs.add((T) obj);
            }
        }
        return objs;
    }

    @Nullable
    public <T> T getSyncObj(@NonNull Class<? extends SyncObj> syncClass) {
        List<T> objs = getSyncObjs(syncClass);
        switch (objs.size()) {
            case 0:
                return null;
            case 1:
                return objs.get(0);
            default:
                throw new IllegalStateException("The class should have only one instance in sync memory: " + syncClass);
        }
    }

    @NonNull
    public List<SyncObj> getAllSyncObjs() {
        List<SyncObj> allSyncObjs = new ArrayList<SyncObj>(mSyncObjs.values());
        return allSyncObjs;
    }

    public void addSyncLifecycleListener(@NonNull Class<? extends SyncObj> syncClass, @NonNull SyncLifecycleListener syncLifecycleListener, boolean notifyExist) {
        List<SyncLifecycleListener> syncLifecycleListeners = mSyncLifecycleListeners.get(syncClass);
        if (syncLifecycleListeners == null) {
            syncLifecycleListeners = new LinkedList<>();
            mSyncLifecycleListeners.put(syncClass, syncLifecycleListeners);
        }
        syncLifecycleListeners.add(syncLifecycleListener);

        if (notifyExist) {
            List<? extends SyncObj> existObjs = getSyncObjs(syncClass);
            for (SyncObj existObj : existObjs) {
                syncLifecycleListener.onLifecycleChange(existObj, SyncMsg.Type.TYPE_REGISTER);
            }
        }
    }

    public void addSyncLifecycleListener(@NonNull Class<? extends SyncObj> syncClass, @NonNull SyncLifecycleListener syncLifecycleListener) {
        addSyncLifecycleListener(syncClass, syncLifecycleListener, true);
    }

    public void removeSyncLifecycleListener(@NonNull Class<? extends SyncObj> syncClass, @NonNull SyncLifecycleListener syncLifecycleListener) {
        List<SyncLifecycleListener> syncLifecycleListeners = mSyncLifecycleListeners.get(syncClass);
        if (syncLifecycleListeners == null) {
            return;
        }
        syncLifecycleListeners.remove(syncLifecycleListener);
    }

    public void addSyncLifecycleListener(@NonNull Class<? extends SyncObj> syncClass, @NonNull SyncLifecycleListener syncLifecycleListener, @NonNull String syncId) {
        addSyncLifecycleListener(syncClass, syncLifecycleListener, syncId, true);
    }

    public void addSyncLifecycleListener(@NonNull Class<? extends SyncObj> syncClass, @NonNull SyncLifecycleListener syncLifecycleListener, @NonNull String syncId, boolean notifyExist) {
        Map<String, List<SyncLifecycleListener>> syncLifecycleListeners = mNamedSyncLifecycleListeners.get(syncClass);
        if (syncLifecycleListeners == null) {
            syncLifecycleListeners = new ConcurrentHashMap<>();
            mNamedSyncLifecycleListeners.put(syncClass, syncLifecycleListeners);
        }
        List<SyncLifecycleListener> listeners = syncLifecycleListeners.get(syncId);
        if (listeners == null) {
            listeners = new LinkedList<>();
            syncLifecycleListeners.put(syncId, listeners);
        }
        listeners.add(syncLifecycleListener);

        if (notifyExist) {
            List<? extends SyncObj> existObjs = getSyncObjs(syncClass);
            for (SyncObj existObj : existObjs) {
                syncLifecycleListener.onLifecycleChange(existObj, SyncMsg.Type.TYPE_REGISTER);
            }
        }
    }

    public void removeSyncLifecycleListener(@NonNull Class<? extends SyncObj> syncClass, @NonNull SyncLifecycleListener syncLifecycleListener, @NonNull String syncId) {
        Map<String, List<SyncLifecycleListener>> syncLifecycleListeners = mNamedSyncLifecycleListeners.get(syncClass);
        if (syncLifecycleListeners == null) {
            return;
        }
        List<SyncLifecycleListener> listeners = syncLifecycleListeners.get(syncId);
        if (listeners == null) {
            return;
        }
        listeners.remove(syncLifecycleListener);
    }

    public void addSyncListener(@NonNull Class<? extends SyncObj> syncClass, @NonNull String syncField, @NonNull SyncListener listener) {
        Map<String, List<SyncListener>> objSyncListeners = mSyncFieldListeners.get(syncClass);
        if (objSyncListeners == null) {
            objSyncListeners = new ConcurrentHashMap<>();
            mSyncFieldListeners.put(syncClass, objSyncListeners);
        }
        List<SyncListener> fieldSyncListeners = objSyncListeners.get(syncField);
        if (fieldSyncListeners == null) {
            fieldSyncListeners = new LinkedList<>();
            objSyncListeners.put(syncField, fieldSyncListeners);
        }
        fieldSyncListeners.add(listener);
    }

    public void addSyncListener(@NonNull Class<? extends SyncObj> syncClass, @NonNull SyncListener listener) {
        List<SyncListener> objSyncListeners = mSyncListeners.get(syncClass);
        if (objSyncListeners == null) {
            objSyncListeners = new LinkedList<>();
            mSyncListeners.put(syncClass, objSyncListeners);
        }
        objSyncListeners.add(listener);
    }

    public void removeSyncListener(@NonNull Class<? extends SyncObj> syncClass, @NonNull String syncField, @NonNull SyncListener listener) {
        Map<String, List<SyncListener>> objSyncListeners = mSyncFieldListeners.get(syncClass);
        if (objSyncListeners == null) {
            return;
        }
        List<SyncListener> fieldSyncListeners = objSyncListeners.get(syncField);
        if (fieldSyncListeners == null) {
            return;
        }
        fieldSyncListeners.remove(listener);
    }

    public void removeSyncListener(@NonNull Class<? extends SyncObj> syncClass, @NonNull SyncListener listener) {
        List<SyncListener> objSyncListeners = mSyncListeners.get(syncClass);
        if (objSyncListeners == null) {
            return;
        }
        objSyncListeners.remove(listener);
    }

    public SyncConnector getSyncConnector() {
        return mSyncConnector;
    }

    public void setSyncConnector(@NonNull SyncConnector syncConnector) {
        mSyncConnector = syncConnector;
    }

}
