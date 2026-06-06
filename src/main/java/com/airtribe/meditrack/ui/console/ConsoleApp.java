package com.airtribe.meditrack.ui.console;

import com.airtribe.meditrack.exception.InvalidDataException;

/** Top-level console application shell that routes to focused menu handlers. */
public class ConsoleApp {
  private final InputReader inputReader;
  private final DoctorConsoleMenu doctorMenu;
  private final PatientConsoleMenu patientMenu;
  private final AppointmentConsoleMenu appointmentMenu;
  private final BillingConsoleMenu billingMenu;
  private final AnalyticsConsoleMenu analyticsMenu;
  private final AiConsoleMenu aiMenu;
  private final Runnable saveAction;

  /**
   * Creates the top-level console application shell.
   */
  public ConsoleApp(
      InputReader inputReader,
      DoctorConsoleMenu doctorMenu,
      PatientConsoleMenu patientMenu,
      AppointmentConsoleMenu appointmentMenu,
      BillingConsoleMenu billingMenu,
      AnalyticsConsoleMenu analyticsMenu,
      AiConsoleMenu aiMenu,
      Runnable saveAction) {
    this.inputReader = inputReader;
    this.doctorMenu = doctorMenu;
    this.patientMenu = patientMenu;
    this.appointmentMenu = appointmentMenu;
    this.billingMenu = billingMenu;
    this.analyticsMenu = analyticsMenu;
    this.aiMenu = aiMenu;
    this.saveAction = saveAction;
  }

  /** Runs the console application until the user exits or stdin closes. */
  public void run() {
    try {
      while (true) {
        System.out.println();
        System.out.println("MediTrack - Main Menu");
        System.out.println("1. Doctors");
        System.out.println("2. Patients");
        System.out.println("3. Appointments");
        System.out.println("4. Billing");
        System.out.println("5. AI Assist");
        System.out.println("6. Analytics");
        System.out.println("7. Save Now");
        System.out.println("0. Exit");

        String choice = inputReader.promptOptionalString("Choose: ");
        try {
          switch (choice) {
            case "1" -> doctorMenu.run();
            case "2" -> patientMenu.run();
            case "3" -> appointmentMenu.run();
            case "4" -> billingMenu.run();
            case "5" -> aiMenu.run();
            case "6" -> analyticsMenu.run();
            case "7" -> {
              saveAction.run();
              System.out.println("Saved.");
            }
            case "0" -> {
              return;
            }
            default -> System.out.println("Invalid choice.");
          }
        } catch (InvalidDataException e) {
          System.out.println("Error: " + e.getMessage());
        } catch (Exception e) {
          System.out.println("Unexpected error: " + e.getMessage());
        }
      }
    } catch (InputReader.EndOfInputException ignored) {
      System.out.println();
      System.out.println("Input closed. Exiting.");
    }
  }
}
