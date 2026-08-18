package utils;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import constants.FrameworkConstants;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ExtentManager {

    private static ExtentReports extentReports;

    private ExtentManager() {
    }

    public static synchronized ExtentReports getExtentReports() {
        if (extentReports == null) {
            extentReports = createExtentReports();
        }

        return extentReports;
    }

    private static ExtentReports createExtentReports() {
        try {
            Files.createDirectories(FrameworkConstants.REPORT_DIRECTORY);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create Extent report directory", exception);
        }

        Path reportPath = FrameworkConstants.REPORT_DIRECTORY.resolve(FrameworkConstants.EXTENT_REPORT_FILE_NAME);
        ExtentSparkReporter sparkReporter = new ExtentSparkReporter(reportPath.toString());
        sparkReporter.config().setDocumentTitle("Arogga Automation Report");
        sparkReporter.config().setReportName("Arogga Web Regression Suite");

        ExtentReports reports = new ExtentReports();
        reports.attachReporter(sparkReporter);
        reports.setSystemInfo("Java", System.getProperty("java.version"));
        reports.setSystemInfo("OS", System.getProperty("os.name"));
        reports.setSystemInfo("Framework", "Selenium Java TestNG POM");
        return reports;
    }
}
