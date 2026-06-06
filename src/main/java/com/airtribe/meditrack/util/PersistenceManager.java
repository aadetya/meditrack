package com.airtribe.meditrack.util;

import com.airtribe.meditrack.constants.Constants;
import com.airtribe.meditrack.entity.Appointment;
import com.airtribe.meditrack.entity.Bill;
import com.airtribe.meditrack.entity.Doctor;
import com.airtribe.meditrack.entity.MedicalEntity;
import com.airtribe.meditrack.entity.Patient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Coordinates CSV and serialization persistence for all MediTrack stores. */
public final class PersistenceManager {
  private PersistenceManager() {}

  /**
   * Saves doctors, patients, and appointments to the default data directory.
   */
  public static void saveAll(
      DataStore<Doctor> doctors, DataStore<Patient> patients, DataStore<Appointment> appointments) {
    saveAll(Path.of(Constants.DATA_DIR), doctors, patients, appointments);
  }

  /**
   * Saves doctors, patients, and appointments to a custom directory.
   */
  public static void saveAll(
      Path baseDir,
      DataStore<Doctor> doctors,
      DataStore<Patient> patients,
      DataStore<Appointment> appointments) {
    Validator.requireNonNull("baseDir", baseDir);
    Validator.requireNonNull("doctors", doctors);
    Validator.requireNonNull("patients", patients);
    Validator.requireNonNull("appointments", appointments);

    new DoctorCSV().write(baseDir.resolve(Constants.DOCTORS_CSV), doctors.getAll());
    new PatientCSV().write(baseDir.resolve(Constants.PATIENTS_CSV), patients.getAll());
    new AppointmentCSV().write(baseDir.resolve(Constants.APPOINTMENTS_CSV), appointments.getAll());

    SerializationUtil.save(baseDir.resolve(Constants.DOCTORS_SER), doctors);
    SerializationUtil.save(baseDir.resolve(Constants.PATIENTS_SER), patients);
    SerializationUtil.save(baseDir.resolve(Constants.APPOINTMENTS_SER), appointments);
  }

  /**
   * Saves doctors, patients, appointments, and bills to the default data directory.
   */
  public static void saveAll(
      DataStore<Doctor> doctors,
      DataStore<Patient> patients,
      DataStore<Appointment> appointments,
      DataStore<Bill> bills) {
    saveAll(Path.of(Constants.DATA_DIR), doctors, patients, appointments, bills);
  }

  /**
   * Saves doctors, patients, appointments, and bills to a custom directory.
   */
  public static void saveAll(
      Path baseDir,
      DataStore<Doctor> doctors,
      DataStore<Patient> patients,
      DataStore<Appointment> appointments,
      DataStore<Bill> bills) {
    saveAll(baseDir, doctors, patients, appointments);
    Validator.requireNonNull("bills", bills);
    new BillCSV().write(baseDir.resolve(Constants.BILLS_CSV), bills.getAll());
    SerializationUtil.save(baseDir.resolve(Constants.BILLS_SER), bills);
  }

  /**
   * Loads doctors, patients, and appointments from the default data directory.
   */
  public static void loadAll(
      DataStore<Doctor> doctors, DataStore<Patient> patients, DataStore<Appointment> appointments) {
    loadAllWithReport(Path.of(Constants.DATA_DIR), doctors, patients, appointments);
  }

  /**
   * Loads doctors, patients, and appointments from a custom directory.
   */
  public static void loadAll(
      Path baseDir,
      DataStore<Doctor> doctors,
      DataStore<Patient> patients,
      DataStore<Appointment> appointments) {
    loadAllWithReport(baseDir, doctors, patients, appointments);
  }

  /**
   * Loads doctors, patients, appointments, and bills from the default data directory.
   */
  public static void loadAll(
      DataStore<Doctor> doctors,
      DataStore<Patient> patients,
      DataStore<Appointment> appointments,
      DataStore<Bill> bills) {
    loadAllWithReport(Path.of(Constants.DATA_DIR), doctors, patients, appointments, bills);
  }

  /**
   * Loads doctors, patients, appointments, and bills from a custom directory.
   */
  public static void loadAll(
      Path baseDir,
      DataStore<Doctor> doctors,
      DataStore<Patient> patients,
      DataStore<Appointment> appointments,
      DataStore<Bill> bills) {
    loadAllWithReport(baseDir, doctors, patients, appointments, bills);
  }

  /**
   * Loads doctors, patients, and appointments while returning per-entity reports.
   *
   * @return load reports
   */
  public static List<PersistenceReport> loadAllWithReport(
      DataStore<Doctor> doctors, DataStore<Patient> patients, DataStore<Appointment> appointments) {
    return loadAllWithReport(Path.of(Constants.DATA_DIR), doctors, patients, appointments);
  }

