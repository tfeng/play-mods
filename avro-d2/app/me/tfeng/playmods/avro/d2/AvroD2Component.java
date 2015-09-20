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

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.avro.Protocol;
import org.apache.avro.specific.SpecificData;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import me.tfeng.playmods.avro.d2.factories.ClientFactory;
import me.tfeng.toolbox.avro.AvroHelper;
import me.tfeng.toolbox.spring.ApplicationManager;
import me.tfeng.toolbox.spring.ExtendedStartable;
import play.Logger;
import play.Logger.ALogger;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
@Component("play-mods.avro-d2.component")
public class AvroD2Component implements ExtendedStartable, InitializingBean, Watcher, ZooKeeperProvider {

  public static final String PROTOCOL_PATHS_KEY = "play-mods.avro-d2.protocol-paths";

  private static final ALogger LOG = Logger.of(AvroD2Component.class);

  @Autowired
  @Qualifier("play-mods.spring.application-manager")
  private ApplicationManager applicationManager;

  @Autowired
  @Qualifier("play-mods.avro-d2.client-factory")
  private ClientFactory clientFactory;

  @Value("${play-mods.avro-d2.client-refresh-retry-delay:1000}")
  private long clientRefreshRetryDelay;

  private boolean expired;

  private Map<Class<?>, String> protocolPaths;

  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

  @Value("${play-mods.avro-d2.server-host}")
  private String serverHost;

  @Value("${play-mods.avro-d2.server-port}")
  private int serverPort;

  @Value("${play-mods.avro-d2.server-register-retry-delay:1000}")
  private long serverRegisterRetryDelay;

  private List<AvroD2Server> servers;

  private ZooKeeper zk;

  @Value("${play-mods.avro-d2.zk-connect-string}")
  private String zkConnectString;

  @Value("${play-mods.avro-d2.zk-session-timeout:10000}")
  private int zkSessionTimeout;

  @Override
  public void afterPropertiesSet() throws Exception {
    try {
      protocolPaths = applicationManager.getBean(PROTOCOL_PATHS_KEY, Map.class);
    } catch (NoSuchBeanDefinitionException e) {
      // Ignore.
    }
  }

  @Override
  public void afterStart() {
    connect();
  }

  @Override
  public void afterStop() {
  }

  @Override
  public void beforeStart() {
  }

  @Override
  public void beforeStop() {
    stopServers();
  }

  public <T> T client(Class<T> interfaceClass) {
    return client(interfaceClass, new SpecificData(interfaceClass.getClassLoader()));
  }

  public <T> T client(Class<T> interfaceClass, SpecificData data) {
    return interfaceClass.cast(Proxy.newProxyInstance(interfaceClass.getClassLoader(),
        new Class<?>[] { interfaceClass }, clientFactory.create(interfaceClass, data, false)));
  }

  public long getClientRefreshRetryDelay() {
    return clientRefreshRetryDelay;
  }

  public ScheduledExecutorService getScheduler() {
    return scheduler;
  }

  public ZooKeeper getZooKeeper() {
    return zk;
  }

  @Override
  public void onStart() throws Throwable {
  }

  @Override
  public void onStop() throws Throwable {
  }

  @Override
  public void process(WatchedEvent event) {
    LOG.info(event.toString());
    switch (event.getState()) {
    case SyncConnected:
      if (expired) {
        expired = false;
        servers.forEach(AvroD2Server::register);
      }
      break;
    case Expired:
      expired = true;
      try {
        zk.close();
      } catch (InterruptedException e) {
        // Ignore.
      }
      connect();
    default:
    }
  }

  public void startServers() {
    servers = new ArrayList<>(protocolPaths.size());
    for (Entry<Class<?>, String> entry : protocolPaths.entrySet()) {
      Protocol protocol = AvroHelper.getProtocol(entry.getKey());
      String path = entry.getValue();
      if (!path.startsWith("/")) {
        path = "/" + path;
      }
      URL url;
      try {
        url = new URL("http", serverHost, serverPort, path);
      } catch (Exception e) {
        throw new RuntimeException("Unable to initialize server", e);
      }
      AvroD2Server server = new AvroD2Server(protocol, url, zk, scheduler, serverRegisterRetryDelay);
      server.register();
      servers.add(server);
    }
  }

  public void stopServers() {
    servers.stream().forEach(server -> {
      try {
        server.close();
      } catch (Exception e) {
        LOG.error("Unable to close server for " + server.getProtocol().getName() + " at " + server.getUrl()
            + "; ignoring");
      }
    });
    servers.clear();
  }

  protected void connect() {
    try {
      zk = new ZooKeeper(zkConnectString, zkSessionTimeout, this);
      startServers();
    } catch (IOException e) {
      if (zk != null) {
        try {
          zk.close();
        } catch (InterruptedException e1) {
          // Ignore.
        }
      }
      getScheduler().schedule(this::connect, clientRefreshRetryDelay, TimeUnit.MILLISECONDS);
      LOG.warn("Unable to connect to ZooKeeper; retry later", e);
    }
  }
}
