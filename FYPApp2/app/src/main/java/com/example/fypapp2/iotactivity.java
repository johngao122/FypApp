


package com.example.fypapp2;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobileconnectors.iot.AWSIotKeystoreHelper;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttLastWillAndTestament;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.AttachPrincipalPolicyRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateResult;
import com.example.fypapp2.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.google.zxing.qrcode.encoder.QRCode;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.security.KeyStore;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

@TargetApi(18)




public class iotactivity extends Activity {

    static final String LOG_TAG = iotactivity.class.getCanonicalName();
    RecyclerView recyclerView;
    ArrayList<String> Name, Data;

    private static final int REQUEST_CAMERA_PERMISSION = 201;


    //REST endpoint for IOT
    private static final String CUSTOMER_SPECIFIC_ENDPOINT = "a3mmmp6isp7ryu-ats.iot.ap-southeast-1.amazonaws.com";

    private static final String COGNITO_POOL_ID = "ap-southeast-1:de732ae3-03ec-46a8-9a77-2338f4a00c6b";
    //AWS policy
    private static final String AWS_IOT_POLICY_NAME = "androidappPolicy";

    // Region of AWS IoT
    private static final Regions MY_REGION = Regions.AP_SOUTHEAST_1;
    //Keystore stuff
    private static final String KEYSTORE_NAME = "iot_keystore";
    private static final String KEYSTORE_PASSWORD = "password";
    private static final String CERTIFICATE_ID = "default"; //IMPORTANT: set default so the cert wont be created everytime a new instance is loaded
    private static final String UPLINK_TOPIC = "+/devices/+/up";
    String message;



    TextView tvStatus;
    FloatingActionButton btnAdd,btnconnect,btndisconnect;

    AWSIotClient mIotAndroidClient;
    AWSIotMqttManager mqttManager;
    String clientId;
    String keystorePath;
    String keystoreName;
    String keystorePassword;
    String email;

    KeyStore clientKeyStore = null;
    String certificateId;

    CognitoCachingCredentialsProvider credentialsProvider;
    TinyDB tinyDB;
    JSONObject jsonObject;
    Boolean temp, pressure, humid, adc, digital, digital2,connected = false;
    Integer tempalert, pressurealert, humidalert, adcalert, logicalert, logicalert2;







    @Override
    protected void onResume() {
        super.onResume();
        loadData();
        Intent i  = new Intent(this,MyService.class);
        stopService(i);
        if(connected == true){
            tvStatus.setText("Connected");
            mqttconnect();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_iotactivity);
        tinyDB = new TinyDB(getApplicationContext());
        email = getIntent().getStringExtra("Email");
        tvStatus = (TextView) findViewById(R.id.connected);
        btnAdd = (FloatingActionButton) findViewById(R.id.btAdd);
        btnAdd.setOnClickListener(addclick);
        btnconnect = (FloatingActionButton) findViewById(R.id.btconnect);
        btnconnect.setOnClickListener(connectClick);
        btndisconnect = (FloatingActionButton) findViewById(R.id.btdisconnect);
        btndisconnect.setOnClickListener(disconnectClick);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        loadData();
        Intent serviceintent = new Intent(this,MyService.class);
        stopService(serviceintent);
        //To generate a unique uuid
        clientId = UUID.randomUUID().toString();
        connected = false;
        // Initialize the AWS Cognito credentials provider
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                COGNITO_POOL_ID,
                MY_REGION
        );

        Region region = Region.getRegion(MY_REGION);


        mqttManager = new AWSIotMqttManager(clientId, CUSTOMER_SPECIFIC_ENDPOINT);


        // MQTT pings every 10 seconds.
        mqttManager.setKeepAlive(10);

        //IMPORTANT: AWS app server will crash if this is not implemented(bad code from amazon)
        AWSIotMqttLastWillAndTestament lwt = new AWSIotMqttLastWillAndTestament("my/lwt/topic",
                "Android client lost connection", AWSIotMqttQos.QOS0);
        mqttManager.setMqttLastWillAndTestament(lwt);

        //Details to make cert if not allowed
        mIotAndroidClient = new AWSIotClient(credentialsProvider);
        mIotAndroidClient.setRegion(region);

        keystorePath = getFilesDir().getPath();
        keystoreName = KEYSTORE_NAME;
        keystorePassword = KEYSTORE_PASSWORD;
        certificateId = CERTIFICATE_ID;

