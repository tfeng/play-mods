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

import java.util.concurrent.CompletionStage;

import org.apache.http.HttpStatus;

import com.google.inject.Inject;

import me.tfeng.playmods.avro.IpcContextHolder;
import me.tfeng.playmods.spring.ApplicationError;
import me.tfeng.playmods.spring.ExceptionWrapper;
import me.tfeng.toolbox.spring.ApplicationManager;
import play.mvc.Action;
import play.mvc.Http.Context;
import play.mvc.Http.Request;
import play.mvc.Http.RequestHeader;
import play.mvc.Result;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class OAuth2AuthenticationAction extends Action<OAuth2Authentication> {

  public static final String ACCESS_TOKEN = "access_token";

  public static final String AUTHORIZATION_HEADER = "Authorization";

  public static final String BEARER = "Bearer";

  private static final String OAUTH2_COMPONENT_KEY = "play-mods.oauth2.component";

  public static String getAuthorizationToken(RequestHeader request) {
    String[] headers = request.headers().get(AUTHORIZATION_HEADER);
    if (headers != null) {
      for (String header : headers) {
        if (header.toLowerCase().startsWith(BEARER.toLowerCase())) {
          String authHeaderValue = header.substring(BEARER.length()).trim();
          return authHeaderValue.split(",")[0];
        }
      }
    }
    return request.getQueryString(ACCESS_TOKEN);
  }

  @Inject
  private ApplicationManager applicationManager;

  public CompletionStage<Result> authorizeAndCall(Context context, Action<?> delegate) {
    OAuth2Component oauth2Component = applicationManager.getBean(OAUTH2_COMPONENT_KEY, OAuth2Component.class);
    Request request = context.request();
    String token = getAuthorizationToken(request);

    String oldToken = IpcContextHolder.get(AUTHORIZATION_HEADER);
    try {
      IpcContextHolder.set(AUTHORIZATION_HEADER, token);
      return oauth2Component
          .callWithAuthorizationToken(token, () -> delegate.call(context)
              .exceptionally(ExceptionWrapper.wrapFunction(t -> handleAuthenticationError(token, t))))
          .exceptionally(ExceptionWrapper.wrapFunction(t -> handleAuthenticationError(token, t)));
    } finally {
      IpcContextHolder.set(AUTHORIZATION_HEADER, oldToken);
    }
  }

  @Override
  public CompletionStage<Result> call(Context context) {
    return authorizeAndCall(context, delegate);
  }

  protected Result handleAuthenticationError(String token, Throwable t) throws Throwable {
    t = ExceptionWrapper.unwrap(t);
    if (OAuth2Component.isAuthenticationError(t)) {
      if (!(t instanceof ApplicationError)) {
        throw new ApplicationError(HttpStatus.SC_UNAUTHORIZED, "Authentication failed for token " + token, t);
      }
    }
    throw t;
  }
}
