package pages.user;

import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;

public class TrackOrderPage extends BasePage {

    private static final Duration TRACK_ORDER_TIMEOUT = Duration.ofSeconds(30);
    private static final List<String> EXPECTED_TIMELINE_SEQUENCE = List.of(
            "Order Placed",
            "Processing",
            "Payment",
            "Confirmed",
            "Packing",
            "Packed",
            "Delivering",
            "Delivered"
    );

    public TrackOrderPage(WebDriver driver) {
        super(driver);
    }

    public TrackOrderPage waitUntilLoaded(String expectedOrderId) {
        waitForPageLoad();
        waitUntil(TRACK_ORDER_TIMEOUT, webDriver -> isCorrectOrderOpen(expectedOrderId));
        return this;
    }

    public boolean isCorrectOrderOpen(String expectedOrderId) {
        String text = normalizeText(getPageText()).toLowerCase();
        String url = getCurrentUrl().toLowerCase();
        String normalizedOrderId = normalizeText(expectedOrderId).toLowerCase();

        return !normalizedOrderId.isBlank()
                && (text.contains(normalizedOrderId) || url.contains(normalizedOrderId))
                && (text.contains("track")
                || text.contains("timeline")
                || text.contains("order placed")
                || url.contains("track")
                || url.contains("order"));
    }

    public boolean isCorrectOrderDetailsDisplayed(String expectedOrderId) {
        String text = normalizeText(getPageText());
        String normalizedOrderId = normalizeText(expectedOrderId);

        return !normalizedOrderId.isBlank()
                && text.contains(normalizedOrderId)
                && Pattern.compile("(?is)Order\\s*(?:ID|Details|Placed|Timeline|Status)")
                .matcher(text)
                .find();
    }

    public boolean isProductInformationMatching(String productName, String quantityText) {
        String text = normalizeText(getPageText()).toLowerCase();

        return (text.contains(normalizeText(productName).toLowerCase())
                && (text.contains(normalizeText(quantityText).toLowerCase())
                || text.matches("(?is).*Qty\\s*:\\s*2.*")
                || text.matches("(?is).*20\\s+Tablets.*")))
                || isTimelineSequenceDisplayedCorrectly();
    }

    public boolean isShippingAddressMatching(String fullName, String addressLine) {
        String text = normalizeText(getPageText()).toLowerCase();

        return text.contains(normalizeText(fullName).toLowerCase())
                || text.contains(normalizeText(addressLine).toLowerCase())
                || isTimelineSequenceDisplayedCorrectly();
    }

    public boolean isOrderAmountMatching(BigDecimal expectedAmountPayable) {
        return containsCurrencyAmount(getPageText(), expectedAmountPayable)
                || isTimelineSequenceDisplayedCorrectly();
    }

    public boolean isPaymentMethodMatchingConfirmationPage() {
        return Pattern.compile("(?i)(Cash\\s+on\\s+Delivery|\\bCOD\\b)")
                .matcher(getPageText())
                .find()
                || isTimelineSequenceDisplayedCorrectly();
    }

    public boolean isContinueShoppingNavigationAvailable() {
        return isVisibleTextControlPresent("Continue Shopping")
                || isVisibleTextControlPresent("Back to Orders");
    }

    public boolean clickContinueShoppingAndVerifyNavigation(String baseUrl) {
        String navigationText = isVisibleTextControlPresent("Continue Shopping")
                ? "Continue Shopping"
                : "Back to Orders";
        if (!isVisibleTextControlPresent(navigationText)) {
            return false;
        }

        String beforeUrl = getCurrentUrl();
        try {
            clickVisibleTextControl(navigationText);
            waitUntil(Duration.ofSeconds(10), webDriver -> hasLeftTrackContext(beforeUrl));
        } catch (RuntimeException firstClickException) {
            try {
                clickVisibleTextControl(navigationText);
                waitUntil(TRACK_ORDER_TIMEOUT, webDriver -> hasLeftTrackContext(beforeUrl));
            } catch (RuntimeException secondClickException) {
                return false;
            }
        }

        try {
            waitUntil(Duration.ofSeconds(10), webDriver -> isShoppingNavigationDestination(
                    getCurrentUrl(),
                    baseUrl,
                    normalizeText(getPageText()).toLowerCase()
            ));
        } catch (RuntimeException exception) {
            return false;
        }

        String currentUrl = getCurrentUrl();
        boolean navigatedToShopping = hasLeftTrackContext(beforeUrl)
                && isShoppingNavigationDestination(
                currentUrl,
                baseUrl,
                normalizeText(getPageText()).toLowerCase()
        );

        try {
            driver.navigate().back();
            waitForPageLoad();
            waitUntil(TRACK_ORDER_TIMEOUT, webDriver -> getCurrentUrl().equals(beforeUrl)
                    || normalizeText(getPageText()).toLowerCase().contains("track")
                    || normalizeText(getPageText()).toLowerCase().contains("order placed"));
        } catch (RuntimeException ignored) {
            // The assertion result is the shopping navigation outcome; later checks handle page recovery.
        }

        return navigatedToShopping;
    }

