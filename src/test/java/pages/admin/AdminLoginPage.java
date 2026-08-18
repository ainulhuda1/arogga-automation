package pages.admin;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import pages.user.BasePage;

import java.time.Duration;

public class AdminLoginPage extends BasePage {

    private static final Duration ADMIN_LOGIN_TIMEOUT = Duration.ofSeconds(25);

    private static final By API_URL_INPUT = By.xpath(
            "//input[contains(translate(@name,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'api') "
                    + "or contains(translate(@id,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'api') "
                    + "or contains(translate(@placeholder,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'api') "
                    + "or contains(translate(@aria-label,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'api')]"
                    + "|//*[contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'api url')]/following::input[1]"
    );
    private static final By MOBILE_NUMBER_INPUT = By.xpath(
            "//input[@type='tel' "
                    + "or contains(translate(@name,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'mobile') "
                    + "or contains(translate(@name,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'phone') "
                    + "or contains(translate(@id,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'mobile') "
                    + "or contains(translate(@id,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'phone') "
                    + "or contains(translate(@placeholder,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'mobile') "
                    + "or contains(translate(@placeholder,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'phone') "
                    + "or contains(translate(@placeholder,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'number')]"
                    + "|//*[contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'mobile no') "
                    + "or contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'mobile number')]/following::input[1]"
    );
    private static final By NEXT_BUTTON = By.xpath(
            "//button[@type='submit' or contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'next') "
                    + "or contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'continue') "
                    + "or contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'send')]"
    );
    private static final By OTP_INPUT = By.xpath(
            "//input[contains(translate(@name,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'otp') "
                    + "or contains(translate(@id,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'otp') "
                    + "or contains(translate(@placeholder,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'otp') "
                    + "or @inputmode='numeric' or @autocomplete='one-time-code']"
    );
    private static final By USERNAME_INPUT = By.cssSelector(
            "input[name='email'], input[name='username'], input[type='email'], input[placeholder*='Email'], input[placeholder*='Phone']"
    );
    private static final By PASSWORD_INPUT = By.cssSelector(
            "input[name='password'], input[type='password'], input[placeholder*='Password']"
    );
    private static final By LOGIN_BUTTON = By.xpath(
            "//button[@type='submit' or contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'login') "
                    + "or contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'sign in') "
                    + "or contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'verify')]"
    );
    private static final By VALIDATION_MESSAGES = By.xpath(
            "//*[contains(@class,'error') or contains(@class,'danger') or contains(@class,'invalid') "
                    + "or contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'invalid') "
                    + "or contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'required')]"
    );

    public AdminLoginPage(WebDriver driver) {
        super(driver);
    }

    public AdminLoginPage waitUntilLoaded() {
        waitForPageLoad();
        waitUntil(ADMIN_LOGIN_TIMEOUT, webDriver -> isAdminLoginPageLoaded());
        return this;
    }

    public boolean isAdminLoginPageLoaded() {
        return (isApiUrlInputDisplayed() || isUsernameInputDisplayed() || isMobileNumberInputDisplayed())
                && (isNextButtonDisplayed() || isLoginButtonDisplayed());
    }

    public boolean isApiUrlInputDisplayed() {
        return isDisplayed(API_URL_INPUT);
    }

    public boolean isApiUrlFieldDisplayed() {
        return isApiUrlInputDisplayed();
    }

    public boolean isUsernameInputDisplayed() {
        return isDisplayed(USERNAME_INPUT);
    }

    public boolean isPasswordInputDisplayed() {
        return isDisplayed(PASSWORD_INPUT);
    }

    public boolean isMobileNumberInputDisplayed() {
        return isDisplayed(MOBILE_NUMBER_INPUT);
    }

    public boolean isMobileNumberFieldDisplayed() {
        return isMobileNumberInputDisplayed();
    }

    public boolean isOtpInputDisplayed() {
        return isDisplayed(OTP_INPUT);
    }

