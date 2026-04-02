package core.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Utility class for reading and parsing JSON files.
 *
 * <p>Supports:
 * <ul>
 *   <li>Deserializing JSON to Java objects (POJO, Map, List)</li>
 *   <li>Reading JSON from classpath resources or file system</li>
 *   <li>Raw {@link JsonNode} access for flexible parsing</li>
 * </ul>
 *
 * @author Enterprise QA Team
 * @version 1.0
 */
public class JsonUtils {

    private static final Logger log = LogManager.getLogger(JsonUtils.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonUtils() {}

    /**
     * Reads a JSON file from the classpath and deserializes it to the given type.
     *
     * @param resourcePath classpath path (e.g., "testdata/json/users.json")
     * @param clazz        target class
     * @param <T>          type parameter
     * @return deserialized object
     */
    public static <T> T readFromClasspath(String resourcePath, Class<T> clazz) {
        log.debug("Reading JSON from classpath: {}", resourcePath);
        try (InputStream is = JsonUtils.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new RuntimeException("JSON resource not found: " + resourcePath);
            }
            return OBJECT_MAPPER.readValue(is, clazz);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse JSON from: " + resourcePath, e);
        }
    }

    /**
     * Reads a JSON file from the classpath into a List of objects.
     *
     * @param resourcePath classpath path
     * @param typeRef      Jackson type reference for List<T>
     * @param <T>          type parameter
     * @return list of objects
     */
    public static <T> List<T> readListFromClasspath(String resourcePath, TypeReference<List<T>> typeRef) {
        log.debug("Reading JSON list from classpath: {}", resourcePath);
        try (InputStream is = JsonUtils.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new RuntimeException("JSON resource not found: " + resourcePath);
            }
            return OBJECT_MAPPER.readValue(is, typeRef);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse JSON list from: " + resourcePath, e);
        }
    }

    /**
     * Reads a JSON file from the file system into a Map.
     *
     * @param filePath absolute or relative file path
     * @return Map representation of the JSON
     */
    public static Map<String, Object> readAsMap(String filePath) {
        log.debug("Reading JSON as map from: {}", filePath);
        try {
            return OBJECT_MAPPER.readValue(new File(filePath),
                new TypeReference<Map<String, Object>>() {});
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse JSON map from: " + filePath, e);
        }
    }

    /**
     * Reads raw JSON file from classpath into a {@link JsonNode}.
     *
     * @param resourcePath classpath path
     * @return root JsonNode
     */
    public static JsonNode readAsNode(String resourcePath) {
        log.debug("Reading JSON node from: {}", resourcePath);
        try (InputStream is = JsonUtils.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new RuntimeException("JSON resource not found: " + resourcePath);
            }
            return OBJECT_MAPPER.readTree(is);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse JSON node from: " + resourcePath, e);
        }
    }

    /**
     * Serializes an object to a JSON string.
     *
     * @param object the object to serialize
     * @return JSON string representation
     */
    public static String toJsonString(Object object) {
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize object to JSON", e);
        }
    }
}
