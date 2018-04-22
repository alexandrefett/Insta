package com.insta;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import me.postaddict.instagram.scraper.Instagram;
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
import java.io.IOException;
import java.net.URLDecoder;
import java.util.List;


public class InstaService {
    private final MongoDatabase db;
    private final MongoCollection<Document> collection;
    private Instagram instagram;
    private Account account;

    public InstaService(MongoDatabase db) {
        this.db = db;
        this.collection = db.getCollection("users");
    }

    public List<Account> followers(String id) throws IOException{
        if(instagram==null)
            return null;
        return instagram.getFollowers(Long.parseLong(id), 800).getNodes();
    }

    public String reset() {
        DeleteResult r = collection.deleteMany(new Document());
        return r.getDeletedCount() + " OK";
    }

    public Account find(String id) throws IOException{
        long userid = Long.valueOf(id);
        return instagram.getAccountById(userid);
    }

    private boolean addAccountMongoDB(String name, String email, String mac){
        try {
            Document u = new Document("name", name).append("email", email).append("mac", mac);
            System.out.println("addAccountMongoDB:" + u.toJson());
            collection.insertOne(u);
        } catch (MongoException e) {
            System.out.println("MongoException:" + e.getMessage());
            return false;
        }
        return true;
    }

    public Account login(Request body) throws IOException {
        System.out.println("login: " + body.body());
        String name = URLDecoder.decode(body.queryParams("username"),"UTF-8");
        String password = URLDecoder.decode(body.queryParams("password"),"UTF-8");
        doLogin(name, password);
        this.account = instagram.getAccountByUsername("hoteiseverest");
        return this.account;
    }

    private Account doLogin(String username, String password) throws IOException{
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
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

        return instagram.getAccountByUsername("hoteiseverest");
    }
}

