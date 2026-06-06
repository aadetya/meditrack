package com.airtribe.meditrack.util;

import com.airtribe.meditrack.entity.Address;
import com.airtribe.meditrack.entity.Patient;
import com.airtribe.meditrack.exception.InvalidDataException;
import java.util.ArrayList;
import java.util.List;

/** CSV mapper for {@link Patient}. */
public class PatientCSV extends CSVUtil<Patient> {
  private static final int EXPECTED_COLUMNS = 12;

  @Override
  protected String header() {
    return "id,name,age,phone,email,insured,insuranceCoveragePercent,addressLine1,city,state,zip,allergies";
  }

  @Override
  protected String toRow(Patient p) {
    Validator.requireNonNull("patient", p);
    String allergies = String.join("|", p.getAllergies());
    Address a = p.getAddress();
    return String.join(
        ",",
        p.getId(),
        escape(p.getName()),
        String.valueOf(p.getAge()),
        escape(p.getPhone()),
        escape(p.getEmail()),
        String.valueOf(p.isInsured()),
        String.valueOf(p.getInsuranceCoveragePercent()),
        escape(a.getLine1()),
        escape(a.getCity()),
        escape(a.getState()),
        escape(a.getZip()),
        escape(allergies));
  }

  @Override
  protected Patient fromColumns(String[] cols) {
    if (cols == null || cols.length != EXPECTED_COLUMNS) {
      throw new InvalidDataException(
          "Invalid patient CSV row: expected " + EXPECTED_COLUMNS + " columns but got "
              + (cols == null ? 0 : cols.length));
    }
    try {
      String id = cols[0];
      String name = cols[1];
      int age = Integer.parseInt(cols[2]);
      String phone = cols[3];
      String email = cols[4];
      boolean insured = Boolean.parseBoolean(cols[5]);
      double coverage = Double.parseDouble(cols[6]);
      Address address = new Address(cols[7], cols[8], cols[9], cols[10]);
      List<String> allergies = new ArrayList<>();
      if (!Validator.isBlank(cols[11])) {
        String[] parts = cols[11].split("\\|");
        for (String part : parts) {
          if (!Validator.isBlank(part)) allergies.add(part.trim());
        }
      }
      return new Patient(id, name, age, phone, email, insured, coverage, address, allergies);
    } catch (Exception e) {
      throw new InvalidDataException("Invalid patient CSV row", e);
    }
  }

  private String escape(String v) {
    return v == null ? "" : v.trim();
  }
}