    public boolean isNextButtonDisplayed() {
        return isDisplayed(NEXT_BUTTON);
    }

    public boolean isLoginButtonDisplayed() {
        return isDisplayed(LOGIN_BUTTON);
    }

    public boolean isLoginButtonEnabled() {
        return isEnabled(LOGIN_BUTTON);
    }

    public AdminLoginPage enterApiUrl(String apiUrl) {
        replaceInputValue(API_URL_INPUT, apiUrl);
        return this;
    }

    public String getApiUrl() {
        String value = getAttribute(API_URL_INPUT, "value");
        return value == null ? "" : value.trim();
    }

    public AdminLoginPage enterUsername(String username) {
        type(USERNAME_INPUT, username);
        return this;
    }

    public AdminLoginPage enterMobileNumber(String mobileNumber) {
        replaceInputValue(MOBILE_NUMBER_INPUT, mobileNumber);
        return this;
    }

    public String getMobileNumber() {
        String value = getAttribute(MOBILE_NUMBER_INPUT, "value");
        return value == null ? "" : value.trim();
    }

    public AdminLoginPage enterPassword(String password) {
        type(PASSWORD_INPUT, password);
        return this;
    }

    public DashboardPage submitLogin() {
        click(LOGIN_BUTTON);
        return new DashboardPage(driver).waitUntilLoaded();
    }

    public AdminLoginPage clickNextAndWaitForOtp() {
        clickWithFallback(NEXT_BUTTON);
        try {
            waitUntil(ADMIN_LOGIN_TIMEOUT, webDriver -> isOtpInputDisplayed() && isLoginButtonDisplayed());
        } catch (TimeoutException exception) {
            throw new TimeoutException("Admin OTP field did not appear after clicking Next. "
                    + "Current API URL: '" + getApiUrl() + "', mobile number: '" + getMobileNumber() + "'.", exception);
        }
        return this;
    }

    public AdminLoginPage enterOtp(String otp) {
        type(OTP_INPUT, otp);
        return this;
    }

    public DashboardPage clickLoginAfterOtp() {
        clickWithFallback(LOGIN_BUTTON);
        return new DashboardPage(driver).waitUntilLoaded();
    }

    public AdminLoginPage submitLoginExpectingValidation() {
        click(LOGIN_BUTTON);

        try {
            waitUntil(Duration.ofSeconds(5), webDriver -> hasValidationMessage());
        } catch (TimeoutException ignored) {
            // Test assertions can still verify that the admin remained on the login page.
        }

        return this;
    }

    public DashboardPage login(String username, String password) {
        enterUsername(username);
        enterPassword(password);
        return submitLogin();
    }

    public DashboardPage loginWithOtp(String apiUrl, String mobileNumber, String otp) {
        enterApiUrl(apiUrl);
        enterMobileNumber(mobileNumber);
        clickNextAndWaitForOtp();
        enterOtp(otp);
        return clickLoginAfterOtp();
    }

    public String getValidationMessage() {
        return firstVisibleText(VALIDATION_MESSAGES);
    }

    public boolean hasValidationMessage() {
        return !getValidationMessage().isBlank();
    }

    private void replaceInputValue(By locator, String value) {
        WebElement element = waitForVisible(locator);
        executeScript("""
                const input = arguments[0];
                const value = String(arguments[1] || '');
                const descriptor = Object.getOwnPropertyDescriptor(input.constructor.prototype, 'value')
                    || Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value');

                if (descriptor && descriptor.set) {
                    descriptor.set.call(input, value);
                } else {
                    input.value = value;
                }

                input.dispatchEvent(new Event('input', { bubbles: true }));
                input.dispatchEvent(new Event('change', { bubbles: true }));
                """, element, value);
        waitUntil(Duration.ofSeconds(5), webDriver -> value.equals(getAttribute(locator, "value").trim()));
    }
}
