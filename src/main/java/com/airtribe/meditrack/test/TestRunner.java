package com.airtribe.meditrack.test;

import com.airtribe.meditrack.constants.Constants;
import com.airtribe.meditrack.entity.Address;
import com.airtribe.meditrack.entity.Appointment;
import com.airtribe.meditrack.entity.AppointmentStatus;
import com.airtribe.meditrack.entity.Bill;
import com.airtribe.meditrack.entity.BillSummary;
import com.airtribe.meditrack.entity.BillType;
import com.airtribe.meditrack.entity.Doctor;
import com.airtribe.meditrack.entity.Patient;
import com.airtribe.meditrack.entity.Specialization;
import com.airtribe.meditrack.exception.InvalidDataException;
import com.airtribe.meditrack.service.AppointmentService;
import com.airtribe.meditrack.service.AutoSaveService;
import com.airtribe.meditrack.service.BillingService;
import com.airtribe.meditrack.service.ClinicManagementService;
import com.airtribe.meditrack.service.DoctorService;
import com.airtribe.meditrack.service.PatientService;
import com.airtribe.meditrack.service.billing.BillFactory;
import com.airtribe.meditrack.service.billing.BillingStrategyFactory;
import com.airtribe.meditrack.service.billing.InsuranceBillingStrategy;
import com.airtribe.meditrack.service.billing.SeniorDiscountBillingStrategy;
import com.airtribe.meditrack.service.billing.StandardBillingStrategy;
import com.airtribe.meditrack.service.notification.AppointmentEvent;
import com.airtribe.meditrack.service.notification.AppointmentEventType;
import com.airtribe.meditrack.service.notification.AppointmentObserver;
import com.airtribe.meditrack.service.notification.AppointmentSnapshot;
import com.airtribe.meditrack.service.notification.ReminderSchedulerObserver;
import com.airtribe.meditrack.util.AIHelper;
import com.airtribe.meditrack.util.DataStore;
import com.airtribe.meditrack.util.IdGenerator;
import com.airtribe.meditrack.util.PersistenceManager;
import com.airtribe.meditrack.util.PersistenceReport;
import com.airtribe.meditrack.util.Validator;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/** Manual test runner used as the canonical verification harness for the assignment. */
public final class TestRunner {
  private static int passed = 0;
  private static int failed = 0;

  private TestRunner() {}

  /**
   * Runs the full manual test suite.
   *
   * @param args ignored
   */
  public static void main(String[] args) {
    try {
      runAll();
    } catch (Exception e) {
      fail("Unhandled exception in tests: " + e.getMessage());
    }

    System.out.println();
    System.out.println("Passed: " + passed);
    System.out.println("Failed: " + failed);
    if (failed == 0) {
      System.out.println("ALL TESTS PASSED");
    } else {
      System.out.println("SOME TESTS FAILED");
      System.exit(1);
    }
  }

  private static void runAll() {
    IdGenerator.getInstance().resetForTests();
    testValidationEdgeCases();
    testPerPrefixIdGenerator();
    IdGenerator.getInstance().resetForTests();
    testDoctorCrudSearchAndSearchableDefault();
    testPatientCrudAndOverloads();
    testAddressEncapsulationAndPatientClone();
    testDataStoreContracts();
    testAppointmentCloneAndLifecycle();
    testReschedulingAndConflictDetection();
    testSafeDeletionReferentialIntegrity();
    testBillingStrategyFactoryAndBillFactory();
    testBillSummaryImmutabilityAndEquality();
    testPersistenceWithBills();
    testCsvTrailingEmptyFieldsAndFallbackReporting();
    testObserverNotificationAndCopyOnWriteSafety();
    testReminderCleanup();
    testAutosaveWritesFiles();
    testStreamsAnalytics();
    testAIHelper();
  }

  private static void testValidationEdgeCases() {
    assertEquals(
        "indian phone bare 10-digit",
        "9876543210",
        Validator.requirePhoneLike("9876543210"));
    assertEquals(
        "indian phone +91 normalization",
        "9876543210",
        Validator.requirePhoneLike("+919876543210"));
    assertThrows("reject non-indian phone", () -> Validator.requirePhoneLike("+1 9999999999"));
    assertThrows("reject invalid age", () -> Validator.requireAge(121));
    assertThrows("reject invalid email", () -> Validator.requireEmailLike("invalid-email"));
    assertThrows("reject invalid percent", () -> Validator.requirePercent("coverage", 101.0));
    assertThrows("reject blank address line", () -> new Address(" ", "City", "State", "00001"));
  }

