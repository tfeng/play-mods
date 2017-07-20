/**
 * Copyright 2016 Thomas Feng
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.tfeng.playmods.oauth2;

import java.lang.reflect.InvocationTargetException;

import javax.inject.Provider;

import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;

import com.google.inject.Inject;
import com.typesafe.config.Config;

import play.Environment;
import play.api.OptionalSourceMapper;
import play.api.routing.Router;
import play.mvc.Result;
import play.mvc.Results;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class ErrorHandler extends me.tfeng.playmods.spring.ErrorHandler {

  @Inject
  public ErrorHandler(Config config, Environment environment, OptionalSourceMapper sourceMapper,
      Provider<Router> routes) {
    super(config, environment, sourceMapper, routes);
  }

  @Override
  public Result getResultOnError(Throwable t) {
    if (OAuth2Component.isAuthenticationError(t)) {
      return Results.unauthorized();
    } else {
      OAuth2Exception oauth2Exception = getOAuth2Exception(t);
      if (oauth2Exception == null) {
        return super.getResultOnError(t);
      } else {
        return Results.status(oauth2Exception.getHttpErrorCode(), oauth2Exception.getMessage());
      }
    }
  }

  private OAuth2Exception getOAuth2Exception(Throwable t) {
    if (t instanceof InvocationTargetException) {
      return getOAuth2Exception(((InvocationTargetException) t).getTargetException());
    } else if (t instanceof OAuth2Exception) {
      return (OAuth2Exception) t;
    } else {
      Throwable cause = t.getCause();
      if (cause != null && cause != t) {
        return getOAuth2Exception(cause);
      } else {
        return null;
      }
    }
  }
}
