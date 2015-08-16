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

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Request;

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

  @Override
  public Promise<Result> call(Context context) throws Throwable {
    return authorizeAndCall(context, delegate);
  }

  public Promise<Result> authorizeAndCall(Context context, Action<?> delegate) throws Throwable {
    Authentication currentAuthentication = SecurityContextHolder.getContext().getAuthentication();
    try {
      Request request = context.request();
      String token = getAuthorizationToken(request);
      if (token == null) {
        SecurityContextHolder.clearContext();
        try {
          return delegate.call(context).recover(t -> handleAuthenticationError(t));
        } catch (Throwable t) {
          return Promise.pure(handleAuthenticationError(t));
        }
      } else {
        Promise<me.tfeng.playmods.oauth2.Authentication> promise =
            oauth2Component.getAuthenticationManager().authenticate(token);
        return promise.flatMap(authentication -> {
          org.springframework.security.oauth2.provider.OAuth2Authentication oauth2Authentication =
              new org.springframework.security.oauth2.provider.OAuth2Authentication(
                  getOAuth2Request(authentication.getClient()),
                  getAuthentication(authentication.getUser()));
          SecurityContextHolder.getContext().setAuthentication(oauth2Authentication);
          try {
            return delegate.call(context).recover(t -> handleAuthenticationError(t));
          } catch (Throwable t) {
            return Promise.pure(handleAuthenticationError(t));
          }
        }).recover(t -> handleAuthenticationError(t));
      }
    } finally {
      SecurityContextHolder.getContext().setAuthentication(currentAuthentication);
    }
  }

  protected UsernamePasswordAuthenticationToken getAuthentication(UserAuthentication user) {
    if (user == null) {
      return null;
    } else {
      List<GrantedAuthority> authorities = user.getAuthorities().stream()
          .map(authority -> new SimpleGrantedAuthority(authority.toString()))
          .collect(Collectors.toList());
      return new UsernamePasswordAuthenticationToken(user.getId().toString(), null, authorities);
    }
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

  protected OAuth2Request getOAuth2Request(ClientAuthentication client) {
    List<GrantedAuthority> authorities = client.getAuthorities().stream()
        .map(authority -> new SimpleGrantedAuthority(authority.toString()))
        .collect(Collectors.toList());
    Set<String> scopes = client.getScopes().stream()
        .map(scope -> scope.toString())
        .collect(Collectors.toSet());
    return new OAuth2Request(Collections.emptyMap(), client.getId().toString(), authorities, true, scopes,
        Collections.emptySet(), null, Collections.emptySet(), Collections.emptyMap());
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
