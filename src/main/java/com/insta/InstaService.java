package com.insta;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.insta.Response.StandardResponse;
import com.insta.Response.StatusResponse;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import me.postaddict.instagram.scraper.cookie.CookieHashSet;
import me.postaddict.instagram.scraper.cookie.DefaultCookieJar;
import me.postaddict.instagram.scraper.interceptor.ErrorInterceptor;
import me.postaddict.instagram.scraper.interceptor.UserAgentInterceptor;
import me.postaddict.instagram.scraper.interceptor.UserAgents;
import me.postaddict.instagram.scraper.model.Account;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.bson.Document;
import spark.Request;
import java.io.*;
import java.lang.reflect.Type;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class InstaService {
    private final MongoDatabase db;
    private final MongoCollection<Document> requested;
    private final Firestore fs;
    private Instagram instagram;
    private Account account;
    private List<Account> fol;
    private List<String> whitelist;

    public InstaService(MongoDatabase db, Firestore fs) {
        this.db = db;
        this.fs = fs;
        this.requested = db.getCollection("requested");
        this.whitelist = whitelist();
    }


    public StandardResponse addwhitelist(String userlist, String username){
        if(instagram==null)
            return new StandardResponse(StatusResponse.ERROR, "Do login first");
        return adduserdb(userlist, username, "whitelist");
    }


    public StandardResponse addrequested(String userlist, String username){
        if(instagram==null)
            return new StandardResponse(StatusResponse.ERROR, "Do login first");
        return adduserdb(userlist, username, "requested");
    }

    private  StandardResponse adduserdb(String userlist, String username, String list){
        try {
            instagram.basePage();
            Account a = instagram.getAccountByUsername(userlist);
            Account b = instagram.getAccountByUsername(username);;

            DocumentReference doc = fs.collection("users").document(String.valueOf(a.getId()));

            Map<String, Object> map = new HashMap<>();
            map.put("id", b.getId());
            map.put("username", b.getUsername());
            map.put("fullName", b.getFullName());
            map.put("followedByViewer", b.getFollowedByViewer());
            map.put("requestedByViewer", b.getRequestedByViewer());
            map.put("profilePictureUrl", b.getProfilePicUrl());
            map.put("biography", b.getBiography());
            map.put("follows", b.getFollows());
            map.put("followsViewer", b.getFollowsViewer());
            map.put("hasRequestedViewer", b.getHasRequestedViewer());
            map.put("isPrivate", b.getIsPrivate());
            map.put("isVerified", b.getIsVerified());

            ApiFuture<WriteResult> result = doc.collection(list).document(String.valueOf(b.getUsername())).set(map);
            return new StandardResponse(StatusResponse.SUCCESS, "OK");
        }
        catch (IOException e){
            e.printStackTrace();
            return new StandardResponse(StatusResponse.ERROR, e.getMessage());
        }

    }

    public StandardResponse find(String username){
        if(instagram==null)
            return new StandardResponse(StatusResponse.ERROR, "Do login first");
        try {
            return new StandardResponse(StatusResponse.SUCCESS, new Gson().toJsonTree(instagram.getAccountByUsername(username)));
        }
        catch(IOException e){
            e.printStackTrace();
            return new StandardResponse(StatusResponse.ERROR, e.getMessage());
        }
    }

    public StandardResponse doFollow(String id){
        if(instagram==null)
            return new StandardResponse(StatusResponse.ERROR, "Do login first");

        new Thread() {
            @Override
            public void run() {
                try {
                    int i = 0;
                    final List<Account> f =  followers(id, 20);
                    for (Account a:fol) {
                        if(!a.getRequestedByViewer() && !a.getFollowedByViewer()){
                            i++;
                            System.out.println("i="+i+"  username: "+a.getUsername());
                            instagram.followAccount(a.getId());
                            addRequestedAccount(a);
                            sleep(5000);
                        }
                        if(i==39){
                            System.out.println("Paused for 20min.");
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

        return new StandardResponse(StatusResponse.SUCCESS, "Thread started");
    }

    public List<String> whitelist(){
        ArrayList<String> r  = new ArrayList<String>();

        File f = new File("whitelist.csv");
        if(f.exists()){
            BufferedReader br = null;
            String line = "";
            try {
                br = new BufferedReader(new FileReader("whitelist.csv"));
                String username="";
                while ((line = br.readLine()) != null) {
                    r.add(line);
                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return r;
    }

    public StandardResponse doUnFollow(){
        if(instagram==null)
            return new StandardResponse(StatusResponse.ERROR, "Do login first");

        new Thread() {
            @Override
            public void run() {
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
                        System.out.println("Paused for 1min");
                        sleep(1000 * 60);
                        doUnFollow();
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
        return new StandardResponse(StatusResponse.SUCCESS, "Thread started");
    }

    public StandardResponse getSearchUser(String username){
        if(instagram==null)
            return new StandardResponse(StatusResponse.ERROR, "Do login first");

        try {
            return new StandardResponse(StatusResponse.SUCCESS, new Gson().toJsonTree(instagram.getSearchUserByUsername(username)));
        } catch (IOException e) {
            return new StandardResponse(StatusResponse.ERROR, e.getMessage());
        }

    }

    public String status(){
        return "Requested accounts: "+requested.count();
    }

    private void addRequestedAccount(Account a){
        Document d = new Document("user", a.getUsername())
                .append("id", ""+a.getId())
                .append("fullname", a.getFullName())
                .append("profilepicurl", a.getProfilePicUrl())
                .append("requeste_by_viewer", a.getRequestedByViewer())
                .append("followed_by_viewer", a.getFollowedByViewer())
                .append("date", ""+Calendar.getInstance().getTimeInMillis());
        requested.insertOne(d);
    }

    public List<Account> followers(String id, int pagecount) throws IOException{
        if(instagram==null)
            return null;
        return instagram.getFollowers(Long.parseLong(id), pagecount).getNodes();
    }

    public String requested(Request body) throws IOException {
        String r="";
        MongoCursor<Document> cursor = requested.find().iterator();
        try {
            while (cursor.hasNext()) {
                r +=cursor.next().toJson() +"<BR>";
            }
        } finally {
            cursor.close();
        }
        return r;
    }

    public StandardResponse login(Request body) throws IOException {
        String name = URLDecoder.decode(body.queryParams("username"),"UTF-8");
        String password = URLDecoder.decode(body.queryParams("password"),"UTF-8");
        try{
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.NONE);
            OkHttpClient httpClient = new OkHttpClient.Builder()
                    .addNetworkInterceptor(loggingInterceptor)
                    .addInterceptor(new UserAgentInterceptor(UserAgents.WIN10_CHROME))
                    .addInterceptor(new com.fett.interceptor.ErrorInterceptor())
                    .cookieJar(new DefaultCookieJar(new CookieHashSet()))
                    .build();
            this.instagram = new Instagram(httpClient);
            this.instagram.basePage();
            this.instagram.login(name, password);
            this.instagram.basePage();
            return new StandardResponse(StatusResponse.SUCCESS, new Gson().toJsonTree(instagram.getAccountByUsername(name)));
        }
        catch(IOException e){
            e.printStackTrace();
            return new StandardResponse(StatusResponse.ERROR, e.getMessage());
        }
    }
}