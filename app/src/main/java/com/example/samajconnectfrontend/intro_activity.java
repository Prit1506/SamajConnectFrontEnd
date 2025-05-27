package com.example.samajconnectfrontend;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class intro_activity extends AppCompatActivity {
    private static final String TAG = "IntroActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        Button loginButton = findViewById(R.id.button);
        Button signupButton = findViewById(R.id.button2);

        loginButton.setOnClickListener(v -> {
            Log.d(TAG, "Login button clicked");
            try {
                startActivity(new Intent(intro_activity.this, login_activity.class));
            } catch (Exception e) {
                Log.e(TAG, "Error navigating to LoginActivity", e);
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        signupButton.setOnClickListener(v -> {
            Log.d(TAG, "Sign Up button clicked");
            try {
                startActivity(new Intent(intro_activity.this, signup.class));
            } catch (Exception e) {
                Log.e(TAG, "Error navigating to SignupActivity", e);
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
