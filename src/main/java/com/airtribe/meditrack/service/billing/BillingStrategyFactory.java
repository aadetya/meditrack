package com.airtribe.meditrack.service.billing;

import com.airtribe.meditrack.entity.Patient;

/** Factory that selects a billing strategy based on patient attributes. */
public final class BillingStrategyFactory {
  private BillingStrategyFactory() {}

  /**
   * Returns the appropriate billing strategy for a patient.
   *
   * @param patient patient being billed
   * @return chosen billing strategy
   */
  public static BillingStrategy forPatient(Patient patient) {
    if (patient == null) return new StandardBillingStrategy();
    if (patient.isInsured()) return new InsuranceBillingStrategy();
    if (patient.getAge() >= 60) return new SeniorDiscountBillingStrategy();
    return new StandardBillingStrategy();
  }
}
