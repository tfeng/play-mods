/**
 * Copyright 2015 Thomas Feng
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

import me.tfeng.playmods.modules.SpringModule;
import me.tfeng.toolbox.spring.ApplicationManager;
import play.Logger;
import play.Logger.ALogger;
import play.libs.F.Promise;
import play.mvc.Action;
import play.mvc.Http.Context;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.Results;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class OAuth2AuthenticationAction extends Action<OAuth2Authentication> {

  private static final String ACCESS_TOKEN = "access_token";

  private static final String AUTHORIZATION_HEADER = "authorization";

  private static final String BEARER = "bearer";

  private static final ALogger LOG = Logger.of(OAuth2AuthenticationAction.class);

  private static final String OAUTH2_COMPONENT_KEY = "play-mods.oauth2.component";

  private final OAuth2Component oauth2Component;

  public OAuth2AuthenticationAction() {
    ApplicationManager applicationManager = SpringModule.getApplicationManager();
    oauth2Component = applicationManager.getBean(OAUTH2_COMPONENT_KEY, OAuth2Component.class);
  }

  public Promise<Result> authorizeAndCall(Context context, Action<?> delegate) throws Throwable {
    Request request = context.request();
    String token = getAuthorizationToken(request);
    return oauth2Component.callWithAuthorizationToken(token,
        () -> delegate.call(context).recover(t -> handleAuthenticationError(t)))
        .recover(t -> handleAuthenticationError(t));
  }

  @Override
  public Promise<Result> call(Context context) throws Throwable {
    return authorizeAndCall(context, delegate);
  }

  protected String getAuthorizationToken(Request request) {
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

  protected Result handleAuthenticationError(Throwable t) throws Throwable {
    if (OAuth2Component.isAuthenticationError(t)) {
      LOG.warn("Authentication failed", t);
      return Results.unauthorized();
    } else {
      throw t;
    }
  }
}
