package com.airtribe.meditrack.ui.console;

import com.airtribe.meditrack.entity.Appointment;
import com.airtribe.meditrack.entity.Doctor;
import com.airtribe.meditrack.service.AppointmentService;
import com.airtribe.meditrack.service.DoctorService;
import com.airtribe.meditrack.util.AIHelper;
import com.airtribe.meditrack.util.DateUtil;
import java.time.LocalDateTime;
import java.util.List;

/** Console menu for AI-style rule-based recommendations and slot suggestions. */
public class AiConsoleMenu {
  private final InputReader inputReader;
  private final DoctorService doctorService;
  private final AppointmentService appointmentService;

  /**
   * Creates the AI console menu.
   */
  public AiConsoleMenu(
      InputReader inputReader,
      DoctorService doctorService,
      AppointmentService appointmentService) {
    this.inputReader = inputReader;
    this.doctorService = doctorService;
    this.appointmentService = appointmentService;
  }

  /** Prints recommendations once and returns. */
  public void run() {
    System.out.println();
    System.out.println("AI Assist");
    List<String> symptoms =
        ConsoleSupport.splitComma(inputReader.promptOptionalString("Symptoms (comma-separated): "));
    String desired =
        inputReader.promptOptionalString("Desired start (yyyy-MM-dd HH:mm) (blank for now): ");
    LocalDateTime desiredStart =
        desired.isBlank() ? LocalDateTime.now() : DateUtil.parseDateTime(desired);

    List<Doctor> doctors = doctorService.listDoctors();
    List<Appointment> appointments = appointmentService.listAppointments();
    List<Doctor> recommended =
        AIHelper.recommendDoctors(symptoms, doctors, appointments, desiredStart, 3);
    if (recommended.isEmpty()) {
      System.out.println("No doctors available.");
      return;
    }

    System.out.println("Recommended doctors:");
    for (Doctor doctor : recommended) {
      System.out.println("- " + doctor);
      List<LocalDateTime> slots =
          AIHelper.suggestSlots(doctor, appointments, desiredStart, 5);
      for (LocalDateTime slot : slots) {
        System.out.println("  * " + DateUtil.formatDateTime(slot));
      }
    }
  }
}
