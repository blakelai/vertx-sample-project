package io.vertx.starter.database;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import org.jooq.Configuration;
import org.jooq.SQLDialect;
import org.jooq.impl.DefaultConfiguration;

import javax.sql.DataSource;

public class WikiDatabaseBinder extends AbstractModule {

  public static final String CONFIG_WIKIDB_JDBC_URL = "wikidb.jdbc.url";
  public static final String CONFIG_WIKIDB_JDBC_DRIVER_CLASS = "wikidb.jdbc.driver_class";
  public static final String CONFIG_WIKIDB_JDBC_MAX_POOL_SIZE = "wikidb.jdbc.max_pool_size";

  @Override
  protected void configure() {
  }

  @Provides @Singleton
  public DataSource provideDataSource(Vertx vertx) {
    JsonObject config = vertx.getOrCreateContext().config();
    HikariConfig dsConfig= new HikariConfig();
    dsConfig.setJdbcUrl(config.getString(CONFIG_WIKIDB_JDBC_URL, "jdbc:hsqldb:file:db/wiki"));
    dsConfig.setDriverClassName(config.getString(CONFIG_WIKIDB_JDBC_DRIVER_CLASS, "org.hsqldb.jdbc.JDBCDriver"));
    dsConfig.setMaximumPoolSize(config.getInteger(CONFIG_WIKIDB_JDBC_MAX_POOL_SIZE, 30));
    return new HikariDataSource(dsConfig);
  }

  @Provides @Singleton
  public SQLDialect provideSQLDialect() {
    return SQLDialect.HSQLDB;
  }

  @Provides @Singleton
  public JDBCClient provideJDBCClient(Vertx vertx, DataSource ds) {
    /*
    JsonObject config = vertx.getOrCreateContext().config();
    return JDBCClient.createShared(vertx, new JsonObject()
      .put("provider_class", "io.vertx.ext.jdbc.spi.impl.HikariCPDataSourceProvider")
      .put("jdbcUrl", config.getString(CONFIG_WIKIDB_JDBC_URL, "jdbc:hsqldb:file:db/wiki"))
      .put("driverClassName", config.getString(CONFIG_WIKIDB_JDBC_DRIVER_CLASS, "org.hsqldb.jdbc.JDBCDriver"))
      .put("maximumPoolSize", config.getInteger(CONFIG_WIKIDB_JDBC_MAX_POOL_SIZE, 30))
      );
    */
    return JDBCClient.create(vertx, ds);
  }

  @Provides @Singleton
  public io.vertx.rxjava.ext.jdbc.JDBCClient provideRxJDBCClient(JDBCClient client) {
    return io.vertx.rxjava.ext.jdbc.JDBCClient.newInstance(client);
  }

  @Provides @Singleton
  public Configuration providerConfiguration(DataSource ds) {
    return new DefaultConfiguration().set(ds);
  }

}
