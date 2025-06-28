package com.example.samajconnectfrontend;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.samajconnectfrontend.adapters.SearchResultsAdapter;
import com.example.samajconnectfrontend.dialogs.MemberDetailsDialog;
import com.example.samajconnectfrontend.models.DetailedUserDto;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SearchManager {
    private static final String BASE_URL = "http://10.0.2.2:8080/api/";

    private Context context;
    private RequestQueue requestQueue;
    private EditText searchEditText;
    private LinearLayout searchResultsContainer;
    private RecyclerView searchResultsRecyclerView;
    private TextView searchResultsTitle;
    private ScrollView mainScrollView;
    private SearchCallback callback;

    private List<DetailedUserDto> searchResults;
    private SearchResultsAdapter searchResultsAdapter;
    private boolean isSearching = false;
    private boolean isSearchResultsVisible = false;

    public interface SearchCallback {
        Long getCurrentSamajId();
        String getAuthToken();
        void onMemberClicked(DetailedUserDto member);
    }

    public SearchManager(Context context, RequestQueue requestQueue, EditText searchEditText,
                         LinearLayout searchResultsContainer, RecyclerView searchResultsRecyclerView,
                         TextView searchResultsTitle, ScrollView mainScrollView, SearchCallback callback) {
        this.context = context;
        this.requestQueue = requestQueue;
        this.searchEditText = searchEditText;
        this.searchResultsContainer = searchResultsContainer;
        this.searchResultsRecyclerView = searchResultsRecyclerView;
        this.searchResultsTitle = searchResultsTitle;
        this.mainScrollView = mainScrollView;
        this.callback = callback;

        this.searchResults = new ArrayList<>();
        this.searchResultsAdapter = new SearchResultsAdapter(context, searchResults, this::onMemberClicked);
    }

    public void setupSearch() {
        // Setup search results RecyclerView
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        searchResultsRecyclerView.setAdapter(searchResultsAdapter);

        // Initially hide search results
        hideSearchResults();

        // Set up search functionality
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER)) {
                performSearch();
                return true;
            }
            return false;
        });

        // Add text change listener for real-time search
        searchEditText.addTextChangedListener(new android.text.TextWatcher() {
            private Handler searchHandler = new Handler(Looper.getMainLooper());
            private Runnable searchRunnable;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                searchRunnable = () -> {
                    String query = s.toString().trim();
                    if (query.length() >= 2) {
                        performSearch();
                    } else if (query.isEmpty()) {
                        hideSearchResults();
                    }
                };

                searchHandler.postDelayed(searchRunnable, 500);
            }
        });

        // Add click listener to search icon
        searchEditText.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (searchEditText.getRight() - searchEditText.getCompoundDrawables()[2].getBounds().width())) {
                    performSearch();
                    return true;
                }
            }
            return false;
        });
    }

    private void performSearch() {
        String query = searchEditText.getText().toString().trim();

        if (query.isEmpty()) {
            Toast.makeText(context, "Please enter a search term", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isSearching) {
            Log.d("SearchManager", "Search already in progress");
            return;
        }

        Long currentSamajId = callback.getCurrentSamajId();
        if (currentSamajId == null || currentSamajId <= 0) {
            Toast.makeText(context, "Samaj information not available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Hide keyboard
        android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);

        isSearching = true;
        searchMembers(query, currentSamajId);
    }

    private void searchMembers(String query, Long samajId) {
        String url = BASE_URL + "users/samaj/search-members";

        Log.d("SearchManager", "Searching members with query: " + query + " for samaj: " + samajId);

        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("samajId", samajId);
            requestBody.put("query", query);
            requestBody.put("page", 0);
            requestBody.put("size", 20);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    url,
                    requestBody,
                    response -> {
                        Log.d("SearchManager", "Search response: " + response.toString());
                        handleSearchResponse(response, query);
                    },
                    error -> {
                        Log.e("SearchManager", "Search error: " + error.toString());
                        handleSearchError(error);
                    }
            ) {
                @Override
                public java.util.Map<String, String> getHeaders() {
                    java.util.Map<String, String> headers = new java.util.HashMap<>();
                    headers.put("Content-Type", "application/json");

                    String authToken = callback.getAuthToken();
                    if (!authToken.isEmpty()) {
                        headers.put("Authorization", "Bearer " + authToken);
                    }

                    return headers;
                }
            };

            request.setRetryPolicy(new DefaultRetryPolicy(10000, 1, 1.0f));
            requestQueue.add(request);

        } catch (Exception e) {
            Log.e("SearchManager", "Error creating search request: " + e.getMessage());
            isSearching = false;
            Toast.makeText(context, "Error performing search", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleSearchResponse(JSONObject response, String query) {
        try {
            isSearching = false;

            if (response.has("success") && response.getBoolean("success")) {
                JSONObject data = response.getJSONObject("data");
                JSONArray membersArray = data.getJSONArray("members");

                searchResults.clear();

                for (int i = 0; i < membersArray.length(); i++) {
                    JSONObject memberJson = membersArray.getJSONObject(i);
                    DetailedUserDto member = parseDetailedUserDto(memberJson);
                    searchResults.add(member);
                }

                Log.d("SearchManager", "Found " + searchResults.size() + " search results");

                if (searchResults.isEmpty()) {
                    showNoResults(query);
                } else {
                    showSearchResults(query);
                }

            } else {
                String message = response.optString("message", "Search failed");
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                hideSearchResults();
            }

        } catch (Exception e) {
            Log.e("SearchManager", "Error parsing search response: " + e.getMessage());
            isSearching = false;
            hideSearchResults();
            Toast.makeText(context, "Error processing search results", Toast.LENGTH_SHORT).show();
        }
    }

    private DetailedUserDto parseDetailedUserDto(JSONObject memberJson) throws Exception {
        DetailedUserDto member = new DetailedUserDto();

        member.setId(memberJson.getLong("id"));
        member.setName(memberJson.getString("name"));
        member.setEmail(memberJson.getString("email"));
        member.setGender(memberJson.optString("gender", ""));
        member.setPhoneNumber(memberJson.optString("phoneNumber", ""));
        member.setAddress(memberJson.optString("address", ""));
        member.setIsAdmin(memberJson.optBoolean("isAdmin", false));

        // Handle profile image
        if (memberJson.has("profileImageBase64") && !memberJson.isNull("profileImageBase64")) {
            String base64Image = memberJson.getString("profileImageBase64");
            member.setProfileImageBase64(base64Image);
        }

        return member;
    }

    private void showSearchResults(String query) {
        searchResultsTitle.setText("Found " + searchResults.size() + " member(s) for '" + query + "'");
        searchResultsAdapter.notifyDataSetChanged();
        limitSearchResultsHeight(); // Add this line
        showSearchResultsContainer();
    }

    private void showNoResults(String query) {
        searchResults.clear();
        searchResultsTitle.setText("No members found for '" + query + "'");
        searchResultsAdapter.notifyDataSetChanged();
        limitSearchResultsHeight(); // Add this line
        showSearchResultsContainer();
    }

    private void showSearchResultsContainer() {
        if (!isSearchResultsVisible) {
            searchResultsContainer.setVisibility(View.VISIBLE);
            isSearchResultsVisible = true;

            // Set maximum height for search results container to prevent full screen scroll
            searchResultsContainer.post(() -> {
                android.view.ViewGroup.LayoutParams params = searchResultsContainer.getLayoutParams();

                // Calculate maximum height (30% of screen height or 400dp, whichever is smaller)
                int screenHeight = context.getResources().getDisplayMetrics().heightPixels;
                int maxHeight = Math.min(
                        (int) (screenHeight * 0.3f),
                        (int) (400 * context.getResources().getDisplayMetrics().density)
                );

                // Set the maximum height
                if (params instanceof LinearLayout.LayoutParams) {
                    LinearLayout.LayoutParams linearParams = (LinearLayout.LayoutParams) params;
                    linearParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
                    searchResultsContainer.setLayoutParams(linearParams);
                }

                // Set maximum height for the RecyclerView specifically
                android.view.ViewGroup.LayoutParams recyclerParams = searchResultsRecyclerView.getLayoutParams();
                recyclerParams.height = Math.min(maxHeight, recyclerParams.height);
                searchResultsRecyclerView.setLayoutParams(recyclerParams);

                // Enable nested scrolling for the RecyclerView
                searchResultsRecyclerView.setNestedScrollingEnabled(true);
            });

            // Gentle scroll to show search results without full screen scroll
            mainScrollView.post(() -> {
                int[] location = new int[2];
                searchResultsContainer.getLocationOnScreen(location);

                // Only scroll if search results are not visible
                int scrollY = mainScrollView.getScrollY();
                int containerTop = location[1] - mainScrollView.getTop();

                // Calculate gentle scroll position (just enough to show the search results)
                int targetScroll = Math.max(0, containerTop - 50);

                // Only scroll if necessary and limit the scroll distance
                if (targetScroll > scrollY) {
                    int maxScroll = scrollY + 200; // Limit scroll distance to 200dp
                    int finalScroll = Math.min(targetScroll, maxScroll);
                    mainScrollView.smoothScrollTo(0, finalScroll);
                }
            });
        }
    }

    private void limitSearchResultsHeight() {
        searchResultsRecyclerView.post(() -> {
            // Calculate optimal height based on number of results
            int itemCount = searchResults.size();
            int itemHeight = (int) (72 * context.getResources().getDisplayMetrics().density); // ~72dp per item
            int calculatedHeight = itemCount * itemHeight;

            // Set maximum height limits
            int screenHeight = context.getResources().getDisplayMetrics().heightPixels;
            int maxHeight = Math.min(
                    (int) (screenHeight * 0.25f), // 25% of screen height
                    (int) (250 * context.getResources().getDisplayMetrics().density) // or 250dp
            );

            // Apply the height constraint
            android.view.ViewGroup.LayoutParams params = searchResultsRecyclerView.getLayoutParams();
            params.height = Math.min(calculatedHeight, maxHeight);
            searchResultsRecyclerView.setLayoutParams(params);

            // Ensure smooth scrolling within the RecyclerView
            searchResultsRecyclerView.setNestedScrollingEnabled(true);
        });
    }

    private void hideSearchResults() {
        if (isSearchResultsVisible) {
            searchResultsContainer.setVisibility(View.GONE);
            isSearchResultsVisible = false;
            searchResults.clear();
            if (searchResultsAdapter != null) {
                searchResultsAdapter.notifyDataSetChanged();
            }
        }
    }

    private void handleSearchError(com.android.volley.VolleyError error) {
        isSearching = false;

        String errorMessage = "Search failed";
        if (error.networkResponse != null) {
            int statusCode = error.networkResponse.statusCode;
            if (statusCode == 401) {
                errorMessage = "Unauthorized - Please login again";
            } else if (statusCode == 404) {
                errorMessage = "Samaj not found";
            } else if (statusCode >= 500) {
                errorMessage = "Server error - Please try again later";
            }
        } else {
            errorMessage = "Network error - Check your connection";
        }

        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show();
        Log.e("SearchManager", "Search error: " + error.toString());
    }

    private void onMemberClicked(DetailedUserDto member) {
        callback.onMemberClicked(member);
    }

    public void showMemberDetails(DetailedUserDto member) {
        MemberDetailsDialog dialog = new MemberDetailsDialog(context);
        dialog.showMemberDetails(member);
    }

    public void cleanup() {
        hideSearchResults();
    }
}