  private static void testPerPrefixIdGenerator() {
    IdGenerator generator = IdGenerator.getInstance();
    generator.resetForTests();

    assertEquals("doc ids use independent counter", "DOC-0001", generator.nextId("DOC"));
    assertEquals("doc ids increment independently", "DOC-0002", generator.nextId("DOC"));
    assertEquals("patient ids start at 1", "PAT-0001", generator.nextId("PAT"));
    assertEquals("appointment ids start at 1", "APT-0001", generator.nextId("APT"));
    assertEquals("bill ids start at 1", "BIL-0001", generator.nextId("BIL"));

    generator.seedFromExistingIds(List.of("DOC-0009", "PAT-0003", "APT-0007", "BIL-0004"));
    assertEquals("doc reseed works per prefix", "DOC-0010", generator.nextId("DOC"));
    assertEquals("patient reseed works per prefix", "PAT-0004", generator.nextId("PAT"));
    assertEquals("appointment reseed works per prefix", "APT-0008", generator.nextId("APT"));
    assertEquals("bill reseed works per prefix", "BIL-0005", generator.nextId("BIL"));
  }

  private static void testDoctorCrudSearchAndSearchableDefault() {
    TestEnvironment env = newEnvironment();
    Doctor doctorOne =
        env.doctorService()
            .createDoctor(
                "Alice Doctor",
                40,
                "+919876543210",
                "alice@clinic.com",
                Specialization.CARDIOLOGIST,
                500,
                30);
    Doctor doctorTwo =
        env.doctorService()
            .createDoctor(
                "Bob Doctor",
                45,
                "9123456789",
                "bob@clinic.com",
                Specialization.DERMATOLOGIST,
                300,
                20);

    assertEquals("doctor list size", 2, env.doctorService().listDoctors().size());
    assertEquals("doctor search by name", 1, env.doctorService().searchByName("alice").size());
    assertEquals(
        "doctor search by specialization",
        1,
        env.doctorService().searchBySpecialization(Specialization.DERMATOLOGIST).size());
    assertEquals("searchable default normalize", "mixed", env.doctorService().normalize("  Mixed  "));

    env.doctorService().updateDoctor(doctorOne.getId(), "Alice D", null, null, null, null, 550.0, null);
    assertEquals(
        "doctor updated fee",
        550.0,
        env.doctorService().getDoctor(doctorOne.getId()).getConsultationFee(),
        0.0001);

    List<Doctor> sorted = env.doctorService().sortByFeeThenName();
    assertEquals("doctor sort by fee", doctorTwo.getId(), sorted.get(0).getId());
  }

  private static void testPatientCrudAndOverloads() {
    TestEnvironment env = newEnvironment();
    Patient patient =
        env.patientService()
            .createPatient(
                "Charlie Patient",
                30,
                "9234567890",
                "charlie@clinic.com",
                false,
                0,
                new Address("Line1", "City", "State", "00001"),
                List.of("dust"));

    assertTrue("patient list size", env.patientService().listPatients().size() >= 1);
    assertTrue("search patient by id", env.patientService().searchPatient(patient.getId()) != null);
    assertEquals(
        "search patient by name", 1, env.patientService().searchPatient("charlie", true).size());
    assertEquals("search patient by age", 1, env.patientService().searchPatient(30).size());

    Patient updated =
        env.patientService()
            .updatePatient(
                patient.getId(),
                "Charlie Updated",
                31,
                null,
                null,
                true,
                75.0,
                new Address("Line2", "City", "State", "00002"),
                List.of("dust", "pollen"));
    assertEquals("patient update insured flag", true, updated.isInsured());
    assertEquals("patient update address", "Line2, City, State 00002", updated.getAddress().toString());
  }

  private static void testAddressEncapsulationAndPatientClone() {
    TestEnvironment env = newEnvironment();
    Address sourceAddress = new Address("12 MG Road", "Bengaluru", "Karnataka", "560001");
    Patient patient =
        env.patientService()
            .createPatient(
                "Immutable Patient",
                28,
                "9345678901",
                "immutable@clinic.com",
                false,
                0,
                sourceAddress,
                List.of("dust"));

    assertTrue("address class is final", Modifier.isFinal(Address.class.getModifiers()));
    assertTrue(
        "address exposes no setters",
        Arrays.stream(Address.class.getMethods())
            .map(Method::getName)
            .noneMatch(name -> name.startsWith("set")));
    assertTrue("patient stores defensive address copy", patient.getAddress() != sourceAddress);

    Patient cloned = patient.clone();
    cloned.addAllergy("new-allergy");
    assertTrue("patient deep clone list", !patient.getAllergies().contains("new-allergy"));
    assertTrue("patient clone address copy", cloned.getAddress() != patient.getAddress());
    assertEquals("patient clone address equality", patient.getAddress(), cloned.getAddress());
  }

