package com.airtribe.meditrack.service.notification;

/** Observer contract for appointment lifecycle updates. */
public interface AppointmentObserver {
  /**
   * Handles an appointment event.
   *
   * @param event immutable appointment event
   */
  void onEvent(AppointmentEvent event);
}
