package com.insta;

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

        get(API_CONTEXT + "/users/id/:id", "application/json", (request, response)
                -> instaService.find(request.params(":id")), new JsonTransformer());

        get(API_CONTEXT + "/reset", "application/json", (request, response)
                -> instaService.reset(), new JsonTransformer());

    }

}
