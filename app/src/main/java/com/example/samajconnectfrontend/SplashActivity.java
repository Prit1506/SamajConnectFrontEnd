package com.example.samajconnectfrontend;

import android.os.Bundle;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        new Handler().postDelayed(() -> {
            checkLoginStatusAndNavigate();
        }, SPLASH_DURATION);
    }

    private void checkLoginStatusAndNavigate() {
        SharedPreferences sharedPrefs = getSharedPreferences("SamajConnect", MODE_PRIVATE);
        boolean isLoggedIn = sharedPrefs.getBoolean("is_logged_in", false);
        String authToken = sharedPrefs.getString("auth_token", "");

        Intent intent;

        if (isLoggedIn && !authToken.isEmpty() && !isTokenExpired()) {
            // User is logged in and token is valid
            intent = new Intent(this, DashboardActivity.class);
        } else {
            // User needs to login or token expired
            if (isTokenExpired()) {
                clearExpiredLoginData();
            }
            intent = new Intent(this, IntroActivity.class);
        }

        startActivity(intent);
        finish();
    }

    private boolean isTokenExpired() {
        SharedPreferences sharedPrefs = getSharedPreferences("SamajConnect", MODE_PRIVATE);
        long expiryTime = sharedPrefs.getLong("token_expiry", 0);
        return System.currentTimeMillis() > expiryTime;
    }

    private void clearExpiredLoginData() {
        SharedPreferences sharedPrefs = getSharedPreferences("SamajConnect", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.clear();
        editor.apply();
    }
}