package com.insta;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.insta.Response.StandardResponse;
import com.insta.Response.StatusResponse;
import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import me.postaddict.instagram.scraper.cookie.CookieHashSet;
import me.postaddict.instagram.scraper.cookie.DefaultCookieJar;
import me.postaddict.instagram.scraper.exception.InstagramException;
import me.postaddict.instagram.scraper.interceptor.ErrorInterceptor;
import me.postaddict.instagram.scraper.interceptor.UserAgentInterceptor;
import me.postaddict.instagram.scraper.interceptor.UserAgents;
import me.postaddict.instagram.scraper.model.Account;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.bson.Document;
import spark.Request;
import java.io.*;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static java.lang.Thread.sleep;


public class InstaService {
    private final MongoDatabase db;
    private final MongoCollection<Document> requested;
    private Instagram instagram;
    private Account account;
    private List<Account> fol;
    private List<String> whitelist;
    private List<Account> following;

    public InstaService(MongoDatabase db) {
        this.db = db;
        this.requested = db.getCollection("requested");
        this.whitelist = whitelist();
    }

    public void setF(List<Account> f){
        this.fol = f;
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
                    final List<Account> f =  followers(id, 10);
                    setF(f);

                    for (Account a:fol) {
                        System.out.println("i="+i+"username: "+a.getUsername());
                        if(!a.getRequestedByViewer() && !a.getFollowedByViewer()){
                            i++;
                            instagram.followAccount(a.getId());
                            addRequestedAccount(a);
                            sleep(3000);
                        }
                        if(i==40){
                            System.out.println("i="+i+"username: "+a.getUsername());
                            i = 0;
                            sleep(1000 * 60 * 20);
                        }
                    }
                }
                catch(IOException e){
                    e.printStackTrace();
                } catch (InterruptedException e) {
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

    public Account login(Request body) throws IOException {
        System.out.println("login: " + body.body());
        String name = URLDecoder.decode(body.queryParams("username"),"UTF-8");
        String password = URLDecoder.decode(body.queryParams("password"),"UTF-8");
        doLogin(name, password);
        this.account = instagram.getAccountById(Long.valueOf("3472751680"));
        return account;
    }

    private void doLogin(String username, String password) throws IOException{
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.HEADERS);
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addNetworkInterceptor(loggingInterceptor)
                .addInterceptor(new UserAgentInterceptor(UserAgents.WIN10_CHROME))
                .addInterceptor(new ErrorInterceptor())
                .cookieJar(new DefaultCookieJar(new CookieHashSet()))
                .build();
        this.instagram = new Instagram(httpClient);
        this.instagram.basePage();
        this.instagram.login(username, password);
        this.instagram.basePage();
    }
}