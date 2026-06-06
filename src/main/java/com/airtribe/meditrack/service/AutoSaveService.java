package com.airtribe.meditrack.service;

import com.airtribe.meditrack.entity.Appointment;
import com.airtribe.meditrack.entity.Bill;
import com.airtribe.meditrack.entity.Doctor;
import com.airtribe.meditrack.entity.Patient;
import com.airtribe.meditrack.util.DataStore;
import com.airtribe.meditrack.util.PersistenceManager;
import java.nio.file.Path;
import java.util.Objects;

/** Background daemon thread that periodically persists application data. */
public class AutoSaveService extends Thread {
  private final Path baseDir;
  private final DataStore<Doctor> doctors;
  private final DataStore<Patient> patients;
  private final DataStore<Appointment> appointments;
  private final DataStore<Bill> bills;
  private final int intervalSeconds;
  private volatile boolean running;

  /**
   * Creates an autosave service for doctors, patients, and appointments.
   *
   * @param baseDir persistence directory
   * @param doctors doctor store
   * @param patients patient store
   * @param appointments appointment store
   * @param intervalSeconds autosave interval in seconds
   */
  public AutoSaveService(
      Path baseDir,
      DataStore<Doctor> doctors,
      DataStore<Patient> patients,
      DataStore<Appointment> appointments,
      int intervalSeconds) {
    this(baseDir, doctors, patients, appointments, null, intervalSeconds);
  }

  /**
   * Creates an autosave service that can also persist bills.
   *
   * @param baseDir persistence directory
   * @param doctors doctor store
   * @param patients patient store
   * @param appointments appointment store
   * @param bills optional bill store
   * @param intervalSeconds autosave interval in seconds
   */
  public AutoSaveService(
      Path baseDir,
      DataStore<Doctor> doctors,
      DataStore<Patient> patients,
      DataStore<Appointment> appointments,
      DataStore<Bill> bills,
      int intervalSeconds) {
    super("meditrack-autosave");
    this.baseDir = Objects.requireNonNull(baseDir, "baseDir");
    this.doctors = Objects.requireNonNull(doctors, "doctors");
    this.patients = Objects.requireNonNull(patients, "patients");
    this.appointments = Objects.requireNonNull(appointments, "appointments");
    this.bills = bills;
    this.intervalSeconds = intervalSeconds <= 0 ? 30 : intervalSeconds;
    this.running = true;
    setDaemon(true);
  }

  /** Requests a graceful shutdown. */
  public void requestStop() {
    running = false;
    interrupt();
  }

  @Override
  public void run() {
    while (running) {
      try {
        Thread.sleep(intervalSeconds * 1000L);
      } catch (InterruptedException e) {
        if (!running) {
          break;
        }
      }
      try {
        if (bills == null) {
          PersistenceManager.saveAll(baseDir, doctors, patients, appointments);
        } else {
          PersistenceManager.saveAll(baseDir, doctors, patients, appointments, bills);
        }
      } catch (Exception e) {
        System.err.println("Autosave failed: " + e.getMessage());
      }
    }
  }
}
