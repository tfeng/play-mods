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

import org.springframework.context.ConfigurableApplicationContext;

import com.google.inject.Injector;

import me.tfeng.toolbox.spring.ApplicationManager;
import play.Application;
import play.GlobalSettings;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
@SuppressWarnings("deprecation")
public class Global extends GlobalSettings {

  @Override
  public void onStart(Application application) {
    ApplicationManager applicationManager = getApplicationManager(application);
    applicationManager.processInjection(this);

    Injector injector = application.injector().instanceOf(Injector.class);
    ConfigurableApplicationContext applicationContext = applicationManager.getApplicationContext();
    for (String beanName : applicationContext.getBeanDefinitionNames()) {
      Object bean = applicationContext.getBean(beanName);
      injector.injectMembers(bean);
    }

    applicationManager.start();
  }

  @Override
  public void onStop(Application application) {
    getApplicationManager(application).stop();
  }

  protected ApplicationManager getApplicationManager(Application application) {
    return application.injector().instanceOf(ApplicationManager.class);
  }
}
