package anynpo.app.shared;

import com.google.web.bindery.autobean.shared.AutoBean;

public interface Has_rev extends Has_id {

  /**
   * couchdb record _rev;
   * 
   * @return
   */
  @AutoBean.PropertyName("_rev")
  String get_rev();

  void set_rev(String _rev);
}
