package com.airtribe.meditrack.service.billing;

import com.airtribe.meditrack.entity.Appointment;
import com.airtribe.meditrack.entity.Bill;
import com.airtribe.meditrack.entity.BillType;
import com.airtribe.meditrack.entity.Doctor;
import com.airtribe.meditrack.entity.Patient;

/** Default billing strategy with no discount. */
public class StandardBillingStrategy extends BillingStrategy {
  @Override
  public Bill generateBill(String billId, Appointment appointment, Patient patient, Doctor doctor) {
    double discount = 0.0;
    return BillFactory.createBill(
        BillType.STANDARD,
        billId,
        appointment,
        patient,
        doctor,
        discount,
        "Standard billing (no discounts)");
  }
}
