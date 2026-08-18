package tests.user;

import base.BaseTest;
import constants.TestGroups;
import org.openqa.selenium.WebDriverException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;
import pages.user.HomePage;
import pages.user.LoginPage;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class


LoginTest extends BaseTest {

    private static final String INVALID_OTP_VALIDATION_MESSAGE = "Error verifying your code";
    private static final String ALPHANUMERIC_OTP = "12ab!@34";
    private static final String OVERSIZED_OTP = "123456";

    @Override
    @BeforeClass(alwaysRun = true)
    public void setUp() {
        super.setUp();
    }

    @Override
    @AfterClass(alwaysRun = true)
    public void tearDown() {
        super.tearDown();
    }

    @BeforeMethod(alwaysRun = true)
    public void resetLoginTestState() {
        restartBrowserAtBaseUrl();
        try {
            resetLoginTestStateOnce();
        } catch (WebDriverException exception) {
            if (!isRecoverableSessionFailure(exception)) {
                throw exception;
            }
            restartBrowserAtBaseUrl();
            resetLoginTestStateOnce();
        }
    }

    private void resetLoginTestStateOnce() {
        closeExtraBrowserWindows();
        driver.get(config.baseUrl());
        ensureLoggedOut();
        driver.get(config.baseUrl());
    }

    @Test(groups = {TestGroups.LOGIN, TestGroups.REGRESSION},
            description = "Verify Login page loads successfully")
    public void verifyLoginPageLoadsSuccessfully() {
        LoginPage loginPage = openLoginPage();

        Assert.assertTrue(loginPage.isLoginPageLoaded(), "Login modal should load successfully");
    }

    @Test(groups = {TestGroups.LOGIN, TestGroups.REGRESSION},
            description = "Verify Phone Number field is displayed")
    public void verifyPhoneNumberFieldIsDisplayed() {
        Assert.assertTrue(openLoginPage().isPhoneNumberInputDisplayed(), "Phone number input should be displayed");
    }

    @Test(groups = {TestGroups.LOGIN, TestGroups.REGRESSION},
            description = "Verify Send button is displayed")
    public void verifySendButtonIsDisplayed() {
        Assert.assertTrue(openLoginPage().isSendButtonDisplayed(), "Send button should be displayed");
    }

    @Test(groups = {TestGroups.LOGIN, TestGroups.REGRESSION},
            description = "Verify Terms & Conditions link is displayed")
    public void verifyTermsAndConditionsLinkIsDisplayed() {
        Assert.assertTrue(openLoginPage().isTermsAndConditionsLinkDisplayed(),
                "Terms & Conditions link should be displayed");
    }

    @Test(groups = {TestGroups.LOGIN, TestGroups.REGRESSION},
            description = "Verify Privacy Policy link is displayed")
    public void verifyPrivacyPolicyLinkIsDisplayed() {
        Assert.assertTrue(openLoginPage().isPrivacyPolicyLinkDisplayed(),
                "Privacy Policy link should be displayed");
    }

    @Test(groups = {TestGroups.LOGIN, TestGroups.REGRESSION},
            description = "Verify Refund & Return Policy link is displayed")
    public void verifyRefundReturnPolicyLinkIsDisplayed() {
        Assert.assertTrue(openLoginPage().isRefundReturnPolicyLinkDisplayed(),
                "Refund & Return Policy link should be displayed");
    }

    @Test(groups = {TestGroups.LOGIN, TestGroups.REGRESSION},
            description = "Verify valid phone number can be entered")
    public void verifyValidPhoneNumberCanBeEntered() {
        LoginPage loginPage = openLoginPage().enterPhoneNumber(config.validPhoneNumber());

        Assert.assertEquals(loginPage.getPhoneNumber(), config.validPhoneNumber(),
                "Entered phone number should match test data");
    }

    @Test(groups = {TestGroups.LOGIN, TestGroups.REGRESSION},
            description = "Verify empty phone number validation")
    public void verifyEmptyPhoneNumberValidation() {
        LoginPage loginPage = openLoginPage().submitEmptyPhoneNumber();

        Assert.assertEquals(loginPage.getPhoneNumberValidationMessage(), "Please enter phone number",
                "Empty phone number validation message should be displayed");
    }

    @Test(groups = {TestGroups.LOGIN, TestGroups.REGRESSION},
            description = "Verify invalid phone length validation")
    public void verifyInvalidPhoneLengthValidation() {
        LoginPage loginPage = openLoginPage()
                .submitPhoneNumberExpectingValidation(config.invalidShortPhoneNumber());

        Assert.assertTrue(loginPage.hasPhoneNumberValidationMessage() || loginPage.isLoginModalDisplayed(),
                "Invalid phone length should not proceed to a successful login state");
    }

    @Test(groups = {TestGroups.LOGIN, TestGroups.REGRESSION},
            description = "Verify non-numeric characters validation")
    public void verifyNonNumericCharactersValidation() {
        LoginPage loginPage = openLoginPage()
                .enterPhoneNumber(config.nonNumericPhoneNumber());

        if (!loginPage.isPhoneNumberNumericOnly()) {
            loginPage.submitPhoneNumberExpectingValidation(config.nonNumericPhoneNumber());
        }

        Assert.assertTrue(loginPage.isPhoneNumberNumericOnly() || loginPage.hasPhoneNumberValidationMessage(),
                "Phone field should block non-numeric characters or show validation");
    }

    @Test(groups = {TestGroups.LOGIN, TestGroups.REGRESSION},
            description = "Verify Send button enabled after valid phone")
    public void verifySendButtonEnabledAfterValidPhone() {
        LoginPage loginPage = openLoginPage().enterPhoneNumber(config.validPhoneNumber());

        Assert.assertTrue(loginPage.isSendButtonEnabled(), "Send button should be enabled for valid phone");
    }

    @Test(groups = {TestGroups.LOGIN, TestGroups.REGRESSION},
            description = "Verify Google Login button")
    public void verifyGoogleLoginButton() {
        LoginPage loginPage = openLoginPage();

        Assert.assertTrue(loginPage.isGoogleSocialLoginButtonDisplayed(),
                "Google social login button should be displayed");
        Assert.assertTrue(loginPage.isGoogleSocialLoginIconLoaded(),
                "Google social login icon should be loaded");
    }

    @Test(groups = {TestGroups.LOGIN, TestGroups.REGRESSION},
            description = "Verify LinkedIn Login button")
    public void verifyLinkedInLoginButton() {
        LoginPage loginPage = openLoginPage();

        Assert.assertTrue(loginPage.isLinkedInSocialLoginButtonDisplayed(),
                "LinkedIn social login button should be displayed");
        Assert.assertTrue(loginPage.isLinkedInSocialLoginIconLoaded(),
                "LinkedIn social login icon should be loaded");
    }

    @Test(groups = {TestGroups.LOGIN, TestGroups.REGRESSION},
            description = "Verify successful login using valid phone number and valid OTP")
    public void verifySuccessfulLoginUsingValidPhoneNumberAndValidOtp() {
        HomePage homePage = loginSuccessfully();

        Assert.assertTrue(homePage.isSessionActive(),
                "Valid phone number and valid OTP should authenticate the user");
    }

    @Test(groups = {TestGroups.LOGIN, TestGroups.REGRESSION},
            description = "Verify user is redirected to Home page after successful login")
    public void verifyUserRedirectedToHomePageAfterSuccessfulLogin() {
        HomePage homePage = loginSuccessfully();

        Assert.assertTrue(homePage.isHomePageLoaded(),
                "Successful login should redirect the user to the Home page");
    }

    @Test(groups = {TestGroups.LOGIN, TestGroups.REGRESSION},
            description = "Verify authenticated header menus and layout after login")
    public void verifyUserProfileAccountIconIsDisplayedAfterLogin() {
        HomePage homePage = loginSuccessfully()
                .waitForAuthenticatedHeader();
        String urlAfterLogin = homePage.getCurrentPageUrl();
        long pageTimeOriginAfterLogin = homePage.getPageTimeOriginMillis();
        String cartBadgeText = homePage.getHeaderCartBadgeText();
        List<String> brokenHeaderAssets = homePage.getBrokenHeaderIconOrPlaceholderSources();
        List<String> headerLayoutIssues = homePage.getHeaderTextTruncationOrOverlapIssues();
        SoftAssert softAssert = new SoftAssert();

        softAssert.assertTrue(homePage.isAccountMenuVisibleAndClickable(),
                "Account menu should be visible and clickable after successful login");
        softAssert.assertTrue(homePage.isOrdersMenuVisible(),
                "Orders menu should be visible after successful login");
        softAssert.assertTrue(homePage.isInboxMenuVisible(),
                "Inbox menu should be visible after successful login");
        softAssert.assertTrue(homePage.isCartIconVisible(),
                "Cart icon should be visible after successful login");
        softAssert.assertTrue(homePage.isHeaderCartBadgeDisplayed(),
                "Cart badge should be displayed with the current cart count. Actual badge: " + cartBadgeText);
        softAssert.assertTrue(homePage.isLoginButtonHidden(),
                "Login button should no longer be displayed after successful login");
        softAssert.assertTrue(brokenHeaderAssets.isEmpty(),
                "Header should not contain broken icons or placeholder images: " + brokenHeaderAssets);
        softAssert.assertTrue(headerLayoutIssues.isEmpty(),
                "Header should not have UI overlap or text truncation after successful login: "
                        + headerLayoutIssues);
        softAssert.assertTrue(homePage.isHeaderLayoutAligned(),
                "Header layout should remain properly aligned after successful login");
        softAssert.assertEquals(homePage.getCurrentPageUrl(), urlAfterLogin,
                "Header verification should not trigger an unexpected redirect");
        softAssert.assertEquals(homePage.getPageTimeOriginMillis(), pageTimeOriginAfterLogin,
                "Header verification should not trigger an unexpected page refresh");
        softAssert.assertAll();
    }

    @Test(groups = {TestGroups.LOGIN, TestGroups.REGRESSION},
            description = "Verify invalid OTP shows proper validation message")
    public void verifyInvalidOtpShowsProperValidationMessage() {
        LoginPage loginPage = openOtpVerification()
                .submitOtpExpectingValidation(config.invalidOtp());

        Assert.assertTrue(loginPage.hasOtpErrorMessageContaining(INVALID_OTP_VALIDATION_MESSAGE),
                "Invalid OTP should show the expected validation message. Actual message: "
                        + loginPage.getOtpErrorMessage());
    }

    @Test(groups = {TestGroups.LOGIN, TestGroups.REGRESSION},
            description = "Verify expired OTP shows proper validation message")
    public void verifyExpiredOtpShowsProperValidationMessage() {
        // TODO: Enable this scenario when the automation environment supports real OTP expiry validation.
        skipIfOtpExpiryIsNotSupported();

        LoginPage loginPage = openOtpVerification()
                .waitForOtpExpiry(Duration.ofSeconds(config.otpExpiryTimeoutSeconds()))
                .submitOtpExpectingValidation(config.validOtp());

        Assert.assertFalse(loginPage.isAuthenticated(), "Expired OTP should not authenticate the user");
        Assert.assertTrue(loginPage.hasOtpErrorMessage(),
                "Expired OTP should show a validation message and keep the user on OTP verification");
    }

    @Test(groups = {TestGroups.LOGIN, TestGroups.REGRESSION},
            description = "Verify incorrect OTP cannot complete login")
    public void verifyIncorrectOtpCannotCompleteLogin() {
        LoginPage loginPage = openOtpVerification()
                .submitOtpExpectingValidation(config.invalidOtp());

        Assert.assertFalse(loginPage.isAuthenticated(), "Incorrect OTP must not authenticate the user");
        Assert.assertTrue(loginPage.isOtpVerificationDisplayed(),
                "Incorrect OTP should keep the user on OTP verification");
    }

    @Test(groups = {TestGroups.LOGIN, TestGroups.REGRESSION},
            description = "Verify Resend OTP button is displayed")
    public void verifyResendOtpButtonIsDisplayed() {
        LoginPage loginPage = openOtpVerification()
                .waitUntilResendOtpAvailable();

        Assert.assertTrue(loginPage.isResendOtpButtonDisplayed(),
                "Resend OTP button should be displayed after the OTP timer completes");
    }

    @Test(groups = {TestGroups.LOGIN, TestGroups.REGRESSION},
            description = "Verify clicking Resend OTP generates a new OTP request")
    public void verifyClickingResendOtpGeneratesNewOtpRequest() {
        LoginPage loginPage = openOtpVerification()
                .waitUntilResendOtpAvailable()
                .clickResendOtp();

        Assert.assertTrue(loginPage.hasOtpRequestConfirmationMessage() || loginPage.isOtpTimerDisplayed(),
                "Clicking Resend OTP should generate a new OTP request or restart the OTP timer");
    }

    @Test(groups = {TestGroups.LOGIN, TestGroups.REGRESSION},
            description = "Verify OTP input field is displayed after clicking Send")
    public void verifyOtpInputFieldIsDisplayedAfterClickingSend() {
        LoginPage loginPage = openOtpVerification();

        Assert.assertTrue(loginPage.isOtpInputDisplayed(),
                "OTP input field should be displayed after clicking Send with a valid phone number");
    }

    @Test(groups = {TestGroups.LOGIN, TestGroups.REGRESSION},
            description = "Verify OTP input accepts only numeric values")
    public void verifyOtpInputAcceptsOnlyNumericValues() {
        LoginPage loginPage = openOtpVerification()
                .enterOtp(ALPHANUMERIC_OTP);

        Assert.assertTrue(loginPage.isOtpNumericOnly(),
                "OTP input should reject non-numeric characters. Actual OTP value: " + loginPage.getOtp());
    }

    @Test(groups = {TestGroups.LOGIN, TestGroups.REGRESSION},
            description = "Verify OTP length validation")
    public void verifyOtpLengthValidation() {
        LoginPage loginPage = openOtpVerification()
                .submitOtpExpectingValidation(OVERSIZED_OTP);

        Assert.assertFalse(loginPage.isAuthenticated(),
                "OTP longer than " + LoginPage.EXPECTED_OTP_LENGTH + " digits should not authenticate the user");
        Assert.assertTrue(loginPage.hasOtpErrorMessage() || loginPage.isOtpVerificationDisplayed(),
                "OTP length validation should reject oversized OTP values and keep the user on OTP verification");
    }

    @Test(groups = {TestGroups.LOGIN, TestGroups.REGRESSION},
            description = "Verify login session remains active after page refresh")
    public void verifyLoginSessionRemainsActiveAfterPageRefresh() {
        HomePage homePage = loginSuccessfully()
                .refreshAndWaitUntilLoaded();

        Assert.assertTrue(homePage.isSessionActive(),
                "Logged-in session should remain active after refreshing the page");
    }

    @Test(groups = {TestGroups.LOGIN, TestGroups.REGRESSION},
            description = "Verify browser Back button does not return to the Login page after successful login")
    public void verifyBrowserBackButtonDoesNotReturnToLoginPageAfterSuccessfulLogin() {
        loginSuccessfully();
        driver.navigate().back();

        LoginPage loginPage = new LoginPage(driver);

        Assert.assertTrue(loginPage.isLoginModalClosed(),
                "Browser Back should not reopen the Login modal after successful login. Current URL: "
                        + loginPage.getCurrentPageUrl());

        driver.get(config.baseUrl());
        HomePage homePage = new HomePage(driver).waitUntilLoaded();

        Assert.assertTrue(homePage.isSessionActive(),
                "Authenticated session should remain active after using browser Back");
    }

    @Test(groups = {TestGroups.LOGIN, TestGroups.REGRESSION},
            description = "Verify logout functionality")
    public void verifyLogoutFunctionality() {
        HomePage homePage = loginSuccessfully()
                .clickLogout();

        Assert.assertTrue(homePage.isLogoutConfirmationDisplayed(),
                "Logout confirmation should be displayed before completing logout");

        homePage.confirmLogout();

        Assert.assertTrue(homePage.isLoggedOut(), "Confirming logout should log the user out");
    }

    /*@Test(groups = {TestGroups.LOGIN, TestGroups.REGRESSION},
            description = "Verify user is redirected to Login page after logout")
    public void verifyUserIsRedirectedToLoginPageAfterLogout() {
        HomePage homePage = logoutSuccessfully();

        Assert.assertTrue(homePage.isLoggedOut(),
                "After logout, the header login action should be displayed");

        // TODO: Replace with a dedicated Login page URL assertion if the application introduces one.
        Assert.assertTrue(openLoginPage().isLoginPageLoaded(),
                "Logged-out user should be able to open the Login modal from the header");
    }*/

    @Test(groups = {TestGroups.LOGIN, TestGroups.REGRESSION},
            description = "Verify user can login again after logout")
    public void verifyUserCanLoginAgainAfterLogout() {
        logoutSuccessfully();

        HomePage homePage = loginSuccessfully();

        Assert.assertTrue(homePage.isSessionActive(),
                "User should be able to login again after completing logout");
    }

    @Test(groups = {TestGroups.LOGIN, TestGroups.REGRESSION},
            description = "Verify Terms & Conditions link opens the correct page")
    public void verifyTermsAndConditionsLinkOpensCorrectPage() {
        LoginPage loginPage = openLoginPage()
                .openTermsAndConditionsPage();

        Assert.assertTrue(loginPage.isOnTermsAndConditionsPage(),
                "Terms & Conditions link should open the correct page. Current URL: "
                        + loginPage.getCurrentPageUrl());
    }

    @Test(groups = {TestGroups.LOGIN, TestGroups.REGRESSION},
            description = "Verify Privacy Policy link opens the correct page")
    public void verifyPrivacyPolicyLinkOpensCorrectPage() {
        LoginPage loginPage = openLoginPage()
                .openPrivacyPolicyPage();

        Assert.assertTrue(loginPage.isOnPrivacyPolicyPage(),
                "Privacy Policy link should open the correct page. Current URL: "
                        + loginPage.getCurrentPageUrl());
    }

    @Test(groups = {TestGroups.LOGIN, TestGroups.REGRESSION},
            description = "Verify Refund & Return Policy link opens the correct page")
    public void verifyRefundReturnPolicyLinkOpensCorrectPage() {
        LoginPage loginPage = openLoginPage()
                .openRefundReturnPolicyPage();

        Assert.assertTrue(loginPage.isOnRefundReturnPolicyPage(),
                "Refund & Return Policy link should open the correct page. Current URL: "
                        + loginPage.getCurrentPageUrl());
    }

    @Test(groups = {TestGroups.LOGIN, TestGroups.REGRESSION},
            description = "Verify clicking Google Login redirects to Google authentication page")
    public void verifyClickingGoogleLoginRedirectsToGoogleAuthenticationPage() {
        LoginPage loginPage = openLoginPage()
                .clickGoogleSocialLoginAndWaitForRedirect();

        Assert.assertTrue(loginPage.isOnGoogleAuthenticationPage(),
                "Google Login should redirect to Google authentication page. Current URL: "
                        + loginPage.getCurrentPageUrl());
    }

    @Test(groups = {TestGroups.LOGIN, TestGroups.REGRESSION},
            description = "Verify clicking LinkedIn Login redirects to LinkedIn authentication page")
    public void verifyClickingLinkedInLoginRedirectsToLinkedInAuthenticationPage() {
        LoginPage loginPage = openLoginPage()
                .clickLinkedInSocialLoginAndWaitForRedirect();

        Assert.assertTrue(loginPage.isOnLinkedInAuthenticationPage(),
                "LinkedIn Login should redirect to LinkedIn authentication page. Current URL: "
                        + loginPage.getCurrentPageUrl());
    }

    private LoginPage openOtpVerification() {
        return openLoginPage().requestOtp(config.validPhoneNumber());
    }

    private HomePage loginSuccessfully() {
        return new LoginPage(driver).loginWithOtp(config.validPhoneNumber(), config.validOtp());
    }

    private HomePage logoutSuccessfully() {
        HomePage homePage = loginSuccessfully()
                .clickLogout();

        Assert.assertTrue(homePage.isLogoutConfirmationDisplayed(),
                "Logout confirmation should be displayed before completing logout");

        return homePage.confirmLogout();
    }

    private void ensureLoggedOut() {
        LoginPage loginPage = new LoginPage(driver);

        if (!loginPage.isAuthenticatedAfterHeaderSettles()) {
            return;
        }

        HomePage homePage = new HomePage(driver)
                .clickLogout();

        if (homePage.isLogoutConfirmationDisplayed()) {
            homePage.confirmLogout();
        }
    }

    private void closeExtraBrowserWindows() {
        List<String> windowHandles = new ArrayList<>(driver.getWindowHandles());

        if (windowHandles.isEmpty()) {
            return;
        }

        String primaryWindow = windowHandles.get(0);
        for (int index = 1; index < windowHandles.size(); index++) {
            driver.switchTo().window(windowHandles.get(index));
            driver.close();
        }

        driver.switchTo().window(primaryWindow);
    }
}
