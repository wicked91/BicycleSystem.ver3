package com.example.wicke.bicyclesystemver2;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


public class Speed extends AppCompatActivity {
    private final static int DEVICES_DIALOG = 1;
    private final static int ERROR_DIALOG = 2;

    public static Context mContext;
    public static AppCompatActivity activity;

    TextView mRecv, maxRecv;
    static BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    double max_speed_data=0.0;
    volatile boolean stopWorker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speed);
        mRecv = (TextView)findViewById(R.id.recv);
        maxRecv = (TextView)findViewById(R.id.max_recv);


        mContext = this;
        activity=this;

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            ErrorDialog("This device is not implement Bluetooth.");
            return;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            ErrorDialog("This device is disabled Bluetooth.");
            return;
        }else DeviceDialog();

    }
    static public Set<BluetoothDevice> getPairedDevices() {
        return mBluetoothAdapter.getBondedDevices();
    }
    @Override
    public void onBackPressed() {
        doClose();
        super.onBackPressed();
    }

    public void doClose() {
        workerThread.interrupt();
        new CloseTask().execute();
    }
    public void doConnect(BluetoothDevice device) {
        mmDevice = device;

        //Standard SerialPortService ID
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

        try {
            mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
            mBluetoothAdapter.cancelDiscovery();
            new ConnectTask().execute();
        } catch (IOException e) {
            Log.e("", e.toString(), e);
            ErrorDialog("doConnect "+e.toString());
        }
    }
    private class ConnectTask extends AsyncTask<Void, Void, Object> {
        @Override
        protected void onPreExecute() {

        }

        @Override
        protected Object doInBackground(Void... params) {
            try {

                mmSocket.connect();
                mmOutputStream = mmSocket.getOutputStream();
                mmInputStream = mmSocket.getInputStream();

                beginListenForData();


            } catch (Throwable t) {
                Log.e( "", "connect? "+ t.getMessage() );
                doClose();
                return t;
            }
            return null;
        }
    }
    private class CloseTask extends AsyncTask<Void, Void, Object> {
        @Override
        protected Object doInBackground(Void... params) {
            try {
                try{mmOutputStream.close();}catch(Throwable t){/*ignore*/}
                try{mmInputStream.close();}catch(Throwable t){/*ignore*/}
                mmSocket.close();
            } catch (Throwable t) {
                return t;
            }
            return null;
        }
    }

    public void DeviceDialog()
    {
        if (activity.isFinishing()) return;

        FragmentManager fm = Speed.this.getSupportFragmentManager();
        Speed_Fragmentation alertDialog = Speed_Fragmentation.newInstance(DEVICES_DIALOG, "");
        alertDialog.show(fm, "");
    }

    public void ErrorDialog(String text)
    {
        if (activity.isFinishing()) return;

        FragmentManager fm = Speed.this.getSupportFragmentManager();
        Speed_Fragmentation alertDialog = Speed_Fragmentation.newInstance(ERROR_DIALOG, text);
        alertDialog.show(fm, "");
    }

    void beginListenForData()
    {
        final Handler handler = new Handler(Looper.getMainLooper());

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    try
                    {
                        int bytesAvailable = mmInputStream.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == '\n')
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");

                                    readBufferPosition = 0;


                                    handler.post(new Runnable()
                                    {
                                        public void run()
                                        {
                                            mRecv.setText(data);
                                            max_speed_data = Math.max(max_speed_data,Double.parseDouble(data));
                                            maxRecv.setText(Double.toString(max_speed_data));
                                        }
                                    });
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }
}
