package anynpo.app.shared;

import java.util.List;

public interface ErrorDump {
  void setClassName(String aClass);

  void setMessage(String message);

  String getMessage();

  void setTrace(List<String> trace);

  String getClassName();

  List<String> getTrace();
}
