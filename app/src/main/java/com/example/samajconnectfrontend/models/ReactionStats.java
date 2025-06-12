package com.example.samajconnectfrontend.models;

public class ReactionStats {
    private long likeCount;
    private long dislikeCount;
    private long totalReactions;
    private EventReaction userReaction;
    private double likePercentage;
    private double dislikePercentage;

    // Constructors
    public ReactionStats() {}

    public ReactionStats(long likeCount, long dislikeCount) {
        this.likeCount = likeCount;
        this.dislikeCount = dislikeCount;
        this.totalReactions = likeCount + dislikeCount;
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

    public String getUserReaction() {
        return likeCount>=1?"LIKE":"DISLIKE";
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

    public boolean hasUserReacted() {
        return likeCount >= 1 || dislikeCount >= 1;
    }

    public boolean hasUserLiked() {
        return likeCount >= 1;
    }

    public boolean hasUserDisliked() {
        return dislikeCount >= 1;
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
                ", userReaction=" + userReaction +
                ", likePercentage=" + likePercentage +
                ", dislikePercentage=" + dislikePercentage +
                '}';
    }
}