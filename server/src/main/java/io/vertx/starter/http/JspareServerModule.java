package io.vertx.starter.http;

import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.starter.reactivex.database.WikiDatabaseService;
import org.jspare.vertx.AbstractModule;

import javax.inject.Inject;

public class JspareServerModule extends AbstractModule {
  public static final String CONFIG_WIKIDB_QUEUE = "wikidb.queue";

  @Inject
  private Vertx vertx;

  @Override
  public void load() {
    bind(WebClient.class).registry(
      WebClient.create(vertx, new WebClientOptions()
        .setSsl(true)
        .setUserAgent("vert-x3")
      ));

    String dbQueue = getConfig().getString(CONFIG_WIKIDB_QUEUE, "wikidb.queue");
    bind(WikiDatabaseService.class).registry(
      io.vertx.starter.database.WikiDatabaseService.createProxy(getVertx(), dbQueue)
    );
  }

}
