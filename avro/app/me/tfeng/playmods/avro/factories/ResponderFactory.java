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

package me.tfeng.playmods.avro.factories;

import org.apache.avro.Protocol;
import org.apache.avro.specific.SpecificData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import me.tfeng.playmods.avro.AsyncResponder;
import me.tfeng.playmods.avro.AvroComponent;
import me.tfeng.playmods.avro.HandshakingProtocolVersionResolver;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
@Component("play-mods.avro.responder-factory")
public class ResponderFactory {

  @Autowired
  @Qualifier("play-mods.avro.component")
  private AvroComponent avroComponent;

  @Autowired
  @Qualifier("play-mods.avro.protocol-version-resolver")
  private HandshakingProtocolVersionResolver protocolVersionResolver;

  public AsyncResponder create(Class<?> iface, Object impl) {
    return new AsyncResponder(iface, impl, avroComponent.getExecutionContext(), protocolVersionResolver);
  }

  public AsyncResponder create(Class<?> iface, Object impl, SpecificData data) {
    return new AsyncResponder(iface, impl, data, avroComponent.getExecutionContext(), protocolVersionResolver);
  }

  public AsyncResponder create(Protocol protocol, Object impl) {
    return new AsyncResponder(protocol, impl, avroComponent.getExecutionContext(), protocolVersionResolver);
  }

  public AsyncResponder create(Protocol protocol, Object impl, SpecificData data) {
    return new AsyncResponder(protocol, impl, data, avroComponent.getExecutionContext(), protocolVersionResolver);
  }
}
