package me.tfeng.playmods.avro;

import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;

import com.google.common.collect.Maps;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class IpcContextHolder {

  private static final ThreadLocal<Map<String, Object>> CONTEXT_HOLDER =
      ThreadLocal.withInitial(new Supplier<Map<String, Object>>() {
        @Override
        public Map<String, Object> get() {
          return Maps.newHashMap();
        }
      });

  public static void clearContext() {
    CONTEXT_HOLDER.remove();
  }

  public static Map<String, Object> getContext() {
    return Collections.unmodifiableMap(CONTEXT_HOLDER.get());
  }

  public static void setContext(Map<String, Object> context) {
    CONTEXT_HOLDER.set(context);
  }
}