  private static void testDataStoreContracts() {
    DataStore<Doctor> doctors = new DataStore<>();
    DoctorService service = new DoctorService(doctors);
    service.createDoctor(
        "Store One", 38, "9456789012", "store1@clinic.com", Specialization.ENT, 400, 25);
    service.createDoctor(
        "Store Two", 44, "9567890123", "store2@clinic.com", Specialization.NEUROLOGIST, 700, 30);

    int iteratedCount = 0;
    for (Doctor ignored : doctors) {
      iteratedCount++;
    }
    assertEquals("datastore iterable count", doctors.size(), iteratedCount);
    assertEquals(
        "datastore predicate find", 1, doctors.find(d -> d.getConsultationFee() >= 700.0).size());
  }

  private static void testAppointmentCloneAndLifecycle() {
    TestEnvironment env = newEnvironment();
    Doctor doctor = createDoctor(env, "Lifecycle Doc", 450.0, Specialization.GENERAL_PHYSICIAN);
    Patient patient = createPatient(env, "Lifecycle Pat", 24, false, 0.0);

    Appointment pending =
        env.appointmentService()
            .createPendingAppointment(
                patient.getId(),
                doctor.getId(),
                fixedTime(10),
                30,
                List.of("fever"));
    assertEquals("pending appointment created", AppointmentStatus.PENDING, pending.getStatus());

    env.appointmentService().confirmAppointment(pending.getId());
    assertEquals(
        "pending appointment confirmed",
        AppointmentStatus.CONFIRMED,
        env.appointmentService().getAppointment(pending.getId()).getStatus());

    Appointment cloned = pending.clone();
    cloned.addSymptom("fatigue");
    assertTrue("appointment deep clone list", !pending.getSymptoms().contains("fatigue"));

    env.appointmentService().completeAppointment(pending.getId());
    assertEquals(
        "appointment completed",
        AppointmentStatus.COMPLETED,
        env.appointmentService().getAppointment(pending.getId()).getStatus());
    assertThrows(
        "completed appointment cannot be cancelled",
        () -> env.appointmentService().cancelAppointment(pending.getId()));

    Appointment pendingForInvalidTransition =
        env.appointmentService()
            .createPendingAppointment(
                patient.getId(),
                doctor.getId(),
                fixedTime(70),
                30,
                List.of("checkup"));
    assertThrows(
        "pending appointment cannot be completed directly",
        () -> env.appointmentService().completeAppointment(pendingForInvalidTransition.getId()));

    Appointment cancellable =
        env.appointmentService()
            .createAppointment(
                patient.getId(),
                doctor.getId(),
                fixedTime(130),
                30,
                List.of("cold"));
    env.appointmentService().cancelAppointment(cancellable.getId());
    assertThrows(
        "duplicate cancellation rejected",
        () -> env.appointmentService().cancelAppointment(cancellable.getId()));
    assertThrows(
        "cancelled appointment cannot be completed",
        () -> env.appointmentService().completeAppointment(cancellable.getId()));
  }

  private static void testReschedulingAndConflictDetection() {
    TestEnvironment env = newEnvironment();
    Doctor doctor = createDoctor(env, "Schedule Doc", 600.0, Specialization.CARDIOLOGIST);
    Patient patientOne = createPatient(env, "Patient One", 31, false, 0.0);
    Patient patientTwo = createPatient(env, "Patient Two", 33, false, 0.0);

    Appointment first =
        env.appointmentService()
            .createAppointment(patientOne.getId(), doctor.getId(), fixedTime(20), 30, List.of("bp"));
    Appointment second =
        env.appointmentService()
            .createAppointment(patientTwo.getId(), doctor.getId(), fixedTime(80), 30, List.of("pain"));

    assertThrows(
        "appointment conflict detection",
        () ->
            env.appointmentService()
                .createAppointment(patientTwo.getId(), doctor.getId(), fixedTime(35), 30, List.of()));

    Appointment rescheduled =
        env.appointmentService().rescheduleAppointment(first.getId(), fixedTime(140), 45);
    assertEquals("appointment rescheduled start", fixedTime(140), rescheduled.getStartTime());
    assertEquals("appointment rescheduled duration", 45, rescheduled.getDurationMinutes());
    assertEquals("appointment symptoms preserved", List.of("bp"), rescheduled.getSymptoms());

    assertThrows(
        "reschedule respects doctor availability",
        () -> env.appointmentService().rescheduleAppointment(first.getId(), second.getStartTime(), 30));

    env.appointmentService().cancelAppointment(first.getId());
    assertThrows(
        "cancelled appointment cannot be rescheduled",
        () -> env.appointmentService().rescheduleAppointment(first.getId(), fixedTime(200), 30));
  }

