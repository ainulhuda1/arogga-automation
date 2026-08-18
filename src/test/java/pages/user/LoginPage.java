package pages.user;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoginPage extends BasePage {

    public static final String DEFAULT_TEST_OTP = "1234";
    public static final int EXPECTED_OTP_LENGTH = 4;
    private static final Duration RESEND_OTP_AVAILABLE_TIMEOUT = Duration.ofSeconds(45);
    private static final Duration EXTERNAL_NAVIGATION_TIMEOUT = Duration.ofSeconds(20);
    private static final String TERMS_AND_CONDITIONS_PATH = "/page/tos";
    private static final String PRIVACY_POLICY_PATH = "/page/privacy";
    private static final String REFUND_RETURN_POLICY_PATH = "/page/return-policy";
    private static final String GOOGLE_AUTH_URL_FRAGMENT = "accounts.google.com";
    private static final String LINKEDIN_AUTH_URL_FRAGMENT = "linkedin.com";
    private static final String OTP_REQUEST_CONFIRMATION_TEXT = "SMS sent";

    private static final String LOGIN_MODAL_XPATH =
            "//*[self::div or self::section][.//h2[normalize-space()='Login'] "
                    + "and .//input[@placeholder='Enter phone number']]";

    private static final By HEADER_LOGIN_BUTTON = By.xpath("//button[.//img[@alt='Login icon']]");
    private static final By AUTHENTICATED_USER_AVATAR =
            By.xpath("//button[.//img[contains(@class,'rounded-full') and string-length(@alt) > 0]]");

    private static final By LOGIN_MODAL = By.xpath(LOGIN_MODAL_XPATH);
    private static final By MODAL_CLOSE_BUTTON =
            By.xpath(LOGIN_MODAL_XPATH + "//button[@aria-label='close modal button']");
    private static final By PHONE_NUMBER_INPUT =
            By.xpath(LOGIN_MODAL_XPATH + "//input[@placeholder='Enter phone number']");
    private static final By SEND_BUTTON =
            By.xpath(LOGIN_MODAL_XPATH + "//button[@type='submit' and normalize-space()='Send']");
    private static final By LOGIN_BUTTON =
            By.xpath(LOGIN_MODAL_XPATH + "//button[@type='submit' and normalize-space()='Login']");
    private static final By OTP_INPUT =
            By.xpath(LOGIN_MODAL_XPATH + "//input[@placeholder='Enter OTP']");
    private static final By UPDATE_PHONE_NUMBER_BUTTON =
            By.xpath(LOGIN_MODAL_XPATH + "//button[@title='Update phone number']");
    private static final By RESEND_OTP_BUTTON =
            By.xpath(LOGIN_MODAL_XPATH + "//button[normalize-space()='Resend OTP']");
    private static final By OTP_TIMER_TEXT =
            By.xpath(LOGIN_MODAL_XPATH
                    + "//*[contains(normalize-space(),'Resent OTP in') "
                    + "or contains(normalize-space(),'Resend OTP in')]");

    private static final By REFERRAL_CODE_TOGGLE =
            By.xpath(LOGIN_MODAL_XPATH + "//button[normalize-space()='Have a referral code?']");
    private static final By REFERRAL_CODE_INPUT =
            By.xpath(LOGIN_MODAL_XPATH + "//input[@placeholder='Enter referral code']");

    private static final By TERMS_AND_CONDITIONS_LINK =
            By.xpath(LOGIN_MODAL_XPATH + "//a[normalize-space()='Terms & Conditions']");
    private static final By PRIVACY_POLICY_LINK =
            By.xpath(LOGIN_MODAL_XPATH + "//a[normalize-space()='Privacy Policy']");
    private static final By REFUND_RETURN_POLICY_LINK =
            By.xpath(LOGIN_MODAL_XPATH + "//a[normalize-space()='Refund-Return Policy']");

    private static final By GOOGLE_SOCIAL_LOGIN_BUTTON =
            By.xpath(LOGIN_MODAL_XPATH + "//button[.//img[contains(@src,'google-logo.svg')]]");
    private static final By LINKEDIN_SOCIAL_LOGIN_BUTTON =
            By.xpath(LOGIN_MODAL_XPATH + "//button[.//img[contains(@src,'linkedin-logo.svg')]]");
    private static final By GOOGLE_SOCIAL_LOGIN_ICON =
            By.xpath(LOGIN_MODAL_XPATH + "//img[contains(@src,'google-logo.svg')]");
    private static final By LINKEDIN_SOCIAL_LOGIN_ICON =
            By.xpath(LOGIN_MODAL_XPATH + "//img[contains(@src,'linkedin-logo.svg')]");

    private static final By PHONE_REQUIRED_VALIDATION =
            By.xpath(LOGIN_MODAL_XPATH + "//*[normalize-space()='Please enter phone number']");
    private static final By VALIDATION_MESSAGES = By.xpath(
            LOGIN_MODAL_XPATH
                    + "//*[normalize-space() and ("
                    + "contains(@class,'text-error') "
                    + "or contains(@class,'text-red') "
                    + "or normalize-space()='Please enter phone number' "
                    + "or contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'invalid') "
                    + "or contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'required'))]"
    );
    private static final By TOAST_MESSAGES = By.xpath(
            "//*[contains(@class,'Toastify__toast') "
                    + "or @data-sonner-toast "
                    + "or contains(translate(@class,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'toast')]"
    );

    private static final Pattern OTP_TIMER_PATTERN = Pattern.compile("Rese(?:nt|nd) OTP in \\d{2}:\\d{2}");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    public LoginPage openLoginModal() {
        if (!isDisplayedNow(LOGIN_MODAL)) {
            if (!clickLoginEntryPoint()) {
                throw new TimeoutException("Login entry point was not found in the current header.");
            }
        }
        return waitUntilLoginPageLoaded();
    }

    public LoginPage waitUntilLoginPageLoaded() {
        waitForVisible(LOGIN_MODAL);
        waitForVisible(PHONE_NUMBER_INPUT);
        waitForVisible(SEND_BUTTON);
        return this;
    }

    public boolean isLoginPageLoaded() {
        return isDisplayed(LOGIN_MODAL)
                && isDisplayed(PHONE_NUMBER_INPUT)
                && isDisplayed(SEND_BUTTON);
    }

    public boolean isPhoneNumberInputDisplayed() {
        return isDisplayed(PHONE_NUMBER_INPUT);
    }

    public boolean isSendButtonDisplayed() {
        return isDisplayed(SEND_BUTTON);
    }

    public boolean isSendButtonEnabled() {
        return isEnabled(SEND_BUTTON);
    }

    public boolean isLoginModalDisplayed() {
        return isDisplayed(LOGIN_MODAL);
    }

    public boolean isLoginModalClosed() {
        return !isDisplayedNow(LOGIN_MODAL);
    }

    public LoginPage enterPhoneNumber(String phoneNumber) {
        type(PHONE_NUMBER_INPUT, phoneNumber);
        return this;
    }

    public String getPhoneNumber() {
        return getAttribute(PHONE_NUMBER_INPUT, "value");
    }

    public boolean isPhoneNumberNumericOnly() {
        String phoneNumber = getPhoneNumber();
        return !phoneNumber.isBlank() && phoneNumber.matches("\\d+");
    }

    public LoginPage submitPhoneNumber() {
        click(SEND_BUTTON);
        return this;
    }

    public LoginPage submitEmptyPhoneNumber() {
        type(PHONE_NUMBER_INPUT, "");
        click(SEND_BUTTON);
        waitForVisible(PHONE_REQUIRED_VALIDATION);
        return this;
    }

    public LoginPage submitPhoneNumberExpectingValidation(String phoneNumber) {
        enterPhoneNumber(phoneNumber);
        click(SEND_BUTTON);

        try {
            shortWait().until(webDriver -> !getPhoneNumberValidationMessage().isBlank());
        } catch (TimeoutException ignored) {
            // Some client-side validations only mark the input; tests can still inspect the field state.
        }

        return this;
    }

    public LoginPage requestOtp(String phoneNumber) {
        enterPhoneNumber(phoneNumber);
        submitPhoneNumber();
        return waitForOtpVerificationPage();
    }

    public LoginPage waitForOtpVerificationPage() {
        waitForVisible(OTP_INPUT);
        waitForVisible(LOGIN_BUTTON);
        return this;
    }

    public boolean isOtpVerificationDisplayed() {
        return isDisplayedNow(OTP_INPUT)
                && isDisplayedNow(LOGIN_BUTTON)
                && !isEnabledNow(PHONE_NUMBER_INPUT);
    }

    public boolean isOtpInputDisplayed() {
        return isDisplayed(OTP_INPUT);
    }

    public LoginPage enterOtp(String otp) {
        type(OTP_INPUT, otp);
        return this;
    }

    public String getOtp() {
        String value = getAttribute(OTP_INPUT, "value");
        return value == null ? "" : value.trim();
    }

    public boolean isOtpNumericOnly() {
        String otp = getOtp();
        return !otp.isBlank() && otp.matches("\\d+");
    }

    public LoginPage submitInvalidOtp(String otp) {
        return submitOtpExpectingValidation(otp);
    }

    public LoginPage submitOtpExpectingValidation(String otp) {
        enterOtp(otp);
        waitForToastMessagesToClear();
        click(LOGIN_BUTTON);

        try {
            shortWait().until(webDriver -> hasOtpErrorMessage());
        } catch (TimeoutException ignored) {
            // Keep the method non-flaky when the environment accepts or suppresses invalid OTP errors.
        }

        return this;
    }

    public HomePage submitOtpExpectingSuccess() {
        waitForToastMessagesToClear();
        waitUntil(Duration.ofSeconds(15), webDriver -> getOtp().length() == EXPECTED_OTP_LENGTH
                && isEnabledNow(LOGIN_BUTTON));
        submitOtpLoginButton();

        if (!waitForAuthenticationResult(Duration.ofSeconds(15)) && canRetrySuccessfulOtpSubmit()) {
            submitOtpLoginButton();
        }

        waitUntil(Duration.ofSeconds(45), webDriver -> isAuthenticated() || hasOtpErrorMessage());
        HomePage homePage = new HomePage(driver).waitUntilLoaded();
        homePage.waitForAuthenticatedHeader();
        return homePage;
    }

    public HomePage loginWithOtp(String phoneNumber, String otp) {
        openLoginModal();
        requestOtp(phoneNumber);
        enterOtp(otp);
        return submitOtpExpectingSuccess();
    }

    public HomePage loginWithDefaultOtp(String phoneNumber) {
        return loginWithOtp(phoneNumber, DEFAULT_TEST_OTP);
    }

    public HomePage closeLoginModal() {
        click(MODAL_CLOSE_BUTTON);
        waitForInvisible(LOGIN_MODAL);
        return new HomePage(driver).waitUntilLoaded();
    }

    public String getPhoneNumberValidationMessage() {
        String requiredMessage = firstVisibleText(PHONE_REQUIRED_VALIDATION);
        if (!requiredMessage.isBlank()) {
            return requiredMessage;
        }

        return firstVisibleText(VALIDATION_MESSAGES);
    }

    public boolean hasPhoneNumberValidationMessage() {
        return !getPhoneNumberValidationMessage().isBlank() || isPhoneNumberFieldMarkedInvalid();
    }

    public boolean isPhoneNumberFieldMarkedInvalid() {
        String classValue = getAttribute(PHONE_NUMBER_INPUT, "class");
        return classValue != null && classValue.contains("border-error");
    }

    public String getOtpErrorMessage() {
        return visibleFeedbackMessages()
                .stream()
                .filter(this::isOtpErrorMessage)
                .findFirst()
                .orElse("");
    }

    public boolean hasOtpErrorMessage() {
        return !getOtpErrorMessage().isBlank();
    }

    public boolean hasOtpErrorMessageContaining(String expectedText) {
        String normalizedExpectedText = normalizeMessage(expectedText);
        return !normalizedExpectedText.isBlank()
                && normalizeMessage(getOtpErrorMessage()).contains(normalizedExpectedText);
    }

    public boolean hasOtpRequestConfirmationMessage() {
        return visibleFeedbackMessages()
                .stream()
                .map(this::normalizeMessage)
                .anyMatch(message -> message.contains(normalizeMessage(OTP_REQUEST_CONFIRMATION_TEXT)));
    }

    public boolean isTermsAndConditionsLinkDisplayed() {
        return isDisplayed(TERMS_AND_CONDITIONS_LINK);
    }

    public boolean isPrivacyPolicyLinkDisplayed() {
        return isDisplayed(PRIVACY_POLICY_LINK);
    }

    public boolean isRefundReturnPolicyLinkDisplayed() {
        return isDisplayed(REFUND_RETURN_POLICY_LINK);
    }

    public String getTermsAndConditionsHref() {
        return getAttribute(TERMS_AND_CONDITIONS_LINK, "href");
    }

    public String getPrivacyPolicyHref() {
        return getAttribute(PRIVACY_POLICY_LINK, "href");
    }

    public String getRefundReturnPolicyHref() {
        return getAttribute(REFUND_RETURN_POLICY_LINK, "href");
    }

    public LoginPage openTermsAndConditionsPage() {
        return clickLinkAndWaitForUrl(TERMS_AND_CONDITIONS_LINK, TERMS_AND_CONDITIONS_PATH);
    }

    public LoginPage openPrivacyPolicyPage() {
        return clickLinkAndWaitForUrl(PRIVACY_POLICY_LINK, PRIVACY_POLICY_PATH);
    }

    public LoginPage openRefundReturnPolicyPage() {
        return clickLinkAndWaitForUrl(REFUND_RETURN_POLICY_LINK, REFUND_RETURN_POLICY_PATH);
    }

    public boolean isOnTermsAndConditionsPage() {
        return getCurrentUrl().contains(TERMS_AND_CONDITIONS_PATH);
    }

    public boolean isOnPrivacyPolicyPage() {
        return getCurrentUrl().contains(PRIVACY_POLICY_PATH);
    }

    public boolean isOnRefundReturnPolicyPage() {
        return getCurrentUrl().contains(REFUND_RETURN_POLICY_PATH);
    }

    public String getCurrentPageUrl() {
        return getCurrentUrl();
    }

    public boolean isGoogleSocialLoginButtonDisplayed() {
        return isDisplayed(GOOGLE_SOCIAL_LOGIN_BUTTON);
    }

    public boolean isLinkedInSocialLoginButtonDisplayed() {
        return isDisplayed(LINKEDIN_SOCIAL_LOGIN_BUTTON);
    }

    public boolean isGoogleSocialLoginIconLoaded() {
        return isImageLoaded(GOOGLE_SOCIAL_LOGIN_ICON);
    }

    public boolean isLinkedInSocialLoginIconLoaded() {
        return isImageLoaded(LINKEDIN_SOCIAL_LOGIN_ICON);
    }

    public LoginPage clickGoogleSocialLogin() {
        click(GOOGLE_SOCIAL_LOGIN_BUTTON);
        return this;
    }

    public LoginPage clickLinkedInSocialLogin() {
        click(LINKEDIN_SOCIAL_LOGIN_BUTTON);
        return this;
    }

    public LoginPage clickGoogleSocialLoginAndWaitForRedirect() {
        return clickAndWaitForNavigation(GOOGLE_SOCIAL_LOGIN_BUTTON, GOOGLE_AUTH_URL_FRAGMENT);
    }

    public LoginPage clickLinkedInSocialLoginAndWaitForRedirect() {
        return clickAndWaitForNavigation(LINKEDIN_SOCIAL_LOGIN_BUTTON, LINKEDIN_AUTH_URL_FRAGMENT);
    }

    public boolean isOnGoogleAuthenticationPage() {
        return getCurrentUrl().contains(GOOGLE_AUTH_URL_FRAGMENT);
    }

    public boolean isOnLinkedInAuthenticationPage() {
        return getCurrentUrl().contains(LINKEDIN_AUTH_URL_FRAGMENT);
    }

    public LoginPage toggleReferralCodeAccordion() {
        click(REFERRAL_CODE_TOGGLE);
        return this;
    }

    public boolean isReferralCodeAccordionExpanded() {
        return isDisplayedNow(REFERRAL_CODE_INPUT);
    }

    public LoginPage enterReferralCode(String referralCode) {
        if (!isReferralCodeAccordionExpanded()) {
            toggleReferralCodeAccordion();
        }

        type(REFERRAL_CODE_INPUT, referralCode);
        return this;
    }

    public LoginPage clickUpdatePhoneNumber() {
        click(UPDATE_PHONE_NUMBER_BUTTON);
        waitUntil(webDriver -> isDisplayedNow(PHONE_NUMBER_INPUT)
                && isEnabledNow(PHONE_NUMBER_INPUT)
                && !isDisplayedNow(OTP_INPUT));
        return this;
    }

    public boolean isPhoneNumberEditable() {
        return isEnabled(PHONE_NUMBER_INPUT);
    }

    public boolean isOtpTimerDisplayed() {
        return !getOtpTimerText().isBlank();
    }

    public String getOtpTimerText() {
        return findAll(OTP_TIMER_TEXT)
                .stream()
                .filter(this::isSafelyDisplayed)
                .map(this::safeText)
                .map(OTP_TIMER_PATTERN::matcher)
                .filter(Matcher::find)
                .map(Matcher::group)
                .findFirst()
                .orElse("");
    }

    public boolean isResendOtpButtonDisplayed() {
        return isDisplayed(RESEND_OTP_BUTTON);
    }

    public LoginPage waitUntilResendOtpAvailable() {
        waitUntil(RESEND_OTP_AVAILABLE_TIMEOUT, webDriver ->
                isDisplayedNow(RESEND_OTP_BUTTON) && isEnabledNow(RESEND_OTP_BUTTON));
        return this;
    }

    public LoginPage clickResendOtp() {
        waitUntilResendOtpAvailable();
        waitForToastMessagesToClear();
        click(RESEND_OTP_BUTTON);
        waitUntil(webDriver -> hasOtpRequestConfirmationMessage()
                || isOtpTimerDisplayed()
                || !isDisplayedNow(RESEND_OTP_BUTTON));
        return this;
    }

    public LoginPage waitUntilOtpTimerStarts() {
        waitUntil(webDriver -> isOtpTimerDisplayed());
        return this;
    }

    public LoginPage waitForOtpExpiry(Duration expiryTimeout) {
        waitUntil(expiryTimeout, webDriver -> isResendOtpButtonDisplayed() || hasOtpErrorMessage());
        return this;
    }

    public boolean isAuthenticated() {
        return isDisplayedNow(AUTHENTICATED_USER_AVATAR) || isAuthenticatedAccountHeaderDisplayed();
    }

    public boolean isAuthenticatedAfterHeaderSettles() {
        try {
            shortWait().until(webDriver -> isAuthenticated()
                    || isDisplayedNow(HEADER_LOGIN_BUTTON));
        } catch (TimeoutException ignored) {
            // The immediate state below is the final source of truth for callers.
        }

        return isAuthenticated();
    }

    private LoginPage clickLinkAndWaitForUrl(By linkLocator, String expectedUrlFragment) {
        Set<String> handlesBeforeClick = driver.getWindowHandles();
        click(linkLocator);

        waitUntil(EXTERNAL_NAVIGATION_TIMEOUT, webDriver -> {
            switchToNewWindowIfOpened(handlesBeforeClick);
            return getCurrentUrl().contains(expectedUrlFragment);
        });
        waitForPageLoad();

        return this;
    }

    private LoginPage clickAndWaitForNavigation(By locator, String expectedUrlFragment) {
        Set<String> handlesBeforeClick = driver.getWindowHandles();
        String urlBeforeClick = getCurrentUrl();
        click(locator);

        waitUntil(EXTERNAL_NAVIGATION_TIMEOUT, webDriver -> {
            switchToNewWindowIfOpened(handlesBeforeClick);
            String currentUrl = getCurrentUrl();
            return !currentUrl.equals(urlBeforeClick) && currentUrl.contains(expectedUrlFragment);
        });

        return this;
    }

    private void switchToNewWindowIfOpened(Set<String> handlesBeforeClick) {
        for (String windowHandle : driver.getWindowHandles()) {
            if (!handlesBeforeClick.contains(windowHandle)) {
                driver.switchTo().window(windowHandle);
                return;
            }
        }
    }

    private List<String> visibleFeedbackMessages() {
        List<String> messages = new ArrayList<>();
        messages.addAll(visibleTexts(TOAST_MESSAGES));
        messages.addAll(visibleTexts(VALIDATION_MESSAGES));
        return messages.stream().distinct().toList();
    }

    private List<String> visibleTexts(By locator) {
        return findAll(locator)
                .stream()
                .filter(this::isSafelyDisplayed)
                .map(this::safeText)
                .filter(this::isMeaningfulFeedbackMessage)
                .toList();
    }

    private boolean isOtpErrorMessage(String message) {
        String normalizedMessage = normalizeMessage(message);
        return normalizedMessage.contains("otp")
                || normalizedMessage.contains("code")
                || normalizedMessage.contains("invalid")
                || normalizedMessage.contains("incorrect")
                || normalizedMessage.contains("expired")
                || normalizedMessage.contains("error verifying");
    }

    private boolean isMeaningfulFeedbackMessage(String message) {
        String normalizedMessage = normalizeMessage(message);
        return !normalizedMessage.isBlank() && !normalizedMessage.matches("\\d+");
    }

    private String normalizeMessage(String message) {
        return message == null ? "" : message.replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
    }

    private void submitOtpLoginButton() {
        waitForToastMessagesToClear();
        clickWithFallback(LOGIN_BUTTON);
    }

    private boolean isAuthenticatedAccountHeaderDisplayed() {
        return Boolean.TRUE.equals(executeScript("""
                const visible = element => {
                    if (!element) {
                        return false;
                    }

                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                const describe = element => [
                    normalize(element.innerText || element.textContent || ''),
                    element.getAttribute('aria-label'),
                    element.getAttribute('title'),
                    element.getAttribute('href'),
                    element.getAttribute('alt'),
                    element.getAttribute('src'),
                    element.getAttribute('class')
                ].filter(Boolean).join(' ');
                const header = document.querySelector('header');
                if (!visible(header)) {
                    return false;
                }

                return Array.from(header.querySelectorAll('button, a'))
                    .filter(visible)
                    .some(element => {
                        const text = normalize(element.innerText || element.textContent || '');
                        const descriptor = describe(element);
                        const hasAccountLabel = /\\baccount\\b/i.test(text + ' ' + descriptor);
                        const hasLoginLabel = /\\b(login|sign\\s*in)\\b/i.test(text + ' ' + descriptor)
                            || /login\\s*icon/i.test(descriptor);
                        if (!hasAccountLabel || hasLoginLabel) {
                            return false;
                        }

                        const hasProfileImage = Array.from(element.querySelectorAll('img'))
                            .filter(visible)
                            .some(image => {
                                const imageDescriptor = describe(image);
                                const imageAlt = normalize(image.getAttribute('alt') || '');
                                return /rounded-full|profile|avatar/i.test(imageDescriptor)
                                    || Boolean(imageAlt)
                                        && !/login\\s*icon|location|cart|order|inbox|search|icon/i.test(imageAlt);
                            });

                        return hasProfileImage || /৳\\s*\\d+|wallet|balance/i.test(text);
                    });
                """));
    }

    private boolean clickLoginEntryPoint() {
        try {
            if (isDisplayedNow(HEADER_LOGIN_BUTTON)) {
                clickWithFallback(HEADER_LOGIN_BUTTON);
                return true;
            }
        } catch (RuntimeException ignored) {
            // The JavaScript fallback below handles the updated header variants.
        }

        return Boolean.TRUE.equals(executeScript("""
                const visible = element => {
                    if (!element) {
                        return false;
                    }

                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && style.pointerEvents !== 'none'
                        && Number(style.opacity || 1) !== 0;
                };
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                const describe = element => [
                    normalize(element.innerText || element.textContent || ''),
                    element.getAttribute('aria-label'),
                    element.getAttribute('title'),
                    element.getAttribute('href'),
                    element.getAttribute('alt'),
                    element.getAttribute('src'),
                    element.getAttribute('class'),
                    Array.from(element.querySelectorAll('img, svg'))
                        .map(icon => [
                            icon.getAttribute('alt'),
                            icon.getAttribute('src'),
                            icon.getAttribute('class'),
                            icon.getAttribute('aria-label'),
                            icon.getAttribute('title')
                        ].filter(Boolean).join(' '))
                        .join(' ')
                ].filter(Boolean).join(' ');
                const header = document.querySelector('header') || document;
                const entryPoint = Array.from(header.querySelectorAll('button, a'))
                    .filter(visible)
                    .map(element => {
                        const descriptor = describe(element);
                        const text = normalize(element.innerText || element.textContent || '');
                        let score = 0;
                        const loginSignal = /\\b(login|sign\\s*in)\\b/i.test(text)
                            || /login\\s*icon/i.test(descriptor);
                        const accountWithoutLogin = /\\baccount\\b/i.test(text + ' ' + descriptor)
                            && !loginSignal;
                        if (loginSignal) {
                            score += 100;
                        }
                        if (accountWithoutLogin) {
                            score -= 100;
                        }
                        if (/cart|orders?|inbox|search|delivery|place\\s+order\\s+by|flash\\s+sale|category|scroll/i
                                .test(descriptor)) {
                            score -= 60;
                        }
                        return { element, score };
                    })
                    .filter(candidate => candidate.score > 0)
                    .sort((first, second) => second.score - first.score)
                    .map(candidate => candidate.element)[0] || null;

                if (!entryPoint) {
                    return false;
                }

                entryPoint.scrollIntoView({ block: 'center', inline: 'nearest' });
                entryPoint.click();
                return true;
                """));
    }

    private boolean waitForAuthenticationResult(Duration timeout) {
        try {
            waitUntil(timeout, webDriver -> isAuthenticated() || hasOtpErrorMessage());
            return isAuthenticated();
        } catch (TimeoutException ignored) {
            return false;
        }
    }

    private boolean canRetrySuccessfulOtpSubmit() {
        return isDisplayedNow(LOGIN_MODAL)
                && getOtp().length() == EXPECTED_OTP_LENGTH
                && isEnabledNow(LOGIN_BUTTON)
                && !hasOtpErrorMessage();
    }

    private void waitForToastMessagesToClear() {
        try {
            waitForInvisible(TOAST_MESSAGES);
        } catch (TimeoutException ignored) {
            // Continue with the click; a persistent overlay will surface as an intercepted click or submit failure.
        }
    }
}
