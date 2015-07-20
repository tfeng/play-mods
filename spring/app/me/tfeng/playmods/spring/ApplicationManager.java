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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import me.tfeng.playmods.common.DependencyUtils;
import play.Play;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
@Component("play-mods.spring.application-manager")
public class ApplicationManager implements ApplicationContextAware {

  private static class DependencyComparator implements Comparator<Entry<String, Startable>> {

    private final BeanDefinitionRegistry registry;

    DependencyComparator(BeanDefinitionRegistry registry) {
      this.registry = registry;
    }

    @Override
    public int compare(Entry<String, Startable> bean1, Entry<String, Startable> bean2) {
      String beanName1 = bean1.getKey();
      String beanName2 = bean2.getKey();
      BeanDefinition beanDefinition1 = registry.getBeanDefinition(beanName1);
      BeanDefinition beanDefinition2 = registry.getBeanDefinition(beanName2);
      if (beanDefinition1 == null || beanDefinition2 == null) {
        return 0;
      } else if (ArrayUtils.contains(beanDefinition1.getDependsOn(), beanName2)) {
        return 1;
      } else if (ArrayUtils.contains(beanDefinition2.getDependsOn(), beanName1)) {
        return -1;
      } else {
        return beanName1.compareTo(beanName2);
      }
    }
  }

  public static ApplicationManager getApplicationManager() {
    return Play.application().injector().instanceOf(ApplicationManager.class);
  }

  private ApplicationContext applicationContext;

  @Autowired
  private List<StartableInterceptor> startableInterceptors;

  private List<Startable> startables;

  public ApplicationContext getApplicationContext() {
    return applicationContext;
  }

  public <T> T getBean(Class<T> type) {
    return getApplicationContext().getBean(type);
  }

  public <T> T getBean(String name, Class<T> type) {
    return getApplicationContext().getBean(name, type);
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  protected List<Startable> findStartables() {
    BeanDefinitionRegistry registry = (BeanDefinitionRegistry) getApplicationContext().getAutowireCapableBeanFactory();
    Set<Entry<String, Startable>> entries = getApplicationContext().getBeansOfType(Startable.class).entrySet();
    List<Entry<String, Startable>> sortedEntries =
        DependencyUtils.dependencySort(entries, new DependencyComparator(registry));
    return Collections.unmodifiableList(sortedEntries.stream().map(Entry::getValue).collect(Collectors.toList()));
  }

  protected void start() {
    startables = findStartables();

    startables.stream().filter(startable -> startable instanceof ExtendedStartable).forEach(startable ->
        ((ExtendedStartable) startable).beforeStart());

    startables.stream().forEach(startable -> {
      startableInterceptors.stream().forEach(interceptor -> interceptor.beginStart(startable));
      try {
        startable.onStart();
        startableInterceptors.stream().forEach(interceptor -> interceptor.endStart(startable));
      } catch (Throwable t) {
        startableInterceptors.stream().forEach(interceptor -> interceptor.failStart(startable, t));
      }
    });

    startables.stream().filter(startable -> startable instanceof ExtendedStartable).forEach(startable ->
        ((ExtendedStartable) startable).afterStart());
  }

  protected void stop() {
    for (ListIterator<Startable> iterator = startables.listIterator(startables.size()); iterator.hasPrevious();) {
      Startable startable = iterator.previous();
      if (startable instanceof ExtendedStartable) {
        ((ExtendedStartable) startable).beforeStop();
      }
    }

    for (ListIterator<Startable> iterator = startables.listIterator(startables.size()); iterator.hasPrevious();) {
      Startable startable = iterator.previous();
      startableInterceptors.stream().forEach(interceptor -> interceptor.beginStop(startable));
      try {
        startable.onStop();
        startableInterceptors.stream().forEach(interceptor -> interceptor.beginStop(startable));
      } catch (Throwable t) {
        startableInterceptors.stream().forEach(interceptor -> interceptor.failStop(startable, t));
      }
    }

    for (ListIterator<Startable> iterator = startables.listIterator(startables.size()); iterator.hasPrevious();) {
      Startable startable = iterator.previous();
      if (startable instanceof ExtendedStartable) {
        ((ExtendedStartable) startable).afterStop();
      }
    }
  }
}
