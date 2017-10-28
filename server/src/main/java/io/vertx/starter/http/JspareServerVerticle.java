package io.vertx.starter.http;

import com.github.rjeschke.txtmark.Processor;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JdkSSLEngineOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.auth.jwt.JWTOptions;
import io.vertx.ext.auth.shiro.ShiroAuthOptions;
import io.vertx.ext.auth.shiro.ShiroAuthRealmType;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.rxjava.core.http.HttpServer;
import io.vertx.rxjava.ext.auth.AuthProvider;
import io.vertx.rxjava.ext.auth.jwt.JWTAuth;
import io.vertx.rxjava.ext.auth.shiro.ShiroAuth;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.handler.*;
import io.vertx.rxjava.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.rxjava.ext.web.sstore.LocalSessionStore;
import io.vertx.starter.http.route.ApiRoute;
import io.vertx.starter.http.route.PageRoute;
import org.jspare.vertx.annotation.Module;
import org.jspare.vertx.annotation.Modules;
import org.jspare.vertx.rxjava.JspareVerticle;
import org.jspare.vertx.rxjava.web.builder.RouterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Single;

import java.util.ArrayList;
import java.util.List;

@Modules({
  @Module(JspareServerModule.class)
})
public class JspareServerVerticle extends JspareVerticle {

  public static final String CONFIG_HTTP_SERVER_PORT = "http.server.port";

  private static final Logger logger = LoggerFactory.getLogger(JspareServerVerticle.class);


  @Override
  public void start() throws Exception {

    boolean useAlpn = JdkSSLEngineOptions.isAlpnAvailable();
    if (useAlpn) {
      logger.info("ALPN is enabled by JDK SSL/TLS engine");
    } else {
      logger.info("ALPN not available for JDK SSL/TLS engine");
    }

    HttpServer server = vertx.createHttpServer(new HttpServerOptions()
      .setSsl(true)
      .setJdkSslEngineOptions(new JdkSSLEngineOptions())
      .setKeyStoreOptions(new JksOptions()
        .setPath("server-keystore.jks")
        .setPassword("secret"))
      .setUseAlpn(useAlpn));

    AuthProvider auth = ShiroAuth.create(vertx, new ShiroAuthOptions()
      .setType(ShiroAuthRealmType.PROPERTIES)
      .setConfig(new JsonObject()
        .put("properties_path", "classpath:wiki-users.properties")));

    Router router = RouterBuilder.create(vertx)
      .addHandler(CookieHandler.create())
      .addHandler(BodyHandler.create())
      .addHandler(SessionHandler.create(LocalSessionStore.create(vertx)))
      .addHandler(UserSessionHandler.create(auth))
      .authHandler(() -> RedirectAuthHandler.create(auth, "/login"))
      .addRoute(PageRoute.class)
      .route(route -> route.path("/app/*").handler(StaticHandler.create().setCachingEnabled(false)))
      .build();

    router.post("/login-auth").handler(FormLoginHandler.create(auth));

    router.get("/logout").handler(context -> {
      context.clearUser();
      context.response()
        .setStatusCode(302)
        .putHeader("Location", "/")
        .end();
    });

    JWTAuth jwtAuth = JWTAuth.create(vertx, new JsonObject()
      .put("keyStore", new JsonObject()
        .put("path", "keystore.jceks")
        .put("type", "jceks")
        .put("password", "secret")));

    Router apiRouter = RouterBuilder.create(vertx)
      .authHandler(() -> JWTAuthHandler.create(jwtAuth, "/api/token"))
      .addRoute(ApiRoute.class)
      .build();

    apiRouter.get("/token").handler(context -> {

      JsonObject creds = new JsonObject()
        .put("username", context.request().getHeader("login"))
        .put("password", context.request().getHeader("password"));
      auth.rxAuthenticate(creds).flatMap(user -> {

        Single<Boolean> create = user.rxIsAuthorised("create");
        Single<Boolean> delete = user.rxIsAuthorised("delete");
        Single<Boolean> update = user.rxIsAuthorised("update");

        return Single.zip(create, delete, update, (canCreate, canDelete, canUpdate) -> {
          List<String> permissions = new ArrayList<>();
          if (canCreate) {
            permissions.add("create");
          }
          if (canDelete) {
            permissions.add("delete");
          }
          if (canUpdate) {
            permissions.add("update");
          }
          return jwtAuth.generateToken(new JsonObject()
            .put("username", context.request().getHeader("login")),
          new JWTOptions()
            .setSubject("Wiki API")
            .setIssuer("Vert.x")
            .setExpiresInMinutes(3600L)
            .setPermissions(permissions));
        });

      }).subscribe(token -> {
        context.response()
          .putHeader(HttpHeaders.CONTENT_TYPE, MediaType.PLAIN_TEXT_UTF_8.toString())
          .end(token);
      }, t -> context.fail(401));
    });

    router.mountSubRouter("/api", apiRouter);

    int portNumber = config().getInteger(CONFIG_HTTP_SERVER_PORT, 8080);
    server
      .requestHandler(router::accept)
      .rxListen(portNumber)
      .subscribe();

    SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
    BridgeOptions bridgeOptions = new BridgeOptions()
      .addInboundPermitted(new PermittedOptions().setAddress("app.markdown"))
      .addOutboundPermitted(new PermittedOptions().setAddress("page.saved"));
    sockJSHandler.bridge(bridgeOptions);
    router.route("/eventbus/*").handler(sockJSHandler);

    vertx.eventBus().<String>consumer("app.markdown", msg -> {
      String html = Processor.process(msg.body());
      msg.reply(html);
    });
  }

}
