/*
 * Copyright 2016 JSpare.org.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.jspare.vertx.reactivex.web.builder;

import io.vertx.core.Handler;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.AuthHandler;
import io.vertx.reactivex.ext.web.handler.sockjs.SockJSHandler;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.commons.lang.StringUtils;
import org.jspare.vertx.web.builder.HandlerType;

import java.lang.reflect.Method;

/* (non-Javadoc)
 * @see java.lang.Object#toString()
 */
@Data
@Accessors(fluent = true)

/**
 * Instantiates a new handler data.
 */
@NoArgsConstructor

/**
 * Instantiates a new handler data.
 *
 * @param clazz
 *          the clazz
 * @param method
 *          the method
 * @param handlerType
 *          the handler type
 * @param routeHandlerClass
 *          the route handler class
 * @param sockJSHandler
 *          the sock JS handler
 * @param path
 *          the path
 * @param order
 *          the order
 * @param pathRegex
 *          the path regex
 * @param httpMethod
 *          the http method
 * @param consumes
 *          the consumes
 * @param produces
 *          the produces
 * @param authHandler
 *          the auth handler
 * @param documentation
 *          the documentation
 */
@AllArgsConstructor

/*
 * (non-Javadoc)
 *
 * @see java.lang.Object#hashCode()
 */
@EqualsAndHashCode
public class HandlerData implements Cloneable {

  /**
   * The clazz.
   */
  private Class<?> clazz;

  /**
   * The method.
   */
  private Method method;

  /**
   * The handler type.
   */
  private HandlerType handlerType;

  /**
   * The route handler class.
   */
  private Class<? extends Handler<RoutingContext>> routeHandlerClass;

  /**
   * The sock JS handler.
   */
  private SockJSHandler sockJSHandler;

  /**
   * The path.
   */
  private String path = StringUtils.EMPTY;

  /**
   * The order.
   */
  private int order;

  /**
   * The path regex.
   */
  private boolean pathRegex;

  /**
   * The http method.
   */
  private String httpMethod;

  /**
   * The consumes.
   */
  private String consumes;

  /**
   * The produces.
   */
  private String produces;

  /**
   * The auth handler.
   */
  private AuthHandler authHandler;

  /**
   * To string line.
   *
   * @return the string
   */
  public String toStringLine() {
    StringBuilder line = new StringBuilder();
    line.append(String.format("[%s.%s]", clazz().getSimpleName(), method().getName()));
    line.append(String.format("[%s]", handlerType));
    if (StringUtils.isNotEmpty(httpMethod())) {
      line.append(String.format("[%s] ", httpMethod()));
    }
    if (StringUtils.isNotEmpty(path())) {
      line.append(String.format("[%s] ", path()));
    } else {
      line.append(String.format("[%s] ", clazz().getSimpleName())).append(String.format("[%s] ", method().getName()));
    }
    return line.toString();
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#clone()
   */
  @Override
  protected Object clone() throws CloneNotSupportedException {

    return super.clone();
  }
}
