package com.example.fypapp2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class MyReceiver extends BroadcastReceiver {

    //When receiver is called it will restart the service

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        Log.i("Broadcast","Broadcast listened");
        Toast.makeText(context,"Service restarted",Toast.LENGTH_SHORT).show();

        context.startService(new Intent(context,MyService.class));
    }
}