  private static void testSafeDeletionReferentialIntegrity() {
    TestEnvironment env = newEnvironment();
    Doctor doctor = createDoctor(env, "Delete Doc", 500.0, Specialization.ENT);
    Patient patient = createPatient(env, "Delete Pat", 27, false, 0.0);
    Appointment appointment =
        env.appointmentService()
            .createAppointment(patient.getId(), doctor.getId(), fixedTime(30), 30, List.of("ear pain"));

    assertThrows(
        "safe doctor delete blocked by active appointment",
        () -> env.clinicManagementService().deleteDoctorSafely(doctor.getId()));
    assertThrows(
        "safe patient delete blocked by active appointment",
        () -> env.clinicManagementService().deletePatientSafely(patient.getId()));

    env.appointmentService().cancelAppointment(appointment.getId());
    assertTrue("safe doctor delete succeeds after cancellation", env.clinicManagementService().deleteDoctorSafely(doctor.getId()));
    assertTrue("safe patient delete succeeds after cancellation", env.clinicManagementService().deletePatientSafely(patient.getId()));
  }

  private static void testBillingStrategyFactoryAndBillFactory() {
    TestEnvironment env = newEnvironment();
    Doctor doctor = createDoctor(env, "Bill Doc", 1000.0, Specialization.DERMATOLOGIST);
    Patient standard = createPatient(env, "Standard Pat", 35, false, 0.0);
    Patient insured = createPatient(env, "Insured Pat", 36, true, 80.0);
    Patient senior = createPatient(env, "Senior Pat", 70, false, 0.0);

    Appointment standardAppointment =
        env.appointmentService()
            .createAppointment(standard.getId(), doctor.getId(), fixedTime(40), 30, List.of("checkup"));
    Appointment insuredAppointment =
        env.appointmentService()
            .createAppointment(insured.getId(), doctor.getId(), fixedTime(90), 30, List.of("rash"));
    Appointment seniorAppointment =
        env.appointmentService()
            .createAppointment(senior.getId(), doctor.getId(), fixedTime(140), 30, List.of("cold"));

    assertTrue(
        "standard strategy selected",
        BillingStrategyFactory.forPatient(standard) instanceof StandardBillingStrategy);
    assertTrue(
        "insurance strategy selected",
        BillingStrategyFactory.forPatient(insured) instanceof InsuranceBillingStrategy);
    assertTrue(
        "senior strategy selected",
        BillingStrategyFactory.forPatient(senior) instanceof SeniorDiscountBillingStrategy);

    Bill preview =
        BillFactory.createBill(
            BillType.STANDARD,
            "BIL-PREVIEW",
            standardAppointment,
            standard,
            doctor,
            50.0,
            "preview");
    assertEquals("bill factory sets type", BillType.STANDARD, preview.getType());
    assertEquals("bill factory sets base", 1000.0, preview.getBaseAmount(), 0.0001);
    assertEquals("bill factory sets discount", 50.0, preview.getDiscountAmount(), 0.0001);

    Bill standardBill = env.billingService().generateBill(standardAppointment.getId());
    Bill insuredBill = env.billingService().generateBill(insuredAppointment.getId());
    Bill seniorBill = env.billingService().generateBill(seniorAppointment.getId());

    assertEquals("standard bill type", BillType.STANDARD, standardBill.getType());
    assertEquals("standard discount", 0.0, standardBill.getDiscountAmount(), 0.0001);
    assertEquals("insurance bill type", BillType.INSURANCE, insuredBill.getType());
    assertEquals("insurance discount", 800.0, insuredBill.getDiscountAmount(), 0.0001);
    assertEquals("senior bill type", BillType.SENIOR_DISCOUNT, seniorBill.getType());
    assertEquals("senior discount", 100.0, seniorBill.getDiscountAmount(), 0.0001);
  }

