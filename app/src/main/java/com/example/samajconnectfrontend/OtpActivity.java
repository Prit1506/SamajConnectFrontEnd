package com.example.samajconnectfrontend;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class OtpActivity extends AppCompatActivity {

    private static final String TAG = "OtpActivity";
    private static final int OTP_TIMER_DURATION = 10 * 60 * 1000; // 10 minutes in milliseconds

    private EditText otpEditText;
    private Button verifyButton;
    private TextView emailDisplayTextView, timerTextView, resendOtpTextView, backToLoginTextView;
    private RequestQueue requestQueue;

    private String userEmail, userName, userPassword;
    private CountDownTimer countDownTimer;
    private boolean canResend = false;

    // API URLs
    private static final String VERIFY_EMAIL_URL = ApiHelper.getBaseUrl() + "auth/verify-email";
    private static final String RESEND_OTP_URL = ApiHelper.getBaseUrl() + "auth/resend-otp";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_otp);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();
        getIntentData();
        setupClickListeners();
        startOtpTimer();
        requestQueue = Volley.newRequestQueue(this);
    }

    private void initializeViews() {
        otpEditText = findViewById(R.id.otpEditText);
        verifyButton = findViewById(R.id.verifyButton);
        emailDisplayTextView = findViewById(R.id.emailDisplayTextView);
        timerTextView = findViewById(R.id.timerTextView);
        resendOtpTextView = findViewById(R.id.resendOtpTextView);
        backToLoginTextView = findViewById(R.id.backToLoginTextView);
    }

    private void getIntentData() {
        Intent intent = getIntent();
        userEmail = intent.getStringExtra("email");
        userName = intent.getStringExtra("name");
        userPassword = intent.getStringExtra("password");

        if (userEmail != null) {
            emailDisplayTextView.setText(userEmail);
        }

        // Validate required data
        if (userEmail == null || userName == null || userPassword == null) {
            Toast.makeText(this, "Missing registration data. Please sign up again.", Toast.LENGTH_LONG).show();
            navigateToSignup();
        }
    }

    private void setupClickListeners() {
        verifyButton.setOnClickListener(v -> attemptOtpVerification());

        resendOtpTextView.setOnClickListener(v -> {
            if (canResend) {
                resendOtp();
            } else {
                Toast.makeText(this, "Please wait before requesting another code", Toast.LENGTH_SHORT).show();
            }
        });

        backToLoginTextView.setOnClickListener(v -> navigateToLogin());
    }

    private void startOtpTimer() {
        canResend = false;
        updateResendTextView();

        countDownTimer = new CountDownTimer(OTP_TIMER_DURATION, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long minutes = millisUntilFinished / 60000;
                long seconds = (millisUntilFinished % 60000) / 1000;
                String timeText = String.format("Code expires in: %02d:%02d", minutes, seconds);
                timerTextView.setText(timeText);
            }

            @Override
            public void onFinish() {
                timerTextView.setText("Code expired");
                canResend = true;
                updateResendTextView();
                Toast.makeText(OtpActivity.this, "OTP has expired. Please request a new code.", Toast.LENGTH_LONG).show();
            }
        };

        countDownTimer.start();
    }

    private void updateResendTextView() {
        if (canResend) {
            resendOtpTextView.setText("Resend Code");
            resendOtpTextView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            resendOtpTextView.setText("Resend (wait...)");
            resendOtpTextView.setTextColor(getResources().getColor(android.R.color.darker_gray));
        }
    }

    private void attemptOtpVerification() {
        String otpCode = otpEditText.getText().toString().trim();

        // Clear previous errors
        otpEditText.setError(null);

        // Validate OTP input
        if (!validateOtpInput(otpCode)) {
            return;
        }

        // Disable button and show loading state
        setLoadingState(true);

        // Send verification request
        sendVerificationRequest(otpCode);
    }

    private boolean validateOtpInput(String otpCode) {
        if (otpCode.isEmpty()) {
            otpEditText.setError("Please enter the verification code");
            otpEditText.requestFocus();
            return false;
        }

        if (otpCode.length() != 6) {
            otpEditText.setError("Verification code must be 6 digits");
            otpEditText.requestFocus();
            return false;
        }

        if (!otpCode.matches("\\d{6}")) {
            otpEditText.setError("Verification code must contain only numbers");
            otpEditText.requestFocus();
            return false;
        }

        return true;
    }

    private void setLoadingState(boolean isLoading) {
        verifyButton.setEnabled(!isLoading);
        verifyButton.setText(isLoading ? "Verifying..." : "VERIFY CODE");
        otpEditText.setEnabled(!isLoading);
        resendOtpTextView.setEnabled(!isLoading);
    }

    private void sendVerificationRequest(String otpCode) {
        JSONObject verificationData = new JSONObject();
        try {
            verificationData.put("email", userEmail);
            verificationData.put("otp", otpCode);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating verification JSON", e);
            setLoadingState(false);
            Toast.makeText(this, "Error creating verification data", Toast.LENGTH_SHORT).show();
            return;
        }

        JsonObjectRequest verificationRequest = new JsonObjectRequest(
                Request.Method.POST,
                VERIFY_EMAIL_URL,
                verificationData,
                this::handleVerificationResponse,
                this::handleVerificationError
        ) {
            @Override
            public java.util.Map<String, String> getHeaders() {
                return ApiHelper.getBasicHeaders();
            }
        };

        requestQueue.add(verificationRequest);
    }

    private void handleVerificationResponse(JSONObject response) {
        try {
            boolean success = response.getBoolean("success");
            String message = response.getString("message");

            if (success) {
                Toast.makeText(this, "Email verified successfully! You can now login.", Toast.LENGTH_LONG).show();
                Log.d(TAG, "Email verification successful for: " + userEmail);

                // Stop timer
                if (countDownTimer != null) {
                    countDownTimer.cancel();
                }

                // Navigate to login activity
                navigateToLogin();

            } else {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                Log.w(TAG, "Email verification failed: " + message);

                // Handle specific error cases
                if (message.toLowerCase().contains("expired")) {
                    canResend = true;
                    updateResendTextView();
                    if (countDownTimer != null) {
                        countDownTimer.cancel();
                    }
                    timerTextView.setText("Code expired");
                }
            }

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing verification response", e);
            Toast.makeText(this, "Error parsing response", Toast.LENGTH_SHORT).show();
        }

        setLoadingState(false);
    }

    private void handleVerificationError(com.android.volley.VolleyError error) {
        Log.e(TAG, "Verification error: " + error.toString());

        String errorMessage = "Verification failed. Please try again.";

        if (error.networkResponse != null) {
            int statusCode = error.networkResponse.statusCode;
            Log.d(TAG, "Error status code: " + statusCode);

            // Try to parse error response
            if (error.networkResponse.data != null) {
                try {
                    String errorJson = new String(error.networkResponse.data, "UTF-8");
                    JSONObject errorObj = new JSONObject(errorJson);

                    if (errorObj.has("message")) {
                        errorMessage = errorObj.getString("message");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing error response", e);
                }
            }

            // Handle specific status codes
            switch (statusCode) {
                case 400:
                    errorMessage = "Invalid verification code. Please check and try again.";
                    break;
                case 404:
                    errorMessage = "User not found. Please sign up again.";
                    break;
                case 410:
                    errorMessage = "Verification code has expired. Please request a new code.";
                    canResend = true;
                    updateResendTextView();
                    break;
                case 500:
                    errorMessage = "Server error. Please try again later.";
                    break;
            }
        } else {
            errorMessage = "Network error. Please check your internet connection.";
        }

        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        setLoadingState(false);
    }

    private void resendOtp() {
        if (!canResend) {
            return;
        }

        // Show loading state for resend
        resendOtpTextView.setText("Sending...");
        resendOtpTextView.setEnabled(false);

        JSONObject resendData = new JSONObject();
        try {
            resendData.put("email", userEmail);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating resend JSON", e);
            updateResendTextView();
            Toast.makeText(this, "Error creating resend request", Toast.LENGTH_SHORT).show();
            return;
        }

        JsonObjectRequest resendRequest = new JsonObjectRequest(
                Request.Method.POST,
                RESEND_OTP_URL,
                resendData,
                this::handleResendResponse,
                this::handleResendError
        ) {
            @Override
            public java.util.Map<String, String> getHeaders() {
                return ApiHelper.getBasicHeaders();
            }
        };

        requestQueue.add(resendRequest);
    }

    private void handleResendResponse(JSONObject response) {
        try {
            boolean success = response.getBoolean("success");
            String message = response.getString("message");

            if (success) {
                Toast.makeText(this, "New verification code sent to your email!", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "OTP resent successfully to: " + userEmail);

                // Clear the current OTP input
                otpEditText.setText("");

                // Restart timer
                if (countDownTimer != null) {
                    countDownTimer.cancel();
                }
                startOtpTimer();

            } else {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                Log.w(TAG, "OTP resend failed: " + message);
            }

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing resend response", e);
            Toast.makeText(this, "Error parsing response", Toast.LENGTH_SHORT).show();
        }

        updateResendTextView();
        resendOtpTextView.setEnabled(true);
    }

    private void handleResendError(com.android.volley.VolleyError error) {
        Log.e(TAG, "Resend error: " + error.toString());

        String errorMessage = "Failed to resend code. Please try again.";

        if (error.networkResponse != null) {
            int statusCode = error.networkResponse.statusCode;

            switch (statusCode) {
                case 404:
                    errorMessage = "User not found. Please sign up again.";
                    break;
                case 409:
                    errorMessage = "Email is already verified. You can login now.";
                    break;
                case 500:
                    errorMessage = "Server error. Please try again later.";
                    break;
            }
        } else {
            errorMessage = "Network error. Please check your internet connection.";
        }

        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        updateResendTextView();
        resendOtpTextView.setEnabled(true);
    }

    private void navigateToLogin() {
        Intent intent = new Intent(OtpActivity.this, login_activity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToSignup() {
        Intent intent = new Intent(OtpActivity.this, signup.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // When user presses back, go to signup
        navigateToSignup();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cancel timer and pending requests to avoid memory leaks
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        if (requestQueue != null) {
            requestQueue.cancelAll(TAG);
        }
    }
}