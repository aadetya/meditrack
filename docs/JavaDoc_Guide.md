# JavaDoc Guide

Generate the JavaDoc site with:

```bash
mvn -q javadoc:javadoc
```

The generated site is written to:

- `target/site/apidocs/index.html`

The documented package groups are:

- `com.airtribe.meditrack.entity`
- `com.airtribe.meditrack.service`
- `com.airtribe.meditrack.service.billing`
- `com.airtribe.meditrack.service.notification`
- `com.airtribe.meditrack.util`
- `com.airtribe.meditrack.interfaces`
- `com.airtribe.meditrack.exception`
- `com.airtribe.meditrack.ui.console`

The site covers:

- package summaries from `package-info.java`
- public classes
- public constructors
- public methods on the documented API surface

To verify the output locally:

```bash
open target/site/apidocs/index.html
```

Recorded transcript: [`docs/setup_images/08_javadocs.txt`](setup_images/08_javadocs.txt)

Screenshot proof: [`docs/setup_images/08_javadocs.png`](setup_images/08_javadocs.png)
