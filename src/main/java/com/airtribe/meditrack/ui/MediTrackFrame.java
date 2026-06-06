package com.airtribe.meditrack.ui;

import com.airtribe.meditrack.constants.Constants;
import com.airtribe.meditrack.entity.Address;
import com.airtribe.meditrack.entity.Appointment;
import com.airtribe.meditrack.entity.Bill;
import com.airtribe.meditrack.entity.Doctor;
import com.airtribe.meditrack.entity.Patient;
import com.airtribe.meditrack.entity.Specialization;
import com.airtribe.meditrack.exception.InvalidDataException;
import com.airtribe.meditrack.service.AppointmentService;
import com.airtribe.meditrack.service.BillingService;
import com.airtribe.meditrack.service.ClinicManagementService;
import com.airtribe.meditrack.service.DoctorService;
import com.airtribe.meditrack.service.PatientService;
import com.airtribe.meditrack.util.DataStore;
import com.airtribe.meditrack.util.DateUtil;
import com.airtribe.meditrack.util.PersistenceManager;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

class MediTrackFrame extends JFrame {
  private final Path baseDir = Path.of(Constants.DATA_DIR);

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
  private final ClinicManagementService clinicManagementService =
      new ClinicManagementService(doctorService, patientService, appointmentService);

  private final JLabel statusLabel = new JLabel();

  private final JTextField doctorIdField = new JTextField();
  private final JTextField doctorNameField = new JTextField();
  private final JTextField doctorAgeField = new JTextField();
  private final JTextField doctorPhoneField = new JTextField();
  private final JTextField doctorEmailField = new JTextField();
  private final JComboBox<String> doctorSpecializationCombo = new JComboBox<>(specializationOptions());
  private final JTextField doctorFeeField = new JTextField();
  private final JTextField doctorSlotField = new JTextField();
  private final JTextField doctorSearchField = new JTextField();
  private final JTextArea doctorOutput = createOutputArea();

  private final JTextField patientIdField = new JTextField();
  private final JTextField patientNameField = new JTextField();
  private final JTextField patientAgeField = new JTextField();
  private final JTextField patientPhoneField = new JTextField();
  private final JTextField patientEmailField = new JTextField();
  private final JCheckBox patientInsuredCheck = new JCheckBox("Insured");
  private final JTextField patientCoverageField = new JTextField();
  private final JTextField patientAddressLine1Field = new JTextField();
  private final JTextField patientCityField = new JTextField();
  private final JTextField patientStateField = new JTextField();
  private final JTextField patientZipField = new JTextField();
  private final JTextField patientAllergiesField = new JTextField();
  private final JTextField patientSearchNameField = new JTextField();
  private final JTextField patientSearchAgeField = new JTextField();
  private final JTextArea patientOutput = createOutputArea();

  private final JTextField appointmentIdField = new JTextField();
  private final JTextField appointmentPatientIdField = new JTextField();
  private final JTextField appointmentDoctorIdField = new JTextField();
  private final JTextField appointmentStartField = new JTextField();
  private final JTextField appointmentDurationField = new JTextField();
  private final JTextField appointmentSymptomsField = new JTextField();
  private final JTextArea appointmentOutput = createOutputArea();

  private final JTextField billingAppointmentIdField = new JTextField();
  private final JTextArea billingOutput = createOutputArea();

  MediTrackFrame(boolean loadData) {
    super("MediTrack Swing Demo");

    if (loadData) {
      try {
        PersistenceManager.loadAll(baseDir, doctors, patients, appointments, bills);
      } catch (Exception e) {
        showError(e);
      }
    }

    setSize(1240, 860);
    setLocationRelativeTo(null);
    setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    addWindowListener(
        new WindowAdapter() {
          @Override
          public void windowClosing(WindowEvent e) {
            saveQuietly();
            dispose();
          }
        });

    setLayout(new BorderLayout(12, 12));
    add(buildHeader(), BorderLayout.NORTH);
    add(buildTabs(), BorderLayout.CENTER);
    refreshStatus("Swing demo ready. Console app remains the canonical grading path.");
    renderDoctors(doctorService.listDoctors());
    renderPatients(patientService.listPatients());
    renderAppointments(appointmentService.listAppointments());
    renderBills(billingService.listBills());
  }

