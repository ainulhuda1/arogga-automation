package tests.user;

import base.BaseTest;
import constants.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;
import pages.user.HomePage;
import utils.VisualRegressionUtils;

public class VisualRegressionTest extends BaseTest {

    @Test(groups = {TestGroups.VISUAL, TestGroups.REGRESSION},
            description = "Verify no unexpected UI changes after deployment")
    public void verifyNoUnexpectedUiChangesAfterDeployment() {
        new HomePage(driver).waitUntilLoaded();

        VisualRegressionUtils.VisualComparisonResult result =
                VisualRegressionUtils.comparePageWithBaseline(
                        driver,
                        "home-page",
                        config.visualBaselineEnforced(),
                        config.visualMismatchThresholdPercent()
                );

        Assert.assertTrue(result.passed(), result.message());
    }
}
