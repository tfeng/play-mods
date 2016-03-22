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

package me.tfeng.playmods.spring;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import play.Configuration;
import play.inject.guice.GuiceApplicationBuilder;
import play.inject.guice.GuiceApplicationLoader;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class ApplicationLoader extends GuiceApplicationLoader {

  @Override
  public GuiceApplicationBuilder builder(Context context) {
    Map<String, Object> extra = Maps.newHashMap();
    addExtraConfiguration(context.initialConfiguration(), extra);
    return initialBuilder
        .in(context.environment())
        .loadConfig(new Configuration(extra).withFallback(context.initialConfiguration()))
        .overrides(overrides(context));
  }

  protected void addExtraConfiguration(Configuration initialConfig, Map<String, Object> extra) {
    setExtraConfigurationValue(initialConfig, extra, "application.global", Global.class.getName());
    setExtraConfigurationValue(initialConfig, extra, "play.http.errorHandler", ErrorHandler.class.getName());
    addToExtraConfigurationList(initialConfig, extra, "play.modules.enabled", Module.class.getName());
  }

  protected void addToExtraConfigurationList(Configuration initialConfig, Map<String, Object> extra, String key,
      Object value) {
    List<Object> list = null;
    try {
      list = initialConfig.getList(key);
    } catch (Throwable t) {
    }
    if (list == null) {
      list = Collections.emptyList();
    }

    if (!list.contains(value)) {
      List<Object> newList = Lists.newArrayListWithCapacity(list.size() + 1);
      newList.addAll(list);
      newList.add(value);
      extra.put(key, newList);
    }
  }

  protected void setExtraConfigurationValue(Configuration initialConfig, Map<String, Object> extra, String key,
      Object value) {
    try {
      if (initialConfig.getConfig(key) == null) {
        extra.put(key, value);
      }
    } catch (Throwable t) {
      extra.put(key, value);
    }
  }
}
