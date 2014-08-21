package anynpo.auth.server;

import bbcursive.Cursive;
import com.google.api.client.util.ArrayMap;
import com.google.common.io.CharStreams;
import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBeanCodex;
import one.xio.AsioVisitor.Helper.F;
import one.xio.HttpHeaders;
import one.xio.HttpMethod;
import one.xio.HttpStatus;
import one.xio.MimeType;
import rxf.core.CookieRfc6265Util;
import rxf.core.Rfc822HeaderState;
import rxf.core.Rfc822HeaderState.HttpResponse;
import rxf.core.Tx;
import rxf.rpc.RpcHelper;
import rxf.shared.KeepMatcher;
import rxf.shared.KouchTx;
import rxf.shared.PreRead;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static bbcursive.Cursive.post.rewind;
import static bbcursive.std.*;
import static one.xio.AsioVisitor.Helper.bye;
import static one.xio.AsioVisitor.Helper.finishWrite;

/**
 * Created by jim on 6/17/14.
 */
@PreRead
@KeepMatcher
public class GoogleOauthToken extends GoogleOAuth {
  @Override
  public void onWrite(final SelectionKey key) throws Exception {
    log("!!! entering " + getClass().getSimpleName());
    key.interestOps(0);
    Runnable task = new Runnable() {
      @Override
      public void run() {
        Rfc822HeaderState.HttpRequest req = getReq();
        System.err.println("" + req.asRequestHeaderString());
        final String path = req.path();

        final Map<String, String> query = Rfc822HeaderState.parseQuery(path);

        /*
         * 
         * String state = getReq().getCookie(app.getClient_id()); String state1 = query.get("state"); if
         * (!state1.equals(state)) { Errors.$401(key, "invalid oauth state passed in"); try { key.channel().close(); }
         * catch (IOException e) { e.printStackTrace(); } return; }
         */

        HttpURLConnection c = null;
        final String redirectUri = serverUrl + "/auth/google";

        Map<String, String> linkedHashMap = new ArrayMap<String, String>() {
          {
            put("grant_type", "authorization_code");
            put("code", query.get("code"));
            put("scope", "");
            put("client_id", app.getClient_id());
            put("client_secret", app.getClient_secret());
            put("redirect_uri", redirectUri);
          }
        };
        String data = Tx.formUrlEncode(linkedHashMap);

        System.err.println("" + data);
        // TODO gut this when we can instead make ssl calls via 1xio
        // todo: we can and wish we had time to!
        try {
          URLConnection urlConnection = new URL(app.getToken_uri()).openConnection();
          c = (HttpURLConnection) urlConnection;
          c.setDoOutput(true);
          c.setRequestMethod("POST");
          c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
          c.setFixedLengthStreamingMode(data.getBytes().length);
          c.getOutputStream().write(data.getBytes());
          String resultString;
          try (InputStream result = c.getInputStream()) {
            resultString = CharStreams.toString(new InputStreamReader(result));
            log(resultString, "+++ ");
            try {
              ByteBuffer bb = bb(resultString, Cursive.pre.debug);
              final Tx fromCouch = new Tx();
              Helper.finishWriteSeq(key, new F() {
                @Override
                public void apply(SelectionKey key) throws Exception {
                  if (fromCouch.key(key).readHttpHeaders())
                    fromCouch.finishPayload(new F() {
                      @Override
                      public void apply(SelectionKey key) throws Exception {
                        AutoBean<KouchTx> ctx =
                            AutoBeanCodex.decode(CONF_FACTORY, KouchTx.class, str(fromCouch
                                .payload(), rewind));
                        KouchTx couchTx = ctx.as();
                        assert couchTx.getOk();// todo: branch instead
                        String id = couchTx.getId();

                        HttpResponse httpResponse = new Rfc822HeaderState().asResponse();

                        // todo: close the login frame.
                        String actionResponse =
                            "<html>\n" + "<body>Authentication successful, finishing login...\n"
                                + "<script>setTimeout(close, 500);\n"
                                + "(opener || parent).location=\"/\"\n" + "</script>\n"
                                + "</html></body>";
                        ByteBuffer encode = StandardCharsets.UTF_8.encode(actionResponse);
                        int limit = encode.limit();
                        String theCookie = "SSID=" + id + ";"//
                            + CookieRfc6265Util.HttpOnly.name() + ';'//
                            + CookieRfc6265Util.Path + "=/";//
                        finishWrite(key, new F() {
                          public void apply(SelectionKey key) {
                            bye(key);
                          }
                        },//
                            bb(httpResponse//
                                .resCode(HttpStatus.$200)//
                                .headerString(HttpHeaders.Content$2dLength, String.valueOf(limit))//
                                .headerString(HttpHeaders.Set$2dCookie, theCookie)),//
                            encode);
                        bye(key);
                      }
                    });
                }
              }, bb(new Rfc822HeaderState().asRequest().method(HttpMethod.POST).path("/session/")
                  .headerString(HttpHeaders.Content$2dType, MimeType.json.contentType)
                  .headerString(HttpHeaders.Host, "localhost:5984").headerString(
                      HttpHeaders.Content$2dLength, String.valueOf(bb.limit()))), bb);

            } catch (Throwable e) {
              e.printStackTrace();
            }
          }
          c.disconnect();
        } catch (IOException e) {
          try (InputStream errorStream = c.getErrorStream()) {
            log(CharStreams.toString(new InputStreamReader(errorStream)), "--- ");
            bye(key);
          } catch (IOException e1) {
            e1.printStackTrace();
          }
          e.printStackTrace();
        }
      }
    };
    RpcHelper.getEXECUTOR_SERVICE().submit(task);
  }
}
