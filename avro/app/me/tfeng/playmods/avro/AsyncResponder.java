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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Protocol;
import org.apache.avro.Protocol.Message;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.ipc.RPCContext;
import org.apache.avro.ipc.RPCContextHelper;
import org.apache.avro.ipc.RPCPlugin;
import org.apache.avro.ipc.Transceiver;
import org.apache.avro.ipc.specific.SpecificResponder;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.util.ByteBufferInputStream;
import org.apache.avro.util.ByteBufferOutputStream;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import me.tfeng.playmods.spring.ExceptionWrapper;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class AsyncResponder extends SpecificResponder {

  private final Executor executor;

  private final Object impl;

  private final ProtocolVersionResolver protocolVersionResolver;

  public AsyncResponder(Class<?> iface, Object impl, Executor executor,
      ProtocolVersionResolver protocolVersionResolver) {
    super(iface, impl);
    this.impl = impl;
    this.executor = executor;
    this.protocolVersionResolver = protocolVersionResolver;
  }

  public AsyncResponder(Class<?> iface, Object impl, SpecificData data, Executor executor,
      ProtocolVersionResolver protocolVersionResolver) {
    super(iface, impl, data);
    this.impl = impl;
    this.executor = executor;
    this.protocolVersionResolver = protocolVersionResolver;
  }

  public AsyncResponder(Protocol protocol, Object impl, Executor executor,
      ProtocolVersionResolver protocolVersionResolver) {
    super(protocol, impl);
    this.impl = impl;
    this.executor = executor;
    this.protocolVersionResolver = protocolVersionResolver;
  }

  public AsyncResponder(Protocol protocol, Object impl, SpecificData data, Executor executor,
      ProtocolVersionResolver protocolVersionResolver) {
    super(protocol, impl, data);
    this.impl = impl;
    this.executor = executor;
    this.protocolVersionResolver = protocolVersionResolver;
  }

  public CompletionStage<List<ByteBuffer>> asyncRespond(List<ByteBuffer> buffers) throws Exception {
    Decoder in = DecoderFactory.get().binaryDecoder(new ByteBufferInputStream(buffers), null);
    ByteBufferOutputStream bbo = new ByteBufferOutputStream();
    BinaryEncoder out = EncoderFactory.get().binaryEncoder(bbo, null);
    RPCContext context = new RPCContext();
    List<ByteBuffer> handshake;
    Protocol remote = handshake(in, out, null);
    out.flush();
    if (remote == null) {
      // handshake failed
      return CompletableFuture.completedFuture(bbo.getBufferList());
    }
    handshake = bbo.getBufferList();

    // read request using remote protocol specification
    RPCContextHelper.setResponseCallMeta(context, AvroConstants.META_READER.read(null, in));
    String messageName = in.readString(null).toString();
    if (messageName.equals("")) {
      // a handshake ping
      return CompletableFuture.completedFuture(handshake);
    }
    Message rm = remote.getMessages().get(messageName);
    if (rm == null) {
      throw new AvroRuntimeException("No such remote message: " + messageName);
    }
    Message m = getLocal().getMessages().get(messageName);
    if (m == null) {
      throw new AvroRuntimeException("No message named " + messageName + " in " + getLocal());
    }

    Object request = readRequest(rm.getRequest(), m.getRequest(), in);

    context.setMessage(rm);
    for (RPCPlugin plugin : rpcMetaPlugins) {
      plugin.serverReceiveRequest(context);
    }

    List<ByteBuffer> handshakeFinal = handshake;
    CompletionStage<Object> completionStage;
    if (impl.getClass().getAnnotation(AvroClient.class) != null) {
      completionStage = (CompletionStage<Object>) respond(m, request);
    } else {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      completionStage = CompletableFuture
          .supplyAsync(() -> {
            SecurityContextHolder.getContext().setAuthentication(authentication);
            try {
              return respond(m, request);
            } catch (Throwable t) {
              throw ExceptionWrapper.wrap(t);
            } finally{
              SecurityContextHolder.clearContext();
            }
          }, executor)
          .thenCompose(result -> {
            if (result instanceof CompletionStage) {
              return (CompletionStage<Object>) result;
            } else {
              return CompletableFuture.completedFuture(result);
            }
          });
    }
    return completionStage
        .thenApply(result -> {
          RPCContextHelper.setResponse(context, result);
          try {
            processResult(bbo, out, context, m, handshakeFinal, result, null);
          } catch (Throwable t) {
            throw ExceptionWrapper.wrap(t);
          }
          return bbo.getBufferList();
        })
        .exceptionally(t -> {
          t = ExceptionWrapper.unwrap(t);
          RPCContextHelper.setError(context, (Exception) t);
          try {
            processResult(bbo, out, context, m, handshakeFinal, null, (Exception) t);
          } catch (Throwable t2) {
            throw ExceptionWrapper.wrap(t2);
          }
          return bbo.getBufferList();
        });
  }

  protected Protocol handshake(Decoder in, Encoder out, Transceiver connection) throws IOException {
    return protocolVersionResolver.resolve(this, in, out, connection);
  }

  private void processResult(ByteBufferOutputStream bbo, BinaryEncoder out, RPCContext context, Message m,
      List<ByteBuffer> handshake, Object response, Exception error) throws Exception {
    out.writeBoolean(error != null);
    if (error == null) {
      writeResponse(m.getResponse(), response, out);
    } else {
      try {
        writeError(m.getErrors(), error, out);
      } catch (AvroRuntimeException e) {
        throw error;
      }
    }
    out.flush();
    List<ByteBuffer> payload = bbo.getBufferList();

    // Grab meta-data from plugins
    RPCContextHelper.setResponsePayload(context, payload);
    for (RPCPlugin plugin : rpcMetaPlugins) {
      plugin.serverSendResponse(context);
    }
    AvroConstants.META_WRITER.write(context.responseCallMeta(), out);
    out.flush();
    // Prepend handshake and append payload
    bbo.prepend(handshake);
    bbo.append(payload);
  }
}
