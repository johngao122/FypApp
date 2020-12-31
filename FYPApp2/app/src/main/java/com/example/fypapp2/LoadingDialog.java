package com.example.fypapp2;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.view.LayoutInflater;
import android.widget.Toast;

public class LoadingDialog extends Application {

    Activity activity;
    AlertDialog alertDialog;

    LoadingDialog(Activity myactivity){
        activity = myactivity;
    }

    void startloading(){
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        LayoutInflater inflater = activity.getLayoutInflater();
        builder.setView(inflater.inflate(R.layout.loadingdialog,null));
        builder.setCancelable(false);

        alertDialog = builder.create();
        alertDialog.show();
    }

    void dismiss(){
        alertDialog.dismiss();
    }
}
