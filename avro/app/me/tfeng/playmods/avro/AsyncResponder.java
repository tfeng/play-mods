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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Protocol;
import org.apache.avro.Protocol.Message;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
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

import play.libs.F.Promise;
import scala.concurrent.ExecutionContext;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class AsyncResponder extends SpecificResponder {

  private static final Schema META = Schema.createMap(Schema.create(Schema.Type.BYTES));

  public static final GenericDatumReader<Map<String,ByteBuffer>> META_READER =
      new GenericDatumReader<>(META);

  public static final GenericDatumWriter<Map<String,ByteBuffer>> META_WRITER =
      new GenericDatumWriter<>(META);

  private final ExecutionContext executionContext;

  private final Object impl;

  private final ProtocolVersionResolver protocolVersionResolver;

  public AsyncResponder(Class<?> iface, Object impl, ExecutionContext executionContext,
      ProtocolVersionResolver protocolVersionResolver) {
    super(iface, impl);
    this.impl = impl;
    this.executionContext = executionContext;
    this.protocolVersionResolver = protocolVersionResolver;
  }

  public AsyncResponder(Class<?> iface, Object impl, SpecificData data,
      ExecutionContext executionContext, ProtocolVersionResolver protocolVersionResolver) {
    super(iface, impl, data);
    this.impl = impl;
    this.executionContext = executionContext;
    this.protocolVersionResolver = protocolVersionResolver;
  }

  public AsyncResponder(Protocol protocol, Object impl, ExecutionContext executionContext,
      ProtocolVersionResolver protocolVersionResolver) {
    super(protocol, impl);
    this.impl = impl;
    this.executionContext = executionContext;
    this.protocolVersionResolver = protocolVersionResolver;
  }

  public AsyncResponder(Protocol protocol, Object impl, SpecificData data,
      ExecutionContext executionContext, ProtocolVersionResolver protocolVersionResolver) {
    super(protocol, impl, data);
    this.impl = impl;
    this.executionContext = executionContext;
    this.protocolVersionResolver = protocolVersionResolver;
  }

  public Promise<List<ByteBuffer>> asyncRespond(List<ByteBuffer> buffers) throws Exception {
    Decoder in = DecoderFactory.get().binaryDecoder(new ByteBufferInputStream(buffers), null);
    ByteBufferOutputStream bbo = new ByteBufferOutputStream();
    BinaryEncoder out = EncoderFactory.get().binaryEncoder(bbo, null);
    RPCContext context = new RPCContext();
    List<ByteBuffer> handshake;
    Protocol remote = handshake(in, out, null);
    out.flush();
    if (remote == null) {
      // handshake failed
      return Promise.pure(bbo.getBufferList());
    }
    handshake = bbo.getBufferList();

    // read request using remote protocol specification
    RPCContextHelper.setResponseCallMeta(context, META_READER.read(null, in));
    String messageName = in.readString(null).toString();
    if (messageName.equals("")) {
      // a handshake ping
      return Promise.pure(handshake);
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
    if (AvroHelper.isAvroClient(impl.getClass())) {
      Promise<?> promise = (Promise<?>) respond(m, request);
      return promise.map(result -> {
          RPCContextHelper.setResponse(context, result);
          processResult(bbo, out, context, m, handshakeFinal, result, null);
          return bbo.getBufferList();
      }).recover(e -> {
        if (e instanceof Exception) {
          RPCContextHelper.setError(context, (Exception) e);
          processResult(bbo, out, context, m, handshakeFinal, null, (Exception) e);
          return bbo.getBufferList();
        } else {
          throw e;
        }
      });
    } else {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      return Promise.promise(() -> {
        Authentication currentAuthentication =
            SecurityContextHolder.getContext().getAuthentication();
        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
          Object result = respond(m, request);
          RPCContextHelper.setResponse(context, result);
          processResult(bbo, out, context, m, handshakeFinal, result, null);
        } catch (Exception e) {
          RPCContextHelper.setError(context, e);
          processResult(bbo, out, context, m, handshakeFinal, null, e);
        } finally {
          SecurityContextHolder.getContext().setAuthentication(currentAuthentication);
        }
        return bbo.getBufferList();
      }, executionContext);
    }
  }

  protected Protocol handshake(Decoder in, Encoder out, Transceiver connection) throws IOException {
    return protocolVersionResolver.resolve(this, in, out, connection);
  }

  private void processResult(ByteBufferOutputStream bbo, BinaryEncoder out, RPCContext context,
      Message m, List<ByteBuffer> handshake, Object response, Exception error) throws Exception {
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
    META_WRITER.write(context.responseCallMeta(), out);
    out.flush();
    // Prepend handshake and append payload
    bbo.prepend(handshake);
    bbo.append(payload);
  }
}
