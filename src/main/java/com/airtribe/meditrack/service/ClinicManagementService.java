package com.airtribe.meditrack.service;

import com.airtribe.meditrack.entity.Appointment;
import com.airtribe.meditrack.exception.InvalidDataException;
import java.util.List;
import java.util.Objects;

/** Facade that coordinates cross-entity clinic rules such as safe deletion. */
public class ClinicManagementService {
  private final DoctorService doctorService;
  private final PatientService patientService;
  private final AppointmentService appointmentService;

  /**
   * Creates the clinic management facade.
   *
   * @param doctorService doctor service
   * @param patientService patient service
   * @param appointmentService appointment service
   */
  public ClinicManagementService(
      DoctorService doctorService,
      PatientService patientService,
      AppointmentService appointmentService) {
    this.doctorService = Objects.requireNonNull(doctorService, "doctorService");
    this.patientService = Objects.requireNonNull(patientService, "patientService");
    this.appointmentService = Objects.requireNonNull(appointmentService, "appointmentService");
  }

  /**
   * Deletes a doctor only when no active appointments reference the doctor.
   *
   * @param doctorId doctor identifier
   * @return true when deleted
   */
  public boolean deleteDoctorSafely(String doctorId) {
    ensureNoActiveAppointments(
        appointmentService.listByDoctor(doctorId),
        "Cannot delete doctor with active appointments: " + doctorId);
    return doctorService.deleteDoctor(doctorId);
  }

  /**
   * Deletes a patient only when no active appointments reference the patient.
   *
   * @param patientId patient identifier
   * @return true when deleted
   */
  public boolean deletePatientSafely(String patientId) {
    ensureNoActiveAppointments(
        appointmentService.listByPatient(patientId),
        "Cannot delete patient with active appointments: " + patientId);
    return patientService.deletePatient(patientId);
  }

  private void ensureNoActiveAppointments(
      List<Appointment> appointments, String failureMessage) {
    boolean hasActive = appointments.stream().anyMatch(Appointment::isActive);
    if (hasActive) {
      throw new InvalidDataException(failureMessage);
    }
  }
}
