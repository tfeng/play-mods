package me.tfeng.playmods.modules;

import java.io.IOException;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Type;
import org.apache.avro.specific.SpecificExceptionBase;

import com.google.common.collect.ImmutableList;

import me.tfeng.playmods.avro.ApplicationError;
import me.tfeng.playmods.avro.RemoteInvocationException;
import me.tfeng.toolbox.avro.AvroHelper;
import play.Logger;
import play.Logger.ALogger;
import play.libs.F.Promise;
import play.mvc.Http.RequestHeader;
import play.mvc.Result;
import play.mvc.Results;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class AvroIpcGlobalSettings extends SpringGlobalSettings {

  private static final ALogger LOG = Logger.of(AvroIpcGlobalSettings.class);

  @Override
  public Promise<Result> onError(RequestHeader request, Throwable t) {
    try {
      if (t instanceof RemoteInvocationException) {
        t = t.getCause();
      }
      if (t instanceof SpecificExceptionBase) {
        SpecificExceptionBase avroException = (SpecificExceptionBase) t;
        LOG.warn("Exception thrown while processing Json IPC request", t);
        Schema schema = Schema.createUnion(ImmutableList.of(Schema.create(Type.STRING), avroException.getSchema()));
        if (t instanceof ApplicationError) {
          int status = ((ApplicationError) t).getStatus();
          return Promise.pure(Results.status(status, AvroHelper.toJson(schema, avroException)));
        } else {
          return Promise.pure(Results.badRequest(AvroHelper.toJson(schema, avroException)));
        }
      }
    } catch (IOException e) {
      LOG.error("Unable to convert Avro exception", e);
    }
    return super.onError(request, t);
  }
}
