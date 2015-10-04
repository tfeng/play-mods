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

package me.tfeng.playmods.modules;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;

import me.tfeng.playmods.oauth2.OAuth2AuthenticationAction;
import me.tfeng.playmods.oauth2.OAuth2Component;
import play.Application;
import play.libs.F.Promise;
import play.mvc.Action;
import play.mvc.Action.Simple;
import play.mvc.Http.Context;
import play.mvc.Http.Request;
import play.mvc.Http.RequestHeader;
import play.mvc.Result;
import play.mvc.Results;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class OAuth2GlobalSettings extends AvroIpcGlobalSettings {

  public class OAuth2Action extends Simple {

    @Override
    public Promise<Result> call(Context context) throws Throwable {
      return authenticationAction.authorizeAndCall(context, delegate);
    }
  }

  private OAuth2AuthenticationAction authenticationAction;

  @Override
  public Promise<Result> onError(RequestHeader request, Throwable t) {
    if (OAuth2Component.isAuthenticationError(t)) {
      return Promise.pure(Results.unauthorized());
    } else {
      OAuth2Exception oauth2Exception = getOAuth2Exception(t);
      if (oauth2Exception != null) {
        return Promise.pure(Results.status(oauth2Exception.getHttpErrorCode(), oauth2Exception.getMessage()));
      } else {
        return super.onError(request, t);
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

  @Override
  public Action<Void> onRequest(Request request, Method actionMethod) {
    return new OAuth2Action();
  }

  public void onStart(Application application) {
    super.onStart(application);
    authenticationAction = new OAuth2AuthenticationAction();
  }
}
