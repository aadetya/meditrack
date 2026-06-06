package com.airtribe.meditrack.test;

import com.airtribe.meditrack.constants.Constants;
import com.airtribe.meditrack.entity.Address;
import com.airtribe.meditrack.entity.Appointment;
import com.airtribe.meditrack.entity.Bill;
import com.airtribe.meditrack.entity.Doctor;
import com.airtribe.meditrack.entity.Patient;
import com.airtribe.meditrack.entity.Specialization;
import com.airtribe.meditrack.service.AppointmentService;
import com.airtribe.meditrack.service.BillingService;
import com.airtribe.meditrack.service.DoctorService;
import com.airtribe.meditrack.service.PatientService;
import com.airtribe.meditrack.util.DataStore;
import com.airtribe.meditrack.util.IdGenerator;
import com.airtribe.meditrack.util.PersistenceManager;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

/** Seeds a deterministic demo dataset for walkthroughs, recorded transcripts, and grading demos. */
public final class DemoDataSeeder {
  private static final LocalDateTime DEMO_BASE_TIME = LocalDateTime.of(2030, 1, 15, 10, 0);

  private final DataStore<Doctor> doctors = new DataStore<>();
  private final DataStore<Patient> patients = new DataStore<>();
  private final DataStore<Appointment> appointments = new DataStore<>();
  private final DataStore<Bill> bills = new DataStore<>();

  private final DoctorService doctorService = new DoctorService(doctors);
  private final PatientService patientService = new PatientService(patients);
  private final AppointmentService appointmentService =
      new AppointmentService(appointments, doctorService, patientService);
  private final BillingService billingService =
      new BillingService(bills, appointmentService, doctorService, patientService);

  /** Seeds the deterministic demo dataset into the default data directory. */
  public static void main(String[] args) {
    new DemoDataSeeder().run();
  }

  private void run() {
    IdGenerator.getInstance().resetForTests();
    Scenario scenario = seedScenario();
    PersistenceManager.saveAll(doctors, patients, appointments, bills);

    System.out.println("=== MediTrack Demo Data Seeder ===");
    System.out.println("Seeded deterministic demo data into: " + Path.of(Constants.DATA_DIR).toAbsolutePath());
    System.out.println("Demo base time: " + DEMO_BASE_TIME);
    System.out.println(
        "Counts: doctors="
            + doctors.size()
            + ", patients="
            + patients.size()
            + ", appointments="
            + appointments.size()
            + ", bills="
            + bills.size());
    System.out.println("Doctors:");
    System.out.println("- " + scenario.primaryDoctor.getId() + " -> " + scenario.primaryDoctor.getName());
    System.out.println("- " + scenario.specialistDoctor.getId() + " -> " + scenario.specialistDoctor.getName());
    System.out.println("- " + scenario.seniorDoctor.getId() + " -> " + scenario.seniorDoctor.getName());
    System.out.println("Patients:");
    System.out.println("- " + scenario.regularPatient.getId() + " -> " + scenario.regularPatient.getName());
    System.out.println("- " + scenario.insuredPatient.getId() + " -> " + scenario.insuredPatient.getName());
    System.out.println("- " + scenario.seniorPatient.getId() + " -> " + scenario.seniorPatient.getName());
    System.out.println("Key appointments:");
    System.out.println("- standard billing -> " + scenario.standardAppointment.getId());
    System.out.println("- insured billing -> " + scenario.insuredAppointment.getId());
    System.out.println("- cancelled -> " + scenario.cancelledAppointment.getId());
    System.out.println("Generated bills:");
    System.out.println("- standard -> " + scenario.standardBill.getId());
    System.out.println("- insured -> " + scenario.insuredBill.getId());
    System.out.println("- senior -> " + scenario.seniorBill.getId());
    System.out.println();
    System.out.println("Next steps:");
    System.out.println("mvn -q exec:java -Dexec.mainClass=com.airtribe.meditrack.Main -Dexec.args=\"--loadData\"");
    System.out.println("mvn -q exec:java -Dexec.mainClass=com.airtribe.meditrack.test.TestRunner");
  }

