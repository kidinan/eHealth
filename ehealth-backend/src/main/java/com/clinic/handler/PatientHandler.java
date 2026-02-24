package com.clinic.handler;

import com.clinic.repository.PatientRepository;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class PatientHandler {

    private final PatientRepository repository;

    public PatientHandler(PatientRepository repository) {
        this.repository = repository;
    }

    /**
     * GET /patients?page=1&limit=10&query=searchterm
     */
    public void getAllPatients(RoutingContext ctx) {
        try {
            int page = Integer.parseInt(ctx.request().getParam("page") != null ? ctx.request().getParam("page") : "1");
            int limit = Integer.parseInt(ctx.request().getParam("limit") != null ? ctx.request().getParam("limit") : "10");
            String query = ctx.request().getParam("query") != null ? ctx.request().getParam("query") : "";

            repository.findAll(page, limit, query)
                .onSuccess(result -> ctx.response()
                    .putHeader("content-type", "application/json")
                    .end(result.encode()))
                .onFailure(err -> ctx.fail(500, err));
        } catch (NumberFormatException e) {
            ctx.fail(400, new Throwable("Invalid pagination parameters"));
        }
    }

    /**
     * POST /patients
     */
    public void createPatient(RoutingContext ctx) {
        repository.save(ctx.getBodyAsJson())
            .onSuccess(id -> ctx.response()
                .setStatusCode(201)
                .putHeader("content-type", "application/json")
                .end(new JsonObject().put("id", id).encode()))
            .onFailure(err -> ctx.fail(400, err));
    }

    /**
     * PUT /patients/:id
     */
    public void updatePatient(RoutingContext ctx) {
        String id = ctx.pathParam("id");
        JsonObject patientData = ctx.getBodyAsJson();

        repository.update(Integer.parseInt(id), patientData)
            .onSuccess(v -> ctx.response()
                .setStatusCode(200)
                .putHeader("content-type", "application/json")
                .end(new JsonObject().put("status", "updated").encode()))
            .onFailure(err -> ctx.fail(500, err));
    }

    /**
     * DELETE /patients/:id
     */
public void deletePatient(RoutingContext ctx) {
    String id = ctx.pathParam("id");
    repository.delete(Integer.parseInt(id))
        .onSuccess(v -> {
            // Notify all connected clients that a patient was deleted
            ctx.vertx().eventBus().publish("patients.updates", 
                new JsonObject().put("action", "deleted").put("id", id));
            
            ctx.response().setStatusCode(204).end();
        });
}
}