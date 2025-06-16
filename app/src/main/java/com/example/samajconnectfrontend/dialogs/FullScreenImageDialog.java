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

import androidx.annotation.NonNull;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.samajconnectfrontend.R;
import com.example.samajconnectfrontend.models.Event;
import com.github.chrisbanes.photoview.PhotoView;

public class FullScreenImageDialog extends Dialog {

    private static final String TAG = "FullScreenImageDialog";

    private Context context;
    private Event event;

    // UI Elements
    private PhotoView photoView;
    private ImageView closeButton;
    private View dialogView;

    public FullScreenImageDialog(@NonNull Context context, Event event) {
        super(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        this.context = context;
        this.event = event;

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        initializeDialog();
    }

    private void initializeDialog() {
        dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_fullscreen_image, null);
        setContentView(dialogView);

        // Make dialog fill the screen
        if (getWindow() != null) {
            getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }

        initializeViews(dialogView);
        setupClickListeners();
        loadEventImage();
    }

    private void initializeViews(View dialogView) {
        photoView = dialogView.findViewById(R.id.photoView);
        closeButton = dialogView.findViewById(R.id.closeButton);
    }

    private void setupClickListeners() {
        if (closeButton != null) {
            closeButton.setOnClickListener(v -> dismiss());
        }

        // Allow closing by tapping outside the image area (optional)
        if (dialogView != null) {
            dialogView.setOnClickListener(v -> dismiss());
        }
    }

    private void loadEventImage() {
        if (event == null || photoView == null) {
            if (photoView != null) {
                photoView.setImageResource(R.drawable.logo_banner);
            }
            return;
        }

        // First try to load from Base64 (from API)
        String base64Image = event.getImageBase64();
        Log.d(TAG, "Base64 image length: " + (base64Image != null ? base64Image.length() : "null"));

        if (base64Image != null && !base64Image.isEmpty()) {
            try {
                // Remove data URL prefix if present
                String cleanBase64 = base64Image;
                if (base64Image.startsWith("data:image")) {
                    cleanBase64 = base64Image.substring(base64Image.indexOf(",") + 1);
                }

                byte[] decodedString = Base64.decode(cleanBase64, Base64.DEFAULT);
                Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                if (decodedBitmap != null) {
                    photoView.setImageBitmap(decodedBitmap);
                    Log.d(TAG, "Successfully loaded Base64 image");
                    return;
                } else {
                    Log.e(TAG, "Decoded bitmap is null");
                }
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Failed to decode base64 image", e);
            } catch (Exception e) {
                Log.e(TAG, "Error processing base64 image", e);
            }
        }

        // Fallback to URL if Base64 fails or is not available
        String imageUrl = event.getImageUrl();
        if (!TextUtils.isEmpty(imageUrl)) {
            Log.d(TAG, "Loading image from URL: " + imageUrl);
            try {
                Glide.with(context)
                        .load(imageUrl)
                        .apply(new RequestOptions()
                                .placeholder(R.drawable.logo_banner)
                                .error(R.drawable.logo_banner))
                        .into(photoView);
            } catch (Exception e) {
                Log.e(TAG, "Error loading image with Glide", e);
                photoView.setImageResource(R.drawable.logo_banner);
            }
        } else {
            // Load default image
            Log.d(TAG, "Loading default image");
            photoView.setImageResource(R.drawable.logo_banner);
        }
    }
}