  private Scenario seedScenario() {
    Doctor primaryDoctor =
        doctorService.createDoctor(
            "Dr Meera Sharma",
            41,
            "+919345678901",
            "meera@meditrack.in",
            Specialization.GENERAL_PHYSICIAN,
            650.0,
            30);
    Doctor specialistDoctor =
        doctorService.createDoctor(
            "Dr Aarav Singh",
            46,
            "9789012345",
            "aarav@meditrack.in",
            Specialization.DERMATOLOGIST,
            900.0,
            20);
    Doctor seniorDoctor =
        doctorService.createDoctor(
            "Dr Kavya Iyer",
            52,
            "9890123456",
            "kavya@meditrack.in",
            Specialization.ENT,
            850.0,
            25);

    Patient regularPatient =
        patientService.createPatient(
            "Rohan Patel",
            29,
            "9876543210",
            "rohan@meditrack.in",
            false,
            0.0,
            new Address("12 MG Road", "Bengaluru", "Karnataka", "560001"),
            List.of("dust"));
    Patient insuredPatient =
        patientService.createPatient(
            "Ananya Rao",
            34,
            "+919876501234",
            "ananya@meditrack.in",
            true,
            80.0,
            new Address("44 Park Street", "Kolkata", "West Bengal", "700016"),
            List.of("peanuts"));
    Patient seniorPatient =
        patientService.createPatient(
            "Vikram Menon",
            67,
            "9765432109",
            "vikram@meditrack.in",
            false,
            0.0,
            new Address("7 Beach Road", "Chennai", "Tamil Nadu", "600001"),
            List.of("pollen"));

    Appointment standardAppointment =
        appointmentService.createAppointment(
            regularPatient.getId(),
            primaryDoctor.getId(),
            DEMO_BASE_TIME,
            primaryDoctor.getSlotMinutes(),
            List.of("fever", "fatigue"));
    Appointment insuredAppointment =
        appointmentService.createAppointment(
            insuredPatient.getId(),
            specialistDoctor.getId(),
            DEMO_BASE_TIME.plusMinutes(45),
            specialistDoctor.getSlotMinutes(),
            List.of("skin rash"));
    Appointment seniorAppointment =
        appointmentService.createAppointment(
            seniorPatient.getId(),
            primaryDoctor.getId(),
            DEMO_BASE_TIME.plusMinutes(90),
            primaryDoctor.getSlotMinutes(),
            List.of("cough"));
    appointmentService.createPendingAppointment(
        regularPatient.getId(),
        seniorDoctor.getId(),
        DEMO_BASE_TIME.plusMinutes(150),
        seniorDoctor.getSlotMinutes(),
        List.of("ear pain"));
    Appointment cancelledAppointment =
        appointmentService.createAppointment(
            regularPatient.getId(),
            specialistDoctor.getId(),
            DEMO_BASE_TIME.plusMinutes(210),
            specialistDoctor.getSlotMinutes(),
            List.of("itching"));
    appointmentService.cancelAppointment(cancelledAppointment.getId());

    Bill standardBill = billingService.generateBill(standardAppointment.getId());
    Bill insuredBill = billingService.generateBill(insuredAppointment.getId());
    Bill seniorBill = billingService.generateBill(seniorAppointment.getId());

    return new Scenario(
        primaryDoctor,
        specialistDoctor,
        seniorDoctor,
        regularPatient,
        insuredPatient,
        seniorPatient,
        standardAppointment,
        insuredAppointment,
        cancelledAppointment,
        standardBill,
        insuredBill,
        seniorBill);
  }

  private record Scenario(
      Doctor primaryDoctor,
      Doctor specialistDoctor,
      Doctor seniorDoctor,
      Patient regularPatient,
      Patient insuredPatient,
      Patient seniorPatient,
      Appointment standardAppointment,
      Appointment insuredAppointment,
      Appointment cancelledAppointment,
      Bill standardBill,
      Bill insuredBill,
      Bill seniorBill) {}
}
