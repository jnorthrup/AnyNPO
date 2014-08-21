package anynpo.app.shared.model;

public interface Attachment {
  String getData();

  void setData(String data);

  String getContent_type();

  void setContent_type(String content_type);

  String getDigest();

  void setDigest(String digest);

  Boolean isStub();

  void setStub(Boolean stub);

  Long getRevpos();

  void setRevpos(Long revpos);

  Long getLength();

  void setLength(Long length);
}
