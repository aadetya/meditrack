package com.airtribe.meditrack.ui.console;

import com.airtribe.meditrack.constants.Constants;
import com.airtribe.meditrack.entity.Doctor;
import com.airtribe.meditrack.entity.Specialization;
import com.airtribe.meditrack.service.ClinicManagementService;
import com.airtribe.meditrack.service.DoctorService;

/** Console menu for doctor CRUD and search flows. */
public class DoctorConsoleMenu {
  private final InputReader inputReader;
  private final DoctorService doctorService;
  private final ClinicManagementService clinicManagementService;

  /**
   * Creates the doctor console menu.
   */
  public DoctorConsoleMenu(
      InputReader inputReader,
      DoctorService doctorService,
      ClinicManagementService clinicManagementService) {
    this.inputReader = inputReader;
    this.doctorService = doctorService;
    this.clinicManagementService = clinicManagementService;
  }

  /** Runs the doctor menu loop. */
  public void run() {
    while (true) {
      System.out.println();
      System.out.println("Doctors");
      System.out.println("1. Add");
      System.out.println("2. Update");
      System.out.println("3. Delete");
      System.out.println("4. List");
      System.out.println("5. Search");
      System.out.println("0. Back");

      switch (inputReader.promptOptionalString("Choose: ")) {
        case "1" -> addDoctor();
        case "2" -> updateDoctor();
        case "3" -> deleteDoctor();
        case "4" -> doctorService.listDoctors().forEach(System.out::println);
        case "5" -> searchDoctors();
        case "0" -> {
          return;
        }
        default -> System.out.println("Invalid choice.");
      }
    }
  }

  private void addDoctor() {
    Specialization specialization = inputReader.promptSpecialization("Specialization: ");
    Integer slotMinutes =
        inputReader.promptOptionalInt(
            "Slot minutes (blank for default " + Constants.SLOT_MINUTES_DEFAULT + "): ");
    Doctor doctor =
        doctorService.createDoctor(
            inputReader.promptString("Name: "),
            inputReader.promptInt("Age: "),
            inputReader.promptString("Phone: "),
            inputReader.promptString("Email: "),
            specialization,
            inputReader.promptDouble("Consultation fee: "),
            slotMinutes == null ? 0 : slotMinutes);
    System.out.println("Created: " + doctor);
  }

  private void updateDoctor() {
    String id = inputReader.promptString("Doctor ID: ");
    Doctor existing = doctorService.getDoctor(id);
    if (existing == null) {
      System.out.println("Doctor not found.");
      return;
    }

    Doctor updated =
        doctorService.updateDoctor(
            id,
            blankToNull(inputReader.promptOptionalString("Name (" + existing.getName() + "): ")),
            inputReader.promptOptionalInt("Age (" + existing.getAge() + "): "),
            blankToNull(inputReader.promptOptionalString("Phone (" + existing.getPhone() + "): ")),
            blankToNull(inputReader.promptOptionalString("Email (" + existing.getEmail() + "): ")),
            inputReader.promptOptionalSpecialization(
                "Specialization (" + existing.getSpecialization() + "): "),
            inputReader.promptOptionalDouble(
                "Fee (" + existing.getConsultationFee() + "): "),
            inputReader.promptOptionalInt("Slot minutes (" + existing.getSlotMinutes() + "): "));
    System.out.println("Updated: " + updated);
  }

  private void deleteDoctor() {
    String id = inputReader.promptString("Doctor ID: ");
    boolean deleted = clinicManagementService.deleteDoctorSafely(id);
    System.out.println(deleted ? "Deleted." : "Doctor not found.");
  }

  private void searchDoctors() {
    String query = inputReader.promptString("Search query (name/spec/id): ");
    doctorService.search(query).forEach(System.out::println);
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }
}
