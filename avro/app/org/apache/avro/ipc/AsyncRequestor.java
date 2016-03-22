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

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.avro.Protocol;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;

import me.tfeng.playmods.avro.AsyncTransceiver;
import me.tfeng.playmods.avro.ResponseProcessor;
import me.tfeng.playmods.http.RequestPreparer;
import me.tfeng.playmods.spring.ExceptionWrapper;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class AsyncRequestor extends SpecificRequestor {

  public class Request extends Requestor.Request {

    public Request(String messageName, Object request, RPCContext context) {
      super(messageName, request, context);
    }
  }

  private final RequestPreparer requestPreparer;

  private final int requestTimeout;

  private final ResponseProcessor responseProcessor;

  private final boolean useGenericRecord;

  public AsyncRequestor(Protocol protocol, AsyncTransceiver transceiver, SpecificData data, int requestTimeout,
      RequestPreparer requestPreparer, ResponseProcessor responseProcessor, boolean useGenericRecord)
      throws IOException {
    super(protocol, (Transceiver) transceiver, data);
    this.requestTimeout = requestTimeout;
    this.requestPreparer = requestPreparer;
    this.responseProcessor = responseProcessor;
    this.useGenericRecord = useGenericRecord;
  }

  @Override
  public DatumReader<Object> getDatumReader(Schema writer, Schema reader) {
    if (useGenericRecord) {
      return new GenericDatumReader<>(writer, reader);
    } else {
      return new SpecificDatumReader<>(writer, reader, getSpecificData());
    }
  }

  @Override
  public DatumWriter<Object> getDatumWriter(Schema schema) {
    if (useGenericRecord) {
      return new GenericDatumWriter<>(schema);
    } else {
      return new SpecificDatumWriter<>(schema, getSpecificData());
    }
  }

  public List<RPCPlugin> getRPCPlugins() {
    return Collections.unmodifiableList(rpcMetaPlugins);
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    CompletionStage<Object> completionStage = request(method.getName(), args);
    if (CompletionStage.class.isAssignableFrom(method.getReturnType())) {
      return completionStage;
    } else {
      try {
        return completionStage.toCompletableFuture().get(requestTimeout, TimeUnit.MILLISECONDS);
      } catch (ExecutionException t) {
        Throwable cause = ExceptionWrapper.unwrap(t);
        if (cause instanceof RuntimeException) {
          throw cause;
        }
        Class<?>[] exceptionTypes = method.getExceptionTypes();
        for (Class<?> exceptionType : exceptionTypes) {
          if (exceptionType.isInstance(cause)) {
            throw cause;
          }
        }
        throw t;
      }
    }
  }

  public CompletionStage<Object> request(String message, Object[] args) throws Exception {
    AsyncTransceiver transceiver = (AsyncTransceiver) getTransceiver();
    Request ipcRequest = new Request(message, args, new RPCContext());
    CallFuture<Object> callFuture = ipcRequest.getMessage().isOneWay() ? null : new CallFuture<>();
    return transceiver.transceive(ipcRequest.getBytes(), requestPreparer).thenApply(response -> {
      Object responseObject;
      try {
        responseObject = responseProcessor.process(this, ipcRequest, message, response);
        if (callFuture != null) {
          callFuture.handleResult(responseObject);
        }
      } catch (Exception e) {
        if (callFuture != null) {
          callFuture.handleError(e);
        }
      }

      // transceiverCallback.handleResult(response);
      if (callFuture == null) {
        return null;
      } else if (callFuture.getError() == null) {
        return callFuture.getResult();
      } else {
        throw ExceptionWrapper.wrap(callFuture.getError());
      }
    });
  }
}
