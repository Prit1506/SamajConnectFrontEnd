package com.example.samajconnectfrontend.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.samajconnectfrontend.R;
import com.example.samajconnectfrontend.models.DetailedUserDto;

import java.util.List;

public class SearchResultsAdapter extends RecyclerView.Adapter<SearchResultsAdapter.ViewHolder> {
    private Context context;
    private List<DetailedUserDto> members;
    private OnMemberClickListener clickListener;

    public interface OnMemberClickListener {
        void onMemberClick(DetailedUserDto member);
    }

    public SearchResultsAdapter(Context context, List<DetailedUserDto> members, OnMemberClickListener clickListener) {
        this.context = context;
        this.members = members;
        this.clickListener = clickListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LinearLayout itemView = new LinearLayout(context);
        itemView.setLayoutParams(new RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT));
        itemView.setOrientation(LinearLayout.HORIZONTAL);
        itemView.setPadding(16, 12, 16, 12);

        // Add ripple effect
        android.graphics.drawable.StateListDrawable selector = new android.graphics.drawable.StateListDrawable();
        selector.addState(new int[]{android.R.attr.state_pressed},
                new android.graphics.drawable.ColorDrawable(0x20000000));
        selector.addState(new int[]{},
                new android.graphics.drawable.ColorDrawable(0x00000000));
        itemView.setBackground(selector);

        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        DetailedUserDto member = members.get(position);
        holder.bind(member, context, clickListener);
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private LinearLayout containerLayout;
        private androidx.cardview.widget.CardView profileCardView;
        private ImageView profileImage;
        private LinearLayout detailsLayout;
        private TextView nameText;
        private TextView emailText;
        private TextView phoneText;
        private TextView adminBadge;

        public ViewHolder(LinearLayout itemView) {
            super(itemView);
            containerLayout = itemView;
            setupViews();
        }

        private void setupViews() {
            // Profile image container
            profileCardView = new androidx.cardview.widget.CardView(containerLayout.getContext());
            profileCardView.setLayoutParams(new LinearLayout.LayoutParams(60, 60));
            profileCardView.setRadius(30);
            profileCardView.setCardElevation(3);

            profileImage = new ImageView(containerLayout.getContext());
            profileImage.setLayoutParams(new androidx.cardview.widget.CardView.LayoutParams(
                    androidx.cardview.widget.CardView.LayoutParams.MATCH_PARENT,
                    androidx.cardview.widget.CardView.LayoutParams.MATCH_PARENT));
            profileImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
            profileCardView.addView(profileImage);

            // Details container
            detailsLayout = new LinearLayout(containerLayout.getContext());
            detailsLayout.setOrientation(LinearLayout.VERTICAL);
            detailsLayout.setPadding(12, 0, 0, 0);
            LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT);
            detailsParams.weight = 1;
            detailsLayout.setLayoutParams(detailsParams);

            // Name
            nameText = new TextView(containerLayout.getContext());
            nameText.setTextSize(16);
            nameText.setTextColor(0xFF000000);
            nameText.setTypeface(null, android.graphics.Typeface.BOLD);

            // Email
            emailText = new TextView(containerLayout.getContext());
            emailText.setTextSize(13);
            emailText.setTextColor(0xFF666666);

            // Phone
            phoneText = new TextView(containerLayout.getContext());
            phoneText.setTextSize(12);
            phoneText.setTextColor(0xFF888888);

            // Admin badge
            adminBadge = new TextView(containerLayout.getContext());
            adminBadge.setText("ADMIN");
            adminBadge.setTextSize(9);
            adminBadge.setTextColor(0xFFFFFFFF);
            adminBadge.setPadding(6, 3, 6, 3);
            adminBadge.setTypeface(null, android.graphics.Typeface.BOLD);

            // Create rounded background for admin badge
            android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
            shape.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            shape.setCornerRadius(8);
            shape.setColor(0xFFFF5722);
            adminBadge.setBackground(shape);

            // Add views to layouts
            detailsLayout.addView(nameText);
            detailsLayout.addView(emailText);

            containerLayout.addView(profileCardView);
            containerLayout.addView(detailsLayout);
        }

        public void bind(DetailedUserDto member, Context context, OnMemberClickListener clickListener) {
            // Set profile image
            if (member.getProfileImageBase64() != null && !member.getProfileImageBase64().isEmpty()) {
                try {
                    byte[] decodedBytes = Base64.decode(member.getProfileImageBase64(), Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                    profileImage.setImageBitmap(bitmap);
                } catch (Exception e) {
                    profileImage.setImageResource(R.drawable.ic_person_placeholder);
                }
            } else {
                profileImage.setImageResource(R.drawable.ic_person_placeholder);
            }

            // Set member details
            nameText.setText(member.getName());
            emailText.setText(member.getEmail());

            // Remove existing phone and admin badge
            detailsLayout.removeView(phoneText);
            detailsLayout.removeView(adminBadge);

            // Add phone if available
            if (member.getPhoneNumber() != null && !member.getPhoneNumber().isEmpty()) {
                phoneText.setText("📞 " + member.getPhoneNumber());
                detailsLayout.addView(phoneText);
            }

            // Add admin badge if admin
            if (member.getIsAdmin() != null && member.getIsAdmin()) {
                detailsLayout.addView(adminBadge);
            }

            // Set click listener
            containerLayout.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onMemberClick(member);
                }
            });
        }
    }
}
