package com.clinic.repository;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PatientRepository {

    // Temporary in-memory list (Replace with your actual Database logic/SQL)
    private static final List<JsonObject> patients = new ArrayList<>();
    private static int nextId = 1;

    public Future<JsonObject> findAll(int page, int limit, String query) {
        Promise<JsonObject> promise = Promise.promise();
        
        // 1. Filter based on search query
        List<JsonObject> filtered = patients.stream()
            .filter(p -> p.getString("name").toLowerCase().contains(query.toLowerCase()) || 
                         p.getString("email").toLowerCase().contains(query.toLowerCase()))
            .collect(Collectors.toList());

        // 2. Apply Pagination logic
        int total = filtered.size();
        int offset = (page - 1) * limit;
        List<JsonObject> paged = filtered.stream()
            .skip(offset)
            .limit(limit)
            .collect(Collectors.toList());

        // 3. Return the exact structure the Angular Frontend expects
        JsonObject result = new JsonObject()
            .put("data", new JsonArray(paged))
            .put("total", total);

        promise.complete(result);
        return promise.future();
    }

    public Future<Integer> save(JsonObject patient) {
        Promise<Integer> promise = Promise.promise();
        patient.put("id", nextId++);
        patients.add(patient);
        promise.complete(patient.getInteger("id"));
        return promise.future();
    }

    public Future<Void> update(int id, JsonObject newData) {
        Promise<Void> promise = Promise.promise();
        for (int i = 0; i < patients.size(); i++) {
            if (patients.get(i).getInteger("id") == id) {
                newData.put("id", id); // Keep the same ID
                patients.set(i, newData);
                promise.complete();
                return promise.future();
            }
        }
        promise.fail("Patient not found");
        return promise.future();
    }

    public Future<Void> delete(int id) {
        Promise<Void> promise = Promise.promise();
        boolean removed = patients.removeIf(p -> p.getInteger("id") == id);
        if (removed) promise.complete();
        else promise.fail("Patient not found");
        return promise.future();
    }
}