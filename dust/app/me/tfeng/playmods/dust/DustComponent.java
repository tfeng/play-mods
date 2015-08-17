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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import akka.dispatch.ExecutionContexts;
import akka.dispatch.Futures;
import me.tfeng.toolbox.dust.JsEnginePool;
import me.tfeng.toolbox.spring.Startable;
import play.Logger;
import play.libs.F.Promise;
import scala.concurrent.ExecutionContextExecutorService;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
@Component("play-mods.dust.component")
public class DustComponent implements Startable {

  private static final Logger.ALogger LOG = Logger.of(DustComponent.class);

  @Autowired
  @Qualifier("play-mods.dust.engine-pool")
  private JsEnginePool enginePool;

  private volatile ExecutionContextExecutorService executionContext;

  @Value("${play-mods.dust.execution-timeout:10000}")
  private long executionTimeout;

  @Override
  public void onStart() throws Throwable {
    BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
    ThreadPoolExecutor executor = new ThreadPoolExecutor(enginePool.getSize(), enginePool.getSize(), executionTimeout,
        TimeUnit.MILLISECONDS, queue);
    executor.setRejectedExecutionHandler((runnable, threadPoolExecutor) ->
        LOG.warn("JS engine rejected a request; executor " + threadPoolExecutor));
    executionContext = ExecutionContexts.fromExecutorService(executor);
  }

  @Override
  public void onStop() throws Throwable {
  }

  public Promise<String> render(String template, JsonNode data) {
    return Promise.wrap(Futures.future(() -> enginePool.render(template, data), executionContext));
  }
}
