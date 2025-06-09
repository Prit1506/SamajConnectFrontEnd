package com.example.samajconnectfrontend;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
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
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CreateEventActivity extends AppCompatActivity {

    private static final String TAG = "CreateEventActivity";
    private static final String BASE_URL = "http://10.0.2.2:8080/api/events";

    // UI Components
    private ImageView backArrow;
    private TextInputEditText etEventTitle, etEventDescription, etEventLocation;
    private TextView tvSelectedDate, tvSelectedTime;
    private Button btnUploadImage, btnCreateEvent;
    private ImageView ivEventImagePreview;
    private CardView cardImagePreview;
    private ImageButton btnRemoveImage;

    // Data variables
    private Calendar selectedDateTime;
    private String selectedImageBase64 = null;
    private Long currentUserId;
    private Long currentSamajId;
    private SharedPreferences sharedPreferences;

    // Date and Time formatters
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
    private SimpleDateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    // Image picker launcher
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    // Volley components
    private RequestQueue requestQueue;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_event);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("SamajConnect", MODE_PRIVATE);

        // Get user data from SharedPreferences
        getUserDataFromPreferences();

        initializeViews();
        setupClickListeners();
        setupImagePicker();
        setupVolley();

        selectedDateTime = Calendar.getInstance();

        // Set initial date and time display
        updateDateDisplay();
        updateTimeDisplay();
    }

    private void getUserDataFromPreferences() {
        currentUserId = sharedPreferences.getLong("user_id", -1);
        currentSamajId = sharedPreferences.getLong("samaj_id", -1);

        Log.d(TAG, "Retrieved from SharedPreferences - User ID: " + currentUserId + ", Samaj ID: " + currentSamajId);

        // Validate that we have the required data
        if (currentUserId == -1 || currentSamajId == -1) {
            Toast.makeText(this, "Error: User data not found. Please log in again.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initializeViews() {
        // Header
        backArrow = findViewById(R.id.back_arrow);

        // Form fields
        etEventTitle = findViewById(R.id.et_event_title);
        etEventDescription = findViewById(R.id.et_event_description);
        etEventLocation = findViewById(R.id.et_event_location);

        // Date and Time
        tvSelectedDate = findViewById(R.id.tv_selected_date);
        tvSelectedTime = findViewById(R.id.tv_selected_time);

        // Image components
        btnUploadImage = findViewById(R.id.btn_upload_image);
        ivEventImagePreview = findViewById(R.id.iv_event_image_preview);
        cardImagePreview = findViewById(R.id.card_image_preview);
        btnRemoveImage = findViewById(R.id.btn_remove_image);

        // Create button
        btnCreateEvent = findViewById(R.id.btn_create_event);

        // Progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Creating event...");
        progressDialog.setCancelable(false);

        // Initially hide image preview
        cardImagePreview.setVisibility(View.GONE);
    }

    private void setupClickListeners() {
        backArrow.setOnClickListener(v -> finish());

        tvSelectedDate.setOnClickListener(v -> showDatePicker());
        tvSelectedTime.setOnClickListener(v -> showTimePicker());

        btnUploadImage.setOnClickListener(v -> showImagePickerDialog());
        btnRemoveImage.setOnClickListener(v -> removeSelectedImage());

        btnCreateEvent.setOnClickListener(v -> createEvent());
    }

    private void setupImagePicker() {
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

    private void setupVolley() {
        // Initialize Volley RequestQueue (reuse if you already have one in your app)
        requestQueue = Volley.newRequestQueue(this);
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDateTime.set(Calendar.YEAR, year);
                    selectedDateTime.set(Calendar.MONTH, month);
                    selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateDateDisplay();
                },
                selectedDateTime.get(Calendar.YEAR),
                selectedDateTime.get(Calendar.MONTH),
                selectedDateTime.get(Calendar.DAY_OF_MONTH)
        );

        // Set minimum date to today
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void showTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selectedDateTime.set(Calendar.MINUTE, minute);
                    selectedDateTime.set(Calendar.SECOND, 0);
                    updateTimeDisplay();
                },
                selectedDateTime.get(Calendar.HOUR_OF_DAY),
                selectedDateTime.get(Calendar.MINUTE),
                false
        );

        timePickerDialog.show();
    }

    private void updateDateDisplay() {
        String formattedDate = dateFormat.format(selectedDateTime.getTime());
        tvSelectedDate.setText(formattedDate);
        tvSelectedDate.setTextColor(getResources().getColor(android.R.color.black, getTheme()));
    }

    private void updateTimeDisplay() {
        String formattedTime = timeFormat.format(selectedDateTime.getTime());
        tvSelectedTime.setText(formattedTime);
        tvSelectedTime.setTextColor(getResources().getColor(android.R.color.black, getTheme()));
    }

    private void showImagePickerDialog() {
        String[] options = {"Camera", "Gallery"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Image")
                .setItems(options, (dialog, which) -> {
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

    private void processCameraBitmap(Bitmap bitmap) {
        try {
            // Compress the camera bitmap
            Bitmap compressedBitmap = compressBitmap(bitmap, 85);

            // Convert to Base64
            selectedImageBase64 = bitmapToBase64(compressedBitmap);

            // Display preview
            ivEventImagePreview.setImageBitmap(compressedBitmap);
            cardImagePreview.setVisibility(View.VISIBLE);
            btnUploadImage.setText("Change Image");

            Log.d(TAG, "Camera image processed successfully, Base64 length: " + selectedImageBase64.length());
        } catch (Exception e) {
            Log.e(TAG, "Error processing camera image", e);
            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show();
        }
    }

    private void processSelectedImage(Uri imageUri) {
        try {
            // Load and compress the image
            Bitmap bitmap = loadAndCompressImage(imageUri);
            if (bitmap != null) {
                // Convert to Base64
                selectedImageBase64 = bitmapToBase64(bitmap);

                // Display preview
                ivEventImagePreview.setImageBitmap(bitmap);
                cardImagePreview.setVisibility(View.VISIBLE);
                btnUploadImage.setText("Change Image");

                Log.d(TAG, "Gallery image processed successfully, Base64 length: " + selectedImageBase64.length());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing image", e);
            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show();
        }
    }

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
    private void removeSelectedImage() {
        selectedImageBase64 = null;
        cardImagePreview.setVisibility(View.GONE);
        btnUploadImage.setText("Add Event Image");
    }

    private void createEvent() {
        if (!validateForm()) {
            return;
        }

        try {
            JSONObject eventJson = createEventJson();
            sendCreateEventRequest(eventJson);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating JSON", e);
            Toast.makeText(this, "Error preparing event data", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean validateForm() {
        String title = etEventTitle.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            etEventTitle.setError("Event title is required");
            etEventTitle.requestFocus();
            return false;
        }

        if (title.length() < 3) {
            etEventTitle.setError("Event title must be at least 3 characters");
            etEventTitle.requestFocus();
            return false;
        }

        // Check if date is in the future
        Calendar now = Calendar.getInstance();
        if (selectedDateTime.getTimeInMillis() <= now.getTimeInMillis()) {
            Toast.makeText(this, "Event date and time must be in the future", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private JSONObject createEventJson() throws JSONException {
        JSONObject json = new JSONObject();

        json.put("eventTitle", etEventTitle.getText().toString().trim());
        json.put("eventDescription", etEventDescription.getText().toString().trim());
        json.put("location", etEventLocation.getText().toString().trim());
        json.put("eventDate", apiDateFormat.format(selectedDateTime.getTime()));
        json.put("createdBy", currentUserId);
        json.put("samajId", currentSamajId);

        if (selectedImageBase64 != null && !selectedImageBase64.isEmpty()) {
            json.put("imageBase64", selectedImageBase64);
        }

        Log.d(TAG, "Event JSON created: " + json.toString());
        return json;
    }

    private void sendCreateEventRequest(JSONObject eventJson) {
        progressDialog.show();

        Log.d(TAG, "Sending request to: " + BASE_URL);
        Log.d(TAG, "Request body: " + eventJson.toString());

        // Create JsonObjectRequest using Volley
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.POST,
                BASE_URL,
                eventJson,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        progressDialog.dismiss();
                        Log.d(TAG, "Response: " + response.toString());

                        boolean success = response.optBoolean("success", false);
                        String message = response.optString("message", "Unknown error");

                        if (success) {
                            Toast.makeText(CreateEventActivity.this, "Event created successfully!", Toast.LENGTH_LONG).show();
                            setResult(RESULT_OK);
                            finish();
                        } else {
                            Toast.makeText(CreateEventActivity.this, "Error: " + message, Toast.LENGTH_LONG).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        progressDialog.dismiss();
                        Log.e(TAG, "Volley error", error);

                        String errorMessage = "Network error occurred";

                        if (error.networkResponse != null) {
                            int statusCode = error.networkResponse.statusCode;
                            errorMessage = "Server error (" + statusCode + ")";

                            // Try to parse error response
                            try {
                                String responseBody = new String(error.networkResponse.data, "utf-8");
                                JSONObject errorJson = new JSONObject(responseBody);
                                errorMessage = errorJson.optString("message", errorMessage);
                            } catch (Exception e) {
                                // Use default error message
                            }
                        } else if (error.getCause() != null) {
                            errorMessage = "Connection error: " + error.getCause().getMessage();
                        }

                        Toast.makeText(CreateEventActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        // Set retry policy for large payloads (Base64 images)
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                30000, // 30 seconds timeout
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        // Add request to queue
        requestQueue.add(jsonObjectRequest);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}