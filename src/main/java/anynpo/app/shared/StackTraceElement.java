package anynpo.app.shared;

public interface StackTraceElement {
  /**
   * Returns the name of the source file containing the execution point represented by this stack trace element.
   * Generally, this corresponds to the {@code SourceFile} attribute of the relevant {@code class} file (as per <i>The
   * Java Virtual Machine Specification</i>, Section 4.7.7). In some systems, the name may refer to some source code
   * unit other than a file, such as an entry in source repository.
   * 
   * @return the name of the file containing the execution point represented by this stack trace element, or
   *         {@code null} if this information is unavailable.
   */
  String getFileName();

  /**
   * Returns the line number of the source line containing the execution point represented by this stack trace element.
   * Generally, this is derived from the {@code LineNumberTable} attribute of the relevant {@code class} file (as per
   * <i>The Java Virtual Machine Specification</i>, Section 4.7.8).
   * 
   * @return the line number of the source line containing the execution point represented by this stack trace element,
   *         or a negative number if this information is unavailable.
   */
  int getLineNumber();

  /**
   * Returns the fully qualified name of the class containing the execution point represented by this stack trace
   * element.
   * 
   * @return the fully qualified name of the {@code Class} containing the execution point represented by this stack
   *         trace element.
   */
  String getClassName();

  /**
   * Returns the name of the method containing the execution point represented by this stack trace element. If the
   * execution point is contained in an instance or class initializer, this method will return the appropriate
   * <i>special method name</i>, {@code <init>} or {@code <clinit>}, as per Section 3.9 of <i>The Java Virtual Machine
   * Specification</i>.
   * 
   * @return the name of the method containing the execution point represented by this stack trace element.
   */
  String getMethodName();

  /**
   * Returns true if the method containing the execution point represented by this stack trace element is a native
   * method.
   * 
   * @return {@code true} if the method containing the execution point represented by this stack trace element is a
   *         native method.
   */
  boolean isNativeMethod();

  String getDeclaringClass();

  void setDeclaringClass(String declaringClass);

  void setMethodName(String methodName);

  void setFileName(String fileName);

  void setLineNumber(int lineNumber);
}
