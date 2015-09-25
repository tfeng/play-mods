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

package me.tfeng.playmods.avro;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

import org.apache.avro.ipc.AsyncRequestor;
import org.apache.avro.specific.SpecificData;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import me.tfeng.playmods.avro.factories.RequestorFactory;
import me.tfeng.playmods.avro.factories.TransceiverFactory;
import me.tfeng.toolbox.spring.ApplicationManager;
import me.tfeng.toolbox.spring.Startable;
import play.Logger;
import play.Logger.ALogger;
import play.libs.Akka;
import play.libs.F.Promise;
import play.libs.HttpExecution;
import scala.concurrent.ExecutionContext;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
@Component("play-mods.avro.component")
public class AvroComponent implements Startable {

  public static final String PROTOCOL_IMPLEMENTATIONS_KEY = "play-mods.avro.protocol-implementations";

  private static final ALogger LOG = Logger.of(AvroComponent.class);

  @Autowired
  @Qualifier("play-mods.spring.application-manager")
  private ApplicationManager applicationManager;

  private ExecutionContext executionContext;

  @Value("${play-mods.avro.execution-context:akka.actor.default-dispatcher}")
  private String executionContextId;

  private Map<Class<?>, Object> protocolImplementations = Collections.emptyMap();

  @Autowired
  @Qualifier("play-mods.avro.requestor-factory")
  private RequestorFactory requestorFactory;

  @Autowired
  @Qualifier("play-mods.avro.transceiver-factory")
  private TransceiverFactory transceiverFactory;

  public <T> T client(Class<T> interfaceClass, AsyncTransceiver transceiver) {
    return client(interfaceClass, transceiver, new SpecificData(interfaceClass.getClassLoader()));
  }

  public <T> T client(Class<T> interfaceClass, AsyncTransceiver transceiver, SpecificData data) {
    try {
      AsyncRequestor requestor = requestorFactory.create(interfaceClass, transceiver, data, false);
      return interfaceClass.cast(Proxy.newProxyInstance(data.getClassLoader(),
          new Class[] { interfaceClass }, requestor));
    } catch (IOException e) {
      throw new RuntimeException("Unable to create async client", e);
    }
  }

  public <T> T client(Class<T> interfaceClass, URL url) {
    return client(interfaceClass, transceiverFactory.create(url));
  }

  public <T> T client(Class<T> interfaceClass, URL url, SpecificData data) {
    return client(interfaceClass, transceiverFactory.create(url), data);
  }

  public ExecutionContext getExecutionContext() {
    return HttpExecution.fromThread(executionContext);
  }

  public Map<Class<?>, Object> getProtocolImplementations() {
    return protocolImplementations == null ? null : Collections.unmodifiableMap(protocolImplementations);
  }

  public <T> T localClient(Class<T> interfaceClass, Object implementation) {
    Class<?> implementationClass = implementation.getClass();
    return interfaceClass.cast(Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class[] { interfaceClass },
        (proxy, method, args) -> {
          if (Promise.class.isAssignableFrom(method.getReturnType())) {
            return Promise.promise(() -> {
              Method implementationMethod = implementationClass.getMethod(method.getName(), method.getParameterTypes());
              try {
                return implementationMethod.invoke(implementation, args);
              } catch (InvocationTargetException e) {
                throw e.getTargetException();
              }
            }, executionContext);
          } else {
            try {
              return method.invoke(implementation, args);
            } catch (InvocationTargetException e) {
              throw e.getTargetException();
            }
          }
        }));
  }

  @Override
  @SuppressWarnings({"unchecked"})
  public void onStart() throws ClassNotFoundException {
    try {
      protocolImplementations = applicationManager.getBean(PROTOCOL_IMPLEMENTATIONS_KEY, Map.class);
    } catch (NoSuchBeanDefinitionException e) {
      // Ignore.
    }

    try {
      executionContext = Akka.system().dispatchers().lookup(executionContextId);
    } catch (Exception e) {
      LOG.warn("Unable to obtain execution context " + executionContextId + "; using default", e);
      executionContext = Akka.system().dispatchers().defaultGlobalDispatcher();
    }
  }

  @Override
  public void onStop() throws Throwable {
  }
}
