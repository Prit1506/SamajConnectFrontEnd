package com.example.samajconnectfrontend.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
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
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.example.samajconnectfrontend.R;
import com.example.samajconnectfrontend.models.Event;
import com.example.samajconnectfrontend.models.ReactionRequest;
import com.example.samajconnectfrontend.models.ReactionStats;
import com.example.samajconnectfrontend.models.ReactionType;
import com.example.samajconnectfrontend.models.UserIdRequest;
import com.example.samajconnectfrontend.dialogs.EventDetailsDialog;
import com.example.samajconnectfrontend.dialogs.FullScreenImageDialog;
import com.example.samajconnectfrontend.utils.CalendarReminderHelper;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EventSliderAdapter extends RecyclerView.Adapter<EventSliderAdapter.EventViewHolder> {

    private static final String TAG = "EventSliderAdapter";
    private static final String REACTION_BASE_URL = "http://10.0.2.2:8080/api/events/";

    private Context context;
    private List<Event> events;
    private OnEventActionListener onEventActionListener;
    private RequestQueue requestQueue;
    private SharedPreferences sharedPrefs;
    private long currentUserId;

    // Pagination properties
    private boolean isLoading = false;
    private boolean hasMorePages = true;
    private int currentPage = 0;
    private OnLoadMoreListener loadMoreListener;

    private LinearLayout reminderButtonContainer;
    private CalendarReminderHelper calendarHelper;

    public interface OnEventActionListener {
        void onEventClick(Event event);
        void onEventImageClick(Event event);
        void onEventDetailsClick(Event event);
    }

    public interface OnLoadMoreListener {
        void onLoadMore(int page);
    }

    public EventSliderAdapter(Context context, List<Event> events) {
        this.context = context;
        this.events = events != null ? events : new ArrayList<>();
        this.requestQueue = Volley.newRequestQueue(context);
        this.sharedPrefs = context.getSharedPreferences("SamajConnect", Context.MODE_PRIVATE);
        this.currentUserId = sharedPrefs.getLong("user_id", -1L);
    }

    public void setOnEventActionListener(OnEventActionListener listener) {
        this.onEventActionListener = listener;
    }

    public void setOnLoadMoreListener(OnLoadMoreListener listener) {
        this.loadMoreListener = listener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_event_slider, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = events.get(position);
        holder.bind(event);

        // Check if we need to load more items (when reaching near the end)
        if (position >= events.size() - 2 && !isLoading && hasMorePages && loadMoreListener != null) {
            isLoading = true;
            loadMoreListener.onLoadMore(currentPage + 1);
        }
    }

    @Override
    public int getItemCount() {
        return events != null ? events.size() : 0;
    }

    // Pagination methods
    public void addMoreEvents(List<Event> newEvents, boolean hasMore) {
        if (newEvents != null && !newEvents.isEmpty()) {
            int startPosition = this.events.size();
            this.events.addAll(newEvents);
            notifyItemRangeInserted(startPosition, newEvents.size());
            currentPage++;
        }
        this.hasMorePages = hasMore;
        this.isLoading = false;
    }

    public void resetPagination() {
        this.currentPage = 0;
        this.hasMorePages = true;
        this.isLoading = false;
        this.events.clear();
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateEvents(List<Event> newEvents) {
        this.events = newEvents != null ? new ArrayList<>(newEvents) : new ArrayList<>();
        notifyDataSetChanged();
    }

    public class EventViewHolder extends RecyclerView.ViewHolder {

        private ImageView eventImageView;
        private TextView eventTitleTextView;
        private TextView eventDateTextView;
        private TextView eventLocationTextView;

        // Reaction UI elements
        private LinearLayout likeButtonContainer;
        private LinearLayout dislikeButtonContainer;
        private LinearLayout viewDetailsContainer;
        private ImageView likeIcon;
        private ImageView dislikeIcon;
        private TextView likeCountText;
        private TextView dislikeCountText;

        // Current reaction state
        private ReactionStats currentStats;
        private boolean isLoadingReaction = false;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);

            eventImageView = itemView.findViewById(R.id.eventImageView);
            eventTitleTextView = itemView.findViewById(R.id.eventTitleTextView);
            eventDateTextView = itemView.findViewById(R.id.eventDateTextView);
            eventLocationTextView = itemView.findViewById(R.id.eventLocationTextView);

            // Initialize reaction UI elements
            likeButtonContainer = itemView.findViewById(R.id.likeButtonContainer);
            dislikeButtonContainer = itemView.findViewById(R.id.dislikeButtonContainer);
            viewDetailsContainer = itemView.findViewById(R.id.viewDetailsContainer);
            likeIcon = itemView.findViewById(R.id.likeIcon);
            dislikeIcon = itemView.findViewById(R.id.dislikeIcon);
            likeCountText = itemView.findViewById(R.id.likeCountText);
            dislikeCountText = itemView.findViewById(R.id.dislikeCountText);

            reminderButtonContainer = itemView.findViewById(R.id.reminderButtonContainer);

            calendarHelper = new CalendarReminderHelper(context);
            calendarHelper.setOnReminderSetListener(new CalendarReminderHelper.OnReminderSetListener() {
                @Override
                public void onReminderSet(boolean success, String message) {
                    // Handle reminder set result
                    if (success) {
                        // Optionally update UI to show reminder is set
                        updateReminderButtonState(true);
                    }
                    // Toast is already shown in CalendarHelper, so no need to show again
                }

                @Override
                public void onPermissionRequired() {
                    Toast.makeText(context, "Please grant calendar permissions to set reminders",
                            Toast.LENGTH_LONG).show();
                }
            });
        }

        private void setupReminderListener(Event event) {
            reminderButtonContainer.setOnClickListener(v -> {
                // Create final reference for lambda
                final Event finalEvent = event;

                // Set reminder using calendar helper
                calendarHelper.setEventReminder(finalEvent);

                // Provide visual feedback
                Toast.makeText(context, "Setting up reminder...", Toast.LENGTH_SHORT).show();
            });
        }

        // Add this method to EventViewHolder class
        private void updateReminderButtonState(boolean isSet) {
            // You can update the reminder button appearance here
            // For example, change icon or text color to indicate reminder is set
            if (isSet) {
                // Change to "filled" alarm icon or different color
                ImageView reminderIcon = reminderButtonContainer.findViewById(R.id.reminderIcon);
                if (reminderIcon != null) {
                    reminderIcon.setImageResource(R.drawable.ic_alarm_on); // You'll need this drawable
                    // Or change tint color
                    // ImageViewCompat.setImageTintList(reminderIcon,
                    //     ColorStateList.valueOf(ContextCompat.getColor(context, R.color.reminder_active_color)));
                }
            }
        }

        public void bind(Event event) {
            Log.d(TAG, "Binding event: " + event.getEventTitle());

            // Create a final reference to the event for use in lambda expressions
            final Event finalEvent = event;

            // Set event title
            if (!TextUtils.isEmpty(finalEvent.getEventTitle())) {
                eventTitleTextView.setText(finalEvent.getEventTitle());
            } else {
                eventTitleTextView.setText("Untitled Event");
            }

            // Format and set date
            String formattedDate = formatDate(finalEvent.getEventDate());
            eventDateTextView.setText(formattedDate);

            // Set event location (don't show description in slider)
            if (!TextUtils.isEmpty(finalEvent.getLocation())) {
                eventLocationTextView.setText(finalEvent.getLocation());
                eventLocationTextView.setVisibility(View.VISIBLE);
            } else {
                eventLocationTextView.setText("Location TBD");
            }

            // Load event image
            loadEventImage(finalEvent);

            // Setup click listeners
            setupClickListeners(finalEvent);

            // Setup reaction functionality
            setupReactionListeners(finalEvent);
            loadReactionStats(finalEvent);

            setupReminderListener(event);
        }

        private void setupClickListeners(Event event) {
            // Create final reference for lambda expressions
            final Event finalEvent = event;

            // Image click for full screen view
            eventImageView.setOnClickListener(v -> {
                FullScreenImageDialog imageDialog = new FullScreenImageDialog(context, finalEvent);
                imageDialog.show();

                // Also call the original listener if it exists
                if (onEventActionListener != null) {
                    onEventActionListener.onEventImageClick(finalEvent);
                }
            });

            // Title click for event details
            eventTitleTextView.setOnClickListener(v -> {
                EventDetailsDialog detailsDialog = new EventDetailsDialog(context, finalEvent);
                detailsDialog.show();

                // Also call the original listener if it exists
                if (onEventActionListener != null) {
                    onEventActionListener.onEventClick(finalEvent);
                }
            });

            // Details button click
            viewDetailsContainer.setOnClickListener(v -> {
                EventDetailsDialog detailsDialog = new EventDetailsDialog(context, finalEvent);
                detailsDialog.show();

                // Also call the original listener if it exists
                if (onEventActionListener != null) {
                    onEventActionListener.onEventDetailsClick(finalEvent);
                }
            });

            // Card click for event details
            itemView.setOnClickListener(v -> {
                EventDetailsDialog detailsDialog = new EventDetailsDialog(context, finalEvent);
                detailsDialog.show();

                // Also call the original listener if it exists
                if (onEventActionListener != null) {
                    onEventActionListener.onEventClick(finalEvent);
                }
            });
        }

        private void setupReactionListeners(Event event) {
            // Create final reference for lambda expressions
            final Event finalEvent = event;

            likeButtonContainer.setOnClickListener(v -> {
                if (!isLoadingReaction) {
                    handleReactionClick(finalEvent, ReactionType.LIKE);
                }
            });

            dislikeButtonContainer.setOnClickListener(v -> {
                if (!isLoadingReaction) {
                    handleReactionClick(finalEvent, ReactionType.DISLIKE);
                }
            });
        }

        private void handleReactionClick(Event event, ReactionType reactionType) {
            String authToken = sharedPrefs.getString("auth_token", "");
            if (authToken.isEmpty() || currentUserId == -1L) {
                Toast.makeText(context, "Please login to react to events", Toast.LENGTH_SHORT).show();
                return;
            }

            isLoadingReaction = true;
            updateReactionButtonsLoadingState(true);

            // Check if user is toggling the same reaction
            boolean isTogglingSame;
            if (currentStats != null && currentStats.hasUserReacted()) {
                String currentUserReaction = currentStats.getUserReaction();
                isTogglingSame = currentUserReaction != null &&
                        currentUserReaction.equalsIgnoreCase(reactionType.name());
            } else {
                isTogglingSame = false;
            }

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

        private void loadReactionStats(Event event) {
            Log.d(TAG, "Loading reaction stats for event: " + event.getIdAsLong());

            String url = REACTION_BASE_URL + event.getIdAsLong() + "/reactions/stats";

            JSONObject jsonBody = null;

            // Include userId in request body only if available
            if (currentUserId != -1L) {
                UserIdRequest userIdRequest = new UserIdRequest(currentUserId);
                Gson gson = new Gson();
                String json = gson.toJson(userIdRequest);
                try {
                    jsonBody = new JSONObject(json);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    url,
                    jsonBody,
                    response -> {
                        Log.d(TAG, "Reaction stats response: " + response.toString());
                        handleReactionStatsResponse(response);
                    },
                    error -> {
                        Log.e(TAG, "Error loading reaction stats", error);
                        currentStats = new ReactionStats(0, 0);
                        updateReactionUI();
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    headers.put("Accept", "application/json");

                    String token = sharedPrefs.getString("auth_token", "");
                    if (!token.isEmpty()) {
                        headers.put("Authorization", "Bearer " + token);
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

            Log.d(TAG, "Updating reaction UI - User reaction: " +
                    (currentStats.hasUserReacted() ? currentStats.getUserReaction() : "No reaction"));

            // Update counts
            likeCountText.setText(String.valueOf(currentStats.getLikeCount()));
            dislikeCountText.setText(String.valueOf(currentStats.getDislikeCount()));

            // Update button states based on user's reaction
            updateReactionButtonStates();
        }

        private void updateReactionButtonStates() {
            // Reset to default state first
            likeIcon.setImageResource(R.drawable.ic_thumb_up_outline);
            dislikeIcon.setImageResource(R.drawable.ic_thumb_down_outline);

            // Reset colors to default
            ImageViewCompat.setImageTintList(likeIcon, null);
            ImageViewCompat.setImageTintList(dislikeIcon, null);

            // Update based on user's current reaction
            if (currentStats != null && currentStats.hasUserReacted()) {
                if (currentStats.hasUserLiked()) {
                    // User has liked - show filled like icon
                    likeIcon.setImageResource(R.drawable.ic_thumb_up_filled);
                    ImageViewCompat.setImageTintList(likeIcon,
                            ColorStateList.valueOf(ContextCompat.getColor(context, R.color.like_active_color)));
                } else if (currentStats.hasUserDisliked()) {
                    // User has disliked - show filled dislike icon
                    dislikeIcon.setImageResource(R.drawable.ic_thumb_down_filled);
                    ImageViewCompat.setImageTintList(dislikeIcon,
                            ColorStateList.valueOf(ContextCompat.getColor(context, R.color.dislike_active_color)));
                }
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
                SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                Date date = inputFormat.parse(dateString);
                return outputFormat.format(date);
            } catch (ParseException e) {
                Log.e(TAG, "Failed to parse date: " + dateString, e);
                return dateString; // Return original if parsing fails
            }
        }
    }
}