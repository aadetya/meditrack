package com.airtribe.meditrack.util;

import com.airtribe.meditrack.exception.InvalidDataException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** Utility methods for Java serialization persistence. */
public final class SerializationUtil {
  private SerializationUtil() {}

  /**
   * Serializes an object to disk.
   *
   * @param path output path
   * @param obj serializable object
   * @param <T> object type
   */
  public static <T> void save(Path path, T obj) {
    Validator.requireNonNull("path", path);
    try {
      if (path.getParent() != null) {
        Files.createDirectories(path.getParent());
      }
      try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(path))) {
        out.writeObject(obj);
      }
    } catch (IOException e) {
      throw new InvalidDataException("Failed to serialize to: " + path, e);
    }
  }

  /**
   * Deserializes an object from disk.
   *
   * @param path source path
   * @param type expected root type
   * @param <T> object type
   * @return loaded object
   */
  public static <T> T load(Path path, Class<T> type) {
    Validator.requireNonNull("path", path);
    Validator.requireNonNull("type", type);
    try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(path))) {
      Object obj = in.readObject();
      if (!type.isInstance(obj)) {
        throw new InvalidDataException("Unexpected serialized type. Expected: " + type.getName());
      }
      return type.cast(obj);
    } catch (IOException | ClassNotFoundException e) {
      throw new InvalidDataException("Failed to deserialize from: " + path, e);
    }
  }
}
