package base;

import drivers.DriverFactory;
import listeners.TestListener;
import org.aeonbits.owner.ConfigFactory;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import pages.user.HomePage;
import pages.user.LoginPage;
import utils.ConfigReader;

@Listeners(TestListener.class)
public abstract class BaseTest {

    protected WebDriver driver;
    protected ConfigReader config;

    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        config = ConfigFactory.create(ConfigReader.class, System.getProperties());
        driver = DriverFactory.initDriver(config);
        configureBrowserWindow();
        try {
            driver.get(config.baseUrl());
        } catch (WebDriverException exception) {
            if (!isRecoverableSessionFailure(exception)) {
                throw exception;
            }
            restartBrowserAtBaseUrl();
        }
    }

    private void configureBrowserWindow() {
        if (config.headless()) {
            driver.manage().window().setSize(new Dimension(1440, 1200));
        } else {
            driver.manage().window().maximize();
        }
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        DriverFactory.quitDriver();
    }

    public WebDriver getDriver() {
        return driver;
    }

    protected LoginPage openLoginPage() {
        return new LoginPage(driver).openLoginModal();
    }

    protected HomePage loginWithValidCredentials() {
        try {
            return loginWithValidCredentialsOnce();
        } catch (WebDriverException exception) {
            if (!isRecoverableSessionFailure(exception)) {
                throw exception;
            }
            restartBrowserAtBaseUrl();
            return loginWithValidCredentialsOnce();
        }
    }

    private HomePage loginWithValidCredentialsOnce() {
        HomePage homePage = new HomePage(driver).waitUntilLoaded();
        LoginPage loginPage = new LoginPage(driver);

        if (homePage.isSessionActiveNow() || loginPage.isAuthenticatedAfterHeaderSettles()) {
            return homePage.waitForAuthenticatedHeader();
        }

        return loginPage.loginWithOtp(config.validPhoneNumber(), config.validOtp())
                .waitForAuthenticatedHeader();
    }

    protected void restartBrowserAtBaseUrl() {
        driver = DriverFactory.restartDriver(config);
        configureBrowserWindow();
        driver.get(config.baseUrl());
    }

    protected boolean isRecoverableSessionFailure(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            String message = current.getMessage() == null ? "" : current.getMessage().toLowerCase();
            if (message.contains("invalid session id")
                    || message.contains("session deleted")
                    || message.contains("browser has closed")
                    || message.contains("disconnected")
                    || message.contains("unable to receive message from renderer")) {
                return true;
            }
            current = current.getCause();
        }

        return false;
    }

    protected void skipIfOtpExpiryIsNotSupported() {
        if (!config.otpExpirySupported()) {
            throw new SkipException("OTP expiry is disabled for this test environment.");
        }
    }

    protected void skipIfVisualBaselineIsNotEnforced() {
        if (!config.visualBaselineEnforced()) {
            throw new SkipException("Visual baseline enforcement is disabled for this test environment.");
        }
    }
}
