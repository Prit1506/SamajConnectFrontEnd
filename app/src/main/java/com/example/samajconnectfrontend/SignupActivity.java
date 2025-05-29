package com.example.samajconnectfrontend;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    private static final String TAG = "SignupActivity";

    // Common Fields
    private Spinner userTypeSpinner;
    private EditText nameEditText, emailEditText, passwordEditText, confirmPasswordEditText;
    private Button registerButton;
    private LinearLayout dynamicFieldsLayout, adminFieldsLayout, userFieldsLayout;

    // Admin Fields
    private EditText samajNameEditText, samajDescriptionEditText, samajRulesEditText, establishedDateEditText;

    // User Fields
    private Spinner selectSamajSpinner;

    private RequestQueue requestQueue;
    private List<JSONObject> availableSamajs = new ArrayList<>();
    private Calendar selectedDate = Calendar.getInstance();

    private static final String SIGNUP_URL = ApiHelper.getBaseUrl() + "auth/register";
    private static final String CREATE_SAMAJ_URL = ApiHelper.getBaseUrl() + "samaj/create";
    private static final String GET_SAMAJS_URL = ApiHelper.getBaseUrl() + "samaj/all";
    private static final String CHECK_SAMAJ_URL = ApiHelper.getBaseUrl() + "samaj/check/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        initializeViews();
        setupSpinners();
        setupClickListeners();
        loadAvailableSamajs();

        requestQueue = Volley.newRequestQueue(this);
    }

    private void initializeViews() {
        userTypeSpinner = findViewById(R.id.spinnerUserType);
        nameEditText = findViewById(R.id.editTextText);
        emailEditText = findViewById(R.id.editTextText2);
        passwordEditText = findViewById(R.id.editTextText1);
        confirmPasswordEditText = findViewById(R.id.editTextText3);
        registerButton = findViewById(R.id.button3);

        dynamicFieldsLayout = findViewById(R.id.layoutDynamicFields);
        adminFieldsLayout = findViewById(R.id.layoutAdminFields);
        userFieldsLayout = findViewById(R.id.layoutUserFields);

        // Admin fields
        samajNameEditText = findViewById(R.id.editTextSamajName);
        samajDescriptionEditText = findViewById(R.id.editTextSamajDescription);
        samajRulesEditText = findViewById(R.id.editTextSamajRules);
        establishedDateEditText = findViewById(R.id.editTextEstablishedDate);

        // User fields
        selectSamajSpinner = findViewById(R.id.spinnerSelectSamaj);
    }

    private void setupSpinners() {
        // User Type Spinner
        String[] userTypes = {"Select User Type", "Admin", "Individual"};
        ArrayAdapter<String> userTypeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, userTypes);
        userTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        userTypeSpinner.setAdapter(userTypeAdapter);

        userTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                handleUserTypeSelection(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void handleUserTypeSelection(int position) {
        dynamicFieldsLayout.setVisibility(View.GONE);
        adminFieldsLayout.setVisibility(View.GONE);
        userFieldsLayout.setVisibility(View.GONE);

        switch (position) {
            case 1: // Admin
                dynamicFieldsLayout.setVisibility(View.VISIBLE);
                adminFieldsLayout.setVisibility(View.VISIBLE);
                registerButton.setText("CREATE SAMAJ & SIGN UP");
                break;
            case 2: // Individual
                dynamicFieldsLayout.setVisibility(View.VISIBLE);
                userFieldsLayout.setVisibility(View.VISIBLE);
                registerButton.setText("JOIN SAMAJ & SIGN UP");
                loadAvailableSamajs();
                break;
            default: // Select User Type
                registerButton.setText("SIGN UP");
                break;
        }
    }

    private void setupClickListeners() {
        registerButton.setOnClickListener(v -> attemptRegistration());

        establishedDateEditText.setOnClickListener(v -> showDatePicker());
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(year, month, dayOfMonth);
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    establishedDateEditText.setText(dateFormat.format(selectedDate.getTime()));
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void loadAvailableSamajs() {
        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                GET_SAMAJS_URL,
                null,
                response -> {
                    availableSamajs.clear();
                    List<String> samajNames = new ArrayList<>();
                    samajNames.add("Select Samaj");

                    try {
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject samaj = response.getJSONObject(i);
                            availableSamajs.add(samaj);
                            samajNames.add(samaj.getString("name"));
                        }

                        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                                android.R.layout.simple_spinner_item, samajNames);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        selectSamajSpinner.setAdapter(adapter);

                    } catch (JSONException e) {
                        Toast.makeText(this, "Error loading samajs", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Failed to load samajs", Toast.LENGTH_SHORT).show()
        );

        requestQueue.add(request);
    }

    private void attemptRegistration() {
        int userTypePosition = userTypeSpinner.getSelectedItemPosition();

        if (userTypePosition == 0) {
            Toast.makeText(this, "Please select user type", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();
        String confirmPassword = confirmPasswordEditText.getText().toString();

        clearErrors();

        if (!validateCommonInput(name, email, password, confirmPassword)) {
            return;
        }

        if (userTypePosition == 1) { // Admin
            handleAdminRegistration(name, email, password);
        } else { // Individual
            handleUserRegistration(name, email, password);
        }
    }

    private void handleAdminRegistration(String name, String email, String password) {
        String samajName = samajNameEditText.getText().toString().trim();
        String samajDescription = samajDescriptionEditText.getText().toString().trim();
        String samajRules = samajRulesEditText.getText().toString().trim();
        String establishedDate = establishedDateEditText.getText().toString().trim();

        if (!validateAdminInput(samajName, samajDescription, samajRules, establishedDate)) {
            return;
        }

        // Check if samaj already exists
        checkSamajExists(samajName, exists -> {
            if (exists) {
                samajNameEditText.setError("Samaj with this name already exists");
                Toast.makeText(this, "Samaj already exists. Please choose a different name.", Toast.LENGTH_LONG).show();
            } else {
                setLoadingState(true);
                createSamajAndRegisterAdmin(name, email, password, samajName, samajDescription, samajRules, establishedDate);
            }
        });
    }

    private void handleUserRegistration(String name, String email, String password) {
        int selectedSamajPosition = selectSamajSpinner.getSelectedItemPosition();

        if (selectedSamajPosition == 0) {
            Toast.makeText(this, "Please select a samaj to join", Toast.LENGTH_SHORT).show();
            return;
        }

        if (availableSamajs.isEmpty()) {
            Toast.makeText(this, "No samajs available to join. Please wait for an admin to create a samaj.", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            JSONObject selectedSamaj = availableSamajs.get(selectedSamajPosition - 1);
            String samajId = selectedSamaj.getString("_id");

            setLoadingState(true);
            registerUserAndJoinSamaj(name, email, password, samajId);
        } catch (JSONException e) {
            Toast.makeText(this, "Error selecting samaj", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkSamajExists(String samajName, SamajExistsCallback callback) {
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                CHECK_SAMAJ_URL + samajName,
                null,
                response -> {
                    try {
                        boolean exists = response.getBoolean("exists");
                        callback.onResult(exists);
                    } catch (JSONException e) {
                        callback.onResult(false);
                    }
                },
                error -> callback.onResult(false)
        );

        requestQueue.add(request);
    }

    private void createSamajAndRegisterAdmin(String name, String email, String password,
                                             String samajName, String samajDescription,
                                             String samajRules, String establishedDate) {

        JSONObject samajData = new JSONObject();
        try {
            samajData.put("name", samajName);
            samajData.put("description", samajDescription);
            samajData.put("rules", samajRules);
            samajData.put("establishedDate", establishedDate);
            samajData.put("adminName", name);
            samajData.put("adminEmail", email);
            samajData.put("adminPassword", password);
        } catch (JSONException e) {
            Toast.makeText(this, "Error creating samaj data", Toast.LENGTH_SHORT).show();
            setLoadingState(false);
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                CREATE_SAMAJ_URL,
                samajData,
                response -> handleSamajCreationSuccess(response, name, email, password),
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

    private void registerUserAndJoinSamaj(String name, String email, String password, String samajId) {
        JSONObject userData = new JSONObject();
        try {
            userData.put("name", name);
            userData.put("email", email);
            userData.put("password", password);
            userData.put("isAdmin", false);
            userData.put("samajId", samajId);
        } catch (JSONException e) {
            Toast.makeText(this, "Error creating user data", Toast.LENGTH_SHORT).show();
            setLoadingState(false);
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                SIGNUP_URL,
                userData,
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

    private boolean validateCommonInput(String name, String email, String password, String confirmPassword) {
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

    private boolean validateAdminInput(String samajName, String samajDescription, String samajRules, String establishedDate) {
        if (samajName.isEmpty()) {
            samajNameEditText.setError("Samaj name is required");
            return false;
        }

        if (samajName.length() < 3) {
            samajNameEditText.setError("Samaj name must be at least 3 characters");
            return false;
        }

        if (samajDescription.isEmpty()) {
            samajDescriptionEditText.setError("Samaj description is required");
            return false;
        }

        if (samajDescription.length() < 10) {
            samajDescriptionEditText.setError("Description must be at least 10 characters");
            return false;
        }

        if (samajRules.isEmpty()) {
            samajRulesEditText.setError("Samaj rules are required");
            return false;
        }

        if (samajRules.length() < 10) {
            samajRulesEditText.setError("Rules must be at least 10 characters");
            return false;
        }

        if (establishedDate.isEmpty()) {
            establishedDateEditText.setError("Established date is required");
            return false;
        }

        return true;
    }

    private boolean isValidPassword(String password) {
        return password.matches(".*[a-zA-Z].*") && password.matches(".*\\d.*");
    }

    private void clearErrors() {
        nameEditText.setError(null);
        emailEditText.setError(null);
        passwordEditText.setError(null);
        confirmPasswordEditText.setError(null);
        samajNameEditText.setError(null);
        samajDescriptionEditText.setError(null);
        samajRulesEditText.setError(null);
        establishedDateEditText.setError(null);
    }

    private void setLoadingState(boolean isLoading) {
        registerButton.setEnabled(!isLoading);
        userTypeSpinner.setEnabled(!isLoading);
        nameEditText.setEnabled(!isLoading);
        emailEditText.setEnabled(!isLoading);
        passwordEditText.setEnabled(!isLoading);
        confirmPasswordEditText.setEnabled(!isLoading);

        // Admin fields
        samajNameEditText.setEnabled(!isLoading);
        samajDescriptionEditText.setEnabled(!isLoading);
        samajRulesEditText.setEnabled(!isLoading);
        establishedDateEditText.setEnabled(!isLoading);

        // User fields
        selectSamajSpinner.setEnabled(!isLoading);

        if (isLoading) {
            int userTypePosition = userTypeSpinner.getSelectedItemPosition();
            if (userTypePosition == 1) {
                registerButton.setText("Creating Samaj...");
            } else if (userTypePosition == 2) {
                registerButton.setText("Joining Samaj...");
            } else {
                registerButton.setText("Signing Up...");
            }
        } else {
            handleUserTypeSelection(userTypeSpinner.getSelectedItemPosition());
        }
    }

    private void handleSamajCreationSuccess(JSONObject response, String name, String email, String password) {
        try {
            boolean success = response.getBoolean("success");
            String message = response.getString("message");

            if (success) {
                Toast.makeText(this, "Samaj created successfully! Admin registered.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this, OtpActivity.class);
                intent.putExtra("name", name);
                intent.putExtra("email", email);
                intent.putExtra("password", password);
                intent.putExtra("isAdmin", true);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                if (message.toLowerCase().contains("samaj") && message.toLowerCase().contains("exist")) {
                    samajNameEditText.setError("Samaj name already exists");
                } else if (message.toLowerCase().contains("email") && message.toLowerCase().contains("exist")) {
                    emailEditText.setError("Email already registered");
                }
            }
        } catch (JSONException e) {
            Toast.makeText(this, "Error parsing response", Toast.LENGTH_SHORT).show();
        }

        setLoadingState(false);
    }

    private void handleSuccessResponse(JSONObject response, String name, String email, String password) {
        try {
            boolean success = response.getBoolean("success");
            String message = response.getString("message");

            if (success) {
                Toast.makeText(this, "Registration successful!", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this, OtpActivity.class);
                intent.putExtra("name", name);
                intent.putExtra("email", email);
                intent.putExtra("password", password);
                intent.putExtra("isAdmin", false);
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
        String errorMessage = "Operation failed. Please try again.";

        if (error.networkResponse != null) {
            int statusCode = error.networkResponse.statusCode;
            switch (statusCode) {
                case 409:
                    errorMessage = "Email already registered or Samaj already exists";
                    break;
                case 400:
                    errorMessage = "Invalid data provided";
                    break;
                case 500:
                    errorMessage = "Server error. Please try later.";
                    break;
            }
        } else if (error.getMessage() != null && error.getMessage().contains("timeout")) {
            errorMessage = "Request timeout. Please check your connection.";
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

    // Callback interface for samaj existence check
    private interface SamajExistsCallback {
        void onResult(boolean exists);
    }
}