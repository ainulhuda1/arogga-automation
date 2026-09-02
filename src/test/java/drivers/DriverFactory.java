package drivers;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import utils.ConfigReader;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class DriverFactory {

    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();
    private static final Set<String> RESOLVED_DRIVER_BINARIES = ConcurrentHashMap.newKeySet();

    private DriverFactory() {
    }

    public static WebDriver initDriver(ConfigReader config) {
        if (getDriver() != null) {
            return getDriver();
        }

        WebDriver driver = createDriver(config);

        driver.manage().timeouts().pageLoadTimeout(
                Duration.ofSeconds(config.pageLoadTimeoutSeconds())
        );

        DRIVER.set(driver);

        return driver;
    }

    public static WebDriver getDriver() {
        return DRIVER.get();
    }

    public static WebDriver restartDriver(ConfigReader config) {
        quitDriver();
        return initDriver(config);
    }

    public static void quitDriver() {
        WebDriver driver = getDriver();

        try {
            if (driver != null) {
                driver.quit();
            }
        } catch (RuntimeException ignored) {
            // The browser may already be gone after a renderer/session crash.
        } finally {
            DRIVER.remove();
        }
    }

    private static WebDriver createDriver(ConfigReader config) {

        String normalizedBrowser =
                config.browser() == null
                        ? "chrome"
                        : config.browser().trim().toLowerCase();

        return switch (normalizedBrowser) {

            // ====================================================
            // FIREFOX
            // ====================================================
            case "firefox" -> {

                setupDriverBinaryOnce("firefox");

                FirefoxOptions options = new FirefoxOptions();

                if (config.headless()) {
                    options.addArguments("-headless");
                    options.addArguments("--width=1440");
                    options.addArguments("--height=1200");
                }

                yield new FirefoxDriver(options);
            }

            // ====================================================
            // EDGE
            // ====================================================
            case "edge" -> {

                setupDriverBinaryOnce("edge");

                EdgeOptions options = new EdgeOptions();

                options.addArguments("--remote-allow-origins=*");

                options.setCapability(
                        "goog:loggingPrefs",
                        createLoggingPreferences()
                );

                if (config.headless()) {
                    options.addArguments("--headless=new");
                    options.addArguments("--window-size=1440,1200");
                    options.addArguments("--no-sandbox");
                }

                yield new EdgeDriver(options);
            }

            // ====================================================
            // CHROME
            // ====================================================
            case "chrome" -> {

                setupDriverBinaryOnce("chrome");

                ChromeOptions options = new ChromeOptions();

                options.addArguments("--remote-allow-origins=*");
                options.addArguments("--disable-notifications");
                options.addArguments("--disable-extensions");
                options.addArguments("--disable-dev-shm-usage");
                options.addArguments("--disable-gpu");
                options.addArguments("--disable-background-timer-throttling");
                options.addArguments("--disable-backgrounding-occluded-windows");
                options.addArguments("--disable-renderer-backgrounding");
                options.addArguments("--no-first-run");
                options.addArguments("--no-default-browser-check");

                options.setCapability(
                        "goog:loggingPrefs",
                        createLoggingPreferences()
                );

                if (config.headless()) {
                    options.addArguments("--headless=new");
                    options.addArguments("--window-size=1440,1200");
                    options.addArguments("--no-sandbox");
                }

                yield new ChromeDriver(options);
            }

            default -> throw new IllegalArgumentException(
                    "Unsupported browser: " + config.browser()
            );
        };
    }

    private static void setupDriverBinaryOnce(String browser) {
        if (!RESOLVED_DRIVER_BINARIES.add(browser)) {
            return;
        }

        switch (browser) {
            case "firefox" -> WebDriverManager.firefoxdriver().setup();
            case "edge" -> WebDriverManager.edgedriver().setup();
            case "chrome" -> WebDriverManager.chromedriver().setup();
            default -> throw new IllegalArgumentException("Unsupported browser: " + browser);
        }
    }

    private static LoggingPreferences createLoggingPreferences() {

        LoggingPreferences loggingPreferences =
                new LoggingPreferences();

        loggingPreferences.enable(
                LogType.BROWSER,
                Level.SEVERE
        );

        loggingPreferences.enable(
                LogType.PERFORMANCE,
                Level.ALL
        );

        return loggingPreferences;
    }
}
