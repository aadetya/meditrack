package com.airtribe.meditrack.entity;

import com.airtribe.meditrack.util.Validator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Patient domain entity with insurance details, address, and allergies. */
public class Patient extends Person implements Cloneable {
  private boolean insured;
  private double insuranceCoveragePercent;
  private Address address;
  private List<String> allergies;

  /**
   * Creates a patient record.
   *
   * @param id patient identifier
   * @param name patient name
   * @param age patient age
   * @param phone patient phone number
   * @param email patient email address
   * @param insured whether the patient is insured
   * @param insuranceCoveragePercent insurance coverage percentage
   * @param address patient address
   * @param allergies allergy list
   */
  public Patient(
      String id,
      String name,
      int age,
      String phone,
      String email,
      boolean insured,
      double insuranceCoveragePercent,
      Address address,
      List<String> allergies) {
    super(id, name, age, phone, email);
    setInsured(insured);
    setInsuranceCoveragePercent(insuranceCoveragePercent);
    setAddress(address);
    setAllergies(allergies);
  }

  /**
   * Returns whether the patient has insurance coverage.
   *
   * @return true when insured
   */
  public boolean isInsured() {
    return insured;
  }

  /**
   * Updates whether the patient is insured.
   *
   * @param insured insurance flag
   */
  public void setInsured(boolean insured) {
    this.insured = insured;
    if (!insured) {
      this.insuranceCoveragePercent = 0.0;
    }
    touch();
  }

  /**
   * Returns the insurance coverage percentage.
   *
   * @return coverage percentage between 0 and 100
   */
  public double getInsuranceCoveragePercent() {
    return insuranceCoveragePercent;
  }

  /**
   * Updates the insurance coverage percentage.
   *
   * @param insuranceCoveragePercent coverage percentage between 0 and 100
   */
  public void setInsuranceCoveragePercent(double insuranceCoveragePercent) {
    if (!insured) {
      this.insuranceCoveragePercent = 0.0;
      return;
    }
    this.insuranceCoveragePercent =
        Validator.requirePercent("insuranceCoveragePercent", insuranceCoveragePercent);
    touch();
  }

  /**
   * Returns the patient's immutable address value object.
   *
   * @return immutable address
   */
  public Address getAddress() {
    return address;
  }

  /**
   * Replaces the patient's address using a defensive copy.
   *
   * @param address source address
   */
  public void setAddress(Address address) {
    this.address = Address.copyOf(Validator.requireNonNull("address", address));
    touch();
  }

  /**
   * Returns an unmodifiable view of allergies.
   *
   * @return allergy list
   */
  public List<String> getAllergies() {
    return Collections.unmodifiableList(allergies);
  }

  /**
   * Adds a new allergy if it is not blank or already present.
   *
   * @param allergy allergy label
   */
  public void addAllergy(String allergy) {
    String v = allergy == null ? "" : allergy.trim();
    if (v.isEmpty()) return;
    if (!allergies.contains(v)) {
      allergies.add(v);
      touch();
    }
  }

  /**
   * Removes an allergy if present.
   *
   * @param allergy allergy label
   */
  public void removeAllergy(String allergy) {
    String v = allergy == null ? "" : allergy.trim();
    if (v.isEmpty()) return;
    if (allergies.remove(v)) {
      touch();
    }
  }

  /**
   * Replaces the allergy list with a sanitized copy.
   *
   * @param allergies allergy list
   */
  public void setAllergies(List<String> allergies) {
    List<String> safe = new ArrayList<>();
    if (allergies != null) {
      for (String a : allergies) {
        String v = a == null ? "" : a.trim();
        if (!v.isEmpty()) safe.add(v);
      }
    }
    this.allergies = safe;
    touch();
  }

  @Override
  /**
   * Creates a safe copy of the patient.
   *
   * @return copied patient
   */
  public Patient clone() {
    try {
      Patient copy = (Patient) super.clone();
      copy.address = address == null ? null : Address.copyOf(address);
      copy.allergies = allergies == null ? new ArrayList<>() : new ArrayList<>(allergies);
      return copy;
    } catch (CloneNotSupportedException e) {
      return new Patient(
          getId(),
          getName(),
          getAge(),
          getPhone(),
          getEmail(),
          insured,
          insuranceCoveragePercent,
          address == null ? null : Address.copyOf(address),
          allergies == null ? List.of() : new ArrayList<>(allergies));
    }
  }

  @Override
  public String toString() {
    return "Patient{"
        + "id='"
        + getId()
        + '\''
        + ", name='"
        + getName()
        + '\''
        + ", age="
        + getAge()
        + ", insured="
        + insured
        + ", coverage="
        + insuranceCoveragePercent
        + ", address='"
        + address
        + '\''
        + ", allergies="
        + allergies
        + '}';
  }
}
