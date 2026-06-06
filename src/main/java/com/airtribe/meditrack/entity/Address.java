package com.airtribe.meditrack.entity;

import com.airtribe.meditrack.util.Validator;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * Immutable postal address used by {@link Patient}.
 *
 * <p>The class is intentionally immutable so callers cannot mutate a patient's nested address
 * state after retrieval.
 */
public final class Address implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  private final String line1;
  private final String city;
  private final String state;
  private final String zip;

  /**
   * Creates a validated immutable address.
   *
   * @param line1 primary address line
   * @param city city name
   * @param state state name
   * @param zip postal code
   */
  public Address(String line1, String city, String state, String zip) {
    this.line1 = Validator.requireNonBlank("address.line1", line1);
    this.city = Validator.requireNonBlank("address.city", city);
    this.state = Validator.requireNonBlank("address.state", state);
    this.zip = Validator.requireNonBlank("address.zip", zip);
  }

  /**
   * Returns a defensive structural copy of the supplied address.
   *
   * @param address address to copy
   * @return copied immutable address
   */
  public static Address copyOf(Address address) {
    Address safe = Validator.requireNonNull("address", address);
    return new Address(safe.line1, safe.city, safe.state, safe.zip);
  }

  /**
   * Returns the first address line.
   *
   * @return first address line
   */
  public String getLine1() {
    return line1;
  }

  /**
   * Returns the city.
   *
   * @return city name
   */
  public String getCity() {
    return city;
  }

  /**
   * Returns the state.
   *
   * @return state name
   */
  public String getState() {
    return state;
  }

  /**
   * Returns the postal code.
   *
   * @return postal code
   */
  public String getZip() {
    return zip;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Address address)) return false;
    return line1.equals(address.line1)
        && city.equals(address.city)
        && state.equals(address.state)
        && zip.equals(address.zip);
  }

  @Override
  public int hashCode() {
    return Objects.hash(line1, city, state, zip);
  }

  @Override
  public String toString() {
    return line1 + ", " + city + ", " + state + " " + zip;
  }
}
