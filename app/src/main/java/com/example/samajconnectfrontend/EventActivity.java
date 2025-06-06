package com.example.samajconnectfrontend;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.samajconnectfrontend.adapters.EventAdapter;
import com.example.samajconnectfrontend.models.Event;
import com.example.samajconnectfrontend.models.EventResponse;
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

    private RecyclerView recyclerView;
    private FloatingActionButton fabAdd;
    private LinearLayout emptyStateLayout;
    private EventAdapter eventAdapter;
    private List<Event> eventList;

    private SharedPreferences sharedPrefs;
    private boolean isAdmin;
    private long currentUserId;
    private long currentSamajId;
    private RequestQueue requestQueue;
    private boolean isFirstLoad = true;

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

            initializeViews();
            setupSharedPreferences();

            // Check if we have valid user data before proceeding
            if (!validateUserData()) {
                Log.e(TAG, "Invalid user data, finishing activity");
                finish();
                return;
            }

            setupRecyclerView();
            setupClickListeners();

            // Show loading state initially
            showLoadingState();
            fetchEvents();

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error initializing events screen", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializeViews() {
        try {
            recyclerView = findViewById(R.id.recview);
            fabAdd = findViewById(R.id.fadd);
            emptyStateLayout = findViewById(R.id.empty_state);

            if (recyclerView == null || fabAdd == null || emptyStateLayout == null) {
                throw new RuntimeException("One or more required views not found in layout");
            }

            requestQueue = Volley.newRequestQueue(this);
            eventList = new ArrayList<>();

            Log.d(TAG, "Views initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
            throw e;
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

    private void setupRecyclerView() {
        try {
            LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            recyclerView.setLayoutManager(layoutManager);

            eventAdapter = new EventAdapter(this, eventList, isAdmin, this);
            recyclerView.setAdapter(eventAdapter);

            // Add some debugging for RecyclerView
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

    private void showLoadingState() {
        recyclerView.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.GONE);
        // You could add a loading indicator here if you have one
        Log.d(TAG, "Showing loading state");
    }

    private void fetchEvents() {
        if (currentSamajId == -1L) {
            Log.e(TAG, "Invalid Samaj ID");
            showEmptyState();
            return;
        }

        String url = EVENT_URL + "samaj/" + currentSamajId;
        Log.d(TAG, "Making API call to: " + url);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    Log.d(TAG, "API response received");
                    handleEventsResponse(response);
                },
                error -> {
                    Log.e(TAG, "API error occurred");
                    handleEventsError(error);
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

        // Increase timeout and reduce retries for debugging
        request.setRetryPolicy(new DefaultRetryPolicy(30000, 0, 1.0f));
        requestQueue.add(request);
        Log.d(TAG, "Request added to queue");
    }

    private void handleEventsResponse(JSONObject response) {
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

                // Clear and update the main list
                eventList.clear();
                eventList.addAll(newEvents);

                Log.d(TAG, "EventList updated, size: " + eventList.size());

                // Always update the UI on the main thread
                runOnUiThread(() -> {
                    if (eventList.isEmpty()) {
                        Log.d(TAG, "No events to display - showing empty state");
                        showEmptyState();
                    } else {
                        Log.d(TAG, "Displaying " + eventList.size() + " events");
                        hideEmptyState();

                        // Update adapter with new data
                        eventAdapter.updateEvents(new ArrayList<>(eventList));

                        // Force RecyclerView to refresh
                        recyclerView.post(() -> {
                            eventAdapter.notifyDataSetChanged();
                            Log.d(TAG, "RecyclerView refresh completed");

                            // Additional debugging
                            Log.d(TAG, "Adapter item count: " + eventAdapter.getItemCount());
                            Log.d(TAG, "RecyclerView child count: " + recyclerView.getChildCount());
                        });
                    }
                });

                Log.d(TAG, "Final loaded events count: " + eventList.size());

            } else {
                Log.w(TAG, "No events found or API call failed");
                runOnUiThread(() -> {
                    showEmptyState();
                    Toast.makeText(this, "No events found", Toast.LENGTH_SHORT).show();
                });
            }

        } catch (Exception e) {
            Log.e(TAG, "Error parsing events response", e);
            runOnUiThread(() -> {
                Toast.makeText(this, "Error loading events: " + e.getMessage(), Toast.LENGTH_LONG).show();
                showEmptyState();
            });
        }
    }

    private void handleEventsError(com.android.volley.VolleyError error) {
        Log.e(TAG, "Error loading events: " + error.toString());

        String errorMessage = "Failed to load events";
        if (error.networkResponse != null) {
            int statusCode = error.networkResponse.statusCode;
            Log.e(TAG, "Status Code: " + statusCode);
            Log.e(TAG, "Response Data: " + new String(error.networkResponse.data));

            if (statusCode == 401) {
                errorMessage = "Unauthorized - Please login again";
                // Clear invalid auth data
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.remove("auth_token");
                editor.apply();
            } else if (statusCode == 404) {
                errorMessage = "No events found for this samaj";
            } else if (statusCode >= 500) {
                errorMessage = "Server error - Please try again later";
            }
        } else {
            errorMessage = "Network error - Check your connection";
            Log.e(TAG, "Network error details: " + error.getMessage());
        }

        runOnUiThread(() -> {
           // Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            showEmptyState();
        });
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

    private void deleteEvent(Event event) {
        String url = EVENT_URL + event.getIdAsLong();
        Log.d(TAG, "Deleting event with URL: " + url);

        StringRequest deleteRequest = new StringRequest(
                Request.Method.DELETE,
                url,
                response -> {
                    Log.d(TAG, "Event deleted successfully: " + response);
                    runOnUiThread(() -> {
                        Toast.makeText(EventActivity.this, "Event deleted successfully", Toast.LENGTH_SHORT).show();
                        fetchEvents(); // Refresh the list
                    });
                },
                error -> {
                    Log.e(TAG, "Error deleting event", error);
                    String errorMessage = "Failed to delete event";
                    if (error.networkResponse != null) {
                        Log.e(TAG, "Delete error status code: " + error.networkResponse.statusCode);
                        Log.e(TAG, "Delete error response: " + new String(error.networkResponse.data));
                    }
                    runOnUiThread(() -> {
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
                fetchEvents(); // Refresh the list
            } else if (requestCode == 1002) { // Update event result
                Toast.makeText(this, "Event updated successfully", Toast.LENGTH_SHORT).show();
                fetchEvents(); // Refresh the list
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called - isFirstLoad: " + isFirstLoad);

        // Only refresh if not the first load and we have valid data
        if (!isFirstLoad && validateUserData()) {
            Log.d(TAG, "onResume - Refreshing events");
            fetchEvents();
        }
        isFirstLoad = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause called");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy called");
        if (requestQueue != null) {
            requestQueue.cancelAll(TAG);
        }
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed called");
        super.onBackPressed();
    }
}