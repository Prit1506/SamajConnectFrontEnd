package com.example.samajconnectfrontend.adapters;

import com.example.samajconnectfrontend.R;

import android.annotation.SuppressLint;
import android.content.Context;
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

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.example.samajconnectfrontend.models.Event;
import com.google.android.material.button.MaterialButton;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private static final String TAG = "EventAdapter";
    private Context context;
    private List<Event> eventList;
    private boolean isAdmin;
    private OnEventActionListener listener;

    // Interface for handling event actions
    public interface OnEventActionListener {
        void onUpdateEvent(Event event);
        void onDeleteEvent(Event event);
    }

    public EventAdapter(Context context, List<Event> eventList, boolean isAdmin, OnEventActionListener listener) {
        this.context = context;
        this.eventList = eventList;
        this.isAdmin = isAdmin;
        this.listener = listener;
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