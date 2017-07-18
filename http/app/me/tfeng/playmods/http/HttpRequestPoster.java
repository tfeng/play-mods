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

package me.tfeng.playmods.http;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.CompletionStage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import akka.util.ByteString;
import me.tfeng.playmods.http.factories.ClientFactory;
import play.libs.ws.InMemoryBodyWritable;
import play.libs.ws.StandaloneWSRequest;
import play.libs.ws.StandaloneWSResponse;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
@Component("play-mods.http.request-poster")
public class HttpRequestPoster implements RequestPoster {

  @Autowired
  @Qualifier("play-mods.http.client-factory")
  private ClientFactory clientFactory;

  @Override
  public CompletionStage<? extends StandaloneWSResponse> postRequest(URL url, String contentType, byte[] body,
      RequestPreparer requestPreparer) throws IOException {
    StandaloneWSRequest request = clientFactory.create().url(url.toString()).setContentType(contentType);
    if (requestPreparer != null) {
      requestPreparer.prepare(request, contentType, url);
    }
    return request.post(new InMemoryBodyWritable(ByteString.fromArray(body), contentType));
  }
}
