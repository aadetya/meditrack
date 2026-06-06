# Design Decisions

This document explains the main design choices in MediTrack. The focus is on why the code is arranged the way it is, not on restating every class.

## 1. Shared identity in `MedicalEntity`

`MedicalEntity` holds the business ID and timestamps used by all persisted records. Equality, update tracking, and store behavior stay consistent across doctors, patients, appointments, and bills.

Without that base class, the same identity logic would be repeated in several places and would be harder to keep consistent.

## 2. `Doctor` and `Patient` extend `Person`

The inheritance here is straightforward. Doctors and patients both have name, age, phone, and email, so those fields and their validation live in `Person`.

This is not decorative inheritance. It removes duplication and keeps shared validation in one place.

## 3. Validation stays in the entities

Core validation is handled inside the model classes. `Person` validates common personal data, `Doctor` checks fee and slot length, `Patient` controls insurance state, and `Appointment` controls lifecycle transitions.

That approach keeps the rules safe across every entry point, including the console menus, the Swing UI, the persistence loader, the demo seeder, and the test runner.

## 4. Address and collection state is protected

`Address` is immutable, and `Patient` still copies it on assignment. Allergy and symptom lists are sanitized before storage and exposed through unmodifiable views.

The goal is simple: callers should not be able to change stored state accidentally through nested references.

The same reasoning applies to cloning. `Patient.clone()` deep copies the address and allergy list, and `Appointment.clone()` deep copies the symptom list. Without that, a copied object could still share mutable nested state with the original.

## 5. Appointment status is treated as a state machine

Appointments are not plain records with a free-form status field. The allowed transitions are limited to the valid workflow:

- `PENDING -> CONFIRMED`
- `PENDING -> CANCELLED`
- `CONFIRMED -> CANCELLED`
- `CONFIRMED -> COMPLETED`

Invalid transitions are rejected. Billing, reminders, safe delete, and analytics all depend on appointment state being trustworthy.

## 6. `AppointmentService` owns scheduling rules

Creation, confirmation, cancellation, completion, rescheduling, overlap checks, and slot suggestions all live in `AppointmentService`.

The scheduling rules stay in one place. The console UI and the Swing UI both call the same logic instead of maintaining their own versions of availability checks.

## 7. Patient search uses overloads by design

`PatientService` keeps separate search methods for ID, name, and age. These are different query types, so separate method signatures make the API easier to read and harder to misuse.

This is a better fit than forcing all patient lookup into one generic text search.

## 8. Doctor search uses a shared contract

`DoctorService` implements `Searchable<Doctor>`. The interface gives doctor search one place for normalization and matching instead of scattering string handling across the codebase.

It is a light abstraction, but it fits the problem well. The default `normalize(...)` method also keeps case-insensitive matching logic out of the service implementation.

## 9. `Bill` and `BillSummary` serve different jobs

`Bill` is mutable because payment changes its state. `BillSummary` is immutable because it is used for display, proof output, and comparisons in tests.

Keeping both types makes the billing flow easier to reason about. The live entity can change, while the summary stays safe to print or compare.

`Bill` also implements `Payable`. That interface makes the payment contract explicit and keeps tax computation tied to the billing model instead of leaving it as ad-hoc utility logic.

## 10. Billing strategy and bill construction are split

The billing code separates three concerns:

- `BillingStrategy` implementations calculate the pricing path
- `BillingStrategyFactory` chooses the right strategy
- `BillFactory` creates the final `Bill`

That split keeps the billing logic modular. Selection, calculation, and object construction do not get mixed together.

## 11. `ClinicManagementService` handles safe delete

Safe delete is a cross-entity rule. A doctor or patient cannot be deleted while active appointments still reference that record.

That rule does not belong inside a low-level CRUD method, so it sits in a small coordinating service instead.

## 12. `DataStore<T>` is deliberately simple

`DataStore<T extends MedicalEntity>` is the shared in-memory store used across the services. It supports the small set of operations the project needs: insert or update, lookup, list, remove, and filtered search.

There is no need for a larger repository layer here. The current abstraction is enough and keeps the code readable.

