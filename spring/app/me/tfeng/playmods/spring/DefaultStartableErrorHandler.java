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

package me.tfeng.playmods.spring;

import play.Application;
import play.Logger;
import play.Logger.ALogger;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class DefaultStartableErrorHandler implements StartableErrorHandler {

  private static final ALogger LOG = Logger.of(DefaultStartableErrorHandler.class);

  @Override
  public void onStartError(Application application, Startable startable, Throwable t) {
    throw new RuntimeException("Unable to start " + startable, t);
  }

  @Override
  public void onStopError(Application application, Startable startable, Throwable t) {
    LOG.error("Unable to stop " + startable, t);
  }
}
