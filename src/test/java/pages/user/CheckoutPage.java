package pages.user;

import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CheckoutPage extends BasePage {

    private static final Duration CHECKOUT_TIMEOUT = Duration.ofSeconds(25);
    private static final BigDecimal FREE_DELIVERY_THRESHOLD = new BigDecimal("999");

    public CheckoutPage(WebDriver driver) {
        super(driver);
    }

    public CheckoutPage waitUntilLoaded() {
        waitForPageLoad();
        try {
            waitUntil(CHECKOUT_TIMEOUT, webDriver -> isCheckoutLoadedNow());
        } catch (TimeoutException exception) {
            throw new TimeoutException("Checkout page did not load. Page text: '"
                    + normalizeText(getCheckoutText()) + "'. URL: " + getCurrentUrl(), exception);
        }

        try {
            waitUntil(CHECKOUT_TIMEOUT, webDriver -> !isCheckoutLoadingActive());
        } catch (TimeoutException exception) {
            if (!isCheckoutContentReady()) {
                throw new TimeoutException("Checkout loading state did not finish. Page text: '"
                        + normalizeText(getCheckoutText()) + "'. URL: " + getCurrentUrl(), exception);
            }
        }
        return this;
    }

    public String getCheckoutText() {
        Object result = executeScript("""
                const root = activeCheckoutRoot() || document.body;
                return String(root?.innerText || root?.textContent || '')
                    .replace(/\\s+/g, ' ')
                    .trim();

                function activeCheckoutRoot() {
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
                    return Array.from(document.querySelectorAll('body > div, [role="dialog"], body div'))
                        .filter(visible)
                        .filter(element => {
                            const text = normalize(element.innerText || element.textContent || '');
                            return /^Checkout\\b/i.test(text)
                                && /(Place\\s+Order|Delivery\\s+Method|Payment\\s+Method|Payable)/i.test(text);
                        })
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        )
                        .map(candidate => candidate.element)[0] || null;
                }
                """);
        return result == null ? "" : String.valueOf(result);
    }

    public boolean isDeliveryToSectionDisplayed() {
        return normalizeText(getCheckoutText()).matches("(?is).*(\\bDelivery\\s+To\\b|Selected\\s+delivery\\s+address).*");
    }

    public boolean isSelectedDeliveryAddressDisplayedCorrectly(String fullName, String addressLine) {
        String text = canonicalText(getCheckoutText());
        String expectedName = canonicalText(fullName);
        String expectedAddress = canonicalText(addressLine);

        return isDeliveryToSectionDisplayed()
                && (expectedName.isBlank() || text.contains(expectedName))
                && (expectedAddress.isBlank() || text.contains(expectedAddress));
    }

    public boolean isAdditionalInformationDisplayed() {
        return Boolean.TRUE.equals(executeScript(additionalInformationFieldScript("""
                return Boolean(field);
                """)));
    }

    public CheckoutPage enterAdditionalInformation(String additionalInformation) {
        Boolean entered = (Boolean) executeScript(additionalInformationFieldScript("""
                if (!field) {
                    return false;
                }

                field.scrollIntoView({ block: 'center', inline: 'nearest' });
                const descriptor = Object.getOwnPropertyDescriptor(
                    field.tagName.toLowerCase() === 'textarea'
                        ? HTMLTextAreaElement.prototype
                        : HTMLInputElement.prototype,
                    'value'
                );
                descriptor.set.call(field, arguments[0]);
                field.dispatchEvent(new Event('input', { bubbles: true }));
                field.dispatchEvent(new Event('change', { bubbles: true }));
                return true;
                """), additionalInformation);

        if (!Boolean.TRUE.equals(entered)) {
            throw new TimeoutException("Additional Information field was not found on checkout.");
        }

        waitUntil(CHECKOUT_TIMEOUT, webDriver -> additionalInformation.equals(getAdditionalInformationValue()));
        return this;
    }

    public String getAdditionalInformationValue() {
        Object result = executeScript(additionalInformationFieldScript("""
                return field ? String(field.value || '') : '';
                """));
        return result == null ? "" : String.valueOf(result);
    }

    public boolean isRegularDeliveryVisible() {
        return isDeliveryMethodVisible("Regular Delivery");
    }

    public boolean isRegularDeliverySelectedByDefault() {
        return isDeliveryMethodSelected("Regular Delivery");
    }

    public boolean isRegularDeliveryDetailsDisplayed() {
        return deliveryMethodText("Regular Delivery").matches("(?is).*(delivery|time|today|tomorrow|hour|day|৳|free).*");
    }

    public boolean isExpressDeliveryVisible() {
        return isDeliveryMethodVisible("Express Delivery");
    }

    public boolean isExpressDeliveryAvailable() {
        return isExpressDeliveryVisible()
                && !deliveryMethodText("Express Delivery").matches("(?is).*not\\s+available.*");
    }

    public CheckoutPage selectExpressDelivery() {
        if (!isExpressDeliveryVisible()) {
            throw new TimeoutException("Express Delivery option is not displayed on checkout.");
        }

        waitUntil(CHECKOUT_TIMEOUT, webDriver -> clickDeliveryMethod("Express Delivery"));
        waitUntil(CHECKOUT_TIMEOUT, webDriver -> isDeliveryMethodSelected("Express Delivery"));
        return this;
    }

    public boolean isExpressDeliverySelected() {
        return isDeliveryMethodSelected("Express Delivery");
    }

    public boolean doesExpressDeliveryUpdateInformationOrCharge(PaymentSummary beforeSelection) {
        PaymentSummary afterSelection = getPaymentSummary();
        String expressText = deliveryMethodText("Express Delivery");
        String regularText = deliveryMethodText("Regular Delivery");

        return isExpressDeliverySelected()
                && (!normalizeText(expressText).equalsIgnoreCase(normalizeText(regularText))
                || !moneyEquals(beforeSelection.deliveryCharge(), afterSelection.deliveryCharge())
                || !moneyEquals(beforeSelection.payableTotal(), afterSelection.payableTotal()));
    }

    public boolean isPaymentMethodSectionDisplayed() {
        return normalizeText(getCheckoutText()).matches("(?is).*Payment\\s+Method.*");
    }

    public boolean isCashOnDeliveryVisible() {
        return normalizeText(getCheckoutText()).matches("(?is).*Cash\\s+on\\s+Delivery.*");
    }

    public boolean isCashOnDeliverySelectedByDefault() {
        String text = normalizeText(getCheckoutText());
        return isCashOnDeliveryVisible()
                && (!text.matches("(?is).*Select\\s+Payment.*")
                || isPaymentOptionSelected("Cash on Delivery"));
    }

    public PaymentSummary getPaymentSummary() {
        String text = normalizeCurrencyText(paymentSummaryText());
        if (text.isBlank()) {
            text = normalizeCurrencyText(getCheckoutText());
        }
        BigDecimal productSubtotal = firstPresentMoney(text,
                "(?i)(?:Product\\s+Subtotal|Subtotal|Product\\s+Total|Total\\s+Product\\s+Price)[^৳]*৳\\s*([0-9]+(?:\\.[0-9]+)?)");
        boolean productSubtotalDisplayed = productSubtotal != null;
        BigDecimal mrp = firstPresentMoney(text,
                "(?i)\\bMRP\\b[^৳]*৳\\s*([0-9]+(?:\\.[0-9]+)?)");
        BigDecimal deliveryCharge = firstPresentMoney(text,
                "(?i)Delivery\\s+charge\\s*(?:\\([^)]*\\))?\\s*৳\\s*([0-9]+(?:\\.[0-9]+)?)");
        if (deliveryCharge == null) {
            deliveryCharge = firstPresentMoney(text,
                    "(?i)(?:Regular\\s+Delivery|Express\\s+Delivery|Delivery\\s+Fee|Shipping\\s+Charge|Shipping\\s+Fee)[^৳]*৳\\s*([0-9]+(?:\\.[0-9]+)?)");
        }
        if (deliveryCharge == null && text.matches("(?is).*(?:Delivery\\s+charge|Regular\\s+Delivery|Express\\s+Delivery|Delivery\\s+Fee|Shipping\\s+Charge|Shipping\\s+Fee)[^৳]*(?:Free|৳\\s*0(?:\\.0+)?).*")) {
            deliveryCharge = BigDecimal.ZERO;
        }
        BigDecimal discount = firstPresentMoney(text,
                "(?i)(?:Discount|Saved)[^৳]*-?\\s*৳\\s*([0-9]+(?:\\.[0-9]+)?)");
        BigDecimal aroggaCashApplied = firstPresentMoney(text,
                "(?i)Arogga\\s+cash\\s+applied[^৳]*-?\\s*৳\\s*([0-9]+(?:\\.[0-9]+)?)");
        BigDecimal roundingAdjustment = signedMoney(text,
                "(?i)Rounding\\s+off\\s*(-?)\\s*৳\\s*([0-9]+(?:\\.[0-9]+)?)");
        BigDecimal payableTotal = firstPresentMoney(text,
                "(?i)(?:Payable\\s+Total|Amount\\s+Payable|Payable|Grand\\s+Total)[^৳]*৳\\s*([0-9]+(?:\\.[0-9]+)?)");
        if (payableTotal == null) {
            payableTotal = lastPresentMoney(text,
                    "(?i)\\bTotal\\b[^৳]*৳\\s*([0-9]+(?:\\.[0-9]+)?)");
        }

        List<CartPage.CartProductData> products = getCheckoutSelectedProducts();
        BigDecimal productSubtotalFromRows = products.stream()
                .map(CartPage.CartProductData::subtotal)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal mrpFromRows = products.stream()
                .map(CartPage.CartProductData::mrp)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discountFromRows = products.stream()
                .map(CartPage.CartProductData::discount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (productSubtotal == null && mrp != null) {
            productSubtotal = mrp.subtract(discount == null ? BigDecimal.ZERO : discount);
        }
        if (productSubtotal == null && productSubtotalFromRows.compareTo(BigDecimal.ZERO) > 0) {
            productSubtotal = productSubtotalFromRows;
        }
        if (mrp == null && mrpFromRows.compareTo(BigDecimal.ZERO) > 0) {
            mrp = mrpFromRows;
        }
        if (discount == null) {
            discount = discountFromRows;
        }

        return new PaymentSummary(
                productSubtotal,
                mrp,
                deliveryCharge == null ? BigDecimal.ZERO : deliveryCharge,
                discount == null ? BigDecimal.ZERO : discount,
                aroggaCashApplied == null ? BigDecimal.ZERO : aroggaCashApplied,
                roundingAdjustment == null ? BigDecimal.ZERO : roundingAdjustment,
                payableTotal,
                productSubtotalDisplayed
        );
    }

    @SuppressWarnings("unchecked")
    public List<CartPage.CartProductData> getCheckoutSelectedProducts() {
        expandSelectedProductSectionIfCollapsed();
        Object result = executeScript("""
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
                const moneyValues = text => {
                    const values = [];
                    const matcher = String(text || '').matchAll(/৳\\s*([0-9]+(?:\\.[0-9]+)?)/g);
                    for (const match of matcher) {
                        values.push(match[1]);
                    }
                    return values;
                };
                const productNameFromHref = href => {
                    const match = String(href || '').match(/\\/product\\/[^/]+\\/(.*)$/);
                    if (!match || !match[1]) {
                        return '';
                    }
                    try {
                        return decodeURIComponent(match[1]).replace(/[-_]+/g, ' ').replace(/\\s+/g, ' ').trim();
                    } catch (error) {
                        return match[1].replace(/[-_]+/g, ' ').trim();
                    }
                };
                const root = activeCheckoutRoot();
                if (!root) {
                    return [];
                }
                const scrollable = Array.from(root.querySelectorAll('div, section'))
                    .filter(element => element.scrollHeight > element.clientHeight + 8)
                    .sort((first, second) => (second.scrollHeight - second.clientHeight)
                        - (first.scrollHeight - first.clientHeight))[0] || root;
                scrollable.scrollTop = scrollable.scrollHeight;

                let productRows = Array.from(root.querySelectorAll("a[href*='/product/']"))
                    .filter(visible)
                    .filter(row => !/flash\\s+sale|products\\?source/i.test(row.href || ''));
                if (!productRows.length) {
                    productRows = Array.from(root.querySelectorAll('div, li, article, section'))
                        .filter(visible)
                        .filter(row => {
                            const text = normalize(row.innerText || row.textContent || '');
                            const productImageAlt = Array.from(row.querySelectorAll('img[alt]'))
                                .map(image => normalize(image.getAttribute('alt')))
                                .find(alt => alt
                                    && !/select item|regular delivery|express delivery|delivery|remove|delete|notfound|placeholder|icon|logo/i
                                        .test(alt));
                            return Boolean(productImageAlt)
                                && /(?:৳\\s*[0-9]+|Qty\\s*:|\\b[0-9]+\\s*x\\b)/i.test(text)
                                && !/(Delivery\\s+Method|Payment\\s+Method|Promo\\s+code|Payable|Saved|Selected\\s+delivery\\s+address)/i
                                    .test(text);
                        })
                        .filter(row => !Array.from(row.children).some(child => {
                            if (!visible(child)) {
                                return false;
                            }

                            const text = normalize(child.innerText || child.textContent || '');
                            const productImageAlt = Array.from(child.querySelectorAll('img[alt]'))
                                .map(image => normalize(image.getAttribute('alt')))
                                .find(alt => alt
                                    && !/select item|regular delivery|express delivery|delivery|remove|delete|notfound|placeholder|icon|logo/i
                                        .test(alt));
                            return Boolean(productImageAlt)
                                && /(?:৳\\s*[0-9]+|Qty\\s*:|\\b[0-9]+\\s*x\\b)/i.test(text);
                        }));
                }

                return productRows.map(row => {
                    const text = normalize(row.innerText || row.textContent || '');
                    const imageAlts = Array.from(row.querySelectorAll('img[alt]'))
                        .map(image => normalize(image.getAttribute('alt')))
                        .filter(Boolean);
                    const productImageAlt = imageAlts.find(alt =>
                        !/select item|regular delivery|express delivery|delivery icon|remove|delete|notfound|placeholder|icon/i
                            .test(alt)
                    );
                    const textLines = text.split(/\\s{2,}|\\n+/).map(normalize).filter(Boolean);
                    const visibleName = textLines.find(line =>
                        line.length > 2
                            && !/^৳/.test(line)
                            && !/%\\s*OFF$/i.test(line)
                            && !/^Qty\\s*:/i.test(line)
                            && !/^Pack\\s+Size\\s*:/i.test(line)
                            && !/^\\d+\\s*x\\b/i.test(line)
                    );
                    const amounts = moneyValues(text);
                    const subtotal = amounts.length ? amounts[amounts.length - 1] : null;
                    const mrp = amounts.length > 1 ? amounts[0] : subtotal;
                    const quantityMatch = text.match(/Qty\\s*:\\s*([0-9]+)/i)
                        || text.match(/\\b([0-9]+)\\s*x\\b/i);
                    const quantity = quantityMatch ? Number(quantityMatch[1]) : 1;

                    return {
                        productName: productImageAlt || visibleName || productNameFromHref(row.href || row.getAttribute('href') || ''),
                        productUrl: row.href || row.getAttribute('href') || '',
                        rowText: text,
                        quantityText: normalize((text.match(/Qty\\s*:\\s*[0-9]+/i) || [])[0] || ''),
                        quantity,
                        subtotal,
                        mrp,
                        hasImage: Array.from(row.querySelectorAll('img')).some(image =>
                            visible(image)
                                && image.complete === true
                                && image.naturalWidth > 0
                                && image.naturalHeight > 0
                                && !/remove|delete|icon|regular delivery|express delivery/i.test(
                                    image.alt || image.src || image.currentSrc || ''
                                )
                        )
                    };
                }).filter(product => product.productName || product.productUrl);

                function activeCheckoutRoot() {
                    return Array.from(document.querySelectorAll('body > div, [role="dialog"], body div'))
                        .filter(visible)
                        .filter(element => {
                            const text = normalize(element.innerText || element.textContent || '');
                            return /^Checkout\\b/i.test(text)
                                && /(Place\\s+Order|Delivery\\s+Method|Payment\\s+Method|Payable)/i.test(text);
                        })
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        )
                        .map(candidate => candidate.element)[0] || null;
                }
                """);

        List<CartPage.CartProductData> products = new ArrayList<>();
        if (!(result instanceof List<?> rawProducts)) {
            return products;
        }

        for (Object rawProduct : rawProducts) {
            if (!(rawProduct instanceof Map<?, ?> product)) {
                continue;
            }

            int quantity = toInt(product.get("quantity"));
            if (quantity <= 0) {
                quantity = 1;
            }

            BigDecimal subtotal = toMoney(product.get("subtotal"));
            BigDecimal mrp = toMoney(product.get("mrp"));
            BigDecimal unitPrice = subtotal == null
                    ? null
                    : subtotal.divide(BigDecimal.valueOf(quantity), 2, RoundingMode.HALF_UP);
            BigDecimal discount = mrp != null && subtotal != null && mrp.compareTo(subtotal) > 0
                    ? mrp.subtract(subtotal)
                    : BigDecimal.ZERO;

            products.add(new CartPage.CartProductData(
                    stringValue(product.get("productName")),
                    stringValue(product.get("productUrl")),
                    stringValue(product.get("rowText")),
                    stringValue(product.get("quantityText")),
                    quantity,
                    unitPrice,
                    subtotal,
                    mrp,
                    discount,
                    true,
                    false,
                    booleanValue(product.get("hasImage")),
                    false
            ));
        }

        List<CartPage.CartProductData> textProducts = getCheckoutSelectedProductsFromText();
        int declaredItemCount = checkoutSelectedProductCountFromText(selectedProductSectionText());
        if (declaredItemCount > 0 && products.size() != declaredItemCount) {
            return textProducts;
        }
        if (products.isEmpty()) {
            products.addAll(textProducts);
        }

        return products;
    }

    public int getCheckoutSelectedProductCount() {
        int declaredItemCount = checkoutSelectedProductCountFromText(selectedProductSectionText());
        if (declaredItemCount > 0) {
            return declaredItemCount;
        }

        return getCheckoutSelectedProducts().size();
    }

    public boolean verifySelectedProductsInCheckout(List<CartPage.CartProductData> expectedProducts) {
        String selectedProductText = selectedProductSectionText();
        if (expectedProducts.size() > 1) {
            return checkoutSelectedProductCountFromText(selectedProductText) == expectedProducts.size()
                    || getCheckoutSelectedProductCount() == expectedProducts.size();
        }

        if (getCheckoutSelectedProductCount() != expectedProducts.size()) {
            return false;
        }

        List<CartPage.CartProductData> actualProducts = getCheckoutSelectedProducts();
        String selectedSectionText = canonicalText(selectedProductText);
        String checkoutText = canonicalText(getCheckoutText());
        int matchedProductCount = 0;
        for (CartPage.CartProductData expectedProduct : expectedProducts) {
            CartPage.CartProductData actualProduct = actualProducts.stream()
                    .filter(product -> product.matches(expectedProduct.productName(), canonicalText(expectedProduct.productName())))
                    .findFirst()
                    .orElse(null);
            if (actualProduct == null
                    && !selectedSectionText.contains(canonicalText(expectedProduct.productName()))
                    && !checkoutText.contains(canonicalText(expectedProduct.productName()))) {
                continue;
            }

            matchedProductCount++;
            if (actualProduct == null) {
                continue;
            }

            if (expectedProduct.quantity() > 0 && actualProduct.quantity() > 0
                    && expectedProduct.quantity() != actualProduct.quantity()) {
                return false;
            }

            if (expectedProduct.subtotal() != null && actualProduct.subtotal() != null
                    && !moneyEquals(expectedProduct.subtotal(), actualProduct.subtotal())) {
                return false;
            }
        }

        return matchedProductCount == expectedProducts.size()
                || expectedProducts.size() > 1;
    }

    public boolean verifyUnselectedProductsExcludedFromCheckout(List<CartPage.CartProductData> unselectedProducts) {
        expandSelectedProductSectionIfCollapsed();
        String selectedSectionText = selectedProductSectionText();
        String checkoutText = canonicalText(selectedSectionText.isBlank() ? getCheckoutText() : selectedSectionText);
        for (CartPage.CartProductData product : unselectedProducts) {
            if (!product.productName().isBlank() && checkoutText.contains(canonicalText(product.productName()))) {
                return false;
            }
        }

        return true;
    }

    public boolean verifyPaymentSummaryMatchesSelectedProducts(List<CartPage.CartProductData> selectedProducts) {
        if (selectedProducts == null || selectedProducts.isEmpty()) {
            return false;
        }

        try {
            waitUntil(Duration.ofSeconds(20), webDriver ->
                    paymentSummaryMatchesSelectedProducts(getPaymentSummary(), selectedProducts));
        } catch (TimeoutException exception) {
            return false;
        }

        return paymentSummaryMatchesSelectedProducts(getPaymentSummary(), selectedProducts);
    }

    public boolean verifyPaymentSummaryMatchesCartBreakdown(CartPage.CartPriceBreakdown cartBreakdown) {
        if (cartBreakdown == null || !cartBreakdown.hasRequiredValues()) {
            return false;
        }

        try {
            waitUntil(Duration.ofSeconds(10), webDriver ->
                    paymentSummaryMatchesCartBreakdown(getPaymentSummary(), cartBreakdown));
        } catch (TimeoutException exception) {
            return false;
        }

        return paymentSummaryMatchesCartBreakdown(getPaymentSummary(), cartBreakdown);
    }

    private boolean paymentSummaryMatchesCartBreakdown(
            PaymentSummary summary,
            CartPage.CartPriceBreakdown cartBreakdown
    ) {
        if (summary.productSubtotal() == null || summary.payableTotal() == null) {
            return false;
        }

        return isPaymentSummaryInternallyConsistent(summary)
                && moneyEquals(summary.productSubtotal(), cartBreakdown.productTotal())
                && optionalMoneyEquals(summary.mrp(), cartBreakdown.mrp())
                && moneyEquals(zeroIfNull(summary.discount()), zeroIfNull(cartBreakdown.discount()))
                && moneyEquals(summary.deliveryCharge(), cartBreakdown.delivery())
                && moneyEquals(summary.aroggaCashApplied(), cartBreakdown.aroggaCashApplied())
                && moneyEquals(summary.roundingAdjustment(), cartBreakdown.roundingAdjustment())
                && moneyEquals(summary.payableTotal(), cartBreakdown.amountPayable());
    }

    private boolean optionalMoneyEquals(BigDecimal actual, BigDecimal expected) {
        return actual == null || expected == null || moneyEquals(actual, expected);
    }

    private boolean isPaymentSummaryInternallyConsistent(PaymentSummary summary) {
        if (summary.productSubtotal() == null) {
            return false;
        }
        if (summary.mrp() != null && summary.mrp().compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }
        if (summary.discount() != null && summary.discount().compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }
        if (summary.payableTotal() == null) {
            return false;
        }

        return moneyEquals(expectedPayable(summary).setScale(0, RoundingMode.HALF_UP),
                summary.payableTotal().setScale(0, RoundingMode.HALF_UP));
    }

    private boolean paymentSummaryMatchesSelectedProducts(
            PaymentSummary summary,
            List<CartPage.CartProductData> selectedProducts
    ) {
        BigDecimal expectedSubtotal = selectedProducts.stream()
                .map(CartPage.CartProductData::subtotal)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expectedMrp = selectedProducts.stream()
                .map(product -> product.mrp() == null ? product.subtotal() : product.mrp())
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expectedDiscount = expectedMrp.subtract(expectedSubtotal).max(BigDecimal.ZERO);

        if (expectedSubtotal.compareTo(BigDecimal.ZERO) <= 0
                || summary.productSubtotal() == null
                || summary.payableTotal() == null) {
            return false;
        }

        BigDecimal displayedProductAmount = summary.productSubtotal();
        boolean productSubtotalMatches = moneyEquals(displayedProductAmount, expectedSubtotal)
                || summary.productSubtotalDisplayed()
                && moneyEquals(displayedProductAmount.subtract(zeroIfNull(summary.discount())), expectedSubtotal);
        boolean mrpDiscountMatches = summary.mrp() != null
                && moneyEquals(summary.mrp(), expectedMrp)
                && moneyEquals(zeroIfNull(summary.discount()), expectedDiscount);
        if (!productSubtotalMatches && !mrpDiscountMatches) {
            return false;
        }

        if (expectedSubtotal.compareTo(FREE_DELIVERY_THRESHOLD) >= 0) {
            if (!moneyEquals(summary.deliveryCharge(), BigDecimal.ZERO)) {
                return false;
            }
        } else if (summary.deliveryCharge() == null || summary.deliveryCharge().compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        BigDecimal expectedPayable = expectedSubtotal
                .add(summary.deliveryCharge())
                .add(summary.roundingAdjustment())
                .subtract(summary.aroggaCashApplied())
                .max(BigDecimal.ZERO);
        return moneyEquals(expectedPayable.setScale(0, RoundingMode.HALF_UP),
                summary.payableTotal().setScale(0, RoundingMode.HALF_UP));
    }

    public boolean isPlaceOrderButtonVisible() {
        return Boolean.TRUE.equals(executeScript("""
                const root = activeCheckoutRoot();
                const button = root ? findActionButton(root, /Place\\s+Order/i) : null;
                return Boolean(button);

                function findActionButton(root, pattern) {
                    return Array.from(root.querySelectorAll('button'))
                        .filter(visible)
                        .find(button => pattern.test(button.innerText || button.textContent || '')) || null;
                }

                function activeCheckoutRoot() {
                    return Array.from(document.querySelectorAll('[role="dialog"], body > div, body div'))
                        .filter(visible)
                        .filter(element => {
                            const text = normalize(element.innerText || element.textContent || '');
                            return /^Checkout\\b/i.test(text)
                                && /(Place\\s+Order|Delivery\\s+Method|Payment\\s+Method|Payable)/i.test(text)
                                && Array.from(element.querySelectorAll('button'))
                                    .some(button => visible(button)
                                        && /Place\\s+Order/i.test(button.innerText || button.textContent || ''));
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
                """));
    }

    public boolean isPlaceOrderButtonEnabled() {
        return Boolean.TRUE.equals(executeScript("""
                const root = activeCheckoutRoot();
                const button = root ? Array.from(root.querySelectorAll('button'))
                    .filter(visible)
                    .find(candidate => /Place\\s+Order/i.test(candidate.innerText || candidate.textContent || '')) : null;
                return Boolean(button)
                    && !button.disabled
                    && button.getAttribute('aria-disabled') !== 'true'
                    && getComputedStyle(button).pointerEvents !== 'none';

                function activeCheckoutRoot() {
                    return Array.from(document.querySelectorAll('[role="dialog"], body > div, body div'))
                        .filter(visible)
                        .filter(element => {
                            const text = normalize(element.innerText || element.textContent || '');
                            return /^Checkout\\b/i.test(text)
                                && /(Place\\s+Order|Delivery\\s+Method|Payment\\s+Method|Payable)/i.test(text)
                                && Array.from(element.querySelectorAll('button'))
                                    .some(button => visible(button)
                                        && /Place\\s+Order/i.test(button.innerText || button.textContent || ''));
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
                """));
    }

    public OrderConfirmationPage clickPlaceOrderAndWaitForConfirmation() {
        try {
            waitUntil(CHECKOUT_TIMEOUT, webDriver -> clickPlaceOrderButton());
        } catch (TimeoutException exception) {
            throw new TimeoutException("Place Order button was not clickable in checkout. Checkout text: '"
                    + normalizeText(getCheckoutText()) + "'. URL: " + getCurrentUrl(), exception);
        }
        return new OrderConfirmationPage(driver).waitUntilLoaded();
    }

    private boolean isCheckoutLoadedNow() {
        if (isCheckoutContentReady()) {
            return true;
        }

        String text = normalizeText(getCheckoutText());
        return text.matches("(?is).*(Delivery\\s+To|Selected\\s+delivery\\s+address|Checkout\\s+Details|Payment\\s+Summary|Place\\s+Order).*")
                && (text.matches("(?is).*Payment\\s+Method.*")
                || text.matches("(?is).*Cash\\s+on\\s+Delivery.*")
                || text.matches("(?is).*Additional\\s+Information.*")
                || text.matches("(?is).*Place\\s+Order.*"));
    }

    private boolean isCheckoutContentReady() {
        String text = normalizeText(getCheckoutText());
        return text.matches("(?is).*Payment\\s+Summary.*")
                && text.matches("(?is).*Selected\\s+product.*")
                && text.matches("(?is).*Place\\s+Order.*");
    }

    private boolean isCheckoutLoadingActive() {
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
                return Array.from(document.querySelectorAll('body *'))
                    .filter(visible)
                    .some(element => {
                        const rect = element.getBoundingClientRect();
                        const text = normalize(element.innerText || element.textContent || '');
                        const descriptor = [
                            text,
                            element.getAttribute('aria-label'),
                            element.getAttribute('alt'),
                            element.getAttribute('src'),
                            element.getAttribute('class')
                        ].filter(Boolean).join(' ');
                        const centered = Math.abs((rect.left + rect.width / 2) - window.innerWidth / 2) < 140
                            && Math.abs((rect.top + rect.height / 2) - window.innerHeight / 2) < 140;
                        return centered && /arogga|for\\s+better\\s+health|loader|loading|spinner/i.test(descriptor);
                    });
                """));
    }

    private void expandSelectedProductSectionIfCollapsed() {
        Boolean sectionTouched = (Boolean) executeScript("""
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
                const root = activeCheckoutRoot();
                if (!root) {
                    return false;
                }

                const section = Array.from(root.querySelectorAll('button, div, section'))
                    .filter(visible)
                    .map(element => ({ element, text: normalize(element.innerText || element.textContent || '') }))
                    .filter(candidate => /Selected\\s+product/i.test(candidate.text))
                    .sort((first, second) => elementArea(first.element) - elementArea(second.element))
                    .map(candidate => candidate.element)[0] || null;
                if (!section) {
                    return false;
                }

                const sectionText = normalize(section.innerText || section.textContent || '');
                if (/\\b[0-9]+\\s*x\\b.*৳\\s*[0-9]/i.test(sectionText)) {
                    return true;
                }

                const button = Array.from(section.querySelectorAll('button'))
                    .filter(visible)[0] || section.closest('button') || section;
                button.scrollIntoView({ block: 'center', inline: 'nearest' });
                button.click();
                return true;

                function activeCheckoutRoot() {
                    return Array.from(document.querySelectorAll('body > div, [role="dialog"], body div'))
                        .filter(visible)
                        .filter(element => {
                            const text = normalize(element.innerText || element.textContent || '');
                            return /^Checkout\\b/i.test(text)
                                && /(Place\\s+Order|Delivery\\s+Method|Payment\\s+Method|Payable)/i.test(text);
                        })
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        )
                        .map(candidate => candidate.element)[0] || null;
                }

                function elementArea(element) {
                    const rect = element.getBoundingClientRect();
                    return rect.width * rect.height;
                }
                """);

        if (Boolean.TRUE.equals(sectionTouched)) {
            try {
                waitUntil(Duration.ofSeconds(5), webDriver ->
                        selectedProductSectionText().matches("(?is).*\\b[0-9]+\\s*x\\b.*৳\\s*[0-9].*")
                                || checkoutSelectedProductCountFromText(selectedProductSectionText()) == 0);
            } catch (TimeoutException ignored) {
                // Some checkout states keep the accordion collapsed; text fallback still validates Item Count.
            }
        }
    }

    private boolean isDeliveryMethodVisible(String methodName) {
        return !deliveryMethodText(methodName).isBlank();
    }

    private boolean isDeliveryMethodSelected(String methodName) {
        Object result = executeScript("""
                const methodName = String(arguments[0] || '').trim();
                const option = deliveryMethodOption(methodName);
                if (!option) {
                    return false;
                }

                const className = String(option.className || '');
                const text = normalize(option.innerText || option.textContent || '');
                const radio = option.querySelector('input[type="radio"], input[type="checkbox"]');
                return (radio && radio.checked)
                    || option.getAttribute('aria-checked') === 'true'
                    || option.getAttribute('aria-selected') === 'true'
                    || /selected|active|border-brand|bg-brand|text-brand|font-semibold|font-bold/i.test(className)
                    || /selected|active/i.test(text);

                function deliveryMethodOption(name) {
                    const expected = canonical(name);
                    const root = activeCheckoutRoot() || document.body;
                    return Array.from(root.querySelectorAll('button, label, div, section'))
                        .filter(visible)
                        .map(element => ({ element, text: normalize(element.innerText || element.textContent || '') }))
                        .filter(candidate => canonical(candidate.text).includes(expected))
                        .sort((first, second) =>
                            elementArea(first.element) - elementArea(second.element)
                        )
                        .map(candidate => candidate.element)[0] || null;
                }

                function activeCheckoutRoot() {
                    return Array.from(document.querySelectorAll('body > div, [role="dialog"]'))
                        .filter(visible)
                        .filter(element => {
                            const text = normalize(element.innerText || element.textContent || '');
                            return /^Checkout\\b/i.test(text)
                                && /(Place\\s+Order|Delivery\\s+Method|Payment\\s+Method|Payable)/i.test(text);
                        })
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        )
                        .map(candidate => candidate.element)[0] || null;
                }

                function elementArea(element) {
                    const rect = element.getBoundingClientRect();
                    return rect.width * rect.height;
                }

                function canonical(text) {
                    return normalize(text).toLowerCase().replace(/[^a-z0-9]+/g, '');
                }

                function normalize(text) {
                    return String(text || '').replace(/\\s+/g, ' ').trim();
                }

                function visible(element) {
                    if (!element) {
                        return false;
                    }

                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                }
                """, methodName);

        return Boolean.TRUE.equals(result);
    }

    private boolean clickDeliveryMethod(String methodName) {
        return Boolean.TRUE.equals(executeScript("""
                const methodName = String(arguments[0] || '').trim();
                const option = deliveryMethodOption(methodName);
                if (!option) {
                    return false;
                }

                const target = option.closest('button,label') || option.querySelector('button,input') || option;
                target.scrollIntoView({ block: 'center', inline: 'nearest' });
                target.click();
                return true;

                function deliveryMethodOption(name) {
                    const expected = canonical(name);
                    const root = activeCheckoutRoot() || document.body;
                    return Array.from(root.querySelectorAll('button, label, div, section'))
                        .filter(visible)
                        .map(element => ({ element, text: normalize(element.innerText || element.textContent || '') }))
                        .filter(candidate => canonical(candidate.text).includes(expected))
                        .sort((first, second) =>
                            elementArea(first.element) - elementArea(second.element)
                        )
                        .map(candidate => candidate.element)[0] || null;
                }

                function activeCheckoutRoot() {
                    return Array.from(document.querySelectorAll('body > div, [role="dialog"]'))
                        .filter(visible)
                        .filter(element => {
                            const text = normalize(element.innerText || element.textContent || '');
                            return /^Checkout\\b/i.test(text)
                                && /(Place\\s+Order|Delivery\\s+Method|Payment\\s+Method|Payable)/i.test(text);
                        })
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        )
                        .map(candidate => candidate.element)[0] || null;
                }

                function elementArea(element) {
                    const rect = element.getBoundingClientRect();
                    return rect.width * rect.height;
                }

                function canonical(text) {
                    return normalize(text).toLowerCase().replace(/[^a-z0-9]+/g, '');
                }

                function normalize(text) {
                    return String(text || '').replace(/\\s+/g, ' ').trim();
                }

                function visible(element) {
                    if (!element) {
                        return false;
                    }

                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                }
                """, methodName));
    }

    private String deliveryMethodText(String methodName) {
        Object result = executeScript("""
                const methodName = String(arguments[0] || '').trim();
                const option = deliveryMethodOption(methodName);
                return option ? normalize(option.innerText || option.textContent || '') : '';

                function deliveryMethodOption(name) {
                    const expected = canonical(name);
                    const root = activeCheckoutRoot() || document.body;
                    return Array.from(root.querySelectorAll('button, label, div, section'))
                        .filter(visible)
                        .map(element => ({ element, text: normalize(element.innerText || element.textContent || '') }))
                        .filter(candidate => canonical(candidate.text).includes(expected))
                        .sort((first, second) =>
                            elementArea(first.element) - elementArea(second.element)
                        )
                        .map(candidate => candidate.element)[0] || null;
                }

                function activeCheckoutRoot() {
                    return Array.from(document.querySelectorAll('body > div, [role="dialog"]'))
                        .filter(visible)
                        .filter(element => {
                            const text = normalize(element.innerText || element.textContent || '');
                            return /^Checkout\\b/i.test(text)
                                && /(Place\\s+Order|Delivery\\s+Method|Payment\\s+Method|Payable)/i.test(text);
                        })
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        )
                        .map(candidate => candidate.element)[0] || null;
                }

                function elementArea(element) {
                    const rect = element.getBoundingClientRect();
                    return rect.width * rect.height;
                }

                function canonical(text) {
                    return normalize(text).toLowerCase().replace(/[^a-z0-9]+/g, '');
                }

                function normalize(text) {
                    return String(text || '').replace(/\\s+/g, ' ').trim();
                }

                function visible(element) {
                    if (!element) {
                        return false;
                    }

                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                }
                """, methodName);

        return result == null ? "" : String.valueOf(result);
    }

    private boolean isPaymentOptionSelected(String paymentMethod) {
        Object result = executeScript("""
                const paymentMethod = String(arguments[0] || '').trim();
                const expected = canonical(paymentMethod);
                const root = activeCheckoutRoot() || document.body;
                const option = Array.from(root.querySelectorAll('button, label, div, section'))
                    .filter(visible)
                    .map(element => ({ element, text: normalize(element.innerText || element.textContent || '') }))
                    .filter(candidate => canonical(candidate.text).includes(expected))
                    .sort((first, second) => elementArea(first.element) - elementArea(second.element))
                    .map(candidate => candidate.element)[0] || null;
                if (!option) {
                    return false;
                }

                const radio = option.querySelector('input[type="radio"], input[type="checkbox"]');
                const className = String(option.className || '');
                return (radio && radio.checked)
                    || option.getAttribute('aria-checked') === 'true'
                    || option.getAttribute('aria-selected') === 'true'
                    || /selected|active|border-brand|bg-brand|text-brand|font-semibold|font-bold/i.test(className);

                function elementArea(element) {
                    const rect = element.getBoundingClientRect();
                    return rect.width * rect.height;
                }

                function activeCheckoutRoot() {
                    return Array.from(document.querySelectorAll('body > div, [role="dialog"]'))
                        .filter(visible)
                        .filter(element => {
                            const text = normalize(element.innerText || element.textContent || '');
                            return /^Checkout\\b/i.test(text)
                                && /(Place\\s+Order|Delivery\\s+Method|Payment\\s+Method|Payable)/i.test(text);
                        })
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        )
                        .map(candidate => candidate.element)[0] || null;
                }

                function canonical(text) {
                    return normalize(text).toLowerCase().replace(/[^a-z0-9]+/g, '');
                }

                function normalize(text) {
                    return String(text || '').replace(/\\s+/g, ' ').trim();
                }

                function visible(element) {
                    if (!element) {
                        return false;
                    }

                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                }
                """, paymentMethod);

        return Boolean.TRUE.equals(result);
    }

    private boolean clickPlaceOrderButton() {
        return Boolean.TRUE.equals(executeScript("""
                const root = activeCheckoutRoot();
                if (!root) {
                    return false;
                }

                const button = Array.from(root.querySelectorAll('button'))
                    .filter(visible)
                    .find(candidate => /Place\\s+Order/i.test(candidate.innerText || candidate.textContent || ''));
                if (!button || button.disabled || button.getAttribute('aria-disabled') === 'true'
                        || getComputedStyle(button).pointerEvents === 'none') {
                    return false;
                }

                button.scrollIntoView({ block: 'center', inline: 'nearest' });
                button.dispatchEvent(new MouseEvent('pointerdown', { bubbles: true, cancelable: true, view: window }));
                button.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true, view: window }));
                button.dispatchEvent(new MouseEvent('pointerup', { bubbles: true, cancelable: true, view: window }));
                button.dispatchEvent(new MouseEvent('mouseup', { bubbles: true, cancelable: true, view: window }));
                button.click();
                return true;

                function activeCheckoutRoot() {
                    return Array.from(document.querySelectorAll('[role="dialog"], body > div, body div'))
                        .filter(visible)
                        .filter(element => {
                            const text = normalize(element.innerText || element.textContent || '');
                            return /^Checkout\\b/i.test(text)
                                && /(Place\\s+Order|Delivery\\s+Method|Payment\\s+Method|Payable)/i.test(text)
                                && Array.from(element.querySelectorAll('button'))
                                    .some(button => visible(button)
                                        && /Place\\s+Order/i.test(button.innerText || button.textContent || ''));
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
                """));
    }

    private String additionalInformationFieldScript(String actionScript) {
        return """
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
                const root = activeCheckoutRoot() || document.body;
                const field = Array.from(root.querySelectorAll('textarea, input'))
                    .filter(visible)
                    .find(input => /additional|instruction|note|comment|carefully/i.test(
                        [
                            input.getAttribute('placeholder'),
                            input.getAttribute('aria-label'),
                            input.getAttribute('name'),
                            input.id,
                            input.closest('label')?.innerText,
                            input.parentElement?.innerText
                        ].filter(Boolean).join(' ')
                    )) || null;
                function activeCheckoutRoot() {
                    return Array.from(document.querySelectorAll('body > div, [role="dialog"]'))
                        .filter(visible)
                        .filter(element => {
                            const text = normalize(element.innerText || element.textContent || '');
                            return /^Checkout\\b/i.test(text)
                                && /(Place\\s+Order|Delivery\\s+Method|Payment\\s+Method|Payable)/i.test(text);
                        })
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        )
                        .map(candidate => candidate.element)[0] || null;
                }
                """ + actionScript;
    }

    private BigDecimal firstPresentMoney(String text, String pattern) {
        Matcher matcher = Pattern.compile(pattern).matcher(text == null ? "" : text);
        if (!matcher.find()) {
            return null;
        }

        for (int index = 1; index <= matcher.groupCount(); index++) {
            String group = matcher.group(index);
            if (group != null && !group.isBlank()) {
                return new BigDecimal(group);
            }
        }

        return BigDecimal.ZERO;
    }

    private BigDecimal lastPresentMoney(String text, String pattern) {
        Matcher matcher = Pattern.compile(pattern).matcher(text == null ? "" : text);
        BigDecimal lastAmount = null;

        while (matcher.find()) {
            for (int index = 1; index <= matcher.groupCount(); index++) {
                String group = matcher.group(index);
                if (group != null && !group.isBlank()) {
                    lastAmount = new BigDecimal(group);
                    break;
                }
            }
        }

        return lastAmount;
    }

    private BigDecimal signedMoney(String text, String pattern) {
        Matcher matcher = Pattern.compile(pattern).matcher(text == null ? "" : text);
        if (!matcher.find()) {
            return null;
        }

        BigDecimal amount = new BigDecimal(matcher.group(2));
        return "-".equals(matcher.group(1)) ? amount.negate() : amount;
    }

    private BigDecimal expectedPayable(PaymentSummary summary) {
        BigDecimal expectedPayable = summary.productSubtotal()
                .add(summary.deliveryCharge())
                .add(summary.roundingAdjustment())
                .subtract(summary.aroggaCashApplied());
        if (summary.productSubtotalDisplayed()) {
            expectedPayable = expectedPayable.subtract(summary.discount());
        }

        return expectedPayable.max(BigDecimal.ZERO);
    }

    private List<CartPage.CartProductData> getCheckoutSelectedProductsFromText() {
        List<CartPage.CartProductData> products = new ArrayList<>();
        String selectedSection = selectedProductSectionText();
        if (selectedSection.isBlank()) {
            return products;
        }

        int declaredItemCount = checkoutSelectedProductCountFromText(selectedSection);
        String rowsText = selectedSection.replaceFirst("(?is)^.*?Item\\s+Count\\s*:\\s*[0-9]+", "").trim();
        Matcher matcher = Pattern.compile("(?is)(.+?)\\s+([0-9]+)\\s*x\\s+([^৳]+?)\\s+৳\\s*([0-9]+(?:\\.[0-9]+)?)")
                .matcher(rowsText);
        while (matcher.find()) {
            String productName = normalizeText(matcher.group(1));
            int quantity = Integer.parseInt(matcher.group(2));
            String quantityText = quantity + " x " + normalizeText(matcher.group(3));
            BigDecimal subtotal = new BigDecimal(matcher.group(4));
            BigDecimal unitPrice = subtotal.divide(BigDecimal.valueOf(quantity), 2, RoundingMode.HALF_UP);

            products.add(new CartPage.CartProductData(
                    productName,
                    "",
                    normalizeText(matcher.group(0)),
                    quantityText,
                    quantity,
                    unitPrice,
                    subtotal,
                    subtotal,
                    BigDecimal.ZERO,
                    true,
                    false,
                    false,
                    false
            ));
        }

        while (products.size() < declaredItemCount) {
            products.add(new CartPage.CartProductData(
                    "",
                    "",
                    selectedSection,
                    "",
                    1,
                    null,
                    null,
                    null,
                    BigDecimal.ZERO,
                    true,
                    false,
                    false,
                    false
            ));
        }

        return products;
    }

    private int checkoutSelectedProductCountFromText(String text) {
        Matcher matcher = Pattern.compile("(?is)Item\\s+Count\\s*:\\s*([0-9]+)").matcher(text == null ? "" : text);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    private String selectedProductSectionText() {
        Object result = executeScript("""
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
                const section = Array.from(document.querySelectorAll('body *'))
                    .filter(visible)
                    .map(element => ({ element, text: normalize(element.innerText || element.textContent || '') }))
                    .filter(candidate => /Selected\\s+product/i.test(candidate.text)
                        && /Item\\s+Count\\s*:/i.test(candidate.text))
                    .sort((first, second) => elementArea(first.element) - elementArea(second.element))
                    .map(candidate => candidate.element)[0] || null;
                if (!section) {
                    return '';
                }

                const texts = new Set();
                const capture = () => {
                    const text = normalize(section.innerText || section.textContent || '');
                    if (text) {
                        texts.add(text);
                    }
                };
                capture();
                const scrollables = [section, ...Array.from(section.querySelectorAll('div, section, ul, li'))]
                    .filter(element => element.scrollHeight > element.clientHeight + 4);
                for (const scrollable of scrollables) {
                    const originalTop = scrollable.scrollTop;
                    scrollable.scrollTop = 0;
                    capture();
                    scrollable.scrollTop = Math.floor(scrollable.scrollHeight / 2);
                    capture();
                    scrollable.scrollTop = scrollable.scrollHeight;
                    capture();
                    scrollable.scrollTop = originalTop;
                }

                return Array.from(texts).join(' ');

                function elementArea(element) {
                    const rect = element.getBoundingClientRect();
                    return rect.width * rect.height;
                }
                """);

        return result == null ? "" : normalizeText(String.valueOf(result));
    }

    private String paymentSummaryText() {
        Object result = executeScript("""
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
                return Array.from(document.querySelectorAll('body *'))
                    .filter(visible)
                    .map(element => ({ element, text: normalize(element.innerText || element.textContent || '') }))
                    .filter(candidate => /Payment\\s+Summary/i.test(candidate.text)
                        && /\\bMRP\\b/i.test(candidate.text)
                        && /Payable/i.test(candidate.text))
                    .sort((first, second) => elementArea(first.element) - elementArea(second.element))
                    .map(candidate => candidate.text)[0] || '';

                function elementArea(element) {
                    const rect = element.getBoundingClientRect();
                    return rect.width * rect.height;
                }
                """);

        return result == null ? "" : normalizeText(String.valueOf(result));
    }

    private boolean moneyEquals(BigDecimal actual, BigDecimal expected) {
        return actual != null
                && expected != null
                && actual.setScale(2, RoundingMode.HALF_UP)
                .compareTo(expected.setScale(2, RoundingMode.HALF_UP)) == 0;
    }

    private BigDecimal zeroIfNull(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
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

    private String canonicalText(String text) {
        return normalizeText(text).replaceAll("[^A-Za-z0-9]+", "").toLowerCase();
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).replaceAll("\\s+", " ").trim();
    }

    private boolean booleanValue(Object value) {
        return value instanceof Boolean bool && bool;
    }

    private int toInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }

        try {
            return Integer.parseInt(stringValue(value));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private BigDecimal toMoney(Object value) {
        String amount = stringValue(value);
        if (amount.isBlank()) {
            return null;
        }

        try {
            return new BigDecimal(amount);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    public record PaymentSummary(
            BigDecimal productSubtotal,
            BigDecimal mrp,
            BigDecimal deliveryCharge,
            BigDecimal discount,
            BigDecimal aroggaCashApplied,
            BigDecimal roundingAdjustment,
            BigDecimal payableTotal,
            boolean productSubtotalDisplayed
    ) {
    }
}
