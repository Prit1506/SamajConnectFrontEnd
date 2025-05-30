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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
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
    private Spinner userTypeSpinner;
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

    // Selected samaj for both admin and user
    private JSONObject selectedSamaj = null;
    private boolean isCreatingNewSamaj = false;

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
        setupSpinners();
        setupClickListeners();
        setupSearchFunctionality();

        requestQueue = Volley.newRequestQueue(this);

        // Load samajs when activity is created
        loadAvailableSamajs();
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
        samajSearchLayout.setVisibility(View.GONE);
        suggestionsRecyclerView.setVisibility(View.GONE);

        // Reset selections
        selectedSamaj = null;
        isCreatingNewSamaj = false;
        samajSearchEditText.setText("");

        switch (position) {
            case 1: // Admin
                samajSearchLayout.setVisibility(View.VISIBLE);
                findViewById(R.id.textViewSamajSearch).setVisibility(View.VISIBLE);
                ((android.widget.TextView) findViewById(R.id.textViewSamajSearch)).setText("Create New Samaj");
                samajSearchEditText.setHint("Enter new samaj name...");
                dynamicFieldsLayout.setVisibility(View.VISIBLE);
                adminFieldsLayout.setVisibility(View.VISIBLE);
                registerButton.setText("CREATE SAMAJ & SIGN UP");
                isCreatingNewSamaj = true;

                // Load samajs for duplicate checking
                if (availableSamajs.isEmpty() && !isLoadingSamajs) {
                    loadAvailableSamajs();
                }
                break;

            case 2: // Individual
                samajSearchLayout.setVisibility(View.VISIBLE);
                findViewById(R.id.textViewSamajSearch).setVisibility(View.VISIBLE);
                ((android.widget.TextView) findViewById(R.id.textViewSamajSearch)).setText("Search Samaj to Join");
                samajSearchEditText.setHint("Search for samaj to join...");
                registerButton.setText("JOIN SAMAJ & SIGN UP");
                isCreatingNewSamaj = false;

                // Load samajs for searching
                if (availableSamajs.isEmpty() && !isLoadingSamajs) {
                    loadAvailableSamajs();
                }
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

                // Create new search runnable
                searchRunnable = () -> performSearch(s.toString().trim());

                // Delay the search
                searchHandler.postDelayed(searchRunnable, SEARCH_DELAY);
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

    private void performSearch(String query) {
        if (query.isEmpty()) {
            suggestionsRecyclerView.setVisibility(View.GONE);
            selectedSamaj = null;
            return;
        }

        int userType = userTypeSpinner.getSelectedItemPosition();

        // For Admin creating new samaj - show similar existing names to prevent duplicates
        if (userType == 1) { // Admin
            searchSimilarSamajsForAdmin(query);
            return;
        }

        // For Individual searching existing samajs to join
        if (userType == 2) { // Individual
            filteredSamajs.clear();
            String lowerQuery = query.toLowerCase();

            for (JSONObject samaj : availableSamajs) {
                try {
                    String name = samaj.getString("name").toLowerCase();
                    if (name.contains(lowerQuery)) {
                        filteredSamajs.add(samaj);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error filtering samajs", e);
                }
            }

            suggestionAdapter.notifyDataSetChanged();
            suggestionsRecyclerView.setVisibility(filteredSamajs.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }

    private void searchSimilarSamajsForAdmin(String query) {
        filteredSamajs.clear();
        String lowerQuery = query.toLowerCase().trim();

        // Find similar samaj names (case-insensitive, partial matches)
        for (JSONObject samaj : availableSamajs) {
            try {
                String existingName = samaj.getString("name");
                String lowerExistingName = existingName.toLowerCase().trim();

                // Check for exact match or partial matches
                if (lowerExistingName.equals(lowerQuery) || lowerExistingName.contains(lowerQuery)) {
                    filteredSamajs.add(samaj);
                    // For exact match, show a specific toast
                    if (lowerExistingName.equals(lowerQuery)) {
                        Toast.makeText(this, "A samaj with this exact name already exists.", Toast.LENGTH_SHORT).show();
                    }
                } else if (query.length() >= 3 && calculateSimilarity(lowerQuery, lowerExistingName) > 0.85) {
                    // Only consider similarity for queries of 3+ characters to avoid single-character noise
                    filteredSamajs.add(samaj);
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error checking samaj similarity", e);
            }
        }

        // Show suggestions if any matches found, but no toast for partial matches
        suggestionAdapter.notifyDataSetChanged();
        suggestionsRecyclerView.setVisibility(filteredSamajs.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private double calculateSimilarity(String s1, String s2) {
        if (s1.equals(s2)) return 1.0;

        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) return 1.0;

        return (maxLength - levenshteinDistance(s1, s2)) / (double) maxLength;
    }

    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(Math.min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1]);
                }
            }
        }

        return dp[s1.length()][s2.length()];
    }

    private void checkSamajExists(String samajName) {
        String url = CHECK_SAMAJ_URL + samajName;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        boolean exists = response.getBoolean("exists");
                        if (exists) {
                            // Show existing samaj in suggestions
                            filteredSamajs.clear();
                            filteredSamajs.add(response.getJSONObject("samaj"));
                            suggestionAdapter.notifyDataSetChanged();
                            suggestionsRecyclerView.setVisibility(View.VISIBLE);
                            Toast.makeText(this, "Samaj already exists. You can become an admin.", Toast.LENGTH_SHORT).show();
                        } else {
                            // Hide suggestions for new samaj creation
                            suggestionsRecyclerView.setVisibility(View.GONE);
                            selectedSamaj = null;
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing samaj check response", e);
                    }
                },
                error -> {
                    Log.e(TAG, "Error checking samaj existence", error);
                    // Hide suggestions on error
                    suggestionsRecyclerView.setVisibility(View.GONE);
                });

        request.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(request);
    }

    private void loadAvailableSamajs() {
        if (isLoadingSamajs) return;

        isLoadingSamajs = true;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, GET_SAMAJS_URL, null,
                response -> {
                    isLoadingSamajs = false;
                    availableSamajs.clear();

                    try {
                        // Extract the "samajs" array from the response object
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
                10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(request);
    }

    @Override
    public void onSamajClick(JSONObject samaj) {
        try {
            int userType = userTypeSpinner.getSelectedItemPosition();

            if (userType == 1) { // Admin - show warning about existing samaj
                String existingName = samaj.getString("name");

                // Show dialog asking if they want to use different name
                new AlertDialog.Builder(this)
                        .setTitle("Samaj Already Exists")
                        .setMessage("A samaj named '" + existingName + "' already exists. " +
                                "Please choose a different name to avoid confusion.")
                        .setPositiveButton("Choose Different Name", (dialog, which) -> {
                            samajSearchEditText.setText("");
                            samajSearchEditText.requestFocus();
                        })
                        .setNegativeButton("Keep Current Name", (dialog, which) -> {
                            // Keep current text, user insists on this name
                        })
                        .show();

            } else if (userType == 2) { // Individual - normal selection
                selectedSamaj = samaj;
                samajSearchEditText.setText(samaj.getString("name"));
                suggestionsRecyclerView.setVisibility(View.GONE);
                samajSearchEditText.clearFocus();
            }

            // Hide suggestions after interaction
            suggestionsRecyclerView.setVisibility(View.GONE);

        } catch (JSONException e) {
            Log.e(TAG, "Error handling samaj click", e);
            Toast.makeText(this, "Error processing selection", Toast.LENGTH_SHORT).show();
        }
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

    private void attemptRegistration() {
        if (!validateInputs()) {
            return;
        }

        int userType = userTypeSpinner.getSelectedItemPosition();

        if (userType == 1) { // Admin
            if (selectedSamaj != null) {
                // Admin joining existing samaj
                registerUser();
            } else {
                // Admin creating new samaj
                createSamajAndRegister();
            }
        } else if (userType == 2) { // Individual
            if (selectedSamaj == null) {
                Toast.makeText(this, "Please select a samaj to join", Toast.LENGTH_SHORT).show();
                return;
            }
            registerUser();
        } else {
            Toast.makeText(this, "Please select user type", Toast.LENGTH_SHORT).show();
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

        // Additional validation for admin creating new samaj
        int userType = userTypeSpinner.getSelectedItemPosition();
        if (userType == 1 && selectedSamaj == null) { // Admin creating new samaj
            String samajName = samajSearchEditText.getText().toString().trim();

            if (samajName.isEmpty()) {
                samajSearchEditText.setError("Samaj name is required");
                samajSearchEditText.requestFocus();
                return false;
            }

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

    private void createSamajAndRegister() {
        JSONObject samajData = new JSONObject();
        try {
            samajData.put("name", samajSearchEditText.getText().toString().trim());
            samajData.put("description", samajDescriptionEditText.getText().toString().trim());
            samajData.put("rules", samajRulesEditText.getText().toString().trim());
            samajData.put("established_date", establishedDateEditText.getText().toString().trim());
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
                        selectedSamaj = response.getJSONObject("samaj");
                        registerUser();
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing created samaj", e);
                        Toast.makeText(this, "Error creating samaj", Toast.LENGTH_SHORT).show();
                        resetRegisterButton();
                    }
                },
                error -> {
                    Log.e(TAG, "Error creating samaj", error);
                    Toast.makeText(this, "Failed to create samaj", Toast.LENGTH_SHORT).show();
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
                15000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(request);
    }

    private void registerUser() {
        JSONObject userData = new JSONObject();
        try {
            userData.put("name", nameEditText.getText().toString().trim());
            userData.put("email", emailEditText.getText().toString().trim());
            userData.put("password", passwordEditText.getText().toString());
            userData.put("user_type", userTypeSpinner.getSelectedItemPosition() == 1 ? "admin" : "individual");

            if (selectedSamaj != null) {
                userData.put("samaj_id", selectedSamaj.getInt("id"));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error creating user JSON", e);
            Toast.makeText(this, "Error preparing user data", Toast.LENGTH_SHORT).show();
            resetRegisterButton();
            return;
        }

        if (!registerButton.getText().toString().contains("Creating")) {
            registerButton.setEnabled(false);
            registerButton.setText("Signing Up...");
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, SIGNUP_URL, userData,
                response -> {
                    try {
                        String message = response.getString("message");
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();

                        // Navigate to login
                        Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();

                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing registration response", e);
                        Toast.makeText(this, "Registration successful but response error", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                },
                error -> {
                    Log.e(TAG, "Registration error", error);
                    String errorMessage = "Registration failed";

                    try {
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            String errorBody = new String(error.networkResponse.data);
                            JSONObject errorJson = new JSONObject(errorBody);
                            errorMessage = errorJson.getString("error");
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
                15000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(request);
    }

    private void resetRegisterButton() {
        registerButton.setEnabled(true);
        int userType = userTypeSpinner.getSelectedItemPosition();

        switch (userType) {
            case 1: // Admin
                registerButton.setText("CREATE SAMAJ & SIGN UP");
                break;
            case 2: // Individual
                registerButton.setText("JOIN SAMAJ & SIGN UP");
                break;
            default:
                registerButton.setText("SIGN UP");
                break;
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