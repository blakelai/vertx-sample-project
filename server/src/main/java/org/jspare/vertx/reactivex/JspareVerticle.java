package org.jspare.vertx.reactivex;

import io.vertx.core.Context;
import io.vertx.core.Vertx;

public class JspareVerticle extends org.jspare.vertx.JspareVerticle {

  // Shadows the AbstractVerticle#vertx field
  protected io.vertx.reactivex.core.Vertx vertx;

  @Override
  public void init(Vertx vertx, Context context) {
    this.vertx = new io.vertx.reactivex.core.Vertx(vertx);
    super.init(vertx, context);
  }

}
