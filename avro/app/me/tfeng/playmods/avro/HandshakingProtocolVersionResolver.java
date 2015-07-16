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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.avro.Protocol;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.Encoder;
import org.apache.avro.ipc.Responder;
import org.apache.avro.ipc.Transceiver;
import org.springframework.stereotype.Component;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
@Component("play-mods.avro.protocol-version-resolver")
public class HandshakingProtocolVersionResolver implements ProtocolVersionResolver {

  private static final Method HANDSHAKE_METHOD;

  static {
    try {
      HANDSHAKE_METHOD = Responder.class.getDeclaredMethod("handshake", Decoder.class, Encoder.class,
          Transceiver.class);
      HANDSHAKE_METHOD.setAccessible(true);
    } catch (NoSuchMethodException | SecurityException e) {
      throw new RuntimeException("Unable to get handshake method", e);
    }
  }

  @Override
  public Protocol resolve(Responder responder, Decoder in, Encoder out, Transceiver connection) throws IOException {
    try {
      return (Protocol) HANDSHAKE_METHOD.invoke(responder, in, out, connection);
    } catch (IllegalAccessException e) {
      throw new RuntimeException("Unable to invoke handshake in responder");
    } catch (InvocationTargetException e) {
      if (e.getTargetException() instanceof IOException) {
        throw (IOException) e.getTargetException();
      } else {
        throw new RuntimeException("Handshake in responder raised an exception", e.getTargetException());
      }
    }
  }
}
