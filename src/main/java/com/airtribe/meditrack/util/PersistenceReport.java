package com.airtribe.meditrack.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable report describing how one entity store was restored from persistence. */
public final class PersistenceReport {
  /** Persistence source used during load. */
  public enum Source {
    SER,
    CSV,
    EMPTY
  }

  private final String entityName;
  private final Source source;
  private final int countLoaded;
  private final List<String> warnings;

  /**
   * Creates a persistence load report.
   *
   * @param entityName logical entity name
   * @param source source used for loading
   * @param countLoaded number of records restored
   * @param warnings fallback warnings or errors
   */
  public PersistenceReport(
      String entityName, Source source, int countLoaded, List<String> warnings) {
    this.entityName = Validator.requireNonBlank("entityName", entityName);
    this.source = Validator.requireNonNull("source", source);
    this.countLoaded = Math.max(0, countLoaded);
    this.warnings =
        Collections.unmodifiableList(new ArrayList<>(warnings == null ? List.of() : warnings));
  }

  /**
   * Returns the logical entity name.
   *
   * @return entity name
   */
  public String getEntityName() {
    return entityName;
  }

  /**
   * Returns the source used for loading.
   *
   * @return persistence source
   */
  public Source getSource() {
    return source;
  }

  /**
   * Returns the number of restored records.
   *
   * @return loaded count
   */
  public int getCountLoaded() {
    return countLoaded;
  }

  /**
   * Returns warnings encountered during load.
   *
   * @return immutable warning list
   */
  public List<String> getWarnings() {
    return warnings;
  }

  /**
   * Returns whether any warnings were recorded.
   *
   * @return true when warnings exist
   */
  public boolean hasWarnings() {
    return !warnings.isEmpty();
  }

  @Override
  public String toString() {
    return entityName + ": source=" + source + ", count=" + countLoaded + ", warnings=" + warnings.size();
  }
}
