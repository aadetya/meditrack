package com.airtribe.meditrack.util;

import com.airtribe.meditrack.entity.Appointment;
import com.airtribe.meditrack.entity.AppointmentStatus;
import com.airtribe.meditrack.entity.Doctor;
import com.airtribe.meditrack.entity.Specialization;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Rule-based helper for specialization inference, doctor recommendation, and slot suggestions. */
public final class AIHelper {
  private static final Map<String, Specialization> KEYWORD_TO_SPECIALIZATION = new HashMap<>();

  static {
    KEYWORD_TO_SPECIALIZATION.put("skin", Specialization.DERMATOLOGIST);
    KEYWORD_TO_SPECIALIZATION.put("rash", Specialization.DERMATOLOGIST);
    KEYWORD_TO_SPECIALIZATION.put("acne", Specialization.DERMATOLOGIST);

    KEYWORD_TO_SPECIALIZATION.put("heart", Specialization.CARDIOLOGIST);
    KEYWORD_TO_SPECIALIZATION.put("chest", Specialization.CARDIOLOGIST);
    KEYWORD_TO_SPECIALIZATION.put("bp", Specialization.CARDIOLOGIST);

    KEYWORD_TO_SPECIALIZATION.put("bone", Specialization.ORTHOPEDIC);
    KEYWORD_TO_SPECIALIZATION.put("joint", Specialization.ORTHOPEDIC);
    KEYWORD_TO_SPECIALIZATION.put("knee", Specialization.ORTHOPEDIC);

    KEYWORD_TO_SPECIALIZATION.put("child", Specialization.PEDIATRICIAN);
    KEYWORD_TO_SPECIALIZATION.put("baby", Specialization.PEDIATRICIAN);
    KEYWORD_TO_SPECIALIZATION.put("fever", Specialization.GENERAL_PHYSICIAN);

    KEYWORD_TO_SPECIALIZATION.put("ear", Specialization.ENT);
    KEYWORD_TO_SPECIALIZATION.put("nose", Specialization.ENT);
    KEYWORD_TO_SPECIALIZATION.put("throat", Specialization.ENT);

    KEYWORD_TO_SPECIALIZATION.put("headache", Specialization.NEUROLOGIST);
    KEYWORD_TO_SPECIALIZATION.put("seizure", Specialization.NEUROLOGIST);
    KEYWORD_TO_SPECIALIZATION.put("migraine", Specialization.NEUROLOGIST);
  }

  private AIHelper() {}

  /**
   * Infers a likely specialization from symptom keywords.
   *
   * @param symptoms symptom list
   * @return inferred specialization
   */
  public static Specialization inferSpecialization(List<String> symptoms) {
    if (symptoms == null || symptoms.isEmpty()) return Specialization.GENERAL_PHYSICIAN;

    Map<Specialization, Integer> scores = new HashMap<>();
    for (String s : symptoms) {
      String normalized = s == null ? "" : s.trim().toLowerCase();
      if (normalized.isEmpty()) continue;

      for (Map.Entry<String, Specialization> entry : KEYWORD_TO_SPECIALIZATION.entrySet()) {
        if (normalized.contains(entry.getKey())) {
          scores.merge(entry.getValue(), 1, Integer::sum);
        }
      }
    }

    Optional<Map.Entry<Specialization, Integer>> best =
        scores.entrySet().stream().max(Map.Entry.comparingByValue());
    return best.map(Map.Entry::getKey).orElse(Specialization.GENERAL_PHYSICIAN);
  }

  /**
   * Recommends doctors based on symptoms, availability, and fee ordering.
   *
   * @param symptoms symptom list
   * @param doctors candidate doctors
   * @param appointments existing appointments
   * @param desiredStart desired start time
   * @param topN maximum recommendations
   * @return recommended doctors
   */
  public static List<Doctor> recommendDoctors(
      List<String> symptoms,
      List<Doctor> doctors,
      List<Appointment> appointments,
      LocalDateTime desiredStart,
      int topN) {
    if (doctors == null || doctors.isEmpty()) return List.of();
    LocalDateTime start = desiredStart == null ? LocalDateTime.now() : desiredStart;
    int limit = topN <= 0 ? 3 : topN;

    Specialization inferred = inferSpecialization(symptoms);
    List<Doctor> filtered =
        doctors.stream().filter(d -> d.getSpecialization() == inferred).collect(Collectors.toList());
    if (filtered.isEmpty()) filtered = doctors;

    Map<String, LocalDateTime> earliest = new HashMap<>();
    for (Doctor d : filtered) {
      LocalDateTime slot = firstAvailableSlot(d, appointments, start);
      earliest.put(d.getId(), slot);
    }

    Comparator<Doctor> cmp =
        Comparator.<Doctor, LocalDateTime>comparing(d -> earliest.get(d.getId()), Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparingDouble(Doctor::getConsultationFee);

    return filtered.stream().sorted(cmp).limit(limit).collect(Collectors.toList());
  }

  /**
   * Suggests the next available slots for a doctor.
   *
   * @param doctor target doctor
   * @param appointments existing appointments
   * @param desiredStart desired search start
   * @param count maximum slots
   * @return suggested slots
   */
  public static List<LocalDateTime> suggestSlots(
      Doctor doctor, List<Appointment> appointments, LocalDateTime desiredStart, int count) {
    Validator.requireNonNull("doctor", doctor);
    LocalDateTime start = desiredStart == null ? LocalDateTime.now() : desiredStart;
    int want = count <= 0 ? 5 : count;

    int candidates = Math.max(want * 10, 30);
    List<LocalDateTime> slots = DateUtil.nextSlots(start, doctor.getSlotMinutes(), candidates);

    List<LocalDateTime> available = new ArrayList<>();
    for (LocalDateTime slot : slots) {
      if (isDoctorAvailable(doctor, slot, doctor.getSlotMinutes(), appointments)) {
        available.add(slot);
        if (available.size() >= want) break;
      }
    }
    return available;
  }

  private static LocalDateTime firstAvailableSlot(
      Doctor doctor, List<Appointment> appointments, LocalDateTime desiredStart) {
    List<LocalDateTime> slots = suggestSlots(doctor, appointments, desiredStart, 1);
    return slots.isEmpty() ? null : slots.get(0);
  }

  private static boolean isDoctorAvailable(
      Doctor doctor, LocalDateTime start, int durationMinutes, List<Appointment> appointments) {
    if (appointments == null) return true;
    LocalDateTime end = start.plusMinutes(durationMinutes);
    for (Appointment a : appointments) {
      if (!doctor.getId().equals(a.getDoctorId())) continue;
      if (a.getStatus() == AppointmentStatus.CANCELLED) continue;
      LocalDateTime aStart = a.getStartTime();
      LocalDateTime aEnd = a.getEndTime();
      boolean overlaps = start.isBefore(aEnd) && aStart.isBefore(end);
      if (overlaps) return false;
    }
    return true;
  }
}
