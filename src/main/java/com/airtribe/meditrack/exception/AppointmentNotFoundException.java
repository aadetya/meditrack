package com.airtribe.meditrack.exception;

/** Raised when an appointment lookup fails for a required identifier. */
public class AppointmentNotFoundException extends RuntimeException {
  /**
   * Creates an exception with a message.
   *
   * @param message failure message
   */
  public AppointmentNotFoundException(String message) {
    super(message);
  }

  /**
   * Creates an exception with a message and root cause.
   *
   * @param message failure message
   * @param cause root cause
   */
  public AppointmentNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}
