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

package me.tfeng.playmods.titan;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.thinkaurelius.titan.diskstorage.BackendException;
import com.thinkaurelius.titan.diskstorage.StaticBuffer;
import com.thinkaurelius.titan.diskstorage.keycolumnvalue.StoreTransaction;
import com.thinkaurelius.titan.diskstorage.keycolumnvalue.keyvalue.KVMutation;
import com.thinkaurelius.titan.diskstorage.keycolumnvalue.keyvalue.KVQuery;
import com.thinkaurelius.titan.diskstorage.keycolumnvalue.keyvalue.KeySelector;
import com.thinkaurelius.titan.diskstorage.keycolumnvalue.keyvalue.KeyValueEntry;
import com.thinkaurelius.titan.diskstorage.keycolumnvalue.keyvalue.OrderedKeyValueStore;
import com.thinkaurelius.titan.diskstorage.util.Hex;
import com.thinkaurelius.titan.diskstorage.util.RecordIterator;
import com.thinkaurelius.titan.diskstorage.util.StaticArrayBuffer;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.bson.Document;
import org.bson.conversions.Bson;
import play.Logger;
import play.Logger.ALogger;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class MongoDbKeyValueStore implements OrderedKeyValueStore {

  private static final String ID_KEY = "_id";

  private static final ALogger LOG = Logger.of(MongoDbKeyValueStore.class);

  private static final String VALUE_KEY = "v";

  private final MongoCollection<Document> collection;

  private final String name;

  public MongoDbKeyValueStore(MongoDatabase mongoDb, String name) {
    this.name = name;
    collection = mongoDb.getCollection(name);
  }

  @Override
  public void acquireLock(StaticBuffer key, StaticBuffer expectedValue, StoreTransaction txh) throws BackendException {
    throw new UnsupportedOperationException();
  }

  public void clear() throws BackendException {
    if (LOG.isDebugEnabled()) {
      LOG.debug(name + ": clearing");
    }

    collection.deleteMany(new Document());
  }

  @Override
  public void close() throws BackendException {
  }

  @Override
  public boolean containsKey(StaticBuffer key, StoreTransaction txh) throws BackendException {
    String keyHex = convertToHex(key);

    if (LOG.isDebugEnabled()) {
      LOG.debug(name + ": checking whether key exists - " + keyHex);
    }

    return collection.find(Filters.eq(ID_KEY, keyHex)).limit(1).iterator().hasNext();
  }

  @Override
  public void delete(StaticBuffer key, StoreTransaction txh) throws BackendException {
    String keyHex = convertToHex(key);

    if (LOG.isDebugEnabled()) {
      LOG.debug(name + ": deleting key - " + keyHex);
    }

    collection.deleteOne(Filters.eq(ID_KEY, keyHex));
  }

  @Override
  public StaticBuffer get(StaticBuffer key, StoreTransaction txh) throws BackendException {
    String keyHex = convertToHex(key);

    MongoCursor<Document> iterator = collection.find(Filters.eq(ID_KEY, keyHex)).limit(1).iterator();
    if (iterator.hasNext()) {
      String valueHex = iterator.next().getString(VALUE_KEY);

      if (LOG.isDebugEnabled()) {
        LOG.debug(name + ": getting key - " + keyHex + " (value is " + valueHex + ")");
      }

      return convertFromHex(valueHex);
    } else {
      if (LOG.isDebugEnabled()) {
        LOG.debug(name + ": getting key - " + keyHex + " (value not found)");
      }

      return null;
    }
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public RecordIterator<KeyValueEntry> getSlice(KVQuery query, StoreTransaction txh) throws BackendException {
    Bson filters = Filters.and(Filters.gte(ID_KEY, convertToHex(query.getStart())),
        Filters.lt(ID_KEY, convertToHex(query.getEnd())));

    if (LOG.isDebugEnabled()) {
      LOG.debug(name + ": querying - "
          + filters.toBsonDocument(Document.class, collection.getCodecRegistry()).toString());
    }

    FindIterable<Document> iterable = collection.find(filters);
    KeySelector keySelector = query.getKeySelector();
    Iterator<Document> iterator = Iterators.filter(iterable.iterator(), document -> {
      if (keySelector.reachedLimit()) {
        return false;
      } else {
        return keySelector.include(convertFromHex(document.getString(ID_KEY)));
      }
    });

    return new RecordIterator<KeyValueEntry>() {

      @Override
      public void close() throws IOException {
      }

      @Override
      public boolean hasNext() {
        return iterator.hasNext();
      }

      @Override
      public KeyValueEntry next() {
        Document document = iterator.next();
        StaticBuffer key = convertFromHex(document.getString(ID_KEY));
        StaticBuffer value = convertFromHex(document.getString(VALUE_KEY));
        return new KeyValueEntry(key, value);
      }
    };
  }

  @Override
  public Map<KVQuery, RecordIterator<KeyValueEntry>> getSlices(List<KVQuery> queries, StoreTransaction txh)
      throws BackendException {
    Map<KVQuery, RecordIterator<KeyValueEntry>> map = Maps.newHashMapWithExpectedSize(queries.size());
    for (KVQuery query : queries) {
      map.put(query, getSlice(query, txh));
    }
    return map;
  }

  @Override
  public void insert(StaticBuffer key, StaticBuffer value, StoreTransaction txh) throws BackendException {
    String keyHex = convertToHex(key);
    String valueHex = convertToHex(value);

    if (LOG.isDebugEnabled()) {
      LOG.debug(name + ": updating - " + new Document(ImmutableMap.of(ID_KEY, keyHex, VALUE_KEY, valueHex)));
    }

    Bson filter = Filters.eq(ID_KEY, keyHex);
    Document document = new Document("$set", new Document(VALUE_KEY, valueHex));
    collection.updateOne(filter, document, new UpdateOptions().upsert(true));
  }

  public void mutate(KVMutation mutation, StoreTransaction txh) throws BackendException {
    List<String> deletions = mutation.getDeletions().stream().map(this::convertToHex).collect(Collectors.toList());

    if (LOG.isDebugEnabled()) {
      LOG.debug(name + ": deleting keys - " + Arrays.toString(deletions.toArray()));
    }

    collection.deleteMany(Filters.in(ID_KEY, deletions));
    for (KeyValueEntry entry : mutation.getAdditions()) {
      insert(entry.getKey(), entry.getValue(), txh);
    }
  }

  private StaticBuffer convertFromHex(String binary) {
    return new StaticArrayBuffer(Hex.hexToBytes(binary));
  }

  private String convertToHex(StaticBuffer buffer) {
    return Hex.bytesToHex(buffer.getBytes(0, buffer.length()));
  }
}
