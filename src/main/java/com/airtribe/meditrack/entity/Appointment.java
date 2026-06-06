package com.airtribe.meditrack.entity;

import com.airtribe.meditrack.exception.InvalidDataException;
import com.airtribe.meditrack.util.Validator;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Appointment entity with lifecycle validation and defensive collection handling. */
public class Appointment extends MedicalEntity implements Cloneable {
  private String patientId;
  private String doctorId;
  private LocalDateTime startTime;
  private int durationMinutes;
  private AppointmentStatus status;
  private List<String> symptoms;

  /**
   * Creates an appointment with explicit lifecycle state.
   *
   * @param id appointment identifier
   * @param patientId patient identifier
   * @param doctorId doctor identifier
   * @param startTime appointment start time
   * @param durationMinutes appointment duration
   * @param status initial status
   * @param symptoms symptom list
   */
  public Appointment(
      String id,
      String patientId,
      String doctorId,
      LocalDateTime startTime,
      int durationMinutes,
      AppointmentStatus status,
      List<String> symptoms) {
    super(id);
    this.patientId = Validator.requireNonBlank("patientId", patientId);
    this.doctorId = Validator.requireNonBlank("doctorId", doctorId);
    this.startTime = Validator.requireNonNull("startTime", startTime);
    this.durationMinutes = Validator.requirePositiveInt("durationMinutes", durationMinutes);
    this.status = Validator.requireNonNull("status", status);
    this.symptoms = sanitizeSymptoms(symptoms);
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
   * Updates the patient identifier.
   *
   * @param patientId patient identifier
   */
  public void setPatientId(String patientId) {
    this.patientId = Validator.requireNonBlank("patientId", patientId);
    touch();
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
   * Updates the doctor identifier.
   *
   * @param doctorId doctor identifier
   */
  public void setDoctorId(String doctorId) {
    this.doctorId = Validator.requireNonBlank("doctorId", doctorId);
    touch();
  }

  /**
   * Returns the appointment start time.
   *
   * @return appointment start
   */
  public LocalDateTime getStartTime() {
    return startTime;
  }

  /**
   * Updates the appointment start time.
   *
   * @param startTime new start time
   */
  public void setStartTime(LocalDateTime startTime) {
    this.startTime = Validator.requireNonNull("startTime", startTime);
    touch();
  }

  /**
   * Returns the appointment duration in minutes.
   *
   * @return duration in minutes
   */
  public int getDurationMinutes() {
    return durationMinutes;
  }

  /**
   * Updates the appointment duration in minutes.
   *
   * @param durationMinutes positive duration
   */
  public void setDurationMinutes(int durationMinutes) {
    this.durationMinutes = Validator.requirePositiveInt("durationMinutes", durationMinutes);
    touch();
  }

  /**
   * Returns the current lifecycle state.
   *
   * @return appointment status
   */
  public AppointmentStatus getStatus() {
    return status;
  }

  /**
   * Transitions the appointment to another legal lifecycle state.
   *
   * @param status target status
   */
  public void setStatus(AppointmentStatus status) {
    transitionTo(status);
  }

  /**
   * Returns an unmodifiable view of symptoms.
   *
   * @return symptoms list
   */
  public List<String> getSymptoms() {
    return Collections.unmodifiableList(symptoms);
  }

  /**
   * Adds a new symptom if not blank or duplicated.
   *
   * @param symptom symptom label
   */
  public void addSymptom(String symptom) {
    String v = symptom == null ? "" : symptom.trim();
    if (v.isEmpty()) return;
    if (!symptoms.contains(v)) {
      symptoms.add(v);
      touch();
    }
  }

  /**
   * Removes a symptom if present.
   *
   * @param symptom symptom label
   */
  public void removeSymptom(String symptom) {
    String v = symptom == null ? "" : symptom.trim();
    if (v.isEmpty()) return;
    if (symptoms.remove(v)) {
      touch();
    }
  }

  /**
   * Replaces symptoms with a sanitized copy.
   *
   * @param symptoms symptom list
   */
  public void setSymptoms(List<String> symptoms) {
    this.symptoms = sanitizeSymptoms(symptoms);
    touch();
  }

  /**
   * Returns the appointment end time derived from start time and duration.
   *
   * @return appointment end time
   */
  public LocalDateTime getEndTime() {
    return startTime.plusMinutes(durationMinutes);
  }

  /**
   * Returns whether the appointment is still active for referential integrity checks.
   *
   * @return true when not cancelled or completed
   */
  public boolean isActive() {
    return status != AppointmentStatus.CANCELLED && status != AppointmentStatus.COMPLETED;
  }

  /**
   * Confirms a pending appointment.
   */
  public void confirm() {
    transitionTo(AppointmentStatus.CONFIRMED);
  }

  /**
   * Cancels a pending or confirmed appointment.
   */
  public void cancel() {
    transitionTo(AppointmentStatus.CANCELLED);
  }

  /**
   * Completes a confirmed appointment.
   */
  public void complete() {
    transitionTo(AppointmentStatus.COMPLETED);
  }

  /**
   * Reschedules the appointment while preserving symptoms and status.
   *
   * @param newStartTime new start time
   * @param newDurationMinutes new duration in minutes
   */
  public void reschedule(LocalDateTime newStartTime, int newDurationMinutes) {
    if (status == AppointmentStatus.CANCELLED || status == AppointmentStatus.COMPLETED) {
      throw new InvalidDataException("Only pending or confirmed appointments can be rescheduled");
    }
    this.startTime = Validator.requireNonNull("newStartTime", newStartTime);
    this.durationMinutes = Validator.requirePositiveInt("newDurationMinutes", newDurationMinutes);
    touch();
  }

  /**
   * Applies a validated lifecycle transition.
   *
   * @param target target status
   */
  public void transitionTo(AppointmentStatus target) {
    AppointmentStatus safeTarget = Validator.requireNonNull("status", target);
    if (status == safeTarget) {
      throw new InvalidDataException("Appointment is already " + safeTarget.name());
    }
    boolean allowed =
        switch (status) {
          case PENDING -> safeTarget == AppointmentStatus.CONFIRMED || safeTarget == AppointmentStatus.CANCELLED;
          case CONFIRMED -> safeTarget == AppointmentStatus.CANCELLED || safeTarget == AppointmentStatus.COMPLETED;
          case CANCELLED, COMPLETED -> false;
        };
    if (!allowed) {
      throw new InvalidDataException(
          "Invalid appointment state transition: " + status + " -> " + safeTarget);
    }
    this.status = safeTarget;
    touch();
  }

  @Override
  /**
   * Creates a deep copy of the appointment.
   *
   * @return copied appointment
   */
  public Appointment clone() {
    try {
      Appointment copy = (Appointment) super.clone();
      copy.symptoms = symptoms == null ? new ArrayList<>() : new ArrayList<>(symptoms);
      return copy;
    } catch (CloneNotSupportedException e) {
      return new Appointment(
          getId(),
          patientId,
          doctorId,
          startTime,
          durationMinutes,
          status,
          symptoms == null ? List.of() : new ArrayList<>(symptoms));
    }
  }

  @Override
  public String toString() {
    return "Appointment{"
        + "id='"
        + getId()
        + '\''
        + ", patientId='"
        + patientId
        + '\''
        + ", doctorId='"
        + doctorId
        + '\''
        + ", startTime="
        + startTime
        + ", durationMinutes="
        + durationMinutes
        + ", status="
        + status
        + ", symptoms="
        + symptoms
        + '}';
  }

  private static List<String> sanitizeSymptoms(List<String> symptoms) {
    List<String> safe = new ArrayList<>();
    if (symptoms != null) {
      for (String symptom : symptoms) {
        String value = symptom == null ? "" : symptom.trim();
        if (!value.isEmpty()) {
          safe.add(value);
        }
      }
    }
    return safe;
  }
}
