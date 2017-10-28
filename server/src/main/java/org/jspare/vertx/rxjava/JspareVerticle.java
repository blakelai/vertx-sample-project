package org.jspare.vertx.rxjava;

import io.vertx.core.Context;
import io.vertx.core.Vertx;

public class JspareVerticle extends org.jspare.vertx.JspareVerticle {

  // Shadows the AbstractVerticle#vertx field
  protected io.vertx.rxjava.core.Vertx vertx;

  @Override
  public void init(Vertx vertx, Context context) {
    this.vertx = new io.vertx.rxjava.core.Vertx(vertx);
    super.init(vertx, context);
  }

}
