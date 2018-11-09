package com.ding.sync.socket;

import android.os.Bundle;
import android.os.Parcel;
import android.util.Log;

import com.ding.sync.core.SyncConnector;
import com.ding.sync.core.SyncDemo;
import com.ding.sync.core.SyncManager;
import com.ding.sync.core.SyncMsg;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by xwding on 11/5/2018.
 */

public class SyncClient {
    private final Socket mSocket;
    private final ExecutorService mParser;
    private final byte[] mMsgBuffer = new byte[1024 * 1024]; // 1M buffer

    private DataInputStream mInputStream;
    private DataOutputStream mOutputStream;

    public SyncClient(Socket socket) {
        mSocket = socket;
        mParser = Executors.newSingleThreadExecutor();
    }

    public void start() throws IOException {
        if (mInputStream != null) {
            return;
        }
        mInputStream = new DataInputStream(mSocket.getInputStream());
        mOutputStream = new DataOutputStream(mSocket.getOutputStream());
        mParser.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        parseMsg();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    close();
                }
            }
        });
    }

    public void close() {
        if (!mSocket.isClosed()) {
            try {
                mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void parseMsg() throws IOException {
        int msgLen = mInputStream.readInt();
        byte[] msgBytes;
        if (msgLen <= mMsgBuffer.length) {
            msgBytes = mMsgBuffer;
        } else {
            msgBytes = new byte[msgLen];
        }
        mInputStream.readFully(msgBytes, 0, msgLen);
        Parcel msgParcel = Parcel.obtain();
        msgParcel.unmarshall(msgBytes, 0, msgLen);
        msgParcel.setDataPosition(0);
        SyncMsg msg = SyncMsg.CREATOR.createFromParcel(msgParcel);
        onMsg(msg);
    }

    public void onMsg(SyncMsg syncMsg) {
        SyncManager.getInstance().getSyncConnector().onSyncMsg(syncMsg);
    }

    public Socket getSocket() {
        return mSocket;
    }

    public void sendMsg(SyncMsg syncMsg) throws IOException {
        Parcel parcel = Parcel.obtain();
        syncMsg.writeToParcel(parcel, 0);
        byte[] bytes = parcel.marshall();
        Log.e("David", "msg len send: " + bytes.length);
        mOutputStream.writeInt(bytes.length);
        mOutputStream.write(bytes);
        mOutputStream.flush();
    }

    public static void startSyncClient() throws IOException {
        Socket socket = new Socket("127.0.0.1", 8888);
        SyncClient syncClient = new SyncClient(socket);
        syncClient.start();
        SyncConnector connector = new SyncClientConnector(syncClient);
        SyncManager.getInstance().setSyncConnector(connector);

        Bundle bundle = new Bundle();
        bundle.putByteArray("byteArray", new byte[10 * 1024 * 1024]);
        bundle.putString("luna", "Luna is a development language");

        SyncDemo demo = new SyncDemo();
        demo.setId(0);
        demo.setName("Mmhm0713");
        demo.setAge("16 month");
        demo.setBytes(new byte[1]);
        demo.setBundle(bundle);
        demo.register();

        bundle.remove("byteArray");
        demo.setBundle(new Bundle(bundle));
    }
}
