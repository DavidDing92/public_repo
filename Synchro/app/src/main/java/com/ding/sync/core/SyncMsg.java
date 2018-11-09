package com.ding.sync.core;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by xwding on 4/27/2018.
 */

public class SyncMsg implements Parcelable {
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({Type.TYPE_REGISTER, Type.TYPE_UNREGISTER, Type.TYPE_FIELD})
    public @interface Type {
        int TYPE_REGISTER = 0;
        int TYPE_UNREGISTER = 1;
        int TYPE_FIELD = 2;
    }

    public int syncType;
    public String syncClass;
    public Bundle syncObj;

    public SyncMsg() {}


    public SyncMsg(SyncField syncField) {
        syncClass = syncField.getSyncClass().getName();
        syncObj = syncField.toBundle();
        syncType = SyncMsg.Type.TYPE_FIELD;
    }

    public SyncMsg(SyncObj originSyncObj, @SyncMsg.Type int state) {
        syncClass = originSyncObj.getClass().getName();
        syncObj = originSyncObj.toBundle();
        syncType = state;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public void readFromParcel(Parcel parcel) {
        syncType = parcel.readInt();
        syncClass = parcel.readString();
        syncObj = parcel.readBundle();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(syncType);
        dest.writeString(syncClass);
        dest.writeBundle(syncObj);
    }

    public static final Creator<SyncMsg> CREATOR = new Creator<SyncMsg>() {
        public SyncMsg createFromParcel(Parcel in) {
            SyncMsg syncMsg = new SyncMsg();
            syncMsg.readFromParcel(in);
            return syncMsg;
        }

        public SyncMsg[] newArray(int size) {
            return new SyncMsg[size];
        }
    };

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("sync_type: " + syncType);
        sb.append(", sync_class: " + syncClass);
        sb.append(", syncObj: ");
        for (String key : syncObj.keySet()) {
            sb.append("[" + key + "=" + syncObj.get(key) + "]");
        }

        return sb.toString();
    }
}
