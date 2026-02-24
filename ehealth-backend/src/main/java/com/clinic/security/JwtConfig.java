package com.clinic.security;

import io.vertx.core.Vertx;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;

public class JwtConfig {
    
    private static final String SECRET = "SUPER_SECRET_HEALTH_KEY_2026";

    public static JWTAuth getProvider(Vertx vertx) {
        return JWTAuth.create(vertx, new JWTAuthOptions()
            .addPubSecKey(new PubSecKeyOptions()
                .setAlgorithm("HS256")
                .setBuffer(SECRET)));
    }
}