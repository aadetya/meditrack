package com.airtribe.meditrack.service;

import com.airtribe.meditrack.entity.Appointment;
import com.airtribe.meditrack.entity.AppointmentStatus;
import com.airtribe.meditrack.entity.Doctor;
import com.airtribe.meditrack.entity.Patient;
import com.airtribe.meditrack.exception.AppointmentNotFoundException;
import com.airtribe.meditrack.exception.InvalidDataException;
import com.airtribe.meditrack.service.notification.AppointmentEvent;
import com.airtribe.meditrack.service.notification.AppointmentEventType;
import com.airtribe.meditrack.service.notification.AppointmentObserver;
import com.airtribe.meditrack.util.DataStore;
import com.airtribe.meditrack.util.DateUtil;
import com.airtribe.meditrack.util.IdGenerator;
import com.airtribe.meditrack.util.Validator;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/** Service for appointment scheduling, lifecycle transitions, and observer publication. */
public class AppointmentService {
  private final DataStore<Appointment> store;
  private final DoctorService doctorService;
  private final PatientService patientService;
  private final List<AppointmentObserver> observers = new CopyOnWriteArrayList<>();

  /**
   * Creates an appointment service over shared stores and related services.
   *
   * @param store appointment store
   * @param doctorService doctor lookup service
   * @param patientService patient lookup service
   */
  public AppointmentService(
      DataStore<Appointment> store, DoctorService doctorService, PatientService patientService) {
    this.store = Objects.requireNonNull(store, "store");
    this.doctorService = Objects.requireNonNull(doctorService, "doctorService");
    this.patientService = Objects.requireNonNull(patientService, "patientService");
  }

  /**
   * Registers an appointment observer.
   *
   * @param observer observer to add
   */
  public void addObserver(AppointmentObserver observer) {
    if (observer != null) {
      observers.add(observer);
    }
  }

  /**
   * Removes an appointment observer.
   *
   * @param observer observer to remove
   */
  public void removeObserver(AppointmentObserver observer) {
    observers.remove(observer);
  }

  /**
   * Creates a confirmed appointment.
   *
   * @param patientId patient identifier
   * @param doctorId doctor identifier
   * @param startTime requested start time
   * @param durationMinutes requested duration, or doctor default when non-positive
   * @param symptoms symptom list
   * @return created appointment
   */
  public Appointment createAppointment(
      String patientId,
      String doctorId,
      LocalDateTime startTime,
      int durationMinutes,
      List<String> symptoms) {
    return createAppointmentInternal(
        patientId, doctorId, startTime, durationMinutes, AppointmentStatus.CONFIRMED, symptoms);
  }

  /**
   * Creates a pending appointment.
   *
   * @param patientId patient identifier
   * @param doctorId doctor identifier
   * @param startTime requested start time
   * @param durationMinutes requested duration, or doctor default when non-positive
   * @param symptoms symptom list
   * @return created appointment
   */
  public Appointment createPendingAppointment(
      String patientId,
      String doctorId,
      LocalDateTime startTime,
      int durationMinutes,
      List<String> symptoms) {
    return createAppointmentInternal(
        patientId, doctorId, startTime, durationMinutes, AppointmentStatus.PENDING, symptoms);
  }

  /**
   * Returns an appointment by identifier.
   *
   * @param id appointment identifier
   * @return matching appointment
   */
  public Appointment getAppointment(String id) {
    Appointment appointment = store.getById(id);
    if (appointment == null) {
      throw new AppointmentNotFoundException("Appointment not found: " + id);
    }
    return appointment;
  }

  /**
   * Returns all appointments ordered by start time.
   *
   * @return appointment list
   */
  public List<Appointment> listAppointments() {
    return store.getAll().stream().sorted(Comparator.comparing(Appointment::getStartTime)).toList();
  }

  /**
   * Returns appointments for a doctor ordered by start time.
   *
   * @param doctorId doctor identifier
   * @return doctor appointments
   */
  public List<Appointment> listByDoctor(String doctorId) {
    return store.getAll().stream()
        .filter(a -> a.getDoctorId().equals(doctorId))
        .sorted(Comparator.comparing(Appointment::getStartTime))
        .toList();
  }

  /**
   * Returns appointments for a patient ordered by start time.
   *
   * @param patientId patient identifier
   * @return patient appointments
   */
  public List<Appointment> listByPatient(String patientId) {
    return store.getAll().stream()
        .filter(a -> a.getPatientId().equals(patientId))
        .sorted(Comparator.comparing(Appointment::getStartTime))
        .toList();
  }

  /**
   * Confirms a pending appointment.
   *
   * @param appointmentId appointment identifier
   * @return updated appointment
   */
  public Appointment confirmAppointment(String appointmentId) {
    Appointment appointment = getAppointment(appointmentId);
    ensureAvailabilityFor(appointment, appointment.getStartTime(), appointment.getDurationMinutes(), appointmentId);
    appointment.confirm();
    store.upsert(appointment);
    notifyObservers(appointment, AppointmentEventType.CONFIRMED);
    return appointment;
  }

  /**
   * Cancels an appointment.
   *
   * @param appointmentId appointment identifier
   * @return updated appointment
   */
  public Appointment cancelAppointment(String appointmentId) {
    Appointment appointment = getAppointment(appointmentId);
    appointment.cancel();
    store.upsert(appointment);
    notifyObservers(appointment, AppointmentEventType.CANCELLED);
    return appointment;
  }

