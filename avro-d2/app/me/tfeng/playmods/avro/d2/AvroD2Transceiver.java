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

import java.net.URL;

import org.apache.avro.Protocol;
import org.apache.avro.ipc.AsyncHttpTransceiver;

import me.tfeng.playmods.http.RequestPoster;
import scala.concurrent.ExecutionContext;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class AvroD2Transceiver extends AsyncHttpTransceiver {

  private final String remoteName;

  public AvroD2Transceiver(Protocol protocol, URL url, ExecutionContext executionContext,
      RequestPoster requestPoster) {
    super(url, executionContext, requestPoster);
    remoteName = AvroD2Helper.getUri(protocol).toString();
  }

  @Override
  public String getRemoteName() {
    return remoteName;
  }
}
