package io.vertx.starter.http.route;

import com.github.rjeschke.txtmark.Processor;
import com.google.common.net.HttpHeaders;
import io.reactivex.Observable;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.starter.reactivex.database.WikiDatabaseService;
import org.jspare.vertx.reactivex.web.handler.APIHandler;
import org.jspare.vertx.web.annotation.handler.Handler;
import org.jspare.vertx.web.annotation.handling.Parameter;
import org.jspare.vertx.web.annotation.method.Delete;
import org.jspare.vertx.web.annotation.method.Get;
import org.jspare.vertx.web.annotation.method.Post;
import org.jspare.vertx.web.annotation.method.Put;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Arrays;

public class ApiRoute extends APIHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(ApiRoute.class);

  @Inject
  private WikiDatabaseService dbService;

  private void apiResponse(RoutingContext context, int statusCode, String jsonField, Object jsonData) {
    context.response().setStatusCode(statusCode);
    context.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
    JsonObject wrapped = new JsonObject().put("success", true);
    if (jsonField != null && jsonData != null) wrapped.put(jsonField, jsonData);
    context.response().end(wrapped.encode());
  }

  private void apiFailure(RoutingContext context, Throwable t) {
    t.printStackTrace();
    apiFailure(context, 500, t.getMessage());
  }

  private void apiFailure(RoutingContext context, int statusCode, String error) {
    context.response().setStatusCode(statusCode);
    context.response().putHeader("Content-Type", "application/json");
    context.response().end(new JsonObject()
      .put("success", false)
      .put("error", error).encode());
  }

  //@Auth
  @Get("/pages")
  @Handler
  public void apiRoot(RoutingContext context) {
    dbService.rxFetchAllPagesData()
      .flatMapObservable(Observable::fromIterable)
      .map(obj -> new JsonObject()
        .put("id", obj.getInteger("ID"))
        .put("name", obj.getString("NAME")))
      .collect(JsonArray::new, JsonArray::add)
      .subscribe(pages -> apiResponse(context, 200, "pages", pages), t -> apiFailure(context, t));
  }

  //@Auth
  @Get("/pages/:id")
  @Handler
  public void apiGetPage(RoutingContext context, @Parameter("id") Integer id) {
    dbService.rxFetchPageById(id)
      .subscribe(dbObject -> {
        if (dbObject.getBoolean("found")) {
          JsonObject payload = new JsonObject()
            .put("name", dbObject.getString("name"))
            .put("id", dbObject.getInteger("id"))
            .put("markdown", dbObject.getString("content"))
            .put("html", Processor.process(dbObject.getString("content")));
          apiResponse(context, 200, "page", payload);
        } else {
          apiFailure(context, 404, "There is no page with ID " + id);
        }
      }, t -> apiFailure(context, t));
  }

  private boolean validateJsonPageDocument(RoutingContext context, JsonObject page, String... expectedKeys) {
    if (!Arrays.stream(expectedKeys).allMatch(page::containsKey)) {
      LOGGER.error("Bad page creation JSON payload: " + page.encodePrettily() + " from " + context.request().remoteAddress());
      context.response().setStatusCode(400);
      context.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
      context.response().end(new JsonObject()
        .put("success", false)
        .put("error", "Bad request payload").encode());
      return false;
    }
    return true;
  }

  //@Auth("create")
  @Post("/pages")
  @Handler
  public void apiCreatePage(RoutingContext context, JsonObject page) {
    if (!validateJsonPageDocument(context, page, "name", "markdown")) {
      return;
    }

    dbService.rxCreatePage(page.getString("name"), page.getString("markdown")).toObservable()
      .subscribe(v -> apiResponse(context, 201, null, null), t -> apiFailure(context, t));
  }

  //@Auth("update")
  @Put("/pages/:id")
  @Handler
  public void apiUpdatePage(RoutingContext context, @Parameter("id") Integer id, JsonObject page) {
    if (!validateJsonPageDocument(context, page, "markdown")) {
      return;
    }

    dbService.rxSavePage(id, page.getString("markdown")).toSingleDefault(true)
      .doOnSuccess(v -> {
        JsonObject event = new JsonObject()
          .put("id", id)
          .put("client", page.getString("client"));
        vertx.eventBus().publish("page.saved", event);
      })
      .subscribe(v -> apiResponse(context, 200, null, null), t -> apiFailure(context, t));
  }

  //@Auth("delete")
  @Delete("/pages/:id")
  @Handler
  public void apiDeletePage(RoutingContext context, @Parameter("id") Integer id) {
    dbService.rxDeletePage(id).toObservable()
      .subscribe(v -> apiResponse(context, 200, null, null), t -> apiFailure(context, t));
  }

}
