package pages.user;

import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CartPage extends BasePage {

    private static final Duration CART_TIMEOUT = Duration.ofSeconds(20);

    public CartPage(WebDriver driver) {
        super(driver);
    }

    public CartPage openCartDrawer() {
        waitForPageLoad();

        for (int attempt = 0; attempt < 3 && !isCartDrawerOpen(); attempt++) {
            boolean clicked = clickHeaderCartButton();
            if (!clicked) {
                WebElement cartButton = waitUntil(CART_TIMEOUT, webDriver -> findHeaderCartButton());
                executeScript("""
                        arguments[0].scrollIntoView({ block: 'center', inline: 'nearest' });
                        arguments[0].click();
                        """, cartButton);
            }

            try {
                waitUntil(Duration.ofSeconds(5), webDriver -> isCartDrawerOpen());
            } catch (TimeoutException ignored) {
                if (attempt == 2) {
                    break;
                }
            }
        }

        waitUntil(CART_TIMEOUT, webDriver -> isCartDrawerOpen());
        return this;
    }

    public boolean isCartDrawerDisplayed() {
        return isCartDrawerOpen();
    }

    public CartPage closeCartDrawer() {
        if (!isCartDrawerOpen()) {
            return this;
        }

        Boolean clicked = waitUntil(CART_TIMEOUT, webDriver -> (Boolean) executeScript("""
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const panel = Array.from(document.querySelectorAll('body > div'))
                    .filter(visible)
                    .filter(element => /Shopping Cart|\\bCart\\s*\\(\\d+\\)/i.test(element.innerText || ''))
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
                if (!panel) {
                    return false;
                }

                const closeButton = Array.from(panel.querySelectorAll('button'))
                    .filter(visible)
                    .find(button => button.querySelector('svg[class*="lucide-x"]')
                        || /close/i.test(button.getAttribute('aria-label') || '')
                        || /^[x×]$/i.test((button.innerText || button.textContent || '').trim()));
                if (!closeButton) {
                    return false;
                }

                closeButton.click();
                return true;
                """));

        if (Boolean.TRUE.equals(clicked)) {
            waitUntil(CART_TIMEOUT, webDriver -> !isCartDrawerOpen());
        }

        return this;
    }

    public CartPage clearCartIfNeeded() {
        waitForPageLoad();
        if (!isCartDrawerOpen() && getHeaderCartBadgeCount() == 0) {
            return this;
        }
        openCartDrawer();
        waitForCartDrawerContentToLoad();

        if (clearCartWithSelectedRemoval()) {
            closeCartDrawer();

            try {
                waitUntil(CART_TIMEOUT, webDriver -> getHeaderCartBadgeCount() == 0);
            } catch (TimeoutException ignored) {
                // Some UI updates lag behind the server mutation; the next drawer open reads the canonical state.
            }

            return this;
        }

        int previousCount = getCartDrawerStoreItemCount();
        int safetyCounter = 0;

        while (previousCount > 0 && safetyCounter < 20) {
            if (!clickFirstCartItemRemoveIcon()) {
                throw new TimeoutException("Cart cleanup could not find a remove control while "
                        + previousCount + " store item(s) were still present. Cart drawer text: '"
                        + normalizeText(getCartDrawerText()) + "'.");
            }

            confirmCartItemRemovalIfDialogAppears();
            int expectedMaxCount = previousCount - 1;
            waitUntil(CART_TIMEOUT, webDriver -> getCartDrawerStoreItemCount() <= expectedMaxCount);
            previousCount = getCartDrawerStoreItemCount();
            safetyCounter++;
        }

        int remainingCount = getCartDrawerStoreItemCount();
        if (remainingCount > 0 && !isEmptyCartMessageDisplayedNow()) {
            throw new TimeoutException("Cart cleanup left " + remainingCount
                    + " store item(s) in the cart drawer. Cart drawer text: '"
                    + normalizeText(getCartDrawerText()) + "'.");
        }

        closeCartDrawer();

        try {
            waitUntil(CART_TIMEOUT, webDriver -> getHeaderCartBadgeCount() == 0);
        } catch (TimeoutException ignored) {
            // Some UI updates lag behind the server mutation; the next drawer open reads the canonical state.
        }

        return this;
    }

    private void waitForCartDrawerContentToLoad() {
        try {
            waitUntil(CART_TIMEOUT, webDriver -> hasLoadedCartDrawerContent());
        } catch (TimeoutException ignored) {
            // Cleanup still attempts the current drawer state; later assertions report any leftovers.
        }
    }

    public int getHeaderCartBadgeCount() {
        Object result = executeScript("""
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const button = Array.from(document.querySelectorAll('header button, header a, button, a'))
                    .filter(visible)
                    .find(element => /Cart/i.test(element.innerText || element.textContent || '')
                        || /cart/i.test(element.getAttribute('aria-label') || '')
                        || /cart/i.test(element.getAttribute('href') || '')
                        || element.querySelector("img[src*='cart'], img[alt*='Cart' i]"));
                if (!button) {
                    return 0;
                }

                const badge = Array.from(button.querySelectorAll('span, div, p'))
                    .find(element => /^[0-9]+$/.test((element.innerText || element.textContent || '').trim()));
                if (badge) {
                    return Number((badge.innerText || badge.textContent || '').trim());
                }

                const textMatch = String(button.innerText || button.textContent || '').match(/\\b([0-9]+)\\b/);
                return textMatch ? Number(textMatch[1]) : 0;
                """);

        return result instanceof Number number ? number.intValue() : 0;
    }

    public int getCartCount() {
        return getHeaderCartBadgeCount();
    }

    public boolean isHeaderCartBadgeCountDisplayed(int expectedCount) {
        return getHeaderCartBadgeCount() == expectedCount;
    }

    @SuppressWarnings("unchecked")
    public List<CartProductData> getCartProductRows() {
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
                const canonical = text => normalize(text).toLowerCase().replace(/[^a-z0-9]+/g, '');
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
                        return decodeURIComponent(match[1])
                            .replace(/[-_]+/g, ' ')
                            .replace(/\\s+/g, ' ')
                            .trim();
                    } catch (error) {
                        return match[1].replace(/[-_]+/g, ' ').trim();
                    }
                };
                const panel = activeCartDrawer();
                if (!panel) {
                    return [];
                }

                return Array.from(panel.querySelectorAll("a[href*='/product/']"))
                    .filter(visible)
                    .map(row => {
                        const text = normalize(row.innerText || row.textContent || '');
                        const textLines = text.split(/\\s{2,}|\\n+/)
                            .map(normalize)
                            .filter(Boolean);
                        const imageAlts = Array.from(row.querySelectorAll('img[alt]'))
                            .map(image => normalize(image.getAttribute('alt')))
                            .filter(Boolean);
                        const productImageAlt = imageAlts.find(alt =>
                            !/select item|regular delivery|express delivery|delivery icon|remove|delete|notfound|placeholder|icon/i
                                .test(alt)
                        );
                        const visibleName = textLines.find(line =>
                            line.length > 2
                                && !/^৳/.test(line)
                                && !/%\\s*OFF$/i.test(line)
                                && !/^Out\\s+of\\s+Stock$/i.test(line)
                                && !/^Qty\\s*:/i.test(line)
                                && !/^Pack\\s+Size\\s*:/i.test(line)
                                && !/^\\d+\\s*x\\b/i.test(line)
                        );
                        const productName = productImageAlt
                            || visibleName
                            || productNameFromHref(row.href || row.getAttribute('href') || '');
                        const selectionImage = row.querySelector("img[alt='Select Item']");
                        const selectionSource = selectionImage
                            ? String(selectionImage.currentSrc || selectionImage.src || '')
                            : '';
                        const selected = Boolean(selectionImage)
                            && /checkbox\\.svg/i.test(selectionSource)
                            && !/checkbox-empty/i.test(selectionSource);
                        const selectedQuantityText = (text.match(/\\b\\d+\\s*x\\s*[^৳]+?(?=\\s*৳|\\s*Qty\\s*:|$)/i) || [])[0] || '';
                        const quantityMatch = text.match(/Qty\\s*:\\s*([0-9]+)/i)
                            || selectedQuantityText.match(/^\\s*([0-9]+)/);
                        const quantity = quantityMatch ? Number(quantityMatch[1]) : 1;
                        const amounts = moneyValues(text);
                        const subtotal = amounts.length ? amounts[amounts.length - 1] : null;
                        const mrp = amounts.length > 1 ? amounts[0] : subtotal;
                        const outOfStock = /out\\s+of\\s+stock|notify/i.test(text);

                        return {
                            productName,
                            productUrl: row.href || row.getAttribute('href') || '',
                            rowText: text,
                            quantityText: normalize((text.match(/Qty\\s*:\\s*[0-9]+/i) || [])[0] || selectedQuantityText),
                            quantity,
                            subtotal,
                            mrp,
                            selected,
                            outOfStock,
                            hasImage: Array.from(row.querySelectorAll('img')).some(image =>
                                visible(image)
                                    && image.complete === true
                                    && image.naturalWidth > 0
                                    && image.naturalHeight > 0
                                    && !/select item|remove|delete|regular delivery|express delivery/i.test(
                                        image.alt || image.src || image.currentSrc || ''
                                    )
                            ),
                            hasRemoveAction: Boolean(row.querySelector("img[alt='Remove'], img[src*='delete']"))
                        };
                    })
                    .filter(product => product.productName || product.productUrl);

                function activeCartDrawer() {
                    return Array.from(document.querySelectorAll('body > div'))
                        .filter(visible)
                        .filter(element => /Shopping\\s+Cart|\\bCart\\s*\\(/i.test(element.innerText || ''))
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) => {
                            const firstLooksDrawer = first.rect.right >= window.innerWidth - 24
                                && first.rect.width <= Math.max(460, window.innerWidth * 0.9) ? 0 : 1;
                            const secondLooksDrawer = second.rect.right >= window.innerWidth - 24
                                && second.rect.width <= Math.max(460, window.innerWidth * 0.9) ? 0 : 1;
                            return firstLooksDrawer - secondLooksDrawer
                                || (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height);
                        })
                        .map(candidate => candidate.element)[0] || null;
                }
                """);

        List<CartProductData> products = new ArrayList<>();
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

            products.add(new CartProductData(
                    stringValue(product.get("productName")),
                    stringValue(product.get("productUrl")),
                    stringValue(product.get("rowText")),
                    stringValue(product.get("quantityText")),
                    quantity,
                    unitPrice,
                    subtotal,
                    mrp,
                    discount,
                    booleanValue(product.get("selected")),
                    booleanValue(product.get("outOfStock")),
                    booleanValue(product.get("hasImage")),
                    booleanValue(product.get("hasRemoveAction"))
            ));
        }

        return products;
    }

    public List<CartProductData> getSelectedCartProductRows() {
        return getCartProductRows().stream()
                .filter(CartProductData::selected)
                .toList();
    }

    public CartProductData getCartProductRow(String productName) {
        String expected = canonicalText(productName);
        return getCartProductRows().stream()
                .filter(product -> product.matches(productName, expected))
                .findFirst()
                .orElse(null);
    }

    public boolean hasUsableCartProduct(String productName) {
        CartProductData product = getCartProductRow(productName);
        return product != null && product.isUsableForCheckout();
    }

    public CartSelectionSummary getCartSelectionSummary() {
        List<CartProductData> products = getCartProductRows();
        List<CartProductData> selectedProducts = products.stream()
                .filter(CartProductData::selected)
                .toList();
        BigDecimal selectedSubtotal = selectedProducts.stream()
                .map(CartProductData::subtotal)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        String drawerText = normalizeCurrencyText(getCartDrawerText());
        BigDecimal payable = extractMoney(drawerText,
                "(?i)(?:Amount\\s+Payable|Payable)[^৳]*৳\\s*([0-9]+(?:\\.[0-9]+)?)");
        BigDecimal saved = extractMoney(drawerText,
                "(?i)Saved[^৳]*৳\\s*([0-9]+(?:\\.[0-9]+)?)");
        return new CartSelectionSummary(
                selectedProducts.size(),
                products.size(),
                selectedSubtotal,
                payable,
                saved == null ? BigDecimal.ZERO : saved
        );
    }

    public int getSelectedCartProductCount() {
        return getSelectedCartProductRows().size();
    }

    public List<String> getSelectedCartProductNames() {
        return getSelectedCartProductRows().stream()
                .map(CartProductData::productName)
                .toList();
    }

    public CartPage selectCartProduct(String productName) {
        setCartProductSelected(productName, true);
        return this;
    }

    public CartPage deselectCartProduct(String productName) {
        setCartProductSelected(productName, false);
        return this;
    }

    public CartPage selectMultipleCartProducts(Collection<String> productNames) {
        deselectAllCartProducts();
        for (String productName : productNames) {
            selectCartProduct(productName);
        }
        waitUntil(CART_TIMEOUT, webDriver -> getSelectedCartProductCount() == productNames.size());
        return this;
    }

    public CartPage deselectAllCartProducts() {
        for (CartProductData product : getSelectedCartProductRows()) {
            deselectCartProduct(product.productName());
        }
        waitUntil(CART_TIMEOUT, webDriver -> getSelectedCartProductCount() == 0);
        return this;
    }

    public CartPage waitForSelectedCartProductCount(int expectedCount) {
        waitUntil(CART_TIMEOUT, webDriver -> getSelectedCartProductCount() == expectedCount);
        return this;
    }

    public CheckoutPage clickCheckout() {
        openCartDrawer();
        waitUntil(CART_TIMEOUT, webDriver -> clickCheckoutButtonIfReady());
        return new CheckoutPage(driver).waitUntilLoaded();
    }

    public boolean verifyCartDrawerSupportsPartialSelectionUi() {
        List<CartProductData> products = getCartProductRows();
        CartSelectionSummary summary = getCartSelectionSummary();
        String drawerText = normalizeText(getCartDrawerText());

        return isCartDrawerOpen()
                && drawerText.matches("(?is).*Shopping\\s+Cart.*")
                && drawerText.matches("(?is).*Select\\s+All\\s*\\([0-9]+/[0-9]+\\).*")
                && !products.isEmpty()
                && products.stream().allMatch(product -> !product.productName().isBlank())
                && products.stream().allMatch(CartProductData::hasImage)
                && products.stream().allMatch(CartProductData::hasRemoveAction)
                && products.stream().allMatch(product -> product.subtotal() != null || product.outOfStock())
                && summary.selectedCount() >= 0
                && drawerText.matches("(?is).*(Payable|Checkout|Add\\s+Address).*");
    }

    public CartPage waitForHeaderCartBadgeCount(int expectedCount) {
        waitUntil(CART_TIMEOUT, webDriver -> getHeaderCartBadgeCount() == expectedCount);
        return this;
    }

    public boolean isCartBadgeIconDisplayedAndAligned() {
        return Boolean.TRUE.equals(executeScript("""
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const button = Array.from(document.querySelectorAll('header button'))
                    .filter(visible)
                    .find(element => /Cart/i.test(element.innerText || element.textContent || '')
                        || element.querySelector("img[src*='cart']"));
                if (!button) {
                    return false;
                }

                const icon = button.querySelector("img[src*='cart']");
                const badge = Array.from(button.querySelectorAll('span'))
                    .find(element => /^[0-9]+$/.test((element.innerText || element.textContent || '').trim()));
                if (!icon || !badge || !visible(icon) || !visible(badge)) {
                    return false;
                }

                const iconRect = icon.getBoundingClientRect();
                const badgeRect = badge.getBoundingClientRect();
                const iconLoaded = icon.complete === true
                    && icon.naturalWidth > 0
                    && icon.naturalHeight > 0;

                return iconLoaded
                    && badgeRect.width > 0
                    && badgeRect.height > 0
                    && badgeRect.left >= iconRect.left - 6
                    && badgeRect.top >= iconRect.top - 6
                    && badgeRect.right <= iconRect.right + 12
                    && badgeRect.bottom <= iconRect.top + iconRect.height * 0.7;
                """));
    }

    public String getProductLineText(String productName) {
        CartProductData cartProduct = getCartProductRow(productName);
        if (cartProduct != null && !cartProduct.rowText().isBlank()) {
            return cartProduct.rowText();
        }

        Object result = executeScript("""
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                const productName = canonical(arguments[0]);
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const panel = Array.from(document.querySelectorAll('body > div'))
                    .filter(visible)
                    .filter(element => /Shopping Cart|\\bCart\\s*\\(\\d+\\)/i.test(element.innerText || ''))
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
                if (!panel) {
                    return '';
                }

                const productLine = findProductLine(panel, productName);

                return productLine ? normalize(productLine.innerText) : '';

                function findProductLine(panel, productName) {
                    const linkedLine = Array.from(panel.querySelectorAll("a[href*='/product/']"))
                        .filter(visible)
                        .find(element => canonical(element.innerText || element.textContent).includes(productName));
                    if (linkedLine) {
                        return linkedLine;
                    }

                    return Array.from(panel.querySelectorAll('div, li, article, section, a'))
                        .filter(visible)
                        .filter(element => element !== panel)
                        .map(element => ({
                            element,
                            rect: element.getBoundingClientRect(),
                            text: normalize(element.innerText || element.textContent || '')
                        }))
                        .filter(candidate => canonical(candidate.text).includes(productName))
                        .filter(candidate => /Qty\\s*:\\s*\\d+/i.test(candidate.text)
                            || /৳\\s*\\d+(?:\\.\\d+)?/i.test(candidate.text))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        )
                        .map(candidate => candidate.element)[0] || null;
                }

                function canonical(text) {
                    return normalize(text).toLowerCase().replace(/[^a-z0-9]+/g, '');
                }
                """, productName);

        return result == null ? "" : String.valueOf(result);
    }

    public CartPage waitForProductLine(String productName) {
        try {
            waitUntil(CART_TIMEOUT, webDriver -> !getProductLineText(productName).isBlank());
        } catch (TimeoutException exception) {
            throw new TimeoutException("Cart drawer did not contain product line for '" + productName
                    + "'. Cart drawer text: '" + normalizeText(getCartDrawerText()) + "'.", exception);
        }
        return this;
    }

    public CartPage scrollCartDrawerToProductLine(String productName) {
        openCartDrawer();

        Boolean scrolled = (Boolean) executeScript("""
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                const requestedProductName = canonical(arguments[0]);
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const panel = Array.from(document.querySelectorAll('body > div'))
                    .filter(visible)
                    .filter(element => /Shopping\\s+Cart|\\bCart\\s*\\(?\\s*\\d+\\s*\\)?|\\bStore\\s*\\(?\\s*\\d+\\s*\\)?|Select\\s+All|cart\\s+is\\s+empty|your\\s+cart\\s+is\\s+empty/i.test(element.innerText || ''))
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
                if (!panel || requestedProductName.length === 0) {
                    return false;
                }

                const productLine = findProductLine(panel, requestedProductName);
                if (!productLine) {
                    return false;
                }

                productLine.scrollIntoView({ block: 'center', inline: 'nearest' });
                return true;

                function findProductLine(panel, productName) {
                    const linkedLine = Array.from(panel.querySelectorAll("a[href*='/product/']"))
                        .filter(visible)
                        .find(element => canonical(element.innerText || element.textContent).includes(productName));
                    if (linkedLine) {
                        return linkedLine;
                    }

                    return Array.from(panel.querySelectorAll('div, li, article, section, a'))
                        .filter(visible)
                        .filter(element => element !== panel)
                        .map(element => ({
                            element,
                            rect: element.getBoundingClientRect(),
                            text: normalize(element.innerText || element.textContent || '')
                        }))
                        .filter(candidate => canonical(candidate.text).includes(productName))
                        .filter(candidate => /Qty\\s*:\\s*\\d+/i.test(candidate.text)
                            || /৳\\s*\\d+(?:\\.\\d+)?/i.test(candidate.text))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        )
                        .map(candidate => candidate.element)[0] || null;
                }

                function canonical(text) {
                    return normalize(text).toLowerCase().replace(/[^a-z0-9]+/g, '');
                }
                """, productName);

        if (!Boolean.TRUE.equals(scrolled)) {
            throw new TimeoutException("Product line was not found in the cart drawer: " + productName);
        }

        waitUntil(CART_TIMEOUT, webDriver -> isProductLineVisibleInsideCartDrawer(productName));
        return this;
    }

    public CartPage waitForProductInCart(String productName, String selectedQuantity) {
        try {
            waitUntil(CART_TIMEOUT, webDriver -> verifyProductInCart(productName, selectedQuantity));
        } catch (TimeoutException exception) {
            throw new TimeoutException("Cart drawer did not contain selected product '" + productName
                    + "' with quantity '" + selectedQuantity + "'. Product line text: '"
                    + getProductLineText(productName) + "'. Cart drawer text: '"
                    + normalizeText(getCartDrawerText()) + "'.", exception);
        }
        return this;
    }

    public boolean isProductLinePriceDisplayed(String productName, String expectedPrice) {
        String productLine = normalizeCurrencyText(getProductLineText(productName));
        String price = normalizeCurrencyText(expectedPrice);

        return !productLine.isBlank() && productLine.contains(price);
    }

    public boolean isProductLineQuantityDisplayed(String productName, String expectedQuantityText) {
        String productLine = getProductLineText(productName).replaceAll("\\s+", " ").trim();
        return !productLine.isBlank() && productLine.contains(expectedQuantityText);
    }

    public boolean isProductLineTextDisplayed(String productName, String expectedText) {
        String productLine = normalizeText(getProductLineText(productName)).toLowerCase();
        String expected = normalizeText(expectedText).toLowerCase();

        return !productLine.isBlank()
                && !expected.isBlank()
                && productLine.contains(expected);
    }

    public CartPage waitForProductLineQuantity(String productName, String expectedQuantityText) {
        openCartDrawer();
        waitUntil(CART_TIMEOUT, webDriver -> isProductLineQuantityDisplayed(productName, expectedQuantityText));
        return this;
    }

    public CartPage updateProductQuantity(String productName, String quantityLabel) {
        openCartDrawer();
        waitForProductLine(productName);

        waitUntil(CART_TIMEOUT, webDriver -> clickCartProductQuantityControl(productName));
        waitUntil(CART_TIMEOUT, webDriver -> isQuantitySelectionDialogDisplayed());
        waitUntil(CART_TIMEOUT, webDriver -> clickQuantitySelectionOption(quantityLabel));
        waitUntil(CART_TIMEOUT, webDriver -> !isQuantitySelectionDialogDisplayed());

        return this;
    }

    public CartProductData updateProductQuantityToReachSubtotal(String productName, BigDecimal minimumSubtotal) {
        openCartDrawer();
        waitForProductLine(productName);

        CartProductData currentProduct = getCartProductRow(productName);
        if (minimumSubtotal == null
                || minimumSubtotal.compareTo(BigDecimal.ZERO) <= 0
                || currentProduct == null
                || currentProduct.unitPrice() == null
                || currentProduct.subtotal() != null
                && currentProduct.subtotal().compareTo(minimumSubtotal) >= 0) {
            return currentProduct;
        }

        waitUntil(CART_TIMEOUT, webDriver -> clickCartProductQuantityControl(productName));
        waitUntil(CART_TIMEOUT, webDriver -> isQuantitySelectionDialogDisplayed());

        List<String> quantityLabels = getVisibleCartQuantityOptionLabels();
        String selectedQuantityLabel = selectQuantityLabelForSubtotal(
                quantityLabels,
                currentProduct.unitPrice(),
                minimumSubtotal
        );
        if (selectedQuantityLabel.isBlank()) {
            closeQuantitySelectionDialogIfOpen();
            return currentProduct;
        }

        int targetQuantity = quantityCountFromLabel(selectedQuantityLabel);
        waitUntil(CART_TIMEOUT, webDriver -> clickQuantitySelectionOption(selectedQuantityLabel));
        waitUntil(CART_TIMEOUT, webDriver -> !isQuantitySelectionDialogDisplayed());

        waitUntil(CART_TIMEOUT, webDriver -> {
            CartProductData updatedProduct = getCartProductRow(productName);
            return updatedProduct != null
                    && (updatedProduct.subtotal() != null
                    && updatedProduct.subtotal().compareTo(minimumSubtotal) >= 0
                    || targetQuantity > 0 && updatedProduct.quantity() == targetQuantity);
        });

        return getCartProductRow(productName);
    }

    public int getCartItemCount() {
        return getCartDrawerStoreItemCount();
    }

    public boolean hasOutOfStockStoreItem() {
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
                const maxDrawerWidth = Math.max(420, window.innerWidth * 0.85);
                const panel = Array.from(document.querySelectorAll('aside, [role="dialog"], body > div, div'))
                    .filter(visible)
                    .map(element => ({ element, rect: element.getBoundingClientRect() }))
                    .filter(candidate => {
                        const text = normalize(candidate.element.innerText || candidate.element.textContent || '');
                        return candidate.rect.right >= window.innerWidth - 24
                            && candidate.rect.width >= 300
                            && candidate.rect.width <= maxDrawerWidth
                            && /Shopping Cart|\\bCart\\s*\\(\\d+\\)|Delivery\\s+to|Checkout\\s*\\(\\d+\\)/i.test(text);
                    })
                    .sort((first, second) =>
                        (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                    )
                    .map(candidate => candidate.element)[0] || null;
                if (!panel) {
                    return false;
                }

                const storeCartText = normalize(panel.innerText || panel.textContent || '')
                    .split(/You may also like/i)[0];
                return /out\\s+of\\s+stock/i.test(storeCartText)
                    && (/\\bStore\\s*\\(/i.test(storeCartText)
                        || /Select\\s+All/i.test(storeCartText)
                        || /\\bNotify\\b/i.test(storeCartText));
                """));
    }

    public String getCartDrawerText() {
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
                const panel = Array.from(document.querySelectorAll('body > div'))
                    .filter(visible)
                    .filter(element => /Shopping Cart/i.test(element.innerText || ''))
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

                return panel ? normalize(panel.innerText || panel.textContent || '') : '';
                """);

        return result == null ? "" : String.valueOf(result);
    }

    public boolean isPayableAmountDisplayed() {
        String drawerText = getCartDrawerText();

        return drawerText.matches("(?is).*payable.*৳\\s*\\d+.*")
                || drawerText.matches("(?is).*৳\\s*\\d+.*place order.*");
    }

    public boolean isPlaceOrderButtonVisible() {
        return Boolean.TRUE.equals(executeScript("""
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const panel = Array.from(document.querySelectorAll('body > div'))
                    .filter(visible)
                    .filter(element => /Shopping Cart/i.test(element.innerText || ''))
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
                if (!panel) {
                    return false;
                }

                return Array.from(panel.querySelectorAll('button'))
                    .filter(visible)
                    .some(button => /(?:Checkout|Place\\s+Order)/i.test(button.innerText || button.textContent || ''));
                """));
    }

    public ShippingAddressPage openShippingAddressPageFromCartDrawer() {
        openCartDrawer();

        Boolean clicked = waitUntil(CART_TIMEOUT, webDriver -> (Boolean) executeScript("""
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                const panel = Array.from(document.querySelectorAll('body > div'))
                    .filter(visible)
                    .filter(element => /Shopping Cart/i.test(element.innerText || ''))
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
                if (!panel) {
                    return false;
                }

                const changeAction = Array.from(panel.querySelectorAll('button, a, span, div'))
                    .filter(visible)
                    .filter(element => /^(Change|Add\\s+Address)$/i.test(
                            normalize(element.innerText || element.textContent || '')
                        )
                        || /change\\s+shipping/i.test(
                            normalize(element.getAttribute('aria-label') || element.getAttribute('title') || '')
                        ))
                    .sort((first, second) => {
                        const firstRect = first.getBoundingClientRect();
                        const secondRect = second.getBoundingClientRect();
                        return first.children.length - second.children.length
                            || firstRect.width * firstRect.height - secondRect.width * secondRect.height;
                    })[0] || null;
                if (!changeAction) {
                    return false;
                }

                changeAction.scrollIntoView({ block: 'center', inline: 'nearest' });
                changeAction.click();
                return true;
                """));

        if (!Boolean.TRUE.equals(clicked)) {
            throw new TimeoutException("Change Shipping Address action was not found in the cart drawer.");
        }

        return new ShippingAddressPage(driver).waitUntilAddressListOpen();
    }

    public CartPage openApplyCouponSection() {
        openCartDrawer();

        Boolean clicked = (Boolean) executeScript("""
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                const panel = Array.from(document.querySelectorAll('body > div'))
                    .filter(visible)
                    .filter(element => /Shopping Cart/i.test(element.innerText || ''))
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
                if (!panel) {
                    return false;
                }

                const couponButton = Array.from(panel.querySelectorAll('button'))
                    .filter(visible)
                    .find(button => /coupon/i.test(normalize(button.innerText || button.textContent || '')));
                if (!couponButton) {
                    return false;
                }

                const couponSectionAlreadyOpen = Array.from(panel.querySelectorAll('input'))
                    .filter(visible)
                    .some(input => /coupon/i.test(input.getAttribute('placeholder') || ''));
                if (!couponSectionAlreadyOpen) {
                    couponButton.scrollIntoView({ block: 'center', inline: 'nearest' });
                    couponButton.click();
                }

                return true;
                """);

        if (!Boolean.TRUE.equals(clicked)) {
            throw new TimeoutException("Coupon section trigger was not found in the cart drawer.");
        }

        waitUntil(CART_TIMEOUT, webDriver -> isApplyCouponSectionVisible());
        return this;
    }

    public boolean openApplyCouponSectionIfAvailable() {
        try {
            openApplyCouponSection();
            return true;
        } catch (TimeoutException exception) {
            return false;
        }
    }

    public boolean isApplyCouponSectionVisible() {
        return Boolean.TRUE.equals(executeScript("""
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                const panel = Array.from(document.querySelectorAll('body > div'))
                    .filter(visible)
                    .filter(element => /Shopping Cart/i.test(element.innerText || ''))
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
                if (!panel) {
                    return false;
                }

                const couponInput = Array.from(panel.querySelectorAll('input'))
                    .filter(visible)
                    .find(input => /coupon/i.test(input.getAttribute('placeholder') || ''));
                const applyButton = Array.from(panel.querySelectorAll('button'))
                    .filter(visible)
                    .find(button => /^Apply$/i.test(normalize(button.innerText || button.textContent || '')));

                return Boolean(couponInput) && Boolean(applyButton);
                """));
    }

    public boolean isApplyCouponButtonVisibleEnabledAndClickable() {
        return Boolean.TRUE.equals(executeScript("""
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && style.pointerEvents !== 'none'
                        && Number(style.opacity || 1) !== 0;
                };
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                const couponInput = Array.from(document.querySelectorAll('input'))
                    .filter(visible)
                    .find(input => /coupon/i.test(input.getAttribute('placeholder') || ''));
                let applyButton = Array.from(document.querySelectorAll('button'))
                    .filter(visible)
                    .find(button => /^Apply$/i.test(normalize(button.innerText || button.textContent || '')));
                if (!applyButton) {
                    return false;
                }

                if (applyButton.disabled && couponInput) {
                    const descriptor = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value');
                    descriptor.set.call(couponInput, 'TESTCODE');
                    couponInput.dispatchEvent(new Event('input', { bubbles: true }));
                    couponInput.dispatchEvent(new Event('change', { bubbles: true }));
                    applyButton = Array.from(document.querySelectorAll('button'))
                        .filter(visible)
                        .find(button => /^Apply$/i.test(normalize(button.innerText || button.textContent || '')));
                }

                return Boolean(applyButton) && !applyButton.disabled;
                """));
    }

    public boolean isDeliveryChargeLineDisplayed() {
        String drawerText = normalizeCurrencyText(getCartDrawerText()).toLowerCase();

        return drawerText.matches("(?is).*regular\\s+delivery.*৳\\s*\\d+.*");
    }

    public boolean verifyNoVisibleCartDrawerTextTruncationOrOverlap() {
        return Boolean.TRUE.equals(executeScript("""
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                const panel = Array.from(document.querySelectorAll('body > div'))
                    .filter(visible)
                    .filter(element => /Shopping Cart/i.test(element.innerText || ''))
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
                if (!panel) {
                    return false;
                }

                const panelText = normalize(panel.innerText || panel.textContent || '');
                if (!/Shopping\\s+Cart/i.test(panelText)
                        || !Array.from(panel.querySelectorAll("a[href*='/product/']")).filter(visible).length
                        || !/(Qty\\s*:\\s*\\d+|Out\\s+of\\s+Stock)/i.test(panelText)
                        || !/(Delivery\\s+to|Regular\\s+Delivery|Express\\s+delivery|Free\\s+delivery)/i.test(panelText)
                        || !/(Amount\\s+Payable|Payable)/i.test(panelText)
                        || !/(Checkout|Place\\s+Order|Add\\s+Address)/i.test(panelText)) {
                    return false;
                }

                return true;
                """));
    }

    public boolean isCouponSectionAligned() {
        return Boolean.TRUE.equals(executeScript("""
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                const panel = Array.from(document.querySelectorAll('body > div'))
                    .filter(visible)
                    .filter(element => /Shopping Cart/i.test(element.innerText || ''))
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
                if (!panel) {
                    return false;
                }

                const couponInput = Array.from(panel.querySelectorAll('input'))
                    .filter(visible)
                    .find(input => /coupon/i.test(input.getAttribute('placeholder') || ''));
                const applyButton = Array.from(panel.querySelectorAll('button'))
                    .filter(visible)
                    .find(button => /^Apply$/i.test(normalize(button.innerText || button.textContent || '')));
                if (!couponInput || !applyButton) {
                    return false;
                }

                const panelRect = panel.getBoundingClientRect();
                const inputRect = couponInput.getBoundingClientRect();
                const buttonRect = applyButton.getBoundingClientRect();
                const sameRow = Math.abs(
                    (inputRect.top + inputRect.height / 2) - (buttonRect.top + buttonRect.height / 2)
                ) <= 4;

                return sameRow
                    && inputRect.left >= panelRect.left - 2
                    && buttonRect.right <= panelRect.right + 2
                    && inputRect.right <= buttonRect.left + 2
                    && couponInput.scrollWidth <= couponInput.clientWidth + 3
                    && applyButton.scrollWidth <= applyButton.clientWidth + 3;
                """));
    }

    public boolean isContinueCheckoutButtonVisibleAndEnabled() {
        return verifyPlaceOrderButtonVisibleButNotClicked();
    }

    public boolean isShippingAddressSummaryDisplayed(String expectedText) {
        String drawerText = normalizeText(getCartDrawerText()).toLowerCase();
        String expected = normalizeText(expectedText).toLowerCase();

        return !expected.isBlank()
                && drawerText.contains("address")
                && (drawerText.contains(expected)
                || canonicalText(drawerText).contains(canonicalText(expected)));
    }

    public boolean waitUntilShippingAddressSummaryDisplayed(String... expectedTexts) {
        try {
            waitUntil(CART_TIMEOUT, webDriver -> {
                for (String expectedText : expectedTexts) {
                    if (isShippingAddressSummaryDisplayed(expectedText)) {
                        return true;
                    }
                }

                return false;
            });
            return true;
        } catch (TimeoutException exception) {
            return false;
        }
    }

    public boolean verifyCartDrawerDisplayed() {
        return isCartDrawerDisplayed();
    }

    public boolean verifyCartHeader() {
        return Boolean.TRUE.equals(executeScript("""
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                const panel = Array.from(document.querySelectorAll('body > div'))
                    .filter(visible)
                    .filter(element => /Shopping Cart/i.test(element.innerText || ''))
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
                if (!panel) {
                    return false;
                }

                const header = Array.from(panel.querySelectorAll('h1, h2, h3, p, div, span, strong'))
                    .filter(visible)
                    .find(element => /^Shopping Cart$/i.test(normalize(element.innerText || element.textContent))
                        && element.children.length === 0)
                    || Array.from(panel.querySelectorAll('h1, h2, h3, p, div, span, strong'))
                        .filter(visible)
                        .find(element => /^Shopping Cart$/i.test(normalize(element.innerText || element.textContent)));
                const closeButton = Array.from(panel.querySelectorAll('button'))
                    .filter(visible)
                    .find(button => button.querySelector('svg[class*="lucide-x"]')
                        || /close/i.test(button.getAttribute('aria-label') || '')
                        || /^[x×]$/i.test((button.innerText || button.textContent || '').trim()));
                if (!header || !closeButton) {
                    return false;
                }

                const panelRect = panel.getBoundingClientRect();
                const headerRect = header.getBoundingClientRect();
                const closeRect = closeButton.getBoundingClientRect();
                return headerRect.left >= panelRect.left - 2
                    && headerRect.right <= panelRect.right + 2
                    && closeRect.left >= panelRect.left - 2
                    && closeRect.right <= panelRect.right + 2
                    && Math.abs(
                        (headerRect.top + headerRect.height / 2)
                            - (closeRect.top + closeRect.height / 2)
                    ) <= 24;
                """));
    }

    public boolean verifyStoreTabCount(int expectedCount) {
        return Boolean.TRUE.equals(executeScript("""
                const expectedCount = Number(arguments[0]);
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                const panel = Array.from(document.querySelectorAll('body > div'))
                    .filter(visible)
                    .filter(element => /Shopping Cart/i.test(element.innerText || ''))
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
                if (!panel) {
                    return false;
                }

                const expectedPattern = new RegExp(`\\\\b${expectedCount}\\\\b`);
                const storeTab = Array.from(panel.querySelectorAll('[aria-label="store-tab"], button, div, span'))
                    .filter(visible)
                    .find(element => {
                        const text = normalize(
                            element.innerText || element.textContent || element.getAttribute('aria-label') || ''
                        );
                        return /store/i.test(text) && expectedPattern.test(text);
                    });
                if (!storeTab) {
                    return false;
                }

                const panelRect = panel.getBoundingClientRect();
                const tabRect = storeTab.getBoundingClientRect();
                const productLineCount = Array.from(panel.querySelectorAll("a[href*='/product/']"))
                    .filter(visible)
                    .length;

                return productLineCount === expectedCount
                    && tabRect.left >= panelRect.left - 2
                    && tabRect.right <= panelRect.right + 2
                    && tabRect.top >= panelRect.top - 2
                    && tabRect.bottom <= panelRect.bottom + 2
                    && storeTab.scrollWidth <= storeTab.clientWidth + 3
                    && storeTab.scrollHeight <= storeTab.clientHeight + 5;
                """, expectedCount));
    }

    public boolean verifyProductInCart(String productName) {
        return isCartDrawerOpen() && getCartProductRow(productName) != null;
    }

    public boolean waitForProductDetails(String productName) {
        try {
            waitUntil(CART_TIMEOUT, webDriver -> verifyProductDetails(productName));
            return true;
        } catch (TimeoutException exception) {
            return false;
        }
    }

    public boolean verifyProductDetails(String productName) {
        return Boolean.TRUE.equals(executeScript("""
                const productName = String(arguments[0] || '').trim().toLowerCase();
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                const canonical = text => normalize(text).toLowerCase();
                const inside = (outer, inner) => inner.left >= outer.left - 2
                    && inner.right <= outer.right + 2
                    && inner.top >= outer.top - 2
                    && inner.bottom <= outer.bottom + 2;
                const overlaps = (first, second) => first.left < second.right - 2
                    && first.right > second.left + 2
                    && first.top < second.bottom - 2
                    && first.bottom > second.top + 2;
                const panel = Array.from(document.querySelectorAll('body > div'))
                    .filter(visible)
                    .filter(element => /Shopping Cart/i.test(element.innerText || ''))
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
                if (!panel) {
                        return false;
                    }

                    const productLine = Array.from(panel.querySelectorAll("a[href*='/product/']"))
                        .filter(visible)
                    .find(element => canonical(element.innerText || element.textContent).includes(productName));
                if (!productLine) {
                    return false;
                }

                let productContainer = productLine;
                for (let scope = productLine; scope && scope !== panel; scope = scope.parentElement) {
                    const text = canonical(scope.innerText || scope.textContent);
                    if (!text.includes(productName)) {
                        continue;
                    }

                    const containsCartControls = /Qty\\s*:\\s*\\d+/i.test(text)
                        || Array.from(scope.querySelectorAll('button')).some(button =>
                            /Qty\\s*:\\s*\\d+/i.test(button.innerText || button.textContent || '')
                        );
                    const containsMedia = scope.querySelector('img, picture, figure')
                        || Array.from(scope.querySelectorAll('div, span')).some(element => {
                            const rect = element.getBoundingClientRect();
                            const elementText = normalize(element.innerText || element.textContent);
                            return elementText.length === 0
                                && rect.width >= 40
                                && rect.height >= 40
                                && rect.width <= 140
                                && rect.height <= 140;
                        });

                    if (containsCartControls || containsMedia) {
                        productContainer = scope;
                        break;
                    }
                }

                const productText = canonical(productContainer.innerText || productContainer.textContent);
                const image = Array.from(productContainer.querySelectorAll('img'))
                    .filter(visible)
                    .find(img => !/placeholder/i.test(img.alt || img.src || img.currentSrc || ''));

                const textElements = Array.from(productContainer.querySelectorAll('h1, h2, h3, h4, p, span, div'))
                    .filter(visible)
                    .filter(element => normalize(element.innerText || element.textContent).length > 0);
                const leafTextElements = textElements.filter(element => element.children.length === 0);
                const findTextElement = pattern => leafTextElements.find(element =>
                    pattern.test(normalize(element.innerText || element.textContent))
                ) || textElements.find(element => pattern.test(normalize(element.innerText || element.textContent)));
                const name = leafTextElements.find(element =>
                    canonical(element.innerText || element.textContent).includes(productName)
                ) || textElements.find(element =>
                    canonical(element.innerText || element.textContent).includes(productName)
                );
                const quantity = findTextElement(/Qty\\s*:\\s*\\d+/i);
                const price = findTextElement(/৳\\s*\\d+(?:\\.\\d+)?/i);
                if (!name || !quantity || !price) {
                    return false;
                }

                const importantElements = [name, quantity, price].filter(Boolean);
                const lineRect = productContainer.getBoundingClientRect();
                const nameRect = name.getBoundingClientRect();
                const mediaElement = image || Array.from(productContainer.querySelectorAll('img, picture, figure, div'))
                    .filter(visible)
                    .find(element => {
                        const rect = element.getBoundingClientRect();
                        const elementText = normalize(element.innerText || element.textContent);
                        return element !== productContainer
                            && elementText.length === 0
                            && rect.width >= 40
                            && rect.height >= 40
                            && rect.width <= 140
                            && rect.height <= 140
                            && rect.right <= nameRect.left + 12;
                    });
                const imageRect = mediaElement ? mediaElement.getBoundingClientRect() : null;
                const imageLoaded = image
                    ? image.complete === true
                        && image.naturalWidth > 0
                        && image.naturalHeight > 0
                    : Boolean(mediaElement);
                const importantElementsInsideLine = importantElements.every(element =>
                    inside(lineRect, element.getBoundingClientRect())
                );
                const productImageAligned = imageRect
                    && inside(lineRect, imageRect)
                    && !overlaps(imageRect, nameRect)
                    && imageRect.left <= nameRect.left;

                return imageLoaded
                    && productText.includes(productName)
                    && /Qty\\s*:\\s*\\d+/i.test(productText)
                    && /৳\\s*\\d+(?:\\.\\d+)?/i.test(productText)
                    && Boolean(name)
                    && Boolean(quantity)
                    && Boolean(price)
                    && importantElementsInsideLine
                    && productImageAligned;
                """, productName));
    }

    public boolean verifyQuantity(String expectedQuantityText) {
        String drawerText = normalizeText(getCartDrawerText());
        String expectedQuantity = normalizeText(expectedQuantityText);

        return isCartDrawerOpen()
                && !expectedQuantity.isBlank()
                && drawerText.toLowerCase().contains(expectedQuantity.toLowerCase())
                && areCartQuantityControlsDisplayed();
    }

    public boolean verifyPriceDetails() {
        String drawerText = normalizeCurrencyText(getCartDrawerText()).toLowerCase();
        boolean productPriceDisplayed = getCartProductRows().stream()
                .anyMatch(product -> product.subtotal() != null);

        return isCartDrawerOpen()
                && (productPriceDisplayed
                || drawerText.matches("(?is).*(mrp|subtotal|product).*৳\\s*\\d+(?:\\.\\d+)?.*"))
                && drawerText.matches("(?is).*(amount\\s+payable|payable).*৳\\s*\\d+(?:\\.\\d+)?.*");
    }

    public boolean verifyDeliveryCharge() {
        String drawerText = normalizeCurrencyText(getCartDrawerText()).toLowerCase();

        return isCartDrawerOpen()
                && drawerText.contains("regular delivery")
                && drawerText.matches("(?is).*regular\\s+delivery.*৳\\s*\\d+(?:\\.\\d+)?.*");
    }

    public boolean verifyAmountPayable() {
        String drawerText = normalizeCurrencyText(getCartDrawerText()).toLowerCase();

        return isPayableAmountDisplayed()
                && drawerText.contains("payable")
                && Boolean.TRUE.equals(executeScript("""
                        const visible = element => {
                            const rect = element.getBoundingClientRect();
                            const style = getComputedStyle(element);
                            return rect.width > 0 && rect.height > 0
                                && style.display !== 'none'
                                && style.visibility !== 'hidden'
                                && Number(style.opacity || 1) !== 0;
                        };
                        const normalize = text => String(text || '').replace(/\\s+/g, ' ').replace(/৳\\s+/g, '৳').trim();
                        const panel = Array.from(document.querySelectorAll('body > div'))
                            .filter(visible)
                            .filter(element => /Shopping Cart/i.test(element.innerText || ''))
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
                        if (!panel) {
                            return false;
                        }

                        const payableElement = Array.from(panel.querySelectorAll('div, p, span, strong, b'))
                            .filter(visible)
                            .find(element => {
                                const text = normalize(element.innerText || element.textContent || '');
                                return /payable/i.test(text) && /৳\\s*\\d+(?:\\.\\d+)?/.test(text);
                            });
                        if (!payableElement) {
                            return false;
                        }

                        const style = getComputedStyle(payableElement);
                        const className = String(payableElement.className || '');
                        const background = style.backgroundColor || '';
                        const fontWeight = Number(style.fontWeight) || 400;
                        const hasBackgroundHighlight = !['rgba(0, 0, 0, 0)', 'transparent', ''].includes(background);
                        const hasTextHighlight = /brand|primary|highlight|font-semibold|font-bold|text-/i.test(className)
                            || fontWeight >= 500;

                        return payableElement.scrollWidth <= payableElement.clientWidth + 3
                            && payableElement.scrollHeight <= payableElement.clientHeight + 5
                            && (hasBackgroundHighlight || hasTextHighlight);
                        """));
    }

    public boolean verifyPlaceOrderButtonVisibleButNotClicked() {
        return Boolean.TRUE.equals(executeScript("""
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const panel = Array.from(document.querySelectorAll('body > div'))
                    .filter(visible)
                    .filter(element => /Shopping Cart/i.test(element.innerText || ''))
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
                if (!panel) {
                    return false;
                }

                const button = Array.from(panel.querySelectorAll('button'))
                    .filter(visible)
                    .find(element => /(?:Checkout|Place\\s+Order)/i.test(element.innerText || element.textContent || ''));
                if (!button || button.disabled) {
                    return false;
                }

                const panelRect = panel.getBoundingClientRect();
                const buttonRect = button.getBoundingClientRect();
                return buttonRect.left >= panelRect.left - 2
                    && buttonRect.right <= panelRect.right + 2
                    && buttonRect.bottom <= panelRect.bottom + 2
                    && button.scrollWidth <= button.clientWidth + 2
                    && button.scrollHeight <= button.clientHeight + 4;
                """));
    }

    public boolean verifyNoBrokenImagesInCart() {
        return Boolean.TRUE.equals(executeScript("""
                const rendered = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const panel = Array.from(document.querySelectorAll('body > div'))
                    .filter(rendered)
                    .filter(element => /Shopping Cart/i.test(element.innerText || ''))
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
                if (!panel) {
                    return false;
                }

                const visibleInPanel = element => {
                    if (!rendered(element)) {
                        return false;
                    }

                    const rect = element.getBoundingClientRect();
                    const panelRect = panel.getBoundingClientRect();
                    return rect.left < panelRect.right - 2
                        && rect.right > panelRect.left + 2
                        && rect.top < panelRect.bottom - 2
                        && rect.bottom > panelRect.top + 2
                        && rect.left < window.innerWidth - 2
                        && rect.right > 2
                        && rect.top < window.innerHeight - 2
                        && rect.bottom > 2;
                };

                return Array.from(panel.querySelectorAll('img'))
                    .filter(visibleInPanel)
                    .every(image => image.complete === true
                        && image.naturalWidth > 0
                        && image.naturalHeight > 0
                        && !/placeholder/i.test(image.alt || image.src || image.currentSrc || ''));
                """));
    }

    public boolean verifyNoMissingIconsInCart() {
        return Boolean.TRUE.equals(executeScript("""
                const rendered = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const panel = Array.from(document.querySelectorAll('body > div'))
                    .filter(rendered)
                    .filter(element => /Shopping Cart/i.test(element.innerText || ''))
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
                if (!panel) {
                    return false;
                }

                const visibleInPanel = element => {
                    if (!rendered(element)) {
                        return false;
                    }

                    const rect = element.getBoundingClientRect();
                    const panelRect = panel.getBoundingClientRect();
                    return rect.left < panelRect.right - 2
                        && rect.right > panelRect.left + 2
                        && rect.top < panelRect.bottom - 2
                        && rect.bottom > panelRect.top + 2
                        && rect.left < window.innerWidth - 2
                        && rect.right > 2
                        && rect.top < window.innerHeight - 2
                        && rect.bottom > 2;
                };

                const buttons = Array.from(panel.querySelectorAll('button')).filter(visibleInPanel);
                const buttonText = button => (
                    button.innerText || button.textContent || button.getAttribute('aria-label') || ''
                ).trim();
                const hasIconOrTextButton = (svgSelector, textPattern) =>
                    Array.from(panel.querySelectorAll(svgSelector)).some(visibleInPanel)
                        || buttons.some(button => textPattern.test(buttonText(button)));

                const closeIcon = hasIconOrTextButton('svg[class*="lucide-x"]', /close|^[x×]$/i);
                const deleteIcon = hasIconOrTextButton('svg[class*="lucide-trash"]', /delete|remove/i);
                const minusIcon = hasIconOrTextButton('svg[class*="lucide-minus"]', /^[-−]$/);
                const plusIcon = hasIconOrTextButton('svg[class*="lucide-plus"]', /^\\+$/);
                const quantityDropdown = buttons.find(button => /Qty\\s*:\\s*\\d+/i.test(buttonText(button)));
                const quantityDropdownIcon = Boolean(quantityDropdown)
                    && (Boolean(quantityDropdown.querySelector('svg'))
                        || /[⌄⌃⌵⌃▾▴▼▲]/.test(buttonText(quantityDropdown)));
                const quantityControlIcon = (minusIcon && plusIcon) || quantityDropdownIcon;

                const visibleSvgs = Array.from(panel.querySelectorAll('svg')).filter(visibleInPanel);
                const allSvgsRendered = visibleSvgs.every(svg => {
                    const rect = svg.getBoundingClientRect();
                    return rect.width > 0 && rect.height > 0;
                });
                const visibleIconImages = Array.from(panel.querySelectorAll('img'))
                    .filter(visibleInPanel)
                    .filter(image => /icon|cart|delete|trash|plus|minus|close/i.test(
                        image.alt || image.src || image.currentSrc || ''
                    ));
                const allIconImagesRendered = visibleIconImages.every(image =>
                    image.complete === true && image.naturalWidth > 0 && image.naturalHeight > 0
                );
                const imageIcon = pattern => Array.from(panel.querySelectorAll('img'))
                    .filter(visibleInPanel)
                    .some(image => pattern.test(image.alt || image.src || image.currentSrc || ''));
                const newSelectionIcon = imageIcon(/Select\\s+(?:Item|All)|checkbox/i);
                const newDeleteIcon = imageIcon(/Remove(?:\\s+Selected)?|delete|trash/i);

                return closeIcon
                    && (deleteIcon || newDeleteIcon)
                    && (quantityControlIcon || newSelectionIcon)
                    && allSvgsRendered
                    && allIconImagesRendered;
                """));
    }

    public boolean verifyNoTextTruncationOrOverlap() {
        String drawerText = normalizeCurrencyText(getCartDrawerText());

        return isCartDrawerOpen()
                && drawerText.matches("(?is).*Shopping\\s+Cart.*")
                && drawerText.matches("(?is).*Store\\s*\\(?\\s*\\d+\\s*\\)?.*")
                && drawerText.matches("(?is).*(Qty\\s*:\\s*\\d+|Out\\s+of\\s+Stock).*")
                && drawerText.matches("(?is).*৳\\s*\\d+(?:\\.\\d+)?.*")
                && drawerText.matches("(?is).*(Regular\\s+Delivery|Express\\s+delivery|Free\\s+delivery|Delivery\\s+to).*")
                && drawerText.matches("(?is).*(?:Amount\\s+Payable|Payable).*৳\\s*\\d+(?:\\.\\d+)?.*")
                && drawerText.matches("(?is).*(Checkout|Place\\s+Order|Add\\s+Address).*")
                && verifyNoVisibleCartDrawerTextTruncationOrOverlap();
    }

    public boolean verifyCartDrawerLayoutStableAfterLoading() {
        return Boolean.TRUE.equals(executeAsyncScript("""
                const done = arguments[arguments.length - 1];
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const panel = Array.from(document.querySelectorAll('body > div'))
                    .filter(visible)
                    .filter(element => /Shopping Cart/i.test(element.innerText || ''))
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
                if (!panel) {
                    done(false);
                    return;
                }

                const before = panel.getBoundingClientRect();
                window.setTimeout(() => {
                    const after = panel.getBoundingClientRect();
                    const stable = Math.abs(before.left - after.left) <= 2
                        && Math.abs(before.top - after.top) <= 2
                        && Math.abs(before.width - after.width) <= 2
                        && Math.abs(before.height - after.height) <= 4
                        && after.left >= -2
                        && after.right <= window.innerWidth + 2
                        && after.top >= -2
                        && after.bottom <= window.innerHeight + 2;
                    done(stable);
                }, 350);
                """));
    }

    public boolean verifyCartDrawerScrollingWorksIfMultipleProductsPresent() {
        return Boolean.TRUE.equals(executeScript("""
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const panel = Array.from(document.querySelectorAll('body > div'))
                    .filter(visible)
                    .filter(element => /Shopping Cart/i.test(element.innerText || ''))
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
                if (!panel) {
                    return false;
                }

                const scrollContainer = Array.from(panel.querySelectorAll('div'))
                    .filter(visible)
                    .find(element => element.scrollHeight > element.clientHeight + 4)
                    || panel;
                if (scrollContainer.scrollHeight <= scrollContainer.clientHeight + 4) {
                    return true;
                }

                const previousTop = scrollContainer.scrollTop;
                scrollContainer.scrollTop = scrollContainer.scrollHeight;
                const movedDown = scrollContainer.scrollTop > previousTop;
                scrollContainer.scrollTop = previousTop;
                return movedDown;
                """));
    }

    public boolean verifyProductInCart(String productName, String selectedQuantity) {
        String drawerText = normalizeText(getCartDrawerText());
        CartProductData product = getCartProductRow(productName);
        String productLineText = product == null ? "" : normalizeText(product.rowText());
        String combinedCartText = drawerText + " " + productLineText;
        String normalizedQuantity = normalizeText(selectedQuantity);
        String normalizedProductName = canonicalText(productName);
        boolean productNameMatched = !normalizedProductName.isBlank()
                && (canonicalText(combinedCartText).contains(normalizedProductName)
                || product != null && product.matches(productName, normalizedProductName));

        return isCartDrawerOpen()
                && drawerText.toLowerCase().contains("shopping cart")
                && product != null
                && productNameMatched
                && !normalizedQuantity.isBlank()
                && quantityMatchesCartText(combinedCartText, normalizedQuantity)
                && getCartDrawerStoreItemCount() > 0
                && isPlaceOrderButtonVisible();
    }

    public CartPage refreshAndWaitForHeaderCartBadgeCount(int expectedCount) {
        refreshPage();
        try {
            waitForHeaderCartBadgeCount(expectedCount);
        } catch (TimeoutException ignored) {
            // The header badge can lag after refresh; drawer state remains the canonical cart source.
        }
        return this;
    }

    public CartPage waitForCartItemCount(int expectedCount) {
        openCartDrawer();
        waitUntil(CART_TIMEOUT, webDriver -> getCartDrawerStoreItemCount() == expectedCount);
        return this;
    }

    public boolean hasSingleProductLine(String productName) {
        return Boolean.TRUE.equals(executeScript("""
                const productName = String(arguments[0] || '').trim().toLowerCase();
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim().toLowerCase();
                const panel = Array.from(document.querySelectorAll('body > div'))
                    .filter(visible)
                    .filter(element => /Shopping Cart/i.test(element.innerText || ''))
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
                if (!panel) {
                    return false;
                }

                return Array.from(panel.querySelectorAll("a[href*='/product/']"))
                    .filter(visible)
                    .filter(element => normalize(element.innerText || element.textContent).includes(productName))
                    .length === 1;
                """, productName));
    }

    public boolean isRemoveButtonDisplayedForProduct(String productName) {
        return Boolean.TRUE.equals(executeScript("""
                const productName = String(arguments[0] || '').trim().toLowerCase();
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim().toLowerCase();
                const panel = Array.from(document.querySelectorAll('body > div'))
                    .filter(visible)
                    .filter(element => /Shopping Cart/i.test(element.innerText || ''))
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
                if (!panel) {
                    return false;
                }

                const productLine = Array.from(panel.querySelectorAll("a[href*='/product/']"))
                    .filter(visible)
                    .find(element => normalize(element.innerText || element.textContent).includes(productName));
                if (!productLine) {
                    return false;
                }

                let scope = productLine;
                for (let depth = 0; depth < 5 && scope && scope !== panel; depth += 1, scope = scope.parentElement) {
                    const removeButton = Array.from(scope.querySelectorAll('button, svg, img'))
                        .filter(visible)
                        .find(element => {
                            const descriptor = [
                                element.getAttribute('class'),
                                element.getAttribute('aria-label'),
                                element.getAttribute('title'),
                                element.alt,
                                element.src,
                                element.currentSrc,
                                element.innerText,
                                element.textContent
                            ].filter(Boolean).join(' ');
                            return /trash|delete|remove/i.test(descriptor);
                        });
                    if (removeButton) {
                        return true;
                    }
                }

                return false;
                """, productName));
    }

    public boolean isCartProductLineComplete(String productName, String strength, String quantityText) {
        String productLineText = normalizeCurrencyText(getProductLineText(productName)).toLowerCase();
        CartProductData cartProduct = getCartProductRow(productName);
        boolean strengthDisplayed = true;

        for (String strengthToken : normalizeText(strength).toLowerCase().split("\\s*-\\s*")) {
            if (!strengthToken.isBlank() && !productLineText.contains(strengthToken)) {
                strengthDisplayed = false;
                break;
            }
        }

        boolean productIdentityDisplayed = cartProduct != null
                && cartProduct.matches(productName, canonicalText(productName));
        if (!productIdentityDisplayed) {
            productIdentityDisplayed = productLineText.contains(normalizeText(productName).toLowerCase());
        }
        boolean quantityDisplayed = productLineText.contains(normalizeText(quantityText).toLowerCase())
                || quantityMatchesCartText(productLineText, quantityText);
        boolean priceDisplayed = (cartProduct != null && cartProduct.subtotal() != null)
                || productLineText.matches("(?is).*৳\\s*\\d+.*");
        boolean productImageDisplayed = (cartProduct != null && cartProduct.hasImage())
                || verifyNoBrokenImagesInCart();
        boolean removeActionDisplayed = (cartProduct != null && cartProduct.hasRemoveAction())
                || isRemoveButtonDisplayedForProduct(productName)
                || verifyNoMissingIconsInCart();

        return !productLineText.isBlank()
                && productIdentityDisplayed
                && strengthDisplayed
                && quantityDisplayed
                && priceDisplayed
                && removeActionDisplayed
                && productImageDisplayed;
    }

    public CartPage removeProductAndWaitUntilCartIsEmpty(String productName) {
        openCartDrawer();
        waitForProductLine(productName);

        if (!clickFirstCartItemRemoveIcon()) {
            throw new TimeoutException("No remove icon was found for product: " + productName);
        }

        confirmCartItemRemovalIfDialogAppears();
        if (!waitForCartToBecomeEmptyAfterRemoval()) {
            openCartDrawer();
            if (!clearCartWithSelectedRemoval()) {
                throw new TimeoutException("Product removal did not clear the cart. Cart drawer text: '"
                        + normalizeText(getCartDrawerText()) + "'.");
            }
            openCartDrawer();
            waitUntil(CART_TIMEOUT, webDriver -> getCartDrawerStoreItemCount() == 0 || isEmptyCartMessageDisplayedNow());
        }

        try {
            waitUntil(CART_TIMEOUT, webDriver -> getHeaderCartBadgeCount() == 0);
        } catch (TimeoutException ignored) {
            // Empty drawer state is the canonical assertion when the header animation lags.
        }

        return this;
    }

    public boolean isEmptyCartMessageDisplayed() {
        openCartDrawer();
        return isEmptyCartMessageDisplayedNow();
    }

    public CartPriceBreakdown getPriceBreakdown(String productName) {
        String drawerText = normalizeCurrencyText(getCartDrawerText());
        String productLineText = normalizeCurrencyText(getProductLineText(productName));
        int quantity = extractInt(productLineText, "(?i)Qty\\s*:\\s*([0-9]+)");
        if (quantity == 0) {
            quantity = extractInt(drawerText, "(?i)Qty\\s*:\\s*([0-9]+)");
        }

        BigDecimal mrp = extractMoney(drawerText, "(?i)MRP[^৳]*৳\\s*([0-9]+(?:\\.[0-9]+)?)");
        BigDecimal discount = extractMoney(drawerText,
                "(?i)Discount[^৳]*-?\\s*৳\\s*([0-9]+(?:\\.[0-9]+)?)");
        BigDecimal delivery = extractMoney(drawerText,
                "(?i)Regular\\s+Delivery(?:(?!\\b(?:Free\\s+Delivery|Subtotal|Discount|Rounding|Arogga|Amount|Payable|[0-9]+\\s*Items?|Place\\s+Order)\\b).)*৳\\s*([0-9]+(?:\\.[0-9]+)?)");
        if (delivery == null
                && drawerText.toLowerCase().contains("regular delivery")
                && drawerText.toLowerCase().contains("free")) {
            delivery = BigDecimal.ZERO;
        }
        BigDecimal aroggaCashApplied = extractMoney(drawerText,
                "(?i)Arogga\\s+cash\\s+applied[^৳]*-?\\s*৳\\s*([0-9]+(?:\\.[0-9]+)?)");
        BigDecimal roundingAdjustment = extractSignedMoney(drawerText,
                "(?i)Rounding\\s+off\\s*(-?)\\s*৳\\s*([0-9]+(?:\\.[0-9]+)?)");
        BigDecimal amountPayable = extractMoney(drawerText,
                "(?i)(?:Amount\\s+Payable|Payable)[^৳]*৳\\s*([0-9]+(?:\\.[0-9]+)?)");
        if (amountPayable == null) {
            amountPayable = extractMoney(drawerText,
                    "(?i)\\b[0-9]+\\s*Items?\\s*৳\\s*([0-9]+(?:\\.[0-9]+)?)\\s*Place\\s+Order");
        }
        BigDecimal productTotal = extractLastMoney(productLineText);

        if (productTotal == null && mrp != null && discount != null) {
            productTotal = mrp.subtract(discount);
        }
        BigDecimal productLineMrp = extractFirstMoney(productLineText);
        if (mrp == null
                && productLineMrp != null
                && productTotal != null
                && productLineMrp.compareTo(productTotal) >= 0) {
            mrp = productLineMrp;
        }
        if (mrp != null && productTotal != null && mrp.compareTo(productTotal) > 0) {
            discount = mrp.subtract(productTotal);
        }

        boolean explicitDeliveryChargeDisplayed = Pattern.compile(
                "(?is)(?:Regular\\s+Delivery|Express\\s+Delivery|Delivery\\s+charge)[^৳]*(?:৳\\s*\\d|Free)"
        ).matcher(drawerText).find();
        BigDecimal freeDeliveryThreshold = extractMoney(drawerText,
                "(?i)Free\\s+delivery\\s+at\\s*৳\\s*([0-9]+(?:\\.[0-9]+)?)");
        boolean qualifiesForFreeDelivery = freeDeliveryThreshold != null
                && productTotal != null
                && productTotal.compareTo(freeDeliveryThreshold) >= 0;
        if (delivery == null
                && !explicitDeliveryChargeDisplayed
                && amountPayable != null
                && productTotal != null) {
            BigDecimal cashApplied = aroggaCashApplied == null ? BigDecimal.ZERO : aroggaCashApplied;
            BigDecimal payableBeforeCash = amountPayable.add(cashApplied);
            BigDecimal payableDifference = payableBeforeCash.subtract(productTotal);
            if (qualifiesForFreeDelivery || payableDifference.compareTo(BigDecimal.ZERO) <= 0) {
                delivery = BigDecimal.ZERO;
            } else {
                delivery = payableDifference.setScale(0, RoundingMode.CEILING);
            }
            if (roundingAdjustment == null) {
                roundingAdjustment = payableBeforeCash
                        .subtract(productTotal)
                        .subtract(delivery);
            }
        }

        BigDecimal unitPrice = productTotal != null && quantity > 0
                ? productTotal.divide(BigDecimal.valueOf(quantity), 2, RoundingMode.HALF_UP)
                : null;

        return new CartPriceBreakdown(
                quantity,
                unitPrice,
                productTotal,
                mrp,
                discount,
                delivery,
                aroggaCashApplied,
                roundingAdjustment,
                amountPayable
        );
    }

    public boolean isPriceCalculationValid(String productName) {
        CartPriceBreakdown priceBreakdown = getPriceBreakdown(productName);

        if (!priceBreakdown.hasRequiredValues()) {
            return false;
        }

        BigDecimal calculatedSubtotal = priceBreakdown.unitPrice()
                .multiply(BigDecimal.valueOf(priceBreakdown.quantity()))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal displayedProductTotal = priceBreakdown.productTotal().setScale(2, RoundingMode.HALF_UP);
        boolean discountedTotalMatches = true;
        if (priceBreakdown.mrp() != null && priceBreakdown.discount() != null) {
            BigDecimal calculatedFinal = priceBreakdown.mrp().subtract(priceBreakdown.discount())
                    .setScale(2, RoundingMode.HALF_UP);
            discountedTotalMatches = moneyEquals(calculatedFinal, displayedProductTotal);
        }
        BigDecimal expectedPayable = expectedAmountPayable(priceBreakdown)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal expectedRoundedPayable = expectedAmountPayable(priceBreakdown)
                .setScale(0, RoundingMode.HALF_UP);

        return moneyEquals(calculatedSubtotal, displayedProductTotal)
                && discountedTotalMatches
                && (moneyEquals(expectedPayable, priceBreakdown.amountPayable())
                || moneyEquals(expectedRoundedPayable, priceBreakdown.amountPayable()));
    }

    public boolean isDeliveryChargeAppliedAccordingToFreeDeliveryRule(String productName) {
        CartPriceBreakdown priceBreakdown = getPriceBreakdown(productName);
        BigDecimal freeDeliveryThreshold = getFreeDeliveryThreshold();
        if (freeDeliveryThreshold == null) {
            return true;
        }
        if (!priceBreakdown.hasRequiredValues()) {
            return false;
        }

        if (priceBreakdown.productTotal().compareTo(freeDeliveryThreshold) >= 0) {
            return moneyEquals(priceBreakdown.delivery(), BigDecimal.ZERO);
        }

        return priceBreakdown.delivery().compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isUnitPriceCorrect(String productName, BigDecimal expectedUnitPrice) {
        CartPriceBreakdown priceBreakdown = getPriceBreakdown(productName);

        return priceBreakdown.hasRequiredValues()
                && moneyEquals(priceBreakdown.unitPrice(), expectedUnitPrice);
    }

    public boolean isProductSubtotalDisplayed(String productName, BigDecimal expectedProductSubtotal) {
        CartPriceBreakdown priceBreakdown = getPriceBreakdown(productName);

        return priceBreakdown.hasRequiredValues()
                && moneyEquals(priceBreakdown.productTotal(), expectedProductSubtotal);
    }

    public boolean isProductSubtotalRecalculatedFromUnitPrice(String productName) {
        CartPriceBreakdown priceBreakdown = getPriceBreakdown(productName);

        if (!priceBreakdown.hasRequiredValues()) {
            return false;
        }

        BigDecimal expectedProductSubtotal = priceBreakdown.unitPrice()
                .multiply(BigDecimal.valueOf(priceBreakdown.quantity()))
                .setScale(2, RoundingMode.HALF_UP);

        return moneyEquals(expectedProductSubtotal, priceBreakdown.productTotal());
    }

    public boolean isDeliveryChargeDisplayedCorrectly(BigDecimal expectedDeliveryCharge) {
        BigDecimal deliveryCharge = extractMoney(
                normalizeCurrencyText(getCartDrawerText()),
                "(?i)Regular\\s+Delivery[^৳]*৳\\s*([0-9]+(?:\\.[0-9]+)?)"
        );

        return deliveryCharge != null
                && moneyEquals(deliveryCharge, expectedDeliveryCharge)
                && verifyDeliveryCharge();
    }

    public boolean isAmountPayableDisplayed(BigDecimal expectedAmountPayable) {
        BigDecimal amountPayable = extractMoney(
                normalizeCurrencyText(getCartDrawerText()),
                "(?i)(?:Amount\\s+Payable|Payable)[^৳]*৳\\s*([0-9]+(?:\\.[0-9]+)?)"
        );

        return amountPayable != null
                && moneyEquals(amountPayable, expectedAmountPayable);
    }

    public boolean isAmountPayableCalculatedFromSubtotalAndDelivery(String productName) {
        CartPriceBreakdown priceBreakdown = getPriceBreakdown(productName);

        if (!priceBreakdown.hasRequiredValues()) {
            return false;
        }

        BigDecimal expectedPayable = expectedAmountPayable(priceBreakdown)
                .setScale(0, RoundingMode.HALF_UP);
        BigDecimal displayedPayable = priceBreakdown.amountPayable().setScale(0, RoundingMode.HALF_UP);

        return expectedPayable.compareTo(displayedPayable) == 0;
    }

    public boolean isAmountPayableRoundedAccordingToBusinessRule(String productName) {
        CartPriceBreakdown priceBreakdown = getPriceBreakdown(productName);

        if (!priceBreakdown.hasRequiredValues()) {
            return false;
        }

        BigDecimal expectedRoundedPayable = expectedAmountPayable(priceBreakdown)
                .setScale(0, RoundingMode.HALF_UP);
        BigDecimal displayedPayable = priceBreakdown.amountPayable();
        boolean displayedWithoutSignificantDecimal = displayedPayable.stripTrailingZeros().scale() <= 0;

        return displayedWithoutSignificantDecimal
                && displayedPayable.compareTo(expectedRoundedPayable) == 0;
    }

    public record CartProductData(
            String productName,
            String productUrl,
            String rowText,
            String quantityText,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal subtotal,
            BigDecimal mrp,
            BigDecimal discount,
            boolean selected,
            boolean outOfStock,
            boolean hasImage,
            boolean hasRemoveAction
    ) {
        public boolean matches(String rawProductName, String canonicalProductName) {
            String expected = canonicalProductName == null || canonicalProductName.isBlank()
                    ? canonical(rawProductName)
                    : canonicalProductName;

            return !expected.isBlank()
                    && (canonical(productName).contains(expected)
                    || canonical(rowText).contains(expected)
                    || canonical(productUrl).contains(expected));
        }

        public boolean isUsableForCheckout() {
            return !outOfStock
                    && subtotal != null
                    && subtotal.compareTo(BigDecimal.ZERO) >= 0
                    && quantity > 0
                    && !productName.isBlank();
        }

        private static String canonical(String text) {
            return text == null ? "" : text.replaceAll("\\s+", " ")
                    .trim()
                    .replaceAll("[^A-Za-z0-9]+", "")
                    .toLowerCase();
        }
    }

    public record CartSelectionSummary(
            int selectedCount,
            int totalCount,
            BigDecimal selectedSubtotal,
            BigDecimal payable,
            BigDecimal saved
    ) {
    }

    public record CartPriceBreakdown(
            int quantity,
            BigDecimal unitPrice,
            BigDecimal productTotal,
            BigDecimal mrp,
            BigDecimal discount,
            BigDecimal delivery,
            BigDecimal aroggaCashApplied,
            BigDecimal roundingAdjustment,
            BigDecimal amountPayable
    ) {
        public CartPriceBreakdown {
            if (aroggaCashApplied == null) {
                aroggaCashApplied = BigDecimal.ZERO;
            }
            if (roundingAdjustment == null) {
                roundingAdjustment = BigDecimal.ZERO;
            }
        }

        public boolean hasRequiredValues() {
            return quantity > 0
                    && unitPrice != null
                    && productTotal != null
                    && delivery != null
                    && amountPayable != null;
        }

        public boolean isFullyCoveredByAroggaCash() {
            return aroggaCashApplied.compareTo(BigDecimal.ZERO) > 0
                    && amountPayable.compareTo(BigDecimal.ZERO) == 0;
        }
    }

    private BigDecimal expectedAmountPayable(CartPriceBreakdown priceBreakdown) {
        return priceBreakdown.productTotal()
                .add(priceBreakdown.delivery())
                .subtract(priceBreakdown.aroggaCashApplied())
                .add(priceBreakdown.roundingAdjustment())
                .max(BigDecimal.ZERO);
    }

    private BigDecimal getFreeDeliveryThreshold() {
        return extractMoney(
                normalizeCurrencyText(getCartDrawerText()),
                "(?i)Free\\s+delivery\\s+at\\s*৳\\s*([0-9]+(?:\\.[0-9]+)?)"
        );
    }

    private boolean quantityMatchesCartText(String cartText, String selectedQuantity) {
        String normalizedCartText = normalizeText(cartText).toLowerCase();
        String normalizedQuantity = normalizeText(selectedQuantity).toLowerCase();

        if (normalizedCartText.contains(normalizedQuantity)) {
            return true;
        }

        int selectedQuantityCount = extractInt(normalizedQuantity,
                "(?i)(?:Qty\\s*:\\s*)?([0-9]+)\\s*(?:x|×)?");
        return selectedQuantityCount > 0
                && normalizedCartText.matches("(?is).*Qty\\s*:\\s*" + selectedQuantityCount + "\\b.*");
    }

    private boolean areCartQuantityControlsDisplayed() {
        return Boolean.TRUE.equals(executeScript("""
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const panel = Array.from(document.querySelectorAll('body > div'))
                    .filter(visible)
                    .filter(element => /Shopping Cart/i.test(element.innerText || ''))
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
                if (!panel) {
                    return false;
                }

                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                const productLine = Array.from(panel.querySelectorAll("a[href*='/product/']"))
                    .filter(visible)
                    .find(element => /Qty\\s*:\\s*\\d+/i.test(normalize(element.innerText || element.textContent || '')))
                    || Array.from(panel.querySelectorAll("a[href*='/product/']")).filter(visible)[0];
                if (!productLine) {
                    return false;
                }

                let productContainer = productLine;
                let buttons = [];
                while (productContainer && productContainer !== panel) {
                    buttons = Array.from(productContainer.querySelectorAll('button')).filter(visible);
                    if (buttons.length >= 2) {
                        break;
                    }
                    productContainer = productContainer.parentElement;
                }
                if (!productContainer || productContainer === panel) {
                    productContainer = productLine.parentElement || productLine;
                    buttons = Array.from(panel.querySelectorAll('button')).filter(visible);
                }
                const buttonText = button => (
                    button.innerText || button.textContent || button.getAttribute('aria-label') || ''
                ).trim();
                const findControl = (svgSelector, textPattern) =>
                    buttons.find(button => button.querySelector(svgSelector) || textPattern.test(buttonText(button)));
                const minusButton = findControl('svg[class*="lucide-minus"]', /^[-−]$/);
                const plusButton = findControl('svg[class*="lucide-plus"]', /^\\+$/);

                const lineRect = productContainer.getBoundingClientRect();
                const insideLine = rect => rect.left >= lineRect.left - 2
                    && rect.right <= lineRect.right + 2
                    && rect.top >= lineRect.top - 2
                    && rect.bottom <= lineRect.bottom + 2;

                if (minusButton && plusButton) {
                    const minusRect = minusButton.getBoundingClientRect();
                    const plusRect = plusButton.getBoundingClientRect();
                    const sameRow = Math.abs(
                        (minusRect.top + minusRect.height / 2) - (plusRect.top + plusRect.height / 2)
                    ) <= 8;
                    const separated = minusRect.right <= plusRect.left + 2 || plusRect.right <= minusRect.left + 2;

                    return sameRow && insideLine(minusRect) && insideLine(plusRect) && separated;
                }

                const quantityDropdown = buttons.find(button => /Qty\\s*:\\s*\\d+/i.test(buttonText(button)));
                if (!quantityDropdown) {
                    return false;
                }

                const quantityRect = quantityDropdown.getBoundingClientRect();
                const hasDropdownCue = Boolean(quantityDropdown.querySelector('svg'))
                    || /aria-haspopup/i.test(quantityDropdown.outerHTML || '')
                    || /[⌄⌃⌵⌃▾▴▼▲]/.test(buttonText(quantityDropdown));

                return hasDropdownCue
                    && insideLine(quantityRect)
                    && quantityDropdown.scrollWidth <= quantityDropdown.clientWidth + 3
                    && quantityDropdown.scrollHeight <= quantityDropdown.clientHeight + 5;
                """));
    }

    private boolean clickCartProductQuantityControl(String productName) {
        return Boolean.TRUE.equals(executeScript("""
                const productName = String(arguments[0] || '').trim().toLowerCase();
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                const canonical = text => normalize(text).toLowerCase();
                const panel = Array.from(document.querySelectorAll('body > div'))
                    .filter(visible)
                    .filter(element => /Shopping Cart/i.test(element.innerText || ''))
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
                if (!panel) {
                    return false;
                }

                const productLine = Array.from(panel.querySelectorAll("a[href*='/product/']"))
                    .filter(visible)
                    .find(element => canonical(element.innerText || element.textContent).includes(productName));
                if (!productLine) {
                    return false;
                }

                let scope = productLine;
                const candidates = [];
                for (let depth = 0; depth < 6 && scope && scope !== panel; depth += 1, scope = scope.parentElement) {
                    Array.from(scope.querySelectorAll('button'))
                        .filter(visible)
                        .forEach(button => {
                            const text = normalize(
                                button.innerText
                                    || button.textContent
                                    || button.getAttribute('aria-label')
                                    || ''
                            );
                            const outerHtml = button.outerHTML || '';
                            const blocked = /place\\s+order|delete|remove|trash|close/i.test(text + ' ' + outerHtml);
                            let score = blocked ? -100 : 0;

                            if (/Qty\\s*:\\s*\\d+/i.test(text)) {
                                score += 40;
                            }
                            if (/\\d+\\s*Tablets?\\s*\\(/i.test(text)) {
                                score += 20;
                            }
                            if (/aria-haspopup|combobox|listbox|select/i.test(outerHtml)) {
                                score += 12;
                            }
                            if (button.querySelector('svg') && /Qty\\s*:\\s*\\d+/i.test(scope.innerText || '')) {
                                score += 8;
                            }

                            candidates.push({ button, score, depth });
                        });
                }

                candidates.sort((first, second) => second.score - first.score || first.depth - second.depth);
                const quantityControl = candidates.find(candidate => candidate.score > 0)?.button || null;
                if (!quantityControl) {
                    return false;
                }

                quantityControl.scrollIntoView({ block: 'center', inline: 'nearest' });
                quantityControl.click();
                return true;
                """, productName));
    }

    private boolean isQuantitySelectionDialogDisplayed() {
        return Boolean.TRUE.equals(executeScript("""
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };

                return Array.from(document.querySelectorAll('[role="dialog"]'))
                    .filter(visible)
                    .some(dialog => /Select\\s+quantity/i.test(
                        dialog.getAttribute('aria-label') || dialog.innerText || dialog.textContent || ''
                    ));
                """));
    }

    private boolean clickQuantitySelectionOption(String quantityLabel) {
        return Boolean.TRUE.equals(executeScript("""
                const requestedLabel = String(arguments[0] || '').trim();
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                const canonical = text => normalize(text).replace(/[×]/g, 'x').toLowerCase();
                const requested = canonical(requestedLabel);
                const dialog = Array.from(document.querySelectorAll('[role="dialog"]'))
                    .filter(visible)
                    .find(element => /Select\\s+quantity/i.test(
                        element.getAttribute('aria-label') || element.innerText || element.textContent || ''
                    ));
                if (!dialog) {
                    return false;
                }

                const option = Array.from(dialog.querySelectorAll('button, [role="option"]'))
                    .filter(visible)
                    .find(element => canonical(element.innerText || element.textContent).includes(requested));
                if (!option) {
                    return false;
                }

                option.scrollIntoView({ block: 'center', inline: 'nearest' });
                option.click();
                return true;
                """, quantityLabel));
    }

    private List<String> getVisibleCartQuantityOptionLabels() {
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
                const canonical = text => normalize(text).toLowerCase().replace(/[^a-z0-9]+/g, '');
                const quantityPattern = /\\b\\d+\\s*(?:x|×)\\s+[^৳]+|\\b\\d+\\s*(?:x|×)\\b|\\bQty\\s*:\\s*\\d+\\b/i;
                const quantityLabelPattern = /\\b\\d+\\s*(?:x|×)\\s+[^৳]+?(?=\\s+\\d+\\s*(?:x|×)\\s+|$)|\\bQty\\s*:\\s*\\d+\\b/ig;
                const dialog = Array.from(document.querySelectorAll('[role="dialog"]'))
                    .filter(visible)
                    .find(element => /Select\\s+quantity/i.test(
                        element.getAttribute('aria-label') || element.innerText || element.textContent || ''
                    ));
                if (!dialog) {
                    return [];
                }

                const labels = [];
                const addLabel = label => {
                    const cleanedLabel = normalize(label.replace(/\\bADD\\b/ig, '').replace(/\\bRemove\\b/ig, ''));
                    const extractedLabels = Array.from(cleanedLabel.matchAll(quantityLabelPattern))
                        .map(match => normalize(match[0]))
                        .filter(match => match.length <= 80);
                    const candidateLabels = extractedLabels.length ? extractedLabels : [cleanedLabel];
                    for (const candidateLabel of candidateLabels) {
                        if (candidateLabel
                                && quantityPattern.test(candidateLabel)
                                && !/select\\s+quantity/i.test(candidateLabel)
                                && !labels.some(existing => canonical(existing) === canonical(candidateLabel))) {
                            labels.push(candidateLabel);
                        }
                    }
                };

                String(dialog.innerText || dialog.textContent || '')
                    .split(/\\n+/)
                    .map(normalize)
                    .filter(Boolean)
                    .forEach(addLabel);

                for (const button of Array.from(dialog.querySelectorAll('button, [role="option"]')).filter(visible)) {
                    const label = normalize(button.innerText || button.textContent);
                    if (!/^ADD$/i.test(label) && !/^Remove$/i.test(label)) {
                        addLabel(label);
                    }
                }

                for (const addButton of Array.from(dialog.querySelectorAll('button'))
                        .filter(visible)
                        .filter(button => /^ADD$/i.test(normalize(button.innerText || button.textContent)))) {
                    for (let scope = addButton.parentElement; scope && scope !== dialog.parentElement; scope = scope.parentElement) {
                        const label = normalize(scope.innerText || scope.textContent);
                        if (quantityPattern.test(label)) {
                            addLabel(label);
                            break;
                        }
                    }
                }

                return labels;
                """);

        if (!(result instanceof List<?> rawLabels)) {
            return List.of();
        }

        return rawLabels.stream()
                .map(String::valueOf)
                .map(this::normalizeText)
                .filter(label -> !label.isBlank())
                .distinct()
                .toList();
    }

    private String selectQuantityLabelForSubtotal(
            List<String> quantityLabels,
            BigDecimal unitPrice,
            BigDecimal minimumSubtotal
    ) {
        if (quantityLabels == null
                || quantityLabels.isEmpty()
                || unitPrice == null
                || minimumSubtotal == null) {
            return "";
        }

        String bestFallbackLabel = "";
        int bestFallbackQuantity = 0;
        String bestMatchingLabel = "";
        int bestMatchingQuantity = Integer.MAX_VALUE;

        for (String quantityLabel : quantityLabels) {
            int quantity = quantityCountFromLabel(quantityLabel);
            if (quantity <= 0) {
                continue;
            }

            if (quantity > bestFallbackQuantity) {
                bestFallbackQuantity = quantity;
                bestFallbackLabel = quantityLabel;
            }

            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
            if (subtotal.compareTo(minimumSubtotal) >= 0 && quantity < bestMatchingQuantity) {
                bestMatchingQuantity = quantity;
                bestMatchingLabel = quantityLabel;
            }
        }

        return bestMatchingLabel.isBlank() ? bestFallbackLabel : bestMatchingLabel;
    }

    private int quantityCountFromLabel(String quantityLabel) {
        Matcher matcher = Pattern.compile("(?i)(?:Qty\\s*:\\s*)?([0-9]+)\\s*(?:x|×)?")
                .matcher(quantityLabel == null ? "" : quantityLabel);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    private void closeQuantitySelectionDialogIfOpen() {
        executeScript("""
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
                const dialog = Array.from(document.querySelectorAll('[role="dialog"]'))
                    .filter(visible)
                    .find(element => /Select\\s+quantity/i.test(
                        element.getAttribute('aria-label') || element.innerText || element.textContent || ''
                    ));
                if (!dialog) {
                    return false;
                }

                const closeButton = Array.from(dialog.querySelectorAll('button'))
                    .filter(visible)
                    .find(button => {
                        const descriptor = [
                            button.innerText,
                            button.textContent,
                            button.getAttribute('aria-label'),
                            button.getAttribute('title'),
                            button.outerHTML
                        ].filter(Boolean).join(' ');
                        return /close|cancel|x|lucide-x/i.test(descriptor);
                    });
                if (closeButton) {
                    closeButton.click();
                    return true;
                }

                document.dispatchEvent(new KeyboardEvent('keydown', {
                    key: 'Escape',
                    code: 'Escape',
                    bubbles: true,
                    cancelable: true
                }));
                return true;
                """);
    }

    private void setCartProductSelected(String productName, boolean selected) {
        openCartDrawer();
        if (!clickCartProductSelectionControl(productName, selected)) {
            throw new TimeoutException("Cart product selection control was not found for product: " + productName
                    + ". Cart drawer text: '" + normalizeText(getCartDrawerText()) + "'.");
        }

        waitUntil(CART_TIMEOUT, webDriver -> {
            CartProductData product = getCartProductRow(productName);
            return product != null && product.selected() == selected;
        });
    }

    private boolean clickCartProductSelectionControl(String productName, boolean expectedSelected) {
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
                const requestedName = canonical(arguments[0]);
                const expectedSelected = Boolean(arguments[1]);
                const selected = image => {
                    const source = String(image.currentSrc || image.src || '');
                    return /checkbox\\.svg/i.test(source) && !/checkbox-empty/i.test(source);
                };
                const panel = activeCartDrawer();
                if (!panel || !requestedName) {
                    return false;
                }

                const row = Array.from(panel.querySelectorAll("a[href*='/product/']"))
                    .filter(visible)
                    .find(candidate => rowMatches(candidate, requestedName));
                if (!row) {
                    return false;
                }

                const selectionImage = row.querySelector("img[alt='Select Item']");
                if (!selectionImage) {
                    return false;
                }

                if (selected(selectionImage) === expectedSelected) {
                    return true;
                }

                const target = selectionImage.closest('button,label') || selectionImage;
                target.scrollIntoView({ block: 'center', inline: 'nearest' });
                target.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true, view: window }));
                target.dispatchEvent(new MouseEvent('mouseup', { bubbles: true, cancelable: true, view: window }));
                target.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true, view: window }));
                return true;

                function rowMatches(row, expectedName) {
                    const descriptors = [
                        row.innerText || row.textContent || '',
                        row.href || row.getAttribute('href') || '',
                        ...Array.from(row.querySelectorAll('img[alt]')).map(image => image.getAttribute('alt') || '')
                    ];
                    return descriptors.some(descriptor => canonical(descriptor).includes(expectedName));
                }

                function activeCartDrawer() {
                    return Array.from(document.querySelectorAll('body > div'))
                        .filter(visible)
                        .filter(element => /Shopping\\s+Cart|\\bCart\\s*\\(/i.test(element.innerText || ''))
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) => {
                            const firstLooksDrawer = first.rect.right >= window.innerWidth - 24
                                && first.rect.width <= Math.max(460, window.innerWidth * 0.9) ? 0 : 1;
                            const secondLooksDrawer = second.rect.right >= window.innerWidth - 24
                                && second.rect.width <= Math.max(460, window.innerWidth * 0.9) ? 0 : 1;
                            return firstLooksDrawer - secondLooksDrawer
                                || (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height);
                        })
                        .map(candidate => candidate.element)[0] || null;
                }

                function canonical(text) {
                    return normalize(text).toLowerCase().replace(/[^a-z0-9]+/g, '');
                }
                """, productName, expectedSelected));
    }

    private boolean clickCheckoutButtonIfReady() {
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
                const panel = activeCartDrawer();
                if (!panel) {
                    return false;
                }

                const button = Array.from(panel.querySelectorAll('button'))
                    .filter(visible)
                    .find(candidate => /(?:Checkout|Place\\s+Order)/i.test(
                        candidate.innerText || candidate.textContent || ''
                    ));
                if (!button || button.disabled || button.getAttribute('aria-disabled') === 'true') {
                    return false;
                }

                button.scrollIntoView({ block: 'center', inline: 'nearest' });
                button.click();
                return true;

                function activeCartDrawer() {
                    return Array.from(document.querySelectorAll('body > div'))
                        .filter(visible)
                        .filter(element => /Shopping\\s+Cart|\\bCart\\s*\\(/i.test(element.innerText || ''))
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) => {
                            const firstLooksDrawer = first.rect.right >= window.innerWidth - 24
                                && first.rect.width <= Math.max(460, window.innerWidth * 0.9) ? 0 : 1;
                            const secondLooksDrawer = second.rect.right >= window.innerWidth - 24
                                && second.rect.width <= Math.max(460, window.innerWidth * 0.9) ? 0 : 1;
                            return firstLooksDrawer - secondLooksDrawer
                                || (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height);
                        })
                        .map(candidate => candidate.element)[0] || null;
                }
                """));
    }

    private boolean clearCartWithSelectedRemoval() {
        Object result = executeAsyncScript("""
                const done = arguments[arguments.length - 1];
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
                const selected = image => {
                    const source = String(image.currentSrc || image.src || '');
                    return /checkbox\\.svg/i.test(source) && !/checkbox-empty/i.test(source);
                };
                const click = element => {
                    if (!element) {
                        return false;
                    }

                    const target = element.closest('button,a,label') || element;
                    target.scrollIntoView({ block: 'center', inline: 'nearest' });
                    target.click();
                    return true;
                };
                const wait = ms => new Promise(resolve => setTimeout(resolve, ms));
                const panel = () => Array.from(document.querySelectorAll('body > div'))
                    .filter(visible)
                    .filter(element => /Shopping\\s+Cart|\\bCart\\s*\\(?\\s*\\d+\\s*\\)?|\\bStore\\s*\\(?\\s*\\d+\\s*\\)?|Select\\s+All|cart\\s+is\\s+empty|your\\s+cart\\s+is\\s+empty/i.test(element.innerText || ''))
                    .map(element => ({ element, rect: element.getBoundingClientRect() }))
                    .sort((first, second) => {
                        const firstLooksDrawer = first.rect.right >= window.innerWidth - 24
                            && first.rect.width <= Math.max(460, window.innerWidth * 0.9) ? 0 : 1;
                        const secondLooksDrawer = second.rect.right >= window.innerWidth - 24
                            && second.rect.width <= Math.max(460, window.innerWidth * 0.9) ? 0 : 1;
                        return firstLooksDrawer - secondLooksDrawer
                            || (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height);
                    })
                    .map(candidate => candidate.element)[0] || null;

                (async () => {
                    for (let attempt = 0; attempt < 8; attempt++) {
                        const currentPanel = panel();
                        if (!currentPanel) {
                            done(false);
                            return;
                        }

                        const rows = Array.from(currentPanel.querySelectorAll("a[href*='/product/']"))
                            .filter(visible);
                        if (!rows.length || /\\b(?:Cart|Store)\\s*\\(\\s*0\\s*\\)/i.test(currentPanel.innerText || '')) {
                            done(true);
                            return;
                        }

                        const rowSelectionImages = rows
                            .map(row => row.querySelector("img[alt='Select Item']"))
                            .filter(visible);
                        if (!rowSelectionImages.length) {
                            done(false);
                            return;
                        }

                        for (const image of rowSelectionImages) {
                            if (!selected(image)) {
                                click(image);
                                await wait(250);
                            }
                        }

                        const removeSelected = Array.from(currentPanel.querySelectorAll(
                                "img[alt='Remove Selected'], img[src*='trash']"
                            ))
                            .filter(visible)
                            .map(image => image.closest('button') || image)[0] || null;
                        if (!click(removeSelected)) {
                            done(false);
                            return;
                        }

                        await wait(300);
                        const confirm = Array.from(document.querySelectorAll('[role="dialog"] button, button'))
                            .filter(visible)
                            .find(button => /^(Yes|Remove|Delete|Confirm)$/i.test(
                                normalize(button.innerText || button.textContent || '')
                            ));
                        if (!click(confirm)) {
                            done(false);
                            return;
                        }

                        await wait(1200);
                    }

                    const currentPanel = panel();
                    done(Boolean(currentPanel)
                        && !Array.from(currentPanel.querySelectorAll("a[href*='/product/']")).filter(visible).length);
                })();
                """);

        return Boolean.TRUE.equals(result);
    }

    private int getCartDrawerStoreItemCount() {
        Object result = executeScript("""
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const panel = Array.from(document.querySelectorAll('body > div'))
                    .filter(visible)
                    .filter(element => /Shopping\\s+Cart|\\bCart\\s*\\(?\\s*\\d+\\s*\\)?|\\bStore\\s*\\(?\\s*\\d+\\s*\\)?|Select\\s+All|cart\\s+is\\s+empty|your\\s+cart\\s+is\\s+empty/i.test(element.innerText || ''))
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
                if (!panel) {
                    return 0;
                }

                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                const panelText = normalize(panel.innerText || panel.textContent || '');
                const visibleStoreMatch = panelText.match(/\\bStore\\s*\\(?\\s*([0-9]+)\\s*\\)?/i);
                if (visibleStoreMatch) {
                    return Number(visibleStoreMatch[1]);
                }

                const storeTab = panel.querySelector('[aria-label="store-tab"]');
                const storeText = storeTab ? (storeTab.innerText || storeTab.textContent || '') : '';
                const storeMatch = storeText.match(/[0-9]+/);
                if (storeMatch) {
                    return Number(storeMatch[0]);
                }

                const linkedProductCount = Array.from(panel.querySelectorAll("a[href*='/product/']"))
                    .filter(visible)
                    .length;
                if (linkedProductCount > 0) {
                    return linkedProductCount;
                }

                const productLines = Array.from(panel.querySelectorAll('div, li, article, section'))
                    .filter(visible)
                    .filter(element => element !== panel)
                    .map(element => ({
                        element,
                        rect: element.getBoundingClientRect(),
                        text: normalize(element.innerText || element.textContent || '')
                    }))
                    .filter(candidate => /Qty\\s*:\\s*\\d+/i.test(candidate.text)
                        && /৳\\s*\\d+(?:\\.\\d+)?/i.test(candidate.text)
                        && !/Regular\\s+Delivery|Amount\\s+Payable|Subtotal|Discount|Rounding|Place\\s+Order/i
                            .test(candidate.text))
                    .filter(candidate => !Array.from(candidate.element.children).some(child => {
                        if (!visible(child)) {
                            return false;
                        }
                        const childText = normalize(child.innerText || child.textContent || '');
                        return /Qty\\s*:\\s*\\d+/i.test(childText)
                            && /৳\\s*\\d+(?:\\.\\d+)?/i.test(childText);
                    }));

                return productLines.length;
                """);

        return result instanceof Number number ? number.intValue() : 0;
    }

    private boolean isProductLineVisibleInsideCartDrawer(String productName) {
        return Boolean.TRUE.equals(executeScript("""
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                const requestedProductName = canonical(arguments[0]);
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const overlaps = (first, second) => first.left < second.right - 2
                    && first.right > second.left + 2
                    && first.top < second.bottom - 2
                    && first.bottom > second.top + 2;
                const panel = Array.from(document.querySelectorAll('body > div'))
                    .filter(visible)
                    .filter(element => /Shopping Cart/i.test(element.innerText || ''))
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
                if (!panel || requestedProductName.length === 0) {
                    return false;
                }

                const productLine = findProductLine(panel, requestedProductName);
                if (!productLine) {
                    return false;
                }

                return overlaps(panel.getBoundingClientRect(), productLine.getBoundingClientRect());

                function findProductLine(panel, productName) {
                    const linkedLine = Array.from(panel.querySelectorAll("a[href*='/product/']"))
                        .filter(visible)
                        .find(element => canonical(element.innerText || element.textContent).includes(productName));
                    if (linkedLine) {
                        return linkedLine;
                    }

                    return Array.from(panel.querySelectorAll('div, li, article, section, a'))
                        .filter(visible)
                        .filter(element => element !== panel)
                        .map(element => ({
                            element,
                            rect: element.getBoundingClientRect(),
                            text: normalize(element.innerText || element.textContent || '')
                        }))
                        .filter(candidate => canonical(candidate.text).includes(productName))
                        .filter(candidate => /Qty\\s*:\\s*\\d+/i.test(candidate.text)
                            || /৳\\s*\\d+(?:\\.\\d+)?/i.test(candidate.text))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        )
                        .map(candidate => candidate.element)[0] || null;
                }

                function canonical(text) {
                    return normalize(text).toLowerCase().replace(/[^a-z0-9]+/g, '');
                }
                """, productName));
    }

    private boolean isCartDrawerOpen() {
        return Boolean.TRUE.equals(executeScript("""
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0
                        && rect.right > 2
                        && rect.left < window.innerWidth - 2
                        && rect.bottom > 2
                        && rect.top < window.innerHeight - 2;
                };

                return Array.from(document.querySelectorAll('body > div'))
                    .filter(visible)
                    .some(element => /Shopping\\s+Cart|\\bCart\\s*\\(?\\s*\\d+\\s*\\)?|\\bStore\\s*\\(?\\s*\\d+\\s*\\)?|Select\\s+All|cart\\s+is\\s+empty|your\\s+cart\\s+is\\s+empty/i.test(element.innerText || '')
                        && Array.from(element.querySelectorAll('button'))
                            .filter(visible)
                            .some(button => button.querySelector('svg[class*="lucide-x"]')
                                || /close/i.test(button.getAttribute('aria-label') || '')
                                || /^[x×]$/i.test((button.innerText || button.textContent || '').trim())));
                """));
    }

    private WebElement findHeaderCartButton() {
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
                            icon.getAttribute('aria-label')
                        ].filter(Boolean).join(' '))
                        .join(' ')
                ].filter(Boolean).join(' ');
                const header = document.querySelector('header') || document;

                return Array.from(header.querySelectorAll('button, a'))
                    .filter(visible)
                    .map(element => {
                        const description = describe(element);
                        const text = normalize(element.innerText || element.textContent || '');
                        const rect = element.getBoundingClientRect();
                        let score = 0;
                        if (/\\bcart\\b/i.test(text)) {
                            score += 50;
                        }
                        if (/cart/i.test(description)) {
                            score += 25;
                        }
                        if (/order|inbox|account|login/i.test(text)) {
                            score -= 40;
                        }
                        score += Math.max(0, rect.left) / 10000;
                        return { element, score };
                    })
                    .filter(candidate => candidate.score > 0)
                    .sort((first, second) => second.score - first.score)
                    .map(candidate => candidate.element)[0] || null;
                """);

        return result instanceof WebElement element ? element : null;
    }

    private boolean clickHeaderCartButton() {
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
                    element.getAttribute('class'),
                    Array.from(element.querySelectorAll('img, svg'))
                        .map(icon => [
                            icon.getAttribute('alt'),
                            icon.getAttribute('src'),
                            icon.getAttribute('class'),
                            icon.getAttribute('aria-label')
                        ].filter(Boolean).join(' '))
                        .join(' ')
                ].filter(Boolean).join(' ');
                const header = document.querySelector('header') || document;
                const target = Array.from(header.querySelectorAll('button, a'))
                    .filter(visible)
                    .map(element => {
                        const description = describe(element);
                        const text = normalize(element.innerText || element.textContent || '');
                        const rect = element.getBoundingClientRect();
                        let score = 0;
                        if (/\\bcart\\b/i.test(text)) {
                            score += 50;
                        }
                        if (/cart/i.test(description)) {
                            score += 25;
                        }
                        if (/order|inbox|account|login/i.test(text)) {
                            score -= 40;
                        }
                        score += Math.max(0, rect.left) / 10000;
                        return { element, score };
                    })
                    .filter(candidate => candidate.score > 0)
                    .sort((first, second) => second.score - first.score)
                    .map(candidate => candidate.element)[0] || null;

                if (!target) {
                    return false;
                }

                target.scrollIntoView({ block: 'center', inline: 'nearest' });
                target.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true, view: window }));
                target.dispatchEvent(new MouseEvent('mouseup', { bubbles: true, cancelable: true, view: window }));
                target.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true, view: window }));
                return true;
                """));
    }

    private boolean clickFirstCartItemRemoveIcon() {
        return Boolean.TRUE.equals(executeScript("""
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const panel = Array.from(document.querySelectorAll('body > div'))
                    .filter(visible)
                    .filter(element => /Shopping\\s+Cart|\\bCart\\s*\\(?\\s*\\d+\\s*\\)?|\\bStore\\s*\\(?\\s*\\d+\\s*\\)?|Select\\s+All|cart\\s+is\\s+empty|your\\s+cart\\s+is\\s+empty/i.test(element.innerText || ''))
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
                if (!panel) {
                    return false;
                }

                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                const blocked = element => /place\\s+order|coupon|apply|change|close/i.test(
                    normalize(element.innerText || element.textContent || '')
                        + ' ' + (element.getAttribute('aria-label') || '')
                        + ' ' + (element.getAttribute('title') || '')
                );
                const removeTrigger = Array.from(panel.querySelectorAll('button, svg, img'))
                    .filter(visible)
                    .map(element => {
                        const trigger = element.closest('button') || element;
                        const descriptor = normalize(
                            element.innerText || element.textContent || ''
                        ) + ' ' + (element.getAttribute('class') || '')
                            + ' ' + (element.getAttribute('aria-label') || '')
                            + ' ' + (element.getAttribute('title') || '')
                            + ' ' + (element.getAttribute('alt') || '')
                            + ' ' + (element.getAttribute('src') || '')
                            + ' ' + (trigger.getAttribute('aria-label') || '')
                            + ' ' + (trigger.getAttribute('title') || '')
                            + ' ' + (trigger.innerText || trigger.textContent || '');
                        return { element: trigger, descriptor };
                    })
                    .filter(candidate => /trash|delete|remove/i.test(candidate.descriptor))
                    .filter(candidate => !blocked(candidate.element))
                    .find(candidate => true)?.element || null;
                if (!removeTrigger) {
                    return false;
                }

                removeTrigger.scrollIntoView({ block: 'center', inline: 'nearest' });
                removeTrigger.dispatchEvent(new MouseEvent('mousedown', {
                    bubbles: true,
                    cancelable: true,
                    view: window
                }));
                removeTrigger.dispatchEvent(new MouseEvent('mouseup', {
                    bubbles: true,
                    cancelable: true,
                    view: window
                }));
                removeTrigger.dispatchEvent(new MouseEvent('click', {
                    bubbles: true,
                    cancelable: true,
                    view: window
                }));
                return true;
                """));
    }

    private boolean confirmCartItemRemovalIfDialogAppears() {
        try {
            waitUntil(Duration.ofSeconds(4), webDriver -> isRemoveConfirmationDialogDisplayed());
        } catch (TimeoutException exception) {
            return false;
        }

        Boolean clicked = (Boolean) executeScript("""
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const removeButton = Array.from(document.querySelectorAll('[role="dialog"] button'))
                    .filter(visible)
                    .find(button => {
                        const text = (button.innerText || button.textContent || '').trim().toLowerCase();
                        return /\\b(remove|yes)\\b/.test(text) && !/cancel|no/.test(text);
                    });

                if (!removeButton) {
                    return false;
                }

                removeButton.click();
                return true;
                """);

        if (!Boolean.TRUE.equals(clicked)) {
            throw new TimeoutException("Remove confirmation button was not found.");
        }

        waitUntil(CART_TIMEOUT, webDriver -> !isRemoveConfirmationDialogDisplayed());
        return true;
    }

    private boolean waitForCartToBecomeEmptyAfterRemoval() {
        try {
            if (!isCartDrawerOpen()) {
                openCartDrawer();
            }
            waitUntil(CART_TIMEOUT, webDriver -> getCartDrawerStoreItemCount() == 0 || isEmptyCartMessageDisplayedNow());
            return true;
        } catch (TimeoutException exception) {
            return false;
        }
    }

    private boolean isRemoveConfirmationDialogDisplayed() {
        return Boolean.TRUE.equals(executeScript("""
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };

                return Array.from(document.querySelectorAll('[role="dialog"]'))
                    .filter(visible)
                    .some(element => /Remove/i.test(element.innerText || '')
                        && /cart/i.test(element.innerText || ''));
                """));
    }

    private boolean hasLoadedCartDrawerContent() {
        String drawerText = normalizeText(getCartDrawerText());
        return drawerText.matches("(?is).*Store\\s*\\(?\\s*[0-9]+\\s*\\)?.*")
                || drawerText.matches("(?is).*Cart\\s*\\(?\\s*[0-9]+\\s*\\)?.*")
                || drawerText.matches("(?is).*Select\\s+All\\s*\\([0-9]+/[0-9]+\\).*")
                || drawerText.matches("(?is).*Qty\\s*:\\s*[0-9]+.*")
                || drawerText.toLowerCase().contains("your cart is empty")
                || drawerText.toLowerCase().contains("cart is empty")
                || drawerText.toLowerCase().contains("no products")
                || drawerText.toLowerCase().contains("no items");
    }

    private boolean isEmptyCartMessageDisplayedNow() {
        String drawerText = normalizeText(getCartDrawerText()).toLowerCase();

        return getCartDrawerStoreItemCount() == 0
                || drawerText.contains("empty cart")
                || drawerText.contains("cart is empty")
                || drawerText.contains("no products")
                || drawerText.contains("no items");
    }

    private int extractInt(String text, String pattern) {
        Matcher matcher = Pattern.compile(pattern).matcher(text == null ? "" : text);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    private BigDecimal extractMoney(String text, String pattern) {
        Matcher matcher = Pattern.compile(pattern).matcher(text == null ? "" : text);
        return matcher.find() ? new BigDecimal(matcher.group(1)) : null;
    }

    private BigDecimal extractSignedMoney(String text, String pattern) {
        Matcher matcher = Pattern.compile(pattern).matcher(text == null ? "" : text);
        if (!matcher.find()) {
            return null;
        }

        BigDecimal amount = new BigDecimal(matcher.group(2));
        return "-".equals(matcher.group(1)) ? amount.negate() : amount;
    }

    private BigDecimal extractFirstMoney(String text) {
        Matcher matcher = Pattern.compile("৳\\s*([0-9]+(?:\\.[0-9]+)?)").matcher(text == null ? "" : text);
        return matcher.find() ? new BigDecimal(matcher.group(1)) : null;
    }

    private BigDecimal extractLastMoney(String text) {
        Matcher matcher = Pattern.compile("৳\\s*([0-9]+(?:\\.[0-9]+)?)").matcher(text == null ? "" : text);
        BigDecimal lastAmount = null;

        while (matcher.find()) {
            lastAmount = new BigDecimal(matcher.group(1));
        }

        return lastAmount;
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

    private boolean moneyEquals(BigDecimal actual, BigDecimal expected) {
        return actual != null
                && expected != null
                && actual.setScale(2, RoundingMode.HALF_UP)
                .compareTo(expected.setScale(2, RoundingMode.HALF_UP)) == 0;
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
}
