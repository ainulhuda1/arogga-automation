package tests.admin;

import base.BaseTest;
import constants.TestGroups;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.admin.AdminLoginPage;
import pages.admin.DashboardPage;

public class AdminLoginTest extends BaseTest {

    private static final String ADMIN_GROUP = "admin";

    @BeforeMethod(alwaysRun = true)
    public void openAdminApplication() {
        driver.get(requiredValue("adminBaseUrl", "ADMIN_BASE_URL"));
    }

    @Test(groups = {ADMIN_GROUP, TestGroups.LOGIN, TestGroups.REGRESSION},
            description = "Verify Admin Login page loads successfully")
    public void verifyAdminLoginPageLoadsSuccessfully() {
        AdminLoginPage adminLoginPage = new AdminLoginPage(driver).waitUntilLoaded();

        Assert.assertTrue(adminLoginPage.isAdminLoginPageLoaded(), "Admin login page should load successfully");
        Assert.assertTrue(adminLoginPage.isUsernameInputDisplayed(), "Username input should be displayed");
        Assert.assertTrue(adminLoginPage.isLoginButtonDisplayed(), "Login button should be displayed");
    }

    @Test(groups = {ADMIN_GROUP, TestGroups.LOGIN, TestGroups.REGRESSION},
            description = "Verify Admin Login required field validation")
    public void verifyAdminLoginRequiredFieldValidation() {
        AdminLoginPage adminLoginPage = new AdminLoginPage(driver)
                .waitUntilLoaded()
                .submitLoginExpectingValidation();

        Assert.assertTrue(adminLoginPage.hasValidationMessage() || adminLoginPage.isAdminLoginPageLoaded(),
                "Admin login should show validation or keep the user on login page for empty credentials");
    }

    @Test(groups = {ADMIN_GROUP, TestGroups.LOGIN, TestGroups.REGRESSION},
            description = "Verify Admin can login using valid credentials")
    public void verifyAdminCanLoginUsingValidCredentials() {
        DashboardPage dashboardPage = new AdminLoginPage(driver)
                .waitUntilLoaded()
                .login(
                        requiredValue("adminUsername", "ADMIN_USERNAME"),
                        requiredValue("adminPassword", "ADMIN_PASSWORD")
                );

        Assert.assertTrue(dashboardPage.isDashboardLoaded(), "Valid admin credentials should open the dashboard");
    }

    private String requiredValue(String propertyName, String environmentName) {
        String value = optionalValue(propertyName, environmentName);

        if (value == null || value.isBlank()) {
            throw new SkipException(propertyName + " or " + environmentName + " is required for admin tests.");
        }

        return value;
    }

    private String optionalValue(String propertyName, String environmentName) {
        String propertyValue = System.getProperty(propertyName);

        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue;
        }

        return System.getenv(environmentName);
    }
}
