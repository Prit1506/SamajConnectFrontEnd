package com.example.samajconnectfrontend.models;

public class ReactionStats {
    private long likeCount;
    private long dislikeCount;
    private long totalReactions;
    private EventReaction userReaction; // This should be EventReaction object, not long counts
    private double likePercentage;
    private double dislikePercentage;

    // Constructors
    public ReactionStats() {
        this.userReaction = null;
        calculatePercentages();
    }

    public ReactionStats(long likeCount, long dislikeCount) {
        this.likeCount = likeCount;
        this.dislikeCount = dislikeCount;
        this.totalReactions = likeCount + dislikeCount;
        this.userReaction = null;
        calculatePercentages();
    }

    public ReactionStats(long likeCount, long dislikeCount, EventReaction userReaction) {
        this.likeCount = likeCount;
        this.dislikeCount = dislikeCount;
        this.totalReactions = likeCount + dislikeCount;
        this.userReaction = userReaction;
        calculatePercentages();
    }

    // Getters and Setters
    public long getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(long likeCount) {
        this.likeCount = likeCount;
        this.totalReactions = this.likeCount + this.dislikeCount;
        calculatePercentages();
    }

    public long getDislikeCount() {
        return dislikeCount;
    }

    public void setDislikeCount(long dislikeCount) {
        this.dislikeCount = dislikeCount;
        this.totalReactions = this.likeCount + this.dislikeCount;
        calculatePercentages();
    }

    public long getTotalReactions() {
        return totalReactions;
    }

    public void setTotalReactions(long totalReactions) {
        this.totalReactions = totalReactions;
    }

    // Fixed: This should return the user's reaction type as string, not based on counts
    public String getUserReaction() {
        if (userReaction != null && userReaction.getReactionType() != null) {
            return userReaction.getReactionType().toUpperCase();
        }
        return null; // User hasn't reacted
    }

    public EventReaction getUserReactionObject() {
        return userReaction;
    }

    public void setUserReaction(EventReaction userReaction) {
        this.userReaction = userReaction;
    }

    public double getLikePercentage() {
        return likePercentage;
    }

    public void setLikePercentage(double likePercentage) {
        this.likePercentage = likePercentage;
    }

    public double getDislikePercentage() {
        return dislikePercentage;
    }

    public void setDislikePercentage(double dislikePercentage) {
        this.dislikePercentage = dislikePercentage;
    }

    // Utility methods
    private void calculatePercentages() {
        if (totalReactions > 0) {
            likePercentage = (double) likeCount / totalReactions * 100;
            dislikePercentage = (double) dislikeCount / totalReactions * 100;
        } else {
            likePercentage = 0;
            dislikePercentage = 0;
        }
    }

    // Fixed: Check if user has reacted based on userReaction object
    public boolean hasUserReacted() {
        return userReaction != null && userReaction.getReactionType() != null;
    }

    // Fixed: Check if user liked based on userReaction object
    public boolean hasUserLiked() {
        return userReaction != null &&
                userReaction.getReactionType() != null &&
                userReaction.getReactionType().equalsIgnoreCase("LIKE");
    }

    // Fixed: Check if user disliked based on userReaction object
    public boolean hasUserDisliked() {
        return userReaction != null &&
                userReaction.getReactionType() != null &&
                userReaction.getReactionType().equalsIgnoreCase("DISLIKE");
    }

    public String getReactionSummary() {
        if (totalReactions == 0) {
            return "No reactions yet";
        } else if (totalReactions == 1) {
            return "1 reaction";
        } else {
            return totalReactions + " reactions";
        }
    }

    @Override
    public String toString() {
        return "ReactionStats{" +
                "likeCount=" + likeCount +
                ", dislikeCount=" + dislikeCount +
                ", totalReactions=" + totalReactions +
                ", userReaction=" + (userReaction != null ? userReaction.getReactionType() : "null") +
                ", likePercentage=" + likePercentage +
                ", dislikePercentage=" + dislikePercentage +
                '}';
    }
}