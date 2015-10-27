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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.common.exceptions.ClientAuthenticationException;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.stereotype.Component;

import me.tfeng.playmods.avro.ApplicationError;
import me.tfeng.playmods.avro.AvroComponent;
import me.tfeng.toolbox.common.ThrowingFunction;
import play.libs.F.Promise;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
@Component("play-mods.oauth2.component")
public class OAuth2Component {

  public static boolean isAuthenticationError(Throwable t) {
    if (t instanceof AccessDeniedException
        || t instanceof AuthenticationException
        || t instanceof ClientAuthenticationException
        || t instanceof ClientRegistrationException
        || t instanceof AuthenticationError
        || (t instanceof ApplicationError) && ((ApplicationError) t).getStatus() == HttpStatus.SC_UNAUTHORIZED) {
      return true;
    }

    Throwable cause = t.getCause();
    if (cause != t && cause != null && isAuthenticationError(cause)) {
      return true;
    }

    if (t instanceof InvocationTargetException) {
      Throwable target = ((InvocationTargetException) t).getTargetException();
      if (isAuthenticationError(target)) {
        return true;
      }
    }

    return false;
  }

  @Autowired
  @Qualifier("play-mods.oauth2.authentication-manager")
  private AuthenticationManagerClient authenticationManager;

  @Autowired
  @Qualifier("play-mods.avro.component")
  private AvroComponent avroComponent;

  public <T, E extends Throwable> Promise<T> callWithAuthorizationToken(String token,
      ThrowingFunction<Promise<T>, E> function) throws E {
    if (token == null) {
      SecurityContextHolder.clearContext();
      return function.apply();
    } else {
      Promise<me.tfeng.playmods.oauth2.Authentication> promise = getAuthenticationManager().authenticate(token);
      return promise.flatMap(authentication -> {
        org.springframework.security.oauth2.provider.OAuth2Authentication oauth2Authentication =
            new org.springframework.security.oauth2.provider.OAuth2Authentication(
                getOAuth2Request(authentication.getClient()),
                getAuthentication(authentication.getUser()));
        SecurityContextHolder.getContext().setAuthentication(oauth2Authentication);
        try {
          return function.apply();
        } finally {
          SecurityContextHolder.clearContext();
        }
      });
    }
  }

  public AuthenticationManagerClient getAuthenticationManager() {
    return authenticationManager;
  }

  public <T> T localClient(Class<T> interfaceClass, Object implementation) {
    Class<?> implementationClass = implementation.getClass();
    return interfaceClass.cast(Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class[] { interfaceClass },
        (proxy, method, args) -> {
          if (Promise.class.isAssignableFrom(method.getReturnType())) {
            org.springframework.security.core.Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
            return Promise.promise(() -> {
              Method implementationMethod = implementationClass.getMethod(method.getName(), method.getParameterTypes());
              SecurityContextHolder.getContext().setAuthentication(authentication);
              try {
                return implementationMethod.invoke(implementation, args);
              } catch (InvocationTargetException e) {
                throw e.getTargetException();
              } finally {
                SecurityContextHolder.clearContext();
              }
            }, avroComponent.getExecutionContext());
          } else {
            try {
              return method.invoke(implementation, args);
            } catch (InvocationTargetException e) {
              throw e.getTargetException();
            }
          }
        }));
  }

  private UsernamePasswordAuthenticationToken getAuthentication(UserAuthentication user) {
    if (user == null) {
      return null;
    } else {
      List<GrantedAuthority> authorities = user.getAuthorities().stream()
          .map(authority -> new SimpleGrantedAuthority(authority.toString()))
          .collect(Collectors.toList());
      return new UsernamePasswordAuthenticationToken(user.getId().toString(), null, authorities);
    }
  }

  private OAuth2Request getOAuth2Request(ClientAuthentication client) {
    List<GrantedAuthority> authorities = client.getAuthorities().stream()
        .map(authority -> new SimpleGrantedAuthority(authority.toString()))
        .collect(Collectors.toList());
    Set<String> scopes = client.getScopes().stream().map(scope -> scope.toString()).collect(Collectors.toSet());
    return new OAuth2Request(Collections.emptyMap(), client.getId().toString(), authorities, true, scopes,
        Collections.emptySet(), null, Collections.emptySet(), Collections.emptyMap());
  }
}
