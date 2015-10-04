package me.tfeng.playmods.avro;

import org.apache.avro.AvroRuntimeException;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class RemoteInvocationException extends AvroRuntimeException {

  public RemoteInvocationException(Throwable cause) {
    super(cause);
  }

  public RemoteInvocationException(String message) {
    super(message);
  }

  public RemoteInvocationException(String message, Throwable cause) {
    super(message, cause);
  }
}
