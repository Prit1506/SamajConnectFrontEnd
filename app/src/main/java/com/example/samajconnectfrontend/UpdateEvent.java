package com.example.samajconnectfrontend;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class UpdateEvent extends AppCompatActivity {

    private static final String TAG = "UpdateEvent";
    private static final String EVENT_URL = "http://10.0.2.2:8080/api/events/";

    // UI Components
    private ImageView eventImagePreview;
    private FloatingActionButton changeImageButton;
    private TextInputEditText eventTitleEditText;
    private TextInputEditText eventDescriptionEditText;
    private TextInputEditText eventLocationEditText;
    private TextInputEditText eventDateEditText;
    private TextInputEditText eventTimeEditText;
    private MaterialButton cancelButton;
    private MaterialButton saveChangesButton;

    // Data
    private long eventId;
    private String selectedImageBase64;
    private String originalImageUrl;
    private Calendar selectedCalendar;
    private SharedPreferences sharedPrefs;
    private RequestQueue requestQueue;
    private boolean hasNewImage = false;
    private ImageView backArrow;

    // Activity Result Launcher (combined for both camera and gallery)
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_update_event);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        try {
            initializeComponents();
            setupActivityResultLaunchers();
            getIntentData();
            setupClickListeners();
            loadExistingImage();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error initializing update screen", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializeComponents() {
        // Initialize UI components
        eventImagePreview = findViewById(R.id.eventImagePreview);
        changeImageButton = findViewById(R.id.changeImageButton);
        eventTitleEditText = findViewById(R.id.eventTitleEditText);
        eventDescriptionEditText = findViewById(R.id.eventDescriptionEditText);
        eventLocationEditText = findViewById(R.id.eventLocationEditText);
        eventDateEditText = findViewById(R.id.eventDateEditText);
        eventTimeEditText = findViewById(R.id.eventTimeEditText);
        cancelButton = findViewById(R.id.cancelButton);
        saveChangesButton = findViewById(R.id.saveChangesButton);

        // ADD THIS LINE - Initialize back arrow
        backArrow = findViewById(R.id.back_arrow);

        // Initialize other components
        sharedPrefs = getSharedPreferences("SamajConnect", MODE_PRIVATE);
        requestQueue = Volley.newRequestQueue(this);
        selectedCalendar = Calendar.getInstance();

        Log.d(TAG, "Components initialized successfully");
    }
    private void setupActivityResultLaunchers() {
        // Combined image picker launcher (similar to CreateEventActivity)
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        Uri imageUri = null;

                        // Handle camera result (bitmap in extras) or gallery result (data URI)
                        if (data.getExtras() != null && data.getExtras().containsKey("data")) {
                            // Camera result
                            Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                            if (bitmap != null) {
                                processCameraBitmap(bitmap);
                            }
                        } else if (data.getData() != null) {
                            // Gallery result
                            imageUri = data.getData();
                            processSelectedImage(imageUri);
                        }
                    }
                }
        );
    }

    private void getIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            eventId = intent.getLongExtra("event_id", -1);
            String eventTitle = intent.getStringExtra("event_title");
            String eventDescription = intent.getStringExtra("event_description");
            String eventLocation = intent.getStringExtra("event_location");
            String eventDate = intent.getStringExtra("event_date");
            String eventTime = intent.getStringExtra("event_time");
            originalImageUrl = intent.getStringExtra("event_image_url");
            // **ADD THIS LINE** to get Base64 image data
            String base64Image = intent.getStringExtra("event_image_base64");

            Log.d(TAG, "Received event data - ID: " + eventId + ", Title: " + eventTitle);
            Log.d(TAG, "Base64 image available: " + (base64Image != null && !base64Image.isEmpty()));

            // Populate the fields
            if (!TextUtils.isEmpty(eventTitle)) {
                eventTitleEditText.setText(eventTitle);
            }

            if (!TextUtils.isEmpty(eventDescription)) {
                eventDescriptionEditText.setText(eventDescription);
            }

            if (!TextUtils.isEmpty(eventLocation)) {
                eventLocationEditText.setText(eventLocation);
            }

            // Parse and set date and time
            if (!TextUtils.isEmpty(eventDate)) {
                parseAndSetDateTime(eventDate, eventTime);
            }
        } else {
            Log.e(TAG, "No intent data received");
            Toast.makeText(this, "Error: No event data received", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadExistingImage() {
        // Get the Base64 image from intent if available
        String base64Image = getIntent().getStringExtra("event_image_base64");

        Log.d(TAG, "Base64 image length: " + (base64Image != null ? base64Image.length() : "null"));
        Log.d(TAG, "Image URL: " + originalImageUrl);

        // First try to load from Base64 (from API)
        if (base64Image != null && !base64Image.isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                if (decodedBitmap != null) {
                    eventImagePreview.setImageBitmap(decodedBitmap);
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
        if (!TextUtils.isEmpty(originalImageUrl)) {
            Log.d(TAG, "Loading image from URL: " + originalImageUrl);

            // If you want to use Glide instead of Picasso, replace this section
            try {
                Picasso.get()
                        .load(originalImageUrl)
                        .placeholder(R.drawable.placeholder_image) // Add a placeholder drawable
                        .error(R.drawable.placeholder_image) // Use same placeholder for error
                        .into(eventImagePreview);
            } catch (Exception e) {
                Log.e(TAG, "Error loading image with Picasso", e);
                // Fallback: set a default placeholder
                eventImagePreview.setImageResource(R.drawable.placeholder_image);
            }
        } else {
            // Set default placeholder when no image exists
            Log.d(TAG, "Loading default placeholder image");
            eventImagePreview.setImageResource(R.drawable.placeholder_image);
        }
    }

    private void parseAndSetDateTime(String dateTimeString, String timeString) {
        try {
            // Parse the date from the API format (assuming "yyyy-MM-dd HH:mm:ss")
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date date = inputFormat.parse(dateTimeString);

            if (date != null) {
                selectedCalendar.setTime(date);

                // Format date for display
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                eventDateEditText.setText(dateFormat.format(date));

                // Format time for display
                SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                eventTimeEditText.setText(timeFormat.format(date));
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date: " + dateTimeString, e);
            // Set current date as fallback
            setCurrentDateTime();
        }
    }

    private void setCurrentDateTime() {
        selectedCalendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

        eventDateEditText.setText(dateFormat.format(selectedCalendar.getTime()));
        eventTimeEditText.setText(timeFormat.format(selectedCalendar.getTime()));
    }

    // Add this in your setupClickListeners() method

    private void setupClickListeners() {
        // ADD THIS - Back arrow click listener
        backArrow.setOnClickListener(v -> {
            Log.d(TAG, "Back arrow clicked");
            navigateToEventActivity();
        });

        // Cancel button
        cancelButton.setOnClickListener(v -> {
            Log.d(TAG, "Cancel button clicked");
            navigateToEventActivity();
        });

        // Save changes button
        saveChangesButton.setOnClickListener(v -> {
            Log.d(TAG, "Save changes button clicked");
            if (validateInputs()) {
                updateEvent();
            }
        });

        // Change image button
        changeImageButton.setOnClickListener(v -> showImagePickerDialog());

        // Date picker
        eventDateEditText.setOnClickListener(v -> showDatePicker());

        // Time picker
        eventTimeEditText.setOnClickListener(v -> showTimePicker());
    }
    private void navigateToEventActivity() {
        Intent intent = new Intent(UpdateEvent.this, EventActivity.class);

        // Optional: Pass any necessary data back to EventActivity
        // You might want to pass the samaj_id or other relevant data
        Long samajId = sharedPrefs.getLong("samaj_id", -1);
        if (samajId != -1) {
            intent.putExtra("samaj_id", samajId);
        }

        // Clear the task stack and start EventActivity
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        startActivity(intent);
        finish(); // Close UpdateEvent activity
    }

    private void showImagePickerDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Select Image")
                .setItems(new String[]{"Camera", "Gallery"}, (dialog, which) -> {
                    if (which == 0) {
                        openCamera();
                    } else {
                        openGallery();
                    }
                })
                .show();
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            imagePickerLauncher.launch(cameraIntent);
        } else {
            Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryIntent.setType("image/*");
        imagePickerLauncher.launch(galleryIntent);
    }

    // Use the same image processing methods as CreateEventActivity
    private void processCameraBitmap(Bitmap bitmap) {
        try {
            // Compress the camera bitmap
            Bitmap compressedBitmap = compressBitmap(bitmap, 85);

            // Convert to Base64 using the same method as CreateEventActivity
            selectedImageBase64 = bitmapToBase64(compressedBitmap);
            hasNewImage = true;

            // Display preview
            eventImagePreview.setImageBitmap(compressedBitmap);

            Log.d(TAG, "Camera image processed successfully, Base64 length: " + selectedImageBase64.length());
        } catch (Exception e) {
            Log.e(TAG, "Error processing camera image", e);
            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show();
        }
    }

    private void processSelectedImage(Uri imageUri) {
        try {
            // Load and compress the image using the same method as CreateEventActivity
            Bitmap bitmap = loadAndCompressImage(imageUri);
            if (bitmap != null) {
                // Convert to Base64
                selectedImageBase64 = bitmapToBase64(bitmap);
                hasNewImage = true;

                // Display preview
                eventImagePreview.setImageBitmap(bitmap);

                Log.d(TAG, "Gallery image processed successfully, Base64 length: " + selectedImageBase64.length());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing image", e);
            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show();
        }
    }

    // Copy the exact image processing methods from CreateEventActivity
    private Bitmap loadAndCompressImage(Uri imageUri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(imageUri);
        if (inputStream == null) return null;

        // First, get image dimensions
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(inputStream, null, options);
        inputStream.close();

        // Calculate sample size for compression
        int maxSize = 1024; // Max width/height
        int sampleSize = 1;
        while (options.outWidth / sampleSize > maxSize || options.outHeight / sampleSize > maxSize) {
            sampleSize *= 2;
        }

        // Load the actual bitmap
        inputStream = getContentResolver().openInputStream(imageUri);
        options.inJustDecodeBounds = false;
        options.inSampleSize = sampleSize;
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
        inputStream.close();

        if (bitmap != null) {
            // Handle rotation
            bitmap = handleImageRotation(imageUri, bitmap);

            // Further compress if still too large
            if (bitmap.getByteCount() > 500 * 1024) { // 500KB
                bitmap = compressBitmap(bitmap, 80);
            }
        }

        return bitmap;
    }

    private Bitmap handleImageRotation(Uri imageUri, Bitmap bitmap) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            if (inputStream != null) {
                ExifInterface exif = new ExifInterface(inputStream);
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                inputStream.close();

                Matrix matrix = new Matrix();
                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        matrix.postRotate(90);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        matrix.postRotate(180);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        matrix.postRotate(270);
                        break;
                }

                if (!matrix.isIdentity()) {
                    Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                    bitmap.recycle();
                    return rotatedBitmap;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error handling image rotation", e);
        }
        return bitmap;
    }

    private Bitmap compressBitmap(Bitmap bitmap, int quality) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
        byte[] byteArray = stream.toByteArray();
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
    }

    // Use the exact same Base64 conversion method as CreateEventActivity
    private String bitmapToBase64(Bitmap bitmap) {
        if (bitmap == null) {
            Log.w(TAG, "Bitmap is null, cannot convert to Base64");
            return null;
        }

        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream);
            byte[] byteArray = stream.toByteArray();

            // Use NO_WRAP to avoid newline characters that break JSON
            String base64String = Base64.encodeToString(byteArray, Base64.NO_WRAP);

            Log.d(TAG, "Base64 conversion successful, length: " + base64String.length());
            return base64String;

        } catch (Exception e) {
            Log.e(TAG, "Error converting bitmap to Base64", e);
            return null;
        }
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedCalendar.set(Calendar.YEAR, year);
                    selectedCalendar.set(Calendar.MONTH, month);
                    selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                    eventDateEditText.setText(dateFormat.format(selectedCalendar.getTime()));
                },
                selectedCalendar.get(Calendar.YEAR),
                selectedCalendar.get(Calendar.MONTH),
                selectedCalendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void showTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    selectedCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selectedCalendar.set(Calendar.MINUTE, minute);

                    SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                    eventTimeEditText.setText(timeFormat.format(selectedCalendar.getTime()));
                },
                selectedCalendar.get(Calendar.HOUR_OF_DAY),
                selectedCalendar.get(Calendar.MINUTE),
                false
        );

        timePickerDialog.show();
    }

    private boolean validateInputs() {
        String title = eventTitleEditText.getText().toString().trim();
        String description = eventDescriptionEditText.getText().toString().trim();
        String location = eventLocationEditText.getText().toString().trim();
        String date = eventDateEditText.getText().toString().trim();
        String time = eventTimeEditText.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            eventTitleEditText.setError("Title is required");
            eventTitleEditText.requestFocus();
            return false;
        }

        if (title.length() < 3) {
            eventTitleEditText.setError("Event title must be at least 3 characters");
            eventTitleEditText.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(description)) {
            eventDescriptionEditText.setError("Description is required");
            eventDescriptionEditText.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(location)) {
            eventLocationEditText.setError("Location is required");
            eventLocationEditText.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(date) || TextUtils.isEmpty(time)) {
            Toast.makeText(this, "Please select date and time", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Check if date is in the future
        Calendar now = Calendar.getInstance();
        if (selectedCalendar.getTimeInMillis() <= now.getTimeInMillis()) {
            Toast.makeText(this, "Event date and time must be in the future", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void updateEvent() {
        String title = eventTitleEditText.getText().toString().trim();
        String description = eventDescriptionEditText.getText().toString().trim();
        String location = eventLocationEditText.getText().toString().trim();

        // Format date time for API (same format as CreateEventActivity)
        SimpleDateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String formattedDateTime = apiDateFormat.format(selectedCalendar.getTime());

        // Get user data from SharedPreferences
        Long currentUserId = sharedPrefs.getLong("user_id", -1);
        Long currentSamajId = sharedPrefs.getLong("samaj_id", -1);

        // Prepare JSON data (same structure as CreateEventActivity)
        JSONObject eventData = new JSONObject();
        try {
            eventData.put("eventTitle", title);
            eventData.put("eventDescription", description);
            eventData.put("location", location);
            eventData.put("eventDate", formattedDateTime);
            eventData.put("createdBy", currentUserId);
            eventData.put("samajId", currentSamajId);

            // Add image data only if a new image was selected
            if (hasNewImage && !TextUtils.isEmpty(selectedImageBase64)) {
                eventData.put("imageBase64", selectedImageBase64);
                Log.d(TAG, "Adding new image to update request, Base64 length: " + selectedImageBase64.length());
            }

            Log.d(TAG, "Update data prepared: " + eventData.toString());

        } catch (JSONException e) {
            Log.e(TAG, "Error creating JSON data", e);
            Toast.makeText(this, "Error preparing data", Toast.LENGTH_SHORT).show();
            return;
        }

        // Make API call
        String url = EVENT_URL + eventId;
        Log.d(TAG, "Update URL: " + url);

        // Show loading state
        saveChangesButton.setEnabled(false);
        saveChangesButton.setText("Updating...");

        JsonObjectRequest updateRequest = new JsonObjectRequest(
                Request.Method.PUT,
                url,
                eventData,
                response -> {
                    Log.d(TAG, "Event updated successfully: " + response.toString());
                    Toast.makeText(UpdateEvent.this, "Event updated successfully", Toast.LENGTH_SHORT).show();

                    // Set result and finish
                    setResult(RESULT_OK);
                    finish();
                },
                error -> {
                    Log.e(TAG, "Error updating event", error);
                    String errorMessage = "Failed to update event";

                    if (error.networkResponse != null) {
                        int statusCode = error.networkResponse.statusCode;
                        String responseData = new String(error.networkResponse.data);
                        Log.e(TAG, "Status Code: " + statusCode);
                        Log.e(TAG, "Response Data: " + responseData);

                        try {
                            JSONObject errorJson = new JSONObject(responseData);
                            errorMessage = errorJson.optString("message", errorMessage);
                        } catch (JSONException e) {
                            // Use default error message
                        }

                        if (statusCode == 401) {
                            errorMessage = "Unauthorized - Please login again";
                        } else if (statusCode == 404) {
                            errorMessage = "Event not found";
                        } else if (statusCode >= 500) {
                            errorMessage = "Server error - Please try again later";
                        }
                    }

                    Toast.makeText(UpdateEvent.this, errorMessage, Toast.LENGTH_LONG).show();

                    // **FIX 1: Restore button state on failure**
                    runOnUiThread(() -> {
                        saveChangesButton.setEnabled(true);
                        saveChangesButton.setText("Save Changes");
                    });
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        // Set retry policy for large payloads (Base64 images) - same as CreateEventActivity
        updateRequest.setRetryPolicy(new DefaultRetryPolicy(
                30000, // 30 seconds timeout
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        // Add to request queue
        requestQueue.add(updateRequest);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (requestQueue != null) {
            requestQueue.cancelAll(TAG);
        }
    }
    @Override
    public void onBackPressed() {
        Log.d(TAG, "System back button pressed");
        navigateToEventActivity();
    }
}