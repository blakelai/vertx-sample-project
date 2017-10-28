package io.vertx.starter.database;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.rx.java.RxHelper;
import io.vertx.rxjava.ext.jdbc.JDBCClient;
import io.vertx.rxjava.ext.sql.SQLConnection;
import rx.Observable;
import rx.Single;

import java.util.HashMap;
import java.util.List;

class WikiDatabaseServiceImpl implements WikiDatabaseService {

  private final HashMap<SqlQuery, String> sqlQueries;
  private final JDBCClient dbClient;


  WikiDatabaseServiceImpl(JDBCClient dbClient, HashMap<SqlQuery, String> sqlQueries, Handler<AsyncResult<WikiDatabaseService>> readyHandler) {
    this.dbClient = dbClient;
    this.sqlQueries = sqlQueries;

    getConnection()
      .flatMap(conn -> conn.rxExecute(sqlQueries.get(SqlQuery.CREATE_PAGES_TABLE)))
      .map(v -> this)
      .subscribe(RxHelper.toSubscriber(readyHandler));
  }

  private Single<SQLConnection> getConnection() {
    return dbClient.rxGetConnection().flatMap(conn -> {
      Single<SQLConnection> connectionSingle = Single.just(conn);
      return connectionSingle.doOnUnsubscribe(conn::close);
    });
  }

  @Override
  public WikiDatabaseService fetchAllPages(Handler<AsyncResult<JsonArray>> resultHandler) {
    getConnection()
      .flatMap(conn -> conn.rxQuery(sqlQueries.get(SqlQuery.ALL_PAGES)))
      .flatMapObservable(res -> {
        List<JsonArray> results = res.getResults();
        return Observable.from(results);
      })
      .map(json -> json.getString(0))
      .sorted()
      .collect(JsonArray::new, JsonArray::add)
      .subscribe(RxHelper.toSubscriber(resultHandler));

    return this;
  }

  @Override
  public WikiDatabaseService fetchPage(String name, Handler<AsyncResult<JsonObject>> resultHandler) {
    getConnection()
      .flatMap(conn -> conn.rxQueryWithParams(sqlQueries.get(SqlQuery.GET_PAGE), new JsonArray().add(name)))
      .map(result -> {
        if (result.getNumRows() > 0) {
          JsonArray row = result.getResults().get(0);
          return new JsonObject()
            .put("found", true)
            .put("id", row.getInteger(0))
            .put("rawContent", row.getString(1));
        } else {
          return new JsonObject().put("found", false);
        }
      })
      .subscribe(RxHelper.toSubscriber(resultHandler));

    return this;
  }

  @Override
  public WikiDatabaseService fetchPageById(int id, Handler<AsyncResult<JsonObject>> resultHandler) {
    getConnection()
      .flatMap(conn -> conn.rxQueryWithParams(sqlQueries.get(SqlQuery.GET_PAGE_BY_ID), new JsonArray().add(id)))
      .map(result -> {
        if (result.getNumRows() > 0) {
          JsonObject row = result.getRows().get(0);
          return new JsonObject()
            .put("found", true)
            .put("id", row.getInteger("ID"))
            .put("name", row.getString("NAME"))
            .put("content", row.getString("CONTENT"));
        } else {
          return new JsonObject().put("found", false);
        }
      })
      .subscribe(RxHelper.toSubscriber(resultHandler));

    return this;
  }

  @Override
  public WikiDatabaseService createPage(String title, String markdown, Handler<AsyncResult<Void>> resultHandler) {
    getConnection()
      .flatMap(conn -> conn.rxQueryWithParams(
        sqlQueries.get(SqlQuery.CREATE_PAGE), new JsonArray().add(title).add(markdown)))
      .map(result -> (Void) null)
      .subscribe(RxHelper.toSubscriber(resultHandler));

    return this;
  }

  @Override
  public WikiDatabaseService savePage(int id, String markdown, Handler<AsyncResult<Void>> resultHandler) {
    getConnection()
      .flatMap(conn -> conn.rxQueryWithParams(
        sqlQueries.get(SqlQuery.SAVE_PAGE), new JsonArray().add(markdown).add(id)))
      .map(result -> (Void) null)
      .subscribe(RxHelper.toSubscriber(resultHandler));

    return this;
  }

  @Override
  public WikiDatabaseService deletePage(int id, Handler<AsyncResult<Void>> resultHandler) {
    getConnection()
      .flatMap(conn -> conn.rxQueryWithParams(
        sqlQueries.get(SqlQuery.DELETE_PAGE), new JsonArray().add(id)))
      .map(result -> (Void) null)
      .subscribe(RxHelper.toSubscriber(resultHandler));

    return this;
  }

  @Override
  public WikiDatabaseService fetchAllPagesData(Handler<AsyncResult<List<JsonObject>>> resultHandler) {
    getConnection()
      .flatMap(conn -> conn.rxQuery(sqlQueries.get(SqlQuery.ALL_PAGES_DATA)))
      .map(ResultSet::getRows)
      .subscribe(RxHelper.toSubscriber(resultHandler));

    return this;
  }

}
