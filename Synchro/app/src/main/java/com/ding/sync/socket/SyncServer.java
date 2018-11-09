package com.ding.sync.socket;

import android.support.annotation.NonNull;

import com.ding.sync.core.SyncManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by xwding on 11/5/2018.
 */

public class SyncServer {
    private final int mPort;
    private final Map<Integer, SyncClient> mClients;
    private final ExecutorService mThreadPool;

    private ServerSocket mServerSocket;

    public SyncServer(int port) {
        mPort = port;
        mClients = new ConcurrentHashMap<>();
        mThreadPool = Executors.newSingleThreadExecutor();
    }

    public void start() throws IOException {
        if (mServerSocket != null) {
            return;
        }
        mServerSocket = new ServerSocket(mPort);
        mThreadPool.submit(new Runnable() {
            @Override
            public void run() {
                while (!mServerSocket.isClosed()) {
                    try {
                        SyncClient client = onClientConnect();
                        mClients.put(client.getSocket().getPort(), client);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void close() throws IOException {
        if (!mServerSocket.isClosed()) {
            mServerSocket.close();
        }
    }

    private SyncClient onClientConnect() throws IOException {
        Socket socket = mServerSocket.accept();
        SyncClient client = new SyncClient(socket);
        client.start();
        return client;
    }

    public void onClientDisconnect(SyncClient client) {
        mClients.remove(client.getSocket().getPort());
    }

    @NonNull
    public Collection<SyncClient> getClients() {
        return new ArrayList<>(mClients.values());
    }

    public static void startDemo() throws IOException {
        SyncServer syncServer = new SyncServer(8888);
        syncServer.start();
        SyncManager.getInstance().setSyncConnector(new SyncServerConnector(syncServer));
    }
}
