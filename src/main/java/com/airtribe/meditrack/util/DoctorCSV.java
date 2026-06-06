package com.airtribe.meditrack.util;

import com.airtribe.meditrack.entity.Doctor;
import com.airtribe.meditrack.entity.Specialization;
import com.airtribe.meditrack.exception.InvalidDataException;

/** CSV mapper for {@link Doctor}. */
public class DoctorCSV extends CSVUtil<Doctor> {
  private static final int EXPECTED_COLUMNS = 8;

  @Override
  protected String header() {
    return "id,name,age,phone,email,specialization,consultationFee,slotMinutes";
  }

  @Override
  protected String toRow(Doctor d) {
    Validator.requireNonNull("doctor", d);
    return String.join(
        ",",
        d.getId(),
        escape(d.getName()),
        String.valueOf(d.getAge()),
        escape(d.getPhone()),
        escape(d.getEmail()),
        d.getSpecialization().name(),
        String.valueOf(d.getConsultationFee()),
        String.valueOf(d.getSlotMinutes()));
  }

  @Override
  protected Doctor fromColumns(String[] cols) {
    if (cols == null || cols.length != EXPECTED_COLUMNS) {
      throw new InvalidDataException(
          "Invalid doctor CSV row: expected " + EXPECTED_COLUMNS + " columns but got "
              + (cols == null ? 0 : cols.length));
    }
    try {
      String id = cols[0];
      String name = cols[1];
      int age = Integer.parseInt(cols[2]);
      String phone = cols[3];
      String email = cols[4];
      Specialization spec = Specialization.valueOf(cols[5]);
      double fee = Double.parseDouble(cols[6]);
      int slot = Integer.parseInt(cols[7]);
      return new Doctor(id, name, age, phone, email, spec, fee, slot);
    } catch (Exception e) {
      throw new InvalidDataException("Invalid doctor CSV row", e);
    }
  }

  private String escape(String v) {
    return v == null ? "" : v.trim();
  }
}
