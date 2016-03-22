/**
 * Copyright 2016 Thomas Feng
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package me.tfeng.playmods.avro;

import java.nio.ByteBuffer;
import java.util.Map;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.ipc.HandshakeRequest;
import org.apache.avro.ipc.HandshakeResponse;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class AvroConstants {

  public static final SpecificDatumReader<HandshakeRequest> HANDSHAKE_REQUEST_READER =
      new SpecificDatumReader<>(HandshakeRequest.class);

  public static final SpecificDatumReader<HandshakeResponse> HANDSHAKE_RESPONSE_READER =
      new SpecificDatumReader<>(HandshakeResponse.class);

  public static final SpecificDatumWriter<HandshakeResponse> HANDSHAKE_RESPONSE_WRITER =
      new SpecificDatumWriter<>(HandshakeResponse.class);

  public static final GenericDatumReader<Map<String,ByteBuffer>> META_READER =
      new GenericDatumReader<>(Schema.createMap(Schema.create(Schema.Type.BYTES)));

  public static final GenericDatumWriter<Map<String,ByteBuffer>> META_WRITER =
      new GenericDatumWriter<>(Schema.createMap(Schema.create(Schema.Type.BYTES)));
}
