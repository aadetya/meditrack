package com.airtribe.meditrack.util;

import com.airtribe.meditrack.entity.MedicalEntity;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** Generic in-memory repository used by the services and persistence layer. */
public class DataStore<T extends MedicalEntity> implements Serializable, Iterable<T> {
  @Serial private static final long serialVersionUID = 1L;

  private final Map<String, T> store = new HashMap<>();

  /**
   * Inserts or replaces an item by id.
   *
   * @param item item to store
   */
  public synchronized void upsert(T item) {
    Validator.requireNonNull("item", item);
    store.put(item.getId(), item);
  }

  /**
   * Returns an item by id.
   *
   * @param id entity identifier
   * @return matching item or {@code null}
   */
  public synchronized T getById(String id) {
    if (Validator.isBlank(id)) return null;
    return store.get(id.trim());
  }

  /**
   * Returns a snapshot of all items.
   *
   * @return item snapshot
   */
  public synchronized List<T> getAll() {
    return new ArrayList<>(store.values());
  }

  /**
   * Removes an item by id.
   *
   * @param id entity identifier
   * @return removed item or {@code null}
   */
  public synchronized T remove(String id) {
    if (Validator.isBlank(id)) return null;
    return store.remove(id.trim());
  }

  /** Clears the store. */
  public synchronized void clear() {
    store.clear();
  }

  /**
   * Returns the number of stored items.
   *
   * @return store size
   */
  public synchronized int size() {
    return store.size();
  }

  /**
   * Finds items using a predicate.
   *
   * @param predicate filter predicate
   * @return matching items
   */
  public synchronized List<T> find(Predicate<T> predicate) {
    Validator.requireNonNull("predicate", predicate);
    List<T> result = new ArrayList<>();
    for (T item : store.values()) {
      if (predicate.test(item)) result.add(item);
    }
    return result;
  }

  @Override
  /**
   * Returns an iterator over a snapshot of the store.
   *
   * @return snapshot iterator
   */
  public Iterator<T> iterator() {
    return getAll().iterator();
  }
}
