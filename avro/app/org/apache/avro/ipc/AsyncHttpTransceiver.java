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

package org.apache.avro.ipc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

import me.tfeng.playmods.avro.AsyncTransceiver;
import me.tfeng.playmods.http.RequestPoster;
import me.tfeng.playmods.http.RequestPreparer;
import me.tfeng.playmods.spring.ApplicationError;
import me.tfeng.playmods.spring.ExceptionWrapper;
import play.libs.ws.WSResponse;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class AsyncHttpTransceiver extends HttpTransceiver implements AsyncTransceiver {

  public static List<ByteBuffer> readBuffers(InputStream in) throws IOException {
    return HttpTransceiver.readBuffers(in);
  }

  public static void writeBuffers(List<ByteBuffer> buffers, OutputStream out) throws IOException {
    HttpTransceiver.writeBuffers(buffers, out);
  }

  private final Executor executor;

  private final RequestPoster requestPoster;

  private final URL url;

  public AsyncHttpTransceiver(URL url, Executor executor, RequestPoster requestPoster) {
    super(url);
    this.url = url;
    this.executor = executor;
    this.requestPoster = requestPoster;
  }

  @Override
  public CompletionStage<List<ByteBuffer>> transceive(List<ByteBuffer> request, RequestPreparer postRequestPreparer) {
    return asyncReadBuffers(asyncWriteBuffers(request, postRequestPreparer));
  }

  protected CompletionStage<List<ByteBuffer>> asyncReadBuffers(CompletionStage<WSResponse> responseCompletionStage) {
    return responseCompletionStage.handle((response, throwable) -> {
      if (throwable != null) {
        throw ExceptionWrapper.wrap(throwable);
      } else {
        try {
          int status = response.getStatus();
          if (status >= 400) {
            throw new ApplicationError(status, "Remote server at " + url + " returned HTTP response code " + status);
          }
          InputStream stream = response.getBodyAsStream();
          return readBuffers(stream);
        } catch (Throwable t) {
          throw ExceptionWrapper.wrap(t);
        }
      }
    });
  }

  protected CompletionStage<WSResponse> asyncWriteBuffers(List<ByteBuffer> buffers,
      RequestPreparer postRequestPreparer) {
    return CompletableFuture
        .supplyAsync(ExceptionWrapper.wrapFunction(() -> {
          ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
          writeBuffers(buffers, outputStream);
          return outputStream;
        }), executor)
        .thenCompose(ExceptionWrapper.wrapFunction(outputStream ->
            postRequest(url, outputStream.toByteArray(), postRequestPreparer)));
  }

  protected String getContentType() {
    return CONTENT_TYPE;
  }

  protected CompletionStage<WSResponse> postRequest(URL url, byte[] body, RequestPreparer postRequestPreparer)
      throws IOException {
    return requestPoster.postRequest(url, getContentType(), body, postRequestPreparer);
  }
}
