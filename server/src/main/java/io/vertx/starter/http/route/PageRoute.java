package io.vertx.starter.http.route;

import com.github.rjeschke.txtmark.Processor;
import com.google.common.net.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.ext.auth.User;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.codec.BodyCodec;
import io.vertx.reactivex.ext.web.templ.FreeMarkerTemplateEngine;
import io.vertx.starter.reactivex.database.WikiDatabaseService;
import org.jspare.vertx.reactivex.web.handler.APIHandler;
import org.jspare.vertx.web.annotation.auth.Auth;
import org.jspare.vertx.web.annotation.handler.Handler;
import org.jspare.vertx.web.annotation.handling.Parameter;
import org.jspare.vertx.web.annotation.method.Get;
import org.jspare.vertx.web.annotation.method.Post;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Date;

public class PageRoute extends APIHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(PageRoute.class);

  private static final String EMPTY_PAGE_MARKDOWN =
    "# A new page\n" +
      "\n" +
      "Feel-free to write in Markdown!\n";

  private final FreeMarkerTemplateEngine templateEngine = FreeMarkerTemplateEngine.create();


  @Inject
  private WikiDatabaseService dbService;

  @Inject
  private WebClient webClient;

  private static final class UnauthorizedThrowable extends Throwable {
    UnauthorizedThrowable(String message) {
      super(message, null, false, false);
    }
  }

  private void onError(RoutingContext context, Throwable t) {
    if (t instanceof UnauthorizedThrowable) {
      context.fail(403);
    } else {
      context.fail(t);
    }
  }

  private Completable checkAuthorised(RoutingContext context, String authority) {
    return context.user().rxIsAuthorised(authority)
      .flatMapCompletable(authorized ->
              authorized ? Completable.complete() : Completable.error(new UnauthorizedThrowable(authority)));
  }

  @Get("/login")
  @Handler
  public void loginHandler(RoutingContext context) {
    context.put("title", "Login");
    templateEngine.rxRender(context, "templates", "/login.ftl")
      .subscribe(markup -> {
        context.response()
          .putHeader(HttpHeaders.CONTENT_TYPE, "text/html")
          .end(markup);
      }, context::fail);
  }

  @Auth
  @Get("/")
  @Handler
  public void indexHandler(RoutingContext context) {
    context.user().rxIsAuthorised("create")
      .flatMap(canCreatePage -> {
        context.put("canCreatePage", canCreatePage);
        return dbService.rxFetchAllPages();
      })
      .flatMap(result -> {
        context.put("title", "Wiki home");
        context.put("pages", result.getList());
        context.put("username", context.user().principal().getString("username"));
        return templateEngine.rxRender(context, "templates", "/index.ftl");
      })
      .subscribe(markup -> {
        context.response()
          .putHeader(HttpHeaders.CONTENT_TYPE, "text/html")
          .end(markup);
      }, context::fail);
  }

  @Auth
  @Get("/wiki/:page")
  @Handler
  public void pageRenderingHandler(@Parameter("page") String requestedPage, RoutingContext context) {
    User user = context.user();

    context.user().rxIsAuthorised("create")
      .flatMap(canSavePage -> {
        context.put("canSavePage", canSavePage);
        return user.rxIsAuthorised("delete");
      }).flatMap(canDeletePage -> {
      context.put("canDeletePage", canDeletePage);
      context.put("title", requestedPage);
      return dbService.rxFetchPage(requestedPage);
    }).flatMap(payLoad -> {
      boolean found = payLoad.getBoolean("found");
      String rawContent = payLoad.getString("rawContent", EMPTY_PAGE_MARKDOWN);
      context.put("id", payLoad.getInteger("id", -1));
      context.put("newPage", found ? "no" : "yes");
      context.put("rawContent", rawContent);
      context.put("content", Processor.process(rawContent));
      context.put("timestamp", new Date().toString());
      context.put("username", user.principal().getString("username"));
      return templateEngine.rxRender(context, "templates", "/page.ftl");
    }).subscribe(markup -> {
        context.response()
          .putHeader(HttpHeaders.CONTENT_TYPE, "text/html")
          .end(markup);
      },
      context::fail);
  }

  @Auth
  @Post("/action/save")
  @Handler
  public void pageUpdateHandler(RoutingContext context) {
    String title = context.request().getParam("title");
    boolean pageCreation = "yes".equals(context.request().getParam("newPage"));
    String markdown = context.request().getParam("markdown");
    checkAuthorised(context, pageCreation ? "create" : "update").toSingleDefault(true)
      .flatMap(v -> {
        if (pageCreation) {
          return dbService.rxCreatePage(title, markdown)
                  .toSingleDefault(true);
        } else {
          return dbService.rxSavePage(Integer.valueOf(context.request().getParam("id")), markdown)
                  .toSingleDefault(true);
        }
      })
      .subscribe(v -> {
        context.response()
          .setStatusCode(HttpResponseStatus.SEE_OTHER.code())
          .putHeader(HttpHeaders.LOCATION, "/wiki/" + title)
          .end();
      }, t -> onError(context, t));
  }

  @Auth
  @Post("/action/create")
  @Handler
  public void pageCreateHandler(RoutingContext context) {
    String pageName = context.request().getParam("name");
    String location = "/wiki/" + pageName;
    if (pageName == null || pageName.isEmpty()) {
      location = "/";
    }
    context.response()
      .setStatusCode(HttpResponseStatus.SEE_OTHER.code())
      .putHeader(HttpHeaders.LOCATION, location)
      .end();
  }

  @Auth("delete")
  @Post("/action/delete")
  @Handler
  public void pageDeletionHandler(RoutingContext context) {
    dbService.rxDeletePage(Integer.parseInt(context.request().getParam("id")))
      .toObservable().subscribe(v -> {
        context.response()
          .setStatusCode(HttpResponseStatus.SEE_OTHER.code())
          .putHeader(HttpHeaders.LOCATION, "/")
          .end();
      }, t -> onError(context, t));
  }

  @Auth("role:writer")
  @Post("/action/backup")
  @Handler
  public void backupHandler(RoutingContext context) {
    dbService.rxFetchAllPagesData()
      .map(pages -> {
        JsonObject filesObject = new JsonObject();
        pages.forEach(page -> {
          JsonObject fileObject = new JsonObject();
          filesObject.put(page.getString("NAME"), fileObject);
          fileObject.put("content", page.getString("CONTENT"));
        });
        return new JsonObject()
          .put("files", filesObject)
          .put("description", "A wiki backup")
          .put("public", true);
      })
      .flatMap(body -> webClient
        .post(443, "api.github.com", "/gists")
        .putHeader(HttpHeaders.ACCEPT, "application/vnd.github.v3+json")
        .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        .as(BodyCodec.jsonObject()).rxSendJsonObject(body))
      .subscribe(response -> {
        if (response.statusCode() == HttpResponseStatus.CREATED.code()) {
          context.put("backup_gist_url", response.body().getString("html_url"));
          indexHandler(context);
        } else {
          StringBuilder message = new StringBuilder()
            .append("Could not backup the wiki: ")
            .append(response.statusMessage());
          JsonObject body = response.body();
          if (body != null) {
            message.append(System.getProperty("line.separator"))
              .append(body.encodePrettily());
          }
          LOGGER.error(message.toString());
          context.fail(HttpResponseStatus.BAD_GATEWAY.code());
        }
      }, t -> onError(context, t));
  }

}
