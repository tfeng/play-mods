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

package me.tfeng.playmods.spark;

import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import me.tfeng.playmods.spring.Startable;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
@Component("play-mods.spark.component")
public class SparkComponent implements Startable {

  @Autowired
  @Qualifier("play-mods.spark.conf")
  private SparkConf sparkConf;

  private SparkContext sparkContext;

  public SparkConf getSparkConf() {
    return sparkConf;
  }

  public SparkContext getSparkContext() {
    return sparkContext;
  }

  public void onStart() {
    sparkContext = new SparkContext(sparkConf);
  }

  public void onStop() {
    sparkContext.stop();
  }
}
