package com.insta;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.insta.Response.StandardResponse;
import com.insta.Response.StatusResponse;
import spark.Request;

import java.io.FileInputStream;

public class InstaManager {

    private InstaService services;

    public InstaManager(){
    }

    public StandardResponse login(Request req){
        String username = req.params("username");
        String password = req.params("password");

        try {
            FileInputStream serviceAccount = new FileInputStream("instamanager-908a3-aab9f9f25fd5.json");
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl("https://instamanager-908a3.firebaseio.com")
                    .build();

            FirebaseApp.initializeApp(options);
            Firestore db = FirestoreClient.getFirestore();

            services = new InstaService.Builder()
                    .setCredentials(username, password)
                    .setFirestore(db)
                    .setPlan(Plan.TOP)
                    .build();

        }
        catch(Exception e){
            e.printStackTrace();
            return new StandardResponse(StatusResponse.ERROR, e.getMessage());
        }
        return services.login();
    }

    public StandardResponse search(Request req){
        String username = req.params(":username");
        return services.search(username);
    }

    public StandardResponse follow(Request req){
        String username = req.params(":username");
        return services.follow(username);
    }

    public StandardResponse unfollow(Request req){
        return services.unfollow();
    }

    public StandardResponse find(Request req){
        String username = req.params(":username");
        return services.find(username);
    }

    public StandardResponse addwhitelist(Request req){
        String username = req.params(":username");
        return services.addwhitelist(username);
    }

    public StandardResponse followers(Request req){
        String pages = req.params(":pages");
        return services.followers(pages);
    }

    public StandardResponse follows(Request req){
        String pages = req.params(":pages");
        return services.follows(pages);
    }

    public StandardResponse requested(Request req){
        return services.requested();
    }
}