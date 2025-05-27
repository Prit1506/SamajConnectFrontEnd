package com.example.samajconnectfrontend;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class ForgotPasswordActivity extends AppCompatActivity {

    EditText emailInput, otpInput, newPasswordInput;
    Button actionButton;

    private static final String FORGOT_PASSWORD_URL = "http://10.0.2.2:8080/api/auth/forgot-password";
    private static final String RESET_PASSWORD_URL = "http://10.0.2.2:8080/api/auth/reset-password";

    RequestQueue requestQueue;
    boolean otpSent = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        emailInput = findViewById(R.id.editTextText);
        otpInput = findViewById(R.id.editTextOTP);
        newPasswordInput = findViewById(R.id.editTextNewPassword);
        actionButton = findViewById(R.id.button3);

        requestQueue = Volley.newRequestQueue(this);

        // Disable OTP and password initially
        otpInput.setEnabled(false);
        newPasswordInput.setEnabled(false);

        actionButton.setText("Send OTP");

        actionButton.setOnClickListener(v -> {
            if (!otpSent) {
                sendOtpRequest();
            } else {
                sendResetPasswordRequest();
            }
        });
    }

    private void sendOtpRequest() {
        String email = emailInput.getText().toString().trim();

        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
            return;
        }

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("email", email);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                FORGOT_PASSWORD_URL,
                requestBody,
                response -> {
                    Toast.makeText(this, "OTP sent successfully!", Toast.LENGTH_SHORT).show();

                    // Enable fields and change button behavior
                    otpSent = true;
                    otpInput.setEnabled(true);
                    newPasswordInput.setEnabled(true);
                    actionButton.setText("Reset Password");
                },
                error -> {
                    Toast.makeText(this, "Failed to send OTP. Try again." + error.toString(), Toast.LENGTH_SHORT).show();
                }
        );

        request.setRetryPolicy(new DefaultRetryPolicy(150000, 2, 1.0f));
        requestQueue.add(request);
    }

    private void sendResetPasswordRequest() {
        String email = emailInput.getText().toString().trim();
        String otp = otpInput.getText().toString().trim();
        String newPassword = newPasswordInput.getText().toString().trim();

        if (otp.isEmpty() || newPassword.isEmpty()) {
            Toast.makeText(this, "Enter OTP and new password", Toast.LENGTH_SHORT).show();
            return;
        }

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("email", email);
            requestBody.put("otp", otp);
            requestBody.put("newPassword", newPassword);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                RESET_PASSWORD_URL,
                requestBody,
                response -> {
                    Toast.makeText(this, "Password reset successful!", Toast.LENGTH_SHORT).show();
                    finish(); // or redirect to login page
                },
                error -> {
                    Toast.makeText(this, "Failed to reset password", Toast.LENGTH_SHORT).show();
                }
        );

        requestQueue.add(request);
    }
}
