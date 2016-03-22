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

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class ExceptionWrapper extends RuntimeException {

  public static Throwable unwrap(Throwable t) {
    while ((t instanceof ExceptionWrapper)
        || (t instanceof CompletionException)
        || (t instanceof ExecutionException)) {
      t = t.getCause();
    }
    return t;
  }

  public static RuntimeException wrap(Throwable cause) {
    if (cause instanceof RuntimeException) {
      return (RuntimeException) cause;
    } else {
      return new ExceptionWrapper(cause);
    }
  }

  public static <T, E extends Throwable> T wrap(ThrowingSupplier<T, E> function) {
    try {
      return function.get();
    } catch (Throwable t) {
      throw wrap(t);
    }
  }

  public static <E extends Throwable> void wrap(ThrowingRunnable<E> action) {
    try {
      action.run();
    } catch (Throwable t) {
      throw wrap(t);
    }
  }

  public static <T, R, E extends Throwable> Function<T, R> wrapFunction(ThrowingFunction<T, R, E> function) {
    try {
      return t -> wrap(() -> function.apply(t));
    } catch (Throwable t) {
      throw wrap(t);
    }
  }

  public static <T, E extends Throwable> Supplier<T> wrapFunction(ThrowingSupplier<T, E> function) {
    try {
      return () -> wrap(function);
    } catch (Throwable t) {
      throw wrap(t);
    }
  }

  public static <E extends Throwable> Runnable wrapFunction(ThrowingRunnable<E> action) {
    try {
      return () -> wrap(action);
    } catch (Throwable t) {
      throw wrap(t);
    }
  }

  private ExceptionWrapper(Throwable cause) {
    super(cause);
  }
}
