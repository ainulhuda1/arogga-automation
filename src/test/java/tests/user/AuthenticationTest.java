package tests.user;

import base.BaseTest;
import constants.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;
import pages.user.HomePage;

public class AuthenticationTest extends BaseTest {

    @Test(groups = {TestGroups.AUTH, TestGroups.REGRESSION},
            description = "Verify valid OTP (1234) logs in successfully")
    public void verifyValidOtpLogsInSuccessfully() {
        HomePage homePage = loginWithValidCredentials();

        Assert.assertTrue(homePage.isSessionActive(), "Valid OTP should authenticate the user");
    }

    @Test(groups = {TestGroups.AUTH, TestGroups.REGRESSION},
            description = "Verify successful login redirects to Home")
    public void verifySuccessfulLoginRedirectsToHome() {
        HomePage homePage = loginWithValidCredentials();

        Assert.assertTrue(homePage.isHomePageLoaded(), "Successful login should redirect to the Home page");
    }

    @Test(groups = {TestGroups.AUTH, TestGroups.REGRESSION},
            description = "Verify session persists after refresh")
    public void verifySessionPersistsAfterRefresh() {
        HomePage homePage = loginWithValidCredentials().refreshAndWaitUntilLoaded();

        Assert.assertTrue(homePage.isSessionActive(), "Logged-in session should persist after refresh");
    }

    @Test(groups = {TestGroups.AUTH, TestGroups.REGRESSION},
            description = "Verify Logout Cancel keeps session")
    public void verifyLogoutCancelKeepsSession() {
        HomePage homePage = loginWithValidCredentials()
                .clickLogout();

        Assert.assertTrue(homePage.isLogoutConfirmationDisplayed(), "Logout confirmation should be displayed");

        homePage.cancelLogout();

        Assert.assertTrue(homePage.isSessionActive(), "Canceling logout should keep the user logged in");
    }

    @Test(groups = {TestGroups.AUTH, TestGroups.REGRESSION},
            description = "Verify Logout Confirm logs out")
    public void verifyLogoutConfirmLogsOut() {
        HomePage homePage = loginWithValidCredentials()
                .clickLogout();

        Assert.assertTrue(homePage.isLogoutConfirmationDisplayed(), "Logout confirmation should be displayed");

        homePage.confirmLogout();

        Assert.assertTrue(homePage.isLoggedOut(), "Confirming logout should log the user out");
    }
}
