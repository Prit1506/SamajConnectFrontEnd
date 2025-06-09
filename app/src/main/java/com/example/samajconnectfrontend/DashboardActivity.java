package com.example.samajconnectfrontend;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.samajconnectfrontend.adapters.EventSliderAdapter;
import com.example.samajconnectfrontend.models.Event;
import com.example.samajconnectfrontend.models.EventResponse;
import com.example.samajconnectfrontend.models.Samaj;
import com.example.samajconnectfrontend.models.SamajResponse;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends AppCompatActivity {

    private TextView userNameTextView, samajNameTextView;
    private ImageView profileImageView;
    private LinearLayout eventsLinearLayout;

    private RecyclerView eventsRecyclerView;
    private EventSliderAdapter eventSliderAdapter;
    private List<Event> eventsList;
    private RequestQueue requestQueue;
    private Long currentUserId;
    private LinearLayoutManager layoutManager;

    // Auto-scroll variables
    private Handler autoScrollHandler;
    private Runnable autoScrollRunnable;
    private static final int AUTO_SCROLL_DELAY = 3000; // 3 seconds
    private static final int PAUSE_DELAY_AFTER_USER_SCROLL = 5000; // 5 seconds pause after user interaction
    private int currentPosition = 0;
    private boolean isAutoScrollEnabled = true;

    private static final String USER_URL = "http://10.0.2.2:8080/api/users/";
    private static final String EVENT_URL = "http://10.0.2.2:8080/api/events/";
    private static final String BASE_URL = "http://10.0.2.2:8080/api/";
    private Long currentSamajId = 6L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        eventsLinearLayout = findViewById(R.id.eventsLinearLayout);
        eventsLinearLayout.setOnClickListener(view -> startActivity(new Intent(DashboardActivity.this, EventActivity.class)));

        requestQueue = Volley.newRequestQueue(this);
        initializeViews();
        setupRecyclerView();
        setupAutoScroll();
        loadUserData();
        loadUserDetails();
    }

    private void initializeViews() {
        userNameTextView = findViewById(R.id.textView10);
        samajNameTextView = findViewById(R.id.textView11);
        profileImageView = findViewById(R.id.imageView4);
        eventsRecyclerView = findViewById(R.id.eventsRecyclerView);
    }

    private void setupRecyclerView() {
        eventsList = new ArrayList<>();
        eventSliderAdapter = new EventSliderAdapter(this, eventsList);

        layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        eventsRecyclerView.setLayoutManager(layoutManager);
        eventsRecyclerView.setAdapter(eventSliderAdapter);

        // Add snap behavior for better positioning (each item snaps to center)
        PagerSnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(eventsRecyclerView);

        // Set click listener for events
        eventSliderAdapter.setOnEventClickListener(event -> {
            Toast.makeText(this, "Clicked: " + event.getEventTitle(), Toast.LENGTH_SHORT).show();
        });

        // Add scroll listener to handle user interaction
        eventsRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                // When user starts scrolling, stop auto-scroll
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    stopAutoScroll();
                    Log.d("DashboardActivity", "User started scrolling - auto-scroll paused");
                }
                // When user stops scrolling, restart auto-scroll after delay
                else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    updateCurrentPosition();
                    startAutoScrollWithDelay(PAUSE_DELAY_AFTER_USER_SCROLL);
                    Log.d("DashboardActivity", "User stopped scrolling - auto-scroll will resume in " + PAUSE_DELAY_AFTER_USER_SCROLL + "ms");
                }
            }
        });
    }

    private void setupAutoScroll() {
        autoScrollHandler = new Handler(Looper.getMainLooper());
        autoScrollRunnable = new Runnable() {
            @Override
            public void run() {
                if (eventsList != null && eventsList.size() > 1 && isAutoScrollEnabled) {
                    currentPosition++;

                    // Reset to beginning if we reach the end
                    if (currentPosition >= eventsList.size()) {
                        currentPosition = 0;
                        // For seamless transition when wrapping around
                        eventsRecyclerView.scrollToPosition(currentPosition);
                        Log.d("DashboardActivity", "Auto-scroll wrapped to beginning");
                    } else {
                        // Smooth scroll to next position
                        eventsRecyclerView.smoothScrollToPosition(currentPosition);
                        Log.d("DashboardActivity", "Auto-scroll to position: " + currentPosition);
                    }

                    // Schedule next scroll
                    autoScrollHandler.postDelayed(this, AUTO_SCROLL_DELAY);
                }
            }
        };
    }

    private void startAutoScroll() {
        if (autoScrollHandler != null && autoScrollRunnable != null && isAutoScrollEnabled) {
            stopAutoScroll(); // Stop any existing auto-scroll
            autoScrollHandler.postDelayed(autoScrollRunnable, AUTO_SCROLL_DELAY);
            Log.d("DashboardActivity", "Auto-scroll started");
        }
    }

    private void startAutoScrollWithDelay(long delay) {
        if (autoScrollHandler != null && autoScrollRunnable != null && isAutoScrollEnabled) {
            stopAutoScroll(); // Stop any existing auto-scroll
            autoScrollHandler.postDelayed(autoScrollRunnable, delay);
            Log.d("DashboardActivity", "Auto-scroll scheduled to start in " + delay + "ms");
        }
    }

    private void stopAutoScroll() {
        if (autoScrollHandler != null && autoScrollRunnable != null) {
            autoScrollHandler.removeCallbacks(autoScrollRunnable);
            Log.d("DashboardActivity", "Auto-scroll stopped");
        }
    }

    private void updateCurrentPosition() {
        if (layoutManager != null) {
            int firstVisiblePosition = layoutManager.findFirstCompletelyVisibleItemPosition();
            if (firstVisiblePosition != RecyclerView.NO_POSITION) {
                currentPosition = firstVisiblePosition;
                Log.d("DashboardActivity", "Current position updated to: " + currentPosition);
            }
        }
    }

    // Method to toggle auto-scroll (useful for user preference)
    public void toggleAutoScroll(boolean enabled) {
        isAutoScrollEnabled = enabled;
        if (enabled && eventsList != null && eventsList.size() > 1) {
            startAutoScroll();
        } else {
            stopAutoScroll();
        }
        Log.d("DashboardActivity", "Auto-scroll " + (enabled ? "enabled" : "disabled"));
    }

    private void loadUserData() {
        SharedPreferences sharedPrefs = getSharedPreferences("SamajConnect", MODE_PRIVATE);
        String userName = sharedPrefs.getString("user_name", "User");
        String userEmail = sharedPrefs.getString("user_email", "");
        currentUserId = sharedPrefs.getLong("user_id", -1L);

        userNameTextView.setText(userName);

        Log.d("DashboardActivity", "User: " + userName + ", Email: " + userEmail + ", UserId: " + currentUserId);

        // Debug SharedPreferences
        debugSharedPreferences();
    }

    private void debugSharedPreferences() {
        SharedPreferences sharedPrefs = getSharedPreferences("SamajConnect", MODE_PRIVATE);

        Log.d("DEBUG_PREFS", "=== SharedPreferences Debug ===");
        Log.d("DEBUG_PREFS", "user_name: " + sharedPrefs.getString("user_name", "NOT_FOUND"));
        Log.d("DEBUG_PREFS", "user_email: " + sharedPrefs.getString("user_email", "NOT_FOUND"));
        Log.d("DEBUG_PREFS", "auth_token: " + sharedPrefs.getString("auth_token", "NOT_FOUND"));
        Log.d("DEBUG_PREFS", "user_id: " + sharedPrefs.getLong("user_id", -1));

        // Print all keys and values
        java.util.Map<String, ?> allPrefs = sharedPrefs.getAll();
        for (java.util.Map.Entry<String, ?> entry : allPrefs.entrySet()) {
            Log.d("DEBUG_PREFS", "Key: " + entry.getKey() + ", Value: " + entry.getValue());
        }
        Log.d("DEBUG_PREFS", "=== End SharedPreferences Debug ===");
    }

    private void loadUserDetails() {
        if (currentUserId == null || currentUserId <= 0) {
            Log.w("DashboardActivity", "Invalid user ID: " + currentUserId);
            samajNameTextView.setText("Your Samaj");
            return;
        }

        String url = USER_URL + currentUserId;
        Log.d("DashboardActivity", "Making API call to get user details: " + url);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    Log.d("DashboardActivity", "User details response: " + response.toString());
                    handleUserDetailsResponse(response);
                },
                error -> {
                    Log.e("DashboardActivity", "Error loading user details:");
                    if (error.networkResponse != null) {
                        Log.e("DashboardActivity", "Status Code: " + error.networkResponse.statusCode);
                        Log.e("DashboardActivity", "Response Data: " + new String(error.networkResponse.data));
                    }
                    Log.e("DashboardActivity", "Error Message: " + error.getMessage());
                    handleUserDetailsError(error);
                }
        ) {
            @Override
            public java.util.Map<String, String> getHeaders() {
                java.util.Map<String, String> headers = new java.util.HashMap<>();
                headers.put("Content-Type", "application/json");

                SharedPreferences sharedPrefs = getSharedPreferences("SamajConnect", MODE_PRIVATE);
                String authToken = sharedPrefs.getString("auth_token", "");
                if (!authToken.isEmpty()) {
                    headers.put("Authorization", "Bearer " + authToken);
                }

                return headers;
            }
        };

        requestQueue.add(request);
    }

    private void handleUserDetailsResponse(JSONObject response) {
        try {
            Log.d("DashboardActivity", "Parsing user details response...");

            // Check if response has success field
            if (response.has("success") && response.getBoolean("success")) {
                JSONObject userData = response.getJSONObject("user");
                SharedPreferences sharedPreferences = getSharedPreferences("SamajConnect", MODE_PRIVATE);
                Log.d("DashboardActivity", "isAdmin from user details: " + userData.getBoolean("isAdmin"));
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("is_admin", userData.getBoolean("isAdmin"));
                editor.apply();

                if (userData.has("samaj") && !userData.isNull("samaj")) {
                    JSONObject samajData = userData.getJSONObject("samaj");
                    currentSamajId = samajData.getLong("id");
                    editor.putLong("samaj_id", currentSamajId);
                    editor.apply();
                    Log.d("DashboardActivity", "Found samaj_id from user details: " + currentSamajId);

                    // Now load samaj details and events
                    loadSamajData();
                    loadEvents();
                } else {
                    Log.w("DashboardActivity", "No samaj data found in user details");
                    samajNameTextView.setText("No Samaj Assigned");
                }
            } else {
                // If response doesn't have success field, try direct parsing
                if (response.has("samaj") && !response.isNull("samaj")) {
                    JSONObject samajData = response.getJSONObject("samaj");
                    currentSamajId = samajData.getLong("id");

                    Log.d("DashboardActivity", "Found samaj_id from direct parsing: " + currentSamajId);

                    // Now load samaj details and events
                    loadSamajData();
                    loadEvents();
                } else {
                    Log.w("DashboardActivity", "No samaj data found in response");
                    samajNameTextView.setText("No Samaj Assigned");
                }
            }

        } catch (Exception e) {
            Log.e("DashboardActivity", "Exception while parsing user details response: " + e.getMessage());
            e.printStackTrace();
            samajNameTextView.setText("Error Loading Samaj");
        }
    }

    private void handleUserDetailsError(com.android.volley.VolleyError error) {
        Log.e("DashboardActivity", "Error loading user details: " + error.toString());

        String errorMessage = "Failed to load user details";
        if (error.networkResponse != null) {
            int statusCode = error.networkResponse.statusCode;
            errorMessage += " (Status: " + statusCode + ")";

            if (statusCode == 404) {
                errorMessage = "User not found";
            } else if (statusCode == 401) {
                errorMessage = "Unauthorized - Please login again";
            } else if (statusCode >= 500) {
                errorMessage = "Server error";
            }
        } else {
            errorMessage = "Network error - Check your connection";
        }

        samajNameTextView.setText("Error Loading");
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
    }

    private void loadSamajData() {
        if (currentSamajId == null || currentSamajId <= 0) {
            Log.w("DashboardActivity", "Invalid samaj ID: " + currentSamajId);
            samajNameTextView.setText("Your Samaj");
            return;
        }

        String url = BASE_URL + "samaj/" + currentSamajId;
        Log.d("DashboardActivity", "Making API call to: " + url);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    Log.d("DashboardActivity", "Raw API Response: " + response.toString());
                    handleSamajResponse(response);
                },
                error -> {
                    Log.e("DashboardActivity", "API Error Details:");
                    if (error.networkResponse != null) {
                        Log.e("DashboardActivity", "Status Code: " + error.networkResponse.statusCode);
                        Log.e("DashboardActivity", "Response Data: " + new String(error.networkResponse.data));
                    }
                    Log.e("DashboardActivity", "Error Message: " + error.getMessage());
                    handleSamajError(error);
                }
        ) {
            @Override
            public java.util.Map<String, String> getHeaders() {
                java.util.Map<String, String> headers = new java.util.HashMap<>();
                headers.put("Content-Type", "application/json");

                SharedPreferences sharedPrefs = getSharedPreferences("SamajConnect", MODE_PRIVATE);
                String authToken = sharedPrefs.getString("auth_token", "");
                if (!authToken.isEmpty()) {
                    headers.put("Authorization", "Bearer " + authToken);
                }

                return headers;
            }
        };

        requestQueue.add(request);
    }

    private void handleSamajResponse(JSONObject response) {
        try {
            Log.d("DashboardActivity", "Parsing samaj response...");

            Gson gson = new Gson();
            SamajResponse samajResponse = gson.fromJson(response.toString(), SamajResponse.class);

            Log.d("DashboardActivity", "Parsed response - Success: " + samajResponse.isSuccess());
            Log.d("DashboardActivity", "Parsed response - Message: " + samajResponse.getMessage());

            if (samajResponse.isSuccess() && samajResponse.getSamaj() != null) {
                Samaj samaj = samajResponse.getSamaj();
                String samajName = samaj.getName();

                Log.d("DashboardActivity", "Samaj object: " + samaj.toString());
                Log.d("DashboardActivity", "Samaj name: " + samajName);
                Log.d("DashboardActivity", "Samaj ID: " + samaj.getId());
                Log.d("DashboardActivity", "Samaj description: " + samaj.getDescription());

                if (samajName != null && !samajName.trim().isEmpty()) {
                    samajNameTextView.setText(samajName);
                    Log.d("DashboardActivity", "Successfully set samaj name to TextView: " + samajName);
                } else {
                    samajNameTextView.setText("Your Samaj");
                    Log.w("DashboardActivity", "Samaj name is null or empty");
                }

            } else {
                samajNameTextView.setText("Your Samaj");
                Log.w("DashboardActivity", "API call failed or samaj is null. Success: " +
                        samajResponse.isSuccess() + ", Message: " + samajResponse.getMessage());
            }

        } catch (Exception e) {
            Log.e("DashboardActivity", "Exception while parsing samaj response: " + e.getMessage());
            e.printStackTrace();
            samajNameTextView.setText("Your Samaj");
        }
    }

    private void handleSamajError(com.android.volley.VolleyError error) {
        Log.e("DashboardActivity", "Error loading samaj: " + error.toString());

        String errorMessage = "Failed to load samaj information";
        if (error.networkResponse != null) {
            int statusCode = error.networkResponse.statusCode;
            errorMessage += " (Status: " + statusCode + ")";

            if (statusCode == 404) {
                errorMessage = "Samaj not found";
            } else if (statusCode == 401) {
                errorMessage = "Unauthorized - Please login again";
            } else if (statusCode >= 500) {
                errorMessage = "Server error";
            }
        } else {
            errorMessage = "Network error - Check your connection";
        }

        samajNameTextView.setText("Your Samaj");

        // Show error message to user for debugging
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
    }

    private void loadEvents() {
        String url = EVENT_URL + "samaj/" + currentSamajId + "/upcoming";
        Log.d("DashboardActivity", "Making API call to: " + url);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> handleEventsResponse(response),
                error -> handleEventsError(error)
        ) {
            @Override
            public java.util.Map<String, String> getHeaders() {
                java.util.Map<String, String> headers = new java.util.HashMap<>();
                headers.put("Content-Type", "application/json");

                SharedPreferences sharedPrefs = getSharedPreferences("SamajConnect", MODE_PRIVATE);
                String authToken = sharedPrefs.getString("auth_token", "");
                if (!authToken.isEmpty()) {
                    headers.put("Authorization", "Bearer " + authToken);
                }

                return headers;
            }
        };
        request.setRetryPolicy(new DefaultRetryPolicy(150000, 1, 1.0f));
        requestQueue.add(request);
    }

    private void handleEventsResponse(JSONObject response) {
        try {
            Log.d("DashboardActivity", "Parsing events response..." + response.toString());
            Gson gson = new Gson();
            EventResponse eventResponse = gson.fromJson(response.toString(), EventResponse.class);

            if (eventResponse.isSuccess() && eventResponse.getEvents() != null) {
                eventsList.clear();
                eventsList.addAll(eventResponse.getEvents());
                eventSliderAdapter.updateEvents(eventsList);

                Log.d("DashboardActivity", "Loaded " + eventsList.size() + " events");

                // Start auto-scroll only if we have multiple events
                if (eventsList.size() > 1) {
                    currentPosition = 0; // Reset position
                    startAutoScroll();
                    Log.d("DashboardActivity", "Auto-scroll started for " + eventsList.size() + " events");
                } else if (eventsList.size() == 1) {
                    Log.d("DashboardActivity", "Only 1 event found - auto-scroll not needed");
                } else {
                    Log.d("DashboardActivity", "No events found - auto-scroll not started");
                }
            } else {
                Toast.makeText(this, "No events found", Toast.LENGTH_SHORT).show();
                Log.d("DashboardActivity", "No events found in response");
            }

        } catch (Exception e) {
            Log.e("DashboardActivity", "Error parsing events response", e);
            Toast.makeText(this, "Error loading events", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleEventsError(com.android.volley.VolleyError error) {
        Log.e("DashboardActivity", "Error loading events: " + error.toString());

        String errorMessage = "Failed to load events";
        if (error.networkResponse != null) {
            int statusCode = error.networkResponse.statusCode;
            if (statusCode == 401) {
                errorMessage = "Unauthorized - Please login again";
            } else if (statusCode >= 500) {
                errorMessage = "Server error - Please try again later";
            }
        } else {
            errorMessage = "Network error - Check your connection";
        }

        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Resume auto-scroll when activity comes back to foreground
        if (eventsList != null && eventsList.size() > 1 && isAutoScrollEnabled) {
            startAutoScroll();
            Log.d("DashboardActivity", "Activity resumed - auto-scroll restarted");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop auto-scroll when activity goes to background
        stopAutoScroll();
        Log.d("DashboardActivity", "Activity paused - auto-scroll stopped");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up handler to prevent memory leaks
        stopAutoScroll();
        if (autoScrollHandler != null) {
            autoScrollHandler.removeCallbacksAndMessages(null);
            autoScrollHandler = null;
        }
        autoScrollRunnable = null;
        Log.d("DashboardActivity", "Activity destroyed - auto-scroll cleaned up");
    }
}