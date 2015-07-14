/**
 * Copyright 2015 Thomas Feng
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements.  See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership.  The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License.  You may obtain
 * a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package me.tfeng.playmods.kafka;

import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.javaapi.producer.Producer;
import kafka.producer.ProducerConfig;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
@Component("play-mods.kafka.component")
public class KafkaComponent {

  @Autowired(required = false)
  @Qualifier("play-mods.kafka.consumer-properties")
  private Properties consumerProperties;

  @Autowired(required = false)
  @Qualifier("play-mods.kafka.producer-properties")
  private Properties producerProperties;

  public ConsumerConnector createConsumerConnector() {
    if (producerProperties == null) {
      throw new RuntimeException("Consumer properties are not set.");
    }
    return Consumer.createJavaConsumerConnector(new ConsumerConfig(consumerProperties));
  }

  public <K, V> Producer<K, V> createProducer() {
    if (producerProperties == null) {
      throw new RuntimeException("Producer properties are not set.");
    }
    return new Producer<K, V>(new ProducerConfig(producerProperties));
  }
}
