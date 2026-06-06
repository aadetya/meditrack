package com.airtribe.meditrack.ui.console;

import com.airtribe.meditrack.entity.Address;
import com.airtribe.meditrack.entity.Patient;
import com.airtribe.meditrack.service.ClinicManagementService;
import com.airtribe.meditrack.service.PatientService;
import java.util.List;

/** Console menu for patient CRUD and overloaded search flows. */
public class PatientConsoleMenu {
  private final InputReader inputReader;
  private final PatientService patientService;
  private final ClinicManagementService clinicManagementService;

  /**
   * Creates the patient console menu.
   */
  public PatientConsoleMenu(
      InputReader inputReader,
      PatientService patientService,
      ClinicManagementService clinicManagementService) {
    this.inputReader = inputReader;
    this.patientService = patientService;
    this.clinicManagementService = clinicManagementService;
  }

  /** Runs the patient menu loop. */
  public void run() {
    while (true) {
      System.out.println();
      System.out.println("Patients");
      System.out.println("1. Add");
      System.out.println("2. Update");
      System.out.println("3. Delete");
      System.out.println("4. List");
      System.out.println("5. Search (ID)");
      System.out.println("6. Search (Name)");
      System.out.println("7. Search (Age)");
      System.out.println("0. Back");

      switch (inputReader.promptOptionalString("Choose: ")) {
        case "1" -> addPatient();
        case "2" -> updatePatient();
        case "3" -> deletePatient();
        case "4" -> patientService.listPatients().forEach(System.out::println);
        case "5" -> searchById();
        case "6" -> searchByName();
        case "7" -> searchByAge();
        case "0" -> {
          return;
        }
        default -> System.out.println("Invalid choice.");
      }
    }
  }

  private void addPatient() {
    boolean insured = inputReader.promptYesNo("Insured (y/n): ");
    double coverage =
        insured ? inputReader.promptDouble("Insurance coverage percent (0-100): ") : 0.0;
    Patient patient =
        patientService.createPatient(
            inputReader.promptString("Name: "),
            inputReader.promptInt("Age: "),
            inputReader.promptString("Phone: "),
            inputReader.promptString("Email: "),
            insured,
            coverage,
            buildAddress(),
            ConsoleSupport.splitComma(
                inputReader.promptOptionalString("Allergies (comma-separated, optional): ")));
    System.out.println("Created: " + patient);
  }

  private void updatePatient() {
    String id = inputReader.promptString("Patient ID: ");
    Patient existing = patientService.getPatient(id);
    if (existing == null) {
      System.out.println("Patient not found.");
      return;
    }

    Boolean insured =
        inputReader.promptOptionalYesNo("Insured (" + existing.isInsured() + ") (y/n/blank): ");
    Double coverage =
        insured != null && insured
            ? inputReader.promptOptionalDouble(
                "Coverage (" + existing.getInsuranceCoveragePercent() + "): ")
            : insured != null && !insured ? 0.0 : null;

    Address address = null;
    if (inputReader.promptYesNo("Update address? (y/n): ")) {
      address =
          new Address(
              coalesce(
                  inputReader.promptOptionalString(
                      "Address line1 (" + existing.getAddress().getLine1() + "): "),
                  existing.getAddress().getLine1()),
              coalesce(
                  inputReader.promptOptionalString(
                      "City (" + existing.getAddress().getCity() + "): "),
                  existing.getAddress().getCity()),
              coalesce(
                  inputReader.promptOptionalString(
                      "State (" + existing.getAddress().getState() + "): "),
                  existing.getAddress().getState()),
              coalesce(
                  inputReader.promptOptionalString(
                      "ZIP (" + existing.getAddress().getZip() + "): "),
                  existing.getAddress().getZip()));
    }

    List<String> allergies = null;
    String allergiesInput =
        inputReader.promptOptionalString("Allergies (comma-separated, blank to keep): ");
    if (!allergiesInput.isBlank()) {
      allergies = ConsoleSupport.splitComma(allergiesInput);
    }

    Patient updated =
        patientService.updatePatient(
            id,
            blankToNull(inputReader.promptOptionalString("Name (" + existing.getName() + "): ")),
            inputReader.promptOptionalInt("Age (" + existing.getAge() + "): "),
            blankToNull(inputReader.promptOptionalString("Phone (" + existing.getPhone() + "): ")),
            blankToNull(inputReader.promptOptionalString("Email (" + existing.getEmail() + "): ")),
            insured,
            coverage,
            address,
            allergies);
    System.out.println("Updated: " + updated);
  }

  private void deletePatient() {
    String id = inputReader.promptString("Patient ID: ");
    boolean deleted = clinicManagementService.deletePatientSafely(id);
    System.out.println(deleted ? "Deleted." : "Patient not found.");
  }

  private void searchById() {
    Patient patient = patientService.searchPatient(inputReader.promptString("Patient ID: "));
    System.out.println(patient == null ? "Not found." : patient);
  }

  private void searchByName() {
    patientService.searchPatient(inputReader.promptString("Name: "), true).forEach(System.out::println);
  }

  private void searchByAge() {
    patientService.searchPatient(inputReader.promptInt("Age: ")).forEach(System.out::println);
  }

  private Address buildAddress() {
    return new Address(
        inputReader.promptString("Address line1: "),
        inputReader.promptString("City: "),
        inputReader.promptString("State: "),
        inputReader.promptString("ZIP: "));
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  private String coalesce(String preferred, String fallback) {
    return preferred == null || preferred.isBlank() ? fallback : preferred;
  }
}
