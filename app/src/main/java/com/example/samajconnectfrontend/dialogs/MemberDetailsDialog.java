package com.example.samajconnectfrontend.dialogs;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.FrameLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;

import com.example.samajconnectfrontend.R;
import com.example.samajconnectfrontend.models.DetailedUserDto;

import java.util.ArrayList;
import java.util.List;

public class MemberDetailsDialog {
    private Context context;
    private AlertDialog fullScreenImageDialog;

    public MemberDetailsDialog(Context context) {
        this.context = context;
    }

    public void showMemberDetails(DetailedUserDto member) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Member Details");

        // Create main layout with clean white background
        LinearLayout mainLayout = new LinearLayout(context);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(Color.WHITE);
        mainLayout.setPadding(24, 24, 24, 24);

        // Profile section with clean card design
        CardView profileCard = new CardView(context);
        LinearLayout.LayoutParams profileCardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        profileCardParams.bottomMargin = 24;
        profileCard.setLayoutParams(profileCardParams);
        profileCard.setRadius(16);
        profileCard.setCardElevation(6);
        profileCard.setCardBackgroundColor(0xFFF8F9FA);

        LinearLayout profileContent = new LinearLayout(context);
        profileContent.setOrientation(LinearLayout.VERTICAL);
        profileContent.setPadding(32, 32, 32, 32);
        profileContent.setGravity(Gravity.CENTER);

        // Profile image with click functionality
        CardView profileImageCard = new CardView(context);
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(100, 100);
        imageParams.bottomMargin = 16;
        profileImageCard.setLayoutParams(imageParams);
        profileImageCard.setRadius(50);
        profileImageCard.setCardElevation(8);

        ImageView profileImage = new ImageView(context);
        profileImage.setLayoutParams(new CardView.LayoutParams(
                CardView.LayoutParams.MATCH_PARENT,
                CardView.LayoutParams.MATCH_PARENT));
        profileImage.setScaleType(ImageView.ScaleType.CENTER_CROP);

        // Set profile image and store bitmap for full screen
        Bitmap profileBitmap = null;
        if (member.getProfileImageBase64() != null && !member.getProfileImageBase64().isEmpty()) {
            try {
                byte[] decodedBytes = Base64.decode(member.getProfileImageBase64(), Base64.DEFAULT);
                profileBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                profileImage.setImageBitmap(profileBitmap);
            } catch (Exception e) {
                profileImage.setImageResource(R.drawable.ic_person_placeholder);
            }
        } else {
            profileImage.setImageResource(R.drawable.ic_person_placeholder);
        }

        // Add click listener for full screen
        final Bitmap finalBitmap = profileBitmap;
        profileImageCard.setOnClickListener(v -> showFullScreenImage(finalBitmap));

        profileImageCard.addView(profileImage);
        profileContent.addView(profileImageCard);

        // Member name
        TextView nameText = new TextView(context);
        nameText.setText(member.getName());
        nameText.setTextSize(22);
        nameText.setTextColor(0xFF1F2937);
        nameText.setTypeface(null, android.graphics.Typeface.BOLD);
        nameText.setGravity(Gravity.CENTER);
        profileContent.addView(nameText);

        // Admin badge
        if (member.getIsAdmin() != null && member.getIsAdmin()) {
            TextView adminBadge = new TextView(context);
            adminBadge.setText("ADMIN");
            adminBadge.setTextSize(12);
            adminBadge.setTextColor(Color.WHITE);
            adminBadge.setPadding(12, 6, 12, 6);
            adminBadge.setTypeface(null, android.graphics.Typeface.BOLD);

            GradientDrawable badgeShape = new GradientDrawable();
            badgeShape.setShape(GradientDrawable.RECTANGLE);
            badgeShape.setCornerRadius(12);
            badgeShape.setColor(0xFFEF4444);
            adminBadge.setBackground(badgeShape);

            LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            badgeParams.topMargin = 12;
            adminBadge.setLayoutParams(badgeParams);
            profileContent.addView(adminBadge);
        }

        profileCard.addView(profileContent);
        mainLayout.addView(profileCard);

        // Details section
        LinearLayout detailsSection = new LinearLayout(context);
        detailsSection.setOrientation(LinearLayout.VERTICAL);

        // Add detail rows with clean cards
        addDetailRow(detailsSection, "📧", "Email", member.getEmail(), true);

        if (member.getPhoneNumber() != null && !member.getPhoneNumber().isEmpty()) {
            addDetailRow(detailsSection, "📞", "Phone", member.getPhoneNumber(), true);
        }

        if (member.getGender() != null && !member.getGender().isEmpty()) {
            addDetailRow(detailsSection, "👤", "Gender", member.getGender(), false);
        }

        if (member.getAddress() != null && !member.getAddress().isEmpty()) {
            addDetailRow(detailsSection, "📍", "Address", member.getAddress(), false);
        }

        addDetailRow(detailsSection, "🆔", "Member ID", String.valueOf(member.getId()), false);

        mainLayout.addView(detailsSection);

        // Scroll view
        ScrollView scrollView = new ScrollView(context);
        scrollView.addView(mainLayout);

