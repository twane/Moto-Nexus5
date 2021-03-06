package com.example.moto.app;

/**
 * Created by agervail on 12/06/14.
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class BtInterface {

    private BluetoothDevice device = null;
    private BluetoothSocket socket = null;
    private InputStream receiveStream = null;
    private OutputStream sendStream = null;

    private ReceiverThread receiverThread;

    Handler handler;

    public BtInterface(Handler hstatus, Handler h) {
        Set<BluetoothDevice> setpairedDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
        BluetoothDevice[] pairedDevices = (BluetoothDevice[]) setpairedDevices.toArray(new BluetoothDevice[setpairedDevices.size()]);

        for(int i=0;i<pairedDevices.length;i++) {
            if(pairedDevices[i].getName().contains("HC-06")) {
                device = pairedDevices[i];
                try {
                    socket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                    receiveStream = socket.getInputStream();
                    sendStream = socket.getOutputStream();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
        }

        handler = hstatus;

        receiverThread = new ReceiverThread(h);
    }

    public void sendData(String data) {
        sendData(data, false);
    }

    public void sendData(String data, boolean deleteScheduledData) {
        try {
            sendStream.write(data.getBytes());
            sendStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void connect(final Activity activity) {
        new Thread() {
            @Override public void run() {
                try {
                    if(socket != null) {
                        socket.connect();

                        Message msg = handler.obtainMessage();
                        msg.arg1 = 1;
                        handler.sendMessage(msg);

                        receiverThread.start();
                    } else {
                        activity.runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(activity, "No device found", Toast.LENGTH_SHORT).show();
                            }
                        });

                    }
                } catch (IOException e) {
                    Log.v("N", "Connection Failed : "+e.getMessage());
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public void close() {
        try {
            if(socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    private class ReceiverThread extends Thread {
        Handler handler;

        ReceiverThread(Handler h) {
            handler = h;
        }

        @Override public void run() {
            while(true) {
                try {
                    if(receiveStream.available() > 0) {
                        Log.v("N4", "Message received");
                        byte buffer[] = new byte[100];
                        int k = receiveStream.read(buffer, 0, 100);

                        if(k > 0) {
                            byte rawdata[] = new byte[k];
                            for(int i=0;i<k;i++)
                                rawdata[i] = buffer[i];

                            String data = new String(rawdata);

                            Message msg = handler.obtainMessage();
                            Bundle b = new Bundle();
                            b.putString("receivedData", data);
                            msg.setData(b);
                            handler.sendMessage(msg);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}