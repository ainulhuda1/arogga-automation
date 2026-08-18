package tests.user;

import base.BaseTest;
import constants.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;
import pages.user.LoginPage;

import java.time.Duration;

public class OtpTest extends BaseTest {

    @Test(groups = {TestGroups.OTP, TestGroups.REGRESSION},
            description = "Verify clicking Send opens OTP page")
    public void verifyClickingSendOpensOtpPage() {
        LoginPage loginPage = openLoginPage().requestOtp(config.validPhoneNumber());

        Assert.assertTrue(loginPage.isOtpVerificationDisplayed(), "OTP verification page should be displayed");
    }

    @Test(groups = {TestGroups.OTP, TestGroups.REGRESSION},
            description = "Verify OTP page loads")
    public void verifyOtpPageLoads() {
        LoginPage loginPage = openLoginPage()
                .requestOtp(config.validPhoneNumber())
                .waitForOtpVerificationPage();

        Assert.assertTrue(loginPage.isOtpVerificationDisplayed(), "OTP page should load after valid phone submission");
    }

    @Test(groups = {TestGroups.OTP, TestGroups.REGRESSION},
            description = "Verify invalid OTP error")
    public void verifyInvalidOtpError() {
        LoginPage loginPage = openLoginPage()
                .requestOtp(config.validPhoneNumber())
                .submitInvalidOtp(config.invalidOtp());

        Assert.assertTrue(loginPage.hasOtpErrorMessage() || loginPage.isOtpVerificationDisplayed(),
                "Invalid OTP should show an error or keep the user on OTP verification");
        Assert.assertFalse(loginPage.isAuthenticated(), "Invalid OTP must not authenticate the user");
    }

    /*@Test(groups = {TestGroups.OTP, TestGroups.REGRESSION},
            description = "Verify Resend OTP functionality")
    public void verifyResendOtpFunctionality() {
        LoginPage loginPage = openLoginPage()
                .requestOtp(config.validPhoneNumber())
                .waitUntilResendOtpAvailable()
                .clickResendOtp();

        Assert.assertTrue(loginPage.isOtpTimerDisplayed() || loginPage.isOtpVerificationDisplayed(),
                "Clicking Resend OTP should restart the timer or keep OTP verification visible");
    }

    @Test(groups = {TestGroups.OTP, TestGroups.REGRESSION},
            description = "Verify Resend OTP timer")
    public void verifyResendOtpTimer() {
        LoginPage loginPage = openLoginPage()
                .requestOtp(config.validPhoneNumber())
                .waitUntilOtpTimerStarts();

        Assert.assertFalse(loginPage.getOtpTimerText().isBlank(), "OTP resend timer should be displayed");
    } */

    @Test(groups = {TestGroups.OTP, TestGroups.REGRESSION},
            description = "Verify OTP expiry")
    public void verifyOtpExpiry() {
        skipIfOtpExpiryIsNotSupported();

        LoginPage loginPage = openLoginPage()
                .requestOtp(config.validPhoneNumber())
                .waitForOtpExpiry(Duration.ofSeconds(config.otpExpiryTimeoutSeconds()))
                .enterOtp(config.validOtp())
                .submitInvalidOtp(config.validOtp());

        Assert.assertTrue(loginPage.hasOtpErrorMessage() || loginPage.isOtpVerificationDisplayed(),
                "Expired OTP should not authenticate the user");
    }

    @Test(groups = {TestGroups.OTP, TestGroups.REGRESSION},
            description = "Verify Update Phone Number")
    public void verifyUpdatePhoneNumber() {
        LoginPage loginPage = openLoginPage()
                .requestOtp(config.validPhoneNumber())
                .clickUpdatePhoneNumber();

        Assert.assertTrue(loginPage.isPhoneNumberEditable(), "Phone number field should become editable");
        Assert.assertFalse(loginPage.isOtpVerificationDisplayed(), "OTP field should be hidden after update phone action");
    }

    @Test(groups = {TestGroups.OTP, TestGroups.REGRESSION},
            description = "Verify Referral Code accordion")
    public void verifyReferralCodeAccordion() {
        LoginPage loginPage = openLoginPage().toggleReferralCodeAccordion();

        Assert.assertTrue(loginPage.isReferralCodeAccordionExpanded(),
                "Referral code accordion should expand and show the referral input");
    }
}
