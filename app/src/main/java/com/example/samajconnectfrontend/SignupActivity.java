package com.example.samajconnectfrontend;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
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

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    private static final String TAG = "SignupActivity";

    private EditText nameEditText, emailEditText, passwordEditText, confirmPasswordEditText;
    private Button registerButton;
    private RequestQueue requestQueue;

    private static final String SIGNUP_URL = ApiHelper.getBaseUrl() + "auth/register";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        initializeViews();
        setupClickListeners();

        requestQueue = Volley.newRequestQueue(this);
    }

    private void initializeViews() {
        nameEditText = findViewById(R.id.editTextText);
        emailEditText = findViewById(R.id.editTextText2);
        passwordEditText = findViewById(R.id.editTextText1);
        confirmPasswordEditText = findViewById(R.id.editTextText3);
        registerButton = findViewById(R.id.button3);
    }

    private void setupClickListeners() {
        registerButton.setOnClickListener(v -> attemptRegistration());
    }

    private void attemptRegistration() {
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();
        String confirmPassword = confirmPasswordEditText.getText().toString();

        clearErrors();

        if (!validateInput(name, email, password, confirmPassword)) {
            return;
        }

        setLoadingState(true);
        sendSignupRequest(name, email, password);
    }

    private void clearErrors() {
        nameEditText.setError(null);
        emailEditText.setError(null);
        passwordEditText.setError(null);
        confirmPasswordEditText.setError(null);
    }

    private boolean validateInput(String name, String email, String password, String confirmPassword) {
        if (name.isEmpty()) {
            nameEditText.setError("Name is required");
            return false;
        }

        if (name.length() < 2 || !name.matches("^[a-zA-Z\\s]+$")) {
            nameEditText.setError("Invalid name format");
            return false;
        }

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Invalid email");
            return false;
        }

        if (password.isEmpty() || password.length() < 6 || !isValidPassword(password)) {
            passwordEditText.setError("Password must be at least 6 characters with a letter and number");
            return false;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordEditText.setError("Passwords do not match");
            return false;
        }

        return true;
    }

    private boolean isValidPassword(String password) {
        return password.matches(".*[a-zA-Z].*") && password.matches(".*\\d.*");
    }

    private void setLoadingState(boolean isLoading) {
        registerButton.setEnabled(!isLoading);
        registerButton.setText(isLoading ? "Registering..." : "SIGN UP");

        nameEditText.setEnabled(!isLoading);
        emailEditText.setEnabled(!isLoading);
        passwordEditText.setEnabled(!isLoading);
        confirmPasswordEditText.setEnabled(!isLoading);
    }

    private void sendSignupRequest(String name, String email, String password) {
        JSONObject data = new JSONObject();
        try {
            data.put("name", name);
            data.put("email", email);
            data.put("password", password);
        } catch (JSONException e) {
            Toast.makeText(this, "Error creating JSON data", Toast.LENGTH_SHORT).show();
            setLoadingState(false);
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                SIGNUP_URL,
                data,
                response -> handleSuccessResponse(response, name, email, password),
                this::handleErrorResponse
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(150000, 0, 1.0f));
        requestQueue.add(request);

    }

    private void handleSuccessResponse(JSONObject response, String name, String email, String password) {
        try {
            boolean success = response.getBoolean("success");
            String message = response.getString("message");

            if (success) {
                Toast.makeText(this, "Registered successfully!", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this, OtpActivity.class);
                intent.putExtra("name", name);
                intent.putExtra("email", email);
                intent.putExtra("password", password);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                if (message.toLowerCase().contains("email") && message.toLowerCase().contains("exist")) {
                    emailEditText.setError("Email already registered");
                }
            }
        } catch (JSONException e) {
            Toast.makeText(this, "Error parsing response", Toast.LENGTH_SHORT).show();
        }

        setLoadingState(false);
    }

    private void handleErrorResponse(com.android.volley.VolleyError error) {
        String errorMessage = "Registration failed. Please try again.";

        if (error.networkResponse != null) {
            int statusCode = error.networkResponse.statusCode;
            if (statusCode == 409) {
                errorMessage = "Email already registered";
                emailEditText.setError(errorMessage);
            } else if (statusCode == 500) {
                errorMessage = "Server error. Please try later.";
            }
        }

        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        setLoadingState(false);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (requestQueue != null) {
            requestQueue.cancelAll(TAG);
        }
    }
}
