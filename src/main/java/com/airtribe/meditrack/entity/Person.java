package com.airtribe.meditrack.entity;

import com.airtribe.meditrack.util.Validator;
import java.time.LocalDateTime;

/** Base class for human records such as doctors and patients. */
public abstract class Person extends MedicalEntity {
  private String name;
  private int age;
  private String phone;
  private String email;

  protected Person(String id, String name, int age, String phone, String email) {
    super(id);
    setName(name);
    setAge(age);
    setPhone(phone);
    setEmail(email);
  }

  protected Person(
      String id,
      LocalDateTime createdAt,
      LocalDateTime updatedAt,
      String name,
      int age,
      String phone,
      String email) {
    super(id, createdAt, updatedAt);
    setName(name);
    setAge(age);
    setPhone(phone);
    setEmail(email);
  }

  /**
   * Returns the display name.
   *
   * @return person name
   */
  public String getName() {
    return name;
  }

  /**
   * Updates the display name.
   *
   * @param name non-blank person name
   */
  public void setName(String name) {
    this.name = Validator.requireNonBlank("name", name);
    touch();
  }

  /**
   * Returns the age.
   *
   * @return age in years
   */
  public int getAge() {
    return age;
  }

  /**
   * Updates the age.
   *
   * @param age age in years
   */
  public void setAge(int age) {
    this.age = Validator.requireAge(age);
    touch();
  }

  /**
   * Returns the normalized phone number.
   *
   * @return normalized phone number
   */
  public String getPhone() {
    return phone;
  }

  /**
   * Updates the phone number.
   *
   * @param phone raw phone input
   */
  public void setPhone(String phone) {
    this.phone = Validator.requirePhoneLike(phone);
    touch();
  }

  /**
   * Returns the email address.
   *
   * @return validated email address
   */
  public String getEmail() {
    return email;
  }

  /**
   * Updates the email address.
   *
   * @param email raw email input
   */
  public void setEmail(String email) {
    this.email = Validator.requireEmailLike(email);
    touch();
  }
}
