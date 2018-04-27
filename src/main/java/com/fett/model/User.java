package com.fett.model;

import lombok.Data;

@Data
public class User {
    private String pk;
    private String username;
    private String full_name;
    private boolean is_private;
    private String profile_pic_url;
    private boolean is_verified;
    private boolean has_anonymous_profile_picture;
    private int follower_count;
    private String byline;
    private double mutual_followers_count;
    private boolean following;
    private boolean outgoing_request;
}