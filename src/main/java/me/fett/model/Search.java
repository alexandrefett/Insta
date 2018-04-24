package me.fett.model;

import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import me.postaddict.instagram.scraper.model.Account;

public class Search {

    @SerializedName("users")
    @Expose
    private List<Users> users = null;

    public List<Users> getUsers() {
        return users;
    }

    public void setUsers(List<Users> users) {
        this.users = users;
    }
}