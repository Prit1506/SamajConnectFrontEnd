package com.example.samajconnectfrontend;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
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

public class SignupActivity extends AppCompatActivity implements SamajSuggestionAdapter.OnSamajClickListener {

    private static final String TAG = "SignupActivity";

    // Common Fields
    private Spinner userTypeSpinner; // Hidden spinner for compatibility
    private TextView userTypeDisplayTextView; // Display TextView from XML
    private EditText nameEditText, emailEditText, passwordEditText, confirmPasswordEditText;
    private Button registerButton;
    private LinearLayout dynamicFieldsLayout, adminFieldsLayout, samajSearchLayout;

    // Searchable Samaj Field
    private EditText samajSearchEditText;
    private RecyclerView suggestionsRecyclerView;
    private SamajSuggestionAdapter suggestionAdapter;

    // Admin Fields
    private EditText samajDescriptionEditText, samajRulesEditText, establishedDateEditText;

    private RequestQueue requestQueue;
    private List<JSONObject> availableSamajs = new ArrayList<>();
    private List<JSONObject> filteredSamajs = new ArrayList<>();
    private Calendar selectedDate = Calendar.getInstance();
    private boolean isLoadingSamajs = false;

    // Selected samaj and user type logic
    private JSONObject selectedSamaj = null;
    private boolean isCreatingNewSamaj = false;
    private String currentUserType = "none"; // "admin", "individual", "none"
    private boolean samajSelectionMade = false; // Track if user has made a selection

    // Search delay handler
    private Handler searchHandler = new Handler();
    private Runnable searchRunnable;
    private static final int SEARCH_DELAY = 300; // milliseconds

    private static final String SIGNUP_URL = ApiHelper.getBaseUrl() + "auth/register";
    private static final String CREATE_SAMAJ_URL = ApiHelper.getBaseUrl() + "samaj/create";
    private static final String GET_SAMAJS_URL = ApiHelper.getBaseUrl() + "samaj/all";
    private static final String CHECK_SAMAJ_URL = ApiHelper.getBaseUrl() + "samaj/check/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        initializeViews();
        setupClickListeners();
        setupSearchFunctionality();

        requestQueue = Volley.newRequestQueue(this);