  private static void testBillSummaryImmutabilityAndEquality() {
    assertTrue("bill summary class is final", Modifier.isFinal(BillSummary.class.getModifiers()));
    assertTrue(
        "bill summary implements serializable",
        Serializable.class.isAssignableFrom(BillSummary.class));
    assertTrue(
        "bill summary fields are final",
        Arrays.stream(BillSummary.class.getDeclaredFields())
            .filter(field -> !Modifier.isStatic(field.getModifiers()))
            .allMatch(field -> Modifier.isFinal(field.getModifiers())));
    assertTrue(
        "bill summary exposes no setters",
        Arrays.stream(BillSummary.class.getMethods())
            .map(Method::getName)
            .noneMatch(name -> name.startsWith("set")));

    LocalDateTime generatedAt = fixedTime(300);
    BillSummary summaryOne =
        new BillSummary(
            "BIL-0001",
            BillType.STANDARD,
            "APT-0001",
            "PAT-0001",
            "DOC-0001",
            1000.0,
            100.0,
            162.0,
            1062.0,
            "summary",
            generatedAt);
    BillSummary summaryTwo =
        new BillSummary(
            "BIL-0001",
            BillType.STANDARD,
            "APT-0001",
            "PAT-0001",
            "DOC-0001",
            1000.0,
            100.0,
            162.0,
            1062.0,
            "summary",
            generatedAt);

    assertEquals("bill summary equality", summaryOne, summaryTwo);
    assertEquals("bill summary hashcode equality", summaryOne.hashCode(), summaryTwo.hashCode());
  }

  private static void testPersistenceWithBills() {
    Path tempDir = tempDir();
    try {
      TestEnvironment env = newEnvironment();
      Doctor doctor = createDoctor(env, "Persist Doc", 850.0, Specialization.NEUROLOGIST);
      Patient patient = createPatient(env, "Persist Pat", 48, true, 60.0);
      Appointment appointment =
          env.appointmentService()
              .createAppointment(patient.getId(), doctor.getId(), fixedTime(50), 30, List.of("headache"));
      Bill bill = env.billingService().generateBill(appointment.getId());
      double beforeTotal = bill.getTotalAmount();

      PersistenceManager.saveAll(
          tempDir, env.doctors(), env.patients(), env.appointments(), env.bills());
      assertTrue("bill csv created", Files.exists(tempDir.resolve(Constants.BILLS_CSV)));
      assertTrue("bill serialization created", Files.exists(tempDir.resolve(Constants.BILLS_SER)));

      env.doctors().clear();
      env.patients().clear();
      env.appointments().clear();
      env.bills().clear();

      List<PersistenceReport> reports =
          PersistenceManager.loadAllWithReport(
              tempDir, env.doctors(), env.patients(), env.appointments(), env.bills());
      assertEquals("reports include bills", 4, reports.size());
      assertEquals("bill count preserved", 1, env.bills().size());
      assertEquals("bill total preserved", beforeTotal, env.bills().getAll().get(0).getTotalAmount(), 0.0001);
    } finally {
      deleteRecursively(tempDir);
    }
  }

