package anynpo.auth.server;

import anynpo.auth.shared.GauthConf;
import anynpo.auth.shared.GauthConf.ConfFactory;
import anynpo.auth.shared.GauthConf.GauthApp;
import com.google.common.io.CharStreams;
import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBeanCodex;
import com.google.web.bindery.autobean.vm.AutoBeanFactorySource;
import rxf.core.Config;
import rxf.shared.KeepMatcher;
import rxf.shared.PreRead;
import rxf.web.inf.ContentRootImpl;

import java.io.*;
import java.nio.file.Paths;

/**
 * Created by jim on 6/17/14.
 */
@PreRead
@KeepMatcher
public class GoogleOAuth extends ContentRootImpl {

  static String serverUrl = Config.get("APP_ROOT", "http://localhost:8888");
  /**
   * while debugging intellij, set this to "WEB-INF/classes"
   */
  static final String RXF_SECRETS_PATH = Config.get("RXF_SECRETS_PATH", null);
  static GauthApp app;

  public static final ConfFactory CONF_FACTORY = AutoBeanFactorySource.create(ConfFactory.class);

  static {
    try {
      try (InputStream resourceAsStream =
          RXF_SECRETS_PATH == null ? GoogleOauthToken.class
              .getResourceAsStream("client_secrets.json") : new FileInputStream(Paths.get(
              RXF_SECRETS_PATH, "client_secrets.json").toFile());
          Reader bufferedReader = new InputStreamReader(resourceAsStream)) {
        // GauthConf gauthConf = gson().fromJson(bufferedReader, GauthConf.class);
        AutoBean<GauthConf> decode =
            AutoBeanCodex.decode(CONF_FACTORY, GauthConf.class, CharStreams
                .toString(bufferedReader));
        GauthConf gauthConf = decode.as();
        app = gauthConf.getInstalled() == null ? gauthConf.getWeb() : gauthConf.getInstalled();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static final char SEPARATOR = '&';
  public static final String ASSIGNMENT_OPERATOR = "=";

}
