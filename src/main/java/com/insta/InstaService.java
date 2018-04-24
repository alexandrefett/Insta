package com.insta;

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
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


public class InstaService {
    private final MongoDatabase db;
    private final MongoCollection<Document> requested;
    private Instagram instagram;
    private Account account;
    private List<Account> fol;

    public InstaService(MongoDatabase db) {
        this.db = db;
        this.requested = db.getCollection("requested");
    }

    public void setF(List<Account> f){
        this.fol = f;
    }

    public String doFollow(String id){
        if(instagram==null)
            return "Need login";

        try{
            final List<Account> f =  followers(id);
            setF(f);
        }
        catch(IOException e){
            e.printStackTrace();
        }

        new Thread() {
            @Override
            public void run() {
                try {
                    for (Account a:fol) {
                        System.out.println("Node: "+a.toString());
                        if(!a.getRequestedByViewer() && !a.getFollowedByViewer()){
                            instagram.followAccount(a.getId());
                            addRequestedAccount(a);
                            Thread.sleep(3000);
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

        return "followers: " + this.fol.size();
    }

    public String status(){
        return "Requested accounts: "+requested.count();
    }

    private void addRequestedAccount(Account a){
        Document d = new Document("user", a.getUsername())
                .append("id", ""+a.getId())
                .append("fullname", a.getFullName())
                .append("date", ""+Calendar.getInstance().getTimeInMillis());
        requested.insertOne(d);
    }

    public List<Account> followers(String id) throws IOException{
        if(instagram==null)
            return null;
        return instagram.getFollowers(Long.parseLong(id), 1).getNodes();
        //https://www.instagram.com/graphql/query/?query_hash=37479f2b8209594dde7facb0d904896a&variables=%7B%22id%22%3A%224966510341%22%2C%22first%22%3A24%7D

    }


    public Account find(String id) throws IOException{
        long userid = Long.valueOf(id);
        System.out.println("userid:" + userid);
        System.out.println("--------------");
        return instagram.getAccountById(userid);
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
        this.account = find("3472751680");
        return account;
    }

    private void doLogin(String username, String password) throws IOException{
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addNetworkInterceptor(loggingInterceptor)
                .addInterceptor(new UserAgentInterceptor(UserAgents.OSX_CHROME))
                .addInterceptor(new ErrorInterceptor())
                .cookieJar(new DefaultCookieJar(new CookieHashSet()))
                .build();
        this.instagram = new Instagram(httpClient);
        this.instagram.basePage();
        this.instagram.login(username, password);
        this.instagram.basePage();
    }
}

//https://www.instagram.com/graphql/query/?query_hash=37479f2b8209594dde7facb0d904896a&variables=%7B%22id%22%3A1341252671%2C%22first%22%3A24%7D
//https://www.instagram.com/graphql/query/?query_hash=37479f2b8209594dde7facb0d904896a&variables=%7B%22id%22%3A1341252671%2C%22first%22%3A200%7D

//https://www.instagram.com/graphql/query/?query_hash=37479f2b8209594dde7facb0d904896a&variables={%22id%22:%201341252671,%20%22first%22:%20200,%20%22after%22:%20%22%22}


