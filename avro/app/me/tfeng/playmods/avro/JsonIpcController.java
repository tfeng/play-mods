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

package me.tfeng.playmods.avro;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Method;

import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Protocol;
import org.apache.avro.Protocol.Message;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.ipc.Responder;
import org.apache.avro.ipc.specific.SpecificResponder;
import org.apache.avro.specific.SpecificExceptionBase;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.entity.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import me.tfeng.toolbox.avro.AvroHelper;
import me.tfeng.toolbox.common.Constants;
import play.Logger;
import play.Logger.ALogger;
import play.Play;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Results;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
@Component("play-mods.avro.json-ipc-controller")
public class JsonIpcController extends Controller {

  public static final String CONTENT_TYPE = "avro/json";

  public static final String CONTENT_TYPE_HEADER = "content-type";

  private static final ALogger LOG = Logger.of(JsonIpcController.class);

  @Autowired
  @Qualifier("play-mods.avro.component")
  private AvroComponent avroComponent;

  @BodyParser.Of(BodyParser.Raw.class)
  public Promise<Result> post(String message, String protocol) throws Throwable {
    String contentTypeHeader = request().getHeader(CONTENT_TYPE_HEADER);
    ContentType contentType = ContentType.parse(contentTypeHeader);
    if (!CONTENT_TYPE.equals(contentType.getMimeType())) {
      throw new RuntimeException("Unable to handle content type " + contentType + "; " + CONTENT_TYPE + " is expected");
    }

    Class<?> protocolClass = Play.application().classloader().loadClass(protocol);
    Object implementation = avroComponent.getProtocolImplementations().get(protocolClass);
    Protocol avroProtocol = AvroHelper.getProtocol(protocolClass);
    Message avroMessage = avroProtocol.getMessages().get(message);
    byte[] bytes = request().body().asRaw().asBytes();
    SpecificResponder responder = new SpecificResponder(protocolClass, implementation);
    Object request = getRequest(responder, avroMessage, bytes);
    Method method = getMethod(responder, implementation, avroMessage, request);

    Promise<?> promise;
    if (Promise.class.isAssignableFrom(method.getReturnType())) {
      promise = (Promise<?>) responder.respond(avroMessage, request);
    } else {
      promise = Promise.promise(() -> responder.respond(avroMessage, request), avroComponent.getExecutionContext());
    }
    return promise
        .map(result -> (Result) Results.ok(AvroHelper.toJson(avroMessage.getResponse(), result)))
        .recover(error -> {
          if (error instanceof SpecificExceptionBase) {
            LOG.warn("Exception thrown while processing Json IPC request", error);
            return Results.badRequest(AvroHelper.toJson(avroMessage.getErrors(), error));
          } else {
            throw error;
          }
        });
  }

  private Method getMethod(SpecificResponder responder, Object implementation, Message message, Object request) {
    int numParams = message.getRequest().getFields().size();
    Object[] params = new Object[numParams];
    Class[] paramTypes = new Class[numParams];
    int i = 0;
    for (Schema.Field param: message.getRequest().getFields()) {
      params[i] = ((GenericRecord) request).get(param.name());
      paramTypes[i] = responder.getSpecificData().getClass(param.schema());
      i++;
    }
    try {
      return implementation.getClass().getMethod(message.getName(), paramTypes);
    } catch (NoSuchMethodException e) {
      throw new AvroRuntimeException(e);
    }
  }

  private Object getRequest(Responder responder, Message message, byte[] data) throws IOException {
    Schema schema = message.getRequest();
    if (ArrayUtils.isEmpty(data)) {
      // The method takes no argument; use empty data.
      data = "{}".getBytes(Constants.UTF8);
    }
    JsonNode node = Json.parse(new ByteArrayInputStream(data));
    node = AvroHelper.convertFromSimpleRecord(schema, node);
    return responder.readRequest(message.getRequest(), message.getRequest(),
        DecoderFactory.get().jsonDecoder(schema, node.toString()));
  }
}
