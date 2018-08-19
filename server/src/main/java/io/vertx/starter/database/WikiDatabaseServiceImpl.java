package io.vertx.starter.database;

import io.github.jklingsporn.vertx.jooq.rx.jdbc.JDBCRXGenericQueryExecutor;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.CompletableHelper;
import io.vertx.reactivex.SingleHelper;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.reactivex.ext.sql.SQLConnection;
import io.vertx.starter.database.tables.daos.PagesDao;
import io.vertx.starter.database.tables.interfaces.IPages;
import io.vertx.starter.database.tables.pojos.Pages;
import org.jooq.Record1;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static io.vertx.starter.database.tables.Pages.PAGES;

class WikiDatabaseServiceImpl implements WikiDatabaseService {

  private static final Logger logger = LoggerFactory.getLogger(WikiDatabaseServiceImpl.class);
  private final HashMap<SqlQuery, String> sqlQueries;
  private final JDBCClient dbClient;
  private final JDBCRXGenericQueryExecutor queryExecutor;
  private final PagesDao pagesDao;


  WikiDatabaseServiceImpl(JDBCRXGenericQueryExecutor queryExecutor, PagesDao pagesDao, JDBCClient dbClient, HashMap<SqlQuery, String> sqlQueries, Handler<AsyncResult<WikiDatabaseService>> readyHandler) {
    this.dbClient = dbClient;
    this.sqlQueries = sqlQueries;
    this.queryExecutor = queryExecutor;
    this.pagesDao = pagesDao;

    queryExecutor.execute(dslContext ->
      dslContext
        .createTableIfNotExists(PAGES)
        .column(PAGES.ID)
        .column(PAGES.NAME)
        .column(PAGES.CONTENT)
        .constraints(
          DSL.constraint().primaryKey(PAGES.ID),
          DSL.constraint().unique(PAGES.NAME)
        )
        .execute()
    ).map(v -> this)
    .subscribe(SingleHelper.toObserver(readyHandler));
  }

  private Single<SQLConnection> getConnection() {
    return dbClient.rxGetConnection().flatMap(conn -> {
      Single<SQLConnection> connectionSingle = Single.just(conn);
      return connectionSingle.doOnDispose(conn::close);
    });
  }

  @Override
  public WikiDatabaseService fetchAllPages(Handler<AsyncResult<JsonArray>> resultHandler) {
      queryExecutor.execute(dslContext ->
        dslContext.select(PAGES.NAME)
          .from(PAGES)
          .fetch()
      )
        .flatMapObservable(Observable::fromIterable)
        .map(Record1::value1)
        .sorted()
        .collect(JsonArray::new, JsonArray::add)
        .subscribe(SingleHelper.toObserver(resultHandler));

    return this;
  }

  @Override
  public WikiDatabaseService fetchPage(String name, Handler<AsyncResult<JsonObject>> resultHandler) {
    pagesDao.findOneByName(name)
      .map(record -> {
        if (record.isPresent()) {
          Pages pages = record.get();
          return new JsonObject()
            .put("found", true)
            .put("id", pages.getId())
            .put("rawContent", pages.getContent());
          } else {
            return new JsonObject().put("found", false);
          }
      })
      .subscribe(SingleHelper.toObserver(resultHandler));

    return this;
  }

  @Override
  public WikiDatabaseService fetchPageById(int id, Handler<AsyncResult<JsonObject>> resultHandler) {
    pagesDao.findOneById(id)
      .map(record -> {
        if (record.isPresent()) {
          Pages pages = record.get();
          return new JsonObject()
            .put("found", true)
            .put("id", pages.getId())
            .put("name", pages.getName())
            .put("rawContent", pages.getContent());
          } else {
            return new JsonObject().put("found", false);
          }
      })
      .subscribe(SingleHelper.toObserver(resultHandler));

    return this;
  }

  @Override
  public WikiDatabaseService createPage(String title, String markdown, Handler<AsyncResult<Void>> resultHandler) {
    Pages pages = new Pages()
      .setName(title)
      .setContent(markdown);
    pagesDao.insert(pages).toCompletable()
      .subscribe(CompletableHelper.toObserver(resultHandler));

    return this;
  }

  @Override
  public WikiDatabaseService savePage(int id, String markdown, Handler<AsyncResult<Void>> resultHandler) {
    queryExecutor.execute(dslContext ->
      dslContext.update(PAGES)
        .set(PAGES.CONTENT, markdown)
        .where(PAGES.ID.eq(id))
        .execute()
    ).toCompletable()
      .subscribe(CompletableHelper.toObserver(resultHandler));

    return this;
  }

  @Override
  public WikiDatabaseService deletePage(int id, Handler<AsyncResult<Void>> resultHandler) {
    pagesDao.deleteById(id).toCompletable()
      .subscribe(CompletableHelper.toObserver(resultHandler));

    return this;
  }

  @Override
  public WikiDatabaseService fetchAllPagesData(Handler<AsyncResult<List<JsonObject>>> resultHandler) {
    pagesDao.findAll().map(result ->
      result.stream()
        .map(IPages::toJson)
        .collect(Collectors.toList())
    ).subscribe(SingleHelper.toObserver(resultHandler));

    return this;
  }

}
