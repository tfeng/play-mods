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

package me.tfeng.playmods.avro.d2;

import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Protocol;
import org.apache.avro.Schema;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.ipc.AsyncRequestor;
import org.apache.avro.ipc.HandshakeMatch;
import org.apache.avro.ipc.HandshakeResponse;
import org.apache.avro.ipc.RPCContext;
import org.apache.avro.ipc.RPCContextHelper;
import org.apache.avro.specific.SpecificExceptionBase;
import org.apache.avro.util.ByteBufferInputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;

import me.tfeng.playmods.avro.AvroConstants;
import me.tfeng.playmods.avro.ResponseProcessor;
import me.tfeng.toolbox.avro.AvroHelper;
import me.tfeng.toolbox.spring.BeanUtils;
import play.Logger;
import play.Logger.ALogger;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
@Component("play-mods.avro-d2.response-processor")
public class AvroD2ResponseProcessor implements ResponseProcessor {

  private static final ALogger LOG = Logger.of(AvroD2ResponseProcessor.class);

  @Autowired
  @Qualifier("play-mods.avro-d2.component")
  private AvroD2Component avroD2Component;

  private final Map<String, Protocol> protocolCache = Collections.synchronizedMap(Maps.newHashMap());

  @Override
  public Object process(AsyncRequestor requestor, AsyncRequestor.Request request, String message,
      List<ByteBuffer> response) throws Exception {
    ByteBufferInputStream bbi = new ByteBufferInputStream(response);
    BinaryDecoder in = DecoderFactory.get().binaryDecoder(bbi, null);

    HandshakeResponse handshake = AvroConstants.HANDSHAKE_RESPONSE_READER.read(null, in);
    Protocol localProtocol = requestor.getLocal();
    Protocol serverProtocol;
    if (handshake.getMatch() == HandshakeMatch.BOTH) {
      serverProtocol = localProtocol;
    } else {
      String serverHash = DatatypeConverter.printHexBinary(handshake.getServerHash().bytes());
      serverProtocol = protocolCache.get(serverHash);
      if (serverProtocol == null) {
        serverProtocol = AvroD2Helper.readProtocolFromZk(avroD2Component.getZooKeeper(), localProtocol.getNamespace(),
            localProtocol.getName(), serverHash);
        protocolCache.put(serverHash, serverProtocol);
      }
    }

    RPCContext context = request.getContext();
    RPCContextHelper.setResponseCallMeta(context, AvroHelper.META_READER.read(null, in));

    if (!in.readBoolean()) {
      Schema localSchema = localProtocol.getMessages().get(message).getResponse();
      Schema remoteSchema = serverProtocol.getMessages().get(message).getResponse();
      Object responseObject = requestor.getDatumReader(remoteSchema, localSchema).read(null, in);
      RPCContextHelper.setResponse(context, responseObject);
      requestor.getRPCPlugins().forEach(plugin -> plugin.clientReceiveResponse(context));
      return responseObject;
    } else {
      Schema localSchema = localProtocol.getMessages().get(message).getErrors();
      Schema remoteSchema = serverProtocol.getMessages().get(message).getErrors();
      Object error = requestor.getDatumReader(remoteSchema, localSchema).read(null, in);
      Exception exception;
      if (error instanceof SpecificExceptionBase) {
        SpecificExceptionBase specificError = (SpecificExceptionBase) error;
        Schema errorSchema = serverProtocol.getMessages().get(message).getErrors();
        String errorMessage = AvroHelper.toJson(errorSchema, error);
        Constructor<? extends SpecificExceptionBase> constructor =
            BeanUtils.findConstructor(specificError.getClass(), true, String.class);
        if (constructor != null) {
          SpecificExceptionBase newSpecificError = BeanUtils.instantiateClass(constructor, errorMessage);
          for (int i = 0; i < newSpecificError.getSchema().getFields().size(); i++) {
            newSpecificError.put(i, specificError.get(i));
          }
          exception = newSpecificError;
        } else {
          LOG.error("Unable to reconstruct Avro specific exception " + errorMessage);
          exception = (Exception) error;
        }
      } else if (error instanceof Exception) {
        exception = (Exception) error;
      } else {
        exception = new AvroRuntimeException(error.toString());
      }
      RPCContextHelper.setError(context, exception);
      requestor.getRPCPlugins().forEach(plugin -> plugin.clientReceiveResponse(context));
      throw exception;
    }
  }
}
