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
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Protocol;
import org.apache.avro.Protocol.Message;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.ipc.Responder;
import org.apache.avro.ipc.specific.SpecificResponder;
import org.apache.avro.specific.SpecificExceptionBase;
import org.apache.http.entity.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;

import akka.util.ByteIterator;
import akka.util.ByteString;
import me.tfeng.playmods.avro.factories.ResponderFactory;
import me.tfeng.playmods.spring.ExceptionWrapper;
import me.tfeng.playmods.spring.ThrowingFunction;
import me.tfeng.toolbox.avro.AvroHelper;
import me.tfeng.toolbox.common.Constants;
import play.Application;
import play.Logger;
import play.Logger.ALogger;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http.Context;
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

  @Inject
  private Application application;

  @Autowired
  @Qualifier("play-mods.avro.component")
  private AvroComponent avroComponent;

  @Autowired
  @Qualifier("play-mods.avro.responder-factory")
  private ResponderFactory responderFactory;

  @BodyParser.Of(BodyParser.Raw.class)
  public CompletionStage<Result> post(String message, String protocol) throws Throwable {
    String contentTypeHeader = request().getHeader(CONTENT_TYPE_HEADER);
    ContentType contentType = ContentType.parse(contentTypeHeader);
    if (!CONTENT_TYPE.equals(contentType.getMimeType())) {
      throw new RuntimeException("Unable to handle content type " + contentType + "; " + CONTENT_TYPE + " is expected");
    }

    Class<?> protocolClass = application.classloader().loadClass(protocol);
    Object implementation = avroComponent.getProtocolImplementations().get(protocolClass);
    Protocol avroProtocol = AvroHelper.getProtocol(protocolClass);
    Message avroMessage = avroProtocol.getMessages().get(message);
    ByteString bytes = request().body().asRaw().asBytes();
    AsyncResponder responder = createResponder(protocolClass, implementation);
    Object request = getRequest(responder, avroMessage, bytes);
    Method method = getMethod(responder, implementation, avroMessage, request);

    ThrowingFunction<Object, CompletionStage<Result>, Exception> resultConverter =
        getResultConverter(avroProtocol, avroMessage);
    ThrowingFunction<Throwable, Result, Exception> errorConverter = getErrorConverter(avroProtocol, avroMessage);

    CompletionStage<?> completionStage;
    if (CompletionStage.class.isAssignableFrom(method.getReturnType())) {
      completionStage = (CompletionStage<?>) responder.respond(avroMessage, request);
    } else {
      try {
        completionStage = CompletableFuture.completedFuture(responder.respond(avroMessage, request));
      } catch (SpecificExceptionBase e) {
        return CompletableFuture.completedFuture(errorConverter.apply(e));
      }
    }

    return completionStage
        .thenCompose(ExceptionWrapper.wrapFunction(resultConverter))
        .exceptionally(ExceptionWrapper.wrapFunction(errorConverter));
  }

  protected Result convertErrorResult(Protocol protocol, Message message, Throwable t) throws IOException {
    t = ExceptionWrapper.unwrap(t);
    if (t instanceof SpecificExceptionBase) {
      try {
        String errorContent = AvroHelper.toSimpleJson(message.getErrors(), t);
        LOG.error("Error occurred while processing request (message = " + message.getName() + ", protocol = "
            + protocol.getName() + "): " + errorContent, t);
        return Results.badRequest(errorContent);
      } catch (IOException e) {
        throw ExceptionWrapper.wrap(e);
      }
    } else {
      throw ExceptionWrapper.wrap(t);
    }
  }

  protected CompletionStage<Result> convertResult(Protocol protocol, Message message, Object result) throws Exception {
    return CompletableFuture.completedFuture(Results.ok(AvroHelper.toSimpleJson(message.getResponse(), result)));
  }

  protected AsyncResponder createResponder(Class<?> protocolClass, Object implementation) {
    return responderFactory.create(protocolClass, implementation);
  }

  protected ThrowingFunction<Throwable, Result, Exception> getErrorConverter(Protocol protocol, Message message) {
    return error -> convertErrorResult(protocol, message, error);
  }

  protected ThrowingFunction<Object, CompletionStage<Result>, Exception> getResultConverter(Protocol protocol, Message message) {
    Context context = Context.current();
    return result -> {
      Context oldContext = Context.current.get();
      try {
        Context.current.set(context);
        return convertResult(protocol, message, result);
      } finally {
        Context.current.set(oldContext);
      }
    };
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

  private Object getRequest(Responder responder, Message message, ByteString bytes) throws IOException {
    ByteIterator iterator = bytes.iterator();
    InputStream inputStream;
    if (iterator.hasNext()) {
      inputStream = iterator.asInputStream();
    } else {
      byte[] emptyBytes = "{}".getBytes(Constants.UTF8);
      inputStream = new ByteArrayInputStream(emptyBytes);
    }
    Schema schema = message.getRequest();
    JsonNode node = Json.parse(inputStream);
    node = AvroHelper.convertFromSimpleRecord(schema, node);
    return responder.readRequest(message.getRequest(), message.getRequest(),
        DecoderFactory.get().jsonDecoder(schema, node.toString()));
  }
}
