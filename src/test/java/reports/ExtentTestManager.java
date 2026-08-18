package reports;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;

public final class ExtentTestManager {

    private static final ThreadLocal<ExtentTest> EXTENT_TEST = new ThreadLocal<>();

    private ExtentTestManager() {
    }

    public static ExtentTest startTest(String testName) {
        ExtentReports reports = ExtentManager.getExtentReports();
        ExtentTest test;

        synchronized (reports) {
            test = reports.createTest(testName);
        }

        EXTENT_TEST.set(test);
        return test;
    }

    public static ExtentTest getTest() {
        return EXTENT_TEST.get();
    }

    public static void unload() {
        EXTENT_TEST.remove();
    }
}
