package com.airtribe.meditrack.service.notification;

import com.airtribe.meditrack.entity.Appointment;
import com.airtribe.meditrack.entity.AppointmentStatus;
import com.airtribe.meditrack.util.Validator;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable observer-safe snapshot of an appointment event payload. */
public final class AppointmentSnapshot implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  private final String appointmentId;
  private final String patientId;
  private final String doctorId;
  private final LocalDateTime startTime;
  private final int durationMinutes;
  private final AppointmentStatus status;
  private final List<String> symptoms;

  /**
   * Creates a snapshot from an appointment instance.
   *
   * @param appointment source appointment
   */
  public AppointmentSnapshot(Appointment appointment) {
    Appointment safe = Validator.requireNonNull("appointment", appointment);
    this.appointmentId = safe.getId();
    this.patientId = safe.getPatientId();
    this.doctorId = safe.getDoctorId();
    this.startTime = safe.getStartTime();
    this.durationMinutes = safe.getDurationMinutes();
    this.status = safe.getStatus();
    this.symptoms = Collections.unmodifiableList(new ArrayList<>(safe.getSymptoms()));
  }

  /**
   * Returns the appointment identifier.
   *
   * @return appointment identifier
   */
  public String getAppointmentId() {
    return appointmentId;
  }

  /**
   * Returns the patient identifier.
   *
   * @return patient identifier
   */
  public String getPatientId() {
    return patientId;
  }

  /**
   * Returns the doctor identifier.
   *
   * @return doctor identifier
   */
  public String getDoctorId() {
    return doctorId;
  }

  /**
   * Returns the appointment start time.
   *
   * @return start time
   */
  public LocalDateTime getStartTime() {
    return startTime;
  }

  /**
   * Returns the appointment duration.
   *
   * @return duration in minutes
   */
  public int getDurationMinutes() {
    return durationMinutes;
  }

  /**
   * Returns the lifecycle state.
   *
   * @return appointment status
   */
  public AppointmentStatus getStatus() {
    return status;
  }

  /**
   * Returns an unmodifiable copy of symptoms.
   *
   * @return symptoms list
   */
  public List<String> getSymptoms() {
    return symptoms;
  }
}
