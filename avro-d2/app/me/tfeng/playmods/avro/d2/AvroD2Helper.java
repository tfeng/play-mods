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

package me.tfeng.playmods.avro.d2;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.xml.bind.DatatypeConverter;

import org.apache.avro.Protocol;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;

import com.google.common.base.Strings;

import me.tfeng.toolbox.common.Constants;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class AvroD2Helper {

  public static final String SCHEME = "avsd";

  public static String createServerNode(ZooKeeper zk, Protocol protocol, URL serverUrl) throws InterruptedException,
      KeeperException {
    ensurePath(zk, getServersZkPath(protocol));
    return zk.create(getServersZkPath(protocol) + "/", serverUrl.toString().getBytes(), Ids.READ_ACL_UNSAFE,
        CreateMode.EPHEMERAL_SEQUENTIAL);
  }

  public static void createVersionNode(ZooKeeper zk, Protocol protocol) throws KeeperException, InterruptedException {
    String path = getVersionsZkPath(protocol);
    String md5 = DatatypeConverter.printHexBinary(protocol.getMD5());
    ensurePath(zk, path);
    try {
      zk.create(path + "/" + md5, protocol.toString().getBytes(Constants.UTF8), Ids.READ_ACL_UNSAFE,
          CreateMode.PERSISTENT);
    } catch (NodeExistsException e) {
      // Ignore.
    }
  }

  public static void ensurePath(ZooKeeper zk, String path) throws InterruptedException, KeeperException {
    int index = path.lastIndexOf("/");
    if (index > 0) {
      ensurePath(zk, path.substring(0, index));
    }
    String part = path.substring(index + 1);
    if (!part.isEmpty()) {
      try {
        zk.create(path, null, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
      } catch (NodeExistsException e) {
        // Ignore.
      }
    }
  }

  public static String getProtocolZkPath(String namespace, String name) {
    return "/protocols/" + (Strings.isNullOrEmpty(namespace) ? name : namespace + "." + name);
  }

  public static String getServersZkPath(Protocol protocol) {
    return getProtocolZkPath(protocol.getNamespace(), protocol.getName()) + "/servers";
  }

  public static URI getUri(Protocol protocol) {
    try {
      return new URI(SCHEME + ":/" + getVersionsZkPath(protocol));
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  public static String getVersionsZkPath(Protocol protocol) {
    return getProtocolZkPath(protocol.getNamespace(), protocol.getName()) + "/versions";
  }

  public static Protocol readProtocolFromZk(ZooKeeper zk, String namespace, String name, String md5)
      throws InterruptedException, KeeperException {
    String versionPath = getProtocolZkPath(namespace, name) + "/versions/" + md5;
    byte[] data = zk.getData(versionPath, false, null);
    String schema = new String(data, Constants.UTF8);
    return Protocol.parse(schema);
  }
}
