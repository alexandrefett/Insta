package com.insta;

import com.google.gson.Gson;

import static spark.Spark.get;
import static spark.Spark.put;

;

public class InstaResource {
    private static final String API_CONTEXT = "/api/v1";

    private final InstaService instaService;

    public InstaResource(InstaService instaService) {
        this.instaService = instaService;
        setupEndpoints();
    }

    private void setupEndpoints() {
        get("/hello", (req, res) -> "Hello World");

        get(API_CONTEXT + "/login", (req, res) -> {
            return instaService.login(req);
        }, new JsonTransformer());

        get(API_CONTEXT + "/followers", (req, res) -> {
            return instaService.login(req);
        }, new JsonTransformer());

        get(API_CONTEXT + "/requested", (req, res) -> {
            return instaService.requested(req);
        }, new JsonTransformer());

        get(API_CONTEXT + "/users/username/:username", "application/json", (request, response)
                -> instaService.find(request.params(":username")), new JsonTransformer());

        get(API_CONTEXT + "/status", "application/json", (request, response)
                -> instaService.status(), new JsonTransformer());

        get(API_CONTEXT + "/follow/id/:id", "application/json", (request, response)
                -> instaService.doFollow(request.params(":id")), new JsonTransformer());

        get(API_CONTEXT + "/unfollow", "application/json", (request, response)
                -> new Gson().toJson(instaService.doUnFollow()), new JsonTransformer());

        get(API_CONTEXT + "/search/id/:username", "application/json", (request, response)
                -> instaService.getSearchUser(request.params(":username")), new JsonTransformer());



    }

}
