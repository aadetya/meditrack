package com.airtribe.meditrack.entity;

import com.airtribe.meditrack.constants.Constants;
import com.airtribe.meditrack.util.Validator;

/** Doctor domain entity with specialization, consultation fee, and slot size metadata. */
public class Doctor extends Person {
  private Specialization specialization;
  private double consultationFee;
  private int slotMinutes;

  /**
   * Creates a doctor record.
   *
   * @param id doctor identifier
   * @param name doctor name
   * @param age doctor age
   * @param phone doctor phone number
   * @param email doctor email address
   * @param specialization doctor specialization
   * @param consultationFee consultation fee
   * @param slotMinutes preferred appointment slot duration
   */
  public Doctor(
      String id,
      String name,
      int age,
      String phone,
      String email,
      Specialization specialization,
      double consultationFee,
      int slotMinutes) {
    super(id, name, age, phone, email);
    setSpecialization(specialization);
    setConsultationFee(consultationFee);
    setSlotMinutes(slotMinutes <= 0 ? Constants.SLOT_MINUTES_DEFAULT : slotMinutes);
  }

  /**
   * Returns the doctor's specialization.
   *
   * @return specialization enum
   */
  public Specialization getSpecialization() {
    return specialization;
  }

  /**
   * Updates the specialization.
   *
   * @param specialization specialization enum
   */
  public void setSpecialization(Specialization specialization) {
    this.specialization = Validator.requireNonNull("specialization", specialization);
    touch();
  }

  /**
   * Returns the current consultation fee.
   *
   * @return consultation fee
   */
  public double getConsultationFee() {
    return consultationFee;
  }

  /**
   * Updates the consultation fee.
   *
   * @param consultationFee positive consultation fee
   */
  public void setConsultationFee(double consultationFee) {
    this.consultationFee = Validator.requirePositiveDouble("consultationFee", consultationFee);
    touch();
  }

  /**
   * Returns the preferred slot duration in minutes.
   *
   * @return slot duration in minutes
   */
  public int getSlotMinutes() {
    return slotMinutes;
  }

  /**
   * Updates the preferred slot duration in minutes.
   *
   * @param slotMinutes positive slot duration
   */
  public void setSlotMinutes(int slotMinutes) {
    this.slotMinutes = Validator.requirePositiveInt("slotMinutes", slotMinutes);
    touch();
  }

  @Override
  public String toString() {
    return "Doctor{"
        + "id='"
        + getId()
        + '\''
        + ", name='"
        + getName()
        + '\''
        + ", specialization="
        + specialization
        + ", fee="
        + consultationFee
        + ", slotMinutes="
        + slotMinutes
        + '}';
  }
}
