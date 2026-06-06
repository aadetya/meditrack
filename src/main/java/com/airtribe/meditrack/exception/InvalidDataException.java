package com.airtribe.meditrack.exception;

/** Raised when domain validation or persistence constraints are violated. */
public class InvalidDataException extends RuntimeException {
  /**
   * Creates an exception with a message.
   *
   * @param message failure message
   */
  public InvalidDataException(String message) {
    super(message);
  }

  /**
   * Creates an exception with a message and root cause.
   *
   * @param message failure message
   * @param cause root cause
   */
  public InvalidDataException(String message, Throwable cause) {
    super(message, cause);
  }
}
