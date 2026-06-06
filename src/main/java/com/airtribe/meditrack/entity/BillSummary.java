package com.airtribe.meditrack.entity;

import com.airtribe.meditrack.util.Validator;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/** Immutable serializable summary projection of a {@link Bill}. */
public final class BillSummary implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  private final String billId;
  private final BillType billType;
  private final String appointmentId;
  private final String patientId;
  private final String doctorId;
  private final double baseAmount;
  private final double discountAmount;
  private final double taxAmount;
  private final double totalAmount;
  private final String notes;
  private final LocalDateTime generatedAt;

  /**
   * Creates an immutable bill summary snapshot.
   *
   * @param billId bill identifier
   * @param billType bill type
   * @param appointmentId appointment identifier
   * @param patientId patient identifier
   * @param doctorId doctor identifier
   * @param baseAmount base amount
   * @param discountAmount discount amount
   * @param taxAmount tax amount
   * @param totalAmount total amount
   * @param notes billing notes
   * @param generatedAt summary generation time
   */
  public BillSummary(
      String billId,
      BillType billType,
      String appointmentId,
      String patientId,
      String doctorId,
      double baseAmount,
      double discountAmount,
      double taxAmount,
      double totalAmount,
      String notes,
      LocalDateTime generatedAt) {
    this.billId = Validator.requireNonBlank("billId", billId);
    this.billType = Validator.requireNonNull("billType", billType);
    this.appointmentId = Validator.requireNonBlank("appointmentId", appointmentId);
    this.patientId = Validator.requireNonBlank("patientId", patientId);
    this.doctorId = Validator.requireNonBlank("doctorId", doctorId);
    this.baseAmount = Validator.requireNonNegativeDouble("baseAmount", baseAmount);
    this.discountAmount = Validator.requireNonNegativeDouble("discountAmount", discountAmount);
    this.taxAmount = Validator.requireNonNegativeDouble("taxAmount", taxAmount);
    this.totalAmount = Validator.requireNonNegativeDouble("totalAmount", totalAmount);
    this.notes = notes == null ? "" : notes.trim();
    this.generatedAt = Validator.requireNonNull("generatedAt", generatedAt);
    if (discountAmount > baseAmount) {
      throw new IllegalArgumentException("discountAmount cannot exceed baseAmount");
    }
  }

  /**
   * Creates a summary from a bill.
   *
   * @param bill source bill
   * @return immutable summary snapshot
   */
  public static BillSummary from(Bill bill) {
    Bill safeBill = Validator.requireNonNull("bill", bill);
    LocalDateTime generatedAt =
        safeBill.getPaidAt() == null ? safeBill.getUpdatedAt() : safeBill.getPaidAt();
    return new BillSummary(
        safeBill.getId(),
        safeBill.getType(),
        safeBill.getAppointmentId(),
        safeBill.getPatientId(),
        safeBill.getDoctorId(),
        safeBill.getBaseAmount(),
        safeBill.getDiscountAmount(),
        safeBill.getTaxAmount(),
        safeBill.getTotalAmount(),
        safeBill.getNotes(),
        generatedAt);
  }

  /**
   * Returns the bill identifier.
   *
   * @return bill identifier
   */
  public String getBillId() {
    return billId;
  }

  /**
   * Returns the bill type.
   *
   * @return bill type
   */
  public BillType getBillType() {
    return billType;
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
   * Returns the base amount.
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
   * Returns the total amount.
   *
   * @return total amount
   */
  public double getTotalAmount() {
    return totalAmount;
  }

  /**
   * Returns the billing notes.
   *
   * @return notes
   */
  public String getNotes() {
    return notes;
  }

  /**
   * Returns the summary generation timestamp.
   *
   * @return generation timestamp
   */
  public LocalDateTime getGeneratedAt() {
    return generatedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof BillSummary that)) return false;
    return Double.compare(that.baseAmount, baseAmount) == 0
        && Double.compare(that.discountAmount, discountAmount) == 0
        && Double.compare(that.taxAmount, taxAmount) == 0
        && Double.compare(that.totalAmount, totalAmount) == 0
        && billId.equals(that.billId)
        && billType == that.billType
        && appointmentId.equals(that.appointmentId)
        && patientId.equals(that.patientId)
        && doctorId.equals(that.doctorId)
        && notes.equals(that.notes)
        && generatedAt.equals(that.generatedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        billId,
        billType,
        appointmentId,
        patientId,
        doctorId,
        baseAmount,
        discountAmount,
        taxAmount,
        totalAmount,
        notes,
        generatedAt);
  }

  @Override
  public String toString() {
    return "BillSummary{"
        + "billId='"
        + billId
        + '\''
        + ", billType="
        + billType
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
        + ", generatedAt="
        + generatedAt
        + '}';
  }
}
