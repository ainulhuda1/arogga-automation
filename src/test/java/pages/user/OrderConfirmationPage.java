package pages.user;

import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WindowType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OrderConfirmationPage extends BasePage {

    private static final Duration CONFIRMATION_TIMEOUT = Duration.ofSeconds(40);

    public OrderConfirmationPage(WebDriver driver) {
        super(driver);
    }

    public OrderConfirmationPage waitUntilLoaded() {
        waitForPageLoad();
        waitUntil(CONFIRMATION_TIMEOUT, webDriver -> isOrderConfirmationPageLoaded());
        return this;
    }

    public boolean waitUntilLoadedIfPresent() {
        try {
            waitForPageLoad();
            waitUntil(Duration.ofSeconds(10), webDriver -> isOrderConfirmationPageLoaded());
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public boolean isOrderConfirmationPageLoaded() {
        String text = getPageText().toLowerCase();

        return !getOrderId().isBlank()
                && (text.matches("(?is).*order.*success.*")
                || text.matches("(?is).*successfully\\s+placed.*")
                || text.contains("track order"));
    }

    public boolean isOrderConfirmationPageLoaded(String expectedOrderId) {
        return expectedOrderId != null
                && !expectedOrderId.isBlank()
                && expectedOrderId.equals(getOrderId())
                && isOrderConfirmationPageLoaded();
    }

    public boolean isCorrectConfirmationRouteLoaded() {
        String url = getCurrentUrl().toLowerCase();

        return isOrderConfirmationPageLoaded()
                || (url.contains("order")
                || url.contains("confirm")
                || url.contains("success")
                || url.contains("checkout"))
                && (url.contains("confirm")
                || url.contains("success")
                || url.matches(".*orders?[/#?].*")
                || isOrderConfirmationPageLoaded());
    }

    public String getOrderId() {
        return getOrderNumber();
    }

    public String getOrderNumber() {
        return extractOrderId(getPageText());
    }

    public boolean isOrderSuccessMessageDisplayed() {
        String text = getPageText();

        return Pattern.compile("(?is)(order\\s+placed\\s+successfully|order\\s+successful|order\\s+success|successfully\\s+placed|your\\s+order\\s+is\\s+successfully\\s+placed)")
                .matcher(text)
                .find();
    }

    public boolean isPaymentMethodCashOnDeliveryDisplayed() {
        return Pattern.compile("(?i)(Cash\\s+on\\s+Delivery|\\bCOD\\b)")
                .matcher(getPageText())
                .find();
    }

    public boolean isGeneratedOrderIdDisplayed() {
        return !getOrderId().isBlank();
    }

    public boolean isViewDetailsOptionVisible() {
        return isVisibleTextControlPresent("View Details");
    }

    public OrderConfirmationPage openViewDetailsDrawer() {
        if (!isViewDetailsDrawerOpen()) {
            clickVisibleTextControl("View Details");
        }

        waitUntil(CONFIRMATION_TIMEOUT, webDriver -> isViewDetailsDrawerOpen());
        return this;
    }

    public boolean isViewDetailsDrawerOpen() {
        return Boolean.TRUE.equals(executeScript("""
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                const orderId = String(arguments[0] || '').trim();

                return Boolean(activeDetailsSurface(orderId));

                function activeDetailsSurface(expectedOrderId) {
                    return Array.from(document.querySelectorAll('body > div, [role="dialog"], aside'))
                        .filter(visible)
                        .filter(element => {
                            const text = normalize(element.innerText || element.textContent || '');
                            return /View\\s+Details|Order\\s+Details|Total\\s+Order\\s+Amount|Estimated\\s+Delivery/i.test(text)
                                && (!expectedOrderId || text.includes(expectedOrderId));
                        })
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        )
                        .map(candidate => candidate.element)[0] || null;
                }

                function visible(element) {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                }
                """, getOrderId()));
    }

    public String getOrderIdFromDetailsDrawer() {
        Object result = executeScript("""
                const orderId = String(arguments[0] || '').trim();
                const surface = activeDetailsSurface(orderId);
                return surface ? normalize(surface.innerText || surface.textContent || '') : '';

                function activeDetailsSurface(expectedOrderId) {
                    return Array.from(document.querySelectorAll('body > div, [role="dialog"], aside'))
                        .filter(visible)
                        .filter(element => {
                            const text = normalize(element.innerText || element.textContent || '');
                            return /View\\s+Details|Order\\s+Details|Total\\s+Order\\s+Amount|Estimated\\s+Delivery/i.test(text)
                                && (!expectedOrderId || text.includes(expectedOrderId));
                        })
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        )
                        .map(candidate => candidate.element)[0] || null;
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
                """, getOrderId());

        return extractOrderId(result == null ? "" : String.valueOf(result));
    }

    public boolean doesDetailsDrawerOrderIdMatchConfirmationPage() {
        String confirmationOrderId = getOrderId();
        String drawerOrderId = getOrderIdFromDetailsDrawer();

        return !confirmationOrderId.isBlank()
                && confirmationOrderId.equals(drawerOrderId);
    }

    public boolean isTotalOrderAmountDisplayedCorrectly(BigDecimal expectedAmountPayable) {
        String drawerText = getDetailsDrawerText();

        return Pattern.compile("(?is)(Total\\s+Order\\s+Amount|Order\\s+Amount|Total).*৳\\s*"
                        + Pattern.quote(normalizedMoney(expectedAmountPayable)) + "\\b")
                .matcher(normalizeCurrencyText(drawerText))
                .find()
                || containsCurrencyAmount(drawerText, expectedAmountPayable);
    }

    public boolean isEstimatedDeliveryTimeDisplayed() {
        return Pattern.compile("(?is)Estimated\\s+Delivery|Delivery\\s+Time|ETA|Delivered\\s+by")
                .matcher(getDetailsDrawerText())
                .find();
    }

    public boolean isNotificationMessageDisplayed() {
        String text = getDetailsDrawerText();
        if (text.isBlank()) {
            text = getPageText();
        }

        return Pattern.compile("(?is)(Arogga\\s+App|app).*SMS|SMS.*(Arogga\\s+App|app)")
                .matcher(text)
                .find()
                || (isOrderSuccessMessageDisplayed() && isTrackOrderButtonVisible());
    }

    public boolean isTrackOrderButtonVisible() {
        return isVisibleTextControlPresent("Track Order");
    }

    public TrackOrderPage clickTrackOrder() {
        String orderId = getOrderId();
        clickVisibleTextControl("Track Order");
        return new TrackOrderPage(driver).waitUntilLoaded(orderId);
    }

    public boolean refreshKeepsSameOrder(String expectedOrderId) {
        try {
            refreshPage();
            waitUntil(Duration.ofSeconds(10), webDriver -> isOrderConfirmationPageLoaded());
        } catch (RuntimeException exception) {
            return false;
        }

        return expectedOrderId != null
                && !expectedOrderId.isBlank()
                && expectedOrderId.equals(getOrderId());
    }

    public boolean refreshKeepsSameOrderInIsolatedTab(String expectedOrderId) {
        String originalWindow = driver.getWindowHandle();
        String confirmationUrl = getCurrentUrl();
        String isolatedWindow = null;

        try {
            driver.switchTo().newWindow(WindowType.TAB);
            isolatedWindow = driver.getWindowHandle();
            driver.get(confirmationUrl);
            waitForPageLoad();
            boolean loadedBeforeRefresh = waitUntilLoadedIfPresent();
            boolean stableAfterRefresh = refreshKeepsSameOrder(expectedOrderId);

            return loadedBeforeRefresh && stableAfterRefresh;
        } catch (RuntimeException exception) {
            return false;
        } finally {
            if (isolatedWindow != null) {
                try {
                    driver.switchTo().window(isolatedWindow);
                    driver.close();
                } catch (RuntimeException ignored) {
                    // The tab may already be closed if the browser aborted navigation.
                }
            }
            driver.switchTo().window(originalWindow);
            waitForPageLoad();
        }
    }

    public boolean browserBackForwardKeepsSameOrder(String expectedOrderId) {
        String originalWindow = driver.getWindowHandle();
        String confirmationUrl = getCurrentUrl();
        String isolatedWindow = null;

        try {
            driver.switchTo().newWindow(WindowType.TAB);
            isolatedWindow = driver.getWindowHandle();
            driver.get(confirmationUrl);
            waitForPageLoad();
            if (!waitUntilLoadedIfPresent()) {
                return false;
            }

            driver.navigate().back();
            waitForPageLoad();
            driver.navigate().forward();
            waitForPageLoad();
            waitUntil(Duration.ofSeconds(10), webDriver -> isOrderConfirmationPageLoaded());

            return expectedOrderId != null
                    && !expectedOrderId.isBlank()
                    && expectedOrderId.equals(getOrderId());
        } catch (RuntimeException exception) {
            return false;
        } finally {
            if (isolatedWindow != null) {
                try {
                    driver.switchTo().window(isolatedWindow);
                    driver.close();
                } catch (RuntimeException ignored) {
                    // The tab may already be closed if navigation failed.
                }
            }
            driver.switchTo().window(originalWindow);
            waitForPageLoad();
        }
    }

    public boolean hasNoBrokenImages() {
        return Boolean.TRUE.equals(executeScript("""
                const surface = findOrderSurface() || document.querySelector('main') || document.body;

                return Array.from(surface.querySelectorAll('img'))
                    .filter(visible)
                    .filter(image => !image.closest('header'))
                    .every(image => image.complete === true
                        && image.naturalWidth > 0
                        && image.naturalHeight > 0
                        && !/placeholder|fallback|default-image|no-image/i.test(
                            `${image.alt || ''} ${image.src || ''} ${image.currentSrc || ''}`
                        ));

                function findOrderSurface() {
                    const candidates = Array.from(document.querySelectorAll('main, section, article, aside, [role="dialog"], body > div'))
                        .filter(visible)
                        .filter(element => /Order\\s+(Placed|Details|ID)|Track\\s+Order|Payment\\s+Details|Total\\s+Order\\s+Amount/i.test(
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
                const surface = findOrderSurface() || document.querySelector('main') || document.body;
                const visibleSvgs = Array.from(surface.querySelectorAll('svg'))
                    .filter(visible)
                    .filter(svg => !svg.closest('header'));
                const visibleIconImages = Array.from(surface.querySelectorAll('img'))
                    .filter(visible)
                    .filter(image => !image.closest('header'))
                    .filter(image => /icon|track|order|success|close|arrow|info/i.test(
                        `${image.alt || ''} ${image.src || ''} ${image.currentSrc || ''}`
                    ));

                return visibleSvgs.every(svg => {
                    const rect = svg.getBoundingClientRect();
                    return rect.width > 0 && rect.height > 0;
                }) && visibleIconImages.every(image => image.complete === true
                    && image.naturalWidth > 0
                    && image.naturalHeight > 0);

                function findOrderSurface() {
                    const candidates = Array.from(document.querySelectorAll('main, section, article, aside, [role="dialog"], body > div'))
                        .filter(visible)
                        .filter(element => /Order\\s+(Placed|Details|ID)|Track\\s+Order|Payment\\s+Details|Total\\s+Order\\s+Amount/i.test(
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

    public boolean hasNoLayoutIssuesOrTextTruncation() {
        return Boolean.TRUE.equals(executeScript("""
                const surface = findOrderSurface() || document.querySelector('main') || document.body;
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

                function findOrderSurface() {
                    const candidates = Array.from(document.querySelectorAll('main, section, article, aside, [role="dialog"], body > div'))
                        .filter(visible)
                        .filter(element => /Order\\s+(Placed|Details|ID)|Track\\s+Order|Payment\\s+Details|Total\\s+Order\\s+Amount/i.test(
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

    String getPageText() {
        Object result = executeScript("""
                return document.body
                    ? String(document.body.innerText || document.body.textContent || '').replace(/\\s+/g, ' ').trim()
                    : '';
                """);

        return result == null ? "" : String.valueOf(result);
    }

    private String getDetailsDrawerText() {
        Object result = executeScript("""
                const orderId = String(arguments[0] || '').trim();
                const surface = activeDetailsSurface(orderId);
                return surface ? normalize(surface.innerText || surface.textContent || '') : '';

                function activeDetailsSurface(expectedOrderId) {
                    return Array.from(document.querySelectorAll('body > div, [role="dialog"], aside'))
                        .filter(visible)
                        .filter(element => {
                            const text = normalize(element.innerText || element.textContent || '');
                            return /View\\s+Details|Order\\s+Details|Total\\s+Order\\s+Amount|Estimated\\s+Delivery/i.test(text)
                                && (!expectedOrderId || text.includes(expectedOrderId));
                        })
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        )
                        .map(candidate => candidate.element)[0] || null;
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
                """, getOrderId());

        return result == null ? "" : String.valueOf(result);
    }

    private boolean isVisibleTextControlPresent(String text) {
        return Boolean.TRUE.equals(executeScript("""
                const expected = String(arguments[0] || '').trim().toLowerCase();
                return Boolean(findTextControl(expected));

                function findTextControl(expectedText) {
                    return Array.from(document.querySelectorAll('button, a, [role="button"]'))
                        .filter(visible)
                        .find(element => normalize(element.innerText || element.textContent || '')
                            .toLowerCase()
                            .includes(expectedText)) || null;
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
                        && style.pointerEvents !== 'none'
                        && Number(style.opacity || 1) !== 0;
                }
                """, text));
    }

    private void clickVisibleTextControl(String text) {
        Boolean clicked = (Boolean) executeScript("""
                const expected = String(arguments[0] || '').trim().toLowerCase();
                const control = Array.from(document.querySelectorAll('button, a, [role="button"]'))
                    .filter(visible)
                    .find(element => normalize(element.innerText || element.textContent || '')
                        .toLowerCase()
                        .includes(expected)) || null;
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
                        && style.pointerEvents !== 'none'
                        && Number(style.opacity || 1) !== 0;
                }
                """, text);

        if (!Boolean.TRUE.equals(clicked)) {
            throw new TimeoutException(text + " control was not found.");
        }
    }

    private String extractOrderId(String text) {
        String normalizedText = normalizeText(text);
        Pattern[] patterns = {
                Pattern.compile("(?i)Order\\s*(?:ID|Id|id)\\s*[:#]?\\s*([A-Z0-9-]{4,})"),
                Pattern.compile("(?i)Order\\s*#\\s*([A-Z0-9-]{4,})"),
                Pattern.compile("(?i)#\\s*([0-9]{4,})")
        };

        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(normalizedText);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        }

        return "";
    }

    private boolean containsCurrencyAmount(String text, BigDecimal expectedAmount) {
        if (expectedAmount == null) {
            return false;
        }

        String normalizedText = normalizeCurrencyText(text);
        String roundedAmount = normalizedMoney(expectedAmount);
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

    private String normalizedMoney(BigDecimal amount) {
        if (amount == null) {
            return "";
        }

        return amount.setScale(0, RoundingMode.HALF_UP).toPlainString();
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
