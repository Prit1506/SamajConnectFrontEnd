package com.example.samajconnectfrontend.dialogs;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.example.samajconnectfrontend.R;
import com.example.samajconnectfrontend.models.Event;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
public class EventDetailsDialog extends Dialog {

    private static final String TAG = "EventDetailsDialog";

    private Context context;
    private Event event;

    // UI Elements
    private ImageView eventImageView;
    private ImageView closeButton;
    private ImageView fullscreenButton;
    private TextView eventTitleTextView;
    private TextView eventDateTextView;
    private TextView eventLocationTextView;
    private TextView eventDescriptionTextView;
    private TextView eventTimeTextView;

    public EventDetailsDialog(@NonNull Context context, Event event) {
        super(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        this.context = context;
        this.event = event;

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        initializeDialog();
    }

    private void initializeDialog() {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_event_details, null);
        setContentView(dialogView);

        // Make dialog fill the screen
        if (getWindow() != null) {
            getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }

        initializeViews(dialogView);
        setupClickListeners();
        populateEventData();
    }

    private void initializeViews(View dialogView) {
        eventImageView = dialogView.findViewById(R.id.eventImageView);
        closeButton = dialogView.findViewById(R.id.closeButton);
        fullscreenButton = dialogView.findViewById(R.id.fullscreenButton);
        eventTitleTextView = dialogView.findViewById(R.id.eventTitleTextView);
        eventDateTextView = dialogView.findViewById(R.id.eventDateTextView);
        eventLocationTextView = dialogView.findViewById(R.id.eventLocationTextView);
        eventDescriptionTextView = dialogView.findViewById(R.id.eventDescriptionTextView);
        eventTimeTextView = dialogView.findViewById(R.id.eventTimeTextView);
    }

    private void setupClickListeners() {
        closeButton.setOnClickListener(v -> dismiss());

        fullscreenButton.setOnClickListener(v -> {
            FullScreenImageDialog imageDialog = new FullScreenImageDialog(context, event);
            imageDialog.show();
        });

        // Also allow clicking on image to open fullscreen
        eventImageView.setOnClickListener(v -> {
            FullScreenImageDialog imageDialog = new FullScreenImageDialog(context, event);
            imageDialog.show();
        });
    }

    private void populateEventData() {
        if (event == null) return;

        // Set event title
        if (!TextUtils.isEmpty(event.getEventTitle())) {
            eventTitleTextView.setText(event.getEventTitle());
        } else {
            eventTitleTextView.setText("Untitled Event");
        }

        // Set event description
        if (!TextUtils.isEmpty(event.getDescription())) {
            eventDescriptionTextView.setText(event.getDescription());
            eventDescriptionTextView.setVisibility(View.VISIBLE);
        } else {
            eventDescriptionTextView.setText("No description available");
        }

        // Set event location
        if (!TextUtils.isEmpty(event.getLocation())) {
            eventLocationTextView.setText(event.getLocation());
        } else {
            eventLocationTextView.setText("Location TBD");
        }

        // Format and set date and time
        String[] dateTime = formatDateTime(event.getEventDate());
        eventDateTextView.setText(dateTime[0]);
        eventTimeTextView.setText(dateTime[1]);

        // Load event image
        loadEventImage();
    }

    private void loadEventImage() {
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

    private String[] formatDateTime(String dateString) {
        String[] result = new String[2];

        if (TextUtils.isEmpty(dateString)) {
            result[0] = "Date TBD";
            result[1] = "Time TBD";
            return result;
        }

        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

            Date date = inputFormat.parse(dateString);
            result[0] = dateFormat.format(date);
            result[1] = timeFormat.format(date);

        } catch (ParseException e) {
            Log.e(TAG, "Failed to parse date: " + dateString, e);
            result[0] = dateString;
            result[1] = "";
        }

        return result;
    }
}