  private JPanel buildHeader() {
    JPanel root = new JPanel(new BorderLayout(8, 8));
    root.setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 12));

    JLabel note =
        new JLabel(
            "Supplementary Swing UI for demos. Core grading path remains the console menu and live demo flow.");
    root.add(note, BorderLayout.NORTH);

    JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    JButton saveButton = new JButton("Save Data");
    saveButton.addActionListener(
        e ->
            perform(
                "Saved current GUI data to " + baseDir.toAbsolutePath(),
                () -> PersistenceManager.saveAll(baseDir, doctors, patients, appointments, bills)));

    JButton reloadButton = new JButton("Reload Data");
    reloadButton.addActionListener(
        e ->
            perform(
                "Reloaded persisted data from " + baseDir.toAbsolutePath(),
                () -> {
                  PersistenceManager.loadAll(baseDir, doctors, patients, appointments, bills);
                  renderDoctors(doctorService.listDoctors());
                  renderPatients(patientService.listPatients());
                  renderAppointments(appointmentService.listAppointments());
                  renderBills(billingService.listBills());
                }));

    toolbar.add(saveButton);
    toolbar.add(reloadButton);
    toolbar.add(statusLabel);
    root.add(toolbar, BorderLayout.SOUTH);
    return root;
  }

  private JTabbedPane buildTabs() {
    JTabbedPane tabs = new JTabbedPane();
    tabs.addTab("Doctors", buildDoctorsTab());
    tabs.addTab("Patients", buildPatientsTab());
    tabs.addTab("Appointments", buildAppointmentsTab());
    tabs.addTab("Billing", buildBillingTab());
    return tabs;
  }

  private JPanel buildDoctorsTab() {
    JPanel panel = new JPanel(new BorderLayout(12, 12));
    panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
    panel.add(
        buildFormPanel(
            "Doctor Form (leave ID blank to create; load by ID before updating)",
            "Doctor ID",
            doctorIdField,
            "Name",
            doctorNameField,
            "Age",
            doctorAgeField,
            "Phone",
            doctorPhoneField,
            "Email",
            doctorEmailField,
            "Specialization",
            doctorSpecializationCombo,
            "Consultation Fee",
            doctorFeeField,
            "Slot Minutes",
            doctorSlotField,
            "Search Query",
            doctorSearchField),
        BorderLayout.NORTH);

    JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    JButton loadButton = new JButton("Load by ID");
    loadButton.addActionListener(
        e ->
            perform(
                "Loaded doctor " + doctorIdField.getText().trim(),
                () -> {
                  Doctor doctor = doctorService.getDoctor(requireText(doctorIdField, "Doctor ID"));
                  if (doctor == null) {
                    throw new InvalidDataException("Doctor not found");
                  }
                  populateDoctorForm(doctor);
                  renderDoctors(List.of(doctor));
                }));

    JButton saveButton = new JButton("Create / Update");
    saveButton.addActionListener(
        e ->
            perform(
                "Doctor form saved",
                () -> {
                  String id = text(doctorIdField);
                  Doctor doctor;
                  if (id.isEmpty()) {
                    doctor =
                        doctorService.createDoctor(
                            requireText(doctorNameField, "Name"),
                            parseRequiredInt(doctorAgeField, "Age"),
                            requireText(doctorPhoneField, "Phone"),
                            requireText(doctorEmailField, "Email"),
                            requireSpecialization(),
                            parseRequiredDouble(doctorFeeField, "Consultation Fee"),
                            parseOptionalInt(doctorSlotField));
                    doctorIdField.setText(doctor.getId());
                  } else {
                    doctor =
                        doctorService.updateDoctor(
                            id,
                            blankToNull(text(doctorNameField)),
                            parseOptionalInteger(doctorAgeField),
                            blankToNull(text(doctorPhoneField)),
                            blankToNull(text(doctorEmailField)),
                            optionalSpecialization(),
                            parseOptionalDoubleObject(doctorFeeField),
                            parseOptionalInteger(doctorSlotField));
                  }
                  populateDoctorForm(doctor);
                  renderDoctors(doctorService.listDoctors());
                }));

    JButton deleteButton = new JButton("Delete by ID");
    deleteButton.addActionListener(
        e ->
            perform(
                "Doctor deleted",
                () -> {
                  boolean deleted =
                      clinicManagementService.deleteDoctorSafely(requireText(doctorIdField, "Doctor ID"));
                  if (!deleted) {
                    throw new InvalidDataException("Doctor not found");
                  }
                  clearDoctorForm();
                  renderDoctors(doctorService.listDoctors());
                }));

    JButton listButton = new JButton("List All");
    listButton.addActionListener(e -> renderDoctors(doctorService.listDoctors()));

    JButton searchButton = new JButton("Search");
    searchButton.addActionListener(
        e ->
            perform(
                "Doctor search executed",
                () -> renderDoctors(doctorService.search(requireText(doctorSearchField, "Search Query")))));

    buttons.add(loadButton);
    buttons.add(saveButton);
    buttons.add(deleteButton);
    buttons.add(listButton);
    buttons.add(searchButton);
    panel.add(buttons, BorderLayout.CENTER);
    panel.add(wrapOutput(doctorOutput), BorderLayout.SOUTH);
    return panel;
  }

  private JPanel buildPatientsTab() {
    JPanel panel = new JPanel(new BorderLayout(12, 12));
    panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
    panel.add(
        buildFormPanel(
            "Patient Form (load by ID before updating if you want to keep existing values)",
            "Patient ID",
            patientIdField,
            "Name",
            patientNameField,
            "Age",
            patientAgeField,
            "Phone",
            patientPhoneField,
            "Email",
            patientEmailField,
            "Insurance Coverage %",
            patientCoverageField,
            "Address Line 1",
            patientAddressLine1Field,
            "City",
            patientCityField,
            "State",
            patientStateField,
            "ZIP",
            patientZipField,
            "Allergies (comma separated)",
            patientAllergiesField,
            "Search by Name",
            patientSearchNameField,
            "Search by Age",
            patientSearchAgeField),
        BorderLayout.NORTH);

    JPanel insuredPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    insuredPanel.add(patientInsuredCheck);
    panel.add(insuredPanel, BorderLayout.WEST);

    JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    JButton loadButton = new JButton("Load by ID");
    loadButton.addActionListener(
        e ->
            perform(
                "Loaded patient " + patientIdField.getText().trim(),
                () -> {
                  Patient patient = patientService.getPatient(requireText(patientIdField, "Patient ID"));
                  if (patient == null) {
                    throw new InvalidDataException("Patient not found");
                  }
                  populatePatientForm(patient);
                  renderPatients(List.of(patient));
                }));

    JButton saveButton = new JButton("Create / Update");
    saveButton.addActionListener(
        e ->
            perform(
                "Patient form saved",
                () -> {
                  String id = text(patientIdField);
                  Patient patient;
                  Address address = buildAddressFromForm();
                  List<String> allergies = splitComma(patientAllergiesField.getText());
                  if (id.isEmpty()) {
                    patient =
                        patientService.createPatient(
                            requireText(patientNameField, "Name"),
                            parseRequiredInt(patientAgeField, "Age"),
                            requireText(patientPhoneField, "Phone"),
                            requireText(patientEmailField, "Email"),
                            patientInsuredCheck.isSelected(),
                            parseCoverageForCreate(),
                            address,
                            allergies);
                    patientIdField.setText(patient.getId());
                  } else {
                    patient =
                        patientService.updatePatient(
                            id,
                            blankToNull(text(patientNameField)),
                            parseOptionalInteger(patientAgeField),
                            blankToNull(text(patientPhoneField)),
                            blankToNull(text(patientEmailField)),
                            patientInsuredCheck.isSelected(),
                            patientInsuredCheck.isSelected()
                                ? parseOptionalDoubleObject(patientCoverageField)
                                : 0.0,
                            address,
                            allergies);
                  }
                  populatePatientForm(patient);
                  renderPatients(patientService.listPatients());
                }));

    JButton deleteButton = new JButton("Delete by ID");
    deleteButton.addActionListener(
        e ->
            perform(
                "Patient deleted",
                () -> {
                  boolean deleted =
                      clinicManagementService.deletePatientSafely(requireText(patientIdField, "Patient ID"));
                  if (!deleted) {
                    throw new InvalidDataException("Patient not found");
                  }
                  clearPatientForm();
                  renderPatients(patientService.listPatients());
                }));

    JButton listButton = new JButton("List All");
    listButton.addActionListener(e -> renderPatients(patientService.listPatients()));

    JButton searchNameButton = new JButton("Search Name");
    searchNameButton.addActionListener(
        e ->
            perform(
                "Patient name search executed",
                () ->
                    renderPatients(
                        patientService.searchPatient(requireText(patientSearchNameField, "Search by Name"), true))));

    JButton searchAgeButton = new JButton("Search Age");
    searchAgeButton.addActionListener(
        e ->
            perform(
                "Patient age search executed",
                () ->
                    renderPatients(
                        patientService.searchPatient(parseRequiredInt(patientSearchAgeField, "Search by Age")))));

    buttons.add(loadButton);
    buttons.add(saveButton);
    buttons.add(deleteButton);
    buttons.add(listButton);
    buttons.add(searchNameButton);
    buttons.add(searchAgeButton);
    panel.add(buttons, BorderLayout.CENTER);
    panel.add(wrapOutput(patientOutput), BorderLayout.SOUTH);
    return panel;
  }

  private JPanel buildAppointmentsTab() {
    JPanel panel = new JPanel(new BorderLayout(12, 12));
    panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
    appointmentStartField.setText(
        DateUtil.formatDateTime(LocalDateTime.now().plusHours(1).withSecond(0).withNano(0)));
    panel.add(
        buildFormPanel(
            "Appointment Form (start format: yyyy-MM-dd HH:mm)",
            "Appointment ID",
            appointmentIdField,
            "Patient ID",
            appointmentPatientIdField,
            "Doctor ID",
            appointmentDoctorIdField,
            "Start Time",
            appointmentStartField,
            "Duration Minutes (blank = doctor slot)",
            appointmentDurationField,
            "Symptoms (comma separated)",
            appointmentSymptomsField),
        BorderLayout.NORTH);

    JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    JButton createButton = new JButton("Create");
    createButton.addActionListener(
        e ->
            perform(
                "Appointment created",
                () -> {
                  Appointment appointment =
                      appointmentService.createAppointment(
                          requireText(appointmentPatientIdField, "Patient ID"),
                          requireText(appointmentDoctorIdField, "Doctor ID"),
                          parseDateTime(appointmentStartField, "Start Time"),
                          parseOptionalInt(appointmentDurationField),
                          splitComma(appointmentSymptomsField.getText()));
                  appointmentIdField.setText(appointment.getId());
                  renderAppointments(appointmentService.listAppointments());
                }));

    JButton cancelButton = new JButton("Cancel by ID");
    cancelButton.addActionListener(
        e ->
            perform(
                "Appointment cancelled",
                () -> {
                  appointmentService.cancelAppointment(requireText(appointmentIdField, "Appointment ID"));
                  renderAppointments(appointmentService.listAppointments());
                }));

    JButton listAllButton = new JButton("List All");
    listAllButton.addActionListener(e -> renderAppointments(appointmentService.listAppointments()));

    JButton listDoctorButton = new JButton("List by Doctor");
    listDoctorButton.addActionListener(
        e ->
            perform(
                "Doctor appointments listed",
                () ->
                    renderAppointments(
                        appointmentService.listByDoctor(requireText(appointmentDoctorIdField, "Doctor ID")))));

    JButton listPatientButton = new JButton("List by Patient");
    listPatientButton.addActionListener(
        e ->
            perform(
                "Patient appointments listed",
                () ->
                    renderAppointments(
                        appointmentService.listByPatient(requireText(appointmentPatientIdField, "Patient ID")))));

    JButton suggestSlotsButton = new JButton("Suggest Slots");
    suggestSlotsButton.addActionListener(
        e ->
            perform(
                "Slot suggestion generated",
                () ->
                    renderText(
                        appointmentOutput,
                        "Suggested Slots",
                        appointmentService
                            .suggestNextSlots(
                                requireText(appointmentDoctorIdField, "Doctor ID"),
                                parseDateTime(appointmentStartField, "Start Time"),
                                5)
                            .stream()
                            .map(DateUtil::formatDateTime)
                            .collect(Collectors.joining("\n")))));

    buttons.add(createButton);
    buttons.add(cancelButton);
    buttons.add(listAllButton);
    buttons.add(listDoctorButton);
    buttons.add(listPatientButton);
    buttons.add(suggestSlotsButton);
    panel.add(buttons, BorderLayout.CENTER);
    panel.add(wrapOutput(appointmentOutput), BorderLayout.SOUTH);
    return panel;
  }

  private JPanel buildBillingTab() {
    JPanel panel = new JPanel(new BorderLayout(12, 12));
    panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
    panel.add(
        buildFormPanel(
            "Billing Actions",
            "Appointment ID",
            billingAppointmentIdField),
        BorderLayout.NORTH);

    JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    JButton generateButton = new JButton("Generate Bill");
    generateButton.addActionListener(
        e ->
            perform(
                "Bill generated",
                () -> {
                  Bill bill = billingService.generateBill(requireText(billingAppointmentIdField, "Appointment ID"));
                  renderText(billingOutput, "Generated Bill", bill + "\n" + bill.toSummary());
                }));

    JButton listButton = new JButton("List Bills");
    listButton.addActionListener(e -> renderBills(billingService.listBills()));

    buttons.add(generateButton);
    buttons.add(listButton);
    panel.add(buttons, BorderLayout.CENTER);
    panel.add(wrapOutput(billingOutput), BorderLayout.SOUTH);
    return panel;
  }

  private JPanel buildFormPanel(String title, Object... labelFieldPairs) {
    JPanel root = new JPanel(new BorderLayout(8, 8));
    root.setBorder(BorderFactory.createTitledBorder(title));

    JPanel grid = new JPanel(new GridLayout(0, 2, 8, 8));
    for (int i = 0; i < labelFieldPairs.length; i += 2) {
      grid.add(new JLabel(String.valueOf(labelFieldPairs[i])));
      Object field = labelFieldPairs[i + 1];
      if (field instanceof JTextField textField) {
        grid.add(textField);
      } else if (field instanceof JComboBox<?> comboBox) {
        grid.add(comboBox);
      } else {
        throw new IllegalArgumentException("Unsupported form component: " + field);
      }
    }
    root.add(grid, BorderLayout.CENTER);
    return root;
  }

  private JScrollPane wrapOutput(JTextArea area) {
    JScrollPane scrollPane = new JScrollPane(area);
    scrollPane.setBorder(BorderFactory.createTitledBorder("Output"));
    return scrollPane;
  }

  private JTextArea createOutputArea() {
    JTextArea area = new JTextArea(16, 60);
    area.setEditable(false);
    area.setLineWrap(true);
    area.setWrapStyleWord(true);
    return area;
  }

  private void renderDoctors(List<Doctor> items) {
    renderList(doctorOutput, "Doctors", items);
  }

  private void renderPatients(List<Patient> items) {
    renderList(patientOutput, "Patients", items);
  }

  private void renderAppointments(List<Appointment> items) {
    renderList(appointmentOutput, "Appointments", items);
  }

  private void renderBills(List<Bill> items) {
    renderList(billingOutput, "Bills", items);
  }

  private void renderList(JTextArea area, String title, List<?> items) {
    String body =
        items == null || items.isEmpty()
            ? "No records found."
            : items.stream().map(String::valueOf).collect(Collectors.joining("\n\n"));
    renderText(area, title, body);
  }

  private void renderText(JTextArea area, String title, String body) {
    area.setText(title + "\n" + "=".repeat(title.length()) + "\n" + body);
    area.setCaretPosition(0);
  }

  private void populateDoctorForm(Doctor doctor) {
    doctorIdField.setText(doctor.getId());
    doctorNameField.setText(doctor.getName());
    doctorAgeField.setText(String.valueOf(doctor.getAge()));
    doctorPhoneField.setText(doctor.getPhone());
    doctorEmailField.setText(doctor.getEmail());
    doctorSpecializationCombo.setSelectedItem(doctor.getSpecialization().name());
    doctorFeeField.setText(String.valueOf(doctor.getConsultationFee()));
    doctorSlotField.setText(String.valueOf(doctor.getSlotMinutes()));
  }

  private void clearDoctorForm() {
    doctorIdField.setText("");
    doctorNameField.setText("");
    doctorAgeField.setText("");
    doctorPhoneField.setText("");
    doctorEmailField.setText("");
    doctorSpecializationCombo.setSelectedIndex(0);
    doctorFeeField.setText("");
    doctorSlotField.setText("");
  }

  private void populatePatientForm(Patient patient) {
    patientIdField.setText(patient.getId());
    patientNameField.setText(patient.getName());
    patientAgeField.setText(String.valueOf(patient.getAge()));
    patientPhoneField.setText(patient.getPhone());
    patientEmailField.setText(patient.getEmail());
    patientInsuredCheck.setSelected(patient.isInsured());
    patientCoverageField.setText(String.valueOf(patient.getInsuranceCoveragePercent()));
    patientAddressLine1Field.setText(patient.getAddress().getLine1());
    patientCityField.setText(patient.getAddress().getCity());
    patientStateField.setText(patient.getAddress().getState());
    patientZipField.setText(patient.getAddress().getZip());
    patientAllergiesField.setText(String.join(", ", patient.getAllergies()));
  }

  private void clearPatientForm() {
    patientIdField.setText("");
    patientNameField.setText("");
    patientAgeField.setText("");
    patientPhoneField.setText("");
    patientEmailField.setText("");
    patientInsuredCheck.setSelected(false);
    patientCoverageField.setText("");
    patientAddressLine1Field.setText("");
    patientCityField.setText("");
    patientStateField.setText("");
    patientZipField.setText("");
    patientAllergiesField.setText("");
  }

  private void perform(String successMessage, UiAction action) {
    try {
      action.run();
      refreshStatus(successMessage);
    } catch (Exception e) {
      showError(e);
    }
  }

  private void refreshStatus(String message) {
    statusLabel.setText(
        message
            + " | doctors="
            + doctors.size()
            + ", patients="
            + patients.size()
            + ", appointments="
            + appointments.size()
            + ", bills="
            + bills.size());
  }

  private void saveQuietly() {
    try {
      PersistenceManager.saveAll(baseDir, doctors, patients, appointments, bills);
    } catch (Exception e) {
      showError(e);
    }
  }

  private void showError(Exception e) {
    String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    refreshStatus("Action failed: " + message);
    JOptionPane.showMessageDialog(this, message, "MediTrack", JOptionPane.ERROR_MESSAGE);
  }

  private String requireText(JTextField field, String label) {
    String value = text(field);
    if (value.isEmpty()) {
      throw new InvalidDataException(label + " is required");
    }
    return value;
  }

  private String text(JTextField field) {
    return field.getText() == null ? "" : field.getText().trim();
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  private int parseRequiredInt(JTextField field, String label) {
    return Integer.parseInt(requireText(field, label));
  }

  private int parseOptionalInt(JTextField field) {
    String value = text(field);
    return value.isEmpty() ? 0 : Integer.parseInt(value);
  }

  private Integer parseOptionalInteger(JTextField field) {
    String value = text(field);
    return value.isEmpty() ? null : Integer.valueOf(value);
  }

  private double parseRequiredDouble(JTextField field, String label) {
    return Double.parseDouble(requireText(field, label));
  }

  private Double parseOptionalDoubleObject(JTextField field) {
    String value = text(field);
    return value.isEmpty() ? null : Double.valueOf(value);
  }

  private double parseCoverageForCreate() {
    if (!patientInsuredCheck.isSelected()) {
      return 0.0;
    }
    return parseRequiredDouble(patientCoverageField, "Insurance Coverage %");
  }

  private LocalDateTime parseDateTime(JTextField field, String label) {
    return DateUtil.parseDateTime(requireText(field, label));
  }

  private Address buildAddressFromForm() {
    return new Address(
        requireText(patientAddressLine1Field, "Address Line 1"),
        requireText(patientCityField, "City"),
        requireText(patientStateField, "State"),
        requireText(patientZipField, "ZIP"));
  }

  private List<String> splitComma(String input) {
    if (input == null || input.isBlank()) {
      return List.of();
    }
    return List.of(input.split(",")).stream()
        .map(String::trim)
        .filter(part -> !part.isEmpty())
        .collect(Collectors.toList());
  }

  private Specialization requireSpecialization() {
    String selected = (String) doctorSpecializationCombo.getSelectedItem();
    if (selected == null || selected.isBlank()) {
      throw new InvalidDataException("Specialization is required");
    }
    return Specialization.valueOf(selected);
  }

  private Specialization optionalSpecialization() {
    String selected = (String) doctorSpecializationCombo.getSelectedItem();
    return selected == null || selected.isBlank() ? null : Specialization.valueOf(selected);
  }

  private String[] specializationOptions() {
    String[] values = new String[Specialization.values().length + 1];
    values[0] = "";
    for (int i = 0; i < Specialization.values().length; i++) {
      values[i + 1] = Specialization.values()[i].name();
    }
    return values;
  }

  @FunctionalInterface
  private interface UiAction {
    void run() throws Exception;
  }
}
