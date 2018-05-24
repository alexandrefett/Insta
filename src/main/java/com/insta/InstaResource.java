package com.insta;

import static spark.Spark.get;
import static spark.Spark.post;
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
        get("/hello", (req, res) -> {
            services.base();
            return "xxxx";
            }, new JsonTransformer());


        get(API_CONTEXT + "/:token/login", (req, res) -> {
            return services.login(req);
        }, new JsonTransformer());

        get(API_CONTEXT + "/:token/followers", (req, res) -> {
            return services.followers(req);
        }, new JsonTransformer());

        get(API_CONTEXT + "/:token/follows", (req, res) -> {
            return services.follows(req);
        }, new JsonTransformer());

        get(API_CONTEXT + "/:token/requested", (req, res) -> {
            return services.requested(req);
        }, new JsonTransformer());

        post(API_CONTEXT + "/:token/whitelist", (req, res) -> {
            return services.addwhitelist(req);
        }, new JsonTransformer());

        get(API_CONTEXT + "/:token/whitelist", (req, res) -> {
            return services.getwhitelist(req);
        }, new JsonTransformer());

        get(API_CONTEXT + "/:token/user/:username", "application/json", (req, res)
                -> services.find(req), new JsonTransformer());

        get(API_CONTEXT + "/:token/follow/:username", "application/json", (req, res)
                -> services.follow(req), new JsonTransformer());

        get(API_CONTEXT + "/:token/unfollow", "application/json", (req, res)
                -> services.unfollow(req), new JsonTransformer());

        get(API_CONTEXT + "/:token/search/:username", "application/json", (req, res)
                -> services.search(req), new JsonTransformer());

        get(API_CONTEXT + "/register", "application/json", (req, res)
                -> services.getinstagram(req), new JsonTransformer());

        post(API_CONTEXT + "/:token/register", "application/json", (req, res)
                -> services.register(req), new JsonTransformer());

    }

}