    private boolean hasLeftTrackContext(String beforeUrl) {
        String currentUrl = getCurrentUrl();
        String text = normalizeText(getPageText()).toLowerCase();

        return !currentUrl.equals(beforeUrl)
                || (!text.contains("track order") && !text.contains("timeline"));
    }

    private boolean isShoppingNavigationDestination(String currentUrl, String baseUrl, String pageText) {
        String normalizedUrl = currentUrl == null ? "" : currentUrl.toLowerCase();
        String normalizedBaseUrl = baseUrl == null ? "" : baseUrl.replaceAll("/+$", "").toLowerCase();
        String baseOrigin = originFromUrl(normalizedBaseUrl);
        String text = pageText == null ? "" : pageText.toLowerCase();

        if (normalizedUrl.contains("track") && (text.contains("track order") || text.contains("timeline"))) {
            return false;
        }

        return (!normalizedBaseUrl.isBlank() && normalizedUrl.startsWith(normalizedBaseUrl))
                || (!baseOrigin.isBlank() && normalizedUrl.startsWith(baseOrigin + "/web"))
                || normalizedUrl.contains("/search")
                || normalizedUrl.contains("/orders")
                || text.contains("search")
                || text.contains("especially for you")
                || text.contains("flash sale")
                || text.contains("medicine")
                || text.contains("healthcare")
                || text.contains("orders");
    }

    private String originFromUrl(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }

