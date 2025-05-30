package com.example.samajconnectfrontend;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.List;

public class SamajSuggestionAdapter extends RecyclerView.Adapter<SamajSuggestionAdapter.ViewHolder> {

    private List<JSONObject> samajList;
    private OnSamajClickListener listener;

    public interface OnSamajClickListener {
        void onSamajClick(JSONObject samaj);
    }

    public SamajSuggestionAdapter(List<JSONObject> samajList, OnSamajClickListener listener) {
        this.samajList = samajList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_samaj_suggestion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JSONObject samaj = samajList.get(position);
        try {
            String samajName = samaj.getString("name");
            String samajDescription = samaj.optString("description", "");

            holder.textViewSamajName.setText(samajName);
            holder.textViewSamajDescription.setText(samajDescription);

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSamajClick(samaj);
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return samajList.size();
    }

    public void updateSuggestions(List<JSONObject> newSuggestions) {
        this.samajList = newSuggestions;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewSamajName;
        TextView textViewSamajDescription;

        ViewHolder(View itemView) {
            super(itemView);
            textViewSamajName = itemView.findViewById(R.id.textViewSamajName);
            textViewSamajDescription = itemView.findViewById(R.id.textViewSamajDescription);
        }
    }
}