package com.airtribe.meditrack.service.billing;

import com.airtribe.meditrack.entity.Appointment;
import com.airtribe.meditrack.entity.Bill;
import com.airtribe.meditrack.entity.Doctor;
import com.airtribe.meditrack.entity.Patient;

/** Strategy base class for patient-specific billing calculations. */
public abstract class BillingStrategy {
  /**
   * Generates a bill for an appointment.
   *
   * @param billId bill identifier
   * @param appointment appointment being billed
   * @param patient patient being billed
   * @param doctor consulting doctor
   * @return generated bill
   */
  public abstract Bill generateBill(String billId, Appointment appointment, Patient patient, Doctor doctor);
}
