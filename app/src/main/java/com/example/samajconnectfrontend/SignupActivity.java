package com.example.samajconnectfrontend;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    private EditText nameEditText, emailEditText, passwordEditText, confirmPasswordEditText;
    private Button signupButton;
    private ProgressDialog progressDialog;
    private static final String REGISTER_URL = "http://10.0.2.2:8080/api/auth/register"; // Correct IP for host machine
    private static final String TAG = "SignupActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        nameEditText = findViewById(R.id.editTextText);
        emailEditText = findViewById(R.id.editTextText2);
        passwordEditText = findViewById(R.id.editTextText1);
        confirmPasswordEditText = findViewById(R.id.editTextText3);
        signupButton = findViewById(R.id.button3);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Signing up...");
        progressDialog.setCancelable(false);

        signupButton.setOnClickListener(v -> attemptSignup());
    }

    private void attemptSignup() {
        final String name = nameEditText.getText().toString().trim();
        final String email = emailEditText.getText().toString().trim();
        final String password = passwordEditText.getText().toString().trim();
        final String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        if (name.isEmpty()) {
            nameEditText.setError("Name is required");
            nameEditText.requestFocus();
            return;
        }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Valid email is required");
            emailEditText.requestFocus();
            return;
        }
        if (password.isEmpty() || password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters");
            passwordEditText.requestFocus();
            return;
        }
        if (!password.equals(confirmPassword)) {
            confirmPasswordEditText.setError("Passwords do not match");
            confirmPasswordEditText.requestFocus();
            return;
        }

        progressDialog.show();
        Log.d(TAG, "Requesting URL: " + REGISTER_URL);

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("name", name);
            requestBody.put("email", email);
            requestBody.put("password", password);
            Log.d(TAG, "Request body: " + requestBody.toString());
        } catch (JSONException e) {
            progressDialog.dismiss();
            Log.e(TAG, "Error creating JSON body: " + e.getMessage());
            Toast.makeText(SignupActivity.this, "Error preparing request", Toast.LENGTH_SHORT).show();
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                REGISTER_URL,
                requestBody,
                response -> {
                    progressDialog.dismiss();
                    try {
                        boolean success = response.getBoolean("success");
                        String message = response.getString("message");
                        Log.d(TAG, "Signup response: " + response.toString());
                        Toast.makeText(SignupActivity.this, message, Toast.LENGTH_LONG).show();
                        if (success) {
                            finish();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing response: " + e.getMessage());
                        Toast.makeText(SignupActivity.this, "Unexpected response from server", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    progressDialog.dismiss();
                    handleSignupError(error);
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
                15000, // 15-second timeout
                2,     // Retry twice
                1.0f   // No backoff
        ));

        Volley.newRequestQueue(this).add(request);
    }

    private void handleSignupError(VolleyError error) {
        String responseBody = null;
        int statusCode = error.networkResponse != null ? error.networkResponse.statusCode : -1;
        String errorMessage = "Signup failed. Please try again.";

        try {
            if (error.networkResponse != null && error.networkResponse.data != null) {
                responseBody = new String(error.networkResponse.data, "UTF-8");
                JSONObject json = new JSONObject(responseBody);
                errorMessage = json.getString("message");
                Log.e(TAG, "Error response (HTTP " + statusCode + "): " + responseBody);
            } else {
                Log.e(TAG, "Empty or null response, HTTP status: " + statusCode);
                if (statusCode == 403) {
                    errorMessage = "Access denied. Check server configuration.";
                } else if (statusCode == -1) {
                    errorMessage = "Unable to reach server. Check your network.";
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing response (HTTP " + statusCode + "): " + e.getMessage());
        }

        Toast.makeText(SignupActivity.this, errorMessage, Toast.LENGTH_LONG).show();
    }
}