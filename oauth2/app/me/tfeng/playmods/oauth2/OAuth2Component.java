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

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */

import java.lang.reflect.InvocationTargetException;

import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.exceptions.ClientAuthenticationException;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.stereotype.Component;

import me.tfeng.playmods.avro.AsyncHttpException;
import me.tfeng.playmods.oauth2.AuthenticationError;
import me.tfeng.playmods.oauth2.AuthenticationManagerClient;

@Component("play-mods.oauth2.component")
public class OAuth2Component {

  public static boolean isAuthenticationError(Throwable t) {
    if (t instanceof AccessDeniedException
        || t instanceof AuthenticationException
        || t instanceof ClientAuthenticationException
        || t instanceof ClientRegistrationException
        || t instanceof AuthenticationError
        || (t instanceof AsyncHttpException)
        && ((AsyncHttpException) t).getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
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

  public AuthenticationManagerClient getAuthenticationManager() {
    return authenticationManager;
  }
}
