package com.example.samajconnectfrontend;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.example.samajconnectfrontend.dialogs.FullScreenImageDialog;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.*;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MembersActivity extends AppCompatActivity {

    private static final String TAG = "MembersActivity";
    private static final String BASE_URL = "http://10.0.2.2:8080/api/family-tree";
    private static final String USER_BASE_URL = "http://10.0.2.2:8080/api/users";
    private static final String PREFS_NAME = "SamajConnect";
    private static final String USER_ID_KEY = "user_id";

    // UI Components
    private Button btnFamilyTree, btnAddRelationship, btnApproveRequests;
    private FrameLayout contentFrame;
    private RequestQueue requestQueue;
    private ProgressDialog progressDialog;
    private SharedPreferences sharedPreferences;
    private Long currentUserId; // The logged-in user
    private Long currentTreeOwnerId; // The user whose tree we're currently viewing
    private UserInfo currentTreeOwner; // Information about the tree owner

    // Current view state
    private String currentView = "family_tree";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_members);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeComponents();
        setupClickListeners();
        loadFamilyTreeView();
    }

    private void initializeComponents() {
        btnFamilyTree = findViewById(R.id.btnFamilyTree);
        btnAddRelationship = findViewById(R.id.btnAddRelationship);
        btnApproveRequests = findViewById(R.id.btnApproveRequests);
        contentFrame = findViewById(R.id.contentFrame);

        requestQueue = Volley.newRequestQueue(this);
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        currentUserId = sharedPreferences.getLong(USER_ID_KEY, 1L); // The logged-in user
        currentTreeOwnerId = currentUserId; // Initially viewing own tree

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading...");
        progressDialog.setCancelable(false);
    }

    private void setupClickListeners() {
        btnFamilyTree.setOnClickListener(v -> {
            updateButtonStates("family_tree");
            // Reset to viewing own tree when clicking Family Tree button
            currentTreeOwnerId = currentUserId;
            loadFamilyTreeView();
        });

        btnAddRelationship.setOnClickListener(v -> {
            updateButtonStates("add_relationship");
            loadAddRelationshipView();
        });

        btnApproveRequests.setOnClickListener(v -> {
            updateButtonStates("approve_requests");
            loadApproveRequestsView();
        });

        ImageView backArrow = findViewById(R.id.back_arrow);
        backArrow.setOnClickListener(v -> onBackPressed());
    }
    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed called");
        super.onBackPressed();
    }
    private void updateButtonStates(String activeView) {
        currentView = activeView;

        // Reset all buttons
        btnFamilyTree.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        btnAddRelationship.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        btnApproveRequests.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));

        // Highlight active button
        switch (activeView) {
            case "family_tree":
                btnFamilyTree.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
                break;
            case "add_relationship":
                btnAddRelationship.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
                break;
            case "approve_requests":
                btnApproveRequests.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
                break;
        }
    }

    // ==================== FAMILY TREE VIEW ====================

    private void loadFamilyTreeView() {
        View familyTreeView = LayoutInflater.from(this).inflate(R.layout.view_family_tree, null);
        contentFrame.removeAllViews();
        contentFrame.addView(familyTreeView);

        RecyclerView recyclerView = familyTreeView.findViewById(R.id.recyclerViewFamilyTree);
        TextView tvCurrentUser = familyTreeView.findViewById(R.id.tvCurrentUser);
        ImageView ivCurrentUserProfile = familyTreeView.findViewById(R.id.ivCurrentUserProfile);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Load tree owner info and family tree
        loadTreeOwnerInfo(tvCurrentUser, ivCurrentUserProfile);
        loadFamilyTreeData(recyclerView);
    }

    private void loadTreeOwnerInfo(TextView tvCurrentUser, ImageView ivCurrentUserProfile) {
        // Load the tree owner's information
        String url = USER_BASE_URL + "/" + currentTreeOwnerId;

        Log.d(TAG, "Loading tree owner info for user ID: " + currentTreeOwnerId);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        Log.d(TAG, "Tree owner response: " + response.toString());

                        if (response.getBoolean("success")) {
                            JSONObject userData = response.getJSONObject("data");

                            // Store tree owner info
                            currentTreeOwner = new UserInfo();
                            currentTreeOwner.userId = userData.getLong("id");
                            currentTreeOwner.name = userData.getString("name");
                            currentTreeOwner.email = userData.optString("email", "");

                            Log.d(TAG, "Tree owner loaded: " + currentTreeOwner.name + " (ID: " + currentTreeOwner.userId + ")");

                            // Handle profile image - check different possible field names
                            boolean imageSet = false;

                            // Try profileImg as byte array (from your backend structure)
                            if (userData.has("profileImg") && !userData.isNull("profileImg")) {
                                try {
                                    // The profileImg field contains byte array data
                                    JSONArray profileImgArray = userData.getJSONArray("profileImg");
                                    byte[] imageBytes = new byte[profileImgArray.length()];
                                    for (int i = 0; i < profileImgArray.length(); i++) {
                                        imageBytes[i] = (byte) profileImgArray.getInt(i);
                                    }

                                    Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                                    if (bitmap != null) {
                                        ivCurrentUserProfile.setImageBitmap(bitmap);
                                        imageSet = true;
                                        Log.d(TAG, "Profile image set from profileImg byte array");
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error processing profileImg byte array", e);
                                }
                            }

                            // Try profileImg as base64 string
                            if (!imageSet && userData.has("profileImg") && !userData.isNull("profileImg")) {
                                try {
                                    String base64Image = userData.getString("profileImg");
                                    if (base64Image != null && !base64Image.isEmpty()) {
                                        byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                                        Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                        if (bitmap != null) {
                                            ivCurrentUserProfile.setImageBitmap(bitmap);
                                            imageSet = true;
                                            Log.d(TAG, "Profile image set from profileImg base64 string");
                                        }
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error processing profileImg base64 string", e);
                                }
                            }

                            // If no image was set, use placeholder
                            if (!imageSet) {
                                ivCurrentUserProfile.setImageResource(R.drawable.ic_person_placeholder);
                                Log.d(TAG, "Using placeholder image for tree owner");
                            }

                            // Set the text based on whether we're viewing our own tree or someone else's
                            if (currentTreeOwnerId.equals(currentUserId)) {
                                tvCurrentUser.setText(currentTreeOwner.name + " (You)");
                                Log.d(TAG, "Displaying own tree");
                            } else {
                                tvCurrentUser.setText(currentTreeOwner.name);
                                Log.d(TAG, "Displaying " + currentTreeOwner.name + "'s tree");
                            }

                        } else {
                            Log.e(TAG, "Failed to load tree owner info: " + response.getString("message"));
                            tvCurrentUser.setText("Unknown User");
                            ivCurrentUserProfile.setImageResource(R.drawable.ic_person_placeholder);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing tree owner data", e);
                        tvCurrentUser.setText("Unknown User");
                        ivCurrentUserProfile.setImageResource(R.drawable.ic_person_placeholder);
                    }
                },
                error -> {
                    Log.e(TAG, "Error loading tree owner info", error);
                    tvCurrentUser.setText("Unknown User");
                    ivCurrentUserProfile.setImageResource(R.drawable.ic_person_placeholder);
                }
        );

        requestQueue.add(request);
    }

    private void loadFamilyTreeData(RecyclerView recyclerView) {
        progressDialog.show();

        String url = BASE_URL + "/user/" + currentTreeOwnerId;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    progressDialog.dismiss();
                    try {
                        if (response.getBoolean("success")) {
                            JSONObject data = response.getJSONObject("data");

                            // Also update tree owner info from family tree response if available
                            if (data.has("rootUser")) {
                                JSONObject rootUser = data.getJSONObject("rootUser");
                                updateTreeOwnerFromRootUser(rootUser);
                            }

                            List<FamilyMember> familyMembers = parseFamilyTreeData(data);

                            FamilyTreeAdapter adapter = new FamilyTreeAdapter(familyMembers, this::onFamilyMemberClick, this::onFamilyMemberLongClick);
                            recyclerView.setAdapter(adapter);
                        } else {
                            showError("Failed to load family tree: " + response.getString("message"));
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing family tree data", e);
                        showError("Error parsing family tree data");
                    }
                },
                error -> {
                    progressDialog.dismiss();
                    Log.e(TAG, "Error loading family tree", error);
                    showError("Error loading family tree: " + error.getMessage());
                }
        );

        requestQueue.add(request);
    }

    private void updateTreeOwnerFromRootUser(JSONObject rootUser) {
        try {
            // Update the UI with root user info if we have the views
            TextView tvCurrentUser = findViewById(R.id.tvCurrentUser);
            ImageView ivCurrentUserProfile = findViewById(R.id.ivCurrentUserProfile);

            if (tvCurrentUser != null && ivCurrentUserProfile != null) {
                String name = rootUser.getString("name");

                // Set the text
                if (currentTreeOwnerId.equals(currentUserId)) {
                    tvCurrentUser.setText(name + " (You)");
                } else {
                    tvCurrentUser.setText(name);
                }

                // Set profile image if available
                if (rootUser.has("profileImageBase64") && !rootUser.isNull("profileImageBase64")) {
                    try {
                        String base64Image = rootUser.getString("profileImageBase64");
                        byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        ivCurrentUserProfile.setImageBitmap(bitmap);
                        Log.d(TAG, "Updated tree owner image from rootUser");
                    } catch (Exception e) {
                        Log.e(TAG, "Error decoding rootUser profile image", e);
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error updating tree owner from rootUser", e);
        }
    }

    private List<FamilyMember> parseFamilyTreeData(JSONObject data) throws JSONException {
        List<FamilyMember> familyMembers = new ArrayList<>();

        // Parse generations
        if (data.has("generations")) {
            JSONArray generations = data.getJSONArray("generations");
            for (int i = 0; i < generations.length(); i++) {
                JSONObject generation = generations.getJSONObject(i);
                JSONArray allMembers = generation.getJSONArray("allMembers");

                for (int j = 0; j < allMembers.length(); j++) {
                    JSONObject member = allMembers.getJSONObject(j);
                    FamilyMember familyMember = new FamilyMember();
                    familyMember.userId = member.getLong("userId");
                    familyMember.name = member.getString("name");
                    familyMember.email = member.optString("email", "");
                    familyMember.relationshipDisplayName = member.optString("relationshipDisplayName", "");
                    familyMember.generationLevel = member.optInt("generationLevel", 0);
                    familyMember.generationName = member.optString("generationName", "");

                    // Mark if this is the logged-in user viewing someone else's tree
                    familyMember.isCurrentLoggedInUser = familyMember.userId.equals(currentUserId) && !currentTreeOwnerId.equals(currentUserId);

                    if (member.has("profileImageBase64") && !member.isNull("profileImageBase64")) {
                        familyMember.profileImageBase64 = member.getString("profileImageBase64");
                    }

                    familyMembers.add(familyMember);
                }
            }
        }

        return familyMembers;
    }

    private void onFamilyMemberClick(FamilyMember member) {
        // Load specific user's family tree
        loadSpecificUserFamilyTree(member.userId);
    }

    private void onFamilyMemberLongClick(FamilyMember member) {
        // Show user details dialog
        showUserDetailsDialog(member);
    }

    private void loadSpecificUserFamilyTree(Long userId) {
        // Update the current tree owner
        currentTreeOwnerId = userId;

        // Reload the family tree view with the new tree owner
        loadFamilyTreeView();
    }
    private void showUserDetailsDialog(FamilyMember member) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_user_details, null);

        // Initialize all views
        ImageView ivProfile = dialogView.findViewById(R.id.ivProfileDialog);
        TextView tvName = dialogView.findViewById(R.id.tvNameDialog);
        TextView tvEmail = dialogView.findViewById(R.id.tvEmailDialog);
        TextView tvRelationship = dialogView.findViewById(R.id.tvRelationshipDialog);
        TextView tvGeneration = dialogView.findViewById(R.id.tvGenerationDialog);
        TextView tvPhone = dialogView.findViewById(R.id.tvPhoneDialog);
        TextView tvGender = dialogView.findViewById(R.id.tvGenderDialog);
        TextView tvDateOfBirth = dialogView.findViewById(R.id.tvDateOfBirthDialog);
        TextView tvAddress = dialogView.findViewById(R.id.tvAddressDialog);
        TextView tvJoinedDate = dialogView.findViewById(R.id.tvJoinedDateDialog);
        TextView tvBio = dialogView.findViewById(R.id.tvBioDialog);
        Button btnViewFamilyTree = dialogView.findViewById(R.id.btnViewFamilyTree);
        Button btnClose = dialogView.findViewById(R.id.btnClose);

        // Store the bitmap for full-screen viewing
        Bitmap profileBitmap = null;

        // Add "(You)" if this is the logged-in user
        String displayName = member.name;
        if (member.isCurrentLoggedInUser) {
            displayName += " (You)";
        }

        // Set basic information
        tvName.setText(displayName);
        tvEmail.setText(member.email);
        tvRelationship.setText(member.relationshipDisplayName);
        tvGeneration.setText(member.generationName);

        // Set profile image and enable click for full-screen view
        if (member.profileImageBase64 != null && !member.profileImageBase64.isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(member.profileImageBase64, Base64.DEFAULT);
                profileBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                if (profileBitmap != null) {
                    ivProfile.setImageBitmap(profileBitmap);

                    // Enable click for full-screen view
                    final Bitmap finalProfileBitmap = profileBitmap;
                    ivProfile.setOnClickListener(v -> {
                        FullScreenImageDialog fullScreenDialog = new FullScreenImageDialog(
                                MembersActivity.this,
                                finalProfileBitmap,
                                R.drawable.ic_person_placeholder
                        );
                        fullScreenDialog.show();
                    });

                    // Add visual indication that image is clickable
                    ivProfile.setClickable(true);
                    ivProfile.setFocusable(true);
                 //   ivProfile.setBackground(getResources().getDrawable(R.drawable.clickable_background));
                } else {
                    ivProfile.setImageResource(R.drawable.ic_person_placeholder);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error decoding profile image", e);
                ivProfile.setImageResource(R.drawable.ic_person_placeholder);
            }
        } else {
            ivProfile.setImageResource(R.drawable.ic_person_placeholder);
        }

        // Load additional user details from API
        loadUserDetailsFromAPI(member.userId, tvPhone, tvGender, tvDateOfBirth,
                tvAddress, tvJoinedDate, tvBio);

        // Set button click listeners
        btnViewFamilyTree.setOnClickListener(v -> {
            loadSpecificUserFamilyTree(member.userId);
            // Dismiss the dialog after clicking
            if (builder != null) {
                AlertDialog dialog = (AlertDialog) v.getTag();
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        btnClose.setOnClickListener(v -> {
            AlertDialog dialog = (AlertDialog) v.getTag();
            if (dialog != null) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.setView(dialogView)
                .setTitle("User Details")
                .create();

        // Store dialog reference in button tags for dismissal
        btnViewFamilyTree.setTag(dialog);
        btnClose.setTag(dialog);

        dialog.show();
    }
    // Add this new method to load additional user details from API
    private void loadUserDetailsFromAPI(Long userId, TextView tvPhone, TextView tvGender,
                                        TextView tvDateOfBirth, TextView tvAddress,TextView tvJoinedDate,
                                        TextView tvBio) {

        String url = USER_BASE_URL + "/" + userId;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        Log.d(TAG, "User details response: " + response.toString());

                        if (response.getBoolean("success")) {
                            JSONObject userData = response.optJSONObject("user");
                            // Set phone number
                            String phone = userData.optString("phoneNumber", "Not provided");
                            tvPhone.setText(phone.isEmpty() ? "Not provided" : phone);

                            // Set gender
                            String gender = userData.optString("gender", "Not specified");
                            tvGender.setText(gender.isEmpty() ? "Not specified" : gender);

                            // Set date of birth
                            String dob = userData.optString("dateOfBirth", "Not provided");
                            tvDateOfBirth.setText(dob.isEmpty() ? "Not provided" : dob);

                            // Set address
                            String address = userData.optString("address", "Not provided");
                            tvAddress.setText(address.isEmpty() ? "Not provided" : address);



                            // Set joined date
                            String joinedDate = userData.optString("createdAt", "Not available");
                            if (!joinedDate.equals("Not available")) {
                                // Format the date if needed
                                tvJoinedDate.setText(formatDate(joinedDate));
                            } else {
                                tvJoinedDate.setText("Not available");
                            }

                            // Set bio/description
                            String bio = userData.optString("bio", "No bio available");
                            if (bio.isEmpty()) {
                                bio = "No bio available";
                            }
                            tvBio.setText(bio);

                        } else {
                            Log.e(TAG, "Failed to load user details: " + response.getString("message"));
                            setDefaultValues(tvPhone, tvGender, tvDateOfBirth, tvAddress,
                                    tvJoinedDate, tvBio);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing user details", e);
                        setDefaultValues(tvPhone, tvGender, tvDateOfBirth, tvAddress,
                                tvJoinedDate, tvBio);
                    }
                },
                error -> {
                    Log.e(TAG, "Error loading user details", error);
                    setDefaultValues(tvPhone, tvGender, tvDateOfBirth, tvAddress,
                            tvJoinedDate, tvBio);
                }
        );

        requestQueue.add(request);
    }

    // Helper method to set default values when API call fails
    private void setDefaultValues(TextView tvPhone, TextView tvGender, TextView tvDateOfBirth,
                                  TextView tvAddress, TextView tvJoinedDate, TextView tvBio) {
        tvPhone.setText("Not provided");
        tvGender.setText("Not specified");
        tvDateOfBirth.setText("Not provided");
        tvAddress.setText("Not provided");
        tvJoinedDate.setText("Not available");
        tvBio.setText("No bio available");
    }

    // Helper method to format date
    private String formatDate(String dateString) {
        try {
            // Assuming the date comes in ISO format, you can format it as needed
            // This is a simple implementation, you might want to use SimpleDateFormat
            if (dateString.contains("T")) {
                return dateString.substring(0, dateString.indexOf("T"));
            }
            return dateString;
        } catch (Exception e) {
            Log.e(TAG, "Error formatting date", e);
            return dateString;
        }
    }

    // ==================== REST OF THE CODE REMAINS THE SAME ====================
    // ... (keeping all your existing methods for add relationship, approve requests, etc.)

    private void loadAddRelationshipView() {
        View addRelationshipView = LayoutInflater.from(this).inflate(R.layout.view_add_relationship, null);
        contentFrame.removeAllViews();
        contentFrame.addView(addRelationshipView);

        EditText etSearchUser = addRelationshipView.findViewById(R.id.etSearchUser);
        Spinner spinnerRelationshipType = addRelationshipView.findViewById(R.id.spinnerRelationshipType);
        EditText etRequestMessage = addRelationshipView.findViewById(R.id.etRequestMessage);
        Button btnSendRequest = addRelationshipView.findViewById(R.id.btnSendRequest);
        RecyclerView recyclerViewSearchResults = addRelationshipView.findViewById(R.id.recyclerViewSearchResults);

        recyclerViewSearchResults.setLayoutManager(new LinearLayoutManager(this));

        // Load relationship types
        loadRelationshipTypes(spinnerRelationshipType);

        // Setup search functionality
        Button btnSearch = addRelationshipView.findViewById(R.id.btnSearch);
        btnSearch.setOnClickListener(v -> {
            String searchQuery = etSearchUser.getText().toString().trim();
            if (!searchQuery.isEmpty()) {
                searchUsers(searchQuery, recyclerViewSearchResults);
            }
        });

        btnSendRequest.setOnClickListener(v -> {
            // Handle send relationship request
            String message = etRequestMessage.getText().toString().trim();
            String selectedRelationship = spinnerRelationshipType.getSelectedItem().toString();

            if (selectedUserId != null) {
                sendRelationshipRequest(selectedUserId, selectedRelationship, message);
            } else {
                showError("Please select a user first");
            }
        });
    }

    private Long selectedUserId = null;

    private void loadRelationshipTypes(Spinner spinner) {
        String url = BASE_URL + "/relationship-types";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.getBoolean("success")) {
                            JSONArray relationshipTypes = response.getJSONArray("data");
                            List<String> types = new ArrayList<>();

                            for (int i = 0; i < relationshipTypes.length(); i++) {
                                types.add(relationshipTypes.getString(i));
                            }

                            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            spinner.setAdapter(adapter);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing relationship types", e);
                    }
                },
                error -> Log.e(TAG, "Error loading relationship types", error)
        );

        requestQueue.add(request);
    }

    private void searchUsers(String query, RecyclerView recyclerView) {
        progressDialog.show();

        String url = BASE_URL.replace("/family-tree", "/users") + "/" + currentUserId + "/search-samaj-members";

        JSONObject searchParams = new JSONObject();
        try {
            searchParams.put("query", query);
            searchParams.put("page", 0);
            searchParams.put("size", 20);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating search params", e);
            progressDialog.dismiss();
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, searchParams,
                response -> {
                    progressDialog.dismiss();
                    try {
                        if (response.getBoolean("success")) {
                            JSONObject data = response.getJSONObject("data");
                            JSONArray members = data.getJSONArray("members");

                            List<SamajMember> searchResults = new ArrayList<>();
                            for (int i = 0; i < members.length(); i++) {
                                JSONObject member = members.getJSONObject(i);
                                SamajMember samajMember = new SamajMember();
                                samajMember.userId = member.getLong("userId");
                                samajMember.name = member.getString("name");
                                samajMember.email = member.optString("email", "");
                                samajMember.gender = member.optString("gender", "");
                                samajMember.phoneNumber = member.optString("phoneNumber", "");
                                samajMember.relationshipStatus = member.optString("relationshipStatus", "AVAILABLE");
                                samajMember.relationshipStatusText = member.optString("relationshipStatusText", "Available");

                                if (member.has("profileImageBase64") && !member.isNull("profileImageBase64")) {
                                    samajMember.profileImageBase64 = member.getString("profileImageBase64");
                                }

                                searchResults.add(samajMember);
                            }

                            SamajMemberSearchAdapter adapter = new SamajMemberSearchAdapter(searchResults, this::onSamajMemberSelected);
                            recyclerView.setAdapter(adapter);
                        } else {
                            showError("Search failed: " + response.getString("message"));
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing search results", e);
                        showError("Error parsing search results");
                    }
                },
                error -> {
                    progressDialog.dismiss();
                    Log.e(TAG, "Error searching samaj members", error);
                    showError("Error searching samaj members: " + error.getMessage());
                }
        );

        requestQueue.add(request);
    }

    private void onSamajMemberSelected(SamajMember member) {
        selectedUserId = member.userId;
        Toast.makeText(this, "Selected: " + member.name, Toast.LENGTH_SHORT).show();
    }

    private void onUserSelected(FamilyMember user) {
        selectedUserId = user.userId;
        Toast.makeText(this, "Selected: " + user.name, Toast.LENGTH_SHORT).show();
    }

    private void sendRelationshipRequest(Long relatedUserId, String relationshipType, String message) {
        progressDialog.show();

        String url = BASE_URL + "/relationship";

        JSONObject requestData = new JSONObject();
        try {
            requestData.put("requestingUserId", currentUserId);
            requestData.put("relatedUserId", relatedUserId);
            requestData.put("relationshipType", relationshipType);
            requestData.put("requestMessage", message);
            requestData.put("sendRequest", true);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating request data", e);
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, requestData,
                response -> {
                    progressDialog.dismiss();
                    try {
                        if (response.getBoolean("success")) {
                            Toast.makeText(this, "Relationship request sent successfully!", Toast.LENGTH_LONG).show();
                            // Clear form
                            selectedUserId = null;
                        } else {
                            showError("Failed to send request: " + response.getString("message"));
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing response", e);
                        showError("Error parsing response");
                    }
                },
                error -> {
                    progressDialog.dismiss();
                    Log.e(TAG, "Error sending relationship request", error);
                    showError("Error sending relationship request: " + error.getMessage());
                }
        );

        request.setRetryPolicy(new DefaultRetryPolicy(150000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(request);
    }

    private void loadApproveRequestsView() {
        View approveRequestsView = LayoutInflater.from(this).inflate(R.layout.view_approve_requests, null);
        contentFrame.removeAllViews();
        contentFrame.addView(approveRequestsView);

        RecyclerView recyclerViewPendingRequests = approveRequestsView.findViewById(R.id.recyclerViewPendingRequests);
        recyclerViewPendingRequests.setLayoutManager(new LinearLayoutManager(this));

        loadPendingRequests(recyclerViewPendingRequests);
    }

    private void loadPendingRequests(RecyclerView recyclerView) {
        progressDialog.show();

        String url = BASE_URL + "/requests/pending/" + currentUserId;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    progressDialog.dismiss();
                    try {
                        if (response.getBoolean("success")) {
                            JSONArray requestsArray = response.getJSONArray("data");
                            List<RelationshipRequest> requests = new ArrayList<>();

                            for (int i = 0; i < requestsArray.length(); i++) {
                                JSONObject requestObj = requestsArray.getJSONObject(i);
                                RelationshipRequest relationshipRequest = new RelationshipRequest();
                                relationshipRequest.id = requestObj.getLong("id");
                                relationshipRequest.requesterName = requestObj.optString("requesterName", "Unknown");
                                relationshipRequest.relationshipDisplayName = requestObj.optString("relationshipDisplayName", "");
                                relationshipRequest.requestMessage = requestObj.optString("requestMessage", "");
                                relationshipRequest.createdAt = requestObj.optString("createdAt", "");

                                requests.add(relationshipRequest);
                            }

                            RelationshipRequestAdapter adapter = new RelationshipRequestAdapter(requests, this::onApproveRequest, this::onRejectRequest);
                            recyclerView.setAdapter(adapter);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing pending requests", e);
                        showError("Error parsing pending requests");
                    }
                },
                error -> {
                    progressDialog.dismiss();
                    Log.e(TAG, "Error loading pending requests", error);
                    showError("Error loading pending requests: " + error.getMessage());
                }
        );

        requestQueue.add(request);
    }

    private void onApproveRequest(RelationshipRequest request) {
        respondToRequest(request.id, "APPROVED");
    }

    private void onRejectRequest(RelationshipRequest request) {
        respondToRequest(request.id, "REJECTED");
    }

    private void respondToRequest(Long requestId, String status) {
        progressDialog.show();

        String url = BASE_URL + "/requests/respond";

        JSONObject responseData = new JSONObject();
        try {
            responseData.put("requestId", requestId);
            responseData.put("status", status);
            responseData.put("respondingUserId", currentUserId);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating response data", e);
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, responseData,
                response -> {
                    progressDialog.dismiss();
                    try {
                        if (response.getBoolean("success")) {
                            Toast.makeText(this, "Request " + status.toLowerCase() + " successfully!", Toast.LENGTH_LONG).show();
                            // Reload pending requests
                            loadApproveRequestsView();
                        } else {
                            showError("Failed to respond to request: " + response.getString("message"));
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing response", e);
                        showError("Error parsing response");
                    }
                },
                error -> {
                    progressDialog.dismiss();
                    Log.e(TAG, "Error responding to request", error);
                    showError("Error responding to request: " + error.getMessage());
                }
        );

        requestQueue.add(request);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    // ==================== DATA CLASSES ====================

    public static class UserInfo {
        public Long userId;
        public String name;
        public String email;
        public String profileImageBase64;
    }

    public static class FamilyMember {
        public Long userId;
        public String name;
        public String email;
        public String relationshipDisplayName;
        public Integer generationLevel;
        public String generationName;
        public String profileImageBase64;
        public boolean isCurrentLoggedInUser = false;
    }

    public static class SamajMember {
        public Long userId;
        public String name;
        public String email;
        public String gender;
        public String phoneNumber;
        public String relationshipStatus;
        public String relationshipStatusText;
        public String profileImageBase64;
        public boolean isSelected = false;
    }

    public static class RelationshipRequest {
        public Long id;
        public String requesterName;
        public String relationshipDisplayName;
        public String requestMessage;
        public String createdAt;
    }

    // ==================== ADAPTERS ====================

    public static class FamilyTreeAdapter extends RecyclerView.Adapter<FamilyTreeAdapter.ViewHolder> {
        private List<FamilyMember> familyMembers;
        private OnItemClickListener clickListener;
        private OnItemLongClickListener longClickListener;

        public interface OnItemClickListener {
            void onItemClick(FamilyMember member);
        }

        public interface OnItemLongClickListener {
            void onItemLongClick(FamilyMember member);
        }

        public FamilyTreeAdapter(List<FamilyMember> familyMembers, OnItemClickListener clickListener, OnItemLongClickListener longClickListener) {
            this.familyMembers = familyMembers;
            this.clickListener = clickListener;
            this.longClickListener = longClickListener;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_family_member, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            FamilyMember member = familyMembers.get(position);
            holder.bind(member);
        }

        @Override
        public int getItemCount() {
            return familyMembers.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            private ImageView ivProfile;
            private TextView tvName;
            private TextView tvRelationship;
            private TextView tvGeneration;

            public ViewHolder(View itemView) {
                super(itemView);
                ivProfile = itemView.findViewById(R.id.ivProfile);
                tvName = itemView.findViewById(R.id.tvName);
                tvRelationship = itemView.findViewById(R.id.tvRelationship);
                tvGeneration = itemView.findViewById(R.id.tvGeneration);

                itemView.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && clickListener != null) {
                        clickListener.onItemClick(familyMembers.get(position));
                    }
                });

                itemView.setOnLongClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && longClickListener != null) {
                        longClickListener.onItemLongClick(familyMembers.get(position));
                        return true;
                    }
                    return false;
                });
            }

            public void bind(FamilyMember member) {
                // Add "(You)" to the name if this is the logged-in user viewing someone else's tree
                String displayName = member.name;
                if (member.isCurrentLoggedInUser) {
                    displayName += " (You)";
                }

                tvName.setText(displayName);
                tvRelationship.setText(member.relationshipDisplayName);
                tvGeneration.setText(member.generationName);

                if (member.profileImageBase64 != null && !member.profileImageBase64.isEmpty()) {
                    try {
                        byte[] decodedString = Base64.decode(member.profileImageBase64, Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        ivProfile.setImageBitmap(bitmap);
                    } catch (Exception e) {
                        Log.e(TAG, "Error decoding profile image", e);
                        ivProfile.setImageResource(R.drawable.ic_person_placeholder);
                    }
                } else {
                    ivProfile.setImageResource(R.drawable.ic_person_placeholder);
                }
            }
        }
    }

    // ... (rest of your adapters remain the same)
    public static class SamajMemberSearchAdapter extends RecyclerView.Adapter<SamajMemberSearchAdapter.ViewHolder> {
        private List<SamajMember> members;
        private OnSamajMemberSelectedListener listener;
        private int selectedPosition = -1;

        public interface OnSamajMemberSelectedListener {
            void onMemberSelected(SamajMember member);
        }

        public SamajMemberSearchAdapter(List<SamajMember> members, OnSamajMemberSelectedListener listener) {
            this.members = members;
            this.listener = listener;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_samaj_member_search, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            SamajMember member = members.get(position);
            holder.bind(member, position);
        }

        @Override
        public int getItemCount() {
            return members.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            private ImageView ivProfile;
            private TextView tvName;
            private TextView tvEmail;
            private TextView tvGender;
            private Button btnAction;

            public ViewHolder(View itemView) {
                super(itemView);
                ivProfile = itemView.findViewById(R.id.ivProfile);
                tvName = itemView.findViewById(R.id.tvName);
                tvEmail = itemView.findViewById(R.id.tvEmail);
                tvGender = itemView.findViewById(R.id.tvGender);
                btnAction = itemView.findViewById(R.id.btnAction);
            }

            public void bind(SamajMember member, int position) {
                tvName.setText(member.name);
                tvEmail.setText(member.email);
                tvGender.setText("Gender: " + member.gender);

                // Set profile image
                if (member.profileImageBase64 != null && !member.profileImageBase64.isEmpty()) {
                    try {
                        byte[] decodedString = Base64.decode(member.profileImageBase64, Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        ivProfile.setImageBitmap(bitmap);
                    } catch (Exception e) {
                        Log.e(TAG, "Error decoding profile image", e);
                        ivProfile.setImageResource(R.drawable.ic_person_placeholder);
                    }
                } else {
                    ivProfile.setImageResource(R.drawable.ic_person_placeholder);
                }

                // Set button state based on relationship status
                setupActionButton(member, position);
            }

            private void setupActionButton(SamajMember member, int position) {
                switch (member.relationshipStatus) {
                    case "AVAILABLE":
                        if (selectedPosition == position) {
                            btnAction.setText("Selected");
                            btnAction.setEnabled(true);
                        } else {
                            btnAction.setText("Select");
                            btnAction.setEnabled(true);
                        }
                        btnAction.setOnClickListener(v -> {
                            if (selectedPosition != position) {
                                int oldPosition = selectedPosition;
                                selectedPosition = position;

                                // Notify changes
                                if (oldPosition != -1) {
                                    notifyItemChanged(oldPosition);
                                }
                                notifyItemChanged(position);

                                if (listener != null) {
                                    listener.onMemberSelected(member);
                                }
                            }
                        });
                        break;

                    case "ALREADY_RELATED":
                        btnAction.setText("Already Related");
                        btnAction.setEnabled(false);
                        break;

                    case "REQUEST_SENT":
                        btnAction.setText("Request Sent");
                        btnAction.setEnabled(false);
                        break;

                    case "REQUEST_RECEIVED":
                        btnAction.setText("Request Received");
                        btnAction.setEnabled(false);
                        break;

                    default:
                        btnAction.setText("Unknown");
                        btnAction.setEnabled(false);
                        break;
                }
            }
        }
    }

    public static class UserSearchAdapter extends RecyclerView.Adapter<UserSearchAdapter.ViewHolder> {
        private List<FamilyMember> users;
        private OnUserSelectedListener listener;

        public interface OnUserSelectedListener {
            void onUserSelected(FamilyMember user);
        }

        public UserSearchAdapter(List<FamilyMember> users, OnUserSelectedListener listener) {
            this.users = users;
            this.listener = listener;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_search, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            FamilyMember user = users.get(position);
            holder.bind(user);
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            private ImageView ivProfile;
            private TextView tvName;
            private TextView tvEmail;
            private Button btnSelect;

            public ViewHolder(View itemView) {
                super(itemView);
                ivProfile = itemView.findViewById(R.id.ivProfile);
                tvName = itemView.findViewById(R.id.tvName);
                tvEmail = itemView.findViewById(R.id.tvEmail);
                btnSelect = itemView.findViewById(R.id.btnSelect);
            }

            public void bind(FamilyMember user) {
                tvName.setText(user.name);
                tvEmail.setText(user.email);

                if (user.profileImageBase64 != null && !user.profileImageBase64.isEmpty()) {
                    try {
                        byte[] decodedString = Base64.decode(user.profileImageBase64, Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        ivProfile.setImageBitmap(bitmap);
                    } catch (Exception e) {
                        Log.e(TAG, "Error decoding profile image", e);
                        ivProfile.setImageResource(R.drawable.ic_person_placeholder);
                    }
                } else {
                    ivProfile.setImageResource(R.drawable.ic_person_placeholder);
                }

                btnSelect.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onUserSelected(user);
                    }
                });
            }
        }
    }

    public static class RelationshipRequestAdapter extends RecyclerView.Adapter<RelationshipRequestAdapter.ViewHolder> {
        private List<RelationshipRequest> requests;
        private OnRequestActionListener approveListener;
        private OnRequestActionListener rejectListener;

        public interface OnRequestActionListener {
            void onAction(RelationshipRequest request);
        }

        public RelationshipRequestAdapter(List<RelationshipRequest> requests, OnRequestActionListener approveListener, OnRequestActionListener rejectListener) {
            this.requests = requests;
            this.approveListener = approveListener;
            this.rejectListener = rejectListener;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_relationship_request, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            RelationshipRequest request = requests.get(position);
            holder.bind(request);
        }

        @Override
        public int getItemCount() {
            return requests.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            private TextView tvRequesterName;
            private TextView tvRelationship;
            private TextView tvMessage;
            private TextView tvDate;
            private Button btnApprove;
            private Button btnReject;

            public ViewHolder(View itemView) {
                super(itemView);
                tvRequesterName = itemView.findViewById(R.id.tvRequesterName);
                tvRelationship = itemView.findViewById(R.id.tvRelationship);
                tvMessage = itemView.findViewById(R.id.tvMessage);
                tvDate = itemView.findViewById(R.id.tvDate);
                btnApprove = itemView.findViewById(R.id.btnApprove);
                btnReject = itemView.findViewById(R.id.btnReject);
            }

            public void bind(RelationshipRequest request) {
                tvRequesterName.setText(request.requesterName);
                tvRelationship.setText("Your " + request.relationshipDisplayName);

                tvMessage.setText(request.requestMessage != null ? request.requestMessage : "No message provided.");
                tvDate.setText(request.createdAt);

                btnApprove.setOnClickListener(v -> {
                    if (approveListener != null) {
                        approveListener.onAction(request);
                    }
                });

                btnReject.setOnClickListener(v -> {
                    if (rejectListener != null) {
                        rejectListener.onAction(request);
                    }
                });
            }

        }
    }
}
