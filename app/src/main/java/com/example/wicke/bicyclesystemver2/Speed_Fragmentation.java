package com.example.wicke.bicyclesystemver2;

import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import java.util.Set;

public class Speed_Fragmentation extends DialogFragment  {
    private final static int DEVICES_DIALOG = 1;
    private final static int ERROR_DIALOG = 2;

    public Speed_Fragmentation() {

    }
    public static Speed_Fragmentation newInstance(int id, String text) {
        Speed_Fragmentation frag = new Speed_Fragmentation();
        Bundle args = new Bundle();
        args.putString("content", text);
        args.putInt("id", id);

        frag.setArguments(args);
        return frag;
    }
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String content = getArguments().getString("content");
        int id = getArguments().getInt("id");
        AlertDialog.Builder alertDialogBuilder = null;

        switch(id)
        {
            case DEVICES_DIALOG:
                alertDialogBuilder = new AlertDialog.Builder(getActivity());
                alertDialogBuilder.setTitle("Select device");

                Set<BluetoothDevice> pairedDevices = Speed.getPairedDevices();
                final BluetoothDevice[] devices = pairedDevices.toArray(new BluetoothDevice[0]);
                String[] items = new String[devices.length];
                for (int i=0;i<devices.length;i++) {
                    items[i] = devices[i].getName();
                }

                alertDialogBuilder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        ((Speed)Speed.mContext).doConnect(devices[which]);
                    }
                });
                alertDialogBuilder.setCancelable(false);
                break;

            case ERROR_DIALOG:
                alertDialogBuilder = new AlertDialog.Builder(getActivity());
                alertDialogBuilder.setTitle("ERROR");
                alertDialogBuilder.setMessage(content);
                alertDialogBuilder.setPositiveButton("OK",  new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        ((Speed)Speed.mContext).finish();
                    }
                });
                break;
        }
        return alertDialogBuilder.create();
    }
}
