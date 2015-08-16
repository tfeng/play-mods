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

package me.tfeng.playmods.dust;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import akka.dispatch.ExecutionContexts;
import akka.dispatch.Futures;
import me.tfeng.toolbox.spring.Startable;
import play.Logger;
import play.libs.F.Promise;
import scala.concurrent.ExecutionContextExecutorService;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
@Component("play-mods.dust.component")
public class DustComponent implements InitializingBean, Startable {

  private static final Logger.ALogger LOG = Logger.of(DustComponent.class);

  @Autowired
  @Qualifier("play-mods.dust.asset-locator")
  private AssetLocator assetLocator;

  private EngineType engineType;

  @Value("${play-mods.dust.js-engine-type:NASHORN}")
  private String engineTypeName;

  private volatile ConcurrentLinkedQueue<JsEngine> engines;

  private volatile ExecutionContextExecutorService executionContext;

  @Value("${play-mods.dust.js-engine-pool-size:4}")
  private int jsEnginePoolSize;

  @Value("${play-mods.dust.js-engine-pool-timeout:10000}")
  private long jsEnginePoolTimeout;

  @Value("${play-mods.dust.node-path:/opt/local/bin/node}")
  private String nodePath;

  @Value("${play-mods.dust.templates-directory:templates}")
  private String templatesDirectory;

  @Override
  public void afterPropertiesSet() throws Exception {
    engineType = EngineType.valueOf(engineTypeName);
  }

  public AssetLocator getAssetLocator() {
    return assetLocator;
  }

  public String getNodePath() {
    return nodePath;
  }

  public String getTemplatesDirectory() {
    return templatesDirectory;
  }

  @Override
  public void onStart() throws Throwable {
    BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
    ThreadPoolExecutor executor = new ThreadPoolExecutor(jsEnginePoolSize, jsEnginePoolSize, jsEnginePoolTimeout,
        TimeUnit.MILLISECONDS, queue);
    executor.setRejectedExecutionHandler((runnable, threadPoolExecutor) ->
        LOG.warn("JS engine rejected a request; executor " + threadPoolExecutor));
    executionContext = ExecutionContexts.fromExecutorService(executor);

    initializeEngines();
  }

  @Override
  public void onStop() throws Throwable {
    engines.stream().forEach(JsEngine::destroy);
  }

  public Promise<String> render(String template, JsonNode data) {
    JsEngine engine = engines.poll();
    return Promise.wrap(Futures.future(() -> {
      try {
        return engine.render(template, data);
      } finally {
        engines.offer(engine);
      }
    }, executionContext));
  }

  public void switchToEngineType(EngineType engineType) throws Exception {
    this.engineType = engineType;
    initializeEngines();
  }

  private JsEngine createEngine() throws Exception {
    switch (engineType) {
    case NASHORN:
      return new NashornEngine(this);
    case NODE:
      return new NodeEngine(this);
    default:
      throw new RuntimeException("Unknown engine type " + engineType);
    }
  }

  private void initializeEngines() throws Exception {
    ConcurrentLinkedQueue engines = new ConcurrentLinkedQueue<>();
    for (int i = 0; i < jsEnginePoolSize; i++) {
      engines.offer(createEngine());
    }
    this.engines = engines;
  }
}
