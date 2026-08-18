package pages.admin;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import pages.user.BasePage;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShipmentVerificationPage extends BasePage {

    private static final Duration SHIPMENT_TIMEOUT = Duration.ofSeconds(45);
    private static final Duration LOADING_APPEAR_TIMEOUT = Duration.ofSeconds(5);
    private static final Pattern SHIPMENT_STATUS_PATTERN = Pattern.compile(
            "(?i)\\b(created|pending|picker assigned|printed|picked|packer assigned|packed|sorting|sorted|in bag"
                    + "|delivering|called|cancel requested|delivered|qc|closed|cancelled|canceled|rescheduled)\\b"
    );

    private static final By VERIFY_SHIPMENT_BUTTON = By.xpath(
            "//button[contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'verify')]"
    );
    private static final By LOAD_SHIPMENT_BUTTON = By.xpath(
            "//*[self::button or self::a or @role='button']"
                    + "[contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'load shipment')]"
    );
    private static final By SHIPMENT_STATUS = By.cssSelector(
            "[data-testid='shipment-status'], [class*='shipment-status'], [class*='status'], [class*='badge']"
    );
    private static final By SHIPMENT_DETAILS_LINK = By.xpath(
            "//a[contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'details')]"
                    + "|//button[contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'details')]"
                    + "|//a[contains(translate(@href,"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'/v1/shipment/')]"
    );

    private String currentOrderId = "";

    public ShipmentVerificationPage(WebDriver driver) {
        super(driver);
    }

    public ShipmentVerificationPage waitUntilLoaded() {
        waitForPageLoad();
        waitUntil(SHIPMENT_TIMEOUT, webDriver -> isShipmentVerificationPageLoaded());
        waitForAdminLoadingToFinish();
        return this;
    }

    public ShipmentVerificationPage waitUntilLoaded(String orderId) {
        currentOrderId = normalize(orderId);
        waitForPageLoad();
        waitForAdminLoadingToFinish();
        waitUntil(SHIPMENT_TIMEOUT, webDriver -> isShipmentVerificationPageLoaded(currentOrderId));
        return this;
    }

    public boolean isShipmentVerificationPageLoaded() {
        return isDisplayedNow(VERIFY_SHIPMENT_BUTTON)
                || isDisplayedNow(SHIPMENT_STATUS)
                || isDisplayedNow(LOAD_SHIPMENT_BUTTON)
                || pageContainsText("Load Shipment");
    }

    public boolean isShipmentVerificationPageLoaded(String orderId) {
        String normalizedOrderId = normalize(orderId);
        if (normalizedOrderId.isBlank()) {
            return isShipmentVerificationPageLoaded();
        }

        return pageContainsText(normalizedOrderId)
                && (isLoadShipmentButtonDisplayedNow() || !getShipmentRowText(normalizedOrderId).isBlank());
    }

    public String getShipmentStatus() {
        String status = firstVisibleText(SHIPMENT_STATUS);
        if (!status.isBlank()) {
            return status;
        }

        return extractShipmentStatus(getPageText());
    }

    public String getShipmentStatus(String orderId) {
        String status = extractShipmentStatus(getShipmentRowText(orderId));
        return status.isBlank() ? getShipmentStatus() : status;
    }

    public boolean isVerifyShipmentButtonVisible() {
        return isDisplayed(VERIFY_SHIPMENT_BUTTON);
    }

    public boolean isLoadShipmentButtonDisplayed() {
        return isLoadShipmentButtonDisplayedNow();
    }

    public ShipmentVerificationPage verifyShipment() {
        clickWithFallback(VERIFY_SHIPMENT_BUTTON);

        try {
            waitUntil(Duration.ofSeconds(10), webDriver -> !getShipmentStatus().isBlank());
        } catch (TimeoutException ignored) {
            // Status text is asserted by the test when the application exposes it.
        }

        return this;
    }

    public ShipmentVerificationPage clickLoadShipment() {
        if (!clickVisibleLoadShipmentButton()) {
            clickWithFallback(LOAD_SHIPMENT_BUTTON);
        }

        try {
            waitUntil(LOADING_APPEAR_TIMEOUT, webDriver -> isAdminLoadingActive() || !isLoadShipmentButtonDisplayedNow());
        } catch (TimeoutException ignored) {
            // Some successful calls finish before the loading overlay is observable.
        }

        waitForAdminLoadingToFinish();
        return this;
    }

    public ShipmentVerificationPage waitUntilShipmentInformationLoads(String orderId) {
        String normalizedOrderId = normalize(orderId);
        waitForAdminLoadingToFinish();
        waitUntil(SHIPMENT_TIMEOUT, webDriver -> isShipmentSuccessfullyCreated(normalizedOrderId));
        return this;
    }

    public boolean isShipmentSuccessfullyCreated(String orderId) {
        return !getShipmentId(orderId).isBlank()
                && !getShipmentRowText(orderId).isBlank();
    }

    public boolean isShipmentInformationLoaded(String orderId) {
        return !getShipmentRowText(orderId).isBlank();
    }

    public String getShipmentId(String orderId) {
        String normalizedOrderId = normalize(orderId);
        if (normalizedOrderId.isBlank()) {
            return "";
        }

        Object result = executeScript("""
                const orderId = String(arguments[0] || '').trim();
                const row = findShipmentRow(orderId);
                if (!row) {
                    return '';
                }

                const links = Array.from(row.querySelectorAll('a[href*="/v1/shipment/"], a[href*="shipment"]'))
                    .filter(visible)
                    .map(link => normalize(link.innerText || link.textContent || ''))
                    .filter(Boolean);
                const matchingLinkText = links.find(text => compact(text).includes(compact(orderId))) || links[0] || '';
                if (matchingLinkText) {
                    return compact(matchingLinkText);
                }

                const rowText = compact(row.innerText || row.textContent || '');
                const orderSequenceMatch = rowText.match(new RegExp(`${compact(orderId)}[A-Za-z0-9]*`));
                return orderSequenceMatch ? orderSequenceMatch[0] : '';

                function findShipmentRow(orderId) {
                    const order = compact(orderId);
                    return Array.from(document.querySelectorAll(
                        'tr, [role="row"], [data-testid*="row"], [class*="row"], [class*="Row"]'
                    ))
                        .filter(visible)
                        .map(element => ({ element, text: normalize(element.innerText || element.textContent || '') }))
                        .filter(candidate => /shipment status|shipment type|tracking|cold|issue type|rescheduled/i.test(candidate.text)
                            || Array.from(candidate.element.querySelectorAll('a[href*="/v1/shipment/"]')).some(visible))
                        .find(candidate => compact(candidate.text).includes(order))?.element || null;
                }

                function compact(text) {
                    return normalize(text).replace(/\\s+/g, '');
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
                """, normalizedOrderId);

        return normalize(result);
    }

    public boolean doesShipmentBelongToOrder(String orderId) {
        String normalizedOrderId = normalize(orderId);
        String shipmentId = getShipmentId(normalizedOrderId);

        return !normalizedOrderId.isBlank()
                && !shipmentId.isBlank()
                && shipmentId.contains(normalizedOrderId)
                && !getShipmentRowText(normalizedOrderId).isBlank();
    }

    public ShipmentDetailsPage openShipmentDetailsPage() {
        clickWithFallback(SHIPMENT_DETAILS_LINK);
        return new ShipmentDetailsPage(driver).waitUntilLoaded();
    }

    public ShipmentDetailsPage openShipmentDetailsPage(String shipmentId) {
        String normalizedShipmentId = normalize(shipmentId);
        if (!clickShipmentDetailsLink(normalizedShipmentId)) {
            clickWithFallback(SHIPMENT_DETAILS_LINK);
        }

        return new ShipmentDetailsPage(driver).waitUntilLoaded(normalizedShipmentId, currentOrderId);
    }

    private boolean isLoadShipmentButtonDisplayedNow() {
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

    private boolean clickVisibleLoadShipmentButton() {
        return Boolean.TRUE.equals(executeScript("""
                const action = findLoadShipmentAction();
                if (!action) {
                    return false;
                }

                action.scrollIntoView({ block: 'center', inline: 'nearest' });
                action.click();
                return true;

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

    private boolean clickShipmentDetailsLink(String shipmentId) {
        return Boolean.TRUE.equals(executeScript("""
                const shipmentId = compact(arguments[0] || '');
                const currentOrderId = compact(arguments[1] || '');
                const links = Array.from(document.querySelectorAll('a[href*="/v1/shipment/"], a[href*="shipment"]'))
                    .filter(visible)
                    .map(link => ({
                        link,
                        text: compact(link.innerText || link.textContent || ''),
                        href: String(link.getAttribute('href') || '')
                    }));
                const target = links.find(candidate => shipmentId && candidate.text.includes(shipmentId))
                    || links.find(candidate => currentOrderId && candidate.text.includes(currentOrderId))
                    || links.find(candidate => /\\/v1\\/shipment\\/\\d+\\/show/i.test(candidate.href))
                    || null;

                if (!target) {
                    return false;
                }

                target.link.scrollIntoView({ block: 'center', inline: 'nearest' });
                target.link.click();
                return true;

                function compact(text) {
                    return String(text || '').replace(/\\s+/g, '').trim();
                }

                function visible(element) {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                }
                """, shipmentId, currentOrderId));
    }

    private String getShipmentRowText(String orderId) {
        String normalizedOrderId = normalize(orderId);
        if (normalizedOrderId.isBlank()) {
            return "";
        }

        Object result = executeScript("""
                const orderId = String(arguments[0] || '').trim();
                const order = compact(orderId);
                const row = Array.from(document.querySelectorAll(
                    'tr, [role="row"], [data-testid*="row"], [class*="row"], [class*="Row"]'
                ))
                    .filter(visible)
                    .map(element => ({ element, text: normalize(element.innerText || element.textContent || '') }))
                    .filter(candidate => compact(candidate.text).includes(order))
                    .filter(candidate => /shipment status|shipment type|tracking|cold|issue type|rescheduled/i.test(candidate.text)
                        || Array.from(candidate.element.querySelectorAll('a[href*="/v1/shipment/"]')).some(visible))
                    .sort((first, second) =>
                        (first.element.getBoundingClientRect().width * first.element.getBoundingClientRect().height)
                            - (second.element.getBoundingClientRect().width * second.element.getBoundingClientRect().height)
                    )[0];

                return row ? row.text : '';

                function compact(text) {
                    return normalize(text).replace(/\\s+/g, '');
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
                """, normalizedOrderId);

        return normalize(result);
    }

    private String extractShipmentStatus(String sourceText) {
        Matcher matcher = SHIPMENT_STATUS_PATTERN.matcher(normalize(sourceText));
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    private String getPageText() {
        Object result = executeScript("""
                return document.body
                    ? String(document.body.innerText || document.body.textContent || '').replace(/\\s+/g, ' ').trim()
                    : '';
                """);

        return normalize(result);
    }

    private void waitForAdminLoadingToFinish() {
        waitUntil(SHIPMENT_TIMEOUT, webDriver -> !isAdminLoadingActive());
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

    private String normalize(Object value) {
        return value == null ? "" : String.valueOf(value).replaceAll("\\s+", " ").trim();
    }
}
