package anynpo.app.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window.Location;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;

import java.lang.Exception;

/**
 * Created by jim on 6/7/14.
 */
public class App implements EntryPoint {
  public static final RootPanel WIDGETS = RootPanel.get();

  public void onModuleLoad() {
    com.google.gwt.dom.client.Document.get().getBody().removeAllChildren();
    WIDGETS.add(new VerticalPanel() {
      {
        add(new FlowPanel() {
          {
            try {
              UserService.App.getInstance().getCurrentUser(new AsyncCallback<UserInfo>() {
                @Override
                public void onFailure(Throwable caught) {

                }

                @Override
                public void onSuccess(final UserInfo result) {
                  add(new HorizontalPanel() {
                    {
                      add(new HTML("Welcome " + result.name()));
                      add(new Image(result.picture()));
                    }
                  });

                }
              });
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        });
        add(new VerticalPanel() {
          {
            add(new Label("welcome to Anynpo"));
            add(new Anchor("Register your NPO") {
              {
                addClickHandler(new ClickHandler() {
                  @Override
                  public void onClick(ClickEvent clickEvent) {
                    WIDGETS.clear();
                    WIDGETS.add(new VerticalPanel() {
                      {
                        add(new Label("Please enter the following information:"));

                        add(new HorizontalPanel() {
                          {
                            add(new Label("NPO Name"));
                            add(new TextBox());
                          }
                        });
                        add(new Button("done") {
                          {
                            addClickHandler(new ClickHandler() {
                              @Override
                              public void onClick(ClickEvent clickEvent) {
                                Location.replace("/");
                              }
                            });
                          }
                        });
                      }
                    });

                  }
                });
              }
            });
          }
        });
      }
    });

  }
}
