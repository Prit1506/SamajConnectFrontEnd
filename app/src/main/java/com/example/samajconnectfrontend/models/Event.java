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

    // Constructors
    public Event() {}

    public Event(Long id, String eventTitle, String eventDescription, String eventDate,
                 String createdAt, Long createdBy, Long samajId, String imageBase64) {
        this.id = id;
        this.eventTitle = eventTitle;
        this.eventDescription = eventDescription;
        this.eventDate = eventDate;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.samajId = samajId;
        this.imageBase64 = imageBase64;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEventTitle() { return eventTitle; }
    public void setEventTitle(String eventTitle) { this.eventTitle = eventTitle; }

    public String getEventDescription() { return eventDescription; }
    public void setEventDescription(String eventDescription) { this.eventDescription = eventDescription; }

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
}
