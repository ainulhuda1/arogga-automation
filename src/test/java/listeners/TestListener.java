package listeners;

import base.BaseTest;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import drivers.DriverFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.IConfigurationListener;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.ITestContext;
import reports.ExtentManager;
import reports.ExtentTestManager;
import utils.ScreenshotUtils;
import utils.TestContext;

public class TestListener implements ITestListener, ISuiteListener, IConfigurationListener {

    private static final Logger LOGGER = LogManager.getLogger(TestListener.class);

    @Override
    public void onTestStart(ITestResult result) {
        ExtentTestManager.startTest(resolveTestName(result));
        LOGGER.info("Starting test: {}", result.getName());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        getCurrentTest(result).log(Status.PASS, "Test passed");
        ExtentTestManager.unload();
        LOGGER.info("Test passed: {}", result.getName());
    }

    @Override
    public void onTestFailure(ITestResult result) {
        ExtentTest test = getCurrentTest(result);
        WebDriver driver = resolveDriver(result);
        String screenshotPath = ScreenshotUtils.captureScreenshot(driver, result.getName());

        if (result.getThrowable() == null) {
            test.fail("Test failed without exception details.");
        } else {
            test.fail(result.getThrowable());
        }

        if (screenshotPath.isBlank()) {
            LOGGER.error("Test failed and screenshot capture was skipped: {}", result.getName());
            ExtentTestManager.unload();
            return;
        }

        test.addScreenCaptureFromPath(screenshotPath);
        ExtentTestManager.unload();
        LOGGER.error("Screenshot captured for failed test {}: {}", result.getName(), screenshotPath);
        Reporter.log("Screenshot captured: " + screenshotPath, true);
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        ExtentTest test = getCurrentTest(result);
        Throwable throwable = result.getThrowable();
        if (result.getThrowable() == null) {
            test.skip("Test skipped");
        } else {
            test.skip(throwable);
        }
        ExtentTestManager.unload();
        if (throwable == null) {
            LOGGER.warn("Test skipped: {}", result.getName());
        } else {
            LOGGER.warn("Test skipped: {} - {}", result.getName(), throwable.getMessage(), throwable);
        }
    }

    @Override
    public void onConfigurationFailure(ITestResult result) {
        Throwable throwable = result.getThrowable();
        if (throwable == null) {
            LOGGER.error("Configuration failed: {}", result.getName());
        } else {
            LOGGER.error("Configuration failed: {} - {}", result.getName(), throwable.getMessage(), throwable);
        }
    }

    @Override
    public void onFinish(ITestContext context) {
        TestContext.clear();
    }

    @Override
    public void onFinish(ISuite suite) {
        ExtentManager.flushReports();
    }

    private ExtentTest getCurrentTest(ITestResult result) {
        ExtentTest test = ExtentTestManager.getTest();
        if (test == null) {
            return ExtentTestManager.startTest(resolveTestName(result));
        }

        return test;
    }

    private String resolveTestName(ITestResult result) {
        String description = result.getMethod().getDescription();
        return description == null || description.isBlank()
                ? result.getName()
                : description;
    }

    private WebDriver resolveDriver(ITestResult result) {
        Object testInstance = result.getInstance();

        if (testInstance instanceof BaseTest baseTest) {
            return baseTest.getDriver();
        }

        return DriverFactory.getDriver();
    }
}
