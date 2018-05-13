package com.fett.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Users {

    @SerializedName("position")
    @Expose
    private Integer position;
    @SerializedName("user")
    @Expose
    private InstaUser user;

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public InstaUser getUser() {
        return user;
    }

    public void setUser(InstaUser user) {
        this.user = user;
    }

}
