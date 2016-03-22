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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletionStage;

import org.apache.avro.ipc.AsyncHttpTransceiver;
import org.apache.http.entity.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.google.inject.Inject;

import me.tfeng.playmods.avro.factories.ResponderFactory;
import me.tfeng.playmods.spring.ExceptionWrapper;
import play.Application;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Results;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
@Component("play-mods.avro.binary-ipc-controller")
public class BinaryIpcController extends Controller {

  public static final String CONTENT_TYPE = "avro/binary";

  public static final String CONTENT_TYPE_HEADER = "content-type";

  @Inject
  private Application application;

  @Autowired
  @Qualifier("play-mods.avro.component")
  private AvroComponent avroComponent;

  @Autowired
  @Qualifier("play-mods.avro.responder-factory")
  private ResponderFactory responderFactory;

  @BodyParser.Of(BodyParser.Raw.class)
  public CompletionStage<Result> post(String protocol) throws Throwable {
    String contentTypeHeader = request().getHeader(CONTENT_TYPE_HEADER);
    ContentType contentType = ContentType.parse(contentTypeHeader);
    if (!CONTENT_TYPE.equals(contentType.getMimeType())) {
      throw new RuntimeException("Unable to handle content type " + contentType + "; " + CONTENT_TYPE + " is expected");
    }

    Class<?> protocolClass = application.classloader().loadClass(protocol);
    Object implementation = avroComponent.getProtocolImplementations().get(protocolClass);
    InputStream inputStream = request().body().asRaw().asBytes().iterator().asInputStream();
    List<ByteBuffer> buffers = AsyncHttpTransceiver.readBuffers(inputStream);
    AsyncResponder responder = createResponder(protocolClass, implementation);
    CompletionStage<List<ByteBuffer>> response = responder.asyncRespond(buffers);

    return response.thenApply(result -> {
      ByteArrayOutputStream outStream = new ByteArrayOutputStream();
      try {
        AsyncHttpTransceiver.writeBuffers(result, outStream);
      } catch (Throwable t) {
        throw ExceptionWrapper.wrap(t);
      } finally {
        try {
          outStream.close();
        } catch (Exception e) {
        }
      }
      return Results.ok(outStream.toByteArray());
    });
  }

  protected AsyncResponder createResponder(Class<?> protocolClass, Object implementation) {
    return responderFactory.create(protocolClass, implementation);
  }
}
