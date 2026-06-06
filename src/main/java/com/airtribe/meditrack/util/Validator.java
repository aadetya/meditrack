package com.airtribe.meditrack.util;

import com.airtribe.meditrack.exception.InvalidDataException;
import java.util.Objects;

/** Shared validation helpers used across entities, services, and persistence code. */
public final class Validator {
  private Validator() {}

  /**
   * Returns whether a string is blank.
   *
   * @param s source string
   * @return true when blank
   */
  public static boolean isBlank(String s) {
    return s == null || s.trim().isEmpty();
  }

  /**
   * Requires a non-null value.
   *
   * @param field field name
   * @param value field value
   * @param <T> value type
   * @return validated value
   */
  public static <T> T requireNonNull(String field, T value) {
    if (value == null) {
      throw new InvalidDataException(field + " cannot be null");
    }
    return value;
  }

  /**
   * Requires a non-blank string.
   *
   * @param field field name
   * @param value raw value
   * @return trimmed value
   */
  public static String requireNonBlank(String field, String value) {
    if (isBlank(value)) {
      throw new InvalidDataException(field + " cannot be blank");
    }
    return value.trim();
  }

  /**
   * Requires a positive integer.
   *
   * @param field field name
   * @param value integer value
   * @return validated value
   */
  public static int requirePositiveInt(String field, int value) {
    if (value <= 0) {
      throw new InvalidDataException(field + " must be > 0");
    }
    return value;
  }

  /**
   * Requires a positive decimal.
   *
   * @param field field name
   * @param value decimal value
   * @return validated value
   */
  public static double requirePositiveDouble(String field, double value) {
    if (!(value > 0.0)) {
      throw new InvalidDataException(field + " must be > 0");
    }
    return value;
  }

  /**
   * Requires a non-negative decimal.
   *
   * @param field field name
   * @param value decimal value
   * @return validated value
   */
  public static double requireNonNegativeDouble(String field, double value) {
    if (value < 0.0) {
      throw new InvalidDataException(field + " must be >= 0");
    }
    return value;
  }

  /**
   * Requires an age in the supported range.
   *
   * @param age age in years
   * @return validated age
   */
  public static int requireAge(int age) {
    if (age < 0 || age > 120) {
      throw new InvalidDataException("age must be between 0 and 120");
    }
    return age;
  }

  /**
   * Requires a minimally valid email address.
   *
   * @param email raw email input
   * @return trimmed email
   */
  public static String requireEmailLike(String email) {
    String v = requireNonBlank("email", email);
    if (!v.contains("@") || v.endsWith("@") || v.startsWith("@")) {
      throw new InvalidDataException("email is invalid");
    }
    return v;
  }

  /**
   * Requires a valid Indian mobile number and returns its normalized 10-digit form.
   *
   * @param phone raw phone input
   * @return normalized phone
   */
  public static String requirePhoneLike(String phone) {
    String v = requireNonBlank("phone", phone);
    String normalized = v.replaceAll("[\\s\\-()]", "");
    if (normalized.startsWith("+")) {
      if (!normalized.startsWith("+91")) {
        throw new InvalidDataException("phone must be an Indian mobile number (10 digits or +91)");
      }
      normalized = normalized.substring(3);
    }
    normalized = normalized.replaceAll("[^0-9]", "");
    if (!normalized.matches("[6-9][0-9]{9}")) {
      throw new InvalidDataException("phone must be an Indian mobile number (10 digits or +91)");
    }
    return normalized;
  }

  /**
   * Requires a percentage between 0 and 100 inclusive.
   *
   * @param field field name
   * @param value percentage value
   * @return validated percentage
   */
  public static double requirePercent(String field, double value) {
    Objects.requireNonNull(field, "field");
    if (value < 0.0 || value > 100.0) {
      throw new InvalidDataException(field + " must be between 0 and 100");
    }
    return value;
  }
}