The store is also synchronized at the method level. That is a practical choice rather than a theoretical one: the same stores are touched by the main flow, autosave, and reminder-related work. `getAll()` and `iterator()` return snapshots, so callers do not iterate over the live backing map while another path is writing to it.

## 13. Persistence uses both CSV and serialization

Each store is written to CSV and `.ser`. CSV is easy to inspect during review, while serialization restores full object state quickly.

Using both formats also makes it easier to recover when one path fails.

The CSV side is built around `CSVUtil<T>`. It acts as a reusable template-style base class: `write(...)` and `read(...)` define the common algorithm, while subclasses supply `header()`, `toRow(...)`, and `fromColumns(...)`. That avoids repeating the same file-handling code in every entity mapper.

## 14. Fallback loading is visible

`PersistenceManager` does not silently fall back from `.ser` to CSV. It records the source and any warnings in `PersistenceReport`.

That makes startup behavior easier to debug, especially when corrupted serialized files are involved.

The CSV path also fails loudly when parsing goes wrong. Rows are read with trailing empty fields preserved, and parse errors include the row number and file path. That makes bad data much easier to diagnose than a generic load failure would.

## 15. ID generation is centralized

`IdGenerator` is responsible for all business IDs such as `DOC-0001`, `PAT-0001`, and `APT-0001`. The counters are tracked per prefix and reseeded after load.

Keeping ID generation in one place avoids duplicated counters and makes persisted reloads predictable.

Configuration is centralized for the same reason. `AppConfig` is a shared singleton that reads runtime settings such as the autosave interval once and exposes them to the rest of the application through a stable access point.

## 16. Observer events use immutable snapshots

Appointment side effects are handled through observers. `AppointmentService` publishes events, while `ConsoleNotificationObserver` and `ReminderSchedulerObserver` react to them.

The event payload uses an immutable `AppointmentSnapshot`, so observers can read the appointment state without being able to mutate it.

Observer registration uses `CopyOnWriteArrayList`, which keeps delivery stable even if observers are added or removed during notification. Publication also catches observer exceptions per callback, so one failing observer does not block the others.

## 17. Reminder scheduling cleans up after itself

The reminder observer cancels and replaces tasks when appointments move, and it removes completed tasks after they fire.

That prevents stale reminder tasks from building up in the background. The internal task map is synchronized because scheduling and cancellation can happen from different execution paths.

## 18. Autosave is a background service

`AutoSaveService` runs as a daemon thread and calls the same persistence path used by manual saves. It is isolated from the UI and shuts down through a simple stop flag and interrupt.

Background persistence stays out of the menu logic.

## 19. Exceptions are small and specific

The code uses a small custom exception set instead of a deep hierarchy.

- `InvalidDataException` is used for validation and persistence failures that should be shown as normal application errors.
- `AppointmentNotFoundException` is used when an appointment lookup is required but the ID is missing or wrong.
- `InputReader.EndOfInputException` is an internal control signal that lets the console app exit cleanly when stdin closes.

This keeps error handling readable. The console UI can catch user-facing failures without mixing them up with low-level JVM or I/O exceptions.

## 20. The recommendation helper is rule-based

`AIHelper` maps symptoms to likely specializations and then asks the appointment logic for real slots. The behavior is deterministic by design.

That makes the feature easier to test and easier to explain in a project review.

## 21. The UIs stay thin

The console menus and the Swing UI gather input, call services, and display results. They do not own scheduling rules, billing rules, or persistence decisions.

That separation makes the code easier to maintain and keeps the core logic reusable.

## 22. `Main` is the composition root

`Main` wires together the stores, services, observers, autosave, and console menus. Startup code stays in one place, and hidden dependencies stay out of the rest of the project.

It also makes the runtime flow easier to follow during review.

## 23. Manual verification is part of the design

`TestRunner` is not just a convenience class. It checks the areas most likely to break: lifecycle rules, persistence fallback, autosave, observers, safe delete, billing, analytics, and the recommendation helper.

It also checks deep cloning, immutable snapshots, reminder cleanup, CSV fallback behavior, and observer safety during add/remove operations.

Together with the seeded demo data and the screenshot transcripts, it gives the project a concrete verification path.
