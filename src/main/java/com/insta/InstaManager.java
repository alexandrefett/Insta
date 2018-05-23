package com.insta;

import com.fett.model.InstaUser;
import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.google.gson.Gson;
import com.insta.Response.StandardResponse;
import com.insta.Response.StatusResponse;
import com.insta.model.User;
import me.postaddict.instagram.scraper.model.Account;
import spark.Request;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class InstaManager {

    private Firestore db;
    private Map<String, InstaService> usersMap;

    public InstaManager(){
        try {
            usersMap = new HashMap<String, InstaService>();
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
        String token = req.queryParams("token");
        InstaService service = usersMap.get(token);
        try {
            service = new InstaService.Builder()
                .setCredentials(token)
                .setFirestore(db)
                .setPlan(Plan.TOP)
                .build();
            Account account = service.login();
            if(account == null)
                return new StandardResponse(StatusResponse.ERROR, "login fail");
            else
                return new StandardResponse(StatusResponse.SUCCESS, new Gson().toJsonTree(account));
        }
        catch(IOException e){
            return new StandardResponse(StatusResponse.ERROR, e.getMessage());
        }
    }

    public StandardResponse search(Request req){
        String token = req.params("token");
        String username = req.params(":username");
        InstaService service = usersMap.get(token);
        return service.search(username);
    }

    public StandardResponse follow(Request req){
        String username = req.params(":username");
        String token = req.params("token");
        InstaService service = usersMap.get(token);
        return service.follow(username);
    }

    public StandardResponse unfollow(Request req){
        String token = req.params("token");
        InstaService service = usersMap.get(token);
        return service.unfollow();
    }

    public StandardResponse find(Request req){
        String username = req.params(":username");
        String token = req.params("token");
        InstaService service = usersMap.get(token);
        return service.find(username);
    }

    public StandardResponse getwhitelist(Request req){
        String token = req.params("token");
        InstaService service = usersMap.get(token);

        ApiFuture<QuerySnapshot> query = db.collection("users")
                .document(token)
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
        String token = req.params("token");
        InstaService service = usersMap.get(token);
        return service.addwhitelist(username);
    }

    public StandardResponse followers(Request req){
        String pages = req.queryParams("pages");
        String token = req.params("token");
        InstaService service = usersMap.get(token);
        return service.followers(pages);
    }

    public StandardResponse follows(Request req){
        String pages = req.params(":pages");
        String token = req.params("token");
        InstaService service = usersMap.get(token);
        return service.follows(pages);
    }

    public StandardResponse requested(Request req){
        String token = req.params("token");
        InstaService service = usersMap.get(token);
        if(service.getAccount()==null)
            return new StandardResponse(StatusResponse.ERROR, "user not logged");

        try {
            String _offset = req.queryParams("offset"); // is date
            String _limit = req.queryParams("limit");
            long offset = Long.valueOf(_offset);
            int limit = Integer.valueOf(_limit);

            ApiFuture<QuerySnapshot> query = db.collection("users")
                    .document(String.valueOf(service.getAccount().getId()))
                    .collection("requested").orderBy("date").startAt(offset).limit(limit).get();

            QuerySnapshot querySnapshot = query.get();
            List<QueryDocumentSnapshot> documents = querySnapshot.getDocuments();
            List<InstaUser> accounts = new ArrayList<>();
            InstaUser a;
            for (QueryDocumentSnapshot q:documents) {
                a = q.toObject(InstaUser.class);
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
        User user = new Gson().fromJson(req.body(), User.class);
        ApiFuture<WriteResult> result = db.collection("profile").document(user.getUid()).set(user.toMap());
        try {
            System.out.println("WriteResult:" + result.get().toString());
            InstaService s = new InstaService.Builder()
                    .setCredentials(user.getUid())
                    .setFirestore(db)
                    .setPlan(Plan.TOP)
                    .build();

            return new StandardResponse(StatusResponse.SUCCESS, new Gson().toJsonTree(result.get()));

        } catch (InterruptedException e) {
            e.printStackTrace();
            return new StandardResponse(StatusResponse.ERROR, e.getMessage());
        } catch (ExecutionException e) {
            e.printStackTrace();
            return new StandardResponse(StatusResponse.ERROR, e.getMessage());
        }
    }

    public StandardResponse getinstagram(Request req){
        try {
            String uid = req.queryParams("uid"); // is date

            DocumentReference docRef = db.collection("instagram").document(uid);
            ApiFuture<DocumentSnapshot> future = docRef.get();
            DocumentSnapshot document = future.get();
            User user = null;
            if (document.exists()) {
                user = document.toObject(User.class);
                System.out.println(user);
            }
            return new StandardResponse(StatusResponse.SUCCESS, new Gson().toJsonTree(user));
        }
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
}