  private static void testCsvTrailingEmptyFieldsAndFallbackReporting() {
    Path tempDir = tempDir();
    try {
      TestEnvironment env = newEnvironment();
      Doctor doctor = createDoctor(env, "CSV Doc", 650.0, Specialization.GENERAL_PHYSICIAN);
      Patient patient =
          env.patientService()
              .createPatient(
                  "CSV Pat",
                  29,
                  "9678901234",
                  "csv@clinic.com",
                  false,
                  0.0,
                  new Address("L1", "City", "State", "12345"),
                  List.of());
      env.appointmentService()
          .createAppointment(patient.getId(), doctor.getId(), fixedTime(60), 30, List.of());

      PersistenceManager.saveAll(tempDir, env.doctors(), env.patients(), env.appointments());
      Files.deleteIfExists(tempDir.resolve(Constants.DOCTORS_SER));
      Files.deleteIfExists(tempDir.resolve(Constants.PATIENTS_SER));
      Files.deleteIfExists(tempDir.resolve(Constants.APPOINTMENTS_SER));

      env.doctors().clear();
      env.patients().clear();
      env.appointments().clear();

      List<PersistenceReport> csvReports =
          PersistenceManager.loadAllWithReport(tempDir, env.doctors(), env.patients(), env.appointments());
      assertEquals("csv fallback loads doctors", 1, env.doctors().size());
      assertEquals("csv fallback loads patients", 1, env.patients().size());
      assertEquals("csv fallback loads appointments", 1, env.appointments().size());
      assertTrue(
          "csv fallback report source",
          csvReports.stream().allMatch(report -> report.getSource() == PersistenceReport.Source.CSV));
      assertTrue("empty allergies preserved", env.patients().getAll().get(0).getAllergies().isEmpty());
      assertTrue("empty symptoms preserved", env.appointments().getAll().get(0).getSymptoms().isEmpty());

      PersistenceManager.saveAll(tempDir, env.doctors(), env.patients(), env.appointments());
      Files.writeString(tempDir.resolve(Constants.PATIENTS_SER), "corrupt-ser-data");
      env.doctors().clear();
      env.patients().clear();
      env.appointments().clear();

      List<PersistenceReport> reportsWithFallback =
          PersistenceManager.loadAllWithReport(tempDir, env.doctors(), env.patients(), env.appointments());
      PersistenceReport patientReport =
          reportsWithFallback.stream()
              .filter(report -> "Patients".equals(report.getEntityName()))
              .findFirst()
              .orElseThrow();
      assertEquals("corrupted patient ser falls back to csv", PersistenceReport.Source.CSV, patientReport.getSource());
      assertTrue("corrupted ser fallback warning reported", patientReport.hasWarnings());
    } catch (Exception e) {
      fail("csv fallback test failed: " + e.getMessage());
    } finally {
      deleteRecursively(tempDir);
    }
  }

  private static void testObserverNotificationAndCopyOnWriteSafety() {
    TestEnvironment env = newEnvironment();
    Doctor doctor = createDoctor(env, "Observer Doc", 500.0, Specialization.ENT);
    Patient patient = createPatient(env, "Observer Pat", 42, false, 0.0);

    List<AppointmentEventType> events = new ArrayList<>();
    AtomicBoolean snapshotImmutable = new AtomicBoolean(false);
    AtomicInteger selfRemovingCount = new AtomicInteger(0);

    AppointmentObserver recordingObserver =
        event -> {
          events.add(event.getType());
          AppointmentSnapshot snapshot = event.getAppointment();
          try {
            snapshot.getSymptoms().add("mutate");
          } catch (UnsupportedOperationException e) {
            snapshotImmutable.set(true);
          }
        };
    AppointmentObserver selfRemovingObserver =
        new AppointmentObserver() {
          @Override
          public void onEvent(AppointmentEvent event) {
            selfRemovingCount.incrementAndGet();
            env.appointmentService().removeObserver(this);
            env.appointmentService().addObserver(ignored -> {});
          }
        };

    env.appointmentService().addObserver(recordingObserver);
    env.appointmentService().addObserver(selfRemovingObserver);

    Appointment appointment =
        env.appointmentService()
            .createPendingAppointment(
                patient.getId(), doctor.getId(), fixedTime(70), 30, List.of("ear pain"));
    env.appointmentService().confirmAppointment(appointment.getId());
    env.appointmentService().rescheduleAppointment(appointment.getId(), fixedTime(120), 30);
    env.appointmentService().cancelAppointment(appointment.getId());

    assertEquals(
        "observer event order",
        List.of(
            AppointmentEventType.CREATED,
            AppointmentEventType.CONFIRMED,
            AppointmentEventType.RESCHEDULED,
            AppointmentEventType.CANCELLED),
        events);
    assertTrue("observer snapshot immutable", snapshotImmutable.get());
    assertEquals("self removing observer handled once", 1, selfRemovingCount.get());
  }

  private static void testReminderCleanup() {
    TestEnvironment env = newEnvironment();
    ReminderSchedulerObserver reminder = new ReminderSchedulerObserver(0);
    env.appointmentService().addObserver(reminder);
    try {
      Doctor doctor = createDoctor(env, "Reminder Doc", 350.0, Specialization.PEDIATRICIAN);
      Patient patient = createPatient(env, "Reminder Pat", 19, false, 0.0);

      Appointment appointment =
          env.appointmentService()
              .createAppointment(
                  patient.getId(),
                  doctor.getId(),
                  LocalDateTime.now().plusSeconds(1),
                  1,
                  List.of("follow up"));
      assertTrue("reminder scheduled", reminder.isScheduled(appointment.getId()));
      waitForCondition(
          "reminder scheduled count decreases after firing",
          () -> reminder.getScheduledCount() == 0,
          4000L);
    } finally {
      reminder.close();
      env.appointmentService().removeObserver(reminder);
    }
  }

