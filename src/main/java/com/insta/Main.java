package com.insta;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.io.InputStream;

public class Main {

    public static void main(String[] args) {
        BasicConfigurator.configure();
        Logger.getLogger("org").setLevel(Level.OFF);
        Logger.getLogger("akka").setLevel(Level.OFF);


        try {

            FileInputStream serviceAccount =  new FileInputStream("instamanager-908a3-aab9f9f25fd5.json");

            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl("https://instamanager-908a3.firebaseio.com")
                    .build();

            FirebaseApp.initializeApp(options);
            Firestore db = FirestoreClient.getFirestore();


            new InstaResource(new InstaService(mongo(), db));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static MongoDatabase mongo() throws Exception {
        System.setProperty("java.net.preferIPv4Stack", "true");
        MongoClientURI uri = new MongoClientURI("mongodb+srv://alexandrefett:fvrAqxTY4IIiPtSO@everest-2ne3f.mongodb.net");
        //MongoClientURI uri = new MongoClientURI("mongodb://localhost:27017");
        MongoClient mongoClient = new MongoClient(uri);
        MongoDatabase database = mongoClient.getDatabase("instamanager");
        return database;
    }
}

