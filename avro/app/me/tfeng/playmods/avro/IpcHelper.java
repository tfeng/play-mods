package me.tfeng.playmods.avro;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import play.mvc.Http.Context;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class IpcHelper {

  private static class AllContexts {

    private final Map<String, Object> ipcContext;

    private final Context playContext;

    private final SecurityContext securityContext;

    AllContexts(Context playContext, SecurityContext securityContext, Map<String, Object> ipcContext) {
      this.playContext = playContext;
      this.securityContext = securityContext;
      this.ipcContext = ipcContext;
    }
  }

  public static <T, R> Function<T, R> preserveContext(Function<T, R> function) {
    AllContexts contexts = getContexts();
    return t -> {
      AllContexts oldContexts = getContexts();
      try {
        setContexts(contexts);
        return function.apply(t);
      } finally {
        setContexts(oldContexts);
      }
    };
  }

  public static <R> Supplier<R> preserveContext(Supplier<R> supplier) {
    AllContexts contexts = getContexts();
    return () -> {
      AllContexts oldContexts = getContexts();
      try {
        setContexts(contexts);
        return supplier.get();
      } finally {
        setContexts(oldContexts);
      }
    };
  }

  private static AllContexts getContexts() {
    return new AllContexts(Context.current.get(), SecurityContextHolder.getContext(), IpcContextHolder.getContext());
  }

  private static void setContexts(AllContexts contexts) {
    Context.current.set(contexts.playContext);
    SecurityContextHolder.setContext(contexts.securityContext);
    IpcContextHolder.setContext(contexts.ipcContext);
  }
}
