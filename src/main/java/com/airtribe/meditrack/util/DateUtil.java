package com.airtribe.meditrack.util;

import com.airtribe.meditrack.exception.InvalidDataException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/** Shared date parsing, formatting, and slot-generation helpers. */
public final class DateUtil {
  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  private DateUtil() {}

  /**
   * Parses a date-time in {@code yyyy-MM-dd HH:mm} format.
   *
   * @param input raw date-time input
   * @return parsed date-time
   */
  public static LocalDateTime parseDateTime(String input) {
    String v = Validator.requireNonBlank("datetime", input);
    try {
      return LocalDateTime.parse(v, FORMATTER);
    } catch (DateTimeParseException e) {
      throw new InvalidDataException("Invalid datetime format. Expected: yyyy-MM-dd HH:mm", e);
    }
  }

  /**
   * Formats a date-time in {@code yyyy-MM-dd HH:mm} format.
   *
   * @param dt date-time to format
   * @return formatted date-time
   */
  public static String formatDateTime(LocalDateTime dt) {
    Validator.requireNonNull("datetime", dt);
    return dt.format(FORMATTER);
  }

  /**
   * Generates a sequence of equally spaced slots.
   *
   * @param start start time
   * @param slotMinutes slot spacing in minutes
   * @param count number of slots to generate
   * @return generated slots
   */
  public static List<LocalDateTime> nextSlots(LocalDateTime start, int slotMinutes, int count) {
    Validator.requireNonNull("start", start);
    Validator.requirePositiveInt("slotMinutes", slotMinutes);
    Validator.requirePositiveInt("count", count);

    List<LocalDateTime> slots = new ArrayList<>(count);
    LocalDateTime cur = start;
    for (int i = 0; i < count; i++) {
      slots.add(cur);
      cur = cur.plusMinutes(slotMinutes);
    }
    return slots;
  }
}
