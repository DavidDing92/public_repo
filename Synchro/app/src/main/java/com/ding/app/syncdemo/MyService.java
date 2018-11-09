package com.ding.app.syncdemo;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.ding.sync.socket.SyncClient;

import java.io.IOException;

/**
 * Created by xwding on 11/6/2018.
 */

public class MyService extends Service {

    public MyService() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    SyncClient.startSyncClient();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
