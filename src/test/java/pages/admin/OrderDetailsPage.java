package pages.admin;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import pages.user.BasePage;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class OrderDetailsPage extends BasePage {

    private static final Duration ORDER_DETAILS_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration DIALOG_APPEAR_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration DIALOG_CLOSE_TIMEOUT = Duration.ofSeconds(10);
    private static final String UPPERCASE_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE_LETTERS = "abcdefghijklmnopqrstuvwxyz";
    private static final String CONFIRMATION_DIALOG_XPATH =
            "//*[@role='dialog' or contains(@class,'modal') or contains(@class,'Modal') "
                    + "or contains(@class,'dialog') or contains(@class,'Dialog')]";
    private static final String ACTION_TEXT_XPATH =
            "translate(concat(normalize-space(), ' ', @value, ' ', @aria-label, ' ', @title),"
                    + "'" + UPPERCASE_LETTERS + "','" + LOWERCASE_LETTERS + "')";

    private static final By ORDER_STATUS = By.cssSelector(
            "[data-testid='order-status'], [class*='status'], [class*='badge'], [class*='Status'], [class*='Badge']"
    );
    private static final By LOCATION_SECTION = By.xpath(
            "//*[self::section or self::div or self::article]"
                    + "[.//*[contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'location')] "
                    + "or contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'customer location') "
                    + "or contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'shipping address')]"
    );
    private static final By CONFIRM_LOCATION_BUTTON = By.xpath(
            "//*[self::button or self::a or self::input or @role='button' or contains(@class,'btn') or contains(@class,'Button')]"
                    + "[contains(translate(concat(normalize-space(), ' ', @value, ' ', @aria-label, ' ', @title),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'confirm location')]"
    );
    private static final By CONFIRM_ORDER_BUTTON = By.xpath(
            "//*[self::button or self::a or self::input or @role='button' or contains(@class,'btn') or contains(@class,'Button')]"
                    + "[contains(translate(concat(normalize-space(), ' ', @value, ' ', @aria-label, ' ', @title),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'confirm order') "
                    + "or translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')='confirm']"
    );
    private static final By SUCCESS_MESSAGE = By.xpath(
            "//*[contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'success') "
                    + "or contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'confirmed')]"
    );
    private static final By LOAD_SHIPMENT_BUTTON = By.xpath(
            "//*[self::button or self::a or @role='button']"
                    + "[contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'load shipment')]"
    );
    private static final By SHIPMENT_SECTION = By.xpath(
            "//*[self::section or self::div or self::article]"
                    + "[.//*[contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'load shipment')] "
                    + "or .//*[contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'shipment status')] "
                    + "or contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'load shipment')]"
    );
    private static final By CONFIRMATION_DIALOG = By.xpath(CONFIRMATION_DIALOG_XPATH);
    private static final By CONFIRMATION_DIALOG_CONFIRM_BUTTON = By.xpath(
            "(" + CONFIRMATION_DIALOG_XPATH
                    + "//*[self::button or self::a or self::input or @role='button']"
                    + "[contains(" + ACTION_TEXT_XPATH + ",'confirm') "
                    + "and not(contains(" + ACTION_TEXT_XPATH + ",'cancel'))]"
                    + ")[last()]"
    );

    private String currentOrderId = "";

    public OrderDetailsPage(WebDriver driver) {
        super(driver);
    }

    public OrderDetailsPage waitUntilLoaded(String orderId) {
        currentOrderId = orderId == null ? "" : orderId.trim();
        waitForPageLoad();
        waitUntil(ORDER_DETAILS_TIMEOUT, webDriver -> isOrderDetailsPageLoaded(orderId));
        waitForAdminLoadingToFinish();
        return this;
    }

    public boolean isOrderDetailsPageLoaded(String orderId) {
        return orderId != null
                && !orderId.isBlank()
                && pageContainsText(orderId)
                && (pageContainsText("Order") || !getOrderStatus().isBlank());
    }

    public String getOrderStatus() {
        String status = firstVisibleText(ORDER_STATUS);
        if (!status.isBlank()) {
            return status;
        }

        Object result = executeScript("""
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                const text = normalize(document.body ? document.body.innerText || document.body.textContent || '' : '');
                const match = text.match(/Order\\s+Status\\s*[:\\-]?\\s*([A-Za-z ]+)/i)
                    || text.match(/Status\\s*[:\\-]?\\s*([A-Za-z ]+)/i);
                return match && match[1] ? match[1].trim() : '';
                """);

        return result == null ? "" : String.valueOf(result).trim();
    }

    public OrderDetailsPage scrollToShipmentSection() {
        openDetailsTab("Shipment");

        if (isDisplayedNow(LOAD_SHIPMENT_BUTTON)) {
            scrollIntoView(LOAD_SHIPMENT_BUTTON);
            return this;
        }

        if (isDisplayedNow(SHIPMENT_SECTION)) {
            scrollIntoView(SHIPMENT_SECTION);
            return this;
        }

        executeScript("""
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                const section = Array.from(document.querySelectorAll('section, article, div'))
                    .filter(visible)
                    .filter(element => /load shipment|shipment status|shipment/i.test(
                        normalize(element.innerText || element.textContent || '')
                    ))
                    .sort((first, second) =>
                        (first.getBoundingClientRect().width * first.getBoundingClientRect().height)
                            - (second.getBoundingClientRect().width * second.getBoundingClientRect().height)
                    )[0];

                if (section) {
                    section.scrollIntoView({ block: 'center', inline: 'nearest' });
                }

                function visible(element) {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                }
                """);

        return this;
    }

    public boolean isLoadShipmentButtonDisplayed() {
        scrollToShipmentSection();
        return isDisplayedNow(LOAD_SHIPMENT_BUTTON)
                || Boolean.TRUE.equals(executeScript("""
                return Boolean(findLoadShipmentAction());

                function findLoadShipmentAction() {
                    const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim().toLowerCase();
                    return Array.from(document.querySelectorAll('button, a, [role="button"]'))
                        .filter(visible)
                        .find(element => normalize(element.innerText || element.textContent || '').includes('load shipment'))
                        || null;
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

    public List<String> getOrderedProductRowTexts() {
        Object result = executeScript("""
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                const excluded = /shipment id|shipment status|load shipment|timeline/i;
                const productTable = /product|medicine|item/i;
                const quantityTable = /\\bqty\\b|quantity/i;
                const rows = [];

                Array.from(document.querySelectorAll('table'))
                    .filter(visible)
                    .map(table => ({ table, text: normalize(table.innerText || table.textContent || '') }))
                    .filter(candidate => productTable.test(candidate.text)
                        && quantityTable.test(candidate.text)
                        && !excluded.test(candidate.text))
                    .forEach(candidate => rows.push(...tableRows(candidate.table)));

                if (rows.length === 0) {
                    Array.from(document.querySelectorAll('section, article, div'))
                        .filter(visible)
                        .filter(element => /ordered product|order item|product item|products/i.test(
                            normalize(element.innerText || element.textContent || '')
                        ))
                        .forEach(section => {
                            Array.from(section.querySelectorAll('table'))
                                .filter(visible)
                                .forEach(table => rows.push(...tableRows(table)));
                        });
                }

                return unique(rows).slice(0, 30);

                function tableRows(table) {
                    const headers = Array.from(table.querySelectorAll('thead th, [role="columnheader"]'))
                        .map(header => normalize(header.innerText || header.textContent || ''))
                        .filter(Boolean);

                    return Array.from(table.querySelectorAll('tbody tr, [role="row"]'))
                        .filter(visible)
                        .map(row => {
                            const cells = Array.from(row.querySelectorAll('td, [role="cell"]'))
                                .filter(visible)
                                .map(cell => normalize(cell.innerText || cell.textContent || ''))
                                .filter(Boolean);

                            if (headers.length > 0 && cells.length > 0) {
                                return cells
                                    .map((cell, index) => `${headers[index] || `Column ${index + 1}`}: ${cell}`)
                                    .join(' | ');
                            }

                            return normalize(row.innerText || row.textContent || '');
                        })
                        .filter(text => text
                            && productTable.test(text)
                            && quantityTable.test(text)
                            && !/^product\\s+.*qty/i.test(text));
                }

                function unique(values) {
                    return Array.from(new Set(values.map(normalize).filter(Boolean)));
                }

                function visible(element) {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                }
                """);

        return toStringList(result);
    }

    public OrderDetailsPage waitUntilOrderedProductsDisplayed() {
        openDetailsTab("Product");
        waitUntil(ORDER_DETAILS_TIMEOUT, webDriver -> !getOrderedProductRowTexts().isEmpty());
        return this;
    }

    public ShipmentVerificationPage openShipmentVerificationSection(String orderId) {
        scrollToShipmentSection();
        return new ShipmentVerificationPage(driver).waitUntilLoaded(orderId);
    }

    public OrderDetailsPage scrollToLocationSection() {
        openDetailsTab("User");

        if (isDisplayedNow(LOCATION_SECTION)) {
            scrollIntoView(LOCATION_SECTION);
            return this;
        }

        executeScript("""
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                const section = Array.from(document.querySelectorAll('section, article, div'))
                    .filter(element => /location|shipping address|customer address/i.test(
                        normalize(element.innerText || element.textContent || '')
                    ))
                    .sort((first, second) =>
                        (first.getBoundingClientRect().width * first.getBoundingClientRect().height)
                            - (second.getBoundingClientRect().width * second.getBoundingClientRect().height)
                    )[0];
                if (section) {
                    section.scrollIntoView({ block: 'center', inline: 'nearest' });
                }
                """);

        return this;
    }

    public boolean isCustomerLocationDisplayed() {
        scrollToLocationSection();
        String locationText = firstVisibleText(LOCATION_SECTION);
        if (!locationText.isBlank()) {
            return locationText.matches("(?is).*(location|address).*")
                    && locationText.replaceAll("\\s+", " ").trim().length() > 20;
        }

        return pageContainsText("Location") || pageContainsText("Address");
    }

    public String getShippingAddressText() {
        scrollToLocationSection();
        String locationText = firstVisibleText(LOCATION_SECTION);
        if (!locationText.isBlank()) {
            return locationText.replaceAll("\\s+", " ").trim();
        }

        Object result = executeScript("""
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                const section = Array.from(document.querySelectorAll('section, article, div'))
                    .filter(visible)
                    .map(element => ({ element, text: normalize(element.innerText || element.textContent || '') }))
                    .filter(candidate => /location|shipping address|customer address|address/i.test(candidate.text))
                    .sort((first, second) =>
                        (first.element.getBoundingClientRect().width * first.element.getBoundingClientRect().height)
                            - (second.element.getBoundingClientRect().width * second.element.getBoundingClientRect().height)
                    )[0];

                return section ? section.text : '';

                function visible(element) {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                }
                """);

        return result == null ? "" : String.valueOf(result).replaceAll("\\s+", " ").trim();
    }

    public boolean isConfirmLocationButtonVisible() {
        scrollToLocationSection();
        return isDisplayed(CONFIRM_LOCATION_BUTTON)
                || isVisibleActionPresent("Confirm Location");
    }

    public OrderDetailsPage waitForLocationActionState() {
        waitUntil(ORDER_DETAILS_TIMEOUT, webDriver -> isLocationConfirmed() || isConfirmLocationButtonVisible());
        return this;
    }

    public OrderDetailsPage confirmLocation() {
        scrollToLocationSection();
        if (isDisplayedNow(CONFIRM_LOCATION_BUTTON)) {
            clickWithFallback(CONFIRM_LOCATION_BUTTON);
        } else {
            clickVisibleAction("Confirm Location");
        }
        confirmDialogIfPresent();

        waitUntil(ORDER_DETAILS_TIMEOUT, webDriver -> isLocationConfirmed());

        return this;
    }

    public boolean isLocationConfirmed() {
        String text = getPageText();
        return PatternMatcher.matches(text, "(?is)(location|address).*confirmed|confirmed.*(location|address)")
                || isDisplayedNow(CONFIRM_ORDER_BUTTON)
                || isVisibleActionPresent("Confirm Order")
                || isDisplayedNow(SUCCESS_MESSAGE);
    }

    public boolean isConfirmOrderButtonVisible() {
        return isDisplayed(CONFIRM_ORDER_BUTTON)
                || isVisibleActionPresent("Confirm Order");
    }

    public OrderDetailsPage waitForOrderActionState() {
        if (!isOrderStatusConfirmed() && !isConfirmOrderButtonVisible()) {
            openDetailsTab("Order");
        }

        waitUntil(ORDER_DETAILS_TIMEOUT, webDriver -> isOrderStatusConfirmed() || isConfirmOrderButtonVisible());
        return this;
    }

    public OrderDetailsPage confirmOrder() {
        if (!isDisplayedNow(CONFIRM_ORDER_BUTTON) && !isVisibleActionPresent("Confirm Order")) {
            openDetailsTab("Order");
        }

        if (isDisplayedNow(CONFIRM_ORDER_BUTTON)) {
            clickWithFallback(CONFIRM_ORDER_BUTTON);
        } else if (isVisibleActionPresent("Confirm Order")) {
            clickVisibleAction("Confirm Order");
        } else {
            clickVisibleAction("Confirm");
        }
        confirmDialogIfPresent();

        waitUntil(ORDER_DETAILS_TIMEOUT, webDriver -> isOrderStatusConfirmed());

        return this;
    }

    public boolean isOrderStatusConfirmed() {
        return getOrderStatus().matches("(?is).*confirmed.*")
                || PatternMatcher.matches(getPageText(), "(?is)order\\s+(status\\s*)?confirmed|confirmed\\s+success")
                || isCurrentOrderConfirmedInList();
    }

    private boolean isCurrentOrderConfirmedInList() {
        if (currentOrderId == null || currentOrderId.isBlank()) {
            return false;
        }

        return Boolean.TRUE.equals(executeScript("""
                const orderId = String(arguments[0] || '').trim();
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                return Array.from(document.querySelectorAll(
                    'tr, [role="row"], [data-testid*="row"], [class*="row"], [class*="Row"]'
                ))
                    .filter(visible)
                    .some(element => {
                        const text = normalize(element.innerText || element.textContent || '');
                        return text.includes(orderId) && /\\bConfirmed\\b/i.test(text);
                    });
                """, currentOrderId));
    }

    private boolean openDetailsTab(String tabName) {
        if (tabName == null || tabName.isBlank()) {
            return false;
        }

        Boolean handled = (Boolean) executeScript("""
                const expected = String(arguments[0] || '').replace(/\\s+/g, ' ').trim().toLowerCase();
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                const tabText = element => normalize(
                    element.innerText
                    || element.textContent
                    || element.getAttribute('aria-label')
                    || element.getAttribute('title')
                    || ''
                );
                const tabCandidates = Array.from(document.querySelectorAll('[role="tab"], [role="button"], button, a, span, div'))
                    .filter(visible)
                    .map(element => {
                        const tab = element.closest('[role="tab"], [role="button"], button, a')
                            || clickableTabAncestor(element)
                            || element;
                        return { tab, text: tabText(element).toLowerCase() };
                    })
                    .filter(candidate => candidate.text === expected)
                    .filter((candidate, index, values) => values.findIndex(value => value.tab === candidate.tab) === index)
                    .map(candidate => ({ ...candidate, rect: candidate.tab.getBoundingClientRect() }))
                    .sort((first, second) =>
                        first.rect.top - second.rect.top
                        || first.rect.left - second.rect.left
                        || (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                    );

                const target = tabCandidates[0]?.tab || null;
                if (!target) {
                    return false;
                }

                const className = normalizeClassName(target);
                const alreadySelected = target.getAttribute('aria-selected') === 'true'
                    || /\\b(selected|active)\\b/.test(className);

                if (!alreadySelected) {
                    target.scrollIntoView({ block: 'center', inline: 'nearest' });
                    target.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true }));
                    target.dispatchEvent(new MouseEvent('mouseup', { bubbles: true, cancelable: true }));
                    target.click();
                }

                return true;

                function visible(element) {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                }

                function clickableTabAncestor(element) {
                    for (let current = element.parentElement; current && current !== document.body; current = current.parentElement) {
                        const text = tabText(current).toLowerCase();
                        if (text !== expected) {
                            continue;
                        }

                        const className = normalizeClassName(current);
                        const role = String(current.getAttribute('role') || '').toLowerCase();
                        const style = getComputedStyle(current);
                        if (role === 'tab'
                            || role === 'button'
                            || /\\b(tab|tabs|button|btn)\\b/.test(className)
                            || style.cursor === 'pointer') {
                            return current;
                        }
                    }

                    return null;
                }

                function normalizeClassName(element) {
                    return String(element.className || '')
                        .replace(/inactive/ig, ' ')
                        .replace(/[_-]+/g, ' ')
                        .toLowerCase();
                }
                """, tabName.trim());

        if (Boolean.TRUE.equals(handled)) {
            waitForAdminLoadingToFinish();
            return true;
        }

        return false;
    }

    private String getPageText() {
        Object result = executeScript("""
                return document.body
                    ? String(document.body.innerText || document.body.textContent || '').replace(/\\s+/g, ' ').trim()
                    : '';
                """);

        return result == null ? "" : String.valueOf(result);
    }

    private List<String> toStringList(Object result) {
        List<String> values = new ArrayList<>();
        if (!(result instanceof List<?> rawValues)) {
            return values;
        }

        for (Object rawValue : rawValues) {
            if (rawValue == null) {
                continue;
            }

            String value = String.valueOf(rawValue).trim();
            if (!value.isBlank()) {
                values.add(value);
            }
        }

        return values;
    }

    private void waitForAdminLoadingToFinish() {
        waitUntil(ORDER_DETAILS_TIMEOUT, webDriver -> Boolean.TRUE.equals(executeScript("""
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };

                return !Array.from(document.querySelectorAll(
                    '[role="progressbar"], [class*="spinner"], [class*="loader"], [class*="loading"], .ant-spin, .MuiCircularProgress-root'
                )).some(visible);
                """)));
    }

    private void confirmDialogIfPresent() {
        try {
            waitUntil(DIALOG_APPEAR_TIMEOUT, webDriver -> isDisplayedNow(CONFIRMATION_DIALOG));
        } catch (TimeoutException ignored) {
            return;
        }

        clickDialogConfirmButton();

        try {
            waitUntil(DIALOG_CLOSE_TIMEOUT, webDriver -> !isDisplayedNow(CONFIRMATION_DIALOG));
        } catch (TimeoutException exception) {
            clickVisibleDialogAction("Confirm");
            try {
                waitUntil(DIALOG_CLOSE_TIMEOUT, webDriver -> !isDisplayedNow(CONFIRMATION_DIALOG));
            } catch (TimeoutException ignored) {
                refreshPage();
            }
        }

        waitForAdminLoadingToFinish();
    }

    private void clickDialogConfirmButton() {
        try {
            clickWithFallback(CONFIRMATION_DIALOG_CONFIRM_BUTTON);
        } catch (RuntimeException exception) {
            clickVisibleDialogAction("Confirm");
        }
    }

    private boolean isVisibleActionPresent(String expectedText) {
        return Boolean.TRUE.equals(executeScript("""
                const expected = String(arguments[0] || '').trim().toLowerCase();
                return Boolean(findAction(expected));

                function findAction(expectedText) {
                    return Array.from(document.querySelectorAll('button, a, input, [role="button"], [class*="btn"], [class*="Button"]'))
                        .filter(visible)
                        .find(element => actionText(element).toLowerCase().includes(expectedText)) || null;
                }

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

                function visible(element) {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                }
                """, expectedText));
    }

    private void clickVisibleAction(String expectedText) {
        Boolean clicked = (Boolean) executeScript("""
                const expected = String(arguments[0] || '').trim().toLowerCase();
                const action = findAction(expected);
                if (!action) {
                    return false;
                }

                action.scrollIntoView({ block: 'center', inline: 'nearest' });
                action.click();
                return true;

                function findAction(expectedText) {
                    return Array.from(document.querySelectorAll('button, a, input, [role="button"], [class*="btn"], [class*="Button"]'))
                        .filter(visible)
                        .find(element => actionText(element).toLowerCase().includes(expectedText)) || null;
                }

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

                function visible(element) {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                }
                """, expectedText);

        if (!Boolean.TRUE.equals(clicked)) {
            throw new TimeoutException(expectedText + " action was not found.");
        }
    }

    private void clickVisibleDialogAction(String expectedText) {
        Boolean clicked = (Boolean) executeScript("""
                const expected = String(arguments[0] || '').trim().toLowerCase();
                const dialog = Array.from(document.querySelectorAll(
                    '[role="dialog"], [class*="modal"], [class*="Modal"], [class*="dialog"], [class*="Dialog"]'
                )).filter(visible).at(-1);
                if (!dialog) {
                    return false;
                }

                const action = Array.from(dialog.querySelectorAll('button, a, input, [role="button"], [class*="btn"], [class*="Button"]'))
                    .filter(visible)
                    .find(element => actionText(element).toLowerCase().includes(expected)) || null;
                if (!action) {
                    return false;
                }

                action.scrollIntoView({ block: 'center', inline: 'nearest' });
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

                function visible(element) {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                }
                """, expectedText);

        if (!Boolean.TRUE.equals(clicked)) {
            throw new TimeoutException(expectedText + " dialog action was not found.");
        }
    }

    private static final class PatternMatcher {

        private PatternMatcher() {
        }

        private static boolean matches(String text, String pattern) {
            return java.util.regex.Pattern.compile(pattern)
                    .matcher(text == null ? "" : text)
                    .find();
        }
    }
}
