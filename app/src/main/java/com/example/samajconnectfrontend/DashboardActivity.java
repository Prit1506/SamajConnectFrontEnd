package com.example.samajconnectfrontend;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.samajconnectfrontend.adapters.EventSliderAdapter;
import com.example.samajconnectfrontend.models.Event;
import com.example.samajconnectfrontend.models.EventResponse;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends AppCompatActivity {

    private TextView userNameTextView, samajNameTextView;
    private ImageView profileImageView;
    private RecyclerView eventsRecyclerView;
    private EventSliderAdapter eventSliderAdapter;
    private List<Event> eventsList;
    private RequestQueue requestQueue;

    private static final String BASE_URL = "http://10.0.2.2:8080/api/events/";
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


        requestQueue = Volley.newRequestQueue(this);
        initializeViews();
        setupRecyclerView();
        loadUserData();
        loadEvents();


    }

    private void initializeViews() {
        userNameTextView = findViewById(R.id.textView10);
        samajNameTextView = findViewById(R.id.textView11);
        profileImageView = findViewById(R.id.imageView4);
        eventsRecyclerView = findViewById(R.id.eventsRecyclerView); // Add this to your XML
    }

    private void setupRecyclerView() {
        eventsList = new ArrayList<>();
        eventSliderAdapter = new EventSliderAdapter(this, eventsList);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        eventsRecyclerView.setLayoutManager(layoutManager);
        eventsRecyclerView.setAdapter(eventSliderAdapter);

        // Set click listener for events
        eventSliderAdapter.setOnEventClickListener(event -> {
            // Handle event click - navigate to event details
            Toast.makeText(this, "Clicked: " + event.getEventTitle(), Toast.LENGTH_SHORT).show();
            // You can add navigation to EventDetailsActivity here
        });
    }

    private void loadUserData() {
        SharedPreferences sharedPrefs = getSharedPreferences("SamajConnect", MODE_PRIVATE);
        String userName = sharedPrefs.getString("user_name", "User");
        String userEmail = sharedPrefs.getString("user_email", "");

        userNameTextView.setText(userName);
        samajNameTextView.setText("Your Samaj"); // This should be fetched from API

        // You might want to fetch samaj_id from user data
        // currentSamajId = sharedPrefs.getLong("samaj_id", 1L);
    }

    private void loadEvents() {
        String url = BASE_URL + "samaj/" + currentSamajId + "/upcoming";

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

        requestQueue.add(request);
    }

    private void handleEventsResponse(JSONObject response) {
        try {
            Gson gson = new Gson();
            EventResponse eventResponse = gson.fromJson(response.toString(), EventResponse.class);

            if (eventResponse.isSuccess() && eventResponse.getEvents() != null) {
                eventsList.clear();
                eventsList.addAll(eventResponse.getEvents());
                eventSliderAdapter.updateEvents(eventsList);

                Log.d("DashboardActivity", "Loaded " + eventsList.size() + " events");
            } else {
                Toast.makeText(this, "No events found", Toast.LENGTH_SHORT).show();
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
}