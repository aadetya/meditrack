package com.airtribe.meditrack.interfaces;

import java.util.List;

/** Generic search contract used by services that support text queries. */
public interface Searchable<T> {
  /**
   * Searches for matching items.
   *
   * @param query user query
   * @return matching results
   */
  List<T> search(String query);

  /**
   * Normalizes text for case-insensitive matching.
   *
   * @param s raw text
   * @return normalized text
   */
  default String normalize(String s) {
    return s == null ? "" : s.trim().toLowerCase();
  }
}