  /**
   * Completes an appointment.
   *
   * @param appointmentId appointment identifier
   * @return updated appointment
   */
  public Appointment completeAppointment(String appointmentId) {
    Appointment appointment = getAppointment(appointmentId);
    appointment.complete();
    store.upsert(appointment);
    notifyObservers(appointment, AppointmentEventType.COMPLETED);
    return appointment;
  }

  /**
   * Reschedules an active appointment while preserving symptoms.
   *
   * @param appointmentId appointment identifier
   * @param newStart new start time
   * @param durationMinutes new duration, or current duration when non-positive
   * @return updated appointment
   */
  public Appointment rescheduleAppointment(
      String appointmentId, LocalDateTime newStart, int durationMinutes) {
    Appointment appointment = getAppointment(appointmentId);
    int effectiveDuration = durationMinutes > 0 ? durationMinutes : appointment.getDurationMinutes();
    ensureAvailabilityFor(appointment, newStart, effectiveDuration, appointmentId);
    appointment.reschedule(newStart, effectiveDuration);
    store.upsert(appointment);
    notifyObservers(appointment, AppointmentEventType.RESCHEDULED);
    return appointment;
  }

  /**
   * Returns whether a doctor is available for the requested time window.
   *
   * @param doctorId doctor identifier
   * @param startTime desired start time
   * @param durationMinutes desired duration
   * @return true when available
   */
  public boolean isDoctorAvailable(String doctorId, LocalDateTime startTime, int durationMinutes) {
    return isDoctorAvailable(doctorId, startTime, durationMinutes, null);
  }

  /**
   * Suggests the next available appointment slots for a doctor.
   *
   * @param doctorId doctor identifier
   * @param from search start time
   * @param count number of requested slots
   * @return suggested slots
   */
  public List<LocalDateTime> suggestNextSlots(String doctorId, LocalDateTime from, int count) {
    Doctor doctor = doctorService.getDoctor(doctorId);
    if (doctor == null) {
      throw new InvalidDataException("Doctor not found: " + doctorId);
    }

    int want = count <= 0 ? 5 : count;
    LocalDateTime start = from == null ? LocalDateTime.now() : from;
    List<LocalDateTime> candidates =
        DateUtil.nextSlots(start, doctor.getSlotMinutes(), Math.max(30, want * 10));

    List<LocalDateTime> available = new ArrayList<>();
    for (LocalDateTime slot : candidates) {
      if (isDoctorAvailable(doctorId, slot, doctor.getSlotMinutes())) {
        available.add(slot);
        if (available.size() >= want) {
          break;
        }
      }
    }
    return available;
  }

  private Appointment createAppointmentInternal(
      String patientId,
      String doctorId,
      LocalDateTime startTime,
      int durationMinutes,
      AppointmentStatus status,
      List<String> symptoms) {
    Patient patient = patientService.getPatient(patientId);
    if (patient == null) {
      throw new InvalidDataException("Patient not found: " + patientId);
    }
    Doctor doctor = doctorService.getDoctor(doctorId);
    if (doctor == null) {
      throw new InvalidDataException("Doctor not found: " + doctorId);
    }
    Validator.requireNonNull("startTime", startTime);

    int duration = durationMinutes <= 0 ? doctor.getSlotMinutes() : durationMinutes;
    ensureAvailabilityFor(null, startTime, duration, doctorId, null);

    Appointment appointment =
        new Appointment(
            IdGenerator.getInstance().nextId("APT"),
            patient.getId(),
            doctor.getId(),
            startTime,
            duration,
            status,
            symptoms);
    store.upsert(appointment);
    notifyObservers(appointment, AppointmentEventType.CREATED);
    return appointment;
  }

  private void ensureAvailabilityFor(
      Appointment appointment,
      LocalDateTime startTime,
      int durationMinutes,
      String excludeAppointmentId) {
    ensureAvailabilityFor(
        appointment,
        startTime,
        durationMinutes,
        appointment.getDoctorId(),
        excludeAppointmentId);
  }

  private void ensureAvailabilityFor(
      Appointment appointment,
      LocalDateTime startTime,
      int durationMinutes,
      String doctorId,
      String excludeAppointmentId) {
    if (!isDoctorAvailable(doctorId, startTime, durationMinutes, excludeAppointmentId)) {
      throw new InvalidDataException("Doctor is not available at this time");
    }
  }

  private boolean isDoctorAvailable(
      String doctorId,
      LocalDateTime startTime,
      int durationMinutes,
      String excludeAppointmentId) {
    Validator.requireNonBlank("doctorId", doctorId);
    Validator.requireNonNull("startTime", startTime);
    Validator.requirePositiveInt("durationMinutes", durationMinutes);

    LocalDateTime endTime = startTime.plusMinutes(durationMinutes);
    for (Appointment appointment : store.getAll()) {
      if (!doctorId.equals(appointment.getDoctorId())) {
        continue;
      }
      if (!appointment.isActive()) {
        continue;
      }
      if (excludeAppointmentId != null && excludeAppointmentId.equals(appointment.getId())) {
        continue;
      }

      LocalDateTime existingStart = appointment.getStartTime();
      LocalDateTime existingEnd = appointment.getEndTime();
      boolean overlaps = startTime.isBefore(existingEnd) && existingStart.isBefore(endTime);
      if (overlaps) {
        return false;
      }
    }
    return true;
  }

  private void notifyObservers(Appointment appointment, AppointmentEventType eventType) {
    AppointmentEvent event =
        AppointmentEvent.fromAppointment(appointment, eventType, LocalDateTime.now());
    for (AppointmentObserver observer : observers) {
      try {
        observer.onEvent(event);
      } catch (Exception e) {
        System.err.println("Observer notification failed: " + e.getMessage());
      }
    }
  }
}
