package com.clinic;

import java.time.LocalDate;

import com.clinic.config.DbConfig; 
import com.clinic.handler.ChatHandler;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

public class MainVerticle extends AbstractVerticle {
    private PgPool pgClient;

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new MainVerticle());
    }

    @Override
    public void start(Promise<Void> startPromise) {
        pgClient = DbConfig.getPool(vertx);
        
        ChatHandler chatHandler = new ChatHandler("API_Key");

        Router router = Router.router(vertx);

        // UPDATED: Added PUT and DELETE to allowed methods
        router.route().handler(CorsHandler.create("http://localhost:4200")
            .allowedMethods(java.util.Set.of(
                io.vertx.core.http.HttpMethod.GET, 
                io.vertx.core.http.HttpMethod.POST,
                io.vertx.core.http.HttpMethod.PUT,
                io.vertx.core.http.HttpMethod.DELETE,
                io.vertx.core.http.HttpMethod.OPTIONS
            ))
            .allowedHeaders(java.util.Set.of("Content-Type", "Authorization")));

        router.route().handler(BodyHandler.create());

        // --- API Routes ---
        router.get("/api/v1/patients").handler(this::handleGetPatients);
        router.get("/api/v1/patients/:id").handler(this::handleGetPatientById);
        router.post("/api/v1/patients").handler(this::handleRegisterPatient);
        
        // NEW: Update and Delete Routes
        router.put("/api/v1/patients/:id").handler(this::handleUpdatePatient);
        router.delete("/api/v1/patients/:id").handler(this::handleDeletePatient);

        router.post("/api/v1/chat").handler(chatHandler::handleChat);

        // Start the server
        vertx.createHttpServer().requestHandler(router).listen(8080, h -> {
            if (h.succeeded()) {
                System.out.println("✅ eHealth Backend Server up on 8080");
                System.out.println("🤖 Gemini Chatbot endpoint ready at /api/v1/chat");
            } else {
                System.err.println("❌ Failed to start server: " + h.cause().getMessage());
                startPromise.fail(h.cause());
            }
        });
    }

    // --- Handler Methods ---

    private void handleGetPatients(RoutingContext ctx) {
        String searchParam = ctx.request().getParam("search");
        String search = (searchParam != null) ? "%" + searchParam + "%" : "%%";
        int limit = Integer.parseInt(ctx.request().getParam("limit") != null ? ctx.request().getParam("limit") : "10");
        int page = Integer.parseInt(ctx.request().getParam("page") != null ? ctx.request().getParam("page") : "1");
        int offset = (page - 1) * limit;

        // Existing query already has "ORDER BY id DESC" which puts latest registered on top!
        String sql = "SELECT *, count(*) OVER() AS full_count FROM patients WHERE name ILIKE $1 OR email ILIKE $1 ORDER BY id DESC LIMIT $2 OFFSET $3";
        pgClient.preparedQuery(sql).execute(Tuple.of(search, limit, offset)).onSuccess(rows -> {
            JsonArray data = new JsonArray();
            long total = 0;
            for (Row row : rows) {
                total = row.getLong("full_count");
                data.add(new JsonObject()
                    .put("id", row.getInteger("id"))
                    .put("name", row.getString("name"))
                    .put("dob", row.getLocalDate("dob").toString())
                    .put("email", row.getString("email"))
                    .put("department", row.getString("primary_department")));
            }
            ctx.response().putHeader("content-type", "application/json").end(new JsonObject().put("data", data).put("total", total).encode());
        }).onFailure(err -> ctx.fail(500));
    }

    private void handleGetPatientById(RoutingContext ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            pgClient.preparedQuery("SELECT * FROM patients WHERE id = $1").execute(Tuple.of(id)).onSuccess(rows -> {
                if (rows.iterator().hasNext()) {
                    Row row = rows.iterator().next();
                    ctx.response().putHeader("content-type", "application/json").end(new JsonObject()
                        .put("id", row.getInteger("id"))
                        .put("name", row.getString("name"))
                        .put("email", row.getString("email"))
                        .put("dob", row.getLocalDate("dob").toString())
                        .put("department", row.getString("primary_department")).encode());
                } else {
                    ctx.response().setStatusCode(404).end(new JsonObject().put("error", "Patient not found").encode());
                }
            }).onFailure(err -> ctx.fail(500));
        } catch (NumberFormatException e) {
            ctx.response().setStatusCode(400).end(new JsonObject().put("error", "Invalid ID format").encode());
        }
    }

    private void handleRegisterPatient(RoutingContext ctx) {
        JsonObject b = ctx.body().asJsonObject();
        if (b == null) {
            ctx.fail(400);
            return;
        }

        pgClient.preparedQuery("INSERT INTO patients (name, dob, email, primary_department) VALUES ($1, $2, $3, $4)")
            .execute(Tuple.of(
                b.getString("name"), 
                LocalDate.parse(b.getString("dob")), 
                b.getString("email"), 
                b.getString("department")
            ))
            .onSuccess(r -> ctx.response().setStatusCode(201).end())
            .onFailure(e -> {
                System.err.println("Registration error: " + e.getMessage());
                ctx.fail(500);
            });
    }

    // NEW: Handle Update
    private void handleUpdatePatient(RoutingContext ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            JsonObject b = ctx.body().asJsonObject();
            if (b == null) { ctx.fail(400); return; }

            String sql = "UPDATE patients SET name = $1, dob = $2, email = $3, primary_department = $4 WHERE id = $5";
            pgClient.preparedQuery(sql)
                .execute(Tuple.of(
                    b.getString("name"), 
                    LocalDate.parse(b.getString("dob")), 
                    b.getString("email"), 
                    b.getString("department"), 
                    id
                ))
                .onSuccess(r -> ctx.response().setStatusCode(200).end())
                .onFailure(e -> ctx.fail(500));
        } catch (Exception e) {
            ctx.fail(400);
        }
    }

    // NEW: Handle Delete
    private void handleDeletePatient(RoutingContext ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            pgClient.preparedQuery("DELETE FROM patients WHERE id = $1")
                .execute(Tuple.of(id))
                .onSuccess(r -> ctx.response().setStatusCode(204).end())
                .onFailure(e -> ctx.fail(500));
        } catch (Exception e) {
            ctx.fail(400);
        }
    }
}
