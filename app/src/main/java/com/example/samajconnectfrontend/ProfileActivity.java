package com.example.samajconnectfrontend;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAMERA_REQUEST = 2;
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int STORAGE_PERMISSION_CODE = 101;

    // UI Components
    private ImageView profileImage, backArrow;
    private TextView emailTextView, adminStatusTextView, samajIdTextView;
    private EditText nameEditText, addressEditText, phoneEditText;
    private Button changeImageButton, saveButton, forgotPasswordButton;

    // Data variables
    private RequestQueue requestQueue;
    private Long userId;
    private String baseUrl = "http://10.0.2.2:8080/api"; // Replace with your server URL
    private boolean hasImageChanged = false;
    private String encodedImageString = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();
        setupClickListeners();

        requestQueue = Volley.newRequestQueue(this);

        // Get user ID from intent or SharedPreferences
        userId = getSharedPreferences("SamajConnect", MODE_PRIVATE).getLong("user_id", -1);

        if (userId != -1) {
            loadUserProfile();
        } else {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializeViews() {
        // Header components
        backArrow = findViewById(R.id.back_arrow);

        // Profile components
        profileImage = findViewById(R.id.profile_image);
        emailTextView = findViewById(R.id.email_text_view);
        adminStatusTextView = findViewById(R.id.admin_status_text_view);
        samajIdTextView = findViewById(R.id.samaj_id_text_view);
        nameEditText = findViewById(R.id.name_edit_text);
        addressEditText = findViewById(R.id.address_edit_text);
        phoneEditText = findViewById(R.id.phone_edit_text);
        changeImageButton = findViewById(R.id.change_image_button);
        saveButton = findViewById(R.id.save_button);
        forgotPasswordButton = findViewById(R.id.forgot_password_button);
    }

    private void setupClickListeners() {
        // Header back button
        backArrow.setOnClickListener(v -> onBackPressed());

        // Profile buttons
        changeImageButton.setOnClickListener(v -> showImagePickerDialog());
        saveButton.setOnClickListener(v -> saveProfile());
        forgotPasswordButton.setOnClickListener(v -> openForgotPasswordActivity());
    }

    private void loadUserProfile() {
        String url = baseUrl + "/users/" + userId;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if (response.getBoolean("success")) {
                                JSONObject userData = response.getJSONObject("user");
                                populateUserData(userData);
                            } else {
                                String message = response.getString("message");
                                Toast.makeText(ProfileActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "JSON parsing error: " + e.getMessage());
                            Toast.makeText(ProfileActivity.this, "Error parsing user data", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Error loading profile: " + error.getMessage());
                        Toast.makeText(ProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        requestQueue.add(request);
    }

    private void populateUserData(JSONObject userData) throws JSONException {
        // Populate editable fields
        nameEditText.setText(userData.optString("name", ""));
        addressEditText.setText(userData.optString("address", ""));
        phoneEditText.setText(userData.optString("phoneNumber", ""));

        // Populate non-editable fields
        emailTextView.setText(userData.optString("email", ""));

        boolean isAdmin = userData.optBoolean("isAdmin", false);
        adminStatusTextView.setText(isAdmin ? "Admin" : "Member");

        // Handle samaj information
        if (userData.has("samaj") && !userData.isNull("samaj")) {
            JSONObject samajData = userData.getJSONObject("samaj");
            samajIdTextView.setText(samajData.optString("name", "Unknown Samaj"));
        } else {
            samajIdTextView.setText("No Samaj");
        }

        // Load profile image
        if (userData.has("profileImg") && !userData.isNull("profileImg")) {
            String base64Image = userData.getString("profileImg");
            loadImageFromBase64(base64Image);
        } else {
            // Set default profile image
            profileImage.setImageResource(R.drawable.profile);
        }
    }

    private void loadImageFromBase64(String base64String) {
        try {
            byte[] decodedString = Base64.decode(base64String, Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            profileImage.setImageBitmap(decodedByte);
        } catch (Exception e) {
            Log.e(TAG, "Error decoding image: " + e.getMessage());
            profileImage.setImageResource(R.drawable.profile);
        }
    }

    private void showImagePickerDialog() {
        String[] options = {"Camera", "Gallery"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Image Source");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                checkCameraPermissionAndOpen();
            } else {
                checkStoragePermissionAndOpen();
            }
        });
        builder.show();
    }

    private void checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            openCamera();
        }
    }

    private void checkStoragePermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        STORAGE_PERMISSION_CODE);
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        STORAGE_PERMISSION_CODE);
            }

        } else {
            openGallery();
        }
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, CAMERA_REQUEST);
        }
    }

    private void openGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            Bitmap bitmap = null;

            if (requestCode == PICK_IMAGE_REQUEST && data != null && data.getData() != null) {
                Uri uri = data.getData();
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                } catch (IOException e) {
                    Log.e(TAG, "Error getting image from gallery: " + e.getMessage());
                }
            } else if (requestCode == CAMERA_REQUEST && data != null) {
                Bundle extras = data.getExtras();
                if (extras != null) {
                    bitmap = (Bitmap) extras.get("data");
                }
            }

            if (bitmap != null) {
                // Resize bitmap to reduce size
                bitmap = resizeBitmap(bitmap, 400, 400);
                profileImage.setImageBitmap(bitmap);
                encodedImageString = encodeImageToBase64(bitmap);
                hasImageChanged = true;
                Log.d(TAG, "Image selected and encoded");
                Log.d(TAG, "Encoded Image Length: " + encodedImageString.length());
                Log.d(TAG, "Has Image Changed: " + hasImageChanged);
            }
        }
    }

    private Bitmap resizeBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float scaleWidth = ((float) maxWidth) / width;
        float scaleHeight = ((float) maxHeight) / height;
        float scale = Math.min(scaleWidth, scaleHeight);

        int newWidth = Math.round(width * scale);
        int newHeight = Math.round(height * scale);

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    private String encodeImageToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.NO_WRAP);
    }

    private void saveProfile() {
        String name = nameEditText.getText().toString().trim();
        String address = addressEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();

        if (name.isEmpty()) {
            nameEditText.setError("Name is required");
            return;
        }

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("name", name);
            requestBody.put("address", address);
            requestBody.put("phoneNumber", phone);

            if (hasImageChanged && !encodedImageString.isEmpty()) {
                requestBody.put("imageBase64", encodedImageString);
            }
            Log.d(TAG, "Request Body: " + requestBody);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating request body: " + e.getMessage());
            return;
        }

        String url = baseUrl + "/users/" + userId + "/profile";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                requestBody,
                response -> {
                    try {
                        if (response.getBoolean("success")) {
                            Toast.makeText(ProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                            hasImageChanged = false; // Reset flag
                        } else {
                            String message = response.getString("message");
                            Toast.makeText(ProfileActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error: " + e.getMessage());
                        Toast.makeText(ProfileActivity.this, "Error updating profile", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e(TAG, "Error updating profile: " + error.getMessage());
                    Toast.makeText(ProfileActivity.this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                }
        );

        request.setRetryPolicy(new DefaultRetryPolicy(150000, 0, 1.0f));
        requestQueue.add(request);
    }

    private void openForgotPasswordActivity() {
        Intent intent = new Intent(this, ForgotPasswordActivity.class);
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == CAMERA_PERMISSION_CODE) {
                openCamera();
            } else if (requestCode == STORAGE_PERMISSION_CODE) {
                openGallery();
            }
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
        }
    }
}