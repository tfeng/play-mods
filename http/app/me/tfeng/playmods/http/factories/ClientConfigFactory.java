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

package me.tfeng.playmods.http.factories;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ning.http.client.AsyncHttpClientConfigBean;

import play.Logger;
import play.Logger.ALogger;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
@Component("play-mods.http.client-config-factory")
public class ClientConfigFactory implements InitializingBean {

  private static final ALogger LOG = Logger.of(ClientConfigFactory.class);

  @Autowired(required = false)
  @Qualifier("play-mods.http.async-http-client-config")
  private AsyncHttpClientConfigBean asyncHttpClientConfig;

  @Value("${play-mods.http.compression-enforced:false}")
  private boolean compressionEnforced;

  @Value("${play-mods.http.connection-timeout:60000}")
  private int connectionTimeout;

  @Value("${play-mods.http.max-total-connections:200}")
  private int maxTotalConnections;

  @Value("${play-mods.http.request-timeout:10000}")
  private int requestTimeout;

  @Override
  public void afterPropertiesSet() throws Exception {
    if (asyncHttpClientConfig == null) {
      asyncHttpClientConfig = new AsyncHttpClientConfigBean();
      asyncHttpClientConfig.setCompressionEnforced(compressionEnforced);
      asyncHttpClientConfig.setConnectionTimeOut(connectionTimeout);
      asyncHttpClientConfig.setMaxTotalConnections(maxTotalConnections);
      asyncHttpClientConfig.setRequestTimeout(requestTimeout);
    } else {
      LOG.info("Async http client config is provided through Spring wiring; ignoring explicit properties");
    }
  }

  public AsyncHttpClientConfigBean create() {
    return asyncHttpClientConfig;
  }
}
