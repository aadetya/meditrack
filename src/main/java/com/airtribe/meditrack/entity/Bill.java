package com.airtribe.meditrack.entity;

import com.airtribe.meditrack.constants.Constants;
import com.airtribe.meditrack.interfaces.Payable;
import com.airtribe.meditrack.util.Validator;
import java.time.LocalDateTime;

/** Bill entity that supports payment lifecycle, persistence, and immutable summary generation. */
public class Bill extends MedicalEntity implements Payable {
  private static final double EPSILON = 0.0001;

  private final BillType type;
  private final String appointmentId;
  private final String patientId;
  private final String doctorId;
  private final double baseAmount;
  private final double discountAmount;
  private double taxAmount;
  private double totalAmount;
  private final String notes;
  private boolean paid;
  private LocalDateTime paidAt;

  /**
   * Creates a new unpaid bill.
   *
   * @param id bill identifier
   * @param type bill type
   * @param appointmentId appointment identifier
   * @param patientId patient identifier
   * @param doctorId doctor identifier
   * @param baseAmount base amount before discounts
   * @param discountAmount discount amount
   * @param notes descriptive billing notes
   */
  public Bill(
      String id,
      BillType type,
      String appointmentId,
      String patientId,
      String doctorId,
      double baseAmount,
      double discountAmount,
      String notes) {
    super(id);
    this.type = Validator.requireNonNull("type", type);
    this.appointmentId = Validator.requireNonBlank("appointmentId", appointmentId);
    this.patientId = Validator.requireNonBlank("patientId", patientId);
    this.doctorId = Validator.requireNonBlank("doctorId", doctorId);
    this.baseAmount = Validator.requireNonNegativeDouble("baseAmount", baseAmount);
    this.discountAmount = Validator.requireNonNegativeDouble("discountAmount", discountAmount);
    validateDiscount(this.baseAmount, this.discountAmount);
    this.notes = notes == null ? "" : notes.trim();
    this.taxAmount = 0.0;
    this.totalAmount = baseAmount - discountAmount;
    this.paid = false;
  }

  /**
   * Rehydrates a bill with persisted payment state.
   *
   * @param id bill identifier
   * @param type bill type
   * @param appointmentId appointment identifier
   * @param patientId patient identifier
   * @param doctorId doctor identifier
   * @param baseAmount base amount before discounts
   * @param discountAmount discount amount
   * @param taxAmount calculated tax amount
   * @param totalAmount total amount
   * @param notes descriptive billing notes
   * @param paid payment flag
   * @param paidAt payment timestamp
   * @return restored bill
   */
  public static Bill restore(
      String id,
      BillType type,
      String appointmentId,
      String patientId,
      String doctorId,
      double baseAmount,
      double discountAmount,
      double taxAmount,
      double totalAmount,
      String notes,
      boolean paid,
      LocalDateTime paidAt) {
    Bill bill = new Bill(id, type, appointmentId, patientId, doctorId, baseAmount, discountAmount, notes);
    bill.taxAmount = Validator.requireNonNegativeDouble("taxAmount", taxAmount);
    bill.totalAmount = Validator.requireNonNegativeDouble("totalAmount", totalAmount);
    bill.paid = paid;
    bill.paidAt = paid ? Validator.requireNonNull("paidAt", paidAt) : paidAt;
    double expectedTotal = bill.baseAmount - bill.discountAmount + bill.taxAmount;
    if (paid && Math.abs(expectedTotal - bill.totalAmount) > EPSILON) {
      throw new IllegalArgumentException("Persisted totalAmount does not match bill amounts");
    }
    if (!paid) {
      bill.taxAmount = 0.0;
      bill.totalAmount = bill.baseAmount - bill.discountAmount;
      bill.paidAt = null;
    }
    return bill;
  }

  /**
   * Returns the bill type.
   *
   * @return bill type
   */
  public BillType getType() {
    return type;
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
   * Returns the base amount before discounts.
   *
   * @return base amount
   */
  public double getBaseAmount() {
    return baseAmount;
  }

  /**
   * Returns the discount amount.
   *
   * @return discount amount
   */
  public double getDiscountAmount() {
    return discountAmount;
  }

  /**
   * Returns the tax amount.
   *
   * @return tax amount
   */
  public double getTaxAmount() {
    return taxAmount;
  }

  /**
   * Returns the total amount after payment.
   *
   * @return total amount
   */
  public double getTotalAmount() {
    return totalAmount;
  }

  /**
   * Returns billing notes.
   *
   * @return notes
   */
  public String getNotes() {
    return notes;
  }

  /**
   * Returns whether the bill has been paid.
   *
   * @return true when paid
   */
  public boolean isPaid() {
    return paid;
  }

  /**
   * Returns the payment timestamp, if any.
   *
   * @return payment timestamp
   */
  public LocalDateTime getPaidAt() {
    return paidAt;
  }

  @Override
  /**
   * Pays the bill once, computing tax and total in one atomic mutation.
   *
   * @return paid bill instance
   */
  public Bill pay() {
    if (paid) return this;
    double taxable = baseAmount - discountAmount;
    this.taxAmount = computeTax(taxable, Constants.TAX_RATE);
    this.totalAmount = taxable + taxAmount;
    this.paid = true;
    this.paidAt = LocalDateTime.now();
    touch();
    return this;
  }

  /**
   * Converts the bill into an immutable summary snapshot.
   *
   * @return immutable bill summary
   */
  public BillSummary toSummary() {
    return BillSummary.from(this);
  }

  @Override
  public String toString() {
    return "Bill{"
        + "id='"
        + getId()
        + '\''
        + ", type="
        + type
        + ", appointmentId='"
        + appointmentId
        + '\''
        + ", base="
        + baseAmount
        + ", discount="
        + discountAmount
        + ", tax="
        + taxAmount
        + ", total="
        + totalAmount
        + ", paid="
        + paid
        + ", notes='"
        + notes
        + '\''
        + '}';
  }

  private static void validateDiscount(double baseAmount, double discountAmount) {
    if (discountAmount > baseAmount) {
      throw new IllegalArgumentException("discountAmount cannot exceed baseAmount");
    }
  }
}
