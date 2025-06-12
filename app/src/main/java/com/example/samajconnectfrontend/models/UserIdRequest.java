package com.example.samajconnectfrontend.models;

public class UserIdRequest {
    private Long userId;

    public UserIdRequest(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }
}
