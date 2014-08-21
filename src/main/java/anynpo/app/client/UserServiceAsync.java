package anynpo.app.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Created by jim on 7/23/14.
 */
public interface UserServiceAsync {
  void getCurrentUser(AsyncCallback<UserInfo> async);
}
