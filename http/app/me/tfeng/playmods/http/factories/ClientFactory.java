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
import org.springframework.stereotype.Component;

import com.ning.http.client.AsyncHttpClient;

import me.tfeng.toolbox.spring.Startable;
import play.Logger;
import play.Logger.ALogger;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
@Component("play-mods.http.client-factory")
public class ClientFactory implements InitializingBean, Startable {

  private static final ALogger LOG = Logger.of(ClientFactory.class);

  @Autowired(required = false)
  @Qualifier("play-mods.http.async-http-client")
  private AsyncHttpClient client;

  @Autowired
  @Qualifier("play-mods.http.client-config-factory")
  private ClientConfigFactory clientConfigFactory;

  @Override
  public void afterPropertiesSet() throws Exception {
    if (client == null) {
      client = new AsyncHttpClient(clientConfigFactory.create());
    } else {
      LOG.info("Async http client is provided through Spring wiring; ignoring configuration");
    }
  }

  public AsyncHttpClient create() {
    return client;
  }

  @Override
  public void onStart() throws Throwable {
  }

  @Override
  public void onStop() throws Throwable {
    client.close();
  }
}
