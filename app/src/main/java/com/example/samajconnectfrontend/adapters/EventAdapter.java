package com.example.samajconnectfrontend.adapters;

import com.example.samajconnectfrontend.R;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.example.samajconnectfrontend.models.Event;
import com.example.samajconnectfrontend.models.EventReaction;
import com.example.samajconnectfrontend.models.ReactionRequest;
import com.example.samajconnectfrontend.models.ReactionStats;
import com.example.samajconnectfrontend.models.ReactionType;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private static final String TAG = "EventAdapter";
    private static final String REACTION_BASE_URL = "http://10.0.2.2:8080/api/events/";

    private Context context;
    private List<Event> eventList;
    private boolean isAdmin;
    private OnEventActionListener listener;
    private RequestQueue requestQueue;
    private SharedPreferences sharedPrefs;
    private long currentUserId;

    // Interface for handling event actions
    public interface OnEventActionListener {
        void onUpdateEvent(Event event);
        void onDeleteEvent(Event event);
        void onViewReactions(Event event, ReactionStats stats);
    }

    public EventAdapter(Context context, List<Event> eventList, boolean isAdmin, OnEventActionListener listener) {
        this.context = context;
        this.eventList = eventList;
        this.isAdmin = isAdmin;
        this.listener = listener;
        this.requestQueue = Volley.newRequestQueue(context);
        this.sharedPrefs = context.getSharedPreferences("SamajConnect", Context.MODE_PRIVATE);
        this.currentUserId = sharedPrefs.getLong("user_id", -1L);
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = eventList.get(position);
        holder.bind(event);
    }

    @Override
    public int getItemCount() {
        int count = eventList != null ? eventList.size() : 0;
        Log.d(TAG, "getItemCount() returning: " + count);
        return count;
    }

    public class EventViewHolder extends RecyclerView.ViewHolder {

        private ImageView eventImageView;
        private TextView eventTitleTextView;
        private TextView eventDescriptionTextView;
        private TextView eventDateTextView;
        private TextView eventLocationTextView;
        private LinearLayout adminButtonsLayout;
        private MaterialButton updateEventButton;
        private MaterialButton deleteEventButton;

        // Reaction UI elements
        private LinearLayout likeButtonContainer;
        private LinearLayout dislikeButtonContainer;
        private LinearLayout viewReactionsContainer;
        private ImageView likeIcon;
        private ImageView dislikeIcon;
        private TextView likeCountText;
        private TextView dislikeCountText;
        private TextView viewReactionsText;
        private LinearLayout reactionProgressContainer;
        private View likeProgressBar;
        private View dislikeProgressBar;
        private TextView reactionSummaryText;

        // Current reaction state
        private ReactionStats currentStats;
        private boolean isLoadingReaction = false;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);

            eventImageView = itemView.findViewById(R.id.eventImageView);
            eventTitleTextView = itemView.findViewById(R.id.eventTitleTextView);
            eventDescriptionTextView = itemView.findViewById(R.id.eventDescriptionTextView);
            eventDateTextView = itemView.findViewById(R.id.eventDateTextView);
            eventLocationTextView = itemView.findViewById(R.id.eventLocationTextView);
            adminButtonsLayout = itemView.findViewById(R.id.adminButtonsLayout);
            updateEventButton = itemView.findViewById(R.id.updateEventButton);
            deleteEventButton = itemView.findViewById(R.id.deleteEventButton);

            // Initialize reaction UI elements
            likeButtonContainer = itemView.findViewById(R.id.likeButtonContainer);
            dislikeButtonContainer = itemView.findViewById(R.id.dislikeButtonContainer);
            viewReactionsContainer = itemView.findViewById(R.id.viewReactionsContainer);
            likeIcon = itemView.findViewById(R.id.likeIcon);
            dislikeIcon = itemView.findViewById(R.id.dislikeIcon);
            likeCountText = itemView.findViewById(R.id.likeCountText);
            dislikeCountText = itemView.findViewById(R.id.dislikeCountText);
            viewReactionsText = itemView.findViewById(R.id.viewReactionsText);
            reactionProgressContainer = itemView.findViewById(R.id.reactionProgressContainer);
            likeProgressBar = itemView.findViewById(R.id.likeProgressBar);
            dislikeProgressBar = itemView.findViewById(R.id.dislikeProgressBar);
            reactionSummaryText = itemView.findViewById(R.id.reactionSummaryText);
        }

        public void bind(Event event) {
            Log.d(TAG, "Binding event: " + event.getEventTitle());

            // Set event title
            if (!TextUtils.isEmpty(event.getEventTitle())) {
                eventTitleTextView.setText(event.getEventTitle());
            } else {
                eventTitleTextView.setText("Untitled Event");
            }

            // Set event description
            if (!TextUtils.isEmpty(event.getEventDescription())) {
                eventDescriptionTextView.setText(event.getEventDescription());
                eventDescriptionTextView.setVisibility(View.VISIBLE);
            } else {
                eventDescriptionTextView.setVisibility(View.GONE);
            }

            // Set event date and time - format the date
            String formattedDate = formatDate(event.getEventDate());
            eventDateTextView.setText(formattedDate);

            // Set event location
            if (!TextUtils.isEmpty(event.getLocation())) {
                eventLocationTextView.setText(event.getLocation());
                eventLocationTextView.setVisibility(View.VISIBLE);
            } else {
                eventLocationTextView.setText("Location TBD");
            }
            // Load event image - prioritize Base64 over URL
            loadEventImage(event);

            // Show/hide admin buttons based on admin status
            if (isAdmin) {
                adminButtonsLayout.setVisibility(View.VISIBLE);
                setupAdminButtonClickListeners(event);
            } else {
                adminButtonsLayout.setVisibility(View.GONE);
            }

            // Setup reaction functionality
            setupReactionListeners(event);
            loadReactionStats(event);

            // Show/hide view reactions for admin only
            if (isAdmin) {
                viewReactionsContainer.setVisibility(View.VISIBLE);
            } else {
                viewReactionsContainer.setVisibility(View.GONE);
            }
        }

        private void setupReactionListeners(Event event) {
            likeButtonContainer.setOnClickListener(v -> {
                if (!isLoadingReaction) {
                    handleReactionClick(event, ReactionType.LIKE);
                }
            });

            dislikeButtonContainer.setOnClickListener(v -> {
                if (!isLoadingReaction) {
                    handleReactionClick(event, ReactionType.DISLIKE);
                }
            });

            viewReactionsContainer.setOnClickListener(v -> {
                if (listener != null && currentStats != null) {
                    listener.onViewReactions(event, currentStats);
                }
            });
        }

        private void handleReactionClick(Event event, ReactionType reactionType) {
            String authToken = sharedPrefs.getString("auth_token", "");
            if (authToken.isEmpty() || currentUserId == -1L) {
                Toast.makeText(context, "Please login to react to events", Toast.LENGTH_SHORT).show();
                return;
            }
            if (currentUserId == -1L) {
                Toast.makeText(context, "Please login to react", Toast.LENGTH_SHORT).show();
                return;
            }

            isLoadingReaction = true;
            updateReactionButtonsLoadingState(true);

            // Check if user is toggling the same reaction (remove) or changing reaction
            boolean isTogglingSame = currentStats != null && currentStats.hasUserReacted() &&
                    currentStats.getUserReaction().getReactionType().equals(reactionType.name());

            String url = REACTION_BASE_URL + event.getIdAsLong() + "/reactions";

            ReactionRequest reactionRequest = new ReactionRequest(currentUserId, reactionType.name());
            Gson gson = new Gson();
            String requestBody = gson.toJson(reactionRequest);

            try {
                JSONObject jsonBody = new JSONObject(requestBody);

                JsonObjectRequest request = new JsonObjectRequest(
                        Request.Method.POST,
                        url,
                        jsonBody,
                        response -> {
                            Log.d(TAG, "Reaction response: " + response.toString());
                            isLoadingReaction = false;
                            updateReactionButtonsLoadingState(false);

                            // Reload reaction stats after successful reaction
                            loadReactionStats(event);

                            String message = isTogglingSame ? "Reaction removed" : "Reaction added";
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                        },
                        error -> {
                            Log.e(TAG, "Error adding/removing reaction", error);
                            isLoadingReaction = false;
                            updateReactionButtonsLoadingState(false);

                            String errorMessage = "Failed to update reaction";
                            if (error.networkResponse != null) {
                                int statusCode = error.networkResponse.statusCode;
                                if (statusCode == 401) {
                                    errorMessage = "Please login again";
                                } else if (statusCode == 404) {
                                    errorMessage = "Event not found";
                                }
                            }
                            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show();
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

                requestQueue.add(request);

            } catch (Exception e) {
                Log.e(TAG, "Error creating reaction request", e);
                isLoadingReaction = false;
                updateReactionButtonsLoadingState(false);
                Toast.makeText(context, "Error updating reaction", Toast.LENGTH_SHORT).show();
            }
        }

        // Alternative approach if reaction stats should be public
        private void loadReactionStats(Event event) {
            Log.d(TAG, "Loading public reaction stats for event: " + event.getIdAsLong());

            // Use public endpoint that doesn't require authentication
            String url = REACTION_BASE_URL + event.getIdAsLong() + "/reactions/stats/public";

            // If you need user-specific data, add userId as query param only if available
            String authToken = sharedPrefs.getString("auth_token", "");
            if (!authToken.isEmpty() && currentUserId != -1L) {
                url += "?userId=" + currentUserId;
            }

            Log.d(TAG, "Making public request to URL: " + url);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.GET,
                    url,
                    null,
                    response -> {
                        Log.d(TAG, "✅ Public reaction stats response: " + response.toString());
                        handleReactionStatsResponse(response);
                    },
                    error -> {
                        Log.e(TAG, "❌ Error loading public reaction stats", error);
                        // Set default stats
                        currentStats = new ReactionStats(0, 0);
                        updateReactionUI();
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    headers.put("Accept", "application/json");

                    // Only add auth header if token is available (for user-specific reactions)
                    String token = sharedPrefs.getString("auth_token", "");
                    if (!token.isEmpty()) {
                        headers.put("Authorization", "Bearer " + token);
                        Log.d(TAG, "📤 Added optional auth header");
                    } else {
                        Log.d(TAG, "📤 Making public request without auth");
                    }

                    return headers;
                }
            };

            requestQueue.add(request);
        }


        private void handleReactionStatsResponse(JSONObject response) {
            try {
                boolean success = response.optBoolean("success", false);
                if (success && response.has("data")) {
                    JSONObject data = response.getJSONObject("data");

                    Gson gson = new Gson();
                    currentStats = gson.fromJson(data.toString(), ReactionStats.class);

                    if (currentStats == null) {
                        currentStats = new ReactionStats(0, 0);
                    }

                    Log.d(TAG, "Parsed reaction stats: " + currentStats.toString());
                } else {
                    currentStats = new ReactionStats(0, 0);
                }

                updateReactionUI();

            } catch (Exception e) {
                Log.e(TAG, "Error parsing reaction stats", e);
                currentStats = new ReactionStats(0, 0);
                updateReactionUI();
            }
        }

        private void updateReactionUI() {
            if (currentStats == null) {
                currentStats = new ReactionStats(0, 0);
            }

            // Update counts
            likeCountText.setText(String.valueOf(currentStats.getLikeCount()));
            dislikeCountText.setText(String.valueOf(currentStats.getDislikeCount()));

            // Update button states based on user's reaction
            updateReactionButtonStates();

            // Update progress bars and summary (only show if admin or has reactions)
            if (isAdmin || currentStats.getTotalReactions() > 0) {
                updateReactionProgress();
                updateReactionSummary();
            } else {
                reactionProgressContainer.setVisibility(View.GONE);
                reactionSummaryText.setVisibility(View.GONE);
            }

            // Update view reactions text
            if (isAdmin) {
                String reactionsText = currentStats.getTotalReactions() == 0 ?
                        "No reactions" : "View " + currentStats.getTotalReactions() + " reactions";
                viewReactionsText.setText(reactionsText);
            }
        }

        private void updateReactionButtonStates() {
            // Reset to default state
            likeIcon.setImageResource(R.drawable.ic_thumb_up_outline);
            dislikeIcon.setImageResource(R.drawable.ic_thumb_down_outline);
            likeIcon.setColorFilter(ContextCompat.getColor(context, R.color.default_icon_color));
            dislikeIcon.setColorFilter(ContextCompat.getColor(context, R.color.default_icon_color));
            likeCountText.setTextColor(ContextCompat.getColor(context, R.color.default_icon_color));
            dislikeCountText.setTextColor(ContextCompat.getColor(context, R.color.default_icon_color));

            // Update based on user's reaction
            if (currentStats != null && currentStats.hasUserReacted()) {
                if (currentStats.hasUserLiked()) {
                    // User has liked
                    likeIcon.setImageResource(R.drawable.ic_thumb_up_filled);
                    likeIcon.setColorFilter(ContextCompat.getColor(context, R.color.like_active_color));
                    likeCountText.setTextColor(ContextCompat.getColor(context, R.color.like_active_color));
                } else if (currentStats.hasUserDisliked()) {
                    // User has disliked
                    dislikeIcon.setImageResource(R.drawable.ic_thumb_down_filled);
                    dislikeIcon.setColorFilter(ContextCompat.getColor(context, R.color.dislike_active_color));
                    dislikeCountText.setTextColor(ContextCompat.getColor(context, R.color.dislike_active_color));
                }
            }
        }

        private void updateReactionProgress() {
            if (currentStats.getTotalReactions() == 0) {
                reactionProgressContainer.setVisibility(View.GONE);
                return;
            }

            reactionProgressContainer.setVisibility(View.VISIBLE);

            // Calculate weights for progress bars
            double likeWeight = currentStats.getLikePercentage() / 100.0;
            double dislikeWeight = currentStats.getDislikePercentage() / 100.0;

            // Update layout weights
            LinearLayout.LayoutParams likeParams = (LinearLayout.LayoutParams) likeProgressBar.getLayoutParams();
            LinearLayout.LayoutParams dislikeParams = (LinearLayout.LayoutParams) dislikeProgressBar.getLayoutParams();

            likeParams.weight = (float) Math.max(likeWeight, 0.1); // Minimum weight for visibility
            dislikeParams.weight = (float) Math.max(dislikeWeight, 0.1);

            likeProgressBar.setLayoutParams(likeParams);
            dislikeProgressBar.setLayoutParams(dislikeParams);
        }

        private void updateReactionSummary() {
            if (currentStats.getTotalReactions() > 0) {
                reactionSummaryText.setText(currentStats.getReactionSummary());
                reactionSummaryText.setVisibility(View.VISIBLE);
            } else {
                reactionSummaryText.setVisibility(View.GONE);
            }
        }

        private void updateReactionButtonsLoadingState(boolean isLoading) {
            likeButtonContainer.setEnabled(!isLoading);
            dislikeButtonContainer.setEnabled(!isLoading);

            if (isLoading) {
                likeButtonContainer.setAlpha(0.6f);
                dislikeButtonContainer.setAlpha(0.6f);
            } else {
                likeButtonContainer.setAlpha(1.0f);
                dislikeButtonContainer.setAlpha(1.0f);
            }
        }

        private void loadEventImage(Event event) {
            // First try to load from Base64 (from API)
            String base64Image = event.getImageBase64();
            Log.d(TAG, "Base64 image length: " + (base64Image != null ? base64Image.length() : "null"));

            if (base64Image != null && !base64Image.isEmpty()) {
                try {
                    byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                    Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    if (decodedBitmap != null) {
                        eventImageView.setImageBitmap(decodedBitmap);
                        Log.d(TAG, "Successfully loaded Base64 image");
                        return;
                    } else {
                        Log.e(TAG, "Decoded bitmap is null");
                    }
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Failed to decode base64 image", e);
                }
            }

            // Fallback to URL if Base64 fails or is not available
            String imageUrl = event.getImageUrl();
            if (!TextUtils.isEmpty(imageUrl)) {
                Log.d(TAG, "Loading image from URL: " + imageUrl);
                Glide.with(context)
                        .load(imageUrl)
                        .apply(new RequestOptions()
                                .placeholder(R.drawable.logo_banner)
                                .error(R.drawable.logo_banner)
                                .transform(new RoundedCorners(16)))
                        .into(eventImageView);
            } else {
                // Load default image
                Log.d(TAG, "Loading default image");
                eventImageView.setImageResource(R.drawable.logo_banner);
            }
        }

        private String formatDate(String dateString) {
            if (TextUtils.isEmpty(dateString)) {
                return "Date TBD";
            }

            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
                Date date = inputFormat.parse(dateString);
                return outputFormat.format(date);
            } catch (ParseException e) {
                Log.e(TAG, "Failed to parse date: " + dateString, e);
                return dateString; // Return original if parsing fails
            }
        }

        private void setupAdminButtonClickListeners(Event event) {
            updateEventButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUpdateEvent(event);
                }
            });

            deleteEventButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteEvent(event);
                }
            });
        }
    }

    // FIXED: Method to update the dataset
    @SuppressLint("NotifyDataSetChanged")
    public void updateEvents(List<Event> newEventList) {
        Log.d(TAG, "Updating events list. New size: " + (newEventList != null ? newEventList.size() : 0));

        // Create a new ArrayList to avoid reference issues
        if (newEventList != null) {
            this.eventList = new ArrayList<>(newEventList);
            Log.d(TAG, "EventList updated, new size: " + this.eventList.size());
        } else {
            this.eventList = new ArrayList<>();
            Log.d(TAG, "EventList set to empty list");
        }

        notifyDataSetChanged();
        Log.d(TAG, "notifyDataSetChanged() called");
    }

    // Method to add a single event
    public void addEvent(Event event) {
        if (this.eventList == null) {
            this.eventList = new ArrayList<>();
        }

        if (event != null) {
            this.eventList.add(0, event); // Add to top
            notifyItemInserted(0);
            Log.d(TAG, "Added event: " + event.getEventTitle());
        }
    }

    // Method to remove an event
    public void removeEvent(int position) {
        if (eventList != null && position >= 0 && position < eventList.size()) {
            Event removedEvent = eventList.remove(position);
            notifyItemRemoved(position);
            Log.d(TAG, "Removed event: " + (removedEvent != null ? removedEvent.getEventTitle() : "unknown"));
        }
    }

    // Method to update admin status
    public void setAdminStatus(boolean isAdmin) {
        this.isAdmin = isAdmin;
        notifyDataSetChanged();
        Log.d(TAG, "Admin status updated: " + isAdmin);
    }
}