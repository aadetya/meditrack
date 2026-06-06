package com.airtribe.meditrack.service.billing;

import com.airtribe.meditrack.entity.Appointment;
import com.airtribe.meditrack.entity.Bill;
import com.airtribe.meditrack.entity.BillType;
import com.airtribe.meditrack.entity.Doctor;
import com.airtribe.meditrack.entity.Patient;
import com.airtribe.meditrack.util.Validator;

/** Factory responsible for constructing validated {@link Bill} instances. */
public final class BillFactory {
  private BillFactory() {}

  /**
   * Creates a bill from domain objects and a calculated discount.
   *
   * @param type bill type
   * @param billId bill identifier
   * @param appointment appointment being billed
   * @param patient patient being billed
   * @param doctor consulting doctor
   * @param discount discount amount
   * @param notes explanatory notes
   * @return validated bill instance
   */
  public static Bill createBill(
      BillType type,
      String billId,
      Appointment appointment,
      Patient patient,
      Doctor doctor,
      double discount,
      String notes) {
    Appointment safeAppointment = Validator.requireNonNull("appointment", appointment);
    Patient safePatient = Validator.requireNonNull("patient", patient);
    Doctor safeDoctor = Validator.requireNonNull("doctor", doctor);
    return new Bill(
        billId,
        Validator.requireNonNull("type", type),
        safeAppointment.getId(),
        safePatient.getId(),
        safeDoctor.getId(),
        safeDoctor.getConsultationFee(),
        discount,
        notes);
  }
}