  /**
   * Loads doctors, patients, and appointments from a custom directory while returning reports.
   *
   * @return load reports
   */
  public static List<PersistenceReport> loadAllWithReport(
      Path baseDir,
      DataStore<Doctor> doctors,
      DataStore<Patient> patients,
      DataStore<Appointment> appointments) {
    Validator.requireNonNull("baseDir", baseDir);
    Validator.requireNonNull("doctors", doctors);
    Validator.requireNonNull("patients", patients);
    Validator.requireNonNull("appointments", appointments);

    doctors.clear();
    patients.clear();
    appointments.clear();

    List<PersistenceReport> reports = new ArrayList<>();
    reports.add(
        loadStore(
            "Doctors",
            baseDir.resolve(Constants.DOCTORS_SER),
            baseDir.resolve(Constants.DOCTORS_CSV),
            new DoctorCSV(),
            doctors));
    reports.add(
        loadStore(
            "Patients",
            baseDir.resolve(Constants.PATIENTS_SER),
            baseDir.resolve(Constants.PATIENTS_CSV),
            new PatientCSV(),
            patients));
    reports.add(
        loadStore(
            "Appointments",
            baseDir.resolve(Constants.APPOINTMENTS_SER),
            baseDir.resolve(Constants.APPOINTMENTS_CSV),
            new AppointmentCSV(),
            appointments));
    seedIdGenerator(doctors, patients, appointments, null);
    return reports;
  }

  /**
   * Loads doctors, patients, appointments, and bills while returning per-entity reports.
   *
   * @return load reports
   */
  public static List<PersistenceReport> loadAllWithReport(
      DataStore<Doctor> doctors,
      DataStore<Patient> patients,
      DataStore<Appointment> appointments,
      DataStore<Bill> bills) {
    return loadAllWithReport(Path.of(Constants.DATA_DIR), doctors, patients, appointments, bills);
  }

  /**
   * Loads doctors, patients, appointments, and bills from a custom directory while returning
   * reports.
   *
   * @return load reports
   */
  public static List<PersistenceReport> loadAllWithReport(
      Path baseDir,
      DataStore<Doctor> doctors,
      DataStore<Patient> patients,
      DataStore<Appointment> appointments,
      DataStore<Bill> bills) {
    List<PersistenceReport> reports = loadAllWithReport(baseDir, doctors, patients, appointments);
    Validator.requireNonNull("bills", bills);
    bills.clear();
    reports.add(
        loadStore(
            "Bills",
            baseDir.resolve(Constants.BILLS_SER),
            baseDir.resolve(Constants.BILLS_CSV),
            new BillCSV(),
            bills));
    seedIdGenerator(doctors, patients, appointments, bills);
    return reports;
  }

  private static <T extends MedicalEntity> PersistenceReport loadStore(
      String entityName,
      Path serPath,
      Path csvPath,
      CSVUtil<T> csvUtil,
      DataStore<T> targetStore) {
    List<String> warnings = new ArrayList<>();

    if (Files.exists(serPath)) {
      try {
        @SuppressWarnings("unchecked")
        DataStore<T> loaded = (DataStore<T>) SerializationUtil.load(serPath, DataStore.class);
        copyStore(loaded, targetStore);
        return new PersistenceReport(
            entityName, PersistenceReport.Source.SER, targetStore.size(), warnings);
      } catch (Exception e) {
        warnings.add("Failed to deserialize " + serPath.getFileName() + ": " + e.getMessage());
      }
    }

    if (Files.exists(csvPath)) {
      for (T item : csvUtil.read(csvPath)) {
        targetStore.upsert(item);
      }
      return new PersistenceReport(
          entityName, PersistenceReport.Source.CSV, targetStore.size(), warnings);
    }

    return new PersistenceReport(entityName, PersistenceReport.Source.EMPTY, 0, warnings);
  }

  private static <T extends MedicalEntity> void copyStore(
      DataStore<T> source, DataStore<T> target) {
    for (T item : source.getAll()) {
      target.upsert(item);
    }
  }

  private static void seedIdGenerator(
      DataStore<Doctor> doctors,
      DataStore<Patient> patients,
      DataStore<Appointment> appointments,
      DataStore<Bill> bills) {
    List<String> allIds = new ArrayList<>();
    doctors.getAll().forEach(doctor -> allIds.add(doctor.getId()));
    patients.getAll().forEach(patient -> allIds.add(patient.getId()));
    appointments.getAll().forEach(appointment -> allIds.add(appointment.getId()));
    if (bills != null) {
      bills.getAll().forEach(bill -> allIds.add(bill.getId()));
    }
    IdGenerator.getInstance().seedFromExistingIds(allIds);
  }
}
