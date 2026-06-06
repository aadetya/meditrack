package com.airtribe.meditrack.service.billing;

import com.airtribe.meditrack.entity.Appointment;
import com.airtribe.meditrack.entity.Bill;
import com.airtribe.meditrack.entity.BillType;
import com.airtribe.meditrack.entity.Doctor;
import com.airtribe.meditrack.entity.Patient;

/** Billing strategy that applies insurance coverage as a discount. */
public class InsuranceBillingStrategy extends BillingStrategy {
  @Override
  public Bill generateBill(String billId, Appointment appointment, Patient patient, Doctor doctor) {
    double base = doctor.getConsultationFee();
    double coverage = patient.isInsured() ? patient.getInsuranceCoveragePercent() : 0.0;
    double discount = base * (coverage / 100.0);
    return BillFactory.createBill(
        BillType.INSURANCE,
        billId,
        appointment,
        patient,
        doctor,
        discount,
        "Insurance applied (" + coverage + "% coverage)");
  }
}
