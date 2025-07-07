package com.example.samajconnectfrontend;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.samajconnectfrontend.adapters.MemberAdapter;
import com.example.samajconnectfrontend.models.Member;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MemberListActivity extends AppCompatActivity {

    private RecyclerView recyclerViewMembers;
    private MemberAdapter memberAdapter;
    private List<Member> memberList;
    private List<Member> filteredMemberList;

    private TextView tvSamajName, tvMemberCount, tvPageInfo;
    private EditText etSearch;
    private ImageView btnBack, btnFilter;
    private Button btnPrevious, btnNext;
    private LinearLayout layoutEmptyState, layoutPagination;

    private RequestQueue requestQueue;
    private String baseUrl = "http://10.0.2.2:8080/api/users/samaj/"; // Fixed the placeholder
    private long samajId; // Changed from int to long
    private int currentPage = 0;
    private int totalPages = 1;
    private boolean hasNext = false;
    private boolean hasPrevious = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member_list);

        initViews();
        setupRecyclerView();
        setupListeners();

        // Get samaj ID from intent first, then from SharedPreferences as fallback
        samajId = getIntent().getIntExtra("samajId", -1);

        if (samajId == -1) {
            // Fallback to SharedPreferences - retrieve as Long
            SharedPreferences sharedPrefs = getSharedPreferences("SamajConnect", MODE_PRIVATE);
            samajId = sharedPrefs.getLong("samaj_id", -1L); // Changed to getLong
        }

        if (samajId == -1) {
            Toast.makeText(this, "Error: No Samaj ID found", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        requestQueue = Volley.newRequestQueue(this);
        loadMembers();
    }

    private void initViews() {
        recyclerViewMembers = findViewById(R.id.recyclerViewMembers);
        tvSamajName = findViewById(R.id.tvSamajName);
        tvMemberCount = findViewById(R.id.tvMemberCount);
        tvPageInfo = findViewById(R.id.tvPageInfo);
        etSearch = findViewById(R.id.etSearch);
        btnBack = findViewById(R.id.btnBack);
        btnFilter = findViewById(R.id.btnFilter);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnNext = findViewById(R.id.btnNext);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        layoutPagination = findViewById(R.id.layoutPagination);

        memberList = new ArrayList<>();
        filteredMemberList = new ArrayList<>();
    }

    private void setupRecyclerView() {
        memberAdapter = new MemberAdapter(this, filteredMemberList);
        recyclerViewMembers.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewMembers.setAdapter(memberAdapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterMembers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnFilter.setOnClickListener(v -> {
            // Implement filter functionality
            Toast.makeText(this, "Filter options coming soon", Toast.LENGTH_SHORT).show();
        });

        btnPrevious.setOnClickListener(v -> {
            if (hasPrevious && currentPage > 0) {
                currentPage--;
                loadMembers();
            }
        });

        btnNext.setOnClickListener(v -> {
            if (hasNext) {
                currentPage++;
                loadMembers();
            }
        });
    }

    private void loadMembers() {
        String url = baseUrl + samajId + "/members?page=" + currentPage + "&size=20";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if (response.getBoolean("success")) {
                                parseResponse(response.getJSONObject("data"));
                            } else {
                                Toast.makeText(MemberListActivity.this,
                                        response.getString("message"), Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(MemberListActivity.this,
                                    "Error parsing response", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        String errorMessage = "Error loading members";
                        if (error.networkResponse != null) {
                            int statusCode = error.networkResponse.statusCode;
                            if (statusCode == 404) {
                                errorMessage = "Samaj not found";
                            } else if (statusCode == 401) {
                                errorMessage = "Unauthorized access";
                            } else if (statusCode >= 500) {
                                errorMessage = "Server error";
                            }
                        } else {
                            errorMessage = "Network error - Check your connection";
                        }
                        Toast.makeText(MemberListActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");

                // Add authorization header
                SharedPreferences sharedPrefs = getSharedPreferences("SamajConnect", MODE_PRIVATE);
                String authToken = sharedPrefs.getString("auth_token", "");
                if (!authToken.isEmpty()) {
                    headers.put("Authorization", "Bearer " + authToken);
                }

                return headers;
            }
        };

        requestQueue.add(request);
    }

    private void parseResponse(JSONObject data) throws JSONException {
        // Update UI with samaj info
        String samajName = data.getString("samajName");
        int totalMembers = data.getInt("totalMembers");
        currentPage = data.getInt("currentPage");
        totalPages = data.getInt("totalPages");
        hasNext = data.getBoolean("hasNext");
        hasPrevious = data.getBoolean("hasPrevious");

        tvSamajName.setText(samajName + " Members");
        tvMemberCount.setText(totalMembers + " Members");

        // Parse members
        JSONArray membersArray = data.getJSONArray("members");
        memberList.clear();

        for (int i = 0; i < membersArray.length(); i++) {
            JSONObject memberObj = membersArray.getJSONObject(i);
            Member member = new Member();

            member.setId(memberObj.getInt("id"));
            member.setName(memberObj.getString("name"));
            member.setEmail(memberObj.getString("email"));
            member.setGender(memberObj.getString("gender"));
            member.setPhoneNumber(memberObj.optString("phoneNumber", "N/A"));
            member.setAddress(memberObj.optString("address", "N/A"));
            member.setProfileImageBase64(memberObj.optString("profileImageBase64", ""));
            member.setAdmin(memberObj.getBoolean("isAdmin"));
            member.setCreatedAt(memberObj.getString("createdAt"));
            member.setUpdatedAt(memberObj.getString("updatedAt"));

            // Parse samaj info
            JSONObject samajObj = memberObj.getJSONObject("samaj");
            member.setSamajId(samajObj.getInt("id"));
            member.setSamajName(samajObj.getString("name"));

            memberList.add(member);
        }

        // Update filtered list and adapter
        filteredMemberList.clear();
        filteredMemberList.addAll(memberList);
        memberAdapter.notifyDataSetChanged();

        // Update pagination
        updatePaginationUI();

        // Show/hide empty state
        if (memberList.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            recyclerViewMembers.setVisibility(View.GONE);
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            recyclerViewMembers.setVisibility(View.VISIBLE);
        }
    }

    private void updatePaginationUI() {
        if (totalPages > 1) {
            layoutPagination.setVisibility(View.VISIBLE);
            tvPageInfo.setText("Page " + (currentPage + 1) + " of " + totalPages);
            btnPrevious.setEnabled(hasPrevious);
            btnNext.setEnabled(hasNext);
        } else {
            layoutPagination.setVisibility(View.GONE);
        }
    }

    private void filterMembers(String query) {
        filteredMemberList.clear();

        if (query.isEmpty()) {
            filteredMemberList.addAll(memberList);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (Member member : memberList) {
                if (member.getName().toLowerCase().contains(lowerCaseQuery) ||
                        member.getEmail().toLowerCase().contains(lowerCaseQuery) ||
                        member.getPhoneNumber().contains(query)) {
                    filteredMemberList.add(member);
                }
            }
        }

        memberAdapter.notifyDataSetChanged();

        // Show/hide empty state based on filtered results
        if (filteredMemberList.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            recyclerViewMembers.setVisibility(View.GONE);
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            recyclerViewMembers.setVisibility(View.VISIBLE);
        }
    }
}
