/**
 * Copyright 2015 Thomas Feng
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package me.tfeng.playmods.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import play.Logger;
import play.Logger.ALogger;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
@Component("play-mods.spring.logging-startable-interceptor")
public class LoggingStartableInterceptor implements StartableInterceptor {

  private static final ALogger LOG = Logger.of(LoggingStartableInterceptor.class);

  @Autowired
  @Qualifier("play-mods.spring.bean-name-registry")
  private BeanNameRegistry beanRegistry;

  @Override
  public void beginStart(Startable startable) {
    LOG.info("Starting " + getName(startable));
  }

  @Override
  public void beginStop(Startable startable) {
    LOG.info("Stopping " + getName(startable));
  }

  @Override
  public void endStart(Startable startable) {
    LOG.info("Started " + getName(startable));
  }

  @Override
  public void endStop(Startable startable) {
    LOG.info("Stopped " + getName(startable));
  }

  @Override
  public void failStart(Startable startable, Throwable t) {
    LOG.error("Unable to start " + getName(startable), t);
  }

  @Override
  public void failStop(Startable startable, Throwable t) {
    LOG.error("Unable to stop " + getName(startable), t);
  }

  private String getName(Startable startable) {
    String name = beanRegistry.getName(startable);
    return name == null ? startable.toString() : name;
  }
}
