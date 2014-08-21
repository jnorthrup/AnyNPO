package anynpo.auth.client;

import com.google.common.base.Joiner;
import com.google.common.net.UrlEscapers;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * Created by jim on 5/24/14.
 */
public class Auth implements EntryPoint {
  public static Element frame;
  static private String urlPrefix = Window.Location.getProtocol() + "//"
      + Window.Location.getHost();

  public void onModuleLoad() {
    final RootPanel widgets = RootPanel.get();
    widgets.clear();
    FlowPanel working = new FlowPanel() {
      {
        add(new Button("google") {
          {

            addClickHandler(new ClickHandler() {
              private native void exportAuthSuccess() /*-{
                                                      var that = this;
                                                      if (!$wnd.authSuccess) {
                                                      $wnd.authSuccess = $entry(function (sessionId, userId, newUser) {
                                                      $wnd.location.href = anynpo.auth.client.Auth.urlPrefix + "/app"
                                                      });
                                                      }
                                                      }-*/;

              @Override
              public void onClick(ClickEvent clickEvent) {
                exportAuthSuccess();
                Window.open(getOAuthUrl(), "oauth", "");
              }
            });

          }
        });
        add(new Button("facebook"));

      }
    };
    widgets.add(working);
  }

  private String getOAuthUrl() {
    String redirect = URL.encodeQueryString(urlPrefix + "/auth/google");
    // https://accounts.google.com/o/oauth2/auth?redirect_uri=https%3A%2F%2Fdevelopers.google.com%2Foauthplayground&response_type=code&client_id=407408718192.apps.googleusercontent.com&&approval_prompt=force&access_type=offline
    // TODO pick a url based on the idServer arg
    // return
    // "https://accounts.google.com/o/oauth2/auth?scope=openid&response_type=code&redirect_uri="
    // + redirect + "&client_id=" + clientId + "&state=" + code +
    // "&hl=en&from_login=1";

    String[] scopes =
        {
            "https://www.googleapis.com/auth/plus.me", "https://www.googleapis.com/auth/appstate",
            "https://www.googleapis.com/auth/activity"};
    String[] query =
        {
            "https://accounts.google.com/o/oauth2/auth?redirect_uri=" + redirect,
            "response_type=code", "client_id=" + URL.encodeQueryString(":-@client_id@-:"),
            "approval_prompt=force", "access_type=offline",
            "state=" + Cookies.getCookie(":-@client_id@-:"),
            "scope=" + UrlEscapers.urlPathSegmentEscaper().escape(Joiner.on(" ").join(scopes))};
    return Joiner.on('&').join(query);

  }

}