        //Check if existing cert exist
        try {
            if (AWSIotKeystoreHelper.isKeystorePresent(keystorePath, keystoreName)) {
                if (AWSIotKeystoreHelper.keystoreContainsAlias(certificateId, keystorePath,
                        keystoreName, keystorePassword)) {
                    Log.i(LOG_TAG, "Certificate " + certificateId
                            + " found in keystore - using for MQTT.");
                    // load keystore from file into memory to pass on connection
                    clientKeyStore = AWSIotKeystoreHelper.getIotKeystore(certificateId,
                            keystorePath, keystoreName, keystorePassword);
                } else {
                    Log.i(LOG_TAG, "Key/cert " + certificateId + " not found in keystore.");
                }
            } else {
                Log.i(LOG_TAG, "Keystore " + keystorePath + "/" + keystoreName + " not found.");
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "An error occurred retrieving cert/key from keystore.", e);
        }

        if (clientKeyStore == null) {
            Log.i(LOG_TAG, "Cert/key was not found in keystore - creating new key and certificate.");

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        CreateKeysAndCertificateRequest createKeysAndCertificateRequest =
                                new CreateKeysAndCertificateRequest();
                        createKeysAndCertificateRequest.setSetAsActive(true);
                        final CreateKeysAndCertificateResult createKeysAndCertificateResult;
                        createKeysAndCertificateResult =
                                mIotAndroidClient.createKeysAndCertificate(createKeysAndCertificateRequest);
                        Log.i(LOG_TAG,
                                "Cert ID: " +
                                        createKeysAndCertificateResult.getCertificateId() +
                                        " created.");


                        AWSIotKeystoreHelper.saveCertificateAndPrivateKey(certificateId,
                                createKeysAndCertificateResult.getCertificatePem(),
                                createKeysAndCertificateResult.getKeyPair().getPrivateKey(),
                                keystorePath, keystoreName, keystorePassword);


                        clientKeyStore = AWSIotKeystoreHelper.getIotKeystore(certificateId,
                                keystorePath, keystoreName, keystorePassword);

                        //Attach the IOT policy to the cert
                        AttachPrincipalPolicyRequest policyAttachRequest =
                                new AttachPrincipalPolicyRequest();
                        policyAttachRequest.setPolicyName(AWS_IOT_POLICY_NAME);
                        policyAttachRequest.setPrincipal(createKeysAndCertificateResult
                                .getCertificateArn());
                        mIotAndroidClient.attachPrincipalPolicy(policyAttachRequest);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                btnconnect.setEnabled(true);
                            }
                        });
                    } catch (Exception e) {
                        Log.e(LOG_TAG,
                                "Exception occurred when generating new private key and certificate.",
                                e);
                    }
                }
            }).start();
        }

        if(connected == false){
            tvStatus.setText("Disconnected");
        }

    }


    View.OnClickListener connectClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            Log.d(LOG_TAG, "clientId = " + clientId);

            mqttconnect();

        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 99) {
            if (resultCode == Activity.RESULT_OK) {
                String devicename = data.getStringExtra("Devicename");
                if(Name.contains(devicename)){
                    Toast.makeText(getApplicationContext(),"Device already exists!",Toast.LENGTH_SHORT).show();
                } else {
                    Name.add(devicename);
                    Data.add("Tap here to set up device");
                    savedata();
                    MyAdaptor myAdaptor = new MyAdaptor(this, Name, Data);
                    recyclerView.setAdapter(myAdaptor);
                    recyclerView.setLayoutManager(new LinearLayoutManager(this));
                }
            }

        }
        if (requestCode == 98) {
            if (resultCode == Activity.RESULT_OK) {
                Boolean delete = data.getBooleanExtra("Delete", false);
                if (delete) {
                    Integer pos = Name.indexOf(data.getStringExtra("Devicename"));
                    Name.remove(Name.get(pos));
                    Data.remove(Data.get(pos));
                    MyAdaptor myAdaptor = new MyAdaptor(this, Name, Data);
                    recyclerView.setAdapter(myAdaptor);
                    recyclerView.setLayoutManager(new LinearLayoutManager(this));
                    SharedPreferences sharedPreferences = getSharedPreferences(data.getStringExtra("Devicename"), MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.clear().commit();
                    editor.apply();
                    savedata();
                } else {
                    updatevar(data.getStringExtra("Devicename"));
                    updateui(data.getStringExtra("Devicename"));
                }


            }

        }
    }


    View.OnClickListener disconnectClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            try {
                mqttManager.disconnect();
                connected = false;
                tvStatus.setText("Disconnected");
            } catch (Exception e) {
                Log.e(LOG_TAG, "Disconnect error.", e);
            }

        }
    };

    private View.OnClickListener addclick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent i = new Intent(getApplicationContext(), QRcodeadd.class);
            startActivityForResult(i, 99);

        }
    };

    private void savedata() {
        SharedPreferences sharedPreferences = getSharedPreferences(email, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String jsondata1 = gson.toJson(Name);
        String jsondata2 = gson.toJson(Data);
        editor.putString("Devices", jsondata1);
        editor.putString("Data", jsondata2);
        editor.putBoolean("connected",connected);
        editor.apply();
    }

    private void loadData() {
        email = getIntent().getStringExtra("email");
        SharedPreferences sharedPreferences = getSharedPreferences(email, MODE_PRIVATE);
        Gson gson = new Gson();
        String jsondata1 = sharedPreferences.getString("Devices", null);
        String jsondata2 = sharedPreferences.getString("Data", null);
        Type type1 = new TypeToken<ArrayList<String>>() {
        }.getType();
        Type type2 = new TypeToken<ArrayList<String>>() {
        }.getType();
        Name = gson.fromJson(jsondata1, type1);
        Data = gson.fromJson(jsondata2, type2);

        if (Name == null) {
            Name = new ArrayList<>();
        }
        if (Data == null) {
            Data = new ArrayList<>();
        }
        connected = sharedPreferences.getBoolean("connected",false);

        MyAdaptor myAdaptor = new MyAdaptor(this, Name, Data);
        recyclerView.setAdapter(myAdaptor);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    protected void onStop() {
        super.onStop();
        savedata();
        if(connected == true) {
            Intent intent = new Intent(this, MyService.class);
            intent.putExtra("email",email);
            startService(intent);
        }
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        savedata();
    }

    public void Processdata(String msg) {
        try {
            jsonObject = new JSONObject(msg);
            String displaymsg = "";
            String devicename = jsonObject.getString("dev_id");
            if (Name.contains(devicename)) {
                if (!msg.isEmpty()) {

                updatevar(devicename);
                JSONObject object = jsonObject.getJSONObject("payload_fields");


                    Integer pos = Name.indexOf(devicename);
                    if (temp == true) {
                        Integer temp = object.getInt("temperature");
                        if (temp > tempalert) {
                            displaymsg += "Temperature too high! (" + temp.toString() + "°C)\n";
                        } else {
                            displaymsg += "Temperature: " + temp.toString() + "°C\n";
                        }
                    }
                    if (pressure == true) {
                        Integer pres = object.getInt("pressure");
                        if (pres > pressurealert) {
                            displaymsg += "Pressure abnormal! (" + pres.toString() + "hPa)\n";
                        } else {
                            displaymsg += "Pressure: " + pres.toString() + "hPa\n";
                        }
                    }
                    if (humid == true) {
                        Integer humid = object.getInt("humidity");
                        if (humid > humidalert) {
                            displaymsg += "Humidity too high! (" + humid.toString() + "%)\n";
                        } else {
                            displaymsg += "Humidity: " + humid.toString() + "%\n";
                        }
                    }
                    if (adc == true) {
                        Double adc = (object.getInt("battery") / 255) * 3.3;
                        if (adc < adcalert) {
                            displaymsg += "Voltage too low! (" + adc.toString() + "V)\n";
                        } else {
                            displaymsg += "Voltage " + adc.toString() + "V\n";
                        }
                    }
                    if (digital == true) {
                        Integer dig = object.getInt("dig");
                        if (dig == logicalert) {
                            displaymsg += "Digital pin 1: " + dig.toString() + "\n";
                        } else {
                            displaymsg += "Digital pin 1: " + dig.toString() + "\n";
                        }
                    }
                    if (digital2 == true) {
                        Integer dig2 = object.getInt("dig2");
                        if (dig2 == logicalert) {
                            displaymsg += "Digital pin 2: " + dig2.toString() + "\n";
                        } else {
                            displaymsg += "Digital pin 2: " + dig2.toString() + "\n";
                        }
                    }



                    Integer batt = object.getInt("battery");
                    batt = (batt/254) * 100;
                    if (batt < 2) {
                        displaymsg += "Battery level low!";
                    } else {
                        displaymsg += "Battery level: " + batt.toString() + "%\n";
                    }

                    Data.set(pos, displaymsg);
                }
            }
            MyAdaptor myAdaptor = new MyAdaptor(this, Name, Data);
            recyclerView.setAdapter(myAdaptor);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));


        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void updateui(String device){

        String displaymsg = "";

        if (temp == true) {
                displaymsg += "Temperature: \n";
            }
        if (pressure == true) {
            displaymsg += "Pressure: \n";
        }
        if (humid == true) {
            displaymsg += "Humidity: \n";
        }
        if (adc == true) {
            displaymsg += "Voltage level: \n";
        }
        if (digital == true) {
            displaymsg += "Digital state: \n";
        }
        if (digital2 == true) {
            displaymsg += "Digital state: \n";
        }
        displaymsg += "Battery level: ";
        Data.set(Name.indexOf(device),displaymsg);
        MyAdaptor myAdaptor = new MyAdaptor(this, Name, Data);
        recyclerView.setAdapter(myAdaptor);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        savedata();
    }



    private void updatevar(String device){
        SharedPreferences sharedPreferences = getSharedPreferences(device,MODE_PRIVATE);
        temp = sharedPreferences.getBoolean("Temp",false);
        pressure = sharedPreferences.getBoolean("Pressure",false);
        humid = sharedPreferences.getBoolean("Humidity",false);
        adc = sharedPreferences.getBoolean("ADC",false);
        digital = sharedPreferences.getBoolean("Digital",false);
        digital2 = sharedPreferences.getBoolean("Digital2",false);



        String temptext = sharedPreferences.getString("alerttemp","");
        String pressuretext = sharedPreferences.getString("alertpressure","");
        String humidtext = sharedPreferences.getString("alerthumid","");
        String adctext = sharedPreferences.getString("alertadc","");

        if(!temptext.isEmpty()) {
            tempalert = Integer.parseInt(temptext);
        } else {
            tempalert = 0;
        }
        if(!pressuretext.isEmpty()){
            pressurealert = Integer.parseInt(pressuretext);
        } else {
            pressurealert = 0;
        }
        if(!humidtext.isEmpty()){
            humidalert = Integer.parseInt(humidtext);
        } else {
            humidalert = 0;
        }
        if(!adctext.isEmpty()){
            adcalert = Integer.parseInt(adctext);
        } else {
            adcalert = 0;
        }
        logicalert = sharedPreferences.getInt("alertdigital",1);
        logicalert2 = sharedPreferences.getInt("alertdigital2",1);

    }

    private void mqttconnect(){
        try {
            mqttManager.connect(clientKeyStore, new AWSIotMqttClientStatusCallback() {
                @Override
                public void onStatusChanged(final AWSIotMqttClientStatus status,
                                            final Throwable throwable) {
                    Log.d(LOG_TAG, "Status = " + String.valueOf(status));

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (status == AWSIotMqttClientStatus.Connecting) {
                                tvStatus.setText("Connecting...");

                            } else if (status == AWSIotMqttClientStatus.Connected) {
                                tvStatus.setText("Connected");
                                connected = true;
                                try {
                                    mqttManager.subscribeToTopic(UPLINK_TOPIC, AWSIotMqttQos.QOS0,
                                            new AWSIotMqttNewMessageCallback() {
                                                @Override
                                                public void onMessageArrived(final String topic, final byte[] data) {
                                                    runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            try {
                                                                message = new String(data, "UTF-8");
                                                                Log.d(LOG_TAG, "Message arrived:");
                                                                Log.d(LOG_TAG, "   Topic: " + topic);
                                                                Log.d(LOG_TAG, " Message: " + message);
                                                                Processdata(message);

                                                            } catch (UnsupportedEncodingException e) {
                                                                Log.e(LOG_TAG, "Message encoding error.", e);
                                                            }
                                                        }
                                                    });
                                                }
                                            });
                                } catch (Exception e) {
                                    Log.e(LOG_TAG, "Subscription error.", e);
                                }


                            } else if (status == AWSIotMqttClientStatus.Reconnecting) {
                                if (throwable != null) {
                                    Log.e(LOG_TAG, "Connection error.", throwable);
                                }
                                tvStatus.setText("Reconnecting");
                            } else if (status == AWSIotMqttClientStatus.ConnectionLost) {
                                if (throwable != null) {
                                    Log.e(LOG_TAG, "Connection error.", throwable);
                                }
                                tvStatus.setText("Disconnected");
                            } else {
                                tvStatus.setText("Disconnected");

                            }
                        }
                    });
                }
            });
        } catch (final Exception e) {
            Log.e(LOG_TAG, "Connection error.", e);
            tvStatus.setText("Error! " + e.getMessage());
        }

    }



}