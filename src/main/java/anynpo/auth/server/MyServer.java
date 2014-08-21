package anynpo.auth.server;

import anynpo.app.client.UserService;
import anynpo.app.shared.model.Session.AnynpoSession;
import anynpo.app.shared.model.Session.SessionFromGoogle;
import com.google.api.client.repackaged.com.google.common.base.Joiner;
import com.google.api.client.util.ArrayMap;
import com.google.common.io.Files;
import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBeanCodex;
import com.google.web.bindery.autobean.shared.AutoBeanUtils;
import one.xio.AsioVisitor;
import one.xio.AsioVisitor.Helper.*;
import one.xio.AsioVisitor.Impl;
import one.xio.AsyncSingletonServer.SingleThreadSingletonServer;
import one.xio.HttpMethod;
import one.xio.MimeType;
import rxf.core.Config;
import rxf.core.Errors;
import rxf.core.Rfc822HeaderState;
import rxf.core.Rfc822HeaderState.HttpRequest;
import rxf.core.Tx;
import rxf.rpc.GwtRpcVisitor;
import rxf.rpc.RelaxFactoryServerImpl;
import rxf.rpc.RpcHelper;
import rxf.shared.KouchTx;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static anynpo.auth.server.GoogleOAuth.CONF_FACTORY;
import static anynpo.auth.server.GoogleOAuth.app;
import static bbcursive.Cursive.post.rewind;
import static bbcursive.Cursive.pre.*;
import static bbcursive.std.*;
import static java.nio.channels.FileChannel.MapMode.READ_ONLY;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.charset.StandardCharsets.UTF_8;
import static one.xio.AsioVisitor.Helper.*;
import static one.xio.AsyncSingletonServer.killswitch;
import static one.xio.HttpHeaders.*;
import static one.xio.HttpStatus.$200;
import static rxf.core.CookieRfc6265Util.HttpOnly;
import static rxf.core.CookieRfc6265Util.Path;
import static rxf.core.DateHeaderParser.formatHttpHeaderDate;
import static rxf.core.Rfc822HeaderState.HttpResponse;
import static rxf.core.Rfc822HeaderState.hdr;
import static rxf.web.inf.SecureScope.SESSION;
import static rxf.web.inf.SecureScope.SSID;

/**
 * Created by jim on 8/7/14.
 */
public class MyServer {
  public static final String CONTENT_ROOT = Config.get("CONTENT_ROOT", "./");
  public static final InetSocketAddress COUCHDBADDR = new InetSocketAddress("localhost", 5984);
  public static final String COUCHDBHOSTSTRING = COUCHDBADDR.getHostString() + ':'
      + COUCHDBADDR.getPort();
  public static final String SECRETS_PATH = Config.get("SECRETS_PATH", "WEB-INF/classes");
  public static final String APPLICATION_X_WWW_FORM_URLENCODED =
      "application/x-www-form-urlencoded";

  public static final UserService USER_SERVICE = new UserServiceImpl();
  private static int c;

  public static void main(String... args) throws IOException {
    String host = args.length > 0 ? args[0] : "localhost";
    Integer port = args.length > 1 ? Integer.parseInt(args[1]) : 8888;
    AsioVisitor.FSM.setExecutorService(RpcHelper.getEXECUTOR_SERVICE());
    final ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
    serverSocketChannel.configureBlocking(false);
    serverSocketChannel.bind(new InetSocketAddress(port));

    Impl root = new Impl() {
      @Override
      public void onAccept(SelectionKey key) throws Exception {
        SocketChannel accept = serverSocketChannel.accept();
        accept.configureBlocking(false);
        RelaxFactoryServerImpl.enqueue(accept, OP_READ);
      }

      @Override
      public void onRead(SelectionKey browserKey) throws Exception {
        final Tx tx =
            Tx.acquireTx(browserKey, Content$2dLength, Transfer$2dEncoding, Location, Cookie, Host);
        if (tx.readHttpHeaders())
          tx.finishPayload(new F() {
            @Override
            public void apply(SelectionKey browserKey) throws Exception {
              HttpRequest httpRequest = tx.hdr().asRequest();
              log(httpRequest.asRequestHeaderString(), "Request " + c++);
              String pathx = httpRequest.path();

              if (pathx.matches("/app/UserService")) {

                GwtRpcVisitor impl = new GwtRpcVisitor(USER_SERVICE, tx);
                toWrite(browserKey, impl);

              } else if (pathx.matches("/app(/.*)?"))
                doSecured(browserKey, httpRequest, pathx);
              else if (pathx.matches("/auth/google([/?#].*)?"))

                performTokenExchange(browserKey, pathx);
              else
              // is actually templated auth details
              if (pathx.matches("/auth(/.*)?")) {
                doAuthTemplates(browserKey, pathx);
              } else {
                deliverStatic(browserKey, pathx);
              }
            }

          });
        else {
          browserKey.attach(tx);
        }
      }
    };
    RelaxFactoryServerImpl.enqueue(serverSocketChannel, SelectionKey.OP_ACCEPT);
    while (!killswitch.get())
      try {
        SingleThreadSingletonServer.init(root);
      } catch (Throwable e) {
        e.printStackTrace();
      }
  }

