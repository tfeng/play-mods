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

package me.tfeng.playmods.modules;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Provider;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.annotation.Order;

import com.google.common.collect.Lists;
import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.name.Names;

import play.Configuration;
import play.Environment;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */@Order
public class SpringModule implements Module {

  private static class BeanProvider<T> implements Provider<T> {

    private T bean;

    private BeanFactory beanFactory;

    private String name;

    private Class<T> type;

    public BeanProvider(BeanFactory beanFactory, Class<T> type, String name) {
      this.beanFactory = beanFactory;
      this.type = type;
      this.name = name;
    }

    @Override
    public T get() {
      if (bean == null) {
        bean = type.cast(beanFactory.getBean(name));
      }
      return bean;
    }
  }

  public static final String ACTIVE_PROFILES_KEY = "play-mods.spring.active-profiles";

  public static final String CONFIG_LOCATIONS_KEY = "play-mods.spring.config-locations";

  public static final List<String> DEFAULT_CONFIG_LOCATIONS = Collections.singletonList("classpath*:spring/**/*.xml");

  public static final String DEFAULT_PROFILES_KEY = "play-mods.spring.default-profiles";

  public static final List<String> INTERNAL_CONFIG_LOCATIONS =
      Collections.singletonList("classpath*:play-mods.spring/**/*.xml");

  private static <T> void bind(ConfigurableListableBeanFactory beanFactory, Binder binder, Class<T> type, String name) {
    if (beanFactory.getBeansOfType(type).size() == 1) {
      Provider<? extends T> provider = new BeanProvider<>(beanFactory, type, name);
      binder.bind(type).toProvider(provider);
      binder.bind(Key.get(type, Names.named(name))).toProvider(provider);

      Class<?> superclass = type.getSuperclass();
      if (!superclass.equals(Object.class)) {
        bind(beanFactory, binder, superclass, name);
      }
    }
  }

  private ConfigurableApplicationContext applicationContext;

  public SpringModule(Environment environment, Configuration configuration) {
    applicationContext = createApplicationContext(configuration);
  }

  @Override
  public void configure(Binder binder) {
    ConfigurableListableBeanFactory beanFactory = applicationContext.getBeanFactory();
    Arrays.stream(beanFactory.getBeanDefinitionNames()).forEach(name -> {
      BeanDefinition definition = beanFactory.getBeanDefinition(name);
      if (definition.isAutowireCandidate() && definition.getRole() == AbstractBeanDefinition.ROLE_APPLICATION) {
        Class<?> type = beanFactory.getType(name);
        if (!type.isInterface()) {
          bind(beanFactory, binder, type, name);
        }
      }
    });
  }

  protected ConfigurableApplicationContext createApplicationContext(Configuration configuration) {
    ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext();
    List<String> activeProfiles = configuration.getStringList(ACTIVE_PROFILES_KEY);
    List<String> defaultProfiles = configuration.getStringList(DEFAULT_PROFILES_KEY);
    if (activeProfiles != null) {
      applicationContext.getEnvironment().setActiveProfiles(activeProfiles.toArray(new String[activeProfiles.size()]));
    }
    if (defaultProfiles != null) {
      applicationContext.getEnvironment().setDefaultProfiles(
          defaultProfiles.toArray(new String[defaultProfiles.size()]));
    }
    applicationContext.setConfigLocations(getSpringConfigLocations(configuration));
    applicationContext.refresh();
    return applicationContext;
  }

  protected String[] getSpringConfigLocations(Configuration configuration) {
    List<String> springConfigLocations = Lists.newArrayList();
    springConfigLocations.addAll(INTERNAL_CONFIG_LOCATIONS);
    springConfigLocations.addAll(configuration.getStringList(CONFIG_LOCATIONS_KEY, DEFAULT_CONFIG_LOCATIONS));
    return springConfigLocations.toArray(new String[springConfigLocations.size()]);
  }
}
