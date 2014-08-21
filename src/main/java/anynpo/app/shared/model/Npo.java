package anynpo.app.shared.model;

import anynpo.app.shared.Has_id;
import anynpo.app.shared.Has_rev;
import java.util.List;
import java.util.Map;

/**
 * Created by jim on 7/1/14.
 */
public interface Npo extends Has_id, Has_rev {

  String getCompanyName();

  void setCompanyName(String companyName);

  String getGetPrimaryPhone();

  void setGetPrimaryPhone(String getPrimaryPhone);

  String getGetPrimaryEmail();

  void setGetPrimaryEmail(String getPrimaryEmail);

  String getGetTaxId();

  void setGetTaxId(String getTaxId);

  List<User> getAdmins();

  void setAdmins(List<User> admins);

  List<String> getAddress();

  void setAddress(List<String> address);

  List<String> getProgramIds();

  void setProgramIds(List<String> programIds);

  Map<String, String> getContentLayouts();

  void setContentLayouts(Map<String, String> contentLayouts);
}
