# JVM Report

This report explains the main JVM concepts behind MediTrack using examples from the project itself.

## JDK, JRE, and JVM

- The **JDK** is the toolkit used to build the project. It includes tools such as `javac` and `javadoc`.
- The **JRE** is the runtime environment used to run the compiled classes.
- The **JVM** is the part of that runtime which loads classes, allocates memory, manages threads, and executes bytecode.

In MediTrack, `mvn -q -DskipTests package` uses the JDK because source files are being compiled. Running `Main`, `TestRunner`, or `DemoDataSeeder` uses the JRE and JVM because the code is already compiled at that point.

## Class Loading

The JVM does not load every class at startup. Classes are loaded as they are needed.

### Bootstrap Class Loader

This loader provides the core Java classes used throughout the project, including:

- `String`
- `List`
- `HashMap`
- `LocalDateTime`
- `ObjectInputStream`
- `ObjectOutputStream`

MediTrack depends on these in almost every package.

### Platform Class Loader

This loader provides standard platform modules outside the smallest core set. In this project, that mainly matters for Swing and some desktop or file-related APIs.

### Application Class Loader

This loader brings in the project classes from the classpath, such as:

- `com.airtribe.meditrack.Main`
- `com.airtribe.meditrack.service.AppointmentService`
- `com.airtribe.meditrack.util.PersistenceManager`
- `com.airtribe.meditrack.test.DemoDataSeeder`
- `com.airtribe.meditrack.test.TestRunner`

When the application starts, the chosen entry point is loaded first. Other classes are loaded as execution reaches them.

### Parent Delegation

Java class loading follows parent delegation. The JVM asks higher-level loaders first before loading application classes.

That prevents a local project class from accidentally replacing trusted runtime classes such as `java.util.Timer` or `java.io.Serializable`.

## Runtime Memory Areas

### Heap

The heap stores objects created at runtime. In MediTrack, that includes:

- `Doctor`, `Patient`, `Appointment`, `Bill`, and `Address`
- collections used inside services and `DataStore<T>`
- observer objects such as `ReminderSchedulerObserver`
- Swing components when the UI is launched

For example, `DemoDataSeeder` creates the demo doctors, patients, appointments, and bills on the heap, and `DataStore<T>` keeps references to them.

The heap is also relevant for cloning and serialization. Nested objects and lists need careful handling because they are part of the same object graph.

### JVM Stack

Each thread has its own stack. Every method call creates a stack frame with local variables, parameters, intermediate values, and return information.

One example is `AppointmentService.createAppointment(...)`. The method call stores the input values in its frame, then calls doctor lookup, patient lookup, and availability checks. Each of those calls gets its own frame.

The `Appointment` object itself is stored on the heap, while the temporary call data stays on the stack.

### Method Area / Metaspace

Class metadata is stored in the method area, which in modern HotSpot JVMs is implemented as **Metaspace**.

For this project, that includes metadata for classes such as:

- `Doctor`
- `Patient`
- `MedicalEntity`
- `DataStore`
- `BillingStrategyFactory`
- `ReminderSchedulerObserver`

That is the difference between the class definition and the runtime objects created from it.

### PC Register

Each thread has its own program counter register. It tracks the instruction currently being executed by that thread.

This matters in MediTrack because the application is not single-threaded. The main menu, autosave thread, and reminder tasks all have their own execution flow.

### Native Method Stack

The project does not call JNI directly, but the JVM and standard library may still use native code behind the scenes for file operations, timers, UI integration, and other OS-level work.

## Execution Engine

The JVM execution engine runs bytecode through interpretation and JIT compilation.

### Interpreter

The interpreter runs bytecode instruction by instruction. This is common at startup and for code paths that are not used often.

In MediTrack, one-time startup work and rarely used menu branches are good examples of code that may stay interpreted for a while.

### JIT Compiler

The Just-In-Time compiler identifies frequently used methods and compiles them into optimized machine code.

In this project, likely candidates include:

- repeated validation in `Validator`
- repeated service calls
- analytics stream operations
- iteration through `DataStore<T>`
- recommendation and slot-suggestion logic

## Interpreter vs JIT

| Aspect | Interpreter | JIT Compiler |
| --- | --- | --- |
| Startup | Runs immediately | Needs runtime profiling first |
| Best fit | Cold or rarely used code | Frequently repeated code |
| Long-run speed | Lower | Higher after optimization |

That balance is one reason Java works well for a project like this: startup is simple, and repeated paths can still be optimized during longer runs.

## Write Once, Run Anywhere

Java source code is compiled into JVM bytecode rather than platform-specific machine code. That means the same MediTrack build can run on macOS, Windows, or Linux as long as a compatible Java runtime is available.

In practice, the code under `target/classes` can be moved to another machine and run there without changing the source.

## How This Maps to MediTrack

One normal run looks like this:

1. Maven compiles the source into `.class` files.
2. The application class loader loads `Main`.
3. As startup continues, service, entity, persistence, and UI classes are loaded as needed.
4. Their metadata is stored in Metaspace.
5. When data is loaded, doctors, patients, appointments, and bills are created on the heap.
6. Method calls in the console flow, billing flow, and persistence flow use stack frames.
7. The main thread, autosave thread, and reminder thread each keep their own stack and program counter state.
8. Repeated code paths may be optimized by the JIT over time.

## Why This Matters Here

The JVM model is directly relevant to this project:

- Heap behavior explains why cloning and serialization need care around nested state.
- Stack behavior explains the service call chains used in scheduling and billing.
- Metaspace explains the difference between class definitions and runtime objects.
- Thread-specific stacks and program counters explain why background autosave and reminders need proper synchronization.
- JIT behavior explains why repeated service and analytics paths can improve over time during longer runs.
