package utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public final class TestContext {

    public static final String GENERATED_ORDER_NUMBER = "generatedOrderNumber";
    public static final String GENERATED_SHIPMENT_ID = "generatedShipmentId";
    public static final String SELECTED_PRODUCT_NAME = "selectedProductName";
    public static final String SELECTED_PRODUCT_QUANTITY = "selectedProductQuantity";

    private static final ThreadLocal<Map<String, Object>> CONTEXT =
            ThreadLocal.withInitial(HashMap::new);
    private static final Map<String, String> SHARED_CONTEXT = new ConcurrentHashMap<>();
    private static final Path SHARED_CONTEXT_FILE = Path.of(
            System.getProperty("arogga.testContextFile", "target/generated-test-context.properties")
    );

    private TestContext() {
    }

    public static void set(String key, Object value) {
        if (value == null) {
            remove(key);
            return;
        }

        CONTEXT.get().put(key, value);
    }

    public static Optional<Object> get(String key) {
        Object threadValue = CONTEXT.get().get(key);
        if (threadValue != null) {
            return Optional.of(threadValue);
        }

        return Optional.ofNullable(SHARED_CONTEXT.get(key));
    }

    public static void remove(String key) {
        CONTEXT.get().remove(key);
    }

    public static void setGeneratedOrderNumber(String orderNumber) {
        setSharedString(GENERATED_ORDER_NUMBER, orderNumber);
    }

    public static Optional<String> getGeneratedOrderNumber() {
        return getSharedString(GENERATED_ORDER_NUMBER);
    }

    public static void setGeneratedShipmentId(String shipmentId) {
        setSharedString(GENERATED_SHIPMENT_ID, shipmentId);
    }

    public static Optional<String> getGeneratedShipmentId() {
        return getSharedString(GENERATED_SHIPMENT_ID);
    }

    public static void setSelectedProductName(String productName) {
        setSharedString(SELECTED_PRODUCT_NAME, productName);
    }

    public static Optional<String> getSelectedProductName() {
        return getSharedString(SELECTED_PRODUCT_NAME);
    }

    public static void setSelectedProductQuantity(String quantity) {
        setSharedString(SELECTED_PRODUCT_QUANTITY, quantity);
    }

    public static Optional<String> getSelectedProductQuantity() {
        return getSharedString(SELECTED_PRODUCT_QUANTITY);
    }

    public static void setSharedString(String key, String value) {
        String normalizedValue = normalize(value);
        if (key == null || key.isBlank() || normalizedValue.isBlank()) {
            return;
        }

        CONTEXT.get().put(key, normalizedValue);
        SHARED_CONTEXT.put(key, normalizedValue);
        persistSharedString(key, normalizedValue);
    }

    public static Optional<String> getSharedString(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }

        Object threadValue = CONTEXT.get().get(key);
        if (threadValue != null && !normalize(String.valueOf(threadValue)).isBlank()) {
            return Optional.of(normalize(String.valueOf(threadValue)));
        }

        String sharedValue = SHARED_CONTEXT.get(key);
        if (sharedValue != null && !normalize(sharedValue).isBlank()) {
            return Optional.of(normalize(sharedValue));
        }

        return readSharedString(key);
    }

    public static void clear() {
        CONTEXT.get().clear();
        CONTEXT.remove();
    }

    private static void persistSharedString(String key, String value) {
        Properties properties = readSharedProperties();
        properties.setProperty(key, value);

        try {
            Path parentDirectory = SHARED_CONTEXT_FILE.toAbsolutePath().getParent();
            if (parentDirectory != null) {
                Files.createDirectories(parentDirectory);
            }

            try (OutputStream outputStream = Files.newOutputStream(SHARED_CONTEXT_FILE)) {
                properties.store(outputStream, "Generated values shared between automation flows");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to persist shared test context to " + SHARED_CONTEXT_FILE, exception);
        }
    }

    private static Optional<String> readSharedString(String key) {
        String value = readSharedProperties().getProperty(key);
        String normalizedValue = normalize(value);
        if (normalizedValue.isBlank()) {
            return Optional.empty();
        }

        SHARED_CONTEXT.put(key, normalizedValue);
        return Optional.of(normalizedValue);
    }

    private static Properties readSharedProperties() {
        Properties properties = new Properties();
        if (!Files.exists(SHARED_CONTEXT_FILE)) {
            return properties;
        }

        try (InputStream inputStream = Files.newInputStream(SHARED_CONTEXT_FILE)) {
            properties.load(inputStream);
            return properties;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read shared test context from " + SHARED_CONTEXT_FILE, exception);
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
