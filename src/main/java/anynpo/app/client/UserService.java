package anynpo.app.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * Created by jim on 7/23/14.
 */
@RemoteServiceRelativePath("UserService")
public interface UserService extends RemoteService {

  UserInfo getCurrentUser();

  /**
   * Utility/Convenience class. Use UserService.App.getInstance() to access static instance of UserServiceAsync
   */
  public static class App {
    private static final UserServiceAsync ourInstance = (UserServiceAsync) GWT
        .create(UserService.class);

    public static UserServiceAsync getInstance() {
      return ourInstance;
    }
  }
}
