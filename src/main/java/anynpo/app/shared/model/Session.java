package anynpo.app.shared.model;

import anynpo.app.shared.Has_id;
import anynpo.app.shared.Has_rev;

import java.util.Date;

/**
 * Created by jim on 7/1/14.
 */
public interface Session {
  String getAccess_token();// "ya29.NQCQ7fmr5wFNzRoAAAAirvXMrDzq9RdSasrbOlzubGWK5Q_YwKqJR-FjZb4bzQ",

  String getToken_type();// "Bearer",

  interface SessionFromGoogle extends Session {
    Long getExpires_in();// 3600,
  }

  interface AnynpoSession extends Session, Has_id, Has_rev {
    Date getExpires_on();

    void setExpires_on(Date end);
  }

  String getId_token();// "eyJhbGciOiJSUzI1NiIsImtpZCI6IjUxMDdmYTJmNTM0Y2FlNWFlNGU1MDdmMjEyYzgzMGU0OWU5M2YxNmMifQ.eyJpc3MiOiJhY2NvdW50cy5nb29nbGUuY29tIiwiaWQiOiIxMDkxOTEzOTgzNDg5MjAyNzYwNTkiLCJzdWIiOiIxMDkxOTEzOTgzNDg5MjAyNzYwNTkiLCJhenAiOiI5MjQyNDAwNjM2NTUtYnA1bTVhNTluMjFraGh0Njc2NTU5bWZ1dmU2dGtjc2cuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJhdF9oYXNoIjoiT1lnS1F0RmxyMTUxaEdXNXRaenBXdyIsImF1ZCI6IjkyNDI0MDA2MzY1NS1icDVtNWE1OW4yMWtoaHQ2NzY1NTltZnV2ZTZ0a2NzZy5hcHBzLmdvb2dsZXVzZXJjb250ZW50LmNvbSIsInRva2VuX2hhc2giOiJPWWdLUXRGbHIxNTFoR1c1dFp6cFd3IiwiY2lkIjoiOTI0MjQwMDYzNjU1LWJwNW01YTU5bjIxa2hodDY3NjU1OW1mdXZlNnRrY3NnLmFwcHMuZ29vZ2xldXNlcmNvbnRlbnQuY29tIiwiaWF0IjoxNDA0MjQxMTMyLCJleHAiOjE0MDQyNDUwMzJ9.wpX-V6kYv7uCVYsBdRm1C6KQyBqLBzGCfK1vZ7kVTYdUvrSLIW99C-gbjdljlYiR4lDXF4B9amu9eRdYwLOVvv-yr_s1LQ74JadVIh52BIZRUghTzDnab98eUnM4lK6U4kLdIHx7loJh1qQn6jY__GtryyW-PElkV4_0bgEgE5Q",

  String getRefresh_token();// "1/fnfR_VLm_rqLVgqnuQLe-PdZFheol-foyGOqmfp266c"

}
