package com.airtribe.meditrack.util;

import com.airtribe.meditrack.entity.Bill;
import com.airtribe.meditrack.entity.BillType;
import com.airtribe.meditrack.exception.InvalidDataException;
import java.time.LocalDateTime;

/** CSV mapper for {@link Bill}. */
public class BillCSV extends CSVUtil<Bill> {
  private static final int EXPECTED_COLUMNS = 12;

  @Override
  protected String header() {
    return "id,type,appointmentId,patientId,doctorId,baseAmount,discountAmount,taxAmount,totalAmount,notes,paid,paidAt";
  }

  @Override
  protected String toRow(Bill bill) {
    Validator.requireNonNull("bill", bill);
    return String.join(
        ",",
        bill.getId(),
        bill.getType().name(),
        escape(bill.getAppointmentId()),
        escape(bill.getPatientId()),
        escape(bill.getDoctorId()),
        String.valueOf(bill.getBaseAmount()),
        String.valueOf(bill.getDiscountAmount()),
        String.valueOf(bill.getTaxAmount()),
        String.valueOf(bill.getTotalAmount()),
        escape(bill.getNotes()),
        String.valueOf(bill.isPaid()),
        bill.getPaidAt() == null ? "" : DateUtil.formatDateTime(bill.getPaidAt()));
  }

  @Override
  protected Bill fromColumns(String[] cols) {
    if (cols == null || cols.length != EXPECTED_COLUMNS) {
      throw new InvalidDataException(
          "Invalid bill CSV row: expected " + EXPECTED_COLUMNS + " columns but got "
              + (cols == null ? 0 : cols.length));
    }
    try {
      LocalDateTime paidAt = Validator.isBlank(cols[11]) ? null : DateUtil.parseDateTime(cols[11]);
      return Bill.restore(
          cols[0],
          BillType.valueOf(cols[1]),
          cols[2],
          cols[3],
          cols[4],
          Double.parseDouble(cols[5]),
          Double.parseDouble(cols[6]),
          Double.parseDouble(cols[7]),
          Double.parseDouble(cols[8]),
          cols[9],
          Boolean.parseBoolean(cols[10]),
          paidAt);
    } catch (Exception e) {
      throw new InvalidDataException("Invalid bill CSV row", e);
    }
  }

  private String escape(String value) {
    return value == null ? "" : value.trim();
  }
}