        // Load samajs when activity is created
        loadAvailableSamajs();
    }

    private void initializeViews() {
        // Hidden spinner for compatibility
        userTypeSpinner = findViewById(R.id.spinnerUserType);

        // Display TextView from XML
        userTypeDisplayTextView = findViewById(R.id.textViewUserTypeDisplay);

        nameEditText = findViewById(R.id.editTextText);
        emailEditText = findViewById(R.id.editTextText2);
        passwordEditText = findViewById(R.id.editTextText1);
        confirmPasswordEditText = findViewById(R.id.editTextText3);
        registerButton = findViewById(R.id.button3);

        dynamicFieldsLayout = findViewById(R.id.layoutDynamicFields);
        adminFieldsLayout = findViewById(R.id.layoutAdminFields);
        samajSearchLayout = findViewById(R.id.layoutSamajSearch);

        // Searchable samaj field
        samajSearchEditText = findViewById(R.id.editTextSamajSearch);
        suggestionsRecyclerView = findViewById(R.id.recyclerViewSamajSuggestions);

        // Admin fields
        samajDescriptionEditText = findViewById(R.id.editTextSamajDescription);
        samajRulesEditText = findViewById(R.id.editTextSamajRules);
        establishedDateEditText = findViewById(R.id.editTextEstablishedDate);

        // Setup RecyclerView
        suggestionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        suggestionAdapter = new SamajSuggestionAdapter(filteredSamajs, this);
        suggestionsRecyclerView.setAdapter(suggestionAdapter);

        // Initially hide all dynamic elements
        resetToInitialState();
    }

    private void resetToInitialState() {
        samajSearchLayout.setVisibility(View.VISIBLE);
        dynamicFieldsLayout.setVisibility(View.GONE);
        adminFieldsLayout.setVisibility(View.GONE);
        suggestionsRecyclerView.setVisibility(View.GONE);

        userTypeDisplayTextView.setText("Select a Samaj first");
        userTypeDisplayTextView.setTextColor(getResources().getColor(android.R.color.darker_gray));

        registerButton.setText("SIGN UP");

        currentUserType = "none";
        selectedSamaj = null;
        isCreatingNewSamaj = false;
        samajSelectionMade = false;
        samajSearchEditText.setText("");
    }

    private void setupClickListeners() {
        registerButton.setOnClickListener(v -> attemptRegistration());
        establishedDateEditText.setOnClickListener(v -> showDatePicker());
    }

    private void setupSearchFunctionality() {
        samajSearchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Cancel previous search
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                String currentText = s.toString().trim();

                // Only reset if user is actually changing the text (not if selection was made)
                if (!samajSelectionMade) {
                    // Reset user type when search text changes
                    if (selectedSamaj != null || !currentUserType.equals("none")) {
                        resetUserTypeSelection();
                    }
                } else {
                    // If selection was made, check if user is changing the selected text
                    try {
                        if (selectedSamaj != null && !currentText.equals(selectedSamaj.getString("name"))) {
                            // User is modifying selected samaj name, reset selection
                            resetUserTypeSelection();
                            samajSelectionMade = false;
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error checking selected samaj name", e);
                    }
                }

                // Create new search runnable only if no selection is made
                if (!samajSelectionMade) {
                    searchRunnable = () -> performSearch(currentText);
                    // Delay the search
                    searchHandler.postDelayed(searchRunnable, SEARCH_DELAY);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Hide suggestions when clicking outside
        samajSearchEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                suggestionsRecyclerView.setVisibility(View.GONE);
            }
        });
    }

    private void resetUserTypeSelection() {
        selectedSamaj = null;
        isCreatingNewSamaj = false;
        currentUserType = "none";
        samajSelectionMade = false;

        userTypeDisplayTextView.setText("Select a Samaj first");
        userTypeDisplayTextView.setTextColor(getResources().getColor(android.R.color.darker_gray));

        dynamicFieldsLayout.setVisibility(View.GONE);
        adminFieldsLayout.setVisibility(View.GONE);

        registerButton.setText("SIGN UP");
    }

    private void performSearch(String query) {
        if (query.isEmpty()) {
            suggestionsRecyclerView.setVisibility(View.GONE);
            return;
        }

        // Search through available samajs
        filteredSamajs.clear();
        String lowerQuery = query.toLowerCase();
        boolean exactMatch = false;

        for (JSONObject samaj : availableSamajs) {
            try {
                String name = samaj.getString("name");
                String lowerName = name.toLowerCase();

                if (lowerName.equals(lowerQuery)) {
                    exactMatch = true;
                    filteredSamajs.add(0, samaj); // Add exact match at the beginning
                } else if (lowerName.contains(lowerQuery)) {
                    filteredSamajs.add(samaj);
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error filtering samajs", e);
            }
        }

        // Show suggestions
        suggestionAdapter.notifyDataSetChanged();
        suggestionsRecyclerView.setVisibility(filteredSamajs.isEmpty() ? View.GONE : View.VISIBLE);

        // Determine if user can create new samaj (no exact match found)
        if (!exactMatch && query.length() >= 2) {
            // Show option to create new samaj by setting creating mode
            // This will be handled when user stops typing and no selection is made
            checkForNewSamajCreation(query);
        }
    }

    private void checkForNewSamajCreation(String query) {
        // This method runs after search delay when no exact match is found
        searchHandler.postDelayed(() -> {
            // Only proceed if no samaj selection has been made and the query is still the same
            if (!samajSelectionMade && selectedSamaj == null && !query.isEmpty() &&
                    samajSearchEditText.getText().toString().trim().equals(query)) {

                // Check if any existing samaj matches exactly
                boolean exactMatch = false;
                for (JSONObject samaj : availableSamajs) {
                    try {
                        if (samaj.getString("name").equalsIgnoreCase(query)) {
                            exactMatch = true;
                            break;
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error checking exact match", e);
                    }
                }

                if (!exactMatch) {
                    // Enable new samaj creation mode
                    setUserTypeAsAdmin(true);
                }
            }
        }, SEARCH_DELAY + 100);
    }

    @Override
    public void onSamajClick(JSONObject samaj) {
        try {
            selectedSamaj = samaj;
            samajSelectionMade = true; // Mark that user has made a selection

            samajSearchEditText.setText(samaj.getString("name"));
            suggestionsRecyclerView.setVisibility(View.GONE);
            samajSearchEditText.clearFocus();

            // Cancel any pending search operations
            if (searchRunnable != null) {
                searchHandler.removeCallbacks(searchRunnable);
            }

            // Set user type as Individual (joining existing samaj)
            setUserTypeAsIndividual();

        } catch (JSONException e) {
            Log.e(TAG, "Error handling samaj click", e);
            Toast.makeText(this, "Error processing selection", Toast.LENGTH_SHORT).show();
        }
    }

    private void setUserTypeAsIndividual() {
        currentUserType = "individual";
        isCreatingNewSamaj = false;

        userTypeDisplayTextView.setText("Individual (Joining Samaj)");
        userTypeDisplayTextView.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));

        // Hide admin fields
        dynamicFieldsLayout.setVisibility(View.GONE);
        adminFieldsLayout.setVisibility(View.GONE);

        registerButton.setText("JOIN SAMAJ & SIGN UP");

        // Set hidden spinner for compatibility
        // Assuming spinner has: [0: Select, 1: Admin, 2: Individual]
        // We'll set it programmatically without triggering the listener
        if (userTypeSpinner.getAdapter() != null) {
            userTypeSpinner.setSelection(2, false);
        }

        Log.d(TAG, "User type set to individual, selectedSamaj: " + (selectedSamaj != null ? "exists" : "null"));
    }

    private void setUserTypeAsAdmin(boolean creatingNew) {
        currentUserType = "admin";
        isCreatingNewSamaj = creatingNew;

        if (creatingNew) {
            userTypeDisplayTextView.setText("Admin (Creating New Samaj)");
            userTypeDisplayTextView.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));

            // Show admin fields for new samaj creation
            dynamicFieldsLayout.setVisibility(View.VISIBLE);
            adminFieldsLayout.setVisibility(View.VISIBLE);

            registerButton.setText("CREATE SAMAJ & SIGN UP");
        } else {
            userTypeDisplayTextView.setText("Admin (Joining Existing Samaj)");
            userTypeDisplayTextView.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));

            // Hide admin fields for existing samaj
            dynamicFieldsLayout.setVisibility(View.GONE);
            adminFieldsLayout.setVisibility(View.GONE);

            registerButton.setText("JOIN AS ADMIN & SIGN UP");
        }

        // Set hidden spinner for compatibility
        if (userTypeSpinner.getAdapter() != null) {
            userTypeSpinner.setSelection(1, false);
        }

        Log.d(TAG, "User type set to admin, creatingNew: " + creatingNew);
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(year, month, dayOfMonth);
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    establishedDateEditText.setText(sdf.format(selectedDate.getTime()));
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );

        // Set max date to today
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void loadAvailableSamajs() {
        if (isLoadingSamajs) return;

        isLoadingSamajs = true;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, GET_SAMAJS_URL, null,
                response -> {
                    isLoadingSamajs = false;
                    availableSamajs.clear();

                    try {
                        JSONArray samajsArray = response.getJSONArray("samajs");

                        for (int i = 0; i < samajsArray.length(); i++) {
                            JSONObject samaj = samajsArray.getJSONObject(i);
                            availableSamajs.add(samaj);
                        }

                        Log.d(TAG, "Loaded " + availableSamajs.size() + " samajs");
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing samajs", e);
                    }
                },
                error -> {
                    isLoadingSamajs = false;
                    Log.e(TAG, "Error loading samajs", error);
                    Toast.makeText(this, "Failed to load samajs", Toast.LENGTH_SHORT).show();
                });

        request.setRetryPolicy(new DefaultRetryPolicy(
                150000, // 150 seconds timeout
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(request);
    }

    private void attemptRegistration() {
        if (!validateInputs()) {
            return;
        }

        if (currentUserType.equals("none")) {
            Toast.makeText(this, "Please select a samaj first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUserType.equals("admin") && isCreatingNewSamaj) {
            // Admin creating new samaj - use the single endpoint
            createSamajWithAdmin();
        } else {
            // Admin joining existing samaj or Individual joining existing samaj
            registerUser();
        }
    }

    private boolean validateInputs() {
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();
        String confirmPassword = confirmPasswordEditText.getText().toString();

        if (name.isEmpty()) {
            nameEditText.setError("Name is required");
            nameEditText.requestFocus();
            return false;
        }

        if (email.isEmpty()) {
            emailEditText.setError("Email is required");
            emailEditText.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Please enter a valid email");
            emailEditText.requestFocus();
            return false;
        }

        if (password.isEmpty()) {
            passwordEditText.setError("Password is required");
            passwordEditText.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters");
            passwordEditText.requestFocus();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordEditText.setError("Passwords do not match");
            confirmPasswordEditText.requestFocus();
            return false;
        }

        // Samaj validation
        String samajName = samajSearchEditText.getText().toString().trim();
        if (samajName.isEmpty()) {
            samajSearchEditText.setError("Please search and select a samaj");
            samajSearchEditText.requestFocus();
            return false;
        }

        // Additional validation for admin creating new samaj
        if (currentUserType.equals("admin") && isCreatingNewSamaj) {
            // Check for exact duplicate (case-insensitive)
            if (isDuplicateSamajName(samajName)) {
                samajSearchEditText.setError("A samaj with this name already exists");
                samajSearchEditText.requestFocus();
                Toast.makeText(this, "Please choose a different samaj name.", Toast.LENGTH_LONG).show();
                return false;
            }

            // Validate other admin fields
            String description = samajDescriptionEditText.getText().toString().trim();
            String rules = samajRulesEditText.getText().toString().trim();
            String establishedDate = establishedDateEditText.getText().toString().trim();

            if (description.isEmpty()) {
                samajDescriptionEditText.setError("Description is required");
                samajDescriptionEditText.requestFocus();
                return false;
            }

            if (rules.isEmpty()) {
                samajRulesEditText.setError("Rules are required");
                samajRulesEditText.requestFocus();
                return false;
            }

            if (establishedDate.isEmpty()) {
                establishedDateEditText.setError("Established date is required");
                establishedDateEditText.requestFocus();
                return false;
            }
        }

        // For individual or admin joining existing samaj, ensure samaj is selected
        if (!isCreatingNewSamaj && selectedSamaj == null) {
            Toast.makeText(this, "Please select an existing samaj from the suggestions", Toast.LENGTH_SHORT).show();
            samajSearchEditText.requestFocus();
            return false;
        }

        // Debug logging
        Log.d(TAG, "Validation passed - currentUserType: " + currentUserType +
                ", isCreatingNewSamaj: " + isCreatingNewSamaj +
                ", selectedSamaj: " + (selectedSamaj != null ? "exists" : "null") +
                ", samajSelectionMade: " + samajSelectionMade);

        return true;
    }

    private boolean isDuplicateSamajName(String newName) {
        String lowerNewName = newName.toLowerCase().trim();

        for (JSONObject samaj : availableSamajs) {
            try {
                String existingName = samaj.getString("name").toLowerCase().trim();
                if (existingName.equals(lowerNewName)) {
                    return true;
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error checking duplicate name", e);
            }
        }
        return false;
    }

    private void createSamajWithAdmin() {
        JSONObject samajData = new JSONObject();
        try {
            // Samaj data
            samajData.put("name", samajSearchEditText.getText().toString().trim());
            samajData.put("description", samajDescriptionEditText.getText().toString().trim());
            samajData.put("rules", samajRulesEditText.getText().toString().trim());
            samajData.put("establishedDate", establishedDateEditText.getText().toString().trim());

            // Admin user data
            samajData.put("adminName", nameEditText.getText().toString().trim());
            samajData.put("adminEmail", emailEditText.getText().toString().trim());
            samajData.put("adminPassword", passwordEditText.getText().toString());

        } catch (JSONException e) {
            Log.e(TAG, "Error creating samaj JSON", e);
            Toast.makeText(this, "Error preparing samaj data", Toast.LENGTH_SHORT).show();
            return;
        }

        registerButton.setEnabled(false);
        registerButton.setText("Creating Samaj...");

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, CREATE_SAMAJ_URL, samajData,
                response -> {
                    try {
                        String message = response.optString("message", "Samaj created successfully!");
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();

                        // Navigate to OTP activity
                        navigateToOtpActivity();

                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing samaj creation response", e);
                        Toast.makeText(this, "Samaj created successfully!", Toast.LENGTH_SHORT).show();

                        // Navigate to OTP activity anyway
                        navigateToOtpActivity();
                    }
                },
                error -> {
                    Log.e(TAG, "Error creating samaj", error);

                    String errorMessage = "Failed to create samaj";
                    try {
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            String errorBody = new String(error.networkResponse.data);
                            JSONObject errorJson = new JSONObject(errorBody);
                            errorMessage = errorJson.optString("error", errorMessage);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing error response", e);
                    }

                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                    resetRegisterButton();
                }) {

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
                150000, // 150 seconds timeout
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(request);
    }

    private void registerUser() {
        JSONObject userData = new JSONObject();
        try {
            userData.put("name", nameEditText.getText().toString().trim());
            userData.put("email", emailEditText.getText().toString().trim());
            userData.put("password", passwordEditText.getText().toString().trim());
            userData.put("isAdmin", currentUserType.equalsIgnoreCase("admin"));

            if (selectedSamaj != null) {
                userData.put("samajId", selectedSamaj.getInt("id"));
                Log.d(TAG, "Registering user with samajId: " + selectedSamaj.getInt("id"));
            } else {
                Log.w(TAG, "selectedSamaj is null during registration");
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error creating user JSON", e);
            Toast.makeText(this, "Error preparing user data", Toast.LENGTH_SHORT).show();
            resetRegisterButton();
            return;
        }

        registerButton.setEnabled(false);
        registerButton.setText("Signing Up...");

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, SIGNUP_URL, userData,
                response -> {
                    try {
                        if(response.getBoolean("success")) {
                            String message = response.optString("message", "Registration successful!");
                            Toast.makeText(this, message, Toast.LENGTH_LONG).show();

                            // Navigate to OTP activity
                            navigateToOtpActivity();
                        }else {
                            resetRegisterButton();
                            Toast.makeText(this, response.getString("message"), Toast.LENGTH_SHORT).show();
                        }


                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing registration response", e);
                        Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();

                        // Navigate to OTP activity anyway
                        navigateToOtpActivity();
                    }
                },
                error -> {
                    Log.e(TAG, "Registration error", error);
                    String errorMessage = "Registration failed";

                    try {
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            String errorBody = new String(error.networkResponse.data);
                            JSONObject errorJson = new JSONObject(errorBody);
                            errorMessage = errorJson.optString("error", errorMessage);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing error response", e);
                    }

                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                    resetRegisterButton();
                }) {

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
                150000, // 150 seconds timeout
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(request);
    }

    private void navigateToOtpActivity() {
        Intent intent = new Intent(SignupActivity.this, OtpActivity.class);

        // Pass user credentials to OTP activity
        intent.putExtra("name", nameEditText.getText().toString().trim());
        intent.putExtra("email", emailEditText.getText().toString().trim());
        intent.putExtra("password", passwordEditText.getText().toString().trim());

        // Set flags to clear the task stack
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);
        finish();
    }

    private void resetRegisterButton() {
        registerButton.setEnabled(true);

        if (currentUserType.equals("admin") && isCreatingNewSamaj) {
            registerButton.setText("CREATE SAMAJ & SIGN UP");
        } else if (currentUserType.equals("admin")) {
            registerButton.setText("JOIN AS ADMIN & SIGN UP");
        } else if (currentUserType.equals("individual")) {
            registerButton.setText("JOIN SAMAJ & SIGN UP");
        } else {
            registerButton.setText("SIGN UP");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (requestQueue != null) {
            requestQueue.cancelAll(TAG);
        }

        // Remove any pending search callbacks
        if (searchHandler != null && searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
    }
}