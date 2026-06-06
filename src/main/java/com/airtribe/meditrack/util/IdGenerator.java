package com.airtribe.meditrack.util;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Thread-safe singleton that generates entity identifiers with per-prefix counters. */
public final class IdGenerator {
  private static volatile IdGenerator instance;
  private final Map<String, AtomicInteger> counters;

  private IdGenerator() {
    this.counters = new ConcurrentHashMap<>();
  }

  /**
   * Returns the shared generator instance.
   *
   * @return id generator singleton
   */
  public static IdGenerator getInstance() {
    if (instance == null) {
      synchronized (IdGenerator.class) {
        if (instance == null) {
          instance = new IdGenerator();
        }
      }
    }
    return instance;
  }

  /**
   * Returns the next identifier for a prefix.
   *
   * @param prefix entity prefix such as {@code DOC} or {@code PAT}
   * @return formatted identifier
   */
  public String nextId(String prefix) {
    String p = Validator.requireNonBlank("prefix", prefix);
    int next = counters.computeIfAbsent(p, ignored -> new AtomicInteger(0)).incrementAndGet();
    return p + "-" + String.format("%04d", next);
  }

  /**
   * Seeds prefix counters from existing persisted identifiers.
   *
   * @param ids existing identifiers
   */
  public void seedFromExistingIds(Collection<String> ids) {
    if (ids == null || ids.isEmpty()) return;
    for (String id : ids) {
      String prefix = parsePrefix(id);
      if (prefix == null) {
        continue;
      }
      int parsed = parseNumericSuffix(id);
      counters.computeIfAbsent(prefix, ignored -> new AtomicInteger(0)).updateAndGet(cur -> Math.max(cur, parsed));
    }
  }

  /**
   * Test-only helper that clears all prefix counters.
   */
  public void resetForTests() {
    counters.clear();
  }

  private String parsePrefix(String id) {
    if (id == null) return null;
    int idx = id.lastIndexOf('-');
    if (idx <= 0) return null;
    String prefix = id.substring(0, idx).trim();
    return prefix.isEmpty() ? null : prefix;
  }

  private int parseNumericSuffix(String id) {
    if (id == null) return 0;
    int idx = id.lastIndexOf('-');
    if (idx < 0 || idx == id.length() - 1) return 0;
    String suffix = id.substring(idx + 1);
    try {
      return Integer.parseInt(suffix);
    } catch (NumberFormatException e) {
      return 0;
    }
  }
}
