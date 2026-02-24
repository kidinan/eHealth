package com.clinic.repository;

import java.util.ArrayList;
import java.util.List;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Tuple;

public class AppointmentRepository {

    private final PgPool client;

    public AppointmentRepository(PgPool client) {
        this.client = client;
    }

    public Future<List<JsonObject>> findAll() {
        // We join with patients to get the patient name for the dashboard
        String sql = "SELECT a.id, a.appointment_date::text, a.department, a.doctor_name, " +
                     "a.reason, a.status, p.name as patient_name " +
                     "FROM appointments a JOIN patients p ON a.patient_id = p.id " +
                     "ORDER BY a.appointment_date ASC";
        
        return client.query(sql).execute().map(rows -> {
            List<JsonObject> list = new ArrayList<>();
            rows.forEach(row -> list.add(row.toJson()));
            return list;
        });
    }

    public Future<Void> save(JsonObject appt) {
        String sql = "INSERT INTO appointments (patient_id, appointment_date, department, doctor_name, reason) " +
                     "VALUES ($1, $2, $3, $4, $5)";
        
        return client.preparedQuery(sql).execute(Tuple.of(
            appt.getInteger("patient_id"),
            appt.getString("appointment_date"),
            appt.getString("department"),
            appt.getString("doctor_name"),
            appt.getString("reason")
        )).mapEmpty();
    }
}