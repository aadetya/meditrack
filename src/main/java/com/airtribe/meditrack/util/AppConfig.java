package com.airtribe.meditrack.util;

/** Eager singleton for simple runtime configuration values. */
public final class AppConfig {
  private static final AppConfig INSTANCE = new AppConfig();

  private final int autosaveIntervalSeconds;

  private AppConfig() {
    this.autosaveIntervalSeconds = readPositiveInt("meditrack.autosaveSeconds", 30);
  }

  /**
   * Returns the shared configuration instance.
   *
   * @return app configuration
   */
  public static AppConfig getInstance() {
    return INSTANCE;
  }

  /**
   * Returns the autosave interval in seconds.
   *
   * @return autosave interval
   */
  public int getAutosaveIntervalSeconds() {
    return autosaveIntervalSeconds;
  }

  private int readPositiveInt(String key, int defaultValue) {
    try {
      String v = System.getProperty(key);
      if (v == null) return defaultValue;
      int parsed = Integer.parseInt(v.trim());
      return parsed > 0 ? parsed : defaultValue;
    } catch (Exception e) {
      return defaultValue;
    }
  }
}
