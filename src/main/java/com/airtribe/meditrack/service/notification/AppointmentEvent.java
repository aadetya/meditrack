package com.airtribe.meditrack.service.notification;

import com.airtribe.meditrack.entity.Appointment;
import com.airtribe.meditrack.util.Validator;
import java.time.LocalDateTime;

/** Immutable event published by appointment lifecycle changes. */
public class AppointmentEvent {
  private final AppointmentSnapshot appointment;
  private final AppointmentEventType type;
  private final LocalDateTime timestamp;

  /**
   * Creates a new appointment event.
   *
   * @param appointment immutable appointment snapshot
   * @param type event type
   * @param timestamp event time
   */
  public AppointmentEvent(
      AppointmentSnapshot appointment, AppointmentEventType type, LocalDateTime timestamp) {
    this.appointment = Validator.requireNonNull("appointment", appointment);
    this.type = Validator.requireNonNull("type", type);
    this.timestamp = Validator.requireNonNull("timestamp", timestamp);
  }

  /**
   * Creates a new event by snapshotting a mutable appointment.
   *
   * @param appointment source appointment
   * @param type event type
   * @param timestamp event time
   * @return immutable appointment event
   */
  public static AppointmentEvent fromAppointment(
      Appointment appointment, AppointmentEventType type, LocalDateTime timestamp) {
    return new AppointmentEvent(new AppointmentSnapshot(appointment), type, timestamp);
  }

  /**
   * Returns the immutable appointment snapshot.
   *
   * @return appointment snapshot
   */
  public AppointmentSnapshot getAppointment() {
    return appointment;
  }

  /**
   * Returns the event type.
   *
   * @return event type
   */
  public AppointmentEventType getType() {
    return type;
  }

  /**
   * Returns the event timestamp.
   *
   * @return event timestamp
   */
  public LocalDateTime getTimestamp() {
    return timestamp;
  }
}
