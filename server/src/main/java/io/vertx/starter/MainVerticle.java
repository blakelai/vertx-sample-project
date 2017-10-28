package io.vertx.starter;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.starter.database.WikiDatabaseBinder;
import io.vertx.starter.database.WikiDatabaseVerticle;
import io.vertx.starter.http.JspareServerVerticle;
import org.jspare.vertx.rxjava.cdi.EnvironmentLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Single;

public class MainVerticle extends AbstractVerticle {

  private static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);

  @Override
  public void start(Future<Void> startFuture) {
    logger.debug("Start vertx.");

    deployDatabaseVerticle()
      .flatMap(id -> deployJerseyServer())
      .subscribe(id -> startFuture.complete(), startFuture::fail);

    //Guice.createInjector(new VertxDataFeature(vertx)).injectMembers(this);
    //Future<Void> steps = prepareDatabase().compose(v -> startHttpServer());
    //steps.setHandler(startFuture.completer());
  }

  private Single<String> deployDatabaseVerticle() {
    String deploymentName = "java-guice:" + WikiDatabaseVerticle.class.getName();
    JsonObject config = JsonObject.mapFrom(config())
      .put("guice_binder", WikiDatabaseBinder.class.getName());

    DeploymentOptions opts = new DeploymentOptions();
    opts.setConfig(config);

    logger.debug("Deploy database verticle.");
    return vertx.rxDeployVerticle(deploymentName, opts);
  }

  private Single<String> deployJerseyServer() {
    EnvironmentLoader.setup();
    EnvironmentLoader.bindInterfaces(vertx);
    String deploymentName = JspareServerVerticle.class.getName();

    DeploymentOptions opts = new DeploymentOptions();
    opts.setInstances(2);

    logger.debug("Deploy http verticle, config: " + opts.toJson().toString());
    return vertx.rxDeployVerticle(deploymentName, opts);
  }

}
