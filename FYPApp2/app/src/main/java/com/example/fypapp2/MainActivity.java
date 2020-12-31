package com.example.fypapp2;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.ShowableListMenu;

import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.AWSCognitoIdentityProvider;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.results.SignInResult;
import com.amazonaws.mobile.client.results.SignUpResult;
import com.amazonaws.mobile.client.results.UserCodeDeliveryDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;
import com.amazonaws.services.cognitoidentityprovider.model.AdminListUserAuthEventsRequest;
import com.amazonaws.services.cognitoidentityprovider.model.ListUsersRequest;
import com.amazonaws.services.cognitoidentityprovider.model.ListUsersResult;
import com.amazonaws.mobile.client.UserState;
import com.amazonaws.mobile.client.UserStateDetails;
import com.amazonaws.mobile.client.UserStateListener;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.material.floatingactionbutton.FloatingActionButton;


import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    EditText Password, Email;
    Button Login;
    FloatingActionButton Register;
    LoadingDialog loadingDialog;

    private TextView email;
    private TextView password;
    private ImageView path_2;




    private View.OnClickListener loginListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if (email.getText().toString().isEmpty() || password.getText().toString().isEmpty()) {
                Toast.makeText(getApplicationContext(), "One or more fields are not filled in", Toast.LENGTH_SHORT).show();
            } else if (password.getText().toString().length() < 8) {
                Toast.makeText(getApplicationContext(), "Password must be at least 8 characters", Toast.LENGTH_SHORT);
            } else {

                loadingDialog.startloading();
                AWSMobileClient.getInstance().signIn(
                        Email.getText().toString(),
                        Password.getText().toString(),
                        null,
                        new Callback<SignInResult>() {
                            @Override
                            public void onResult(SignInResult result) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.d("Auth", "Sign in callback state: " + result.getSignInState());
                                        switch (result.getSignInState()) {
                                            case DONE:
                                                Toast.makeText(getApplicationContext(), "Sign in Done", Toast.LENGTH_LONG).show();
                                                Intent i = new Intent(getApplicationContext(), iotactivity.class);
                                                i.putExtra("Email", Email.getText().toString());
                                                startActivity(i);
                                                break;
                                            default:
                                                Toast.makeText(getApplicationContext(), "Unsupported sign in confirmation:" + result.getSignInState(), Toast.LENGTH_SHORT).show();
                                                loadingDialog.dismiss();
                                                break;
                                        }
                                    }
                                });
                            }

                            @Override
                            public void onError(Exception e) {
                                Log.e("Auth", "Sign in error", e);
                                loadingDialog.dismiss();
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getApplicationContext(),"Wrong Password Entered",Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                );

            }
        }
    };
    private View.OnClickListener registerlistener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {


            if (email.getText().toString().isEmpty() || password.getText().toString().isEmpty()) {
                Toast.makeText(getApplicationContext(), "One or more fields are not filled in", Toast.LENGTH_SHORT).show();
            } else if (password.getText().toString().length() < 8) {
                Toast.makeText(getApplicationContext(), "Password must be at least 8 characters", Toast.LENGTH_SHORT);
            } else {

                final Map<String, String> attributes = new HashMap<>();
                attributes.put("email", Email.getText().toString());
                AWSMobileClient.getInstance().signUp(
                        Email.getText().toString(),
                        Password.getText().toString()
                        , attributes, null, new Callback<SignUpResult>() {
                            @Override
                            public void onResult(final SignUpResult signUpResult) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.d("Auth", "Sign-up callback state: " + signUpResult.getConfirmationState());
                                        if (!signUpResult.getConfirmationState()) {
                                            final UserCodeDeliveryDetails details = signUpResult.getUserCodeDeliveryDetails();
                                            Toast.makeText(getApplicationContext(), "Confirm sign-up with: " + details.getDestination(), Toast.LENGTH_SHORT).show();
                                            Intent i = new Intent(getApplicationContext(), CodeActivity.class);
                                            i.putExtra("Username", Email.getText().toString());
                                            startActivity(i);
                                        } else {
                                            Toast.makeText(getApplicationContext(), "Sign-up done.", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                            }

                            @Override
                            public void onError(Exception e) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.e("Auth", "Sign-up error", e);
                                        Toast.makeText(getApplicationContext(), "User already exists", Toast.LENGTH_SHORT).show();
                                    }
                                });

                            }
                        });

            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Password = findViewById(R.id.passwordTextBox);
        Email = findViewById(R.id.emailTextBox);
        Register = findViewById(R.id.registerbutton);
        Register.setOnClickListener(registerlistener);
        loadingDialog = new LoadingDialog(MainActivity.this);


        email = (TextView) findViewById(R.id.email);
        password = (TextView) findViewById(R.id.password);
        path_2 = (ImageView) findViewById(R.id.path_2);
        Register = (FloatingActionButton) findViewById(R.id.registerbutton);
        Register.setOnClickListener(registerlistener);
        path_2.setOnClickListener(loginListener);


        AWSMobileClient.getInstance().initialize(getApplicationContext(), new Callback<UserStateDetails>() {

                    @Override
                    public void onResult(UserStateDetails userStateDetails) {
                        Log.i("INIT", "onResult: " + userStateDetails.getUserState());
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e("INIT", "Initialization error.", e);
                    }
                }
        );

    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}