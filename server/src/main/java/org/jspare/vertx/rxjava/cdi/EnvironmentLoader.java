package org.jspare.vertx.rxjava.cdi;

import io.vertx.rxjava.core.Context;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.eventbus.EventBus;
import io.vertx.rxjava.core.file.FileSystem;
import io.vertx.rxjava.core.shareddata.SharedData;
import lombok.experimental.UtilityClass;
import org.jspare.core.Environment;
import org.jspare.core.internal.Bind;

@UtilityClass
public class EnvironmentLoader {

  /**
   * Register.
   */
  public void setup() {
    org.jspare.vertx.cdi.EnvironmentLoader.setup();
  }

  public void bindInterfaces(Vertx vertx){
    Environment.registry(Bind.bind(Vertx.class), vertx);
    Environment.registry(Bind.bind(Context.class), vertx.getOrCreateContext());
    Environment.registry(Bind.bind(EventBus.class), vertx.eventBus());
    Environment.registry(Bind.bind(FileSystem.class), vertx.fileSystem());
    Environment.registry(Bind.bind(SharedData.class), vertx.sharedData());
    org.jspare.vertx.cdi.EnvironmentLoader.bindInterfaces(vertx.getDelegate());
  }
}
