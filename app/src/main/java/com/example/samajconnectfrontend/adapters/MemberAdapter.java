package com.example.samajconnectfrontend.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.samajconnectfrontend.R;
import com.example.samajconnectfrontend.models.Member;

import de.hdodenhof.circleimageview.CircleImageView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberViewHolder> {

    private Context context;
    private List<Member> memberList;

    public MemberAdapter(Context context, List<Member> memberList) {
        this.context = context;
        this.memberList = memberList;
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_member, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        Member member = memberList.get(position);

        holder.tvMemberName.setText(member.getName());
        holder.tvEmail.setText(member.getEmail());
        holder.tvPhone.setText(member.getPhoneNumber());
        holder.tvGender.setText(member.getGender());
        holder.tvAddress.setText(member.getAddress());

        // Show admin badge if user is admin
        if (member.isAdmin()) {
            holder.tvAdminBadge.setVisibility(View.VISIBLE);
        } else {
            holder.tvAdminBadge.setVisibility(View.GONE);
        }

        // Format and set joined date
        String formattedDate = formatDate(member.getCreatedAt());
        holder.tvJoinedDate.setText("Joined: " + formattedDate);

        // Load profile image
        loadProfileImage(holder.ivProfileImage, member.getProfileImageBase64());

        // Set click listeners
        holder.itemView.setOnClickListener(v -> {
            // Handle member item click
            Toast.makeText(context, member.getName(), Toast.LENGTH_SHORT).show();
        });

    }

    @Override
    public int getItemCount() {
        return memberList.size();
    }

    private void loadProfileImage(CircleImageView imageView, String base64Image) {
        if (base64Image != null && !base64Image.isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                imageView.setImageBitmap(decodedByte);
            } catch (Exception e) {
                e.printStackTrace();
                imageView.setImageResource(R.drawable.ic_person_placeholder);
            }
        } else {
            imageView.setImageResource(R.drawable.ic_person_placeholder);
        }
    }

    private String formatDate(String dateString) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            Date date = inputFormat.parse(dateString);
            return outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return dateString.substring(0, 10); // Return just the date part
        }
    }

    private void showMemberOptions(Member member) {
        // Implement popup menu or bottom sheet for member actions
        Toast.makeText(context, "Options for " + member.getName(), Toast.LENGTH_SHORT).show();
    }

    static class MemberViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivProfileImage;
        TextView tvMemberName, tvEmail, tvPhone, tvGender, tvAddress, tvJoinedDate, tvAdminBadge;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfileImage = itemView.findViewById(R.id.ivProfileImage);
            tvMemberName = itemView.findViewById(R.id.tvMemberName);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvPhone = itemView.findViewById(R.id.tvPhone);
            tvGender = itemView.findViewById(R.id.tvGender);
            tvAddress = itemView.findViewById(R.id.tvAddress);
            tvJoinedDate = itemView.findViewById(R.id.tvJoinedDate);
            tvAdminBadge = itemView.findViewById(R.id.tvAdminBadge);
        }
    }
}
