package anynpo.app.shared.model;

import com.google.web.bindery.autobean.shared.Splittable;

import java.lang.Integer;
import java.lang.String;
import java.util.List;
import java.util.Map;

public interface Change {
  Integer getSeq();

  void setSeq(Integer seq);

  Integer getLast_seq();

  void setLast_seq(Integer last_seq);

  String getId();

  void setId(String id);

  List<Map<String, Splittable>> getChanges();

  void setChanges(List<Map<String, Splittable>> changes);

  Splittable getDoc();

  void setDoc(Splittable doc);
}
