package com.example.samajconnectfrontend.models;

import com.google.gson.annotations.SerializedName;

public class SamajResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("samaj")
    private Samaj samaj;

    // Default constructor
    public SamajResponse() {}

    // Constructor
    public SamajResponse(boolean success, String message, Samaj samaj) {
        this.success = success;
        this.message = message;
        this.samaj = samaj;
    }

    // Getters
    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Samaj getSamaj() {
        return samaj;
    }

    // Setters
    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setSamaj(Samaj samaj) {
        this.samaj = samaj;
    }

    @Override
    public String toString() {
        return "SamajResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", samaj=" + samaj +
                '}';
    }
}