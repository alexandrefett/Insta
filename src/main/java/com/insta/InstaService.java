package com.insta;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.insta.Response.StandardResponse;
import com.insta.Response.StatusResponse;
import com.insta.model.User;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import me.postaddict.instagram.scraper.cookie.CookieHashSet;
import me.postaddict.instagram.scraper.cookie.DefaultCookieJar;
import me.postaddict.instagram.scraper.interceptor.ErrorInterceptor;
import me.postaddict.instagram.scraper.interceptor.UserAgentInterceptor;
import me.postaddict.instagram.scraper.interceptor.UserAgents;
import me.postaddict.instagram.scraper.model.Account;
import me.postaddict.instagram.scraper.model.PageInfo;
import okhttp3.*;
import okhttp3.internal.InternalCache;
import okhttp3.internal.tls.OkHostnameVerifier;
import okhttp3.internal.tls.TrustRootIndex;
import okhttp3.logging.HttpLoggingInterceptor;
import org.bson.Document;
import spark.Request;

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.lang.reflect.Type;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class InstaService {
    private final Firestore firestore;
    private Instagram instagram;
    private Account account;
    private User user;
    private String token;
    private DocumentReference dbref;

    public InstaService(InstaService.Builder builder){
        this.firestore = builder.firestore;
        this.token = builder.token;
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addNetworkInterceptor(loggingInterceptor)
                .addInterceptor(new UserAgentInterceptor(UserAgents.WIN10_CHROME))
                .addInterceptor(new com.fett.interceptor.ErrorInterceptor())
                .cookieJar(new DefaultCookieJar(new CookieHashSet()))
                .build();
        this.instagram = new Instagram(httpClient);
        //this.user = _getUserFireAccount(token);
    }

    public void base(){
        try {
            instagram.basePage();
            instagram.login("hoteiseverest","everest1357.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private User _getUserFireAccount(String token){
        User user = null;
        try{
            DocumentReference docRef = dbref.collection("profile").document(token);
            ApiFuture<DocumentSnapshot> future = docRef.get();
            DocumentSnapshot document = future.get();
            if (document.exists()) {
                user = document.toObject(User.class);
                user.setUid(document.getId());
                return user;
            }
        }
        catch(InterruptedException e){
            e.printStackTrace();
        }
        catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Account getAccount(){
        return account;
    }

    public StandardResponse addwhitelist(String username){
        return addtolist(username, "whitelist");
    }

    public StandardResponse addtofollow(String username){
        return addtolist(username, "whitelist");
    }

    public StandardResponse find(String username){
        if(instagram==null)
            return new StandardResponse(StatusResponse.ERROR, "user not logged");
        try {
            Account account = instagram.getAccountByUsername(username);
        } catch (IOException e) {
            e.printStackTrace();
            return new StandardResponse(StatusResponse.ERROR, e.getMessage());
        }
        return new StandardResponse(StatusResponse.SUCCESS, new Gson().toJsonTree(account));
    }

    public StandardResponse addrequested(Account b){
        if(instagram==null)
            return new StandardResponse(StatusResponse.ERROR, "user not logged");
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("id", b.getId());
            map.put("username", b.getUsername());
            map.put("fullName", b.getFullName());
            map.put("followedByViewer", b.getFollowedByViewer());
            map.put("requestedByViewer", b.getRequestedByViewer());
            map.put("profilePictureUrl", b.getProfilePicUrl());
            map.put("followsViewer", b.getFollowsViewer());
            map.put("isVerified", b.getIsVerified());
            map.put("date", Calendar.getInstance().getTimeInMillis()*-1);

            ApiFuture<WriteResult> result = dbref.collection("requested").document(String.valueOf(b.getId())).set(map);
            return new StandardResponse(StatusResponse.SUCCESS, "OK");
        }
        catch (Exception e){
            e.printStackTrace();
            return new StandardResponse(StatusResponse.ERROR, e.getMessage());
        }
    }

    private  StandardResponse addtolist(String username, String list){
        try {
            Account b = instagram.getAccountByUsername(username);;
            Map<String, Object> map = new HashMap<>();
            map.put("id", b.getId());
            map.put("username", b.getUsername());
            map.put("fullName", b.getFullName());
            map.put("followedByViewer", b.getFollowedByViewer());
            map.put("requestedByViewer", b.getRequestedByViewer());
            map.put("profilePictureUrl", b.getProfilePicUrl());
            map.put("followsViewer", b.getFollowsViewer());
            map.put("isVerified", b.getIsVerified());
            map.put("date", Calendar.getInstance().getTimeInMillis()*-1);

            ApiFuture<WriteResult> result = dbref.collection(list).document(String.valueOf(b.getId())).set(map);
            return new StandardResponse(StatusResponse.SUCCESS, result.toString());
        }
        catch (IOException e){
            e.printStackTrace();
            return new StandardResponse(StatusResponse.ERROR, e.getMessage());
        }

    }

    public StandardResponse follow(String id){
        new Thread() {
            @Override
            public void run() {
                try {
                    int i = 0;
                    final List<Account> f =  instagram.getFollowers(Long.valueOf(id), 20).getNodes();
                    for (Account a:f) {
                        if(!a.getRequestedByViewer() && !a.getFollowedByViewer()){
                            i++;
                            System.out.println("i="+i+"  username: "+a.getUsername());
                            instagram.followAccount(a.getId());
                            addrequested(a);
                            sleep(5000);
                        }
                        if(i==39){
                            System.out.println("Paused for 1 hour.");
                            i = 0;
                            sleep(1000 * 60 * 60);
                        }
                    }
                }
                catch(IOException e){
                    System.out.println("  IOException");
                    try {
                        Thread.sleep(1000 * 60 * 60);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                } catch (InterruptedException e) {
                    System.out.println("  InterruptException");
                    e.printStackTrace();
                }
            }

        }.start();

        return new StandardResponse(StatusResponse.SUCCESS, "Follow thread started");
    }

    public StandardResponse unfollow(){
        new Thread() {
            @Override
            public void run() {
                List<String> whitelist = new ArrayList<>();
                try {
                    int i = 0;
                    List<Account> f = instagram.getFollows(Long.valueOf("3472751680"), 15).getNodes();
                    while (f.size() > 500) {
                        for (Account a : f) {
                            if (!whitelist.contains(a.getUsername())) {
                                i++;
                                instagram.unfollowAccount(a.getId());
                                System.out.println("userid:" + a.getId() +"    username:"+a.getUsername());
                                sleep(3000);
                            }
                            if(i==15) {
                                System.out.println("Paused for 15min");
                                sleep(1000 * 60 * 15);
                                i = 0;
                            }
                        }
                        f = instagram.getFollows(Long.valueOf("3472751680"), 15).getNodes();
                    }
                } catch (UnknownHostException e) {
                    try {
                        System.out.println("UnkonowHostException");
                        System.out.println("Paused for 1 min");
                        sleep(1000 * 60);
                        unfollow();
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
        return new StandardResponse(StatusResponse.SUCCESS, "Unfollow thread started");
    }

    public StandardResponse search(String username){
        if(instagram==null)
            return new StandardResponse(StatusResponse.ERROR, "user not logged");

        try {
            return new StandardResponse(StatusResponse.SUCCESS, new Gson().toJsonTree(instagram.getSearchUserByUsername(username)));
        } catch (IOException e) {
            return new StandardResponse(StatusResponse.ERROR, e.getMessage());
        }
    }

    public StandardResponse followers(String pages){
        if(instagram==null)
            return new StandardResponse(StatusResponse.ERROR, "user not logged");
        try {
            int pagecount = Integer.valueOf(pages);
            return new StandardResponse(StatusResponse.SUCCESS, new Gson().toJsonTree(instagram.getFollowers(account.getId(), pagecount).getNodes()));
        }
        catch(IOException e){
            return new StandardResponse(StatusResponse.ERROR, e.getMessage());
        }
    }

    public StandardResponse follows(String pages){
        if(instagram==null)
            return new StandardResponse(StatusResponse.ERROR, "user not logged");
        try {
            int pagecount = Integer.valueOf(pages);
            return new StandardResponse(StatusResponse.SUCCESS, new Gson().toJsonTree(instagram.getFollows(account.getId(), pagecount).getNodes()));
        }
        catch(IOException e){
            return new StandardResponse(StatusResponse.ERROR, e.getMessage());
        }
    }

    public Account login() throws IOException{
        this.instagram.basePage();
        this.instagram.login(user.getInstagram(), user.getInstapass());
        this.instagram.basePage();
        this.account = this.instagram.getAccountByUsername(user.getInstagram());
        return account;
    }

    public static final class Builder {
        Plan plan;
        Firestore firestore;
        String token;

        public Builder() {
        }

        InstaService.Builder setCredentials(String token){
            this.token = token;
            return this;
        }

        InstaService.Builder setPlan(Plan plan){
            this.plan = plan;
            return this;
        }

        InstaService.Builder setFirestore(Firestore firestore){
            this.firestore = firestore;
            return this;
        }

        public InstaService build() {
            return new InstaService(this);
        }

    }
}