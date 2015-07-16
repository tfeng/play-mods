/**
 * Copyright 2015 Thomas Feng
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements.  See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership.  The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License.  You may obtain
 * a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package me.tfeng.playmods.dust;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public abstract class JsEngine {

  public static final String DUST_JS_NAME = "dust-core.min.js";

  private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public abstract void destroy();

  public abstract String render(String template, JsonNode data) throws Exception;

  protected String convertJsonToString(JsonNode data) throws JsonProcessingException {
    return OBJECT_MAPPER.writeValueAsString(data);
  }
}
