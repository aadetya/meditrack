# MediTrack

MediTrack is a clinic management system written in Core Java. It covers doctor and patient records, appointment scheduling, billing, persistence, reminders, analytics, and a small rule-based recommendation helper.

The console application in [`Main.java`](src/main/java/com/airtribe/meditrack/Main.java) is the primary interface. A Swing UI is included for demonstration, but the console flow remains the main path.

## Main Features

- doctor and patient management with validation at the entity level
- confirmed and pending appointments, confirmation, cancellation, completion, rescheduling, and slot suggestions
- billing for standard, insured, and senior patients
- persistence to both CSV and `.ser`
- startup load reports with fallback from serialization to CSV
- background autosave
- appointment notifications and reminders through observers
- analytics for fees, appointment counts, and status breakdowns
- deterministic demo data and a manual verification runner

## Project Layout

The main packages are under `src/main/java/com/airtribe/meditrack`:

- `entity`: domain objects such as `Doctor`, `Patient`, `Appointment`, and `Bill`
- `service`: application logic for doctors, patients, appointments, billing, and safe delete
- `service.billing`: billing strategies and bill construction
- `service.notification`: appointment observers, event payloads, and reminder scheduling
- `util`: shared infrastructure such as `DataStore`, persistence utilities, validators, ID generation, and the recommendation helper
- `ui.console`: the console menus and input handling
- `ui`: the optional Swing UI
- `test`: `DemoDataSeeder` and `TestRunner`

## Design Summary

The code is split so each rule has a clear home.

- `MedicalEntity` gives all persisted records a common ID and timestamp base.
- `Doctor` and `Patient` extend `Person`, which avoids repeating shared field validation.
- `Appointment` owns its lifecycle, so illegal state changes are rejected in the domain model.
- `AppointmentService` owns scheduling rules, overlap checks, and slot suggestions.
- `BillingService` uses billing strategies instead of one large conditional block.
- `ClinicManagementService` handles cross-entity rules such as safe delete.
- `PersistenceManager` writes and loads all stores, and reports whether data came from `SER`, `CSV`, or an empty source.
- `AutoSaveService` runs persistence in the background without mixing that logic into the UI.
- `AIHelper` is rule-based and deterministic, which keeps the recommendations explainable.

## Build and Run

Build the project:

```bash
mvn -q -DskipTests package
```

Seed the demo data:

```bash
mvn -q exec:java -Dexec.mainClass=com.airtribe.meditrack.test.DemoDataSeeder
```

Run the console application with persisted data:

```bash
mvn -q exec:java -Dexec.mainClass=com.airtribe.meditrack.Main -Dexec.args="--loadData"
```

Run the manual verification suite:

```bash
mvn -q exec:java -Dexec.mainClass=com.airtribe.meditrack.test.TestRunner
```

Launch the optional Swing UI:

```bash
mvn -q exec:java -Dexec.mainClass=com.airtribe.meditrack.ui.GuiMain
```

Generate JavaDocs:

```bash
mvn -q javadoc:javadoc
```

## Verification

The repository includes both live demo artifacts and a manual verification runner.

- `DemoDataSeeder` creates a fixed dataset so the walkthrough stays repeatable.
- `TestRunner` checks validation, lifecycle rules, persistence, autosave, observer behavior, billing, analytics, and the recommendation helper.
- `docs/setup_images` and `docs/demo_walkthrough_images` contain screenshots with matching transcripts.
- The UML files document the domain model, service collaboration, and persistence flow.

## Documentation

- [`docs/Setup_Instructions.md`](docs/Setup_Instructions.md): local setup and build/run steps
- [`docs/Demo_Walkthrough.md`](docs/Demo_Walkthrough.md): screenshot-based demo walkthrough
- [`docs/Design_Decisions.md`](docs/Design_Decisions.md): the main design choices in the codebase
- [`docs/Design_Traceability_Matrix.md`](docs/Design_Traceability_Matrix.md): class/interface to principle/pattern traceability matrix
- [`docs/MediTrack_System_Design_and_Implementation_Overview.md`](docs/MediTrack_System_Design_and_Implementation_Overview.md): full project overview
- [`docs/MediTrack_System_Design_and_Implementation_Overview.pdf`](docs/MediTrack_System_Design_and_Implementation_Overview.pdf): PDF export of the overview
- [`docs/JVM_Report.md`](docs/JVM_Report.md): JVM concepts explained using this project
- [`docs/JavaDoc_Guide.md`](docs/JavaDoc_Guide.md): JavaDoc generation notes
- [`docs/UML_Domain.puml`](docs/UML_Domain.puml): domain model UML
- [`docs/UML_Architecture.puml`](docs/UML_Architecture.puml): high-level architecture UML
- [`docs/UML_Service_Collaboration.puml`](docs/UML_Service_Collaboration.puml): service and observer collaboration UML
- [`docs/UML_Persistence.puml`](docs/UML_Persistence.puml): persistence and runtime support UML
