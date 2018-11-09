package com.ding.app.syncdemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.ding.sync.core.SyncDemo;
import com.ding.sync.core.SyncLifecycleListener;
import com.ding.sync.core.SyncListener;
import com.ding.sync.core.SyncManager;
import com.ding.sync.core.SyncMsg;
import com.ding.sync.core.SyncObj;
import com.ding.sync.socket.SyncServer;

import java.io.IOException;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            startSyncServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startSyncServer() throws IOException {
        SyncServer.startDemo();
        startService(new Intent(this, MyService.class));

        SyncManager.getInstance().addSyncLifecycleListener(SyncDemo.class, new SyncLifecycleListener() {
            @Override
            public void onLifecycleChange(SyncObj syncObj, @SyncMsg.Type int state) {
                Log.e("David", "syncObj " + syncObj);
            }
        }, true);
        SyncManager.getInstance().addSyncListener(SyncDemo.class, "bundle", new SyncListener() {
            @Override
            public void onChanged(SyncObj syncObj, String syncField) {
                Log.e("David", "bundle " + syncObj.getValue(syncField) + " " + ((Bundle) syncObj.getValue(syncField)).getString("luna"));
            }
        });
    }
}
