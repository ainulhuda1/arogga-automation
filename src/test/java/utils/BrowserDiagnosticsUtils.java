package utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class BrowserDiagnosticsUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Pattern JAVASCRIPT_ERROR_PATTERN = Pattern.compile(
            "(?i)(uncaught|typeerror|referenceerror|syntaxerror|rangeerror|evalerror|urierror|script error"
                    + "|cannot read|not defined|is not a function|exception)"
    );

    private BrowserDiagnosticsUtils() {
    }

    public static void clearBrowserLogs(WebDriver driver) {
        readLogEntries(driver, LogType.BROWSER);
        readLogEntries(driver, LogType.PERFORMANCE);
    }

    public static List<String> getSevereJavaScriptErrors(WebDriver driver) {
        return readLogEntries(driver, LogType.BROWSER)
                .stream()
                .filter(entry -> entry.getLevel().intValue() >= java.util.logging.Level.SEVERE.intValue())
                .map(LogEntry::getMessage)
                .filter(message -> message != null && JAVASCRIPT_ERROR_PATTERN.matcher(message).find())
                .filter(message -> !isIgnorableJavaScriptError(message))
                .distinct()
                .toList();
    }

    public static List<String> getFailedNetworkRequestsRelatedTo(WebDriver driver, String... urlFragments) {
        return getFailedNetworkRequestsRelatedTo(driver, true, urlFragments);
    }

    public static List<String> getFailedNonGetNetworkRequestsRelatedTo(WebDriver driver, String... urlFragments) {
        return getFailedNetworkRequestsRelatedTo(driver, false, urlFragments);
    }

    private static List<String> getFailedNetworkRequestsRelatedTo(
            WebDriver driver,
            boolean includeReadRequests,
            String... urlFragments
    ) {
        List<String> failures = new ArrayList<>();
        Map<String, String> requestUrls = new HashMap<>();
        Map<String, String> requestMethods = new HashMap<>();

        for (LogEntry entry : readLogEntries(driver, LogType.PERFORMANCE)) {
            JsonNode message = parsePerformanceMessage(entry.getMessage());
            if (message.isMissingNode()) {
                continue;
            }

            String method = message.path("method").asText("");
            JsonNode params = message.path("params");
            String requestId = params.path("requestId").asText("");

            if ("Network.requestWillBeSent".equals(method)) {
                JsonNode request = params.path("request");
                String url = request.path("url").asText("");
                String requestMethod = request.path("method").asText("");
                if (!requestId.isBlank() && !url.isBlank()) {
                    requestUrls.put(requestId, url);
                    requestMethods.put(requestId, requestMethod);
                }
                continue;
            }

            if ("Network.responseReceived".equals(method)) {
                JsonNode response = params.path("response");
                int statusCode = response.path("status").asInt(0);
                String url = response.path("url").asText(requestUrls.getOrDefault(requestId, ""));
                String requestMethod = requestMethods.getOrDefault(requestId, "");

                if (statusCode >= 400
                        && isRelatedUrl(url, urlFragments)
                        && shouldIncludeFailedRequest(requestMethod, includeReadRequests)
                        && !isIgnorableDiagnosticUrl(url)) {
                    failures.add(statusCode + " " + url);
                }
                continue;
            }

            if ("Network.loadingFailed".equals(method)) {
                String url = requestUrls.getOrDefault(requestId, "");
                String requestMethod = requestMethods.getOrDefault(requestId, "");
                String errorText = params.path("errorText").asText("Network loading failed");

                if (isRelatedUrl(url, urlFragments)
                        && shouldIncludeFailedRequest(requestMethod, includeReadRequests)
                        && !isIgnorableDiagnosticUrl(url)
                        && !isIgnorableLoadingFailure(errorText, url, requestMethod)) {
                    failures.add(errorText + " " + url);
                }
            }
        }

        return failures.stream().distinct().toList();
    }

    private static List<LogEntry> readLogEntries(WebDriver driver, String logType) {
        if (driver == null) {
            return List.of();
        }

        try {
            return new ArrayList<>(driver.manage().logs().get(logType).getAll());
        } catch (WebDriverException exception) {
            return List.of();
        }
    }

    private static boolean isIgnorableJavaScriptError(String message) {
        String normalizedMessage = message == null ? "" : message.toLowerCase(Locale.ROOT);

        return normalizedMessage.contains("api request error for url")
                && normalizedMessage.contains("failed to fetch")
                && isBackgroundLabServiceUrl(normalizedMessage);
    }

    private static JsonNode parsePerformanceMessage(String rawMessage) {
        try {
            return OBJECT_MAPPER.readTree(rawMessage).path("message");
        } catch (Exception exception) {
            return OBJECT_MAPPER.missingNode();
        }
    }

    private static boolean isRelatedUrl(String url, String... urlFragments) {
        String normalizedUrl = url == null ? "" : url.toLowerCase(Locale.ROOT);
        if (normalizedUrl.isBlank()) {
            return false;
        }

        if (urlFragments == null || urlFragments.length == 0) {
            return true;
        }

        for (String fragment : urlFragments) {
            if (fragment != null && !fragment.isBlank()
                    && normalizedUrl.contains(fragment.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }

        return false;
    }

    private static boolean shouldIncludeFailedRequest(String requestMethod, boolean includeReadRequests) {
        return includeReadRequests
                || requestMethod == null
                || requestMethod.isBlank()
                || !"GET".equalsIgnoreCase(requestMethod);
    }

    private static boolean isIgnorableDiagnosticUrl(String url) {
        String normalizedUrl = url == null ? "" : url.toLowerCase(Locale.ROOT);

        return normalizedUrl.contains("google-analytics.com/")
                || normalizedUrl.contains("googletagmanager.com/")
                || normalizedUrl.contains("analytics.google.com/")
                || normalizedUrl.contains("facebook.com/tr/")
                || normalizedUrl.contains("connect.facebook.net/")
                || normalizedUrl.contains("clarity.ms/")
                || normalizedUrl.contains("hotjar.com/")
                || isBackgroundLabServiceUrl(normalizedUrl);
    }

    private static boolean isBackgroundLabServiceUrl(String normalizedText) {
        return normalizedText != null
                && normalizedText.contains("labcluster.s.arogga.co");
    }

    private static boolean isIgnorableLoadingFailure(String errorText, String url, String requestMethod) {
        String normalizedError = errorText == null ? "" : errorText.toLowerCase(Locale.ROOT);
        String normalizedUrl = url == null ? "" : url.toLowerCase(Locale.ROOT);

        return "GET".equalsIgnoreCase(requestMethod)
                && normalizedError.contains("net::err_aborted")
                && !normalizedUrl.contains("/apiv")
                && !normalizedUrl.contains("/api/");
    }
}
