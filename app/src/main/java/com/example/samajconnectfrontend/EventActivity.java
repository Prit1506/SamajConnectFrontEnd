package com.example.samajconnectfrontend;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.samajconnectfrontend.adapters.EventAdapter;
import com.example.samajconnectfrontend.models.Event;
import com.example.samajconnectfrontend.models.EventResponse;
import com.example.samajconnectfrontend.models.ReactionStats;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventActivity extends AppCompatActivity implements EventAdapter.OnEventActionListener {

    private static final String TAG = "EventActivity";
    private static final String EVENT_URL = "http://10.0.2.2:8080/api/events/";

    // UI Components
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FloatingActionButton fabAdd;
    private LinearLayout emptyStateLayout;
    private EventAdapter eventAdapter;
    private List<Event> eventList;

    // Data and State
    private SharedPreferences sharedPrefs;
    private boolean isAdmin;
    private long currentUserId;
    private long currentSamajId;
    private RequestQueue requestQueue;
    private boolean isFirstLoad = true;
    private boolean isLoadingEvents = false;

    // Handler for UI updates
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            EdgeToEdge.enable(this);
            setContentView(R.layout.activity_event);

            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });

            initializeComponents();
            setupSharedPreferences();

            // Check if we have valid user data before proceeding
            if (!validateUserData()) {
                Log.e(TAG, "Invalid user data, finishing activity");
                finish();
                return;
            }

            setupRecyclerView();
            setupSwipeRefreshLayout();
            setupClickListeners();

            // Show initial loading state
            showInitialLoadingState();
            fetchEvents(false); // false = not a refresh

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error initializing events screen", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializeComponents() {
        try {
            // Initialize UI components
            recyclerView = findViewById(R.id.recview);
            swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
            fabAdd = findViewById(R.id.fadd);
            emptyStateLayout = findViewById(R.id.empty_state);

            if (recyclerView == null || swipeRefreshLayout == null || fabAdd == null || emptyStateLayout == null) {
                throw new RuntimeException("One or more required views not found in layout");
            }

            // Initialize data components
            requestQueue = Volley.newRequestQueue(this);
            eventList = new ArrayList<>();
            mainHandler = new Handler(Looper.getMainLooper());

            Log.d(TAG, "Components initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing components", e);
            throw e;
        }
    }

    private void setupSharedPreferences() {
        sharedPrefs = getSharedPreferences("SamajConnect", MODE_PRIVATE);

        // Get user data
        currentUserId = sharedPrefs.getLong("user_id", -1L);
        currentSamajId = sharedPrefs.getLong("samaj_id", -1L);
        isAdmin = sharedPrefs.getBoolean("is_admin", false);

        Log.d(TAG, "User ID: " + currentUserId + ", Samaj ID: " + currentSamajId + ", Is Admin: " + isAdmin);

        // Show/hide admin controls based on admin status
        if (isAdmin) {
            fabAdd.setVisibility(View.VISIBLE);
        } else {
            fabAdd.setVisibility(View.GONE);
        }
    }

    private boolean validateUserData() {
        if (currentUserId == -1L) {
            Log.e(TAG, "Invalid user ID: " + currentUserId);
            Toast.makeText(this, "User session invalid. Please login again.", Toast.LENGTH_LONG).show();
            return false;
        }

        if (currentSamajId == -1L) {
            Log.e(TAG, "Invalid samaj ID: " + currentSamajId);
            Toast.makeText(this, "No samaj selected. Please select a samaj first.", Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }

    private void setupRecyclerView() {
        try {
            LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            recyclerView.setLayoutManager(layoutManager);

            eventAdapter = new EventAdapter(this, eventList, isAdmin, this);
            recyclerView.setAdapter(eventAdapter);

            // Add scroll listener for debugging
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    Log.d(TAG, "RecyclerView scrolled - items visible: " + layoutManager.getChildCount());
                }
            });

            Log.d(TAG, "RecyclerView setup completed");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up RecyclerView", e);
            throw e;
        }
    }

    private void setupSwipeRefreshLayout() {
        try {
            // Set the color scheme for the refresh indicator
            swipeRefreshLayout.setColorSchemeColors(
                    ContextCompat.getColor(this, R.color.primary_color),
                    ContextCompat.getColor(this, R.color.primary_dark),
                    ContextCompat.getColor(this, R.color.accent_color)
            );

            // Set the background color
            swipeRefreshLayout.setProgressBackgroundColorSchemeColor(
                    ContextCompat.getColor(this, android.R.color.white)
            );

            // Set the refresh listener
            swipeRefreshLayout.setOnRefreshListener(() -> {
                Log.d(TAG, "SwipeRefreshLayout triggered");
                refreshEvents();
            });

            // Set the size of the refresh indicator
            swipeRefreshLayout.setSize(SwipeRefreshLayout.DEFAULT);

            Log.d(TAG, "SwipeRefreshLayout setup completed");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up SwipeRefreshLayout", e);
            // Don't throw here, continue without swipe refresh
            Log.w(TAG, "Continuing without SwipeRefreshLayout functionality");
        }
    }

    private void setupClickListeners() {
        // Back button
        findViewById(R.id.back_arrow).setOnClickListener(v -> {
            Log.d(TAG, "Back button clicked");
            onBackPressed();
        });

        // Floating Action Button - Create Event
        fabAdd.setOnClickListener(v -> {
            Log.d(TAG, "FAB clicked - navigating to CreateEventActivity");
            try {
                Intent intent = new Intent(EventActivity.this, CreateEventActivity.class);
                startActivityForResult(intent, 1001);
            } catch (Exception e) {
                Log.e(TAG, "Error starting CreateEventActivity", e);
                Toast.makeText(this, "Error opening create event screen", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showInitialLoadingState() {
        recyclerView.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.GONE);
        // The SwipeRefreshLayout will show its loading indicator
        Log.d(TAG, "Showing initial loading state");
    }

    private void refreshEvents() {
        Log.d(TAG, "Refreshing events via swipe");
        if (!isLoadingEvents) {
            fetchEvents(true); // true = this is a refresh
        } else {
            // If already loading, just stop the refresh indicator after a delay
            mainHandler.postDelayed(() -> {
                if (swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }, 1000);
        }
    }

    private void fetchEvents(boolean isRefresh) {
        if (currentSamajId == -1L) {
            Log.e(TAG, "Invalid Samaj ID");
            handleFetchEventsComplete(isRefresh);
            showEmptyState();
            return;
        }

        // Prevent multiple simultaneous requests
        if (isLoadingEvents) {
            Log.d(TAG, "Already loading events, skipping request");
            return;
        }

        isLoadingEvents = true;
        String url = EVENT_URL + "samaj/" + currentSamajId;
        Log.d(TAG, "Making API call to: " + url + " (isRefresh: " + isRefresh + ")");

        // Show refresh indicator if this is a refresh
        if (isRefresh && !swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(true);
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    Log.d(TAG, "API response received");
                    handleEventsResponse(response, isRefresh);
                },
                error -> {
                    Log.e(TAG, "API error occurred");
                    handleEventsError(error, isRefresh);
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");

                String authToken = sharedPrefs.getString("auth_token", "");
                if (!authToken.isEmpty()) {
                    headers.put("Authorization", "Bearer " + authToken);
                    Log.d(TAG, "Auth token added to request");
                } else {
                    Log.w(TAG, "No auth token found");
                }

                return headers;
            }
        };

        // Set timeout and retry policy
        request.setRetryPolicy(new DefaultRetryPolicy(
                30000, // 30 seconds timeout
                0,     // No retries to avoid duplicate requests
                1.0f   // Backoff multiplier
        ));

        requestQueue.add(request);
        Log.d(TAG, "Request added to queue");
    }

    private void handleEventsResponse(JSONObject response, boolean isRefresh) {
        try {
            Log.d(TAG, "Raw API response: " + response.toString());

            Gson gson = new Gson();
            EventResponse eventResponse = gson.fromJson(response.toString(), EventResponse.class);

            Log.d(TAG, "EventResponse parsed - Success: " + eventResponse.isSuccess() +
                    ", Count: " + eventResponse.getCount() +
                    ", Events null: " + (eventResponse.getEvents() == null));

            if (eventResponse.isSuccess() && eventResponse.getEvents() != null) {
                List<Event> newEvents = eventResponse.getEvents();
                Log.d(TAG, "Number of events in response: " + newEvents.size());

                // Update the main list
                eventList.clear();
                eventList.addAll(newEvents);

                Log.d(TAG, "EventList updated, size: " + eventList.size());

                // Update UI on main thread
                mainHandler.post(() -> {
                    try {
                        handleFetchEventsComplete(isRefresh);

                        if (eventList.isEmpty()) {
                            Log.d(TAG, "No events to display - showing empty state");
                            showEmptyState();
                        } else {
                            Log.d(TAG, "Displaying " + eventList.size() + " events");
                            hideEmptyState();

                            // Update adapter with new data
                            eventAdapter.updateEvents(new ArrayList<>(eventList));

                            // Show success message for refresh
                            if (isRefresh) {
                                Toast.makeText(EventActivity.this, "Events refreshed successfully", Toast.LENGTH_SHORT).show();
                            }

                            Log.d(TAG, "UI update completed - Adapter item count: " + eventAdapter.getItemCount());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error updating UI after successful response", e);
                        handleFetchEventsComplete(isRefresh);
                        showEmptyState();
                    }
                });

                Log.d(TAG, "Final loaded events count: " + eventList.size());

            } else {
                Log.w(TAG, "No events found or API call failed");
                mainHandler.post(() -> {
                    handleFetchEventsComplete(isRefresh);
                    showEmptyState();
                    if (isRefresh) {
                        Toast.makeText(EventActivity.this, "No events found", Toast.LENGTH_SHORT).show();
                    }
                });
            }

        } catch (Exception e) {
            Log.e(TAG, "Error parsing events response", e);
            mainHandler.post(() -> {
                handleFetchEventsComplete(isRefresh);
                Toast.makeText(EventActivity.this, "Error loading events: " + e.getMessage(), Toast.LENGTH_LONG).show();
                showEmptyState();
            });
        }
    }

    // Fixed version of handleEventsError method in EventActivity.java

    private void handleEventsError(com.android.volley.VolleyError error, boolean isRefresh) {
        Log.e(TAG, "Error loading events: " + error.toString());

        String errorMessage;
        if (error.networkResponse != null) {
            int statusCode = error.networkResponse.statusCode;
            Log.e(TAG, "Status Code: " + statusCode);

            try {
                String responseData = new String(error.networkResponse.data);
                Log.e(TAG, "Response Data: " + responseData);
            } catch (Exception e) {
                Log.e(TAG, "Could not parse error response data");
            }

            if (statusCode == 401 || statusCode == 403) {
                errorMessage = "Session expired - Please login again";
                // Clear invalid auth data
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.remove("auth_token");
                editor.remove("user_id");
                editor.remove("samaj_id");
                editor.apply();

                // Optionally redirect to login
                // Intent loginIntent = new Intent(this, LoginActivity.class);
                // loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                // startActivity(loginIntent);
                // finish();

            } else if (statusCode == 404) {
                errorMessage = "No events found for this samaj";
            } else if (statusCode >= 500) {
                errorMessage = "Server error - Please try again later";
            } else {
                errorMessage = "Failed to load events";
            }
        } else {
            errorMessage = "Network error - Check your connection";
            Log.e(TAG, "Network error details: " + error.getMessage());
        }

        mainHandler.post(() -> {
            handleFetchEventsComplete(isRefresh);

            if (isRefresh) {
                // FIXED: Use the actual error message instead of "FNRJF"
                Toast.makeText(EventActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }

            showEmptyState();
        });
    }

    private void handleFetchEventsComplete(boolean isRefresh) {
        isLoadingEvents = false;

        // Stop refresh indicator if this was a refresh
        if (isRefresh && swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }

        Log.d(TAG, "Fetch events completed (isRefresh: " + isRefresh + ")");
    }

    private void showEmptyState() {
        Log.d(TAG, "Showing empty state");
        recyclerView.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.VISIBLE);
    }

    private void hideEmptyState() {
        Log.d(TAG, "Hiding empty state");
        recyclerView.setVisibility(View.VISIBLE);
        emptyStateLayout.setVisibility(View.GONE);
    }

    // Interface methods for EventAdapter callbacks
    @Override
    public void onUpdateEvent(Event event) {
        try {
            Intent intent = new Intent(EventActivity.this, UpdateEvent.class);
            intent.putExtra("event_id", event.getIdAsLong());
            intent.putExtra("event_title", event.getEventTitle());
            intent.putExtra("event_description", event.getEventDescription());
            intent.putExtra("event_location", event.getLocation());
            intent.putExtra("event_date", event.getEventDate());
            intent.putExtra("event_time", event.getEventTime());
            intent.putExtra("event_image_url", event.getImageUrl());
            intent.putExtra("event_image_base64", event.getImageBase64());
            startActivityForResult(intent, 1002);
        } catch (Exception e) {
            Log.e(TAG, "Error starting UpdateEvent activity", e);
            Toast.makeText(this, "Error opening update screen", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDeleteEvent(Event event) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Event")
                .setMessage("Are you sure you want to delete this event?")
                .setPositiveButton("Delete", (dialog, which) -> deleteEvent(event))
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onViewReactions(Event event, ReactionStats stats) {
        // Handle view reactions - you can implement a reactions viewing activity here
        String message = "Event: " + event.getEventTitle() + "\n" +
                "Likes: " + stats.getLikeCount() + "\n" +
                "Dislikes: " + stats.getDislikeCount() + "\n" +
                "Total: " + stats.getTotalReactions();

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Event Reactions")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void deleteEvent(Event event) {
        String url = EVENT_URL + event.getIdAsLong();
        Log.d(TAG, "Deleting event with URL: " + url);

        StringRequest deleteRequest = new StringRequest(
                Request.Method.DELETE,
                url,
                response -> {
                    Log.d(TAG, "Event deleted successfully: " + response);
                    mainHandler.post(() -> {
                        Toast.makeText(EventActivity.this, "Event deleted successfully", Toast.LENGTH_SHORT).show();
                        fetchEvents(false); // Refresh the list
                    });
                },
                error -> {
                    Log.e(TAG, "Error deleting event", error);
                    String errorMessage = "Failed to delete event";
                    if (error.networkResponse != null) {
                        Log.e(TAG, "Delete error status code: " + error.networkResponse.statusCode);
                        try {
                            Log.e(TAG, "Delete error response: " + new String(error.networkResponse.data));
                        } catch (Exception e) {
                            Log.e(TAG, "Could not parse delete error response");
                        }
                    }
                    mainHandler.post(() -> {
                        Toast.makeText(EventActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    });
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");

                String authToken = sharedPrefs.getString("auth_token", "");
                if (!authToken.isEmpty()) {
                    headers.put("Authorization", "Bearer " + authToken);
                }

                return headers;
            }
        };

        requestQueue.add(deleteRequest);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d(TAG, "onActivityResult - requestCode: " + requestCode + ", resultCode: " + resultCode);

        if (resultCode == RESULT_OK) {
            if (requestCode == 1001) { // Create event result
                Toast.makeText(this, "Event created successfully", Toast.LENGTH_SHORT).show();
                fetchEvents(false); // Refresh the list
            } else if (requestCode == 1002) { // Update event result
                Toast.makeText(this, "Event updated successfully", Toast.LENGTH_SHORT).show();
                fetchEvents(false); // Refresh the list
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called - isFirstLoad: " + isFirstLoad);

        // Only refresh if not the first load and we have valid data
        if (!isFirstLoad && validateUserData() && !isLoadingEvents) {
            Log.d(TAG, "onResume - Refreshing events");
            fetchEvents(false);
        }
        isFirstLoad = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause called");

        // Stop any ongoing refresh
        if (swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy called");

        // Cancel all pending requests
        if (requestQueue != null) {
            requestQueue.cancelAll(TAG);
        }

        // Clean up handler
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }

        // Stop refresh indicator
        if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed called");
        super.onBackPressed();
    }

    // Public method to trigger refresh (can be called from other components)
    public void triggerRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(true);
            refreshEvents();
        }
    }
}