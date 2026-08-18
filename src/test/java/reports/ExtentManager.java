package reports;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import org.aeonbits.owner.ConfigFactory;
import utils.ConfigReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ExtentManager {

    private static final Path REPORT_PATH = Path.of("test-output", "ExtentReport.html");

    private static ExtentReports extentReports;

    private ExtentManager() {
    }

    public static synchronized ExtentReports getExtentReports() {
        if (extentReports == null) {
            extentReports = createExtentReports();
        }

        return extentReports;
    }

    public static synchronized void flushReports() {
        if (extentReports != null) {
            extentReports.flush();
        }
    }

    private static ExtentReports createExtentReports() {
        try {
            Files.createDirectories(REPORT_PATH.getParent());
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create Extent report directory", exception);
        }

        ExtentSparkReporter sparkReporter = new ExtentSparkReporter(REPORT_PATH.toString());
        sparkReporter.config().setDocumentTitle("Arogga Automation Report");
        sparkReporter.config().setReportName("Arogga Web Automation Results");
        sparkReporter.config().setTheme(Theme.STANDARD);

        ExtentReports reports = new ExtentReports();
        reports.attachReporter(sparkReporter);
        addSystemInfo(reports);
        return reports;
    }

    private static void addSystemInfo(ExtentReports reports) {
        ConfigReader config = ConfigFactory.create(ConfigReader.class, System.getProperties());

        reports.setSystemInfo("Tester", resolveValue("tester", System.getProperty("user.name", "Unknown")));
        reports.setSystemInfo("Browser", resolveValue("browser", config.browser()));
        reports.setSystemInfo("Environment", resolveEnvironment(config.baseUrl()));
        reports.setSystemInfo("Java Version", System.getProperty("java.version", "Unknown"));
        reports.setSystemInfo("OS", resolveOsInfo());
    }

    private static String resolveEnvironment(String baseUrl) {
        String configuredEnvironment = System.getProperty("environment");
        if (configuredEnvironment != null && !configuredEnvironment.isBlank()) {
            return configuredEnvironment.trim();
        }

        String normalizedBaseUrl = baseUrl == null ? "" : baseUrl.toLowerCase();
        if (normalizedBaseUrl.contains("dev")) {
            return "dev";
        }
        if (normalizedBaseUrl.contains("qa")) {
            return "qa";
        }
        if (normalizedBaseUrl.contains("staging")) {
            return "staging";
        }
        if (normalizedBaseUrl.contains("prod")) {
            return "prod";
        }

        return "Unknown";
    }

    private static String resolveValue(String systemPropertyName, String defaultValue) {
        String systemPropertyValue = System.getProperty(systemPropertyName);
        if (systemPropertyValue != null && !systemPropertyValue.isBlank()) {
            return systemPropertyValue.trim();
        }

        return defaultValue == null || defaultValue.isBlank() ? "Unknown" : defaultValue.trim();
    }

    private static String resolveOsInfo() {
        String osName = System.getProperty("os.name", "Unknown");
        String osVersion = System.getProperty("os.version", "");
        return (osName + " " + osVersion).trim();
    }
}
