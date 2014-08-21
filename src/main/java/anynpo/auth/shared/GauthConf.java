package anynpo.auth.shared;

import anynpo.app.shared.model.Session;
import anynpo.app.shared.model.Session.AnynpoSession;
import anynpo.app.shared.model.Session.SessionFromGoogle;
import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBeanFactory;
import rxf.shared.KouchTx;

import java.util.List;

/**
 * Created by jim on 6/28/14.
 */
public interface GauthConf {
  interface ConfFactory extends AutoBeanFactory {
    AutoBean<GauthConf> conf();

    AutoBean<KouchTx> couchTxAutoBean();

    AutoBean<Session> sessionAutoBean();

    AutoBean<SessionFromGoogle> sessionFromGoogleAutoBean();

    AutoBean<AnynpoSession> anynpoSessionAutoBean();

  }

  GauthApp getWeb();

  void setWeb(GauthApp web);

  GauthApp getInstalled();

  void setInstalled(GauthApp installed);

  interface GauthParms {

    String getClient_secret();

    void setClient_secret(String client_secret);

    String getClient_id();

    void setClient_id(String client_id);
  }

  interface GauthApp extends GauthParms {

    String getAuth_uri();

    void setAuth_uri(String auth_uri);

    String getToken_uri();

    void setToken_uri(String token_uri);

    String getClient_email();

    void setClient_email(String client_email);

    List<String> getRedirect_uris();

    void setRedirect_uris(List<String> redirect_uris);

    String getClient_x509_cert_url();

    void setClient_x509_cert_url(String client_x509_cert_url);

    List<String> getJavascript_origins();

    void setJavascript_origins(List<String> javascript_origins);

    String getAuth_provider_x509_cert_url();

    void setAuth_provider_x509_cert_url(String auth_provider_x509_cert_url);
  }

}