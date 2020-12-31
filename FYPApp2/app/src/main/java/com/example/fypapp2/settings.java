package com.example.fypapp2;

import androidx.annotation.IntegerRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.mobile.client.AWSMobileClient;

public class settings extends AppCompatActivity {




    //Code for settings menu
    @Override
    protected void onStop() {
        super.onStop();
    }


    private View rectangle_9;
    private View rectangle_10;
    private ImageView imageView;

    CheckBox tempcheck, pressurecheck, humidcheck, adccheck, digitalcheck, digital2check;
    EditText alerttemp, alertpressure, alerthumid, alertadc;
    Spinner alertdig, alertdig2;
    String devicename;
    Integer logic, logic2;
    TextView devicetext;
    Boolean temp, pressure, humid, adc, digital, digital2;
    Intent i;
    Button doneButton,deleteButton;


    private CompoundButton.OnCheckedChangeListener listener1 = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            switch (buttonView.getId()) {
                case R.id.tempcheck:
                    temp = isChecked;
                    break;
                case R.id.pressurecheck:
                    pressure = isChecked;
                    break;
                case R.id.humidcheck:
                    humid = isChecked;
                    break;
                case R.id.adccheck:
                    adc = isChecked;
                    break;
                case R.id.digitalcheck:
                    digital = isChecked;
                    break;
                case R.id.digital2check:
                    digital2 = isChecked;
                    break;
            }
            Toast.makeText(getApplicationContext(), "Checked", Toast.LENGTH_SHORT).show();

        }
    };
    private AdapterView.OnItemSelectedListener listener2 = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            logic = Integer.parseInt(String.valueOf(parent.getItemAtPosition(position)));
            Toast.makeText(getApplicationContext(), logic + " selected", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            logic = 0;
        }
    };

    private AdapterView.OnItemSelectedListener listener3 = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            logic2 = Integer.parseInt(String.valueOf(parent.getItemAtPosition(position)));
            Toast.makeText(getApplicationContext(), logic2 + " selected", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            logic2 = 0;
        }
    };



    void savedata(){
        SharedPreferences sharedPreferences = getSharedPreferences(devicename, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if(temp != null) {
            editor.putBoolean("Temp", temp);
        } else {
            editor.putBoolean("Temp",false);
        }
        if(pressure != null) {
            editor.putBoolean("Pressure", pressure);
        } else {
            editor.putBoolean("Pressure",false);
        }
        if(humid != null) {
            editor.putBoolean("Humidity", humid);
        } else {
            editor.putBoolean("Humidity",false);
        }
        if(adc != null) {
            editor.putBoolean("ADC", adc);
        } else {
            editor.putBoolean("ADC",false);
        }
        if(digital != null){
            editor.putBoolean("Digital",digital);
        } else {
            editor.putBoolean("Digital",false);
        }
        if(digital2 != null) {
            editor.putBoolean("Digital2", digital2);
        } else {
            editor.putBoolean("Digital2",false);
        }
        if(!alerttemp.getText().toString().isEmpty()){
            editor.putString("alerttemp",alerttemp.getText().toString());
        } else {
            editor.putString("alerttemp","");
        }

        if(!alertpressure.getText().toString().isEmpty()) {
            editor.putString("alertpressure", alertpressure.getText().toString());
        } else {
            editor.putString("alertpressure","");
        }
        if(!alerthumid.getText().toString().isEmpty()) {
            editor.putString("alerthumid", alerthumid.getText().toString());
        } else {
            editor.putString("alerthumid","");
        }
        if(!alertadc.getText().toString().isEmpty()) {
            editor.putString("alertadc", alertadc.getText().toString());
        } else {
            editor.putString("alertadc","");
        }

        editor.putInt("alertdigital",logic);
        editor.putInt("alertdigital2",logic2);



        editor.apply();
    }



    void loaddata(){
        SharedPreferences sharedPreferences = getSharedPreferences(devicename, MODE_PRIVATE);
        String temptext = sharedPreferences.getString("alerttemp",null);
        String pressuretext = sharedPreferences.getString("alertpressure",null);
        String humidtext = sharedPreferences.getString("alerthumid",null);
        String adctext = sharedPreferences.getString("alertadc",null);

        if(temptext != null) {
            alerttemp.setText(temptext);
        } else {
            alerttemp.setText("");
        }
        if(pressuretext != null){
            alertpressure.setText(pressuretext);
        } else {
            alertpressure.setText("");
        }
        if(humidtext != null){
            alerthumid.setText(humidtext);
        } else {
            alertpressure.setText("");
        }
        if(adctext != null){
            alertadc.setText(adctext);
        } else {
            alertadc.setText("");
        }
        Integer bool1 = sharedPreferences.getInt("alertdigital",0);
        Integer bool2 = sharedPreferences.getInt("alertdigital2",0);
        if(bool1 == 1){
            alertdig.setSelection(0);
        } else {
            alertdig.setSelection(1);
        }

        if(bool2 == 1){
            alertdig2.setSelection(0);
        } else {
            alertdig2.setSelection(1);
        }

        tempcheck.setChecked(sharedPreferences.getBoolean("Temp",false));
        pressurecheck.setChecked(sharedPreferences.getBoolean("Pressure",false));
        humidcheck.setChecked(sharedPreferences.getBoolean("Humidity",false));
        adccheck.setChecked(sharedPreferences.getBoolean("ADC",false));
        digitalcheck.setChecked(sharedPreferences.getBoolean("Digital",false));
        digital2check.setChecked(sharedPreferences.getBoolean("Digital2",false));
    }

    private View.OnClickListener buttonlistener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
                if (!alerttemp.getText().toString().isEmpty()) {
                    Integer temp = Integer.parseInt(alerttemp.getText().toString());
                    i.putExtra("alerttemp", temp);
                }
                else {
                    i.putExtra("alerttemp",0);
                }
            if (!alertpressure.getText().toString().isEmpty()) {
                i.putExtra("alertpressure", Integer.parseInt(alertpressure.getText().toString()));
            }
            else{
                i.putExtra("alertpressure",0);
            }
            if (!alerthumid.getText().toString().isEmpty()) {
                i.putExtra("alerthumid", Integer.parseInt(alerthumid.getText().toString()));
            } else {
                i.putExtra("alerthumid",0);
            }
            if (!alertadc.getText().toString().isEmpty()) {
                i.putExtra("alertadc", Integer.parseInt(alertadc.getText().toString()));
            } else {
                i.putExtra("alertadc",0);
            }
            if (logic != null) {
                i.putExtra("alertdig", logic);
                Log.i("intent","returned is " + logic);
            }
            if(logic2 != null){
                i.putExtra("alertdig2",logic2);
                Log.i("intent","returned is " + logic2);
            }
            savedata();
            setResult(RESULT_OK, i);
            finish();
        }
    };

    private View.OnClickListener deletelistener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Boolean delete = true;
            i.putExtra("Delete",delete);
            setResult(RESULT_OK,i);
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);


        i = getIntent();
        devicename = getIntent().getStringExtra("Devicename");

        rectangle_9 = (View) findViewById(R.id.rectangle_9);
        rectangle_10 = (View) findViewById(R.id.rectangle_10);

        tempcheck = (CheckBox) findViewById(R.id.tempcheck);
        pressurecheck = (CheckBox) findViewById(R.id.pressurecheck);
        humidcheck = (CheckBox) findViewById(R.id.humidcheck);
        adccheck = (CheckBox) findViewById(R.id.adccheck);
        digitalcheck = (CheckBox) findViewById(R.id.digitalcheck);
        digital2check = (CheckBox) findViewById(R.id.digital2check);
        alerttemp = (EditText) findViewById(R.id.alerttemp);
        alertpressure = (EditText) findViewById(R.id.alertpressure);
        alerthumid = (EditText) findViewById(R.id.alerthumid);
        alertadc = (EditText) findViewById(R.id.alertadc);
        alertdig = (Spinner) findViewById(R.id.alertdig);
        alertdig2 = (Spinner) findViewById(R.id.alertdig2);
        devicetext = (TextView) findViewById(R.id.device_name);
        tempcheck.setOnCheckedChangeListener(listener1);
        pressurecheck.setOnCheckedChangeListener(listener1);
        humidcheck.setOnCheckedChangeListener(listener1);
        adccheck.setOnCheckedChangeListener(listener1);
        digitalcheck.setOnCheckedChangeListener(listener1);
        digital2check.setOnCheckedChangeListener(listener1);
        alertdig.setOnItemSelectedListener(listener2);
        alertdig2.setOnItemSelectedListener(listener3);
        rectangle_9.setOnClickListener(buttonlistener);
        rectangle_10.setOnClickListener(deletelistener);
        imageView = (ImageView) findViewById(R.id.microchip_ek3);
        devicetext.setText(devicename);
        loaddata();

        Intent serviceintent = new Intent(this,MyService.class);
        stopService(serviceintent);




    }

}

