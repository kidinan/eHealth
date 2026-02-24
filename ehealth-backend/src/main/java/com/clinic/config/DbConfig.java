package com.clinic.config;
import io.vertx.core.Vertx;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;

public class DbConfig {

  public static PgPool getPool(Vertx vertx) {
    PgConnectOptions connectOptions = new PgConnectOptions()
      .setPort(5432)
      .setHost("localhost")
      .setDatabase("ehealth_db") // The DB you created
      .setUser("postgres")       // Your Postgres username
      .setPassword("password"); // Your Postgres password

    PoolOptions poolOptions = new PoolOptions()
      .setMaxSize(5); // A senior dev keeps the pool small for MVPs

    return PgPool.pool(vertx, connectOptions, poolOptions);
  }
}