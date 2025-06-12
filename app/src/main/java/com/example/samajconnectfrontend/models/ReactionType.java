package com.example.samajconnectfrontend.models;

public enum ReactionType {
    LIKE("like", "👍"),
    DISLIKE("dislike", "👎");

    private final String value;
    private final String emoji;

    ReactionType(String value, String emoji) {
        this.value = value;
        this.emoji = emoji;
    }

    public String getValue() {
        return value;
    }

    public String getEmoji() {
        return emoji;
    }

    public static ReactionType fromString(String text) {
        if (text == null) {
            throw new IllegalArgumentException("Text cannot be null");
        }

        for (ReactionType type : ReactionType.values()) {
            if (type.value.equalsIgnoreCase(text) || type.name().equalsIgnoreCase(text)) {
                return type;
            }
        }
        throw new IllegalArgumentException("No constant with text " + text + " found");
    }

    public boolean isPositive() {
        return this == LIKE;
    }

    public boolean isNegative() {
        return this == DISLIKE;
    }

    @Override
    public String toString() {
        return value;
    }
}