package com.ding.sync.socket;

import com.ding.sync.core.SyncConnector;
import com.ding.sync.core.SyncMsg;

import java.io.IOException;

/**
 * Created by xwding on 11/5/2018.
 */

public class SyncClientConnector extends SyncConnector {
    private final SyncClient mClient;

    public SyncClientConnector(SyncClient client) {
        mClient = client;
    }

    @Override
    public void sendSyncMsg(SyncMsg syncMsg) {
        try {
            mClient.sendMsg(syncMsg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
