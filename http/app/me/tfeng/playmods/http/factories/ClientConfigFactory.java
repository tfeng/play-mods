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

package me.tfeng.playmods.http.factories;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import play.Logger;
import play.Logger.ALogger;
import play.shaded.ahc.org.asynchttpclient.AsyncHttpClientConfig;
import play.shaded.ahc.org.asynchttpclient.DefaultAsyncHttpClientConfig;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
@Component("play-mods.http.client-config-factory")
public class ClientConfigFactory implements InitializingBean {

  private static final ALogger LOG = Logger.of(ClientConfigFactory.class);

  @Autowired(required = false)
  @Qualifier("play-mods.http.async-http-client-config")
  private AsyncHttpClientConfig asyncHttpClientConfig;

  @Value("${play-mods.http.compression-enforced:false}")
  private boolean compressionEnforced;

  @Value("${play-mods.http.connect-timeout:60000}")
  private int connectTimeout;

  @Value("${play-mods.http.max-connections:200}")
  private int maxConnections;

  @Value("${play-mods.http.request-timeout:10000}")
  private int requestTimeout;

  @Override
  public void afterPropertiesSet() throws Exception {
    if (asyncHttpClientConfig == null) {
      asyncHttpClientConfig = new DefaultAsyncHttpClientConfig.Builder()
          .setCompressionEnforced(compressionEnforced)
          .setConnectTimeout(connectTimeout)
          .setMaxConnections(maxConnections)
          .setRequestTimeout(requestTimeout)
          .build();
    } else {
      LOG.info("Async http client config is provided through Spring wiring; ignoring explicit properties");
    }
  }

  public AsyncHttpClientConfig create() {
    return asyncHttpClientConfig;
  }
}
