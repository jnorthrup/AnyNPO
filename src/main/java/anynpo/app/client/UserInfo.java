package anynpo.app.client;

import anynpo.app.shared.model.User;
import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Created by jim on 7/23/14.
 */
public class UserInfo implements IsSerializable, User {
  public String id;// );
  public String name;// );//:
  public String given_name;//
  public String family_name;
  public String link;// );//:
  public String picture;// );/
  public String gender;// );//

  @Override
  public String id() {
    return id;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public String given_name() {
    return given_name;
  }

  @Override
  public String family_name() {
    return family_name;
  }

  @Override
  public String link() {
    return link;
  }

  @Override
  public String picture() {
    return picture;
  }

  @Override
  public String gender() {
    return gender;
  }
}
