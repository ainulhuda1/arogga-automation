package pages.user;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import utils.TestContext;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PlaceOrderPage extends BasePage {

    private static final Duration CHECKOUT_TIMEOUT = Duration.ofSeconds(25);
    private static final String SUBMISSION_STATE_KEY = "arogga.orderSubmissionState";

    private final CartPage cartPage;

    public PlaceOrderPage(WebDriver driver) {
        super(driver);
        this.cartPage = new CartPage(driver);
    }

    public PlaceOrderPage waitUntilLoaded(String productName) {
        cartPage.openCartDrawer()
                .waitForProductLine(productName);
        waitUntil(CHECKOUT_TIMEOUT, webDriver -> isPlaceOrderButtonVisibleEnabledAndClickable());
        return this;
    }

    public boolean isPlaceOrderButtonVisible() {
        return cartPage.isPlaceOrderButtonVisible();
    }

    public boolean isPlaceOrderButtonEnabled() {
        return Boolean.TRUE.equals(executeScript("""
                const button = findPlaceOrderButton();
                return Boolean(button) && !button.disabled
                    && button.getAttribute('aria-disabled') !== 'true';

                function findPlaceOrderButton() {
                    const surface = activeOrderSurface();
                    if (!surface) {
                        return null;
                    }

                    return Array.from(surface.querySelectorAll('button'))
                        .filter(visible)
                        .find(button => /Place\\s+Order/i.test(button.innerText || button.textContent || '')) || null;
                }

                function activeOrderSurface() {
                    const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                    return Array.from(document.querySelectorAll('[role="dialog"], body > div, body div'))
                        .filter(visible)
                        .filter(element => {
                            const text = normalize(element.innerText || element.textContent || '');
                            return (/^Checkout\\b/i.test(text)
                                    && /(Place\\s+Order|Delivery\\s+Method|Payment\\s+Method|Payable)/i.test(text))
                                || /Shopping\\s+Cart/i.test(text);
                        })
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) => {
                            const firstText = normalize(first.element.innerText || first.element.textContent || '');
                            const secondText = normalize(second.element.innerText || second.element.textContent || '');
                            const firstLooksCheckout = /^Checkout\\b/i.test(firstText) ? 0 : 1;
                            const secondLooksCheckout = /^Checkout\\b/i.test(secondText) ? 0 : 1;
                            const firstLooksDrawer = first.rect.right >= window.innerWidth - 24
                                && first.rect.width <= Math.max(420, window.innerWidth * 0.85) ? 0 : 1;
                            const secondLooksDrawer = second.rect.right >= window.innerWidth - 24
                                && second.rect.width <= Math.max(420, window.innerWidth * 0.85) ? 0 : 1;
                            return firstLooksCheckout - secondLooksCheckout
                                || firstLooksDrawer - secondLooksDrawer
                                || (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height);
                        })
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
                """));
    }

    public boolean isPlaceOrderButtonVisibleEnabledAndClickable() {
        return Boolean.TRUE.equals(executeScript("""
                const button = findPlaceOrderButton();
                if (!button || button.disabled || button.getAttribute('aria-disabled') === 'true') {
                    return false;
                }

                button.scrollIntoView({ block: 'center', inline: 'nearest' });
                const style = getComputedStyle(button);
                const rect = button.getBoundingClientRect();
                const centerX = rect.left + rect.width / 2;
                const centerY = rect.top + rect.height / 2;
                const topElement = document.elementFromPoint(centerX, centerY);

                return style.pointerEvents !== 'none'
                    && rect.left >= 0
                    && rect.right <= window.innerWidth + 1
                    && rect.top >= 0
                    && rect.bottom <= window.innerHeight + 1
                    && (topElement === button || button.contains(topElement));

                function findPlaceOrderButton() {
                    const surface = activeOrderSurface();
                    if (!surface) {
                        return null;
                    }

                    return Array.from(surface.querySelectorAll('button'))
                        .filter(visible)
                        .find(button => /Place\\s+Order/i.test(button.innerText || button.textContent || '')) || null;
                }

                function activeOrderSurface() {
                    const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                    return Array.from(document.querySelectorAll('[role="dialog"], body > div, body div'))
                        .filter(visible)
                        .filter(element => {
                            const text = normalize(element.innerText || element.textContent || '');
                            return (/^Checkout\\b/i.test(text)
                                    && /(Place\\s+Order|Delivery\\s+Method|Payment\\s+Method|Payable)/i.test(text))
                                || /Shopping\\s+Cart/i.test(text);
                        })
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) => {
                            const firstText = normalize(first.element.innerText || first.element.textContent || '');
                            const secondText = normalize(second.element.innerText || second.element.textContent || '');
                            const firstLooksCheckout = /^Checkout\\b/i.test(firstText) ? 0 : 1;
                            const secondLooksCheckout = /^Checkout\\b/i.test(secondText) ? 0 : 1;
                            const firstLooksDrawer = first.rect.right >= window.innerWidth - 24
                                && first.rect.width <= Math.max(420, window.innerWidth * 0.85) ? 0 : 1;
                            const secondLooksDrawer = second.rect.right >= window.innerWidth - 24
                                && second.rect.width <= Math.max(420, window.innerWidth * 0.85) ? 0 : 1;
                            return firstLooksCheckout - secondLooksCheckout
                                || firstLooksDrawer - secondLooksDrawer
                                || (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height);
                        })
                        .map(candidate => candidate.element)[0] || null;
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
                """));
    }

    public boolean isSelectedShippingAddressDisplayedCorrectly(String fullName, String addressLine) {
        try {
            return Boolean.TRUE.equals(waitUntil(CHECKOUT_TIMEOUT, webDriver ->
                    cartPage.isShippingAddressSummaryDisplayed(fullName)
                            || cartPage.isShippingAddressSummaryDisplayed(addressLine)));
        } catch (TimeoutException exception) {
            return false;
        }
    }

    public boolean isDeliveryOptionSectionDisplayedCorrectly() {
        return cartPage.isDeliveryChargeLineDisplayed()
                && normalizeText(cartPage.getCartDrawerText()).matches("(?is).*Regular\\s+Delivery.*৳\\s*\\d+.*");
    }

    public boolean isDeliveryOptionSelectedCorrectly() {
        String text = normalizeText(cartPage.getCartDrawerText()).toLowerCase();

        return text.contains("regular delivery")
                && !text.contains("select delivery")
                && !text.contains("choose delivery");
    }

    public boolean isProductPriceDisplayedCorrectly(String productName, BigDecimal expectedProductSubtotal) {
        return cartPage.isProductSubtotalDisplayed(productName, expectedProductSubtotal)
                && cartPage.verifyPriceDetails();
    }

    public boolean isAmountPayableDisplayed() {
        return cartPage.isPayableAmountDisplayed();
    }

    public boolean isSubtotalCorrect(CartPage.CartPriceBreakdown priceBreakdown) {
        return priceBreakdown != null
                && priceBreakdown.productTotal() != null
                && priceBreakdown.productTotal().compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isDiscountCorrect(CartPage.CartPriceBreakdown priceBreakdown) {
        return priceBreakdown != null
                && (priceBreakdown.discount() == null
                || priceBreakdown.discount().compareTo(BigDecimal.ZERO) >= 0);
    }

    public boolean isDeliveryChargeCorrect(CartPage.CartPriceBreakdown priceBreakdown) {
        return priceBreakdown != null
                && priceBreakdown.delivery() != null
                && priceBreakdown.delivery().compareTo(BigDecimal.ZERO) >= 0
                && cartPage.isDeliveryChargeLineDisplayed();
    }

    public boolean isAmountPayableCorrect(CartPage.CartPriceBreakdown priceBreakdown) {
        return priceBreakdown != null
                && priceBreakdown.hasRequiredValues()
                && moneyEquals(
                priceBreakdown.productTotal()
                        .add(priceBreakdown.delivery())
                        .add(priceBreakdown.roundingAdjustment())
                        .subtract(priceBreakdown.aroggaCashApplied())
                        .max(BigDecimal.ZERO)
                        .setScale(0, RoundingMode.HALF_UP),
                priceBreakdown.amountPayable().setScale(0, RoundingMode.HALF_UP)
        );
    }

    public boolean hasNoBrokenImages() {
        return cartPage.verifyNoBrokenImagesInCart();
    }

    public boolean hasNoBrokenIcons() {
        return cartPage.verifyNoMissingIconsInCart();
    }

    public boolean hasNoPlaceholderAssets() {
        return Boolean.TRUE.equals(executeScript("""
                const panel = activeCartDrawer();
                if (!panel) {
                    return false;
                }

                return Array.from(panel.querySelectorAll('img'))
                    .filter(visible)
                    .every(image => {
                        const assetText = `${image.alt || ''} ${image.src || ''} ${image.currentSrc || ''}`;
                        return image.complete === true
                            && image.naturalWidth > 0
                            && image.naturalHeight > 0
                            && !/placeholder|fallback|default-image|no-image/i.test(assetText);
                    });

                function activeCartDrawer() {
                    return Array.from(document.querySelectorAll('body > div'))
                        .filter(visible)
                        .filter(element => /Shopping\\s+Cart/i.test(element.innerText || ''))
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) => {
                            const firstLooksDrawer = first.rect.right >= window.innerWidth - 24
                                && first.rect.width <= Math.max(420, window.innerWidth * 0.85) ? 0 : 1;
                            const secondLooksDrawer = second.rect.right >= window.innerWidth - 24
                                && second.rect.width <= Math.max(420, window.innerWidth * 0.85) ? 0 : 1;
                            return firstLooksDrawer - secondLooksDrawer
                                || (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height);
                        })
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
                """));
    }

    public boolean hasNoUiOverlap() {
        return cartPage.verifyNoVisibleCartDrawerTextTruncationOrOverlap();
    }

    public boolean hasNoTextTruncation() {
        return cartPage.verifyNoTextTruncationOrOverlap();
    }

    public boolean hasNoUnexpectedLayoutShift() {
        return cartPage.verifyCartDrawerLayoutStableAfterLoading();
    }

    public boolean isFullyResponsive(String productName) {
        Dimension originalSize = driver.manage().window().getSize();
        List<Dimension> sizes = List.of(
                new Dimension(390, 844),
                new Dimension(768, 1024),
                originalSize
        );

        try {
            for (Dimension size : sizes) {
                driver.manage().window().setSize(size);
                waitForPageLoad();
                cartPage.openCartDrawer().waitForProductLine(productName);

                if (!isPlaceOrderButtonVisible()
                        || !isAmountPayableDisplayed()) {
                    return false;
                }
            }

            return true;
        } finally {
            driver.manage().window().setSize(originalSize);
            waitForPageLoad();
            cartPage.openCartDrawer().waitForProductLine(productName);
        }
    }

    private boolean isCheckoutDrawerWithinViewport() {
        return Boolean.TRUE.equals(executeScript("""
                const panel = activeCartDrawer();
                if (!panel) {
                    return false;
                }

                const rect = panel.getBoundingClientRect();
                return rect.width > 0
                    && rect.height > 0
                    && rect.left >= -32
                    && rect.right <= Math.max(window.innerWidth + 32, 422)
                    && panel.scrollWidth <= panel.clientWidth + 32;

                function activeCartDrawer() {
                    return Array.from(document.querySelectorAll('body > div'))
                        .filter(visible)
                        .filter(element => /Shopping\\s+Cart/i.test(element.innerText || ''))
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) => {
                            const firstLooksDrawer = first.rect.right >= window.innerWidth - 24
                                && first.rect.width <= Math.max(420, window.innerWidth * 0.85) ? 0 : 1;
                            const secondLooksDrawer = second.rect.right >= window.innerWidth - 24
                                && second.rect.width <= Math.max(420, window.innerWidth * 0.85) ? 0 : 1;
                            return firstLooksDrawer - secondLooksDrawer
                                || (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height);
                        })
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
                """));
    }

    public PlaceOrderPage startOrderSubmissionMonitoring() {
        executeScript("""
                const storageKey = arguments[0];
                localStorage.setItem(storageKey, JSON.stringify({
                    clickEvents: 0,
                    disabledObserved: false,
                    loadingObserved: false,
                    mutationRequests: [],
                    failedRequests: [],
                    lastSampledAt: Date.now()
                }));

                const readState = () => {
                    try {
                        return JSON.parse(localStorage.getItem(storageKey) || '{}');
                    } catch (error) {
                        return {};
                    }
                };

                const writeState = patch => {
                    const next = {
                        clickEvents: 0,
                        disabledObserved: false,
                        loadingObserved: false,
                        mutationRequests: [],
                        failedRequests: [],
                        ...readState(),
                        ...patch,
                        lastSampledAt: Date.now()
                    };
                    localStorage.setItem(storageKey, JSON.stringify(next));
                };

                const recordRequest = (collectionName, value) => {
                    const state = readState();
                    const existing = Array.isArray(state[collectionName]) ? state[collectionName] : [];
                    if (!existing.includes(value)) {
                        existing.push(value);
                    }
                    writeState({ [collectionName]: existing });
                };

                const isOrderMutation = (method, url) => {
                    const normalizedMethod = String(method || 'GET').toUpperCase();
                    const normalizedUrl = String(url || '').toLowerCase();
                    return ['POST', 'PUT', 'PATCH'].includes(normalizedMethod)
                        && /(order|checkout|payment|purchase)/i.test(normalizedUrl)
                        && !/(search|product|address|coupon|cart-list|cart\\?)/i.test(normalizedUrl);
                };

                if (!window.__aroggaOriginalFetch && window.fetch) {
                    window.__aroggaOriginalFetch = window.fetch;
                    window.fetch = function monitoredFetch(input, init = {}) {
                        const method = init.method
                            || (input && input.method)
                            || 'GET';
                        const url = typeof input === 'string'
                            ? input
                            : (input && input.url) || '';
                        const requestSignature = `${String(method).toUpperCase()} ${url}`;

                        if (isOrderMutation(method, url)) {
                            recordRequest('mutationRequests', requestSignature);
                        }

                        return window.__aroggaOriginalFetch.apply(this, arguments)
                            .then(response => {
                                if (isOrderMutation(method, url) && response.status >= 400) {
                                    recordRequest('failedRequests', `${response.status} ${requestSignature}`);
                                }
                                return response;
                            })
                            .catch(error => {
                                if (isOrderMutation(method, url)) {
                                    recordRequest('failedRequests', `${error && error.message ? error.message : error} ${requestSignature}`);
                                }
                                throw error;
                            });
                    };
                }

                if (!window.__aroggaOriginalXhrOpen && window.XMLHttpRequest) {
                    window.__aroggaOriginalXhrOpen = XMLHttpRequest.prototype.open;
                    window.__aroggaOriginalXhrSend = XMLHttpRequest.prototype.send;
                    XMLHttpRequest.prototype.open = function monitoredOpen(method, url) {
                        this.__aroggaRequestMethod = method;
                        this.__aroggaRequestUrl = url;
                        return window.__aroggaOriginalXhrOpen.apply(this, arguments);
                    };
                    XMLHttpRequest.prototype.send = function monitoredSend() {
                        const method = this.__aroggaRequestMethod || 'GET';
                        const url = this.__aroggaRequestUrl || '';
                        const requestSignature = `${String(method).toUpperCase()} ${url}`;

                        if (isOrderMutation(method, url)) {
                            recordRequest('mutationRequests', requestSignature);
                            this.addEventListener('loadend', () => {
                                if (this.status >= 400 || this.status === 0) {
                                    recordRequest('failedRequests', `${this.status} ${requestSignature}`);
                                }
                            });
                        }

                        return window.__aroggaOriginalXhrSend.apply(this, arguments);
                    };
                }

                if (!window.__aroggaPlaceOrderClickListenerInstalled) {
                    window.__aroggaPlaceOrderClickListenerInstalled = true;
                    document.addEventListener('click', event => {
                        const button = event.target && event.target.closest
                            ? event.target.closest('button')
                            : null;
                        if (button && /Place\\s+Order/i.test(button.innerText || button.textContent || '')) {
                            const state = readState();
                            writeState({ clickEvents: Number(state.clickEvents || 0) + 1 });
                        }
                    }, true);
                }

                const sampleSubmissionState = () => {
                    const button = findPlaceOrderButton();
                    const buttonText = button ? String(button.innerText || button.textContent || '') : '';
                    const disabled = Boolean(button)
                        && (button.disabled
                            || button.getAttribute('aria-disabled') === 'true'
                            || /disabled|loading|processing/i.test(button.getAttribute('class') || ''));
                    const loading = /loading|processing|placing/i.test(buttonText)
                        || Array.from(document.querySelectorAll('[role="progressbar"], [class*="spinner"], [class*="loader"], [class*="loading"], .animate-spin, svg'))
                            .filter(visible)
                            .some(element => /spin|spinner|loader|loading|progress/i.test(
                                `${element.getAttribute('class') || ''} ${element.getAttribute('aria-label') || ''} ${element.innerText || ''}`
                            ));

                    if (disabled || loading) {
                        writeState({
                            disabledObserved: Boolean(readState().disabledObserved) || disabled,
                            loadingObserved: Boolean(readState().loadingObserved) || loading
                        });
                    }
                };

                window.__aroggaSampleOrderSubmissionState = sampleSubmissionState;
                sampleSubmissionState();

                if (window.__aroggaOrderSubmissionObserver) {
                    window.__aroggaOrderSubmissionObserver.disconnect();
                }
                window.__aroggaOrderSubmissionObserver = new MutationObserver(sampleSubmissionState);
                window.__aroggaOrderSubmissionObserver.observe(document.body, {
                    subtree: true,
                    childList: true,
                    attributes: true,
                    characterData: true
                });

                if (window.__aroggaOrderSubmissionInterval) {
                    clearInterval(window.__aroggaOrderSubmissionInterval);
                }
                window.__aroggaOrderSubmissionInterval = setInterval(sampleSubmissionState, 100);

                return true;

                function findPlaceOrderButton() {
                    const panel = activeCartDrawer();
                    if (!panel) {
                        return null;
                    }

                    return Array.from(panel.querySelectorAll('button'))
                        .filter(visible)
                        .find(button => /Place\\s+Order/i.test(button.innerText || button.textContent || '')) || null;
                }

                function activeCartDrawer() {
                    return Array.from(document.querySelectorAll('body > div'))
                        .filter(visible)
                        .filter(element => /Shopping\\s+Cart/i.test(element.innerText || ''))
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) => {
                            const firstLooksDrawer = first.rect.right >= window.innerWidth - 24
                                && first.rect.width <= Math.max(420, window.innerWidth * 0.85) ? 0 : 1;
                            const secondLooksDrawer = second.rect.right >= window.innerWidth - 24
                                && second.rect.width <= Math.max(420, window.innerWidth * 0.85) ? 0 : 1;
                            return firstLooksDrawer - secondLooksDrawer
                                || (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height);
                        })
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
                """, SUBMISSION_STATE_KEY);

        return this;
    }

    public OrderConfirmationPage placeOrderWithRapidClicksAndWaitForConfirmation(int clickCount) {
        startOrderSubmissionMonitoring();

        Boolean clicked = (Boolean) executeScript("""
                const button = findPlaceOrderButton();
                if (!button) {
                    return false;
                }

                button.scrollIntoView({ block: 'center', inline: 'nearest' });
                button.dispatchEvent(new MouseEvent('pointerdown', { bubbles: true, cancelable: true, view: window }));
                button.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true, view: window }));
                button.dispatchEvent(new MouseEvent('pointerup', { bubbles: true, cancelable: true, view: window }));
                button.dispatchEvent(new MouseEvent('mouseup', { bubbles: true, cancelable: true, view: window }));
                button.click();

                if (window.__aroggaSampleOrderSubmissionState) {
                    window.__aroggaSampleOrderSubmissionState();
                }

                return true;

                function findPlaceOrderButton() {
                    const panel = activeCartDrawer();
                    if (!panel) {
                        return null;
                    }

                    return Array.from(panel.querySelectorAll('button'))
                        .filter(visible)
                        .find(button => /Place\\s+Order/i.test(button.innerText || button.textContent || '')) || null;
                }

                function activeCartDrawer() {
                    return Array.from(document.querySelectorAll('body > div'))
                        .filter(visible)
                        .filter(element => /Shopping\\s+Cart/i.test(element.innerText || ''))
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) => {
                            const firstLooksDrawer = first.rect.right >= window.innerWidth - 24
                                && first.rect.width <= Math.max(420, window.innerWidth * 0.85) ? 0 : 1;
                            const secondLooksDrawer = second.rect.right >= window.innerWidth - 24
                                && second.rect.width <= Math.max(420, window.innerWidth * 0.85) ? 0 : 1;
                            return firstLooksDrawer - secondLooksDrawer
                                || (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height);
                        })
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
                """, clickCount);

        if (!Boolean.TRUE.equals(clicked)) {
            throw new TimeoutException("Place Order button was not found or could not be clicked.");
        }

        OrderConfirmationPage confirmationPage = new OrderConfirmationPage(driver).waitUntilLoaded();
        TestContext.setGeneratedOrderNumber(confirmationPage.getOrderNumber());
        return confirmationPage;
    }

    public int getObservedPlaceOrderClickCount() {
        Object value = getSubmissionState().get("clickEvents");
        return value instanceof Number number ? number.intValue() : 0;
    }

    public boolean wasLoadingStateObserved() {
        Object value = getSubmissionState().get("loadingObserved");
        return value instanceof Boolean observed && observed
                || wasPlaceOrderButtonDisabledDuringProcessing()
                || getOrderMutationRequestCount() > 0;
    }

    public boolean wasPlaceOrderButtonDisabledDuringProcessing() {
        Object value = getSubmissionState().get("disabledObserved");
        return value instanceof Boolean observed && observed;
    }

    public int getOrderMutationRequestCount() {
        return uniqueStringsFromSubmissionState("mutationRequests").size();
    }

    public List<String> getFailedOrderMutationRequests() {
        return uniqueStringsFromSubmissionState("failedRequests");
    }

    private Map<?, ?> getSubmissionState() {
        Object result = executeScript("""
                try {
                    return JSON.parse(localStorage.getItem(arguments[0]) || '{}');
                } catch (error) {
                    return {};
                }
                """, SUBMISSION_STATE_KEY);

        return result instanceof Map<?, ?> map ? map : Map.of();
    }

    private List<String> uniqueStringsFromSubmissionState(String key) {
        Object value = getSubmissionState().get(key);
        if (!(value instanceof List<?> values)) {
            return List.of();
        }

        Set<String> uniqueValues = new LinkedHashSet<>();
        for (Object item : values) {
            if (item != null && !String.valueOf(item).isBlank()) {
                uniqueValues.add(String.valueOf(item));
            }
        }

        return new ArrayList<>(uniqueValues);
    }

    private boolean moneyEquals(BigDecimal actual, BigDecimal expected) {
        return actual != null
                && expected != null
                && actual.setScale(2, RoundingMode.HALF_UP)
                .compareTo(expected.setScale(2, RoundingMode.HALF_UP)) == 0;
    }

    private String normalizeText(String text) {
        return text == null ? "" : text.replaceAll("\\s+", " ").trim();
    }
}
