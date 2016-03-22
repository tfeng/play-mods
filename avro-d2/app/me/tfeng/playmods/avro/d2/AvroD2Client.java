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

package me.tfeng.playmods.avro.d2;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.avro.Protocol;
import org.apache.avro.ipc.AsyncRequestor;
import org.apache.avro.specific.SpecificData;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

import com.google.common.collect.Lists;

import me.tfeng.playmods.avro.d2.factories.RequestorFactory;
import me.tfeng.playmods.avro.d2.factories.TransceiverFactory;
import me.tfeng.toolbox.common.Constants;
import play.Logger;
import play.Logger.ALogger;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class AvroD2Client implements Watcher, InvocationHandler {

  private static final ALogger LOG = Logger.of(AvroD2Client.class);

  private final long clientRefreshRetryDelay;

  private final SpecificData data;

  private volatile boolean isVersionRegistered;

  private volatile int lastIndex = -1;

  private final Protocol protocol;

  private volatile boolean refreshed;

  private final RequestorFactory requestorFactory;

  private final ScheduledExecutorService scheduler;

  private final List<URL> serverUrls = Lists.newArrayList();

  private final TransceiverFactory transceiverFactory;

  private final boolean useGenericRecord;

  private final ZooKeeperProvider zkProvider;

  public AvroD2Client(Protocol protocol, SpecificData data, RequestorFactory requestorFactory,
      TransceiverFactory transceiverFactory, ZooKeeperProvider zkProvider, ScheduledExecutorService scheduler,
      long clientRefreshRetryDelay, boolean useGenericRecord) {
    this.protocol = protocol;
    this.data = data;
    this.requestorFactory = requestorFactory;
    this.transceiverFactory = transceiverFactory;
    this.zkProvider = zkProvider;
    this.scheduler = scheduler;
    this.clientRefreshRetryDelay = clientRefreshRetryDelay;
    this.useGenericRecord = useGenericRecord;
  }

  public synchronized URL getNextServerUrl() {
    if (serverUrls.isEmpty()) {
      throw new RuntimeException("No server is found for " + protocol.getName());
    } else {
      lastIndex = (lastIndex + 1) % serverUrls.size();
      return serverUrls.get(lastIndex);
    }
  }

  public Protocol getProtocol() {
    return protocol;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    return setupRequest().invoke(proxy, method, args);
  }

  @Override
  public void process(WatchedEvent event) {
    refresh();
  }

  public void refresh() {
    List<String> children;
    String path = AvroD2Helper.getServersZkPath(protocol);
    try {
      children = zkProvider.getZooKeeper().getChildren(path, this);
    } catch (Exception e) {
      LOG.warn("Unable to list servers for " + protocol.getName() + "; retry later", e);
      scheduleRefresh();
      return;
    }

    synchronized(this) {
      serverUrls.clear();
      for (String child : children) {
        String childPath = path + "/" + child;
        try {
          byte[] data = zkProvider.getZooKeeper().getData(childPath, false, null);
          String serverUrl = new String(data, Constants.UTF8);
          serverUrls.add(new URL(serverUrl));
        } catch (Exception e) {
          LOG.warn("Unable to get server URL from node " + childPath, e);
        }
      }

      if (serverUrls.isEmpty()) {
        LOG.warn("Unable to get any server URL for protocol " + protocol.getName() + "; retry later");
        scheduleRefresh();
      }
    }
  }

  public CompletionStage<Object> request(String message, Object[] request) throws Exception {
    return setupRequest().request(message, request);
  }

  private void scheduleRefresh() {
    scheduler.schedule(this::refresh, clientRefreshRetryDelay, TimeUnit.MILLISECONDS);
  }

  private synchronized AsyncRequestor setupRequest() throws IOException, InterruptedException, KeeperException {
    if (!refreshed) {
      refreshed = true;
      refresh();
    }

    if (!isVersionRegistered) {
      AvroD2Helper.createVersionNode(zkProvider.getZooKeeper(), protocol);
      isVersionRegistered = true;
    }

    return requestorFactory.create(protocol, transceiverFactory.create(protocol, getNextServerUrl()), data,
        useGenericRecord);
  }
}
