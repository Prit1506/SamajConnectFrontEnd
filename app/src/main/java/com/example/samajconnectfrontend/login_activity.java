package com.example.samajconnectfrontend;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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

public class login_activity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private Button loginButton;
    private TextView forgotPasswordTextView;
    private RequestQueue requestQueue;

    private static final String BASE_URL = "http://10.0.2.2:8080/api/auth/";
    private static final String LOGIN_URL = BASE_URL + "login";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();
        setupClickListeners();

        requestQueue = Volley.newRequestQueue(this);
    }

    private void initializeViews() {
        emailEditText = findViewById(R.id.editTextText);
        passwordEditText = findViewById(R.id.editTextText1);
        loginButton = findViewById(R.id.button3);
        forgotPasswordTextView = findViewById(R.id.textView6);
    }

    private void setupClickListeners() {
        loginButton.setOnClickListener(v -> attemptLogin());

        forgotPasswordTextView.setOnClickListener(v -> {
            Intent intent = new Intent(login_activity.this, forgotpassword.class);
            startActivity(intent);
        });
    }

    private void attemptLogin() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty()) {
            emailEditText.setError("Email is required");
            emailEditText.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Please enter a valid email");
            emailEditText.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            passwordEditText.setError("Password is required");
            passwordEditText.requestFocus();
            return;
        }

        if (password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters");
            passwordEditText.requestFocus();
            return;
        }

        loginButton.setEnabled(false);
        loginButton.setText("Logging in...");
        performLogin(email, password);
    }

    private void performLogin(String email, String password) {
        LoginRequest loginRequest = new LoginRequest(email, password);

        JSONObject loginData = new JSONObject();
        try {
            loginData.put("email", loginRequest.getEmail());
            loginData.put("password", loginRequest.getPassword());
        } catch (JSONException e) {
            e.printStackTrace();
            resetLoginButton();
            Toast.makeText(this, "Error creating login data", Toast.LENGTH_SHORT).show();
            return;
        }

        JsonObjectRequest loginRequestObj = new JsonObjectRequest(
                Request.Method.POST,
                LOGIN_URL,
                loginData,
                response -> handleLoginResponse(response),
                error -> handleLoginError(error)
        ) {
            @Override
            public java.util.Map<String, String> getHeaders() {
                java.util.Map<String, String> headers = new java.util.HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        requestQueue.add(loginRequestObj);
    }

    private void handleLoginResponse(JSONObject response) {
        try {
            boolean success = response.getBoolean("success");
            String message = response.getString("message");

            if (success) {
                String token = response.getString("token");
                JSONObject userObject = response.getJSONObject("user");

                saveUserData(token, userObject);
                Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(login_activity.this, MainActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error parsing response", Toast.LENGTH_SHORT).show();
        }

        resetLoginButton();
    }

    private void handleLoginError(com.android.volley.VolleyError error) {
        Log.e("LoginError", "Error: " + error.toString());

        String errorMessage = "Login failed. Please try again.";

        if (error.networkResponse != null) {
            int statusCode = error.networkResponse.statusCode;
            if (statusCode == 401) {
                errorMessage = "Invalid email or password";
            } else if (statusCode >= 500) {
                errorMessage = "Server error. Please try again later.";
            }
        } else {
            errorMessage = "Network error. Please check your connection.";
        }

        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        resetLoginButton();
    }

    private void saveUserData(String token, JSONObject userObject) {
        SharedPreferences sharedPrefs = getSharedPreferences("SamajConnect", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();

        try {
            editor.putString("auth_token", token);
            editor.putLong("user_id", userObject.getLong("id"));
            editor.putString("user_email", userObject.getString("email"));
            editor.putString("user_name", userObject.optString("name", ""));
            editor.putBoolean("is_logged_in", true);
            editor.apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void resetLoginButton() {
        loginButton.setEnabled(true);
        loginButton.setText("Login");
    }
}
