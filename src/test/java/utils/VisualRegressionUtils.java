package utils;

import constants.FrameworkConstants;
import org.openqa.selenium.WebDriver;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public final class VisualRegressionUtils {

    private VisualRegressionUtils() {
    }

    public static VisualComparisonResult comparePageWithBaseline(
            WebDriver driver,
            String baselineName,
            boolean enforceBaseline,
            double mismatchThresholdPercent
    ) {
        try {
            Files.createDirectories(FrameworkConstants.VISUAL_BASELINE_DIRECTORY);
            Files.createDirectories(FrameworkConstants.VISUAL_ACTUAL_DIRECTORY);

            Path actualPath = ScreenshotUtils.captureScreenshotAsPath(driver, baselineName + "_actual")
                    .orElseThrow(() -> new IllegalStateException("Unable to capture actual screenshot"));
            Path normalizedActualPath = FrameworkConstants.VISUAL_ACTUAL_DIRECTORY.resolve(baselineName + ".png");
            Files.copy(actualPath, normalizedActualPath, REPLACE_EXISTING);

            Path baselinePath = FrameworkConstants.VISUAL_BASELINE_DIRECTORY.resolve(baselineName + ".png");
            if (Files.notExists(baselinePath)) {
                String message = "Baseline does not exist: " + baselinePath.toAbsolutePath()
                        + ". Actual screenshot: " + normalizedActualPath.toAbsolutePath();
                return new VisualComparisonResult(!enforceBaseline, 0.0, message);
            }

            double mismatch = calculateMismatchPercentage(baselinePath, normalizedActualPath);
            boolean passed = mismatch <= mismatchThresholdPercent;
            String message = "Visual mismatch: " + mismatch + "%, threshold: " + mismatchThresholdPercent + "%";
            return new VisualComparisonResult(passed, mismatch, message);
        } catch (IOException | RuntimeException exception) {
            return new VisualComparisonResult(false, 100.0, exception.getMessage());
        }
    }

    private static double calculateMismatchPercentage(Path baselinePath, Path actualPath) throws IOException {
        BufferedImage baseline = ImageIO.read(baselinePath.toFile());
        BufferedImage actual = ImageIO.read(actualPath.toFile());

        if (baseline == null || actual == null) {
            throw new IOException("Unable to read baseline or actual screenshot");
        }

        if (baseline.getWidth() != actual.getWidth() || baseline.getHeight() != actual.getHeight()) {
            return 100.0;
        }

        long totalPixels = (long) baseline.getWidth() * baseline.getHeight();
        long mismatchedPixels = 0;

        for (int y = 0; y < baseline.getHeight(); y++) {
            for (int x = 0; x < baseline.getWidth(); x++) {
                if (baseline.getRGB(x, y) != actual.getRGB(x, y)) {
                    mismatchedPixels++;
                }
            }
        }

        return (mismatchedPixels * 100.0) / totalPixels;
    }

    public record VisualComparisonResult(boolean passed, double mismatchPercentage, String message) {
    }
}