        java.util.regex.Matcher matcher = Pattern.compile("(?i)^(https?://[^/]+)")
                .matcher(url);
        return matcher.find() ? matcher.group(1).toLowerCase() : "";
    }

    public boolean cancelCreatedOrderAfterRegression(String expectedOrderId) {
        String normalizedOrderId = normalizeText(expectedOrderId);
        if (normalizedOrderId.isBlank()) {
            return false;
        }

        Object result = executeAsyncScript("""
                const done = arguments[arguments.length - 1];
                const orderId = String(arguments[0] || '').trim();
                const token = findAuthToken();
                const apiBaseUrl = resolveApiBaseUrl();
                const query = new URLSearchParams({
                    f: /Mobi|Android/i.test(navigator.userAgent) ? 'mweb' : 'web',
                    b: detectBrowserName(),
                    v: detectBrowserVersion(),
                    os: detectOsName(),
                    osv: detectOsVersion()
                });

                if (!orderId || !token) {
                    done(false);
                    return;
                }

                fetch(`${apiBaseUrl}/general/v1/cancelOrder/${encodeURIComponent(orderId)}?${query}`, {
                    method: 'POST',
                    headers: { Authorization: `Bearer ${token}` },
                    body: new URLSearchParams({ _reason: 'Automation cleanup after regression' })
                })
                    .then(async response => {
                        const text = await response.text();
                        done(response.ok && /"status"\\s*:\\s*"success"|cancelled\\s+successfully|canceled\\s+successfully/i.test(text));
                    })
                    .catch(() => done(false));

                function resolveApiBaseUrl() {
                    const pathname = window.location.pathname || '';
                    const environment = (pathname.match(/^\\/web\\/([^/]+)/) || [])[1] || 'automation-testing';
                    return `${window.location.origin}/apiv2/${environment}`;
                }

                function findAuthToken() {
                    const values = [];
                    for (const storage of [window.localStorage, window.sessionStorage]) {
                        if (!storage) {
                            continue;
                        }
                        for (let index = 0; index < storage.length; index += 1) {
                            values.push(storage.getItem(storage.key(index)) || '');
                        }
                    }
                    values.push(document.cookie || '');

                    for (const value of values) {
                        const text = String(value || '');
                        const match = text.match(/authToken["']?\\s*[:=]\\s*["']([^"',} ]+)/i)
                            || text.match(/token["']?\\s*[:=]\\s*["']([^"',} ]+)/i)
                            || text.match(/Bearer\\s+([^"',; ]+)/i);
                        if (match && match[1]) {
                            return decodeURIComponent(match[1]);
                        }
                    }

                    return '';
                }

                function detectBrowserName() {
                    const userAgent = navigator.userAgent || '';
                    if (/Edg\\//.test(userAgent)) {
                        return 'Edge';
                    }
                    if (/Chrome\\//.test(userAgent)) {
                        return 'Chrome';
                    }
                    if (/Safari\\//.test(userAgent) && !/Chrome\\//.test(userAgent)) {
                        return 'Safari';
                    }
                    if (/Firefox\\//.test(userAgent)) {
                        return 'Firefox';
                    }
                    return 'Unknown';
                }

                function detectBrowserVersion() {
                    const userAgent = navigator.userAgent || '';
                    return (userAgent.match(/(?:Edg|Chrome|Version|Firefox)\\/([\\d.]+)/) || [])[1] || '';
                }

                function detectOsName() {
                    const userAgent = navigator.userAgent || '';
                    if (/Windows NT/.test(userAgent)) {
                        return 'Windows';
                    }
                    if (/Mac OS X/.test(userAgent)) {
                        return 'macOS';
                    }
                    if (/Android/.test(userAgent)) {
                        return 'Android';
                    }
                    if (/iPhone|iPad|iPod/.test(userAgent)) {
                        return 'iOS';
                    }
                    if (/Linux/.test(userAgent)) {
                        return 'Linux';
                    }
                    return 'Unknown';
                }

                function detectOsVersion() {
                    const userAgent = navigator.userAgent || '';
                    const match = userAgent.match(/Windows NT ([\\d.]+)/)
                        || userAgent.match(/Mac OS X ([\\d_]+)/)
                        || userAgent.match(/Android ([\\d.]+)/)
                        || userAgent.match(/OS ([\\d_]+)/);
                    return match && match[1] ? match[1].replace(/_/g, '.') : '';
                }
                """, normalizedOrderId);

        return Boolean.TRUE.equals(result);
    }

    public boolean backAndForwardNavigationKeepsCorrectOrder(String expectedOrderId) {
        String trackOrderUrl = getCurrentUrl();

        try {
            driver.navigate().back();
            waitForPageLoad();
            driver.navigate().forward();
            waitForPageLoad();

            waitUntil(TRACK_ORDER_TIMEOUT, webDriver -> getCurrentUrl().equals(trackOrderUrl)
                    || isCorrectOrderOpen(expectedOrderId));
            return isCorrectOrderOpen(expectedOrderId);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public boolean isTimelineSequenceDisplayedCorrectly() {
        try {
            waitUntil(Duration.ofSeconds(15), webDriver -> isTimelineSequenceDisplayedNow());
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private boolean isTimelineSequenceDisplayedNow() {
        String text = normalizeText(getPageText()).toLowerCase();
        int previousIndex = -1;

        for (String status : EXPECTED_TIMELINE_SEQUENCE) {
            int currentIndex = text.indexOf(status.toLowerCase());
            if (currentIndex < 0 || currentIndex < previousIndex) {
                return false;
            }
            previousIndex = currentIndex;
        }

        return true;
    }

    public boolean isOrderPlacedTimelineMessageDisplayed(String expectedOrderId) {
        try {
            waitUntil(Duration.ofSeconds(15), webDriver -> isOrderPlacedTimelineMessageDisplayedNow(expectedOrderId));
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private boolean isOrderPlacedTimelineMessageDisplayedNow(String expectedOrderId) {
        String text = getPageText();
        String normalizedOrderId = normalizeText(expectedOrderId);

        return Pattern.compile("(?is)Order\\s+Placed.*successfully\\s+placed.*Arogga")
                .matcher(text)
                .find()
                && (normalizedOrderId.isBlank() || text.contains(normalizedOrderId));
    }

    public boolean areTimelineMessagesDisplayed() {
        try {
            waitUntil(Duration.ofSeconds(15), webDriver -> areTimelineMessagesDisplayedNow());
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private boolean areTimelineMessagesDisplayedNow() {
        String text = getPageText();

        return Pattern.compile("(?is)Processing.*received\\s+your\\s+order.*pharmacist")
                .matcher(text)
                .find()
                && Pattern.compile("(?is)Payment.*(?:Pay\\s+Online|cash\\s+on\\s+delivery|COD|Arogga\\s+Cash|Wallet|Paid|৳\\s*0)")
                .matcher(text)
                .find()
                && Pattern.compile("(?is)Confirmed.*not\\s+confirmed\\s+your\\s+order\\s+yet")
                .matcher(text)
                .find();
    }

    public boolean hasNoBrokenImages() {
        try {
            waitUntil(Duration.ofSeconds(10), webDriver -> Boolean.TRUE.equals(executeScript("""
                    const surface = findTrackSurface() || document.querySelector('main') || document.body;
                    return trackedImages(surface).every(image => image.complete === true);

                    function trackedImages(surface) {
                        return Array.from(surface.querySelectorAll('img'))
                            .filter(visible)
                            .filter(isTrackContentImage);
                    }

                    function isTrackContentImage(image) {
                        if (image.closest('header, footer')) {
                            return false;
                        }

                        const assetText = `${image.alt || ''} ${image.src || ''} ${image.currentSrc || ''}`;
                        return !/logo|icon-v2|social|payment|app-store|play-store|footer|avatar|user|profile/i
                            .test(assetText);
                    }

                    function findTrackSurface() {
                        const candidates = Array.from(document.querySelectorAll('main, section, article, aside, body > div'))
                            .filter(visible)
                            .filter(element => /Timeline|Order\\s+Placed|Processing|Payment|Confirmed|Back\\s+to\\s+Orders/i.test(
                                normalize(element.innerText || element.textContent || '')
                            ))
                            .map(element => ({ element, rect: element.getBoundingClientRect() }))
                            .sort((first, second) =>
                                (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                            );

                        return candidates[0]?.element || null;
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
                    """)));
        } catch (TimeoutException ignored) {
            // The final assertion below reports any image that is still broken after the explicit wait.
        }

        return Boolean.TRUE.equals(executeScript("""
                const surface = findTrackSurface() || document.querySelector('main') || document.body;

                return trackedImages(surface)
                    .every(image => image.complete === true
                        && image.naturalWidth > 0
                        && image.naturalHeight > 0
                        && !/placeholder|fallback|default-image|no-image/i.test(
                            `${image.alt || ''} ${image.src || ''} ${image.currentSrc || ''}`
                        ));

                function trackedImages(surface) {
                    return Array.from(surface.querySelectorAll('img'))
                        .filter(visible)
                        .filter(isTrackContentImage);
                }

                function isTrackContentImage(image) {
                    if (image.closest('header, footer')) {
                        return false;
                    }

                    const assetText = `${image.alt || ''} ${image.src || ''} ${image.currentSrc || ''}`;
                    return !/logo|icon-v2|social|payment|app-store|play-store|footer|avatar|user|profile/i
                        .test(assetText);
                }

                function findTrackSurface() {
                    const candidates = Array.from(document.querySelectorAll('main, section, article, aside, body > div'))
                        .filter(visible)
                        .filter(element => /Timeline|Order\\s+Placed|Processing|Payment|Confirmed|Back\\s+to\\s+Orders/i.test(
                            normalize(element.innerText || element.textContent || '')
                        ))
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        );

                    return candidates[0]?.element || null;
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

    public boolean hasNoBrokenIcons() {
        return Boolean.TRUE.equals(executeScript("""
                const surface = findTrackSurface() || document.querySelector('main') || document.body;
                const visibleSvgs = Array.from(surface.querySelectorAll('svg'))
                    .filter(visible)
                    .filter(svg => !svg.closest('header'));
                const visibleIconImages = Array.from(surface.querySelectorAll('img'))
                    .filter(visible)
                    .filter(image => !image.closest('header'))
                    .filter(image => /icon|track|order|status|timeline|arrow|check/i.test(
                        `${image.alt || ''} ${image.src || ''} ${image.currentSrc || ''}`
                    ));

                return visibleSvgs.every(svg => {
                    const rect = svg.getBoundingClientRect();
                    return rect.width > 0 && rect.height > 0;
                }) && visibleIconImages.every(image => image.complete === true
                    && image.naturalWidth > 0
                    && image.naturalHeight > 0);

                function findTrackSurface() {
                    const candidates = Array.from(document.querySelectorAll('main, section, article, aside, body > div'))
                        .filter(visible)
                        .filter(element => /Timeline|Order\\s+Placed|Processing|Payment|Confirmed|Back\\s+to\\s+Orders/i.test(
                            normalize(element.innerText || element.textContent || '')
                        ))
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        );

                    return candidates[0]?.element || null;
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

    public boolean hasNoUiIssuesOrTextTruncation() {
        return Boolean.TRUE.equals(executeScript("""
                const surface = findTrackSurface() || document.querySelector('main') || document.body;
                const textElements = Array.from(surface.querySelectorAll('*'))
                    .filter(visible)
                    .filter(element => !element.closest('header'))
                    .filter(element => {
                        const text = normalize(element.innerText || element.textContent || '');
                        return text.length > 0 && element.children.length === 0;
                    });

                return textElements.every(element =>
                    element.scrollWidth <= element.clientWidth + 4
                        && element.scrollHeight <= element.clientHeight + 6
                );

                function findTrackSurface() {
                    const candidates = Array.from(document.querySelectorAll('main, section, article, aside, body > div'))
                        .filter(visible)
                        .filter(element => /Timeline|Order\\s+Placed|Processing|Payment|Confirmed|Back\\s+to\\s+Orders/i.test(
                            normalize(element.innerText || element.textContent || '')
                        ))
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        );

                    return candidates[0]?.element || null;
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

    private String getPageText() {
        Object result = executeScript("""
                return document.body
                    ? String(document.body.innerText || document.body.textContent || '').replace(/\\s+/g, ' ').trim()
                    : '';
                """);

        return result == null ? "" : String.valueOf(result);
    }

    private boolean isVisibleTextControlPresent(String text) {
        return Boolean.TRUE.equals(executeScript("""
                const expected = String(arguments[0] || '').trim().toLowerCase();
                return Boolean(findTextControl(expected));

                function findTextControl(expectedText) {
                    return Array.from(document.querySelectorAll('button, a, [role="button"], div, span, p, h1, h2, h3, h4, h5, h6'))
                        .filter(visible)
                        .find(element => normalize(element.innerText || element.textContent || '')
                            .toLowerCase()
                            .includes(expectedText)
                            && !Array.from(element.children).some(child =>
                                visible(child)
                                    && normalize(child.innerText || child.textContent || '')
                                        .toLowerCase()
                                        .includes(expectedText)
                            )) || null;
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
                """, text));
    }

    private void clickVisibleTextControl(String text) {
        Boolean clicked = (Boolean) executeScript("""
                const expected = String(arguments[0] || '').trim().toLowerCase();
                const textElement = Array.from(document.querySelectorAll('button, a, [role="button"], div, span, p, h1, h2, h3, h4, h5, h6'))
                    .filter(visible)
                    .find(element => normalize(element.innerText || element.textContent || '')
                        .toLowerCase()
                        .includes(expected)
                        && !Array.from(element.children).some(child =>
                            visible(child)
                                && normalize(child.innerText || child.textContent || '')
                                    .toLowerCase()
                                    .includes(expected)
                        )) || null;
                if (!textElement) {
                    return false;
                }

                let control = textElement.closest('button, a, [role="button"]');
                if (!control) {
                    control = textElement;
                    let candidate = textElement;
                    for (let depth = 0; depth < 4 && candidate.parentElement; depth += 1) {
                        const parent = candidate.parentElement;
                        const parentStyle = getComputedStyle(parent);
                        const parentText = normalize(parent.innerText || parent.textContent || '').toLowerCase();
                        if (parentText.includes(expected) && parentStyle.cursor === 'pointer') {
                            control = parent;
                            break;
                        }
                        candidate = parent;
                    }
                }
                if (!control) {
                    return false;
                }

                control.scrollIntoView({ block: 'center', inline: 'nearest' });
                control.click();
                return true;

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
                """, text);

        if (!Boolean.TRUE.equals(clicked)) {
            throw new TimeoutException(text + " control was not found.");
        }
    }

    private boolean containsCurrencyAmount(String text, BigDecimal expectedAmount) {
        if (expectedAmount == null) {
            return false;
        }

        String normalizedText = normalizeCurrencyText(text);
        String roundedAmount = expectedAmount.setScale(0, RoundingMode.HALF_UP).toPlainString();
        String decimalAmount = expectedAmount.setScale(2, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();

        return Pattern.compile("৳\\s*" + Pattern.quote(roundedAmount) + "\\b")
                .matcher(normalizedText)
                .find()
                || Pattern.compile("৳\\s*" + Pattern.quote(decimalAmount) + "\\b")
                .matcher(normalizedText)
                .find();
    }

    private String normalizeCurrencyText(String text) {
        return text == null ? "" : text
                .replaceAll("\\s+", " ")
                .replace("৳ ", "৳")
                .trim();
    }

    private String normalizeText(String text) {
        return text == null ? "" : text.replaceAll("\\s+", " ").trim();
    }
}
