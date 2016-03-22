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

package me.tfeng.playmods.avro.factories;

import java.io.IOException;

import org.apache.avro.Protocol;
import org.apache.avro.ipc.AsyncRequestor;
import org.apache.avro.specific.SpecificData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import me.tfeng.playmods.avro.AsyncResponseProcessor;
import me.tfeng.playmods.avro.AsyncTransceiver;
import me.tfeng.playmods.avro.AuthorizationPreservingRequestPreparer;
import me.tfeng.playmods.avro.ResponseProcessor;
import me.tfeng.playmods.http.factories.ClientConfigFactory;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
@Component("play-mods.avro.requestor-factory")
public class RequestorFactory {

  private static final ResponseProcessor RESPONSE_PROCESSOR = new AsyncResponseProcessor();

  @Autowired
  @Qualifier("play-mods.http.client-config-factory")
  private ClientConfigFactory clientConfigFactory;

  public AsyncRequestor create(Class<?> interfaceClass, AsyncTransceiver transceiver, SpecificData data,
      boolean useGenericRecord) throws IOException {
    return create(data.getProtocol(interfaceClass), transceiver, data, useGenericRecord);
  }

  public AsyncRequestor create(Protocol protocol, AsyncTransceiver transceiver, SpecificData data,
      boolean useGenericRecord) throws IOException {
    return new AsyncRequestor(protocol, transceiver, data, clientConfigFactory.create().getRequestTimeout(),
        new AuthorizationPreservingRequestPreparer(), RESPONSE_PROCESSOR, useGenericRecord);
  }
}
