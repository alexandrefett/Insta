package com.insta;

import com.fett.model.User;
import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.cloud.FirestoreClient;
import com.google.gson.Gson;
import com.insta.Response.StandardResponse;
import com.insta.Response.StatusResponse;
import me.postaddict.instagram.scraper.model.Account;
import org.mongodb.morphia.query.QueryException;
import spark.Request;
import spark.utils.StringUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class InstaManager {

    private InstaService services;
    private Firestore db;

    public InstaManager(){
        try {
            FileInputStream serviceAccount = new FileInputStream("instamanager-908a3-aab9f9f25fd5.json");
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl("https://instamanager-908a3.firebaseio.com")
                    .build();

            FirebaseApp.initializeApp(options);
            db = FirestoreClient.getFirestore();
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    public StandardResponse login(Request req){
        String username = req.queryParams("username");
        String password = req.queryParams("password");

        try {
                services = new InstaService.Builder()
                    .setCredentials(username, password)
                    .setFirestore(db)
                    .setPlan(Plan.TOP)
                    .build();

            return services.login();
        }
        catch(IOException e){
            return new StandardResponse(StatusResponse.ERROR, e.getMessage());
        }
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

    public StandardResponse getwhitelist(Request req){
        String apikey = req.queryParams("apikey");


        ApiFuture<QuerySnapshot> query = db.collection("users")
                .document("abc")
                .collection("whitelist").get();

        try {
            QuerySnapshot querySnapshot = query.get();
            List<QueryDocumentSnapshot> documents = querySnapshot.getDocuments();
            return new StandardResponse(StatusResponse.SUCCESS,new Gson().toJsonTree(documents));
        }
        catch(InterruptedException e){
            e.printStackTrace();
            return new StandardResponse(StatusResponse.ERROR, e.getMessage());
        }
        catch(ExecutionException e){
            e.printStackTrace();
            return new StandardResponse(StatusResponse.ERROR, e.getMessage());
        }
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
        if(services.getAccount()==null)
            return new StandardResponse(StatusResponse.ERROR, "user not logged");

        try {
            String _offset = req.queryParams("offset"); // is date
            String _limit = req.queryParams("limit");
            long offset = Long.valueOf(_offset);
            int limit = Integer.valueOf(_limit);

            ApiFuture<QuerySnapshot> query = db.collection("users")
                    .document(String.valueOf(services.getAccount().getId()))
                    .collection("requested").orderBy("date").startAt(offset).limit(limit).get();

            QuerySnapshot querySnapshot = query.get();
            List<QueryDocumentSnapshot> documents = querySnapshot.getDocuments();
            List<User> accounts = new ArrayList<>();
            User a;
            for (QueryDocumentSnapshot q:documents) {
                a = q.toObject(User.class);
                accounts.add(a);
            }
            return new StandardResponse(StatusResponse.SUCCESS, new Gson().toJsonTree(accounts));        }
        catch(InterruptedException e){
            e.printStackTrace();
            return new StandardResponse(StatusResponse.ERROR, e.getMessage());
        }
        catch(ExecutionException e){
            e.printStackTrace();
            return new StandardResponse(StatusResponse.ERROR, e.getMessage());
        }
        catch(Exception e){
            e.printStackTrace();
            return new StandardResponse(StatusResponse.ERROR, e.getMessage());
        }
    }

    public StandardResponse register(Request req){
        String name = req.queryParams("name");
        String email = req.queryParams("email");
        String password = req.queryParams("password");

        UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                .setEmail(email)
                .setEmailVerified(false)
                .setPassword(password)
                .setDisplayName(name)
                .setDisabled(false);

        UserRecord userRecord = null;
        try {
            userRecord = FirebaseAuth.getInstance().createUserAsync(request).get();
            return new StandardResponse(StatusResponse.SUCCESS, new Gson().toJsonTree(userRecord));

        } catch (InterruptedException e) {
            e.printStackTrace();
            return new StandardResponse(StatusResponse.ERROR, e.getMessage());
        } catch (ExecutionException e) {
            e.printStackTrace();
            return new StandardResponse(StatusResponse.ERROR, e.getMessage());
        }
    }
}