package anynpo.app.shared;

import com.google.web.bindery.autobean.shared.Splittable;
import rxf.shared.KouchTx;

import java.lang.reflect.Type;

public enum Ddl {
  session, user, admin, npo, program, drive, wallet, ;

  @SuppressWarnings("GwtInconsistentSerializableClass")
  public DdlDriver delegate;

  public static boolean isReserved(Splittable doc) {
    return !doc.get("_id").asString().isEmpty() && '_' == doc.get("_id").asString().charAt(0);
  }

  public DdlDriver getDelegate() {
    return delegate;
  }

  public void kick() {
    delegate.kick();
  }

  public Type getSchema() {
    return delegate.getSchema();
  }

  public void provision() {
    delegate.provision();
  }

  public void wipe() {
    delegate.wipe();
  }

  public KouchTx populate() {
    return delegate.populate();
  }

}