  public static void doAuthTemplates(SelectionKey browserKey, String pathx) throws IOException {
    deliverStaticTemplate(browserKey, pathx);
  }

  public static void performTokenExchange(final SelectionKey browserKey, final String pathx)
      throws Exception {
    toWrite(browserKey, park(new F() {
      @Override
      public void apply(SelectionKey key) throws Exception {
        final Map<String, String> query = Rfc822HeaderState.parseQuery(pathx);
        Map<String, String> map = new ArrayMap<String, String>() {
          {
            put("grant_type", "authorization_code");
            put("code", query.get("code"));
            put("scope", "");
            put("client_id", app.getClient_id());
            put("client_secret", app.getClient_secret());
            put("redirect_uri", GoogleOAuth.serverUrl + "/auth/google");
          }
        };
        ByteBuffer reqPayload = bb(Tx.formUrlEncode(map));
        String googleTokenUri = app.getToken_uri();
        URL url = new URL(googleTokenUri);
        int port1 = url.getPort();

        int port2 = port1 == -1 ? 443 : port1;
        AutoBean<AnynpoSession> anynpoSessionAutoBean = CONF_FACTORY.anynpoSessionAutoBean();
        Rfc822HeaderState b =
            hdr().asRequest().path(url.getPath()).method(HttpMethod.POST).headerString(Host,
                googleTokenUri.split("/+", 3)[1]).headerString(Content$2dType,
                APPLICATION_X_WWW_FORM_URLENCODED).headerString(Content$2dLength,
                str(reqPayload.limit()));

        F onSuccess = installNewSession(browserKey, anynpoSessionAutoBean.as());
        AtomicReference<String> jsonIn = new AtomicReference<>();
        F success =
            initializeAnynpoSessionFromGoogleToken(jsonIn, anynpoSessionAutoBean, onSuccess);
        Tx.fetchPostContent(reqPayload, b.asRequest(), url, success, jsonIn);

      }
    }));
  }

  public static F initializeAnynpoSessionFromGoogleToken(final AtomicReference<String> jsonIn,
      final AutoBean<AnynpoSession> anynpoSessionAutoBean, final F onSuccess) {
    return new F() {
      @Override
      public void apply(SelectionKey googleKey) throws Exception {
        String s = jsonIn.get();
        log(onSuccess, "initializeAnynpoSessionFromGoogleToken", s);
        AutoBean<SessionFromGoogle> sessionFromGoogleAutoBean = AutoBeanCodex.decode(CONF_FACTORY, SessionFromGoogle.class, jsonIn.get());
        
        AutoBeanCodex.decodeInto(AutoBeanCodex.encode(sessionFromGoogleAutoBean), anynpoSessionAutoBean);
        anynpoSessionAutoBean.as().setExpires_on(
            new Date(new Date().getTime() + 1000 * sessionFromGoogleAutoBean.as().getExpires_in()));
        ByteBuffer acSession =
            bb(AutoBeanCodex.encode(anynpoSessionAutoBean).getPayload(), debug);
        final Tx couchResTx = new Tx();
        final AnynpoSession shim = anynpoSessionAutoBean.as();

        doCouchDbDocPersist_debug("/session" /* + append */, acSession, couchResTx, new F() {
          @Override
          public void apply(SelectionKey couchKey) throws Exception {
            String couchTxJson = str(couchResTx, flip, noop);
            AutoBean<KouchTx> couchTxAutoBean =
                AutoBeanCodex.decode(CONF_FACTORY, KouchTx.class, couchTxJson);
            KouchTx kouchTx = couchTxAutoBean.as();
            assert kouchTx.getOk();
            shim.set_id(kouchTx.getId());
            shim.set_rev(kouchTx.getRev());
            bye(couchKey);
            onSuccess.apply(couchKey);
          }
        });
      }
    };
  }

