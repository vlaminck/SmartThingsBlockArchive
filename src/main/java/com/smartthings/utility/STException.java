package com.smartthings.utility;

public class STException extends Exception {

  public STExceptionType type;

  public STException() {
    super();
    type = STExceptionType.UNKNOWN;
    log(getLocalizedMessage());
  }

  public STException(String message) {
    super(message);
    type = STExceptionType.UNKNOWN;
    log(message);
    log(getLocalizedMessage());
  }

  public STException(String message, STExceptionType type) {
    super(message);
    this.type = type;
    log(message);
    log(getLocalizedMessage());
  }

  public STException(Exception e, STExceptionType type) {
    super(e.getLocalizedMessage());
    this.type = type;
    log(e.getLocalizedMessage());
  }


  //================================================================================
  // Logging
  //================================================================================

  private void log(Object message) {
    STLogger.fLog(message, this.getClass());
  }

}

