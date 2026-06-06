package com.airtribe.meditrack.ui.console;

import java.util.List;

final class ConsoleSupport {
  private ConsoleSupport() {}

  static List<String> splitComma(String input) {
    if (input == null || input.isBlank()) {
      return List.of();
    }
    return java.util.Arrays.stream(input.split(","))
        .map(String::trim)
        .filter(value -> !value.isEmpty())
        .toList();
  }
}
