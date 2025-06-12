package com.example.samajconnectfrontend.models;

public class ReactionRequest {
    private Long userId;
    private String reactionType; // "LIKE" or "DISLIKE"

    // Constructors
    public ReactionRequest() {}

    public ReactionRequest(Long userId, String reactionType) {
        this.userId = userId;
        this.reactionType = reactionType;
    }

    // Getters and Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getReactionType() {
        return reactionType;
    }

    public void setReactionType(String reactionType) {
        this.reactionType = reactionType;
    }

    @Override
    public String toString() {
        return "ReactionRequest{" +
                "userId=" + userId +
                ", reactionType='" + reactionType + '\'' +
                '}';
    }
}