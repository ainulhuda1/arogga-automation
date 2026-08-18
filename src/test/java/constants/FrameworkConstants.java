package constants;

import java.nio.file.Path;

public final class FrameworkConstants {

    public static final Path TARGET_DIRECTORY = Path.of("target");
    public static final Path REPORT_DIRECTORY = TARGET_DIRECTORY.resolve("extent-report");
    public static final Path SCREENSHOT_DIRECTORY = TARGET_DIRECTORY.resolve("screenshots");
    public static final Path VISUAL_BASELINE_DIRECTORY = Path.of("src", "test", "resources", "visual-baselines");
    public static final Path VISUAL_ACTUAL_DIRECTORY = TARGET_DIRECTORY.resolve("visual-actual");

    public static final String EXTENT_REPORT_FILE_NAME = "Arogga-Automation-Report.html";

    private FrameworkConstants() {
    }
}
