package com.insta.model;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class User {
    private String uid;
    private String instagram;
    private String instaPassword;

    public User() {
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String id) {
        this.uid = uid;
    }

    public String getInstagram() {
        return instagram;
    }

    public void setInstagram(String instagram) {
        this.instagram = instagram;
    }

    public String getInstaPassword() {
        return instaPassword;
    }

    public void setInstaPassword(String instaPassword) {
        this.instaPassword = instaPassword;
    }

    public Map toMap(){
        Map<String, Object> map = new HashMap<>();
        map.put("uid", uid);
        map.put("instagram", instagram);
        map.put("fullName", instaPassword);

        return map;
    }
}
