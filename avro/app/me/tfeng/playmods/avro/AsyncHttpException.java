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
import java.net.URL;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class AsyncHttpException extends IOException {

  private static final long serialVersionUID = 1L;

  private final int statusCode;
  private final URL url;

  public AsyncHttpException(int statusCode, URL url) {
    super("Server returned HTTP response code " + statusCode + " at URL " + url);
    this.statusCode = statusCode;
    this.url = url;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public URL getUrl() {
    return url;
  }
}
