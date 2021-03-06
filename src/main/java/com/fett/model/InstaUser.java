
package com.fett.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class InstaUser {
    @SerializedName("followedByViewer")
    @Expose
    Boolean followedByViewer;
    @SerializedName("followsViewer")
    @Expose
    Boolean followsViewer;
    @SerializedName("fullName")
    @Expose
    String fullName;
    @SerializedName("id")
    @Expose
    long id;
    @SerializedName("isVerified")
    @Expose
    Boolean isVerified;
    @SerializedName("profilePictureUrl")
    @Expose
    String profilePictureUrl;
    @SerializedName("requestedByViewer")
    @Expose
    Boolean requestedByViewer;
    @SerializedName("username")
    @Expose
    String username;
    @SerializedName("date")
    @Expose
    private long date;






    public InstaUser(){
    }

    public Boolean getFollowedByViewer() {
        return followedByViewer;
    }

    public void setFollowedByViewer(Boolean followedByViewer) {
        this.followedByViewer = followedByViewer;
    }

    public Boolean getFollowsViewer() {
        return followsViewer;
    }

    public void setFollowsViewer(Boolean followsViewer) {
        this.followsViewer = followsViewer;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Boolean getIsVerified() {
        return isVerified;
    }

    public void setIsVerified(Boolean isVerified) {
        this.isVerified = isVerified;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    public Boolean getRequestedByViewer() {
        return requestedByViewer;
    }

    public void setRequestedByViewer(Boolean requestedByViewer) {
        this.requestedByViewer = requestedByViewer;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }
}
