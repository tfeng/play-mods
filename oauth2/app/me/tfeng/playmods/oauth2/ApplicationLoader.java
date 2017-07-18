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

package me.tfeng.playmods.oauth2;

import java.util.Map;

import com.typesafe.config.Config;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class ApplicationLoader extends me.tfeng.playmods.spring.ApplicationLoader {

  @Override
  protected void addExtraConfiguration(Config config, Map<String, Object> extra) {
    super.addExtraConfiguration(config, extra);

    setExtraConfigurationValue(config, extra, "play.http.actionCreator", ActionCreator.class.getName());
    setExtraConfigurationValue(config, extra, "play.http.errorHandler", ErrorHandler.class.getName());
  }
}
