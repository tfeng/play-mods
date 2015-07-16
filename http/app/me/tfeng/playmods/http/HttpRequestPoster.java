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

package me.tfeng.playmods.http;

import java.io.IOException;
import java.net.URL;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.Response;

import me.tfeng.playmods.http.factories.ClientFactory;
import play.libs.F.Promise;
import play.libs.ws.WSResponse;
import play.libs.ws.ning.NingWSResponse;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
@Component("play-mods.http.request-poster")
public class HttpRequestPoster implements RequestPoster {

  private static final String CONTENT_TYPE_HEADER = "content-type";

  @Autowired
  @Qualifier("play-mods.http.client-factory")
  private ClientFactory clientFactory;

  @Override
  public Promise<WSResponse> postRequest(URL url, String contentType, byte[] body, RequestPreparer requestPreparer)
      throws IOException {
    scala.concurrent.Promise<WSResponse> scalaPromise = scala.concurrent.Promise$.MODULE$.apply();
    BoundRequestBuilder builder = clientFactory.create()
        .preparePost(url.toString())
        .setHeader(CONTENT_TYPE_HEADER, contentType)
        .setContentLength(body.length)
        .setBody(body);
    if (requestPreparer != null) {
      requestPreparer.prepare(builder, contentType, url);
    }
    builder.execute(new AsyncCompletionHandler<Response>() {
      @Override
      public Response onCompleted(Response response) {
        scalaPromise.success(new NingWSResponse(response));
        return response;
      }

      @Override
      public void onThrowable(Throwable t) {
        scalaPromise.failure(t);
      }
    });
    return Promise.wrap(scalaPromise.future());
  }
}
