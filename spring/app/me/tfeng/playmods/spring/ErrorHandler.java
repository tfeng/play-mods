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

package me.tfeng.playmods.spring;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.inject.Provider;

import com.google.inject.Inject;

import play.Configuration;
import play.Environment;
import play.Logger;
import play.Logger.ALogger;
import play.api.OptionalSourceMapper;
import play.api.routing.Router;
import play.http.DefaultHttpErrorHandler;
import play.mvc.Http.RequestHeader;
import play.mvc.Result;
import play.mvc.Results;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class ErrorHandler extends DefaultHttpErrorHandler {

  private static final ALogger LOG = Logger.of(ErrorHandler.class);

  @Inject
  public ErrorHandler(Configuration configuration, Environment environment, OptionalSourceMapper sourceMapper,
      Provider<Router> routes) {
    super(configuration, environment, sourceMapper, routes);
  }

  public Result getResultOnError(Throwable t) {
    if (t instanceof ApplicationError) {
      return Results.status(((ApplicationError) t).getStatus());
    } else {
      return Results.badRequest();
    }
  }

  @Override
  public CompletionStage<Result> onServerError(RequestHeader request, Throwable exception) {
    Throwable cause = ExceptionWrapper.unwrap(exception);
    LOG.warn("Exception thrown while processing request", cause);
    return CompletableFuture.completedFuture(getResultOnError(cause));
  }
}
