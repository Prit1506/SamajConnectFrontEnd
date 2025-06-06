package com.example.samajconnectfrontend.models;

import com.google.gson.annotations.SerializedName;

public class Event {
    private Long id;

    @SerializedName("eventTitle")
    private String eventTitle;

    @SerializedName("eventDescription")
    private String eventDescription;

    @SerializedName("eventDate")
    private String eventDate;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("createdBy")
    private Long createdBy;

    @SerializedName("samajId")
    private Long samajId;

    @SerializedName("imageBase64")
    private String imageBase64;

    // Additional fields from your existing functionality
    private String title;
    private String description;
    private String location;
    private String eventTime;
    private String imageUrl;

    // Default constructor
    public Event() {}

    // Constructor with all fields
    public Event(Long id, String eventTitle, String eventDescription, String eventDate,
                 String createdAt, Long createdBy, Long samajId, String imageBase64,
                 String title, String description, String location, String eventTime, String imageUrl) {
        this.id = id;
        this.eventTitle = eventTitle;
        this.eventDescription = eventDescription;
        this.eventDate = eventDate;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.samajId = samajId;
        this.imageBase64 = imageBase64;
        this.title = title;
        this.description = description;
        this.location = location;
        this.eventTime = eventTime;
        this.imageUrl = imageUrl;
    }

    // Getters and Setters for API fields
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEventTitle() { return eventTitle; }
    public void setEventTitle(String eventTitle) {
        this.eventTitle = eventTitle;
        this.title = eventTitle; // Keep both in sync
    }

    public String getEventDescription() { return eventDescription; }
    public void setEventDescription(String eventDescription) {
        this.eventDescription = eventDescription;
        this.description = eventDescription; // Keep both in sync
    }

    public String getEventDate() { return eventDate; }
    public void setEventDate(String eventDate) { this.eventDate = eventDate; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }

    public Long getSamajId() { return samajId; }
    public void setSamajId(Long samajId) { this.samajId = samajId; }

    public String getImageBase64() { return imageBase64; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }

    // Getters and Setters for existing functionality fields
    public String getTitle() {
        return title != null ? title : eventTitle;
    }
    public void setTitle(String title) {
        this.title = title;
        this.eventTitle = title; // Keep both in sync
    }

    public String getDescription() {
        return description != null ? description : eventDescription;
    }
    public void setDescription(String description) {
        this.description = description;
        this.eventDescription = description; // Keep both in sync
    }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getEventTime() { return eventTime; }
    public void setEventTime(String eventTime) { this.eventTime = eventTime; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    // Convenience method to get samajId as long (for backward compatibility)
    public long getSamajIdAsLong() {
        return samajId != null ? samajId : 0L;
    }

    // Convenience method to get id as long (for backward compatibility)
    public long getIdAsLong() {
        return id != null ? id : 0L;
    }

    // Helper method to get formatted date and time
    public String getFormattedDateTime() {
        if (eventDate != null && eventTime != null) {
            return eventDate + " • " + eventTime;
        } else if (eventDate != null) {
            return eventDate;
        } else if (eventTime != null) {
            return eventTime;
        } else {
            return "Date & Time TBD";
        }
    }

    @Override
    public String toString() {
        return "Event{" +
                "id=" + id +
                ", eventTitle='" + eventTitle + '\'' +
                ", eventDescription='" + eventDescription + '\'' +
                ", eventDate='" + eventDate + '\'' +
                ", createdAt='" + createdAt + '\'' +
                ", createdBy=" + createdBy +
                ", samajId=" + samajId +
                ", imageBase64='" + (imageBase64 != null ? "[BASE64_DATA]" : null) + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", location='" + location + '\'' +
                ", eventTime='" + eventTime + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                '}';
    }
}