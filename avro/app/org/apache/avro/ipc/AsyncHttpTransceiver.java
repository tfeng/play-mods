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

package org.apache.avro.ipc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.List;

import me.tfeng.playmods.avro.ApplicationError;
import me.tfeng.playmods.avro.AsyncTransceiver;
import me.tfeng.playmods.avro.RemoteInvocationException;
import me.tfeng.playmods.http.RequestPoster;
import me.tfeng.playmods.http.RequestPreparer;
import play.libs.F.Promise;
import play.libs.ws.WSResponse;
import scala.concurrent.ExecutionContext;

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

  private final ExecutionContext executionContext;

  private final RequestPoster requestPoster;

  private final URL url;

  public AsyncHttpTransceiver(URL url, ExecutionContext executionContext, RequestPoster requestPoster) {
    super(url);
    this.url = url;
    this.executionContext = executionContext;
    this.requestPoster = requestPoster;
  }

  @Override
  public Promise<List<ByteBuffer>> transceive(List<ByteBuffer> request, RequestPreparer postRequestPreparer) {
    return asyncReadBuffers(asyncWriteBuffers(request, postRequestPreparer));
  }

  protected Promise<List<ByteBuffer>> asyncReadBuffers(Promise<WSResponse> responsePromise) {
    return responsePromise.transform(response -> {
      try {
        int status = response.getStatus();
        if (status >= 400) {
          throw ApplicationError.newBuilder()
              .setStatus(status)
              .setMessage$("Remote server returned HTTP response code " + status)
              .setValue("Remote server at " + url + " returned HTTP response code " + status)
              .build();
        }
        InputStream stream = response.getBodyAsStream();
        return readBuffers(stream);
      } catch (Throwable t) {
        throw new RemoteInvocationException("Remote invocation to server at " + url + " failed", t);
      }
    }, throwable -> new RemoteInvocationException("Remote invocation to server at " + url + " failed", throwable));
  }

  protected Promise<WSResponse> asyncWriteBuffers(List<ByteBuffer> buffers, RequestPreparer postRequestPreparer) {
    return Promise.promise(() -> {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      writeBuffers(buffers, outputStream);
      return outputStream;
    }, executionContext).flatMap(outputStream -> postRequest(url, outputStream.toByteArray(), postRequestPreparer));
  }

  protected String getContentType() {
    return CONTENT_TYPE;
  }

  protected Promise<WSResponse> postRequest(URL url, byte[] body, RequestPreparer postRequestPreparer)
      throws IOException {
    return requestPoster.postRequest(url, getContentType(), body, postRequestPreparer);
  }
}
