package com.example.samajconnectfrontend.models;

import com.google.gson.annotations.SerializedName;

public class Samaj {
    @SerializedName("id")
    private Long id;

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("rules")
    private String rules;

    @SerializedName("establishedDate")
    private String establishedDate;

    @SerializedName("memberCount")
    private Integer memberCount;

    // Default constructor
    public Samaj() {}

    // Constructor with basic fields
    public Samaj(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getRules() {
        return rules;
    }

    public String getEstablishedDate() {
        return establishedDate;
    }

    public Integer getMemberCount() {
        return memberCount;
    }

    // Setters
    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setRules(String rules) {
        this.rules = rules;
    }

    public void setEstablishedDate(String establishedDate) {
        this.establishedDate = establishedDate;
    }

    public void setMemberCount(Integer memberCount) {
        this.memberCount = memberCount;
    }

    @Override
    public String toString() {
        return "Samaj{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", rules='" + rules + '\'' +
                ", establishedDate='" + establishedDate + '\'' +
                ", memberCount=" + memberCount +
                '}';
    }
}