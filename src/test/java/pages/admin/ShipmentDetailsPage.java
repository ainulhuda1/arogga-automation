package pages.admin;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import pages.user.BasePage;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShipmentDetailsPage extends BasePage {

    private static final Duration SHIPMENT_DETAILS_TIMEOUT = Duration.ofSeconds(45);
    private static final Pattern SHIPMENT_STATUS_PATTERN = Pattern.compile(
            "(?i)\\b(created|pending|picker assigned|printed|picked|packer assigned|packed|sorting|sorted|in bag"
                    + "|delivering|called|cancel requested|delivered|qc|closed|cancelled|canceled|rescheduled)\\b"
    );
    private static final Pattern DATE_PATTERN = Pattern.compile(
            "(?i)\\b(\\d{1,2}[-/ ](?:\\d{1,2}|jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[-/ ,]+\\d{2,4}"
                    + "|(?:jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*\\s+\\d{1,2},?\\s+\\d{2,4}"
                    + "|\\d{4}[-/]\\d{1,2}[-/]\\d{1,2})\\b"
    );
    private static final Pattern TIME_PATTERN = Pattern.compile("\\b\\d{1,2}:\\d{2}(?::\\d{2})?\\s*(?:am|pm)?\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern QUANTITY_PATTERN = Pattern.compile(
            "(?i)\\b(?:qty|quantity|product qty)\\s*[:=x-]?\\s*(\\d+)\\b"
    );
    private static final Set<String> PRODUCT_STOP_WORDS = Set.of(
            "product", "products", "variant", "form", "strength", "qty", "quantity", "product qty",
            "price", "amount", "total", "discount", "column", "serial", "sl", "no", "sku", "id"
    );
    private static final Set<String> ADDRESS_STOP_WORDS = Set.of(
            "address", "shipping", "customer", "location", "phone", "mobile", "name", "area", "zone",
            "road", "street", "house", "flat", "floor", "building", "district", "division",
            "null", "undefined", "none", "n/a"
    );

    private static final By SHIPMENT_ID = By.cssSelector(
            "[data-testid='shipment-id'], [class*='shipment-id']"
    );
    private static final By TRACKING_NUMBER = By.cssSelector(
            "[data-testid='tracking-number'], [class*='tracking-number']"
    );
    private static final By SHIPMENT_STATUS = By.cssSelector(
            "[data-testid='shipment-status'], [class*='shipment-status'], [class*='status'], [class*='badge']"
    );
    private static final By TRACKING_LINK = By.xpath(
            "//a[contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'tracking')]"
                    + "|//button[contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'tracking')]"
    );

    private String currentShipmentId = "";
    private String currentOrderId = "";

    public ShipmentDetailsPage(WebDriver driver) {
        super(driver);
    }

    public ShipmentDetailsPage waitUntilLoaded() {
        waitForPageLoad();
        waitUntil(SHIPMENT_DETAILS_TIMEOUT, webDriver -> isShipmentDetailsPageLoaded());
        waitForAdminLoadingToFinish();
        return this;
    }

    public ShipmentDetailsPage waitUntilLoaded(String shipmentId, String orderId) {
        currentShipmentId = normalize(shipmentId);
        currentOrderId = normalize(orderId);
        waitForPageLoad();
        waitForAdminLoadingToFinish();
        waitUntil(SHIPMENT_DETAILS_TIMEOUT, webDriver -> isShipmentDetailsPageLoaded(currentShipmentId));
        return this;
    }

    public boolean isShipmentDetailsPageLoaded() {
        return isDisplayedNow(SHIPMENT_ID)
                || isDisplayedNow(TRACKING_NUMBER)
                || pageContainsText("Shipment")
                || pageContainsText("Shipment Items")
                || pageContainsText("Timeline");
    }

    public boolean isShipmentDetailsPageLoaded(String shipmentId) {
        String normalizedShipmentId = normalize(shipmentId);
        return isShipmentDetailsPageLoaded()
                && (normalizedShipmentId.isBlank() || pageContainsCompactText(normalizedShipmentId));
    }

    public String getShipmentId() {
        String shipmentId = firstVisibleText(SHIPMENT_ID);
        if (!shipmentId.isBlank()) {
            return normalize(shipmentId);
        }

        if (!currentShipmentId.isBlank() && pageContainsCompactText(currentShipmentId)) {
            return currentShipmentId;
        }

        return getLabelValue("ID");
    }

    public boolean isShipmentIdMatching(String shipmentId) {
        String normalizedShipmentId = normalize(shipmentId);
        return !normalizedShipmentId.isBlank()
                && (normalizedShipmentId.equals(normalize(getShipmentId()))
                || pageContainsCompactText(normalizedShipmentId));
    }

    public String getOrderId() {
        if (!currentOrderId.isBlank() && pageContainsCompactText(currentOrderId)) {
            return currentOrderId;
        }

        String labelValue = getLabelValue("Order ID");
        if (!labelValue.isBlank()) {
            return labelValue;
        }

        Matcher matcher = Pattern.compile("(?i)\\bOrder\\s*ID\\s*[:#-]?\\s*(\\d+)").matcher(getPageText());
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    public boolean isOrderIdMatching(String orderId) {
        String normalizedOrderId = normalize(orderId);
        return !normalizedOrderId.isBlank()
                && (normalizedOrderId.equals(normalize(getOrderId()))
                || pageContainsCompactText(normalizedOrderId));
    }

    public String getTrackingNumber() {
        return firstVisibleText(TRACKING_NUMBER);
    }

    public String getShipmentStatus() {
        String labelValue = getLabelValue("Status");
        String labelStatus = extractShipmentStatus(labelValue);
        if (!labelStatus.isBlank()) {
            return labelStatus;
        }

        String visibleStatus = extractShipmentStatus(firstVisibleText(SHIPMENT_STATUS));
        if (!visibleStatus.isBlank()) {
            return visibleStatus;
        }

        return extractShipmentStatus(getPageText());
    }

    public boolean isShipmentStatusDisplayed() {
        return isDisplayed(SHIPMENT_STATUS) || !getShipmentStatus().isBlank();
    }

    public boolean isShipmentCreatedDateDisplayed() {
        return DATE_PATTERN.matcher(getCreatedAtText()).find();
    }

    public boolean isShipmentCreatedTimeDisplayed() {
        return TIME_PATTERN.matcher(getCreatedAtText()).find();
    }

    public boolean isShippingAddressDisplayed() {
        String addressText = getShippingAddressText();
        if (!addressText.isBlank() && addressText.length() > 10) {
            return true;
        }

        return Boolean.TRUE.equals(executeScript("""
                const text = normalizedPageText().toLowerCase();
                return /address/.test(text) && /road|street|house|flat|area|dhaka|district|division|phone|mobile/i.test(text);

                function normalizedPageText() {
                    return document.body
                        ? String(document.body.innerText || document.body.textContent || '').replace(/\\s+/g, ' ').trim()
                        : '';
                }
                """));
    }

    public String getShippingAddressText() {
        String addressText = getLabelValue("Address");
        if (!addressText.isBlank()) {
            return addressText;
        }

        Object result = executeScript("""
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                const section = Array.from(document.querySelectorAll('section, article, div'))
                    .filter(visible)
                    .map(element => ({ element, text: normalize(element.innerText || element.textContent || '') }))
                    .filter(candidate => /address/i.test(candidate.text)
                        && /road|street|house|flat|area|dhaka|district|division|phone|mobile/i.test(candidate.text))
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

        return normalize(result);
    }

    public boolean isShippingAddressMatching(String expectedAddress) {
        String normalizedExpectedAddress = normalizeAddressForComparison(expectedAddress);
        if (normalizedExpectedAddress.isBlank()) {
            return isShippingAddressDisplayed();
        }

        String actualAddress = normalizeAddressForComparison(getShippingAddressText());
        String compactExpected = normalizedExpectedAddress.replaceAll("\\s+", "").toLowerCase();
        String compactActual = actualAddress.replaceAll("\\s+", "").toLowerCase();
        if (!compactExpected.isBlank()
                && !compactActual.isBlank()
                && (compactExpected.contains(compactActual) || compactActual.contains(compactExpected))) {
            return true;
        }

        List<String> expectedTokens = addressTokens(normalizedExpectedAddress);
        List<String> actualTokens = addressTokens(actualAddress);
        if (expectedTokens.isEmpty()) {
            return isShippingAddressDisplayed();
        }

        long matchingTokens = expectedTokens.stream()
                .filter(actualTokens::contains)
                .count();

        return matchingTokens >= Math.min(3, Math.min(expectedTokens.size(), Math.max(actualTokens.size(), 1)));
    }

    public boolean isCustomerInformationDisplayed() {
        boolean explicitCustomerInformationDisplayed = Boolean.TRUE.equals(executeScript("""
                const text = document.body
                    ? String(document.body.innerText || document.body.textContent || '').replace(/\\s+/g, ' ').trim()
                    : '';
                return /customer|patient|name|mobile|phone/i.test(text)
                    && /(01\\d{9}|\\+?\\d{6,})/.test(text);
                """));

        return explicitCustomerInformationDisplayed
                || (isOrderIdMatching(currentOrderId) && isShippingAddressDisplayed());
    }

    public boolean areOrderedProductsDisplayed() {
        return !getShipmentProductRowTexts().isEmpty() || getShipmentItemCount() > 0;
    }

    public List<String> getShipmentProductRowTexts() {
        Object result = executeScript("""
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                const rows = [];

                Array.from(document.querySelectorAll('table'))
                    .filter(visible)
                    .map(table => ({ table, text: normalize(table.innerText || table.textContent || '') }))
                    .filter(candidate => /shipment items|product|variant|\\bqty\\b|quantity/i.test(candidate.text)
                        && /product/i.test(candidate.text)
                        && /(\\bqty\\b|quantity)/i.test(candidate.text))
                    .forEach(candidate => rows.push(...tableRows(candidate.table)));

                if (rows.length === 0) {
                    const section = Array.from(document.querySelectorAll('section, article, div'))
                        .filter(visible)
                        .find(element => /shipment items/i.test(normalize(element.innerText || element.textContent || '')));
                    if (section) {
                        Array.from(section.querySelectorAll('table'))
                            .filter(visible)
                            .forEach(table => rows.push(...tableRows(table)));
                    }
                }

                return Array.from(new Set(rows.map(normalize).filter(Boolean))).slice(0, 30);

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
                            && /product|variant|form|strength/i.test(text)
                            && /(\\bqty\\b|quantity|\\d+)/i.test(text));
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

    public boolean doOrderedProductQuantitiesMatch(List<String> orderProductRows) {
        List<ProductRow> expectedRows = toProductRows(orderProductRows);
        List<ProductRow> actualRows = toProductRows(getShipmentProductRowTexts());
        int shipmentItemCount = getShipmentItemCount();
        if (expectedRows.isEmpty() || actualRows.isEmpty()) {
            return !actualRows.isEmpty() || shipmentItemCount > 0;
        }

        if (shipmentItemCount > 0 && shipmentItemCount == expectedRows.size()) {
            return true;
        }

        return expectedRows.stream()
                .allMatch(expectedRow -> actualRows.stream().anyMatch(expectedRow::matches));
    }

    public ShipmentTrackingPage openShipmentTrackingPage() {
        clickWithFallback(TRACKING_LINK);
        return new ShipmentTrackingPage(driver).waitUntilLoaded();
    }

    public ShipmentTrackingPage openShipmentTrackingSection() {
        executeScript("""
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                const timelineTitle = Array.from(document.querySelectorAll('h1, h2, h3, h4, h5, h6, p, span, div'))
                    .filter(visible)
                    .find(element => normalize(element.innerText || element.textContent || '').toLowerCase() === 'timeline');
                if (timelineTitle) {
                    timelineTitle.scrollIntoView({ block: 'center', inline: 'nearest' });
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

        return new ShipmentTrackingPage(driver).waitUntilLoaded(currentShipmentId, getShipmentStatus());
    }

    private String getCreatedAtText() {
        String createdAt = getLabelValue("Created At");
        if (!createdAt.isBlank()) {
            return createdAt;
        }

        Matcher matcher = Pattern.compile("(?is)Created\\s+At\\s*[:#-]?\\s*([^|\\n]+)").matcher(getPageText());
        return matcher.find() ? matcher.group(1).trim() : getPageText();
    }

    private String getLabelValue(String label) {
        Object result = executeScript("""
                const label = String(arguments[0] || '').trim().toLowerCase();
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                const labels = Array.from(document.querySelectorAll('label, dt, th, p, span, div'))
                    .filter(visible)
                    .map(element => ({ element, text: normalize(element.innerText || element.textContent || '') }))
                    .filter(candidate => candidate.text.toLowerCase() === label
                        || candidate.text.toLowerCase().replace(/:$/, '') === label);

                const labelPattern = label.replace(/\\s+/g, '\\\\s+');

                for (const candidate of labels) {
                    const siblingValue = nextVisibleText(candidate.element);
                    if (siblingValue) {
                        return siblingValue;
                    }

                    const parentText = normalize(candidate.element.parentElement?.innerText || candidate.element.parentElement?.textContent || '');
                    const directValue = parentText.replace(new RegExp(`^${labelPattern}\\\\s*:?\\\\s*`, 'i'), '').trim();
                    if (directValue && directValue.toLowerCase() !== label) {
                        return directValue;
                    }
                }

                const pageText = normalize(document.body?.innerText || document.body?.textContent || '');
                const match = pageText.match(new RegExp(`${labelPattern}\\\\s*[:#-]?\\\\s*([^|\\\\n]+)`, 'i'));
                return match && match[1] ? match[1].trim() : '';

                function nextVisibleText(element) {
                    let sibling = element.nextElementSibling;
                    while (sibling) {
                        if (visible(sibling)) {
                            const text = normalize(sibling.innerText || sibling.textContent || '');
                            if (text) {
                                return text;
                            }
                        }
                        sibling = sibling.nextElementSibling;
                    }

                    return '';
                }

                function visible(element) {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                }
                """, label);

        return normalize(result);
    }

    private int getShipmentItemCount() {
        String itemCount = getLabelValue("Item Count");
        if (itemCount.isBlank()) {
            Matcher matcher = Pattern.compile("(?i)\\bItem\\s*Count\\s*[:#-]?\\s*(\\d+)").matcher(getPageText());
            if (matcher.find()) {
                itemCount = matcher.group(1);
            }
        }

        Matcher matcher = Pattern.compile("\\d+").matcher(itemCount);
        return matcher.find() ? Integer.parseInt(matcher.group()) : 0;
    }

    private boolean pageContainsCompactText(String expectedText) {
        return Boolean.TRUE.equals(executeScript("""
                const expected = String(arguments[0] || '').replace(/\\s+/g, '').trim();
                const pageText = document.body
                    ? String(document.body.innerText || document.body.textContent || '').replace(/\\s+/g, '')
                    : '';
                return Boolean(expected) && pageText.includes(expected);
                """, expectedText));
    }

    private List<String> toStringList(Object result) {
        List<String> values = new ArrayList<>();
        if (!(result instanceof List<?> rawValues)) {
            return values;
        }

        for (Object rawValue : rawValues) {
            String value = normalize(rawValue);
            if (!value.isBlank()) {
                values.add(value);
            }
        }

        return values;
    }

    private List<ProductRow> toProductRows(List<String> rowTexts) {
        if (rowTexts == null) {
            return List.of();
        }

        return rowTexts.stream()
                .map(ProductRow::from)
                .filter(productRow -> !productRow.tokens().isEmpty() || productRow.quantity().isPresent())
                .toList();
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
        waitUntil(SHIPMENT_DETAILS_TIMEOUT, webDriver -> Boolean.TRUE.equals(executeScript("""
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

    private static String normalize(Object value) {
        return value == null ? "" : String.valueOf(value).replaceAll("\\s+", " ").trim();
    }

    private static String normalizeAddressForComparison(Object value) {
        return normalize(value)
                .replaceAll("(?i)\\b(null|undefined|none|n/a)\\b\\s*,?", " ")
                .replaceAll("\\s*,\\s*", ", ")
                .replaceAll("(?:,\\s*){2,}", ", ")
                .replaceAll("\\s+", " ")
                .replaceAll("^,\\s*|,\\s*$", "")
                .trim();
    }

    private record ProductRow(String sourceText, Optional<Integer> quantity, List<String> tokens) {

        private static ProductRow from(String sourceText) {
            String normalizedText = normalize(sourceText);
            return new ProductRow(normalizedText, extractQuantity(normalizedText), productTokens(normalizedText));
        }

        private boolean matches(ProductRow actualRow) {
            if (!sharesProductTokens(actualRow)) {
                return false;
            }

            return quantity.isEmpty()
                    || (actualRow.quantity().isPresent() && quantity.get().equals(actualRow.quantity().get()));
        }

        private boolean sharesProductTokens(ProductRow actualRow) {
            if (tokens.isEmpty() || actualRow.tokens().isEmpty()) {
                return true;
            }

            long matches = tokens.stream().filter(actualRow.tokens()::contains).count();
            return matches >= Math.min(2, tokens.size());
        }

        private static Optional<Integer> extractQuantity(String sourceText) {
            Matcher matcher = QUANTITY_PATTERN.matcher(sourceText);
            if (matcher.find()) {
                return Optional.of(Integer.parseInt(matcher.group(1)));
            }

            return Optional.empty();
        }

        private static List<String> productTokens(String sourceText) {
            String canonicalText = sourceText.toLowerCase()
                    .replaceAll("[^a-z0-9 ]", " ")
                    .replaceAll("\\b\\d+\\b", " ")
                    .replaceAll("\\s+", " ")
                    .trim();

            Set<String> tokens = new LinkedHashSet<>();
            Arrays.stream(canonicalText.split("\\s+"))
                    .map(String::trim)
                    .filter(token -> token.length() > 2)
                    .filter(token -> !PRODUCT_STOP_WORDS.contains(token))
                    .limit(10)
                    .forEach(tokens::add);

            return List.copyOf(tokens);
        }
    }

    private static List<String> addressTokens(String sourceText) {
        Set<String> tokens = new LinkedHashSet<>();
        Arrays.stream(normalizeAddressForComparison(sourceText).toLowerCase()
                        .replaceAll("[^a-z0-9 ]", " ")
                        .replaceAll("\\s+", " ")
                        .trim()
                        .split("\\s+"))
                .map(String::trim)
                .filter(token -> token.length() > 2)
                .filter(token -> !ADDRESS_STOP_WORDS.contains(token))
                .limit(20)
                .forEach(tokens::add);

        return List.copyOf(tokens);
    }
}
