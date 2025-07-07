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
    boolean requestInProgress = false; // Track if a request is currently being processed

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

        // Prevent multiple requests
        if (requestInProgress) {
            Toast.makeText(this, "Please wait, sending OTP...", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable button and show loading state
        requestInProgress = true;
        actionButton.setEnabled(false);
        actionButton.setText("Sending OTP...");

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
                    // Reset request state
                    requestInProgress = false;
                    actionButton.setEnabled(true);

                    Toast.makeText(this, "OTP sent successfully to your email! Please check your inbox.", Toast.LENGTH_LONG).show();

                    // Enable fields and change button behavior
                    otpSent = true;
                    emailInput.setEnabled(false); // Disable email input to prevent changes
                    otpInput.setEnabled(true);
                    newPasswordInput.setEnabled(true);
                    actionButton.setText("Reset Password");
                },
                error -> {
                    // Reset request state on error
                    requestInProgress = false;
                    actionButton.setEnabled(true);
                    actionButton.setText("Send OTP");

                    Toast.makeText(this, "Failed to send OTP. Please try again.", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "Please enter both OTP and new password", Toast.LENGTH_SHORT).show();
            return;
        }

        // Prevent multiple requests
        if (requestInProgress) {
            Toast.makeText(this, "Please wait, resetting password...", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable button and show loading state
        requestInProgress = true;
        actionButton.setEnabled(false);
        actionButton.setText("Resetting Password...");

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
                    Toast.makeText(this, "Password reset successful! You can now login with your new password.", Toast.LENGTH_LONG).show();
                    finish(); // or redirect to login page
                },
                error -> {
                    // Reset request state on error
                    requestInProgress = false;
                    actionButton.setEnabled(true);
                    actionButton.setText("Reset Password");

                    Toast.makeText(this, "Failed to reset password. Please check your OTP and try again.", Toast.LENGTH_SHORT).show();
                }
        );

        requestQueue.add(request);
    }
}