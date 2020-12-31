package com.example.fypapp2;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.UUID;


//Service code for basic it functions when the app is off

public class MyService extends Service {
    public MyService() {
    }

    static final String LOG_TAG = iotactivity.class.getCanonicalName();
    RecyclerView recyclerView;
    ArrayList<String> Name, Data;


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
    Boolean temp, pressure, humid, adc, digital, digital2;
    Integer tempalert, pressurealert, humidalert, adcalert, logicalert, logicalert2;
    PendingIntent pendingIntent;


    @Override
    public void onCreate() {
        super.onCreate();
        clientId = UUID.randomUUID().toString();

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


                    } catch (Exception e) {
                        Log.e(LOG_TAG,
                                "Exception occurred when generating new private key and certificate.",
                                e);
                    }
                }
            }).start();
        }

        createnotificationchannel();

        Intent notifintent = new Intent(this, iotactivity.class);
        pendingIntent = PendingIntent.getActivity(this, 1, notifintent, 0);
        Notification notification = new NotificationCompat.Builder(this, "notif")
                .setContentTitle("Service has started")
                .setContentText("Service is running in the background")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);


    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("restartservice");
        broadcastIntent.setClass(   this,MyReceiver.class);
        this.sendBroadcast(broadcastIntent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("SERVICE", "Service started ");

        loadData();
        try {
            mqttManager.connect(clientKeyStore, new AWSIotMqttClientStatusCallback() {
                @Override
                public void onStatusChanged(final AWSIotMqttClientStatus status,
                                            final Throwable throwable) {
                    Log.d(LOG_TAG, "Status = " + String.valueOf(status));


                    if (status == AWSIotMqttClientStatus.Connecting) {

                    } else if (status == AWSIotMqttClientStatus.Connected) {

                        try {
                            mqttManager.subscribeToTopic(UPLINK_TOPIC, AWSIotMqttQos.QOS0,
                                    new AWSIotMqttNewMessageCallback() {
                                        @Override
                                        public void onMessageArrived(final String topic, final byte[] data) {
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


                        } catch (Exception e) {
                            Log.e(LOG_TAG, "Subscription error.", e);

                        }


                    } else if (status == AWSIotMqttClientStatus.Reconnecting) {
                        if (throwable != null) {
                            Log.e(LOG_TAG, "Connection error.", throwable);
                        }

                    } else if (status == AWSIotMqttClientStatus.ConnectionLost) {
                        if (throwable != null) {
                            Log.e(LOG_TAG, "Connection error.", throwable);
                        }

                    } else {

                    }
                }
            });

        } catch (final Exception e) {
            Log.e(LOG_TAG, "Connection error.", e);
        }

        Intent notifintent = new Intent(this, iotactivity.class);
        pendingIntent = PendingIntent.getActivity(this, 1, notifintent, 0);
        Notification notification = new NotificationCompat.Builder(this, "notif")
                .setContentTitle("Service has started")
                .setContentText("Service is running in the background")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);
        return START_STICKY;

    }


    private void Processdata(String msg) {
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
                            Notification notification = new NotificationCompat.Builder(this, "notif")
                                    .setContentTitle("Temperature alert at " + devicename + "°C")
                                    .setContentText("Temperature is abnormal")
                                    .setSmallIcon(R.drawable.ic_launcher_background)
                                    .setContentIntent(pendingIntent)
                                    .build();
                            startForeground(1, notification);
                        }
                    }
                    if (pressure == true) {
                        Integer pres = object.getInt("pressure");
                        if (pres > pressurealert) {
                            Notification notification = new NotificationCompat.Builder(this, "notif")
                                    .setContentTitle("Pressure alert at " + devicename + " hPa")
                                    .setContentText("Pressure is abnormal")
                                    .setSmallIcon(R.drawable.ic_launcher_background)
                                    .setContentIntent(pendingIntent)
                                    .build();
                            startForeground(1, notification);
                            displaymsg += "Pressure abnormal! (" + pres.toString() + " hPa)\n";
                        } else {
                            displaymsg += "Pressure: " + pres.toString() + "\n";
                        }
                    }
                    if (humid == true) {
                        Integer humid = object.getInt("humidity");
                        if (humid > humidalert) {
                            Notification notification = new NotificationCompat.Builder(this, "notif")
                                    .setContentTitle("Humidity alert at " + devicename)
                                    .setContentText("Humidity is abnormal at " + humid.toString()+ "%")
                                    .setSmallIcon(R.drawable.ic_launcher_background)
                                    .setContentIntent(pendingIntent)
                                    .build();
                            startForeground(1, notification);
                            displaymsg += "Humidity level too high! (" + humid.toString() + "%)\n";
                        } else {
                            displaymsg += "Humidity: " + humid.toString() + "\n";
                        }
                    }
                    if (adc == true) {
                        Double adc = (object.getInt("battery") / 255) * 3.3;
                        if (adc < adcalert) {
                            Notification notification = new NotificationCompat.Builder(this, "notif")
                                    .setContentTitle("Voltage level alert at " + devicename + "V")
                                    .setContentText("Voltage level too low")
                                    .setSmallIcon(R.drawable.ic_launcher_background)
                                    .setContentIntent(pendingIntent)
                                    .build();
                            startForeground(1, notification);
                            displaymsg += "Voltage level too low! (" + adc.toString() + "%)\n";
                        } else {
                            displaymsg += "Voltage " + adc.toString() + "\n";
                        }
                    }
                    if (digital == true) {
                        Integer dig = object.getInt("dig");
                        if (dig == logicalert) {
                            Notification notification = new NotificationCompat.Builder(this, "notif")
                                    .setContentTitle("Digital state alert at " + devicename)
                                    .setContentText("Abnormal state")
                                    .setSmallIcon(R.drawable.ic_launcher_background)
                                    .setContentIntent(pendingIntent)
                                    .build();
                            startForeground(1, notification);
                            displaymsg += "Digital state: " + dig.toString() + "\n";
                        } else {
                            displaymsg += "Digital state: " + dig.toString() + "\n";
                        }
                    }
                    if (digital2 == true) {
                        Integer dig2 = object.getInt("dig2");
                        if (dig2 == logicalert) {
                            Notification notification = new NotificationCompat.Builder(this, "notif")
                                    .setContentTitle("Digital state alert at " + devicename)
                                    .setContentText("Abnormal state")
                                    .setSmallIcon(R.drawable.ic_launcher_background)
                                    .setContentIntent(pendingIntent)
                                    .build();
                            startForeground(1, notification);
                            displaymsg += "Digital state: " + dig2.toString() + "\n";
                        } else {
                            displaymsg += "Digital state: " + dig2.toString() + "\n";
                        }
                    }

                    Integer batt = object.getInt("battery");
                    batt = (batt / 254) * 100;
                    if (batt < 2) {
                        displaymsg += "Battery level low!";
                        Notification notification = new NotificationCompat.Builder(this, "notif")
                                .setContentTitle("Battery level at " + devicename)
                                .setContentText("Battery level low")
                                .setSmallIcon(R.drawable.ic_launcher_background)
                                .setContentIntent(pendingIntent)
                                .build();
                        startForeground(1, notification);
                    } else {
                        displaymsg += "Battery level: " + batt.toString() + "%\n";
                    }


                    Data.set(pos, displaymsg);
                }
            }


        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    private void updatevar(String device) {
        SharedPreferences sharedPreferences = getSharedPreferences(device, MODE_PRIVATE);
        temp = sharedPreferences.getBoolean("Temp", false);
        pressure = sharedPreferences.getBoolean("Pressure", false);
        humid = sharedPreferences.getBoolean("Humidity", false);
        adc = sharedPreferences.getBoolean("ADC", false);
        digital = sharedPreferences.getBoolean("Digital", false);
        digital2 = sharedPreferences.getBoolean("Digital2", false);


        String temptext = sharedPreferences.getString("alerttemp", null);
        String pressuretext = sharedPreferences.getString("alertpressure", null);
        String humidtext = sharedPreferences.getString("alerthumid", null);
        String adctext = sharedPreferences.getString("alertadc", null);

        if (!temptext.isEmpty()) {
            tempalert = Integer.parseInt(temptext);
        } else {
            tempalert = 0;
        }
        if (!pressuretext.isEmpty()) {
            pressurealert = Integer.parseInt(pressuretext);
        } else {
            pressurealert = 0;
        }
        if (!humidtext.isEmpty()) {
            humidalert = Integer.parseInt(humidtext);
        } else {
            humidalert = 0;
        }
        if (!adctext.isEmpty()) {
            adcalert = Integer.parseInt(adctext);
        } else {
            adcalert = 0;
        }
        logicalert = sharedPreferences.getInt("alertdigital", 1);
        logicalert2 = sharedPreferences.getInt("alertdigital2", 1);
    }

    private void loadData() {
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

    }

    private void createnotificationchannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "notif";
            String description = "test";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("notif", name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}