package com.example.fypapp2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.results.SignUpResult;
import com.amazonaws.mobile.client.results.UserCodeDeliveryDetails;

public class CodeActivity extends AppCompatActivity {

    EditText Code;
    String Username;
    Button proceedButton;


    private View rectangle_4;


    private View.OnClickListener listener1 = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if (Code.getText().toString().length() < 6 || Code.getText().toString().length() > 6) {
                Toast.makeText(getApplicationContext(), "Wrong Code Entered", Toast.LENGTH_LONG).show();
            } else {
                AWSMobileClient.getInstance().confirmSignUp(
                        Username,
                        Code.getText().toString(),
                        new Callback<SignUpResult>() {
                            @Override
                            public void onResult(SignUpResult result) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.d("Auth", "Signup callback state: " + result.getConfirmationState());
                                        if (!result.getConfirmationState()) {
                                            final UserCodeDeliveryDetails details = result.getUserCodeDeliveryDetails();
                                            Toast.makeText(getApplicationContext(), "Please enter sign up code again", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(getApplicationContext(), "Sign up is done", Toast.LENGTH_LONG).show();
                                            Intent i = new Intent(getApplicationContext(), iotactivity.class);
                                            i.putExtra("Email", Username);
                                            startActivity(i);
                                        }
                                    }
                                });
                            }

                            @Override
                            public void onError(Exception e) {
                                Log.e("Auth", "Confirm sign up error", e);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getApplicationContext(), "Wrong Code", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                );

            }
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        AWSMobileClient.getInstance().signOut();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_code);
        proceedButton = findViewById(R.id.proceedButton);
        Code = findViewById(R.id.codeTextBox);
        Username = getIntent().getStringExtra("Username");
        proceedButton.setOnClickListener(listener1);
    }
}