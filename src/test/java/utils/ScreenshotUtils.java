package utils;

import constants.FrameworkConstants;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public final class ScreenshotUtils {

    private static final DateTimeFormatter FILE_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private ScreenshotUtils() {
    }

    public static String captureScreenshot(WebDriver driver, String testName) {
        return captureScreenshotAsPath(driver, testName).map(Path::toString).orElse("");
    }

    public static java.util.Optional<Path> captureScreenshotAsPath(WebDriver driver, String testName) {
        if (!(driver instanceof TakesScreenshot)) {
            return java.util.Optional.empty();
        }

        try {
            Path screenshotDirectory = FrameworkConstants.SCREENSHOT_DIRECTORY;
            Files.createDirectories(screenshotDirectory);

            File sourceFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Path destination = screenshotDirectory.resolve(buildScreenshotFileName(testName));
            Files.copy(sourceFile.toPath(), destination, REPLACE_EXISTING);

            return java.util.Optional.of(destination.toAbsolutePath());
        } catch (IOException | WebDriverException exception) {
            return java.util.Optional.empty();
        }
    }

    private static String buildScreenshotFileName(String testName) {
        String safeTestName = testName == null ? "test" : testName.replaceAll("[^a-zA-Z0-9._-]", "_");
        return safeTestName + "_" + LocalDateTime.now().format(FILE_TIMESTAMP) + ".png";
    }
}
