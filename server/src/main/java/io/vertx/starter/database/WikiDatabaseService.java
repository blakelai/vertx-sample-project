package io.vertx.starter.database;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.jdbc.JDBCClient;

import java.util.HashMap;
import java.util.List;

@ProxyGen
@VertxGen
public interface WikiDatabaseService {

  @GenIgnore
  static WikiDatabaseService create(JDBCClient dbClient, HashMap<SqlQuery, String> sqlQueries, Handler<AsyncResult<WikiDatabaseService>> readyHandler) {
    return new WikiDatabaseServiceImpl(dbClient, sqlQueries, readyHandler);
  }

  @GenIgnore
  static io.vertx.starter.rxjava.database.WikiDatabaseService createProxy(Vertx vertx, String address) {
    return new io.vertx.starter.rxjava.database.WikiDatabaseService(new WikiDatabaseServiceVertxEBProxy(vertx, address));
  }

  @Fluent
  WikiDatabaseService fetchAllPages(Handler<AsyncResult<JsonArray>> resultHandler);

  @Fluent
  WikiDatabaseService fetchPage(String name, Handler<AsyncResult<JsonObject>> resultHandler);

  @Fluent
  WikiDatabaseService fetchPageById(int id, Handler<AsyncResult<JsonObject>> resultHandler);

  @Fluent
  WikiDatabaseService createPage(String title, String markdown, Handler<AsyncResult<Void>> resultHandler);

  @Fluent
  WikiDatabaseService savePage(int id, String markdown, Handler<AsyncResult<Void>> resultHandler);

  @Fluent
  WikiDatabaseService deletePage(int id, Handler<AsyncResult<Void>> resultHandler);

  @Fluent
  WikiDatabaseService fetchAllPagesData(Handler<AsyncResult<List<JsonObject>>> resultHandler);

}
