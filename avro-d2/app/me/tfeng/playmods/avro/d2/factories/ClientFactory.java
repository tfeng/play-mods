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

package me.tfeng.playmods.avro.d2.factories;

import org.apache.avro.Protocol;
import org.apache.avro.specific.SpecificData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import me.tfeng.playmods.avro.AvroHelper;
import me.tfeng.playmods.avro.d2.AvroD2Client;
import me.tfeng.playmods.avro.d2.AvroD2Component;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
@Component("play-mods.avro-d2.client-factory")
public class ClientFactory {

  @Autowired
  @Qualifier("play-mods.avro-d2.component")
  private AvroD2Component avroD2Component;

  @Autowired
  @Qualifier("play-mods.avro-d2.requestor-factory")
  private RequestorFactory requestorFactory;

  @Autowired
  @Qualifier("play-mods.avro-d2.transceiver-factory")
  private TransceiverFactory transceiverFactory;

  public AvroD2Client create(Class<?> interfaceClass, SpecificData data, boolean useGenericRecord) {
    return create(AvroHelper.getProtocol(interfaceClass), data, useGenericRecord);
  }

  public AvroD2Client create(Protocol protocol, SpecificData data, boolean useGenericRecord) {
    return new AvroD2Client(protocol, data, requestorFactory, transceiverFactory, avroD2Component.getZooKeeper(),
        avroD2Component.getScheduler(), avroD2Component.getClientRefreshRetryDelay(), useGenericRecord);
  }
}
