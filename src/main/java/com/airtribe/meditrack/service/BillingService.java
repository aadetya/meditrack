package com.airtribe.meditrack.service;

import com.airtribe.meditrack.entity.Appointment;
import com.airtribe.meditrack.entity.AppointmentStatus;
import com.airtribe.meditrack.entity.Bill;
import com.airtribe.meditrack.entity.BillSummary;
import com.airtribe.meditrack.entity.Doctor;
import com.airtribe.meditrack.entity.Patient;
import com.airtribe.meditrack.exception.InvalidDataException;
import com.airtribe.meditrack.service.billing.BillingStrategy;
import com.airtribe.meditrack.service.billing.BillingStrategyFactory;
import com.airtribe.meditrack.util.DataStore;
import com.airtribe.meditrack.util.IdGenerator;
import java.util.List;
import java.util.Objects;

/** Service for bill generation, strategy selection, and bill store access. */
public class BillingService {
  private final DataStore<Bill> bills;
  private final AppointmentService appointmentService;
  private final DoctorService doctorService;
  private final PatientService patientService;

  /**
   * Creates a billing service over the supplied stores and lookup services.
   *
   * @param bills bill store
   * @param appointmentService appointment service
   * @param doctorService doctor service
   * @param patientService patient service
   */
  public BillingService(
      DataStore<Bill> bills,
      AppointmentService appointmentService,
      DoctorService doctorService,
      PatientService patientService) {
    this.bills = Objects.requireNonNull(bills, "bills");
    this.appointmentService = Objects.requireNonNull(appointmentService, "appointmentService");
    this.doctorService = Objects.requireNonNull(doctorService, "doctorService");
    this.patientService = Objects.requireNonNull(patientService, "patientService");
  }

  /**
   * Generates and stores a paid bill for an appointment.
   *
   * @param appointmentId appointment identifier
   * @return generated bill
   */
  public Bill generateBill(String appointmentId) {
    Appointment appointment = appointmentService.getAppointment(appointmentId);
    if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
      throw new InvalidDataException("Cannot generate a bill for a cancelled appointment");
    }
    if (appointment.getStatus() == AppointmentStatus.PENDING) {
      throw new InvalidDataException("Cannot generate a bill for a pending appointment");
    }

    Patient patient = patientService.getPatient(appointment.getPatientId());
    Doctor doctor = doctorService.getDoctor(appointment.getDoctorId());
    if (patient == null || doctor == null) {
      throw new InvalidDataException("Missing doctor or patient for appointment: " + appointmentId);
    }

    BillingStrategy strategy = BillingStrategyFactory.forPatient(patient);
    Bill bill = strategy.generateBill(IdGenerator.getInstance().nextId("BIL"), appointment, patient, doctor).pay();
    bills.upsert(bill);
    return bill;
  }

  /**
   * Generates an immutable summary for a newly created bill.
   *
   * @param appointmentId appointment identifier
   * @return immutable bill summary
   */
  public BillSummary generateBillSummary(String appointmentId) {
    return generateBill(appointmentId).toSummary();
  }

  /**
   * Returns all generated bills.
   *
   * @return bill list snapshot
   */
  public List<Bill> listBills() {
    return bills.getAll();
  }
}
