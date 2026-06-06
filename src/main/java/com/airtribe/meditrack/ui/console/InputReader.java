package com.airtribe.meditrack.ui.console;

import com.airtribe.meditrack.entity.Specialization;
import com.airtribe.meditrack.util.DateUtil;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Scanner;

/** Console input helper with retry logic and strong parsing rules. */
public class InputReader {
  private final Scanner scanner;

  /**
   * Creates an input reader over a scanner.
   *
   * @param scanner console scanner
   */
  public InputReader(Scanner scanner) {
    this.scanner = scanner;
  }

  /**
   * Prompts for a required string.
   *
   * @param label prompt label
   * @return non-blank string
   */
  public String promptString(String label) {
    while (true) {
      String value = promptOptionalString(label);
      if (!value.isBlank()) {
        return value;
      }
      System.out.println("Value is required.");
    }
  }

  /**
   * Prompts for an optional string.
   *
   * @param label prompt label
   * @return trimmed input, possibly blank
   */
  public String promptOptionalString(String label) {
    System.out.print(label);
    if (!scanner.hasNextLine()) {
      throw new EndOfInputException();
    }
    return scanner.nextLine().trim();
  }

  /**
   * Prompts for a required integer.
   *
   * @param label prompt label
   * @return parsed integer
   */
  public int promptInt(String label) {
    while (true) {
      try {
        return Integer.parseInt(promptString(label));
      } catch (NumberFormatException e) {
        System.out.println("Enter a valid integer.");
      }
    }
  }

  /**
   * Prompts for an optional integer.
   *
   * @param label prompt label
   * @return parsed integer or {@code null} when blank
   */
  public Integer promptOptionalInt(String label) {
    while (true) {
      String value = promptOptionalString(label);
      if (value.isBlank()) {
        return null;
      }
      try {
        return Integer.parseInt(value);
      } catch (NumberFormatException e) {
        System.out.println("Enter a valid integer or leave it blank.");
      }
    }
  }

  /**
   * Prompts for a required decimal.
   *
   * @param label prompt label
   * @return parsed decimal
   */
  public double promptDouble(String label) {
    while (true) {
      try {
        return Double.parseDouble(promptString(label));
      } catch (NumberFormatException e) {
        System.out.println("Enter a valid number.");
      }
    }
  }

  /**
   * Prompts for an optional decimal.
   *
   * @param label prompt label
   * @return parsed decimal or {@code null} when blank
   */
  public Double promptOptionalDouble(String label) {
    while (true) {
      String value = promptOptionalString(label);
      if (value.isBlank()) {
        return null;
      }
      try {
        return Double.parseDouble(value);
      } catch (NumberFormatException e) {
        System.out.println("Enter a valid number or leave it blank.");
      }
    }
  }

  /**
   * Prompts for a required yes/no response.
   *
   * @param label prompt label
   * @return yes/no response
   */
  public boolean promptYesNo(String label) {
    while (true) {
      String value = promptString(label).toLowerCase();
      if (value.equals("y") || value.equals("yes")) {
        return true;
      }
      if (value.equals("n") || value.equals("no")) {
        return false;
      }
      System.out.println("Please enter y/n.");
    }
  }

  /**
   * Prompts for an optional yes/no response.
   *
   * @param label prompt label
   * @return yes/no response or {@code null} when blank
   */
  public Boolean promptOptionalYesNo(String label) {
    while (true) {
      String value = promptOptionalString(label).toLowerCase();
      if (value.isBlank()) {
        return null;
      }
      if (value.equals("y") || value.equals("yes")) {
        return true;
      }
      if (value.equals("n") || value.equals("no")) {
        return false;
      }
      System.out.println("Please enter y/n or leave it blank.");
    }
  }

  /**
   * Prompts for a required date-time using {@link DateUtil}.
   *
   * @param label prompt label
   * @return parsed date-time
   */
  public LocalDateTime promptDateTime(String label) {
    while (true) {
      try {
        return DateUtil.parseDateTime(promptString(label));
      } catch (Exception e) {
        System.out.println("Enter a valid datetime in yyyy-MM-dd HH:mm format.");
      }
    }
  }

  /**
   * Prompts for a required specialization.
   *
   * @param label prompt label
   * @return parsed specialization
   */
  public Specialization promptSpecialization(String label) {
    System.out.println("Specializations: " + Arrays.toString(Specialization.values()));
    while (true) {
      String value = promptString(label).toUpperCase();
      try {
        return Specialization.valueOf(value);
      } catch (Exception e) {
        System.out.println("Invalid specialization.");
      }
    }
  }

  /**
   * Prompts for an optional specialization.
   *
   * @param label prompt label
   * @return parsed specialization or {@code null} when blank
   */
  public Specialization promptOptionalSpecialization(String label) {
    System.out.println("Specializations: " + Arrays.toString(Specialization.values()));
    while (true) {
      String value = promptOptionalString(label);
      if (value.isBlank()) {
        return null;
      }
      try {
        return Specialization.valueOf(value.toUpperCase());
      } catch (Exception e) {
        System.out.println("Invalid specialization or leave it blank.");
      }
    }
  }

  /** Internal signal used to exit gracefully when stdin closes. */
  public static final class EndOfInputException extends RuntimeException {}
}
