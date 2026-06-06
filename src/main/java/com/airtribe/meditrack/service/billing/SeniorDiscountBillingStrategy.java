package com.airtribe.meditrack.service.billing;

import com.airtribe.meditrack.entity.Appointment;
import com.airtribe.meditrack.entity.Bill;
import com.airtribe.meditrack.entity.BillType;
import com.airtribe.meditrack.entity.Doctor;
import com.airtribe.meditrack.entity.Patient;

/** Billing strategy that applies the senior citizen discount. */
public class SeniorDiscountBillingStrategy extends BillingStrategy {
  @Override
  public Bill generateBill(String billId, Appointment appointment, Patient patient, Doctor doctor) {
    double discount = doctor.getConsultationFee() * 0.10;
    return BillFactory.createBill(
        BillType.SENIOR_DISCOUNT,
        billId,
        appointment,
        patient,
        doctor,
        discount,
        "Senior citizen discount (10%)");
  }
}
