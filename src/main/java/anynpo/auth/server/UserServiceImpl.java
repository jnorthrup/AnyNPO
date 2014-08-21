package anynpo.auth.server;

import anynpo.app.client.UserInfo;
import anynpo.app.client.UserService;
import anynpo.app.shared.model.Session.AnynpoSession;
import com.google.api.client.util.ArrayMap;
import com.google.gson.Gson;
import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBeanCodex;
import one.xio.AsioVisitor;
import one.xio.HttpMethod;
import one.xio.MimeType;
import rxf.core.Rfc822HeaderState;
import rxf.core.Rfc822HeaderState.HttpRequest;
import rxf.core.Tx;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicReference;

import static anynpo.auth.server.GoogleOAuth.CONF_FACTORY;
import static anynpo.auth.server.GoogleOAuth.app;
import static anynpo.auth.server.MyServer.initializeAnynpoSessionFromGoogleToken;
import static bbcursive.Cursive.post.rewind;
import static bbcursive.Cursive.pre.*;
import static bbcursive.std.*;
import static one.xio.AsioVisitor.Helper.*;
import static one.xio.HttpHeaders.*;
import static rxf.core.Rfc822HeaderState.hdr;

/**
 * Created by jim on 8/9/14.
 */
class UserServiceImpl implements UserService {
//  public static final String AUTH_URI = "https://www.googleapis.com/userinfo/v2/me";
  public static final String AUTH_URI = "https://www.googleapis.com/oauth2/v2/userinfo";
  final AtomicReference<String> payload = new AtomicReference<>();

  @Override
  public UserInfo getCurrentUser() {
    final Phaser phaser = new Phaser( 2);
    try {
      Tx browserTx = Tx.current();
      String ssid = browserTx.hdr().asRequest().getCookie("SSID");

      final String path = "/session/" + (ssid != null ? ssid : "");
      final AtomicReference<UserInfo> results = new AtomicReference<>();
       final Tx refreshTx = new Tx();
      try {
        MyServer.doCouchDbFetchDoc(refreshTx, new F() {
          @Override
          public void apply(SelectionKey couchKey) throws Exception {
            String str = str(refreshTx, flip, debug);
            AutoBean<AnynpoSession> anynpoSessionAutoBean =
                AutoBeanCodex.decode(CONF_FACTORY, AnynpoSession.class, str);
            final AnynpoSession anynpoSession = anynpoSessionAutoBean.as();
            F deliver = new F() {
              @Override
              public void apply(SelectionKey key) throws Exception {
                URI uri = new URI(AUTH_URI);
                HttpRequest authorization =     null;
                try {
                  authorization = hdr().asRequest().path(uri.getPath()).method(HttpMethod.GET)
                      .headerString(Accept, MimeType.json.contentType)
                      .headerString(Host,
                          uri.getHost()).headerString("Authorization",
                          "Bearer " + anynpoSession.getAccess_token()
                      ).asRequest();
                } catch (Exception e) {
                  e.printStackTrace();
                }

                Tx.fetchGetContent(uri.toURL(), authorization, payload,isolate( new F() {
                  @Override
                  public void apply(SelectionKey key) throws Exception {
                    // String str1 = str(googleTx, rewind, debug);
                    String json = payload.get();
                    UserInfo userInfo = new Gson().fromJson(json, UserInfo.class);
                    results.set(userInfo);
                    assert AsioVisitor.FSM.selectorThread!=Thread.currentThread():"must wrap in executor thread";
                    phaser.arrive();
                  }
                }));
              }
            };
            if (anynpoSession.getExpires_on().before(new Date())) {

                refreshToken(anynpoSessionAutoBean, deliver);

            } else
               deliver.apply(couchKey);
          }

        }, path);
      } catch (Exception e1) {
        e1.printStackTrace();
      }

        assert AsioVisitor.FSM.selectorThread!=Thread.currentThread():"must wrap in executor thread";
        phaser.awaitAdvanceInterruptibly(phaser.arrive(), REALTIME_CUTOFF,REALTIME_UNIT);

      return results.get();
    } catch ( Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * assuming we have an expired session token -- we fetch a new one and re-write the data and do the usual update rev,
   * id, etc.
   * 
   * @param anynpoSessionAutoBean
   * @param deliver
   * @throws Exception
   */
  public void refreshToken(final AutoBean<AnynpoSession> anynpoSessionAutoBean, final F deliver)
      throws Exception {
    log(deliver, "refreshToken");
    String token_uri = app.getToken_uri();
    URI url = null;
    try {
      url = new URI(token_uri);
    } catch (URISyntaxException e) {

    }
    final AnynpoSession anynpoSession1 = anynpoSessionAutoBean.as();

    Map<String, String> map = new ArrayMap<String, String>() {
      {
        // put("token_uri", app.token_uri);
        put("client_secret", app.getClient_secret());
        put("grant_type", "refresh_token");
        put("refresh_token", anynpoSession1.getRefresh_token());
        put("client_id", app.getClient_id());
      }
    };
    String s = Tx.formUrlEncode(map);
    final ByteBuffer formurlencodePayload = bb(s, rewind, debug);
    assert s.equals(str(formurlencodePayload, duplicate));

    final URI finalUrl = url;

    final Tx sessionObjectTx = new Tx();

    try {
      Rfc822HeaderState b =
          hdr().asRequest().method(HttpMethod.POST).path(url.getPath()).headerString(
              Content$2dLength, String.valueOf(formurlencodePayload.limit())).headerString(Host,
              finalUrl.getHost()).headerString(Content$2dType, "application/x-www-form-urlencoded");
      Tx.fetchPostContent(formurlencodePayload, b.asRequest(), finalUrl.toURL(), new F() {
        @Override
        public void apply(SelectionKey key) throws Exception {
          String path1 = "/session";// + id + "?rev=" + rev;

          String jsonIn = AutoBeanCodex.encode(anynpoSessionAutoBean).getPayload();

          /*F onSuccess =*/
              initializeAnynpoSessionFromGoogleToken(payload, anynpoSessionAutoBean, deliver).apply(key);
//          doCouchDbDocPersist(path1, bb(jsonIn, debug), sessionObjectTx, onSuccess);

        }
      }, payload);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
