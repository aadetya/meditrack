package com.airtribe.meditrack.util;

import com.airtribe.meditrack.entity.Appointment;
import com.airtribe.meditrack.entity.AppointmentStatus;
import com.airtribe.meditrack.exception.InvalidDataException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** CSV mapper for {@link Appointment}. */
public class AppointmentCSV extends CSVUtil<Appointment> {
  private static final int EXPECTED_COLUMNS = 7;

  @Override
  protected String header() {
    return "id,patientId,doctorId,startTime,durationMinutes,status,symptoms";
  }

  @Override
  protected String toRow(Appointment a) {
    Validator.requireNonNull("appointment", a);
    String symptoms = String.join("|", a.getSymptoms());
    return String.join(
        ",",
        a.getId(),
        escape(a.getPatientId()),
        escape(a.getDoctorId()),
        DateUtil.formatDateTime(a.getStartTime()),
        String.valueOf(a.getDurationMinutes()),
        a.getStatus().name(),
        escape(symptoms));
  }

  @Override
  protected Appointment fromColumns(String[] cols) {
    if (cols == null || cols.length != EXPECTED_COLUMNS) {
      throw new InvalidDataException(
          "Invalid appointment CSV row: expected " + EXPECTED_COLUMNS + " columns but got "
              + (cols == null ? 0 : cols.length));
    }
    try {
      String id = cols[0];
      String patientId = cols[1];
      String doctorId = cols[2];
      LocalDateTime start = DateUtil.parseDateTime(cols[3]);
      int duration = Integer.parseInt(cols[4]);
      AppointmentStatus status = AppointmentStatus.valueOf(cols[5]);
      List<String> symptoms = new ArrayList<>();
      if (!Validator.isBlank(cols[6])) {
        String[] parts = cols[6].split("\\|");
        for (String part : parts) {
          if (!Validator.isBlank(part)) symptoms.add(part.trim());
        }
      }
      return new Appointment(id, patientId, doctorId, start, duration, status, symptoms);
    } catch (Exception e) {
      throw new InvalidDataException("Invalid appointment CSV row", e);
    }
  }

  private String escape(String v) {
    return v == null ? "" : v.trim();
  }
}
