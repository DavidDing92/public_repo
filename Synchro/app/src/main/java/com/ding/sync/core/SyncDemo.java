package com.ding.sync.core;

import android.os.Bundle;

/**
 * Created by xwding on 4/24/2018.
 */

public class SyncDemo extends SyncObj {

    @Syncable(name = "id")
    private int id;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        if (this.id != id) {
            this.id = id;
            notifyFieldChanged("id");
        }
    }

    @Syncable(name = "name")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (this.name != name || !this.name.equals(name)) {
            this.name = name;
            notifyFieldChanged("name");
        }
    }

    // no @Syncable label, won't sync
    private String age;

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    @Syncable(name = "bytes")
    private byte[] bytes;

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
        notifyFieldChanged("bytes");
    }

    @Syncable(name = "bundle")
    private Bundle bundle;

    public Bundle getBundle() {
        return bundle;
    }

    public void setBundle(Bundle bundle) {
        this.bundle = bundle;
        notifyFieldChanged("bundle");
    }
}