  public static F installNewSession(final SelectionKey browserKey,
      final AnynpoSession anynpoSession) {
    return new F() {
      @Override
      public void apply(SelectionKey key) throws Exception {
        ByteBuffer successHtmlBuf =
            bb("<html>\n" + "<body>Authentication successful, finishing login...\n"
                + "<script>setTimeout(close, 500);\n" + "(opener || parent).location=\"/\"\n"
                + "</script>\n" + "</html></body>", debug);
        String theCookieString =
            Joiner.on(';').join("SSID=" + anynpoSession.get_id(), HttpOnly, Path + "=/");
        ByteBuffer headersToAppLogin =
            bb(hdr().asResponse().status($200).headerString(Set$2dCookie, theCookieString)//
                .headerString(Connection, "close")//
                .headerString(Content$2dLength, str(successHtmlBuf.limit()))//
                .headerString(Content$2dType, MimeType.html.contentType));
        finishWrite(browserKey, terminate(), headersToAppLogin, successHtmlBuf);
      }
    };
  }

  public static void doSecured(SelectionKey browserKey, final HttpRequest httpRequest,
      final String pathx) throws Exception {
    park(browserKey, new F() {
      @Override
      public void apply(final SelectionKey browserKey) throws Exception {
        log(pathx, " Secure: ");

        final Tx couchHeadTx = new Tx();
        String cookie = httpRequest.getCookie(SSID);
        if (null != cookie && !cookie.isEmpty() && !Objects.equals("null", cookie)) {
          doCouchDbHead('/' + SESSION + '/' + cookie, couchHeadTx, new F() {
            @Override
            public void apply(SelectionKey key) throws Exception {
              log(couchHeadTx.hdr().asResponse(), "session record");
              if ($200 == couchHeadTx.hdr().asResponse().statusEnum())
                deliverStatic(browserKey, pathx);
              else {
                Errors.$303(browserKey, "/auth");
              }
            }
          });
        } else {
          Errors.$303(browserKey, "/auth");
        }
      }
    });
  }
  public static void doCouchDbDocPersist_debug(final String path, final ByteBuffer doc,
      final Tx resultTx, final F onSuccess) throws Exception {
    log(doc,"doCouchDbDocPersist_debug");
    doCouchDbDocPersist(path, doc, resultTx, onSuccess);
  } public static void doCouchDbDocPersist(final String path, final ByteBuffer doc,
      final Tx resultTx, final F onSuccess) throws Exception {
    F doCouchDbDocPersist = new F() {
      @Override
      public void apply(SelectionKey couchKey) throws Exception {
        log(couchKey, "doCouchDbDocPersist", onSuccess.toString());
        resultTx.key(couchKey);
        Rfc822HeaderState b = hdr().asRequest().method(HttpMethod.POST)//
            .path(path).headerString(Host, COUCHDBHOSTSTRING)//
            .headerString(Content$2dType, MimeType.json.contentType)//
            .headerString(Content$2dLength, str(doc.limit()));
        finishWrite(couchKey, new F() {
          @Override
          public void apply(SelectionKey couchKey) throws Exception {
            toRead(couchKey, new F() {
              @Override
              public void apply(SelectionKey couchKey) throws Exception {
                if (resultTx.key(couchKey).readHttpHeaders()) {
                  log(onSuccess, "post-write");
                  resultTx.finishPayload(onSuccess);
                  bye(couchKey);

                }
              }
            });
          }
        }, bb(b), doc);
      }
    };
    finishConnect(COUCHDBADDR.getHostName(), COUCHDBADDR.getPort(), doCouchDbDocPersist);
  }

  public static void doCouchDbHead(final String path1, final Tx couchHeadTx, final F onSuccess)
      throws Exception {
    SocketChannel open = (SocketChannel) SocketChannel.open().configureBlocking(false);
    open.connect(COUCHDBADDR);
    finishConnect(open, new F() {
      @Override
      public void apply(SelectionKey couchKey) throws Exception {
        couchHeadTx.key(couchKey, ETag).noPayload(true);
        finishWrite(couchKey, new F() {
          @Override
          public void apply(final SelectionKey couchKey) throws Exception {
            toRead(couchKey, new F() {
              @Override
              public void apply(SelectionKey key) throws Exception {
                if (couchHeadTx.key(couchKey).readHttpHeaders()) {
                  terminate(onSuccess).apply(key);
                }
              }
            });
          }
        }, bb(hdr()//
            .asRequest()//
            .path(path1)//
            .method(HttpMethod.HEAD)//
            .headerString(Host, "localhost:5984")//
            .headerString(Accept, MimeType.json.contentType)//
            .asRequestHeaderString(), debug));
      }
    });
  }

