package com.airtribe.meditrack;

import com.airtribe.meditrack.constants.Constants;
import com.airtribe.meditrack.entity.Appointment;
import com.airtribe.meditrack.entity.Bill;
import com.airtribe.meditrack.entity.Doctor;
import com.airtribe.meditrack.entity.Patient;
import com.airtribe.meditrack.service.AppointmentService;
import com.airtribe.meditrack.service.AutoSaveService;
import com.airtribe.meditrack.service.BillingService;
import com.airtribe.meditrack.service.ClinicManagementService;
import com.airtribe.meditrack.service.DoctorService;
import com.airtribe.meditrack.service.PatientService;
import com.airtribe.meditrack.service.notification.ConsoleNotificationObserver;
import com.airtribe.meditrack.service.notification.ReminderSchedulerObserver;
import com.airtribe.meditrack.ui.console.AiConsoleMenu;
import com.airtribe.meditrack.ui.console.AnalyticsConsoleMenu;
import com.airtribe.meditrack.ui.console.AppointmentConsoleMenu;
import com.airtribe.meditrack.ui.console.BillingConsoleMenu;
import com.airtribe.meditrack.ui.console.ConsoleApp;
import com.airtribe.meditrack.ui.console.DoctorConsoleMenu;
import com.airtribe.meditrack.ui.console.InputReader;
import com.airtribe.meditrack.ui.console.PatientConsoleMenu;
import com.airtribe.meditrack.util.AppConfig;
import com.airtribe.meditrack.util.DataStore;
import com.airtribe.meditrack.util.PersistenceManager;
import com.airtribe.meditrack.util.PersistenceReport;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/** Console application entrypoint and startup wiring for MediTrack. */
public final class Main {
  private Main() {}

  /**
   * Starts the console application.
   *
   * @param args command-line arguments
   */
  public static void main(String[] args) {
    boolean loadData = args != null && Arrays.asList(args).contains("--loadData");
    Path baseDir = Path.of(Constants.DATA_DIR);

    DataStore<Doctor> doctors = new DataStore<>();
    DataStore<Patient> patients = new DataStore<>();
    DataStore<Appointment> appointments = new DataStore<>();
    DataStore<Bill> bills = new DataStore<>();

    DoctorService doctorService = new DoctorService(doctors);
    PatientService patientService = new PatientService(patients);
    AppointmentService appointmentService = new AppointmentService(appointments, doctorService, patientService);
    BillingService billingService = new BillingService(bills, appointmentService, doctorService, patientService);
    ClinicManagementService clinicManagementService =
        new ClinicManagementService(doctorService, patientService, appointmentService);

    ConsoleNotificationObserver consoleObserver = new ConsoleNotificationObserver();
    ReminderSchedulerObserver reminderObserver = new ReminderSchedulerObserver();
    appointmentService.addObserver(consoleObserver);
    appointmentService.addObserver(reminderObserver);

    if (loadData) {
      loadPersistedData(baseDir, doctors, patients, appointments, bills);
    }

    AutoSaveService autoSave =
        new AutoSaveService(
            baseDir,
            doctors,
            patients,
            appointments,
            bills,
            AppConfig.getInstance().getAutosaveIntervalSeconds());
    autoSave.start();

    try (Scanner scanner = new Scanner(System.in)) {
      InputReader inputReader = new InputReader(scanner);
      ConsoleApp app =
          new ConsoleApp(
              inputReader,
              new DoctorConsoleMenu(inputReader, doctorService, clinicManagementService),
              new PatientConsoleMenu(inputReader, patientService, clinicManagementService),
              new AppointmentConsoleMenu(inputReader, appointmentService, doctorService),
              new BillingConsoleMenu(inputReader, billingService),
              new AnalyticsConsoleMenu(doctorService, appointmentService),
              new AiConsoleMenu(inputReader, doctorService, appointmentService),
              () -> PersistenceManager.saveAll(baseDir, doctors, patients, appointments, bills));
      app.run();
    } finally {
      autoSave.requestStop();
      try {
        reminderObserver.close();
      } catch (Exception ignored) {
      }
      try {
        PersistenceManager.saveAll(baseDir, doctors, patients, appointments, bills);
      } catch (Exception e) {
        System.err.println("Final save failed: " + e.getMessage());
      }
    }
  }

  private static void loadPersistedData(
      Path baseDir,
      DataStore<Doctor> doctors,
      DataStore<Patient> patients,
      DataStore<Appointment> appointments,
      DataStore<Bill> bills) {
    try {
      List<PersistenceReport> reports =
          PersistenceManager.loadAllWithReport(baseDir, doctors, patients, appointments, bills);
      int totalLoaded = doctors.size() + patients.size() + appointments.size() + bills.size();
      System.out.println("Loaded persisted data from " + baseDir.toAbsolutePath());
      System.out.println(
          "Counts: doctors="
              + doctors.size()
              + ", patients="
              + patients.size()
              + ", appointments="
              + appointments.size()
              + ", bills="
              + bills.size()
              + ", total="
              + totalLoaded);
      for (PersistenceReport report : reports) {
        System.out.println("- " + report);
        for (String warning : report.getWarnings()) {
          System.out.println("  warning: " + warning);
        }
      }
    } catch (Exception e) {
      System.err.println("Failed to load data: " + e.getMessage());
    }
  }
}
