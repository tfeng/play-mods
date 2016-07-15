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

package me.tfeng.playmods.avro;

import java.net.URL;

import me.tfeng.playmods.http.RequestPreparer;
import play.Logger;
import play.Logger.ALogger;
import play.libs.ws.WSRequest;
import play.mvc.Controller;
import play.mvc.Http.Request;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class AuthorizationPreservingRequestPreparer implements RequestPreparer {

  public static final String AUTHORIZATION_HEADER = "Authorization";

  public static final String BEARER = "Bearer";

  private static final ALogger LOG = Logger.of(AuthorizationPreservingRequestPreparer.class);

  private Request controllerRequest;

  public AuthorizationPreservingRequestPreparer() {
    try {
      controllerRequest = Controller.request();
    } catch (RuntimeException e) {
      LOG.info("Unable to get current request; do not preserve authorization header");
    }
  }

  @Override
  public void prepare(WSRequest request, String contentType, URL url) {
    if (request != null) {
      String authorization = IpcContextHolder.get(AUTHORIZATION_HEADER);
      if (authorization != null) {
        request.setHeader(AUTHORIZATION_HEADER, BEARER + " " + authorization);
      } else if (controllerRequest != null) {
        authorization = controllerRequest.getHeader(AUTHORIZATION_HEADER);
        if (authorization != null) {
          request.setHeader(AUTHORIZATION_HEADER, authorization);
        }
      }
    }
  }
}
