package com.airtribe.meditrack.ui.console;

import com.airtribe.meditrack.entity.Appointment;
import com.airtribe.meditrack.entity.Doctor;
import com.airtribe.meditrack.entity.Specialization;
import com.airtribe.meditrack.service.AppointmentService;
import com.airtribe.meditrack.service.DoctorService;
import java.util.Map;
import java.util.stream.Collectors;

/** Console menu for read-only stream and analytics demonstrations. */
public class AnalyticsConsoleMenu {
  private final DoctorService doctorService;
  private final AppointmentService appointmentService;

  /**
   * Creates the analytics console menu.
   */
  public AnalyticsConsoleMenu(
      DoctorService doctorService, AppointmentService appointmentService) {
    this.doctorService = doctorService;
    this.appointmentService = appointmentService;
  }

  /** Prints analytics output once and returns. */
  public void run() {
    System.out.println();
    System.out.println("Analytics");
    System.out.println("Average doctor fee: " + doctorService.averageConsultationFee());

    Map<String, Long> appointmentsPerDoctor =
        appointmentService.listAppointments().stream()
            .collect(Collectors.groupingBy(Appointment::getDoctorId, Collectors.counting()));
    System.out.println("Appointments per doctor:");
    appointmentsPerDoctor.forEach((doctorId, count) -> System.out.println("- " + doctorId + ": " + count));

    appointmentsPerDoctor.entrySet().stream()
        .max(Map.Entry.comparingByValue())
        .ifPresent(
            entry ->
                System.out.println(
                    "Busiest doctor: " + entry.getKey() + " (" + entry.getValue() + " appointments)"));

    Map<String, Long> appointmentsByStatus =
        appointmentService.listAppointments().stream()
            .collect(Collectors.groupingBy(a -> a.getStatus().name(), Collectors.counting()));
    System.out.println("Appointments by status:");
    appointmentsByStatus.forEach((status, count) -> System.out.println("- " + status + ": " + count));

    Map<Specialization, Long> doctorsBySpecialization =
        doctorService.listDoctors().stream()
            .collect(Collectors.groupingBy(Doctor::getSpecialization, Collectors.counting()));
    System.out.println("Doctors by specialization:");
    doctorsBySpecialization.forEach((specialization, count) -> System.out.println("- " + specialization + ": " + count));
  }
}
