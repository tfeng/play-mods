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

import java.lang.reflect.Method;
import java.util.concurrent.CompletionStage;

import com.google.inject.Inject;
import com.google.inject.Provider;

import play.mvc.Action;
import play.mvc.Action.Simple;
import play.mvc.Http.Context;
import play.mvc.Http.Request;
import play.mvc.Result;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class ActionCreator implements play.http.ActionCreator {

  public class OAuth2Action extends Simple {

    @Override
    public CompletionStage<Result> call(Context context) {
      return actionProvider.get().authorizeAndCall(context, delegate);
    }
  }

  private final OAuth2Action action = new OAuth2Action();

  private final Provider<OAuth2AuthenticationAction> actionProvider;

  @Inject
  public ActionCreator(Provider<OAuth2AuthenticationAction> actionProvider) {
    this.actionProvider = actionProvider;
  }

  @Override
  public Action createAction(Request request, Method actionMethod) {
    return action;
  }
}
