package com.airtribe.meditrack.entity;

import com.airtribe.meditrack.util.Validator;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/** Shared identity and timestamp base class for persisted domain records. */
public abstract class MedicalEntity implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  private final String id;
  private final LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  /**
   * Creates a new entity with current timestamps.
   *
   * @param id logical business identifier
   */
  protected MedicalEntity(String id) {
    this(id, LocalDateTime.now(), LocalDateTime.now());
  }

  /**
   * Creates an entity with explicit timestamps, primarily for deserialization paths.
   *
   * @param id logical business identifier
   * @param createdAt initial creation timestamp
   * @param updatedAt last-updated timestamp
   */
  protected MedicalEntity(String id, LocalDateTime createdAt, LocalDateTime updatedAt) {
    this.id = Validator.requireNonBlank("id", id);
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
  }

  /**
   * Returns the stable business identifier.
   *
   * @return entity id
   */
  public String getId() {
    return id;
  }

  /**
   * Returns the record creation time.
   *
   * @return creation timestamp
   */
  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  /**
   * Returns the last mutation time.
   *
   * @return last-updated timestamp
   */
  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  protected void touch() {
    this.updatedAt = LocalDateTime.now();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MedicalEntity that = (MedicalEntity) o;
    return id.equals(that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
