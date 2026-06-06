package com.airtribe.meditrack.ui.console;

import com.airtribe.meditrack.entity.Appointment;
import com.airtribe.meditrack.entity.Doctor;
import com.airtribe.meditrack.service.AppointmentService;
import com.airtribe.meditrack.service.DoctorService;
import com.airtribe.meditrack.util.DateUtil;
import java.time.LocalDateTime;

/** Console menu for appointment scheduling and lifecycle management. */
public class AppointmentConsoleMenu {
  private final InputReader inputReader;
  private final AppointmentService appointmentService;
  private final DoctorService doctorService;

  /**
   * Creates the appointment console menu.
   */
  public AppointmentConsoleMenu(
      InputReader inputReader,
      AppointmentService appointmentService,
      DoctorService doctorService) {
    this.inputReader = inputReader;
    this.appointmentService = appointmentService;
    this.doctorService = doctorService;
  }

  /** Runs the appointment menu loop. */
  public void run() {
    while (true) {
      System.out.println();
      System.out.println("Appointments");
      System.out.println("1. Create confirmed");
      System.out.println("2. Create pending");
      System.out.println("3. Confirm");
      System.out.println("4. Cancel");
      System.out.println("5. Complete");
      System.out.println("6. Reschedule");
      System.out.println("7. List all");
      System.out.println("8. List by doctor");
      System.out.println("9. List by patient");
      System.out.println("10. Suggest slots");
      System.out.println("0. Back");

      switch (inputReader.promptOptionalString("Choose: ")) {
        case "1" -> createConfirmedAppointment();
        case "2" -> createPendingAppointment();
        case "3" -> System.out.println("Confirmed: " + appointmentService.confirmAppointment(inputReader.promptString("Appointment ID: ")).getId());
        case "4" -> System.out.println("Cancelled: " + appointmentService.cancelAppointment(inputReader.promptString("Appointment ID: ")).getId());
        case "5" -> System.out.println("Completed: " + appointmentService.completeAppointment(inputReader.promptString("Appointment ID: ")).getId());
        case "6" -> rescheduleAppointment();
        case "7" -> appointmentService.listAppointments().forEach(System.out::println);
        case "8" -> appointmentService.listByDoctor(inputReader.promptString("Doctor ID: ")).forEach(System.out::println);
        case "9" -> appointmentService.listByPatient(inputReader.promptString("Patient ID: ")).forEach(System.out::println);
        case "10" -> suggestSlots();
        case "0" -> {
          return;
        }
        default -> System.out.println("Invalid choice.");
      }
    }
  }

  private void createConfirmedAppointment() {
    Doctor doctor = requireDoctor();
    Integer duration =
        inputReader.promptOptionalInt(
            "Duration minutes (blank for doctor slot " + doctor.getSlotMinutes() + "): ");
    Appointment appointment =
        appointmentService.createAppointment(
            inputReader.promptString("Patient ID: "),
            doctor.getId(),
            inputReader.promptDateTime("Start (yyyy-MM-dd HH:mm): "),
            duration == null ? 0 : duration,
            ConsoleSupport.splitComma(
                inputReader.promptOptionalString("Symptoms (comma-separated, optional): ")));
    System.out.println("Created: " + appointment);
  }

  private void createPendingAppointment() {
    Doctor doctor = requireDoctor();
    Integer duration =
        inputReader.promptOptionalInt(
            "Duration minutes (blank for doctor slot " + doctor.getSlotMinutes() + "): ");
    Appointment appointment =
        appointmentService.createPendingAppointment(
            inputReader.promptString("Patient ID: "),
            doctor.getId(),
            inputReader.promptDateTime("Start (yyyy-MM-dd HH:mm): "),
            duration == null ? 0 : duration,
            ConsoleSupport.splitComma(
                inputReader.promptOptionalString("Symptoms (comma-separated, optional): ")));
    System.out.println("Created pending: " + appointment);
  }

  private void rescheduleAppointment() {
    String appointmentId = inputReader.promptString("Appointment ID: ");
    Appointment existing = appointmentService.getAppointment(appointmentId);
    Integer duration =
        inputReader.promptOptionalInt(
            "Duration minutes (" + existing.getDurationMinutes() + "): ");
    Appointment updated =
        appointmentService.rescheduleAppointment(
            appointmentId,
            inputReader.promptDateTime("New start (yyyy-MM-dd HH:mm): "),
            duration == null ? existing.getDurationMinutes() : duration);
    System.out.println("Rescheduled: " + updated);
  }

  private void suggestSlots() {
    String doctorId = inputReader.promptString("Doctor ID: ");
    String from = inputReader.promptOptionalString("From (yyyy-MM-dd HH:mm) (blank for now): ");
    LocalDateTime fromTime = from.isBlank() ? LocalDateTime.now() : DateUtil.parseDateTime(from);
    appointmentService
        .suggestNextSlots(doctorId, fromTime, 5)
        .forEach(slot -> System.out.println("Available: " + DateUtil.formatDateTime(slot)));
  }

  private Doctor requireDoctor() {
    String doctorId = inputReader.promptString("Doctor ID: ");
    Doctor doctor = doctorService.getDoctor(doctorId);
    if (doctor == null) {
      throw new com.airtribe.meditrack.exception.InvalidDataException("Doctor not found.");
    }
    return doctor;
  }
}
