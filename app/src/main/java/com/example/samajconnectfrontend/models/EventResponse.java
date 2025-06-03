package com.example.samajconnectfrontend.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class EventResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("events")
    private List<Event> events;

    @SerializedName("count")
    private int count;

    // Constructors
    public EventResponse() {}

    public EventResponse(boolean success, String message, List<Event> events, int count) {
        this.success = success;
        this.message = message;
        this.events = events;
        this.count = count;
    }

    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<Event> getEvents() { return events; }
    public void setEvents(List<Event> events) { this.events = events; }

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
}
