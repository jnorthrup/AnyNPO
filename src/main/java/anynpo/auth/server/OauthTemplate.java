package anynpo.auth.server;

import com.google.common.io.CharStreams;
import com.google.gwt.user.server.Base64Utils;
import one.xio.HttpStatus;
import rxf.core.Rfc822HeaderState;
import rxf.core.Rfc822HeaderState.HttpResponse;
import rxf.web.inf.TemplateContentImpl;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.channels.SelectionKey;
import java.security.SecureRandom;
import java.util.LinkedHashSet;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static one.xio.HttpHeaders.Cookie;
import static one.xio.HttpHeaders.Set$2dCookie;

/**
 * this replaces :-@client_id@-: with one from clients_secret.json, post-gwt-compilation.
 * <p/>
 * creates a state
 */
public class OauthTemplate extends TemplateContentImpl {

  public static final SecureRandom SECURE_RANDOM = new SecureRandom();
  public static final long MINUTES15 = TimeUnit.SECONDS.convert(15, TimeUnit.MINUTES);

  @Override
  public void setReq(Rfc822HeaderState.HttpRequest req) {

    super.setReq((Rfc822HeaderState.HttpRequest) req.addHeaderInterest(Cookie).read(
        (java.nio.ByteBuffer) req.headerBuf().rewind()));
  }

  @Override
  public void onWrite(SelectionKey key) throws Exception {
    // todo: paths
    String finalFname = fileScrub(getRootPath() + SLASHDOTSLASH + getReq().path().split("\\?")[0]);
    File file = new File(finalFname);
    if (file.isDirectory()) {
      file = new File(finalFname + "/index.html");
    }
    finalFname = file.getCanonicalPath();

    boolean send200 = file.canRead() && file.isFile();
    if (send200) {
      File tmpfile;
      if (!getContent().containsKey(file)) {
        tmpfile = File.createTempFile("rxf", ".html");
        tmpfile.deleteOnExit();
        try (FileWriter fileWriter = new FileWriter(tmpfile)) {
          fileWriter.write(doReplace(CharStreams.toString(new FileReader(file))));
        }
        getContent().put(file, tmpfile);
      }
      tmpfile = getContent().get(file);

      HttpResponse status = getReq().asResponse().status(HttpStatus.$200);
      String cookie = getReq().getCookie(GoogleOauthToken.app.getClient_id());
      if (null == cookie) {
        byte[] bytes = new byte[48];
        SECURE_RANDOM.nextBytes(bytes);
        status.headerString(Set$2dCookie, new StringBuilder(GoogleOauthToken.app.getClient_id())
            .append("=").append(Base64Utils.toBase64(bytes)).append("; Max-Age: ")
            .append(MINUTES15).toString());
      }
      sendFile(key, finalFname, tmpfile, new java.util.Date(), status, null);
    }
  }

  @Override
  public String doReplace(String src) {
    Matcher matcher = Pattern.compile(":-@([^@]+)@-:").matcher(src);
    LinkedHashSet<String> objects = new LinkedHashSet<>();

    while (matcher.find()) {
      objects.add(matcher.group(1));
    };
    for (String object : objects)
      try {
        String key = ":-@" + object + "@-:";
        String value =
            String.valueOf(GoogleOauthToken.app.getClass().getField(object).get(
                GoogleOauthToken.app));
        System.err.println("-@- replacing " + key + " :: " + value);
        src = src.replace(key, value);
      } catch (Exception e) {
        throw new Error("invalid token or app data");
      }
    return src;
  }
}
