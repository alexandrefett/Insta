package com.fett.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import me.postaddict.instagram.scraper.model.Account;

public class Users {

    @SerializedName("position")
    @Expose
    private Integer position;
    @SerializedName("user")
    @Expose
    private User user;

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

}
