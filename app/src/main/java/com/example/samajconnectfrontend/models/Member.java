package com.example.samajconnectfrontend.models;

public class Member {
    private int id;
    private String name;
    private String email;
    private String gender;
    private String phoneNumber;
    private String address;
    private String profileImageBase64;
    private boolean isAdmin;
    private String createdAt;
    private String updatedAt;
    private int samajId;
    private String samajName;

    // Constructors
    public Member() {}

    public Member(int id, String name, String email, String gender, String phoneNumber,
                  String address, String profileImageBase64, boolean isAdmin,
                  String createdAt, String updatedAt, int samajId, String samajName) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.gender = gender;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.profileImageBase64 = profileImageBase64;
        this.isAdmin = isAdmin;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.samajId = samajId;
        this.samajName = samajName;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getProfileImageBase64() { return profileImageBase64; }
    public void setProfileImageBase64(String profileImageBase64) { this.profileImageBase64 = profileImageBase64; }

    public boolean isAdmin() { return isAdmin; }
    public void setAdmin(boolean admin) { isAdmin = admin; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public int getSamajId() { return samajId; }
    public void setSamajId(int samajId) { this.samajId = samajId; }

    public String getSamajName() { return samajName; }
    public void setSamajName(String samajName) { this.samajName = samajName; }
}
