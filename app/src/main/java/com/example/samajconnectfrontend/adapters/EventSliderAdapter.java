package com.example.samajconnectfrontend.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.samajconnectfrontend.R;
import com.example.samajconnectfrontend.models.Event;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EventSliderAdapter extends RecyclerView.Adapter<EventSliderAdapter.EventViewHolder> {

    private Context context;
    private List<Event> events;
    private OnEventClickListener onEventClickListener;

    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

    public EventSliderAdapter(Context context, List<Event> events) {
        this.context = context;
        this.events = events;
    }

    public void setOnEventClickListener(OnEventClickListener listener) {
        this.onEventClickListener = listener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_event_slider, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = events.get(position);

        holder.titleTextView.setText(event.getEventTitle());
        holder.descriptionTextView.setText(event.getEventDescription());

        // Format and set date
        String formattedDate = formatDate(event.getEventDate());
        holder.dateTextView.setText(formattedDate);

        // Decode base64 image and set to ImageView
        String base64Image = event.getImageBase64();
        Log.d("EventImage", "Base64 length: " + (event.getImageBase64() != null ? event.getImageBase64().length() : "null"));

        if (base64Image != null && !base64Image.isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                if (decodedByte != null) {
                    holder.eventImageView.setImageBitmap(decodedByte);
                } else {
                    holder.eventImageView.setImageResource(R.drawable.logo_banner);
                    Log.e("Decode", "Decoded bitmap is null");
                }
            } catch (IllegalArgumentException e) {
                Log.e("Adapter", "Failed to decode base64 image", e);
            }
        } else {
            holder.eventImageView.setImageResource(R.drawable.logo_banner); // fallback
        }

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (onEventClickListener != null) {
                onEventClickListener.onEventClick(event);
            }
        });
    }

    @Override
    public int getItemCount() {
        return events != null ? events.size() : 0;
    }

    private String formatDate(String dateString) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            Date date = inputFormat.parse(dateString);
            return outputFormat.format(date);
        } catch (ParseException e) {
            return dateString; // Return original if parsing fails
        }
    }

    public void updateEvents(List<Event> newEvents) {
        this.events = newEvents;
        notifyDataSetChanged();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        ImageView eventImageView;
        TextView titleTextView;
        TextView descriptionTextView;
        TextView dateTextView;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            eventImageView = itemView.findViewById(R.id.eventImageView);
            titleTextView = itemView.findViewById(R.id.eventTitleTextView);
            descriptionTextView = itemView.findViewById(R.id.eventDescriptionTextView);
            dateTextView = itemView.findViewById(R.id.eventDateTextView);
        }
    }
}