  private static void testAutosaveWritesFiles() {
    Path tempDir = tempDir();
    AutoSaveService autoSave = null;
    try {
      TestEnvironment env = newEnvironment();
      Doctor doctor = createDoctor(env, "AutoSave Doc", 720.0, Specialization.ORTHOPEDIC);
      Patient patient = createPatient(env, "AutoSave Pat", 55, true, 50.0);
      Appointment appointment =
          env.appointmentService()
              .createAppointment(patient.getId(), doctor.getId(), fixedTime(75), 30, List.of("knee pain"));
      env.billingService().generateBill(appointment.getId());

      autoSave =
          new AutoSaveService(
              tempDir, env.doctors(), env.patients(), env.appointments(), env.bills(), 1);
      autoSave.start();
      Thread.sleep(1300L);
      autoSave.requestStop();
      autoSave.join(3000L);

      assertTrue("autosave doctors csv", Files.exists(tempDir.resolve(Constants.DOCTORS_CSV)));
      assertTrue("autosave patients csv", Files.exists(tempDir.resolve(Constants.PATIENTS_CSV)));
      assertTrue("autosave appointments csv", Files.exists(tempDir.resolve(Constants.APPOINTMENTS_CSV)));
      assertTrue("autosave bills csv", Files.exists(tempDir.resolve(Constants.BILLS_CSV)));
    } catch (Exception e) {
      fail("autosave test failed: " + e.getMessage());
    } finally {
      if (autoSave != null && autoSave.isAlive()) {
        autoSave.requestStop();
      }
      deleteRecursively(tempDir);
    }
  }

  private static void testStreamsAnalytics() {
    TestEnvironment env = newEnvironment();
    Doctor doctorOne = createDoctor(env, "Analytics One", 400.0, Specialization.GENERAL_PHYSICIAN);
    Doctor doctorTwo = createDoctor(env, "Analytics Two", 800.0, Specialization.CARDIOLOGIST);
    Patient patientOne = createPatient(env, "Analytics Pat 1", 30, false, 0.0);
    Patient patientTwo = createPatient(env, "Analytics Pat 2", 31, false, 0.0);

    env.appointmentService()
        .createAppointment(patientOne.getId(), doctorOne.getId(), fixedTime(85), 30, List.of("fever"));
    env.appointmentService()
        .createAppointment(patientTwo.getId(), doctorTwo.getId(), fixedTime(135), 30, List.of("bp"));

    assertEquals("average fee calculation", 600.0, env.doctorService().averageConsultationFee(), 0.0001);

    long groupedCount =
        env.appointmentService().listAppointments().stream()
            .collect(Collectors.groupingBy(Appointment::getStatus, Collectors.counting()))
            .values()
            .stream()
            .mapToLong(Long::longValue)
            .sum();
    assertEquals("appointments grouping count", env.appointments().size(), groupedCount);
  }

  private static void testAIHelper() {
    TestEnvironment env = newEnvironment();
    Doctor dermatologist = createDoctor(env, "Derm Doc", 900.0, Specialization.DERMATOLOGIST);
    Doctor general = createDoctor(env, "General Doc", 500.0, Specialization.GENERAL_PHYSICIAN);
    Patient patient = createPatient(env, "AI Pat", 26, false, 0.0);

    env.appointmentService()
        .createAppointment(patient.getId(), general.getId(), fixedTime(95), 30, List.of("fever"));

    assertEquals(
        "ai inferred specialization",
        Specialization.DERMATOLOGIST,
        AIHelper.inferSpecialization(List.of("skin rash")));

    List<Doctor> recommendations =
        AIHelper.recommendDoctors(
            List.of("skin rash"),
            env.doctorService().listDoctors(),
            env.appointmentService().listAppointments(),
            fixedTime(100),
            2);
    assertTrue("ai recommendations non-empty", !recommendations.isEmpty());
    assertEquals("ai dermatology recommendation first", dermatologist.getId(), recommendations.get(0).getId());
    assertTrue(
        "ai slot suggestions non-empty",
        !AIHelper.suggestSlots(
                dermatologist,
                env.appointmentService().listAppointments(),
                fixedTime(100),
                2)
            .isEmpty());
  }

