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
    private String username;
    private String password;
    private List<Account> fol;
    private List<String> whitelist;
    private DocumentReference dbref;


    public InstaService(InstaService.Builder builder){
        this.firestore = builder.firestore;
        this.username = builder.username;
        this.password = builder.password;
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.NONE);
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addNetworkInterceptor(loggingInterceptor)
                .addInterceptor(new UserAgentInterceptor(UserAgents.WIN10_CHROME))
                .addInterceptor(new com.fett.interceptor.ErrorInterceptor())
                .cookieJar(new DefaultCookieJar(new CookieHashSet()))
                .build();
        this.instagram = new Instagram(httpClient);
    }

    public StandardResponse addwhitelist(String userlist, String username){
        if(instagram==null)
            return new StandardResponse(StatusResponse.ERROR, "Do login first");
        return addtolist(username, "whitelist");
    }


    public StandardResponse addrequested(String username){
        if(instagram==null)
            return new StandardResponse(StatusResponse.ERROR, "Do login first");
        return addtolist(username, "requested");
    }

    private  StandardResponse addtolist(String username, String list){
        try {
            instagram.basePage();
            Account b = instagram.getAccountByUsername(username);;

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

            ApiFuture<WriteResult> result = dbref.collection(list).document(String.valueOf(b.getUsername())).set(map);
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

    public List<Account> followers(String id, int pagecount) throws IOException{
        if(instagram==null)
            return null;
        return instagram.getFollowers(Long.parseLong(id), pagecount).getNodes();
    }

    public StandardResponse login() {
        try {
            this.instagram.basePage();
            this.instagram.login(username, password);
            this.instagram.basePage();
            this.account = this.instagram.getAccountByUsername(username);
            this.dbref = firestore.collection("users").document(String.valueOf(account.getId()));
        }
        catch(IOException e){
            new StandardResponse(StatusResponse.ERROR, e.getMessage());
            e.printStackTrace();
        }
        return new StandardResponse(StatusResponse.SUCCESS, "OK");
    }

    public static final class Builder {
        String username;
        String password;
        Plan plan;
        Firestore firestore;

        public Builder() {
            this.username = "";
            this.password = "";
        }

        InstaService.Builder setCredentials(String username, String password){
            this.username = username;
            this.password = password;
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