        builder.setView(scrollView);
        builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());
        builder.setNeutralButton("Contact", (dialog, which) -> {
            showContactOptions(member);
            dialog.dismiss();
        });

        AlertDialog detailsDialog = builder.create();
        detailsDialog.show();

        // Set dialog size
        Window window = detailsDialog.getWindow();
        if (window != null) {
            window.setLayout(
                    (int) (context.getResources().getDisplayMetrics().widthPixels * 0.9),
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
        }
    }

    private void addDetailRow(LinearLayout parent, String icon, String label, String value, boolean isClickable) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }

        CardView card = new CardView(context);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = 12;
        card.setLayoutParams(cardParams);
        card.setRadius(12);
        card.setCardElevation(2);
        card.setCardBackgroundColor(Color.WHITE);

        LinearLayout cardContent = new LinearLayout(context);
        cardContent.setOrientation(LinearLayout.HORIZONTAL);
        cardContent.setPadding(20, 16, 20, 16);
        cardContent.setGravity(Gravity.CENTER_VERTICAL);

        // Icon
        TextView iconText = new TextView(context);
        iconText.setText(icon);
        iconText.setTextSize(20);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(40, 40);
        iconParams.rightMargin = 16;
        iconText.setLayoutParams(iconParams);
        iconText.setGravity(Gravity.CENTER);
        cardContent.addView(iconText);

        // Text content
        LinearLayout textContent = new LinearLayout(context);
        textContent.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT);
        textParams.weight = 1;
        textContent.setLayoutParams(textParams);

        TextView labelText = new TextView(context);
        labelText.setText(label);
        labelText.setTextSize(12);
        labelText.setTextColor(0xFF6B7280);
        labelText.setTypeface(null, android.graphics.Typeface.BOLD);
        textContent.addView(labelText);

        TextView valueText = new TextView(context);
        valueText.setText(value);
        valueText.setTextSize(16);
        valueText.setTextColor(0xFF1F2937);
        valueText.setPadding(0, 4, 0, 0);

        if (isClickable) {
            if (label.equals("Email")) {
                valueText.setTextColor(0xFF3B82F6);
                card.setOnClickListener(v -> {
                    Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                    emailIntent.setData(android.net.Uri.parse("mailto:" + value));
                    try {
                        context.startActivity(emailIntent);
                    } catch (Exception e) {
                        Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show();
                    }
                });
            } else if (label.equals("Phone")) {
                valueText.setTextColor(0xFF10B981);
                card.setOnClickListener(v -> {
                    Intent dialIntent = new Intent(Intent.ACTION_DIAL);
                    dialIntent.setData(android.net.Uri.parse("tel:" + value));
                    try {
                        context.startActivity(dialIntent);
                    } catch (Exception e) {
                        Toast.makeText(context, "No phone app found", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

        textContent.addView(valueText);
        cardContent.addView(textContent);
        card.addView(cardContent);
        parent.addView(card);
    }

    private void showFullScreenImage(Bitmap bitmap) {
        if (bitmap == null) {
            Toast.makeText(context, "No image available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create full screen dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);

        // Main container
        FrameLayout container = new FrameLayout(context);
        container.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        container.setBackgroundColor(Color.BLACK);

        // Full screen image view
        ImageView imageView = new ImageView(context);
        FrameLayout.LayoutParams imageParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        imageView.setLayoutParams(imageParams);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setImageBitmap(bitmap);
        imageView.setBackgroundColor(Color.BLACK);

        // Close button
        TextView closeButton = new TextView(context);
        closeButton.setText("×");
        closeButton.setTextSize(28);
        closeButton.setTextColor(Color.WHITE);
        closeButton.setPadding(20, 20, 20, 20);
        closeButton.setBackgroundColor(0x80000000);

        FrameLayout.LayoutParams closeParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        closeParams.gravity = Gravity.TOP | Gravity.END;
        closeParams.setMargins(20, 40, 20, 20);
        closeButton.setLayoutParams(closeParams);

        // Make close button circular
        GradientDrawable closeShape = new GradientDrawable();
        closeShape.setShape(GradientDrawable.OVAL);
        closeShape.setColor(0x80000000);
        closeButton.setBackground(closeShape);
        closeButton.setGravity(Gravity.CENTER);

        container.addView(imageView);
        container.addView(closeButton);

        builder.setView(container);
        fullScreenImageDialog = builder.create();

        // Set full screen flags
        Window window = fullScreenImageDialog.getWindow();
        if (window != null) {
            window.setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN |
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN |
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }

        // Close handlers
        closeButton.setOnClickListener(v -> fullScreenImageDialog.dismiss());
        imageView.setOnClickListener(v -> fullScreenImageDialog.dismiss());

        fullScreenImageDialog.show();
    }

    private void showContactOptions(DetailedUserDto member) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Contact " + member.getName());

        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();

        // Email option
        if (member.getEmail() != null && !member.getEmail().isEmpty()) {
            options.add("📧 Send Email");
            actions.add(() -> {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                emailIntent.setData(android.net.Uri.parse("mailto:" + member.getEmail()));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Hello from SamajConnect");
                try {
                    context.startActivity(emailIntent);
                } catch (Exception e) {
                    Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Phone options
        if (member.getPhoneNumber() != null && !member.getPhoneNumber().isEmpty()) {
            options.add("📞 Call");
            actions.add(() -> {
                Intent dialIntent = new Intent(Intent.ACTION_DIAL);
                dialIntent.setData(android.net.Uri.parse("tel:" + member.getPhoneNumber()));
                try {
                    context.startActivity(dialIntent);
                } catch (Exception e) {
                    Toast.makeText(context, "No phone app found", Toast.LENGTH_SHORT).show();
                }
            });

            options.add("💬 Send SMS");
            actions.add(() -> {
                Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
                smsIntent.setData(android.net.Uri.parse("smsto:" + member.getPhoneNumber()));
                smsIntent.putExtra("sms_body", "Hello from SamajConnect!");
                try {
                    context.startActivity(smsIntent);
                } catch (Exception e) {
                    Toast.makeText(context, "No SMS app found", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (options.isEmpty()) {
            Toast.makeText(context, "No contact information available", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] optionsArray = options.toArray(new String[0]);

        builder.setItems(optionsArray, (dialog, which) -> {
            actions.get(which).run();
            dialog.dismiss();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
}