package com.airtribe.meditrack.util;

import com.airtribe.meditrack.exception.InvalidDataException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Reusable template for simple line-oriented CSV persistence. */
public abstract class CSVUtil<T> {
  /**
   * Writes the supplied items to CSV.
   *
   * @param path output path
   * @param items items to write
   */
  public final void write(Path path, List<T> items) {
    Validator.requireNonNull("path", path);
    List<T> safe = items == null ? List.of() : items;

    try {
      if (path.getParent() != null) {
        Files.createDirectories(path.getParent());
      }
      try (BufferedWriter writer = Files.newBufferedWriter(path)) {
        writer.write(header());
        writer.newLine();
        for (T item : safe) {
          writer.write(toRow(item));
          writer.newLine();
        }
      }
    } catch (IOException e) {
      throw new InvalidDataException("Failed to write CSV: " + path, e);
    }
  }

  /**
   * Reads items from CSV while preserving trailing empty fields.
   *
   * @param path source path
   * @return parsed items
   */
  public final List<T> read(Path path) {
    Validator.requireNonNull("path", path);
    if (!Files.exists(path)) return List.of();

    List<T> items = new ArrayList<>();
    try (BufferedReader reader = Files.newBufferedReader(path)) {
      reader.readLine();
      String line;
      int lineNumber = 1;
      while ((line = reader.readLine()) != null) {
        lineNumber++;
        if (Validator.isBlank(line)) continue;
        String[] cols = line.split(",", -1);
        try {
          items.add(fromColumns(cols));
        } catch (RuntimeException e) {
          throw new InvalidDataException(
              "Failed to parse CSV row " + lineNumber + " in " + path + ": " + e.getMessage(), e);
        }
      }
      return items;
    } catch (IOException e) {
      throw new InvalidDataException("Failed to read CSV: " + path, e);
    }
  }

  /**
   * Returns the CSV header row.
   *
   * @return header row
   */
  protected abstract String header();

  /**
   * Serializes a single item into a CSV row.
   *
   * @param item item to serialize
   * @return CSV row
   */
  protected abstract String toRow(T item);

  /**
   * Parses a single CSV row.
   *
   * @param cols split columns
   * @return parsed item
   */
  protected abstract T fromColumns(String[] cols);
}
