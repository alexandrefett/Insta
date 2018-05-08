package com.insta;

import com.google.gson.Gson;

import static spark.Spark.get;
import static spark.Spark.put;

;

public class InstaResource {
    private static final String API_CONTEXT = "/api/v1";

    private final InstaManager services;

    public InstaResource(InstaManager services) {
        this.services = services;
        setupEndpoints();
    }

    private void setupEndpoints() {
        get("/hello", (req, res) -> "Hello World");

        get(API_CONTEXT + "/login", (req, res) -> {
            return services.login(req);
        }, new JsonTransformer());

        get(API_CONTEXT + "/users/:username/followers", (req, res) -> {
            return services.followers(req);
        }, new JsonTransformer());

        get(API_CONTEXT + "/users/:username/follows", (req, res) -> {
            return services.follows(req);
        }, new JsonTransformer());

        get(API_CONTEXT + "/requested", (req, res) -> {
            return services.requested(req);
        }, new JsonTransformer());

        get(API_CONTEXT + "/whitelist/:userlist/:username", (req, res) -> {
            return services.addwhitelist(req);
//            return services.addwhitelist(req.params(":userlist"),req.params(":username"));
        }, new JsonTransformer());

        get(API_CONTEXT + "/users/:username", "application/json", (request, response)
                -> services.find(req), new JsonTransformer());
//                -> services.find(request.params(":username")), new JsonTransformer());

        get(API_CONTEXT + "/users/:username/follow", "application/json", (request, response)
                -> services.follow(req), new JsonTransformer());
//                -> services.follow(request.params(":id")), new JsonTransformer());

        get(API_CONTEXT + "/unfollow", "application/json", (request, response)
                -> services.unfollow(req), new JsonTransformer());

        get(API_CONTEXT + "/search/:username", "application/json", (request, response)
                -> services.search(req), new JsonTransformer());
//                -> services.search(request.params(":username")), new JsonTransformer());



    }

}