  private static TestEnvironment newEnvironment() {
    DataStore<Doctor> doctors = new DataStore<>();
    DataStore<Patient> patients = new DataStore<>();
    DataStore<Appointment> appointments = new DataStore<>();
    DataStore<Bill> bills = new DataStore<>();
    DoctorService doctorService = new DoctorService(doctors);
    PatientService patientService = new PatientService(patients);
    AppointmentService appointmentService = new AppointmentService(appointments, doctorService, patientService);
    BillingService billingService = new BillingService(bills, appointmentService, doctorService, patientService);
    ClinicManagementService clinicManagementService =
        new ClinicManagementService(doctorService, patientService, appointmentService);
    return new TestEnvironment(
        doctors,
        patients,
        appointments,
        bills,
        doctorService,
        patientService,
        appointmentService,
        billingService,
        clinicManagementService);
  }

  private static Doctor createDoctor(
      TestEnvironment env, String name, double fee, Specialization specialization) {
    int suffix = env.doctors().size() + 1;
    return env.doctorService()
        .createDoctor(
            name,
            40 + suffix,
            "98" + String.format("%08d", suffix),
            "doctor" + suffix + "@clinic.com",
            specialization,
            fee,
            30);
  }

  private static Patient createPatient(
      TestEnvironment env, String name, int age, boolean insured, double coverage) {
    int suffix = env.patients().size() + 1;
    return env.patientService()
        .createPatient(
            name,
            age,
            "97" + String.format("%08d", suffix),
            "patient" + suffix + "@clinic.com",
            insured,
            coverage,
            new Address("Address " + suffix, "City", "State", "1000" + suffix),
            List.of("dust"));
  }

  private static LocalDateTime fixedTime(int minuteOffset) {
    return LocalDateTime.of(2030, 1, 15, 10, 0).plusMinutes(minuteOffset);
  }

  private static Path tempDir() {
    return Path.of(Constants.DATA_DIR, "test-" + UUID.randomUUID());
  }

  private static void waitForCondition(String name, Check condition, long timeoutMillis) {
    long deadline = System.currentTimeMillis() + timeoutMillis;
    try {
      while (System.currentTimeMillis() < deadline) {
        if (condition.evaluate()) {
          pass(name);
          return;
        }
        Thread.sleep(100L);
      }
      fail(name);
    } catch (Exception e) {
      fail(name + " (" + e.getMessage() + ")");
    }
  }

  private static void deleteRecursively(Path path) {
    if (path == null || !Files.exists(path)) {
      return;
    }
    try (var walk = Files.walk(path)) {
      walk.sorted((a, b) -> b.compareTo(a))
          .forEach(
              current -> {
                try {
                  Files.deleteIfExists(current);
                } catch (Exception ignored) {
                }
              });
    } catch (Exception ignored) {
    }
  }

  private static void assertTrue(String name, boolean condition) {
    if (condition) {
      pass(name);
    } else {
      fail(name);
    }
  }

  private static void assertThrows(String name, ThrowingRunnable action) {
    try {
      action.run();
      fail(name);
    } catch (Exception ignored) {
      pass(name);
    }
  }

  private static void assertEquals(String name, Object expected, Object actual) {
    if (java.util.Objects.equals(expected, actual)) {
      pass(name);
    } else {
      fail(name + " (expected=" + expected + ", actual=" + actual + ")");
    }
  }

  private static void assertEquals(String name, int expected, int actual) {
    if (expected == actual) {
      pass(name);
    } else {
      fail(name + " (expected=" + expected + ", actual=" + actual + ")");
    }
  }

  private static void assertEquals(String name, long expected, long actual) {
    if (expected == actual) {
      pass(name);
    } else {
      fail(name + " (expected=" + expected + ", actual=" + actual + ")");
    }
  }

  private static void assertEquals(String name, double expected, double actual, double eps) {
    if (Math.abs(expected - actual) <= eps) {
      pass(name);
    } else {
      fail(name + " (expected=" + expected + ", actual=" + actual + ")");
    }
  }

  private static void pass(String name) {
    passed++;
    System.out.println("[PASS] " + name);
  }

  private static void fail(String name) {
    failed++;
    System.out.println("[FAIL] " + name);
  }

  @FunctionalInterface
  private interface ThrowingRunnable {
    void run() throws Exception;
  }

  @FunctionalInterface
  private interface Check {
    boolean evaluate() throws Exception;
  }

  private record TestEnvironment(
      DataStore<Doctor> doctors,
      DataStore<Patient> patients,
      DataStore<Appointment> appointments,
      DataStore<Bill> bills,
      DoctorService doctorService,
      PatientService patientService,
      AppointmentService appointmentService,
      BillingService billingService,
      ClinicManagementService clinicManagementService) {}
}
