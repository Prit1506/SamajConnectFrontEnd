package com.example.samajconnectfrontend.models;

public class EventReaction {
    private Long id;
    private Long userId;
    private Long eventId;
    private String reactionType; // "LIKE" or "DISLIKE"
    private String createdAt;
    private String updatedAt;

    // Constructors
    public EventReaction() {}

    public EventReaction(Long userId, Long eventId, String reactionType) {
        this.userId = userId;
        this.eventId = eventId;
        this.reactionType = reactionType;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public String getReactionType() {
        return reactionType;
    }

    public void setReactionType(String reactionType) {
        this.reactionType = reactionType;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Utility methods
    public boolean isLike() {
        return "LIKE".equals(reactionType);
    }

    public boolean isDislike() {
        return "DISLIKE".equals(reactionType);
    }

    @Override
    public String toString() {
        return "EventReaction{" +
                "id=" + id +
                ", userId=" + userId +
                ", eventId=" + eventId +
                ", reactionType='" + reactionType + '\'' +
                ", createdAt='" + createdAt + '\'' +
                ", updatedAt='" + updatedAt + '\'' +
                '}';
    }
}