package com.ding.sync.socket;

import com.ding.sync.core.SyncConnector;
import com.ding.sync.core.SyncMsg;

import java.io.IOException;
import java.util.ConcurrentModificationException;

/**
 * Created by xwding on 11/5/2018.
 */

public class SyncServerConnector extends SyncConnector {
    private SyncServer mServer;

    public SyncServerConnector(SyncServer server) {
        mServer = server;
    }

    @Override
    public void sendSyncMsg(SyncMsg syncMsg) {
        try {
            for (SyncClient client : mServer.getClients()) {
                client.sendMsg(syncMsg);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
