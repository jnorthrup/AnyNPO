package anynpo.app.shared;

import anynpo.app.shared.model.Attachment;
import com.google.web.bindery.autobean.shared.AutoBean;

import java.util.Map;

public interface Has_attachments extends Has_rev {
  /**
   * 
   * @return
   */

  @AutoBean.PropertyName("_attachments")
  Map<String, Attachment> get_attachments();

  void set_attachments(Map<String, Attachment> attachments);
}
