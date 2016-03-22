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

package me.tfeng.playmods.avro.d2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import me.tfeng.playmods.avro.AsyncResponder;
import me.tfeng.playmods.avro.BinaryIpcController;
import me.tfeng.playmods.avro.d2.factories.ResponderFactory;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
@Component("play-mods.avro-d2.binary-ipc-controller")
public class AvroD2BinaryIpcController extends BinaryIpcController {

  @Autowired
  @Qualifier("play-mods.avro-d2.responder-factory")
  private ResponderFactory responderFactory;

  @Override
  protected AsyncResponder createResponder(Class<?> protocolClass, Object implementation) {
    return responderFactory.create(protocolClass, implementation);
  }
}
