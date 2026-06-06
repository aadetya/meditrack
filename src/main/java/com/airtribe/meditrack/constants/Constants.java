package com.airtribe.meditrack.constants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Shared application constants and static initialization. */
public final class Constants {
  public static final double TAX_RATE = 0.18;

  public static final String DATA_DIR = "data";

  public static final String DOCTORS_CSV = "doctors.csv";
  public static final String PATIENTS_CSV = "patients.csv";
  public static final String APPOINTMENTS_CSV = "appointments.csv";
  public static final String BILLS_CSV = "bills.csv";

  public static final String DOCTORS_SER = "doctors.ser";
  public static final String PATIENTS_SER = "patients.ser";
  public static final String APPOINTMENTS_SER = "appointments.ser";
  public static final String BILLS_SER = "bills.ser";

  public static final int SLOT_MINUTES_DEFAULT = 30;
  public static final int REMINDER_MINUTES_BEFORE = 15;

  static {
    try {
      Files.createDirectories(Path.of(DATA_DIR));
    } catch (IOException e) {
      throw new ExceptionInInitializerError("Failed to create data directory: " + DATA_DIR);
    }
  }

  private Constants() {}
}
