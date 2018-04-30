package com.fett.interceptor;

import java.io.IOException;

import com.fett.model.MessageResult;
import com.fett.model.Search;
import com.google.gson.Gson;
import me.postaddict.instagram.scraper.exception.InstagramAuthException;
import me.postaddict.instagram.scraper.exception.InstagramException;
import me.postaddict.instagram.scraper.exception.InstagramNotFoundException;
import okhttp3.Interceptor;
import okhttp3.Response;
import okhttp3.Interceptor.Chain;

public class ErrorInterceptor implements Interceptor {
    public ErrorInterceptor() {
    }

    public Response intercept(Chain chain) throws IOException {
        Response response = chain.proceed(chain.request());
        int code = response.code();
        if (code == 200) {
            return response;
        } else {
            response.body().close();
            switch(code) {
                case 401:
                    throw new InstagramAuthException("Unauthorized");
                case 402:
                default:
                    Gson g = new Gson();
                    MessageResult s = g.fromJson(response.body().string(), MessageResult.class);
                    throw new InstagramException(s.getMessage());
                case 403:
                    throw new InstagramAuthException("Access denied");
                case 404:
                    throw new InstagramNotFoundException("Resource does not exist");
            }
        }
    }
}