  public static void doCouchDbFetchDoc(final Tx txResult, final F onSuccess, final String path1)
      throws Exception {
    SocketChannel open = (SocketChannel) SocketChannel.open().configureBlocking(false);
    open.connect(COUCHDBADDR);
    finishConnect(open, new F() {
      @Override
      public void apply(final SelectionKey couchKey) throws Exception {
        txResult.key(couchKey).noPayload(false);
        finishWrite(couchKey, new F() {
          @Override
          public void apply(SelectionKey key) throws Exception {
            toRead(couchKey, new F() {
              @Override
              public void apply(SelectionKey key) throws Exception {
                if (txResult.key(couchKey).readHttpHeaders()) {
                  txResult.finishPayload(terminate(onSuccess));
                }
              }
            });
          }
        }, bb(hdr()//
            .asRequest().path(path1)//
            .method(HttpMethod.GET)//
            .headerString(Host, "localhost:5984")//
            .headerString(Accept, MimeType.json.contentType)//
            .asRequestHeaderString(), debug));

      }
    });
  }

  public static void deliverStatic(SelectionKey key, String pathx) throws IOException {
    String fn = pathx.replaceAll("[.]+", ".").split("[?#]", 2)[0];
    Path path = Paths.get(CONTENT_ROOT, fn);

    File file = path.toFile();
    if (file.isDirectory()) {
      log("adding index.html");
      path = Paths.get(CONTENT_ROOT, fn, "index.html");
      file = path.toFile();
    }
    java.nio.file.Path other = Paths.get(CONTENT_ROOT, "WEB-INF");
    if (!path.startsWith(other))
      if (file.canRead() && file.isFile()) {
        sendFile(key, path);
        return;
      }
    Errors.$404(key, pathx);
  }

  public static void deliverStaticTemplate(SelectionKey key, String pathx) throws IOException {
    String fn = pathx.replaceAll("[.]+", ".").split("[?#]", 2)[0];
    Path path = Paths.get(CONTENT_ROOT, fn);

    File file = path.toFile();
    if (file.isDirectory()) {
      log("adding index.html");
      path = Paths.get(CONTENT_ROOT, fn, "index.html");
      file = path.toFile();
    }
    if (file.canRead() && file.isFile()) {
      String s = Files.toString(file, UTF_8);
      ByteBuffer payload = bb(doTemplateReplace(s));
      finishWrite(key, terminate(), bb(hdr()//
          .asResponse()//
          .status($200)//
          .headerString(Content$2dLength, String.valueOf(payload.limit()))), payload);//
    }
  }

  public static String doTemplateReplace(String src) {
    Map<String, Object> allProperties =
        AutoBeanUtils.getAllProperties(AutoBeanUtils.getAutoBean(app));
    Matcher matcher = Pattern.compile(":-@([^@]+)@-:").matcher(src);
    Set<String> objects = new LinkedHashSet<>();

    while (matcher.find()) {
      objects.add(matcher.group(1));
    }
    for (String object : objects) {
      try {
        String key = ":-@" + object + "@-:";
        String value = String.valueOf(allProperties.get(object));
        System.err.println("-@- replacing " + key + " :: " + value);
        src = src.replace(key, value);
      } catch (Exception e) {
        throw new Error("invalid token or app data");
      }
    }
    return src;
  }

  public static void sendFile(SelectionKey key, Path path) throws IOException {
    File file;
    file = path.toFile();
    log("resolved " + file);

    String mimeType = java.nio.file.Files.probeContentType(path);
    HttpResponse res = hdr().asResponse();
    RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
    long length = randomAccessFile.length();

    res.status($200).headerString(Content$2dType,
        null == mimeType ? MimeType.bin.contentType : mimeType).headerString(Content$2dLength,
        String.valueOf(length)).headerString(Connection, "close").headerString(Date,
        formatHttpHeaderDate(new Date(file.lastModified())));
    if (null != mimeType)
      res.headerString(Content$2dEncoding, mimeType);

    try {

      FileChannel channel = randomAccessFile.getChannel();
      MappedByteBuffer map = channel.map(READ_ONLY, 0, length);

      ByteBuffer fileContent = bb(map, rewind);
      ByteBuffer headers = bb(res.asResponseHeaderString(), rewind);
      finishWriteSeq(key, new F() {
        @Override
        public void apply(SelectionKey key) throws Exception {
          key.channel().close();
        }
      }, headers, fileContent);
      try {
        // closing the origins of a mmap file is specified to have no effect on the mmap itself.
        channel.close();
        randomAccessFile.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
