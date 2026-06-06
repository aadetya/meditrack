package com.airtribe.meditrack.service;

import com.airtribe.meditrack.entity.Address;
import com.airtribe.meditrack.entity.Patient;
import com.airtribe.meditrack.exception.InvalidDataException;
import com.airtribe.meditrack.util.DataStore;
import com.airtribe.meditrack.util.IdGenerator;
import com.airtribe.meditrack.util.Validator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class PatientService {
  private final DataStore<Patient> store;

  public PatientService(DataStore<Patient> store) {
    this.store = Objects.requireNonNull(store, "store");
  }

  public Patient createPatient(
      String name,
      int age,
      String phone,
      String email,
      boolean insured,
      double insuranceCoveragePercent,
      Address address,
      List<String> allergies) {
    String id = IdGenerator.getInstance().nextId("PAT");
    Patient patient =
        new Patient(id, name, age, phone, email, insured, insuranceCoveragePercent, address, allergies);
    store.upsert(patient);
    return patient;
  }

  public Patient getPatient(String id) {
    return store.getById(id);
  }

  public List<Patient> listPatients() {
    return store.getAll();
  }

  public boolean deletePatient(String id) {
    return store.remove(id) != null;
  }

  public Patient updatePatient(
      String id,
      String name,
      Integer age,
      String phone,
      String email,
      Boolean insured,
      Double insuranceCoveragePercent,
      Address address,
      List<String> allergies) {
    Patient p = store.getById(id);
    if (p == null) throw new InvalidDataException("Patient not found: " + id);

    if (!Validator.isBlank(name)) p.setName(name);
    if (age != null) p.setAge(age);
    if (!Validator.isBlank(phone)) p.setPhone(phone);
    if (!Validator.isBlank(email)) p.setEmail(email);
    if (insured != null) p.setInsured(insured);
    if (insuranceCoveragePercent != null) p.setInsuranceCoveragePercent(insuranceCoveragePercent);
    if (address != null) p.setAddress(address);
    if (allergies != null) p.setAllergies(allergies);

    store.upsert(p);
    return p;
  }

  public Patient searchPatient(String id) {
    return store.getById(id);
  }

  public List<Patient> searchPatient(String name, boolean partialMatch) {
    if (Validator.isBlank(name)) return List.of();
    String q = name.trim().toLowerCase();
    return store.getAll().stream()
        .filter(
            p -> {
              String n = p.getName() == null ? "" : p.getName().toLowerCase();
              return partialMatch ? n.contains(q) : n.equals(q);
            })
        .collect(Collectors.toList());
  }

  public List<Patient> searchPatient(int age) {
    return store.getAll().stream().filter(p -> p.getAge() == age).collect(Collectors.toList());
  }
}

