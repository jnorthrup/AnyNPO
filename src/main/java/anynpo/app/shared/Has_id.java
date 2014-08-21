package anynpo.app.shared;

import com.google.web.bindery.autobean.shared.AutoBean;

public interface Has_id {
  /**
   * couchdb record id. names a new/replaced lib, model, or new experiment name when set.
   * 
   * @return
   */
  @AutoBean.PropertyName("_id")
  String get_id();

  void set_id(String _id);
}
