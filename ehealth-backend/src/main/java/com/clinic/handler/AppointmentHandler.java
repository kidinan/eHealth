package com.clinic.handler;

import com.clinic.repository.AppointmentRepository;

import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;

public class AppointmentHandler {

    private final AppointmentRepository repository;

    public AppointmentHandler(AppointmentRepository repository) {
        this.repository = repository;
    }

    public void getAll(RoutingContext ctx) {
        repository.findAll()
            .onSuccess(data -> ctx.response()
                .putHeader("content-type", "application/json")
                .end(new JsonArray(data).encode()))
            .onFailure(err -> ctx.fail(500, err));
    }

    public void create(RoutingContext ctx) {
        repository.save(ctx.getBodyAsJson())
            .onSuccess(v -> ctx.response().setStatusCode(201).end())
            .onFailure(err -> ctx.fail(400, err));
    }
}