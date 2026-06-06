package com.airtribe.meditrack.service;

import com.airtribe.meditrack.entity.Doctor;
import com.airtribe.meditrack.entity.Specialization;
import com.airtribe.meditrack.exception.InvalidDataException;
import com.airtribe.meditrack.interfaces.Searchable;
import com.airtribe.meditrack.util.DataStore;
import com.airtribe.meditrack.util.IdGenerator;
import com.airtribe.meditrack.util.Validator;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class DoctorService implements Searchable<Doctor> {
  private final DataStore<Doctor> store;

  public DoctorService(DataStore<Doctor> store) {
    this.store = Objects.requireNonNull(store, "store");
  }

  public Doctor createDoctor(
      String name,
      int age,
      String phone,
      String email,
      Specialization specialization,
      double consultationFee,
      int slotMinutes) {
    String id = IdGenerator.getInstance().nextId("DOC");
    Doctor doctor = new Doctor(id, name, age, phone, email, specialization, consultationFee, slotMinutes);
    store.upsert(doctor);
    return doctor;
  }

  public Doctor getDoctor(String id) {
    return store.getById(id);
  }

  public List<Doctor> listDoctors() {
    return store.getAll();
  }

  public boolean deleteDoctor(String id) {
    return store.remove(id) != null;
  }

  public Doctor updateDoctor(
      String id,
      String name,
      Integer age,
      String phone,
      String email,
      Specialization specialization,
      Double consultationFee,
      Integer slotMinutes) {
    Doctor d = store.getById(id);
    if (d == null) throw new InvalidDataException("Doctor not found: " + id);

    if (!Validator.isBlank(name)) d.setName(name);
    if (age != null) d.setAge(age);
    if (!Validator.isBlank(phone)) d.setPhone(phone);
    if (!Validator.isBlank(email)) d.setEmail(email);
    if (specialization != null) d.setSpecialization(specialization);
    if (consultationFee != null) d.setConsultationFee(consultationFee);
    if (slotMinutes != null) d.setSlotMinutes(slotMinutes);

    store.upsert(d);
    return d;
  }

  public List<Doctor> searchByName(String name) {
    String q = normalize(name);
    return store.getAll().stream()
        .filter(d -> normalize(d.getName()).contains(q))
        .collect(Collectors.toList());
  }

  public List<Doctor> searchBySpecialization(Specialization specialization) {
    if (specialization == null) return List.of();
    return store.getAll().stream()
        .filter(d -> d.getSpecialization() == specialization)
        .collect(Collectors.toList());
  }

  @Override
  public List<Doctor> search(String query) {
    String q = normalize(query);
    if (q.isEmpty()) return List.of();
    return store.getAll().stream()
        .filter(
            d ->
                normalize(d.getName()).contains(q)
                    || normalize(d.getSpecialization().name()).contains(q)
                    || normalize(d.getId()).contains(q))
        .collect(Collectors.toList());
  }

  public double averageConsultationFee() {
    return store.getAll().stream().mapToDouble(Doctor::getConsultationFee).average().orElse(0.0);
  }

  public List<Doctor> sortByFeeThenName() {
    return store.getAll().stream()
        .sorted(Comparator.comparingDouble(Doctor::getConsultationFee).thenComparing(Doctor::getName))
        .collect(Collectors.toList());
  }
}

