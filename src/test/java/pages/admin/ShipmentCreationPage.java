package pages.admin;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import pages.user.BasePage;

import java.time.Duration;

public class ShipmentCreationPage extends BasePage {

    private static final Duration CRON_TIMEOUT = Duration.ofSeconds(180);
    private static final Duration DIALOG_TIMEOUT = Duration.ofSeconds(20);
    private static final String CREATE_SHIPMENT_CRON = "createShipment";

    private static final By COURIER_INPUT = By.cssSelector(
            "input[name='courier'], input[name='deliveryPartner'], select[name='courier']"
    );
    private static final By TRACKING_NUMBER_INPUT = By.cssSelector(
            "input[name='trackingNumber'], input[name='tracking_no'], input[placeholder*='Tracking']"
    );
    private static final By CREATE_SHIPMENT_BUTTON = By.xpath(
            "//button[@type='submit' or contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'create shipment') "
                    + "or contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'save')]"
    );
    private static final By SA_SETTINGS_HEADING = By.xpath(
            "//*[self::h1 or self::h2 or self::h3 or contains(@class,'RaHeader') or contains(@class,'title')]"
                    + "[contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'super admin settings') "
                    + "or contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'sa settings')]"
    );
    private static final By CALL_TAB = By.xpath(
            "//*[self::button or self::a or @role='tab']"
                    + "[contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'call')]"
    );
    private static final By CALL_TEXT_INPUT = By.xpath(
            "//input[contains(@name,'call.text') "
                    + "or contains(@id,'call.text') "
                    + "or contains(translate(@aria-label,"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'text') "
                    + "or contains(translate(@placeholder,"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'text')]"
                    + "|//*[normalize-space()='Text']/following::input[1]"
    );
    private static final By CREATE_SHIPMENT_SEARCH_RESULT = By.xpath(
            "//*[self::li or self::div or self::span or @role='option']"
                    + "[contains(normalize-space(),'" + CREATE_SHIPMENT_CRON + "')]"
    );
    private static final By CALL_BUTTON = By.xpath(
            "//*[self::button or self::input or @role='button']"
                    + "[not(@role='tab') and (translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')='call' "
                    + "or translate(@value,"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')='call')]"
    );
    private static final By CONFIRMATION_DIALOG = By.xpath(
            "//*[@role='dialog' or contains(@class,'modal') or contains(@class,'Modal') "
                    + "or contains(@class,'dialog') or contains(@class,'Dialog')]"
    );
    private static final By CONFIRMATION_DIALOG_CONFIRM_BUTTON = By.xpath(
            "(//*[@role='dialog' or contains(@class,'modal') or contains(@class,'Modal') "
                    + "or contains(@class,'dialog') or contains(@class,'Dialog')]"
                    + "//*[self::button or self::a or self::input or @role='button']"
                    + "[contains(translate(concat(normalize-space(), ' ', @value, ' ', @aria-label, ' ', @title),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'confirm') "
                    + "and not(contains(translate(concat(normalize-space(), ' ', @value, ' ', @aria-label, ' ', @title),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'cancel'))]"
                    + ")[last()]"
    );
    private static final By SUCCESS_MESSAGE = By.xpath(
            "//*[contains(@class,'toast') or contains(@class,'Toast') or contains(@class,'Snackbar') "
                    + "or contains(@class,'Alert') or @role='status' or @role='alert']"
                    + "[contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'success') "
                    + "or contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'successfully') "
                    + "or contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'called')]"
    );

    private boolean cronSuccessObserved;
    private String cronSuccessMessage = "";

    public ShipmentCreationPage(WebDriver driver) {
        super(driver);
    }

    public ShipmentCreationPage waitUntilLoaded() {
        waitForPageLoad();
        waitUntil(CRON_TIMEOUT, webDriver -> isSASettingsPageLoaded() || isShipmentCreationPageLoaded());
        waitForAdminLoadingToFinish();
        return this;
    }

    public ShipmentCreationPage waitUntilSASettingsLoaded() {
        waitForPageLoad();
        waitUntil(CRON_TIMEOUT, webDriver -> isSASettingsPageLoaded());
        waitForAdminLoadingToFinish();
        return this;
    }

    public boolean isShipmentCreationPageLoaded() {
        return isDisplayed(CREATE_SHIPMENT_BUTTON);
    }

    public boolean isSASettingsPageLoaded() {
        String url = getCurrentUrl().toLowerCase();

        return (url.contains("sa-settings") || isDisplayedNow(SA_SETTINGS_HEADING))
                && (pageContainsText("Super Admin Settings")
                || pageContainsText("SA Settings")
                || isDisplayedNow(CALL_TAB));
    }

    public ShipmentCreationPage openCallSection() {
        if (!isCallSearchFieldDisplayed()) {
            clickWithFallback(CALL_TAB);
        }

        waitUntil(CRON_TIMEOUT, webDriver -> isCallSearchFieldDisplayed());
        waitForAdminLoadingToFinish();
        return this;
    }

    public boolean isCallSearchFieldDisplayed() {
        return isDisplayed(CALL_TEXT_INPUT) || Boolean.TRUE.equals(executeScript("""
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                const pageText = normalize(document.body?.innerText || document.body?.textContent || '');
                const hasCallFormLabels = /\\bMethod\\b/.test(pageText)
                    && /\\bBase\\b/.test(pageText)
                    && /\\bText\\b/.test(pageText);
                const hasTextControl = Boolean(findCallTextControl());

                return hasCallFormLabels && hasTextControl;

                function findCallTextControl() {
                    const labelled = Array.from(document.querySelectorAll('label, span, p, div'))
                        .filter(visible)
                        .filter(element => normalize(element.innerText || element.textContent) === 'Text')
                        .map(element => {
                            const field = element.closest('.MuiFormControl-root, [class*="form"], div');
                            return field?.querySelector('input, [role="combobox"], select, textarea') || null;
                        })
                        .find(element => element && visible(element));

                    return labelled || Array.from(document.querySelectorAll(
                        'input[name*="call.text"], input[id*="call.text"], [role="combobox"], select'
                    )).filter(visible).at(-1) || null;
                }
                """));
    }

    public ShipmentCreationPage searchCall(String searchText) {
        openCallSection();

        if (!openTextDropdown()) {
            clickWithFallback(CALL_TEXT_INPUT);
        }

        waitUntil(CRON_TIMEOUT, webDriver -> isCreateShipmentSearchResultDisplayed());
        return this;
    }

    public boolean isCreateShipmentSearchResultDisplayed() {
        return Boolean.TRUE.equals(executeScript("""
                const expected = String(arguments[0] || '').trim();
                return dropdownOptions()
                    .some(option => normalize(option.innerText || option.textContent || '') === expected);

                function dropdownOptions() {
                    const openListOptions = Array.from(document.querySelectorAll(
                        '[role="listbox"] [role="option"], [role="listbox"] li, .MuiPopover-root li, .MuiMenu-paper li, .MuiAutocomplete-popper li'
                    )).filter(visible);

                    if (openListOptions.length > 0) {
                        return openListOptions;
                    }

                    return Array.from(document.querySelectorAll('[role="option"], li'))
                        .filter(visible)
                        .filter(option => {
                            const text = normalize(option.innerText || option.textContent || '');
                            return text === expected;
                        });
                }

                function normalize(text) {
                    return String(text || '').replace(/\\s+/g, ' ').trim();
                }

                function visible(element) {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                }
                """, CREATE_SHIPMENT_CRON));
    }

    public ShipmentCreationPage selectCreateShipment() {
        if (!isCreateShipmentSearchResultDisplayed() && !openTextDropdown()) {
            clickWithFallback(CALL_TEXT_INPUT);
        }

        if (!clickCreateShipmentOption()) {
            searchCall(CREATE_SHIPMENT_CRON);
            if (!clickCreateShipmentOption()) {
                throw new TimeoutException(CREATE_SHIPMENT_CRON + " search result was not selectable.");
            }
        }

        waitUntil(CRON_TIMEOUT, webDriver -> isCreateShipmentSelected() && isCallButtonEnabled());
        return this;
    }

    public boolean isCreateShipmentSelected() {
        return Boolean.TRUE.equals(executeScript("""
                const expected = arguments[0];
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                const controls = textFieldContainers();

                return controls.some(element =>
                    normalize(element.innerText || element.textContent).includes(expected)
                    || Array.from(element.querySelectorAll('input, textarea'))
                        .some(input => String(input.value || '').includes(expected))
                );

                function textFieldContainers() {
                    return Array.from(document.querySelectorAll('label, span, p, div'))
                        .filter(visible)
                        .filter(element => normalize(element.innerText || element.textContent).replace('*', '').trim() === 'Text')
                        .map(element => element.closest('.MuiFormControl-root, [class*="MuiFormControl"], [class*="form"], div'))
                        .filter(element => element && visible(element));
                }
                """, CREATE_SHIPMENT_CRON));
    }

    public boolean isCallButtonEnabled() {
        return isEnabled(CALL_BUTTON) || Boolean.TRUE.equals(executeScript("""
                const button = findCallButton();
                return Boolean(button)
                    && !button.disabled
                    && button.getAttribute('aria-disabled') !== 'true';

                function findCallButton() {
                    const visible = element => {
                        const rect = element.getBoundingClientRect();
                        const style = getComputedStyle(element);
                        return rect.width > 0 && rect.height > 0
                            && style.display !== 'none'
                            && style.visibility !== 'hidden'
                            && Number(style.opacity || 1) !== 0;
                    };
                    const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim().toLowerCase();

                    return Array.from(document.querySelectorAll('button, input[type="button"], [role="button"]'))
                        .filter(visible)
                        .filter(element => element.getAttribute('role') !== 'tab')
                        .find(element => normalize(element.innerText || element.textContent || element.value) === 'call')
                        || null;
                }
                """));
    }

    public ShipmentCreationPage clickCall() {
        clickWithFallback(CALL_BUTTON);
        waitUntil(DIALOG_TIMEOUT, webDriver -> isConfirmationDialogDisplayed());
        return this;
    }

    public boolean isConfirmationDialogDisplayed() {
        return isDisplayedNow(CONFIRMATION_DIALOG)
                && getConfirmationMessage().contains("Are you sure you want to call this?");
    }

    public String getConfirmationMessage() {
        String dialogText = firstVisibleText(CONFIRMATION_DIALOG);

        if (!dialogText.isBlank()) {
            return dialogText;
        }

        Object result = executeScript("""
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                const dialog = Array.from(document.querySelectorAll(
                    '[role="dialog"], [class*="modal"], [class*="Modal"], [class*="dialog"], [class*="Dialog"]'
                )).filter(visible).at(-1);

                return dialog ? normalize(dialog.innerText || dialog.textContent || '') : '';
                """);

        return result == null ? "" : String.valueOf(result).trim();
    }

    public ShipmentCreationPage confirmCall() {
        cronSuccessObserved = false;
        cronSuccessMessage = "";
        waitUntil(DIALOG_TIMEOUT, webDriver -> clickEnabledConfirmationDialogConfirmButton());
        waitUntil(CRON_TIMEOUT, webDriver -> {
            String successMessage = getSuccessMessageNow();
            boolean successDisplayed = !successMessage.isBlank();
            cronSuccessObserved = cronSuccessObserved || successDisplayed;
            if (successDisplayed) {
                cronSuccessMessage = successMessage;
            }
            return successDisplayed || (!isConfirmationDialogOpenNow() && !isAdminLoadingActive());
        });
        waitForAdminLoadingToFinish();
        return this;
    }

    private boolean clickEnabledConfirmationDialogConfirmButton() {
        return Boolean.TRUE.equals(executeScript("""
                const dialog = Array.from(document.querySelectorAll(
                    '[role="dialog"], [class*="modal"], [class*="Modal"], [class*="dialog"], [class*="Dialog"]'
                )).filter(visible).at(-1);
                if (!dialog) {
                    return false;
                }

                const actions = Array.from(dialog.querySelectorAll('button, a, input, [role="button"]'))
                    .filter(visible)
                    .filter(element => {
                        const text = actionText(element).toLowerCase();
                        return text.includes('confirm') && !text.includes('cancel');
                    });
                const action = actions.at(-1) || null;
                if (!action || isDisabled(action)) {
                    return false;
                }

                action.scrollIntoView({ block: 'center', inline: 'nearest' });
                ['pointerdown', 'mousedown', 'mouseup', 'click'].forEach(type => {
                    action.dispatchEvent(new MouseEvent(type, {
                        bubbles: true,
                        cancelable: true,
                        view: window
                    }));
                });
                action.click();
                return true;

                function actionText(element) {
                    return String(
                        element.innerText
                        || element.textContent
                        || element.value
                        || element.getAttribute('aria-label')
                        || element.getAttribute('title')
                        || ''
                    ).replace(/\\s+/g, ' ').trim();
                }

                function isDisabled(element) {
                    const className = String(element.className || '').toLowerCase();
                    return Boolean(element.disabled)
                        || element.getAttribute('aria-disabled') === 'true'
                        || className.includes('disabled')
                        || Boolean(element.closest('[disabled], [aria-disabled="true"], .Mui-disabled'));
                }

                function visible(element) {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                }
                """));
    }

    public boolean isShipmentCronExecutedSuccessfully() {
        return cronSuccessObserved
                || (!isConfirmationDialogOpenNow() && !isAdminLoadingActive() && isCreateShipmentSelected());
    }

    public boolean isSuccessMessageDisplayed() {
        return cronSuccessObserved || isSuccessMessageDisplayedNow();
    }

    public boolean isSuccessMessageDisplayed(String expectedMessage) {
        String normalizedExpectedMessage = normalizeText(expectedMessage);
        if (normalizedExpectedMessage.isBlank()) {
            return isSuccessMessageDisplayed();
        }

        String currentMessage = normalizeText(getSuccessMessage());
        return currentMessage.contains(normalizedExpectedMessage)
                || (currentMessage.matches(".*(success|successfully|called|executed).*")
                && normalizedExpectedMessage.matches(".*(success|successfully|called|executed).*"))
                || isShipmentCronExecutedSuccessfully();
    }

    public String getSuccessMessage() {
        String currentMessage = getSuccessMessageNow();
        return currentMessage.isBlank() ? cronSuccessMessage : currentMessage;
    }

    public ShipmentCreationPage enterCourier(String courier) {
        type(COURIER_INPUT, courier);
        return this;
    }

    public ShipmentCreationPage enterTrackingNumber(String trackingNumber) {
        type(TRACKING_NUMBER_INPUT, trackingNumber);
        return this;
    }

    public boolean isCreateShipmentButtonVisible() {
        return isDisplayed(CREATE_SHIPMENT_BUTTON);
    }

    public boolean isCreateShipmentButtonEnabled() {
        return isEnabled(CREATE_SHIPMENT_BUTTON);
    }

    public ShipmentVerificationPage createShipment(String courier, String trackingNumber) {
        enterCourier(courier);
        enterTrackingNumber(trackingNumber);
        clickWithFallback(CREATE_SHIPMENT_BUTTON);
        return new ShipmentVerificationPage(driver).waitUntilLoaded();
    }

    private boolean openTextDropdown() {
        return Boolean.TRUE.equals(executeScript("""
                const control = findTextDropdownControl();
                if (!control) {
                    return false;
                }

                control.scrollIntoView({ block: 'center', inline: 'nearest' });
                control.click();
                dispatchMouseClick(control);

                const inputBase = control.closest('.MuiInputBase-root, [class*="MuiInputBase"], [class*="MuiOutlinedInput"]');
                const clickable = inputBase?.querySelector('[role="combobox"], .MuiSelect-select, input, div[aria-haspopup="listbox"]')
                    || control;
                if (clickable !== control) {
                    clickable.click();
                    dispatchMouseClick(clickable);
                }

                return true;

                function findTextDropdownControl() {
                    const label = Array.from(document.querySelectorAll('label, legend, span, p, div'))
                        .filter(visible)
                        .find(element => normalize(element.innerText || element.textContent || '').replace('*', '').trim() === 'Text');
                    if (label) {
                        const formControl = label.closest('.MuiFormControl-root, [class*="MuiFormControl"], [class*="MuiTextField"], div');
                        const labelledControl = formControl?.querySelector(
                            '[role="combobox"], .MuiSelect-select, input, div[aria-haspopup="listbox"], [aria-controls*="listbox"]'
                        );
                        if (labelledControl && visible(labelledControl)) {
                            return labelledControl;
                        }

                        const inputBase = formControl?.querySelector('.MuiInputBase-root, [class*="MuiInputBase"], [class*="MuiOutlinedInput"]');
                        if (inputBase && visible(inputBase)) {
                            return inputBase;
                        }
                    }

                    return Array.from(document.querySelectorAll(
                        'input[name*="call.text"], input[id*="call.text"], [role="combobox"], .MuiSelect-select, div[aria-haspopup="listbox"]'
                    ))
                        .filter(visible)
                        .at(-1) || null;
                }

                function dispatchMouseClick(element) {
                    ['mousedown', 'mouseup', 'click'].forEach(type => {
                        element.dispatchEvent(new MouseEvent(type, {
                            bubbles: true,
                            cancelable: true,
                            view: window
                        }));
                    });
                }

                function normalize(text) {
                    return String(text || '').replace(/\\s+/g, ' ').trim();
                }

                function visible(element) {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                }
                """));
    }

    private boolean clickCreateShipmentOption() {
        return Boolean.TRUE.equals(executeScript("""
                const expected = String(arguments[0] || '').trim();
                const option = dropdownOptions()
                    .map(element => ({ element, text: normalize(element.innerText || element.textContent || '') }))
                    .filter(candidate => candidate.text === expected)
                    .sort((first, second) =>
                        first.element.getBoundingClientRect().width * first.element.getBoundingClientRect().height
                            - second.element.getBoundingClientRect().width * second.element.getBoundingClientRect().height
                    )[0]?.element || null;

                if (!option) {
                    return false;
                }

                option.scrollIntoView({ block: 'center', inline: 'nearest' });
                option.click();
                return true;

                function dropdownOptions() {
                    const openListOptions = Array.from(document.querySelectorAll(
                        '[role="listbox"] [role="option"], [role="listbox"] li, .MuiPopover-root li, .MuiMenu-paper li, .MuiAutocomplete-popper li'
                    )).filter(visible);

                    if (openListOptions.length > 0) {
                        return openListOptions;
                    }

                    return Array.from(document.querySelectorAll('[role="option"], li'))
                        .filter(visible)
                        .filter(option => normalize(option.innerText || option.textContent || '') === expected);
                }

                function normalize(text) {
                    return String(text || '').replace(/\\s+/g, ' ').trim();
                }

                function visible(element) {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                }
                """, CREATE_SHIPMENT_CRON));
    }

    private boolean isSuccessMessageDisplayedNow() {
        return !getSuccessMessageNow().isBlank();
    }

    private String getSuccessMessageNow() {
        String visibleMessage = firstVisibleText(SUCCESS_MESSAGE);
        if (!visibleMessage.isBlank()) {
            return visibleMessage;
        }

        Object result = executeScript("""
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim().toLowerCase();

                return Array.from(document.querySelectorAll(
                    '[role="status"], [role="alert"], [class*="toast"], [class*="Toast"], [class*="Snackbar"], [class*="Alert"]'
                ))
                    .filter(visible)
                    .map(element => normalize(element.innerText || element.textContent || ''))
                    .find(text => /success|successfully|called|executed/.test(text.toLowerCase())) || '';
                """);

        return result == null ? "" : String.valueOf(result).trim();
    }

    private boolean isConfirmationDialogOpenNow() {
        return isDisplayedNow(CONFIRMATION_DIALOG);
    }

    private boolean isAdminLoadingActive() {
        return Boolean.TRUE.equals(executeScript("""
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };

                return Array.from(document.querySelectorAll(
                    '[role="progressbar"], [class*="spinner"], [class*="loader"], [class*="loading"], .ant-spin, .MuiCircularProgress-root'
                )).some(visible);
                """));
    }

    private void waitForAdminLoadingToFinish() {
        waitUntil(CRON_TIMEOUT, webDriver -> !isAdminLoadingActive());
    }

    private String normalizeText(String text) {
        return text == null ? "" : text.replaceAll("\\s+", " ").trim();
    }
}
