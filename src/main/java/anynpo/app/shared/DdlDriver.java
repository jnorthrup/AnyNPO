package anynpo.app.shared;

import rxf.shared.KouchTx;

import java.lang.reflect.Type;

public interface DdlDriver {
  /**
   * kick the view generation.
   */
  void kick();

  Type getSchema();

  void provision();

  void wipe();

  KouchTx populate();
}
