package pages.user;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import utils.TestContext;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AddProductToCartPage extends SearchPage {

    private static final Duration UI_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration SHORT_UI_TIMEOUT = Duration.ofSeconds(4);
    private static final Duration PRODUCT_PROBE_TIMEOUT = Duration.ofSeconds(4);
    private static final Duration TOAST_TIMEOUT = Duration.ofSeconds(12);
    private static final String DEFAULT_SELECTED_QUANTITY = "1 x Bottle";
    private static final int MAX_PRODUCT_PROBE_CANDIDATES = 6;

    private static final By QUANTITY_SELECTOR =
            By.xpath("//*[@role='dialog' and @aria-label='Select quantity']");
    private static final By OUT_OF_STOCK_FILTER =
            By.xpath("//main//label[.//span[normalize-space()='Out of Stock'] or normalize-space()='Out of Stock']");
    private static final By OUT_OF_STOCK_PRODUCT_CARDS =
            By.xpath("//main//a[contains(@href,'/product/') and contains(normalize-space(),'Out Of Stock')]");

    public AddProductToCartPage(WebDriver driver) {
        super(driver);
    }

    public AddProductToCartPage openSearchResultsFor(String baseUrl, String productName) {
        super.openSearchResults(baseUrl, productName);
        waitForProductCard(productName);
        return this;
    }

    public AddProductToCartPage openOutOfStockSearchResults(String baseUrl, String query) {
        super.openSearchResults(baseUrl, query);
        clickWithFallback(OUT_OF_STOCK_FILTER);
        waitUntil(UI_TIMEOUT, webDriver -> displayedElementCount(OUT_OF_STOCK_PRODUCT_CARDS) > 0);
        return this;
    }

    public AddedProduct addProductToCartFromSearchResult() {
        return addProductToCartFromSearchResult(Set.of());
    }

    public AddedProduct addUniqueProductToCartFromSearchResult(Collection<String> excludedProductNames) {
        return addProductToCartFromSearchResult(excludedProductNames);
    }

    private AddedProduct addProductToCartFromSearchResult(Collection<String> excludedProductNames) {
        waitForToastMessagesToClear();
        Set<String> excludedNames = new HashSet<>();
        if (excludedProductNames != null) {
            excludedProductNames.stream()
                    .map(this::canonicalProductName)
                    .filter(name -> !name.isBlank())
                    .forEach(excludedNames::add);
        }

        WebElement addButton;

        try {
            addButton = waitForSearchResultAddButton(excludedNames);
        } catch (TimeoutException exception) {
            return getAlreadyAddedProductFromSearchResult();
        }

        ProductCardSelection selectedProduct = readProductCardSelection(addButton);

        try {
            clickElementWithFallback(addButton);
        } catch (StaleElementReferenceException exception) {
            addButton = waitForSearchResultAddButton(excludedNames);
            selectedProduct = readProductCardSelection(addButton);
            clickElementWithFallback(addButton);
        }

        String selectedQuantity = selectQuantityIfVisible();

        if (selectedQuantity.isBlank()) {
            try {
                selectedQuantity = waitForProductActionToChange(
                        selectedProduct.productName(),
                        selectedProduct.actionText()
                );
            } catch (TimeoutException ignored) {
                selectedQuantity = DEFAULT_SELECTED_QUANTITY;
            }
        }

        waitForAddedProductInCart(selectedProduct, selectedQuantity);
        return new AddedProduct(selectedProduct.productName(), selectedQuantity, false);
    }

    public String selectQuantityIfVisible() {
        return selectQuantityIfVisible(DEFAULT_SELECTED_QUANTITY);
    }

    public String selectQuantityIfVisible(String quantityLabel) {
        if (!waitForQuantitySelectorIfVisible()) {
            return "";
        }

        String selectedQuantity = preferredQuantityLabel(quantityLabel);
        selectQuantity(selectedQuantity);
        return selectedQuantity;
    }

    public boolean isProductCardDisplayed(String productName) {
        return isDisplayed(productCardByName(productName));
    }

    public boolean isProductSummaryDisplayed(
            String productName,
            String strength,
            String company,
            String packSize,
            String price,
            String mrp,
            String discount
    ) {
        String cardText = getProductCardText(productName);

        return cardText.contains(productName)
                && cardText.contains(strength)
                && cardText.contains(company)
                && cardText.contains(packSize)
                && normalizeCurrencyText(cardText).contains(normalizeCurrencyText(price))
                && normalizeCurrencyText(cardText).contains(normalizeCurrencyText(mrp))
                && cardText.contains(discount)
                && "ADD".equalsIgnoreCase(getProductActionText(productName));
    }

    public boolean isAvailableProductCardSummaryDisplayed(String productName) {
        String cardText = normalizeCurrencyText(getProductCardText(productName));

        return isProductCardDisplayed(productName)
                && cardText.toLowerCase().contains(productName.replaceAll("\\s+", " ").trim().toLowerCase())
                && cardText.matches("(?is).*৳\\s*\\d+(?:\\.\\d+)?.*")
                && !cardText.toLowerCase().contains("out of stock")
                && "ADD".equalsIgnoreCase(getProductActionText(productName))
                && isAddToCartButtonVisibleEnabledAndAligned(productName);
    }

    public int getDisplayedProductActionButtonCount(String productName) {
        Object result = executeScript("""
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
                const card = Array.from(document.querySelectorAll("main a[href*='/product/']"))
                    .find(element => normalize(element.innerText || element.textContent).includes(productName));
                if (!card) {
                    return 0;
                }

                return Array.from(card.querySelectorAll('button'))
                    .filter(visible)
                    .filter(button => normalize(button.innerText || button.textContent).length > 0)
                    .length;
                """, productName);

        return result instanceof Number number ? number.intValue() : 0;
    }

    public ProductSearchResult getFirstProductWithWorkingQuantitySelector() {
        return getFirstProductWithWorkingQuantitySelector(Set.of());
    }

    public ProductSearchResult getFirstProductWithWorkingQuantitySelector(Collection<String> excludedProductNames) {
        Set<String> skippedProductNames = canonicalProductNameSet(excludedProductNames);
        List<String> attemptedProductNames = new ArrayList<>();
        int previousCount = -1;
        String searchText = getSubmittedSearchText();
        ensureExplicitSearchResultsPage(searchText);

        for (int attempt = 0; attempt < 6; attempt++) {
            waitForSearchResultsToLoadCompletely(searchText);
            List<ProductSearchResult> products = getVisibleActiveAddButtonProducts();

            for (ProductSearchResult product : products) {
                String canonicalProductName = canonicalProductName(product.productName());
                if (canonicalProductName.isBlank()
                        || skippedProductNames.contains(canonicalProductName)) {
                    continue;
                }

                skippedProductNames.add(canonicalProductName);
                if (attemptedProductNames.size() >= MAX_PRODUCT_PROBE_CANDIDATES) {
                    throw new TimeoutException("Stopped product probing after "
                            + MAX_PRODUCT_PROBE_CANDIDATES + " search-result candidates for keyword '"
                            + searchText + "'. Tried products: " + attemptedProductNames);
                }

                attemptedProductNames.add(product.productName());
                if (tryOpenQuantitySelectorFor(product.productName())) {
                    TestContext.setSelectedProductName(product.productName());
                    reloadSearchResults(searchText);
                    return product;
                }

                reloadSearchResults(searchText);
            }

            int currentCount = getDisplayedProductCount();
            if (currentCount == previousCount || isNoResultsStateDisplayed()) {
                break;
            }

            previousCount = currentCount;
            scrollToBottom();

            try {
                waitUntil(Duration.ofSeconds(8), webDriver -> getDisplayedProductCount() > currentCount
                        || isNoResultsStateDisplayed());
            } catch (TimeoutException ignored) {
                // The final exception below reports which products were tried.
            }
        }

        throw new TimeoutException("No displayed in-stock product opened the quantity selector. Tried products: "
                + attemptedProductNames);
    }

    public ProductDetailsPage selectFirstProductWithWorkingQuantitySelector() {
        return selectFirstProductWithWorkingQuantitySelector(Set.of());
    }

    public ProductDetailsPage selectFirstProductWithWorkingQuantitySelector(Collection<String> excludedProductNames) {
        Set<String> skippedProductNames = canonicalProductNameSet(excludedProductNames);
        List<String> attemptedProductNames = new ArrayList<>();
        int previousCount = -1;
        String searchText = getSubmittedSearchText();
        String searchUrl = ensureExplicitSearchResultsPage(searchText);

        for (int attempt = 0; attempt < 6; attempt++) {
            waitForSearchResultsToLoadCompletely(searchText);
            List<ProductSearchResult> products = getVisibleActiveAddButtonProducts();

            for (ProductSearchResult product : products) {
                String canonicalProductName = canonicalProductName(product.productName());
                if (canonicalProductName.isBlank()
                        || skippedProductNames.contains(canonicalProductName)) {
                    continue;
                }

                skippedProductNames.add(canonicalProductName);
                if (attemptedProductNames.size() >= MAX_PRODUCT_PROBE_CANDIDATES) {
                    throw new TimeoutException("Stopped product-details probing after "
                            + MAX_PRODUCT_PROBE_CANDIDATES + " search-result candidates for keyword '"
                            + searchText + "'. Tried products: " + attemptedProductNames);
                }

                attemptedProductNames.add(product.productName());
                if (!tryOpenQuantitySelectorFor(product.productName())) {
                    reloadSearchResults(searchText);
                    continue;
                }

                openProductDetailsUrl(product.productUrl());
                ProductDetailsPage detailsPage = new ProductDetailsPage(driver).waitUntilLoaded(product.productName());
                if (detailsPage.hasActiveAddToCartButtonForCurrentProduct()) {
                    TestContext.setSelectedProductName(product.productName());
                    return detailsPage;
                }

                driver.get(searchUrl);
                waitUntilSearchPageLoaded();
                waitForSearchResultsToLoadCompletely(searchText);
            }

            int currentCount = getDisplayedProductCount();
            if (currentCount == previousCount || isNoResultsStateDisplayed()) {
                break;
            }

            previousCount = currentCount;
            scrollToBottom();

            try {
                waitUntil(Duration.ofSeconds(8), webDriver -> getDisplayedProductCount() > currentCount
                        || isNoResultsStateDisplayed());
            } catch (TimeoutException ignored) {
                // The final exception below reports which products were tried.
            }
        }

        throw new TimeoutException("No displayed in-stock product had both a working search quantity selector "
                + "and a product-scoped Add to Cart button on its details page. Tried products: "
                + attemptedProductNames);
    }

    private void openProductDetailsUrl(String productUrl) {
        TimeoutException lastTimeout = null;

        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                driver.get(productUrl);
                return;
            } catch (TimeoutException exception) {
                lastTimeout = exception;
                stopPageLoad();
                if (currentUrlSafely().contains("/product/")) {
                    return;
                }
            }
        }

        throw lastTimeout;
    }

    private void stopPageLoad() {
        try {
            executeScript("window.stop();");
        } catch (RuntimeException ignored) {
            // The retry path handles renderer timeouts where JavaScript is temporarily unavailable.
        }
    }

    private String currentUrlSafely() {
        try {
            return driver.getCurrentUrl();
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    @SuppressWarnings("unchecked")
    private List<ProductSearchResult> getVisibleActiveAddButtonProducts() {
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
                const enabled = button => Boolean(button)
                    && !button.disabled
                    && button.getAttribute('aria-disabled') !== 'true'
                    && getComputedStyle(button).pointerEvents !== 'none';
                const extractProductName = productLink => {
                    const textLines = String(productLink.innerText || productLink.textContent || '')
                        .split(/\\n+/)
                        .map(normalize)
                        .filter(Boolean);
                    const visibleName = textLines.find(line =>
                        !/^ADD$/i.test(line)
                            && !/^Add\\s+to\\s+Cart$/i.test(line)
                            && !/^Notify$/i.test(line)
                            && !/out\\s+of\\s+stock/i.test(line)
                            && !/^৳/.test(line)
                            && !/%\\s*OFF$/i.test(line)
                            && !/^(?:\\d+\\s*-\\s*\\d+|\\d+)\\s*(?:hours?|hrs?)$/i.test(line)
                            && !/^prescription\\s+required$/i.test(line)
                    );
                    const imageAlt = Array.from(productLink.querySelectorAll('img[alt]'))
                        .map(image => normalize(image.getAttribute('alt')))
                        .find(alt => alt && !/icon|logo|placeholder|cart|search/i.test(alt));

                    return visibleName || imageAlt || '';
                };
                const actionButtonFor = productLink => {
                    const isAddButton = button => visible(button)
                        && enabled(button)
                        && !button.closest('[role="dialog"]')
                        && normalize(button.innerText || button.textContent).toUpperCase() === 'ADD';

                    for (let scope = productLink; scope && !scope.matches('main'); scope = scope.parentElement) {
                        const scopedButton = Array.from(scope.querySelectorAll('button')).find(isAddButton);
                        if (scopedButton) {
                            return scopedButton;
                        }
                    }

                    const linkRect = productLink.getBoundingClientRect();
                    const linkCenterY = linkRect.top + linkRect.height / 2;
                    return Array.from(document.querySelectorAll('main button'))
                        .filter(isAddButton)
                        .map(button => ({ button, rect: button.getBoundingClientRect() }))
                        .filter(candidate => candidate.rect.left >= linkRect.left - 8)
                        .sort((first, second) =>
                            Math.abs((first.rect.top + first.rect.height / 2) - linkCenterY)
                                - Math.abs((second.rect.top + second.rect.height / 2) - linkCenterY)
                        )
                        .map(candidate => candidate.button)[0] || null;
                };

                return Array.from(document.querySelectorAll("main a[href*='/product/']"))
                    .filter(visible)
                    .map(productLink => {
                        const addButton = actionButtonFor(productLink);
                        if (!addButton) {
                            return null;
                        }

                        return [extractProductName(productLink), productLink.href || productLink.getAttribute('href') || ''];
                    })
                    .filter(product => product && product[0] && product[1]);
                """);

        List<ProductSearchResult> products = new ArrayList<>();
        if (!(result instanceof List<?> rawProducts)) {
            return products;
        }

        for (Object rawProduct : rawProducts) {
            if (!(rawProduct instanceof List<?> product) || product.size() < 2) {
                continue;
            }

            String productName = String.valueOf(product.get(0)).replaceAll("\\s+", " ").trim();
            String productUrl = String.valueOf(product.get(1)).trim();
            if (!productName.isBlank() && !productUrl.isBlank()) {
                products.add(new ProductSearchResult(productName, productUrl, "ADD", true, true, false, true));
            }
        }

        if (products.isEmpty()) {
            getSearchResults().stream()
                    .filter(this::isProductAvailable)
                    .forEach(products::add);
        }

        return products;
    }

    public boolean hasNoEmptySearchState() {
        return !isNoResultsStateDisplayed();
    }

    public boolean isProductImageNonPlaceholder(String productName) {
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
                const card = Array.from(document.querySelectorAll("main a[href*='/product/']"))
                    .find(element => normalize(element.innerText || element.textContent).includes(productName));
                if (!card) {
                    return false;
                }

                const image = Array.from(card.querySelectorAll('img')).filter(visible)
                    .find(candidate => !/icon|logo/i.test(candidate.alt || candidate.src || candidate.currentSrc || ''));
                if (!image) {
                    return false;
                }

                const descriptor = image.alt || image.src || image.currentSrc || '';
                return image.complete === true
                    && image.naturalWidth > 0
                    && image.naturalHeight > 0
                    && !/placeholder|default-product|no-image/i.test(descriptor);
                """, productName));
    }

    public boolean isProductCardLayoutAligned(String productName) {
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
                const card = Array.from(document.querySelectorAll("main a[href*='/product/']"))
                    .find(element => normalize(element.innerText).includes(productName));
                if (!card || !visible(card)) {
                    return false;
                }

                const image = card.querySelector('img[alt]');
                const title = Array.from(card.querySelectorAll('div, h4, p'))
                    .find(element => normalize(element.textContent) === productName);
                const action = Array.from(card.querySelectorAll('button'))
                    .find(button => (button.innerText || button.textContent || '').trim().length > 0);
                if (!image || !title || !action || !visible(image) || !visible(title) || !visible(action)) {
                    return false;
                }

                const cardRect = card.getBoundingClientRect();
                const imageRect = image.getBoundingClientRect();
                const titleRect = title.getBoundingClientRect();
                const actionRect = action.getBoundingClientRect();
                const insideCard = rect => rect.left >= cardRect.left - 2
                    && rect.right <= cardRect.right + 2
                    && rect.top >= cardRect.top - 2
                    && rect.bottom <= cardRect.bottom + 2;
                const overlaps = (first, second) => first.left < second.right - 2
                    && first.right > second.left + 2
                    && first.top < second.bottom - 2
                    && first.bottom > second.top + 2;

                return insideCard(imageRect)
                    && insideCard(titleRect)
                    && insideCard(actionRect)
                    && imageRect.right <= titleRect.left + 24
                    && titleRect.left < actionRect.left
                    && !overlaps(imageRect, actionRect)
                    && cardRect.width > 0
                    && cardRect.height > 0;
                """, productName));
    }

    public boolean isAddToCartButtonVisibleEnabledAndAligned(String productName) {
        By actionButton = productActionButtonByName(productName);

        return isDisplayed(actionButton)
                && isEnabled(actionButton)
                && "ADD".equalsIgnoreCase(getProductActionText(productName))
                && Boolean.TRUE.equals(executeScript("""
                        const productName = String(arguments[0] || '').trim().toLowerCase();
                        const visible = element => {
                            const rect = element.getBoundingClientRect();
                            const style = getComputedStyle(element);
                            return rect.width > 0 && rect.height > 0
                                && style.display !== 'none'
                                && style.visibility !== 'hidden';
                        };
                        const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim().toLowerCase();
                        const card = Array.from(document.querySelectorAll("main a[href*='/product/']"))
                            .find(element => normalize(element.innerText).includes(productName));
                        const button = card
                            ? Array.from(card.querySelectorAll('button'))
                                .find(element => normalize(element.innerText) === 'add')
                            : null;
                        if (!card || !button || !visible(button)) {
                            return false;
                        }

                        const cardRect = card.getBoundingClientRect();
                        const buttonRect = button.getBoundingClientRect();
                        return buttonRect.right <= cardRect.right + 2
                            && buttonRect.left >= cardRect.left
                            && buttonRect.bottom <= cardRect.bottom + 2
                            && button.scrollWidth <= button.clientWidth + 2
                            && button.scrollHeight <= button.clientHeight + 2;
                        """, productName));
    }

    public boolean isProductActionButtonEnabled(String productName) {
        try {
            return waitForProductActionButton(productName).isEnabled();
        } catch (TimeoutException exception) {
            return false;
        }
    }

    public ProductCardBounds getProductCardBounds(String productName) {
        Object result = executeScript("""
                const productName = String(arguments[0] || '').trim().toLowerCase();
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim().toLowerCase();
                const card = Array.from(document.querySelectorAll("main a[href*='/product/']"))
                    .find(element => normalize(element.innerText).includes(productName));
                if (!card) {
                    return [];
                }

                const rect = card.getBoundingClientRect();
                return [
                    rect.left + window.scrollX,
                    rect.top + window.scrollY,
                    rect.width,
                    rect.height
                ];
                """, productName);

        if (result instanceof List<?> values && values.size() == 4) {
            return new ProductCardBounds(
                    asDouble(values.get(0)),
                    asDouble(values.get(1)),
                    asDouble(values.get(2)),
                    asDouble(values.get(3))
            );
        }

        return new ProductCardBounds(0, 0, 0, 0);
    }

    public AddProductToCartPage clickAddToCart(String productName) {
        return openQuantitySelector(productName);
    }

    public AddProductToCartPage openQuantitySelector(String productName) {
        waitForToastMessagesToClear();
        WebElement actionButton = waitForProductActionButton(productName);
        executeScript("arguments[0].scrollIntoView({block: 'center', inline: 'center'});", actionButton);

        try {
            actionButton.click();
        } catch (ElementClickInterceptedException | StaleElementReferenceException exception) {
            executeScript("arguments[0].click();", waitForProductActionButton(productName));
        }

        try {
            waitForQuantitySelector();
        } catch (TimeoutException exception) {
            throw new TimeoutException("Quantity selector did not open for product '" + productName
                    + "'. Current action text: '" + getProductActionText(productName)
                    + "'. The product may be unavailable or stuck loading.", exception);
        }
        return this;
    }

    public AddProductToCartPage waitForQuantitySelector() {
        waitUntil(UI_TIMEOUT, webDriver -> isQuantitySelectorDisplayedCorrectly());
        return this;
    }

    public boolean isQuantitySelectorDisplayedCorrectly() {
        return Boolean.TRUE.equals(executeScript("""
                const quantityPattern = /\\b\\d+\\s*(?:x|×)\\s+[^৳]+|\\b\\d+\\s*(?:x|×)\\b/i;
                const panel = findQuantityPanel();
                if (!panel) {
                    return false;
                }

                const rect = panel.getBoundingClientRect();
                const text = normalize(panel.innerText || panel.textContent || '');
                return rect.width > 0
                    && rect.height > 0
                    && rect.left >= 0
                    && rect.right <= window.innerWidth + 2
                    && /Select\\s+quantity/i.test(text)
                    && quantityPattern.test(text);

                function findQuantityPanel() {
                    const dialog = Array.from(document.querySelectorAll('[role="dialog"], [aria-label="Select quantity"]'))
                        .find(element => visible(element)
                            && /Select\\s+quantity/i.test(element.innerText || element.textContent || ''));
                    if (dialog) {
                        return dialog;
                    }

                    const candidates = Array.from(document.querySelectorAll('body *'))
                        .filter(visible)
                        .map(element => ({
                            element,
                            rect: element.getBoundingClientRect(),
                            text: normalize(element.innerText || element.textContent || '')
                        }))
                        .filter(candidate => /Select\\s+quantity/i.test(candidate.text))
                        .filter(candidate => candidate.rect.width <= Math.max(520, window.innerWidth * 0.8)
                            && candidate.rect.height <= Math.max(620, window.innerHeight * 0.85));

                    return candidates
                        .filter(candidate => quantityPattern.test(candidate.text))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        )
                        .map(candidate => candidate.element)[0] || null;
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
                """));
    }

    public boolean areQuantityOptionsDisplayed(String... quantityLabels) {
        for (String quantityLabel : quantityLabels) {
            if (!isDisplayed(quantityOptionByLabel(quantityLabel))) {
                return false;
            }
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    public List<String> getVisibleQuantityOptionLabels() {
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
                const canonical = text => normalize(text).toLowerCase().replace(/[^a-z0-9]+/g, '');
                const quantityPattern = /\\b\\d+\\s*(?:x|×)\\s+[^৳]+|\\b\\d+\\s*(?:x|×)\\b/i;
                const quantityLabelPattern = /\\b\\d+\\s*(?:x|×)\\s+[^৳]+?(?=\\s+\\d+\\s*(?:x|×)\\s+|$)/ig;
                const panel = findQuantityPanel();
                if (!panel) {
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

                String(panel.innerText || panel.textContent || '')
                    .split(/\\n+/)
                    .map(normalize)
                    .filter(Boolean)
                    .forEach(addLabel);

                for (const button of Array.from(panel.querySelectorAll('button, [role="option"]')).filter(visible)) {
                    const label = normalize(button.innerText || button.textContent);
                    if (!/^ADD$/i.test(label) && !/^Remove$/i.test(label)) {
                        addLabel(label);
                    }
                }

                for (const addButton of Array.from(panel.querySelectorAll('button'))
                        .filter(visible)
                        .filter(button => /^ADD$/i.test(normalize(button.innerText || button.textContent)))) {
                    for (let scope = addButton.parentElement; scope && scope !== panel.parentElement; scope = scope.parentElement) {
                        const label = normalize(scope.innerText || scope.textContent);
                        if (quantityPattern.test(label)) {
                            addLabel(label);
                            break;
                        }
                    }
                }

                return labels;

                function findQuantityPanel() {
                    const dialog = Array.from(document.querySelectorAll('[role="dialog"], [aria-label="Select quantity"]'))
                        .find(element => visible(element)
                            && /Select\\s+quantity/i.test(element.innerText || element.textContent || ''));
                    if (dialog) {
                        return dialog;
                    }

                    const candidates = Array.from(document.querySelectorAll('body *'))
                        .filter(visible)
                        .map(element => ({
                            element,
                            rect: element.getBoundingClientRect(),
                            text: normalize(element.innerText || element.textContent || '')
                        }))
                        .filter(candidate => /Select\\s+quantity/i.test(candidate.text))
                        .filter(candidate => candidate.rect.width <= Math.max(520, window.innerWidth * 0.8)
                            && candidate.rect.height <= Math.max(620, window.innerHeight * 0.85));

                    return candidates
                        .filter(candidate => quantityPattern.test(candidate.text))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        )
                        .map(candidate => candidate.element)[0] || null;
                }
                """);

        if (result instanceof List<?>) {
            return ((List<Object>) result).stream().map(String::valueOf).toList();
        }

        return List.of();
    }

    public boolean isDefaultQuantityOptionDisplayed(String quantityLabel) {
        return isQuantityOptionSelectedOrFirstVisible(quantityLabel);
    }

    public boolean isQuantityOptionSelectedInPopup(String quantityLabel) {
        return isQuantityOptionSelectedOrFirstVisible(quantityLabel);
    }

    public boolean areQuantityOptionsAccessibleWithoutTruncationOrOverlap() {
        return Boolean.TRUE.equals(executeScript("""
                const dialog = document.querySelector('[role="dialog"][aria-label="Select quantity"]');
                if (!dialog) {
                    return false;
                }

                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                const list = dialog.querySelector('ul') || dialog;
                const scrollPort = dialog.querySelector('.overflow-y-auto') || dialog;
                const options = Array.from(list.querySelectorAll('button'))
                    .filter(button => normalize(button.innerText || button.textContent).length > 0);
                if (options.length < 1) {
                    return false;
                }

                const hasNoTruncatedText = options.every(button => {
                    const label = button.querySelector('span') || button;
                    return label.scrollWidth <= label.clientWidth + 2
                        && label.scrollHeight <= label.clientHeight + 2;
                });

                const visibleInScrollPort = element => {
                    const rect = element.getBoundingClientRect();
                    const portRect = scrollPort.getBoundingClientRect();
                    return rect.bottom >= portRect.top
                        && rect.top <= portRect.bottom
                        && rect.width > 0
                        && rect.height > 0;
                };
                const overlaps = (first, second) => first.left < second.right - 2
                    && first.right > second.left + 2
                    && first.top < second.bottom - 2
                    && first.bottom > second.top + 2;

                const visibleOptions = options.filter(visibleInScrollPort);
                const rects = visibleOptions.map(option => option.getBoundingClientRect());
                let hasNoOverlap = true;
                for (let i = 0; i < rects.length; i += 1) {
                    for (let j = i + 1; j < rects.length; j += 1) {
                        if (overlaps(rects[i], rects[j])) {
                            hasNoOverlap = false;
                        }
                    }
                }

                const firstOption = options[0];
                const lastOption = options[options.length - 1];
                firstOption.scrollIntoView({ block: 'nearest' });
                const firstRect = firstOption.getBoundingClientRect();
                const firstPortRect = scrollPort.getBoundingClientRect();
                const firstReachable = firstRect.top >= firstPortRect.top - 2
                    && firstRect.bottom <= firstPortRect.bottom + 2;

                lastOption.scrollIntoView({ block: 'nearest' });
                const lastRect = lastOption.getBoundingClientRect();
                const lastPortRect = scrollPort.getBoundingClientRect();
                const lastReachable = lastRect.top >= lastPortRect.top - 2
                    && lastRect.bottom <= lastPortRect.bottom + 2;

                return hasNoTruncatedText && hasNoOverlap && firstReachable && lastReachable;
                """));
    }

    public AddProductToCartPage selectQuantity(String quantityLabel) {
        waitUntil(UI_TIMEOUT, webDriver -> Boolean.TRUE.equals(executeScript("""
                const requestedLabel = canonical(arguments[0]);
                const panel = findQuantityPanel();
                if (!panel || !requestedLabel) {
                    return false;
                }

                const option = findQuantityOption(panel, requestedLabel);
                if (!option) {
                    return false;
                }

                const clickTarget = option.addButton || option.element;
                clickTarget.scrollIntoView({ block: 'center', inline: 'nearest' });
                clickTarget.click();
                return true;

                function findQuantityOption(panel, requestedLabel) {
                    const options = [];
                    const quantityPattern = /\\b\\d+\\s*(?:x|×)\\s+[^৳]+|\\b\\d+\\s*(?:x|×)\\b/i;

                    for (const button of Array.from(panel.querySelectorAll('button, [role="option"]')).filter(visible)) {
                        const label = normalize(button.innerText || button.textContent);
                        if (!/^ADD$/i.test(label) && !/^Remove$/i.test(label) && quantityPattern.test(label)) {
                            options.push({ label, element: button, addButton: null });
                        }
                    }

                    for (const addButton of Array.from(panel.querySelectorAll('button'))
                            .filter(visible)
                            .filter(button => /^ADD$/i.test(normalize(button.innerText || button.textContent)))) {
                        for (let scope = addButton.parentElement; scope && scope !== panel.parentElement; scope = scope.parentElement) {
                            const label = normalize(scope.innerText || scope.textContent)
                                .replace(/\\bADD\\b/ig, '')
                                .replace(/\\bRemove\\b/ig, '');
                            if (quantityPattern.test(label)) {
                                options.push({ label, element: scope, addButton });
                                break;
                            }
                        }
                    }

                    return options.find(option => {
                        const optionLabel = canonical(option.label);
                        return optionLabel.includes(requestedLabel) || requestedLabel.includes(optionLabel);
                    }) || null;
                }

                function findQuantityPanel() {
                    const dialog = Array.from(document.querySelectorAll('[role="dialog"], [aria-label="Select quantity"]'))
                        .find(element => visible(element)
                            && /Select\\s+quantity/i.test(element.innerText || element.textContent || ''));
                    if (dialog) {
                        return dialog;
                    }

                    const quantityPattern = /\\b\\d+\\s*(?:x|×)\\s+[^৳]+|\\b\\d+\\s*(?:x|×)\\b/i;
                    const candidates = Array.from(document.querySelectorAll('body *'))
                        .filter(visible)
                        .map(element => ({
                            element,
                            rect: element.getBoundingClientRect(),
                            text: normalize(element.innerText || element.textContent || '')
                        }))
                        .filter(candidate => /Select\\s+quantity/i.test(candidate.text))
                        .filter(candidate => candidate.rect.width <= Math.max(520, window.innerWidth * 0.8)
                            && candidate.rect.height <= Math.max(620, window.innerHeight * 0.85));

                    return candidates
                        .filter(candidate => quantityPattern.test(candidate.text))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        )
                        .map(candidate => candidate.element)[0] || null;
                }

                function normalize(text) {
                    return String(text || '').replace(/\\s+/g, ' ').trim();
                }

                function canonical(text) {
                    return normalize(text)
                        .replace(/[×]/g, 'x')
                        .replace(/\\bbot\\b/ig, 'bottle')
                        .toLowerCase()
                        .replace(/[^a-z0-9]+/g, '');
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
                """, quantityLabel)));
        waitUntil(UI_TIMEOUT, webDriver -> !isQuantitySelectorDisplayedCorrectly());
        return this;
    }

    public AddProductToCartPage selectQuantityAndWaitForAddedToast(String quantityLabel) {
        selectQuantity(quantityLabel);
        waitForAddedToCartToast();
        return this;
    }

    public AddProductToCartPage rapidlyClickAddToCart(String productName, int clickCount) {
        waitForToastMessagesToClear();
        WebElement actionButton = waitForProductActionButton(productName);
        String actionText = actionButton.getText().replaceAll("\\s+", " ").trim();
        executeScript("arguments[0].scrollIntoView({block: 'center', inline: 'nearest'});", actionButton);

        int actionClickCount = "ADD".equalsIgnoreCase(actionText) ? Math.max(1, clickCount) : 1;
        for (int index = 0; index < actionClickCount; index++) {
            try {
                clickElementWithFallback(actionButton);
            } catch (ElementClickInterceptedException | StaleElementReferenceException exception) {
                actionButton = waitForProductActionButton(productName);
                executeScript("arguments[0].click();", actionButton);
            }

            if (waitForQuantitySelectorAfterClick()) {
                break;
            }

            actionButton = waitForProductActionButton(productName);
        }

        waitForQuantitySelector();
        return this;
    }

    private boolean waitForQuantitySelectorAfterClick() {
        try {
            waitUntil(Duration.ofSeconds(2), webDriver -> isQuantitySelectorDisplayedCorrectly());
            return true;
        } catch (TimeoutException exception) {
            return false;
        }
    }

    public boolean isSelectedQuantityHighlighted(String productName, String quantityLabel) {
        WebElement actionButton = waitForProductActionButton(productName);
        String buttonClass = actionButton.getAttribute("class");

        return actionTextMatchesQuantity(actionButton.getText(), quantityLabel)
                && buttonClass != null
                && buttonClass.contains("bg-brand")
                && buttonClass.contains("text-white");
    }

    public String getProductActionText(String productName) {
        return waitForProductActionButton(productName).getText()
                .replaceAll("\\s+", " ")
                .trim();
    }

    public boolean isProductActionChangedToQuantity(String productName, String quantityLabel) {
        return actionTextMatchesQuantity(getProductActionText(productName), quantityLabel);
    }

    public AddProductToCartPage waitForProductActionQuantity(String productName, String quantityLabel) {
        try {
            waitUntil(UI_TIMEOUT, webDriver -> isProductActionChangedToQuantity(productName, quantityLabel));
        } catch (TimeoutException exception) {
            throw new TimeoutException("Product action did not show selected quantity '" + quantityLabel
                    + "' for product '" + productName + "'. Current action text: '"
                    + getProductActionText(productName) + "'.", exception);
        }
        return this;
    }

    public AddProductToCartPage waitForAddedToCartToast() {
        waitUntil(TOAST_TIMEOUT, webDriver -> isToastWithTextDisplayed("Added to cart!"));
        return this;
    }

    public boolean isAddedToCartToastDisplayed() {
        return isToastWithTextDisplayed("Added to cart!");
    }

    public int getVisibleToastCount(String expectedText) {
        Object result = executeScript("""
                const expectedText = String(arguments[0] || '').trim().toLowerCase();
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim().toLowerCase();
                const matches = Array.from(document.querySelectorAll(
                    "section[aria-label*='Notifications'] *, [class*='toast'], [class*='Toast'], li"
                ))
                    .filter(visible)
                    .filter(element => normalize(element.innerText || element.textContent).includes(expectedText));

                return matches
                    .filter(element => !matches.some(other => other !== element && other.contains(element)))
                    .length;
                """, expectedText);

        return result instanceof Number number ? number.intValue() : 0;
    }

    public AddProductToCartPage waitForToastToAutoDismiss(String expectedText) {
        waitUntil(Duration.ofSeconds(10), webDriver -> getVisibleToastCount(expectedText) == 0);
        return this;
    }

    public boolean isToastMessageDisplayedWithoutUiDistortion(String expectedText) {
        return isToastWithTextDisplayed(expectedText);
    }

    public boolean hasNoUnexpectedLayoutShift(String productName, ProductCardBounds beforeBounds) {
        ProductCardBounds afterBounds = getProductCardBounds(productName);

        return Math.abs(afterBounds.left() - beforeBounds.left()) <= 8
                && Math.abs(afterBounds.width() - beforeBounds.width()) <= 8
                && Math.abs(afterBounds.height() - beforeBounds.height()) <= 32
                && hasNoProductCardOverlappingUi(productName)
                && hasNoProductCardTextOverflow(productName);
    }

    public boolean isProductCardUpdatedWithoutBrokenOrOverlappingUi(String productName, String selectedQuantity) {
        return isProductActionChangedToQuantity(productName, selectedQuantity)
                && isProductCardLayoutAligned(productName)
                && hasNoProductCardTextOverflow(productName)
                && hasNoProductCardBrokenAssets(productName)
                && hasNoProductCardOverlappingUi(productName);
    }

    public boolean isProductCardResponsiveAfterQuantityChange(String productName) {
        return isProductCardLayoutAligned(productName)
                && hasNoProductCardTextOverflow(productName)
                && hasNoProductCardOverlappingUi(productName);
    }

    public boolean isUpdatedQuantityDisplayedWithoutOverlapOrTruncation(String productName, String quantityLabel) {
        return isProductActionChangedToQuantity(productName, quantityLabel)
                && hasNoProductCardTextOverflow(productName)
                && hasNoProductCardOverlappingUi(productName);
    }

    public boolean isPriceMrpAndDiscountFormattedAndAligned(String productName) {
        return Boolean.TRUE.equals(executeScript("""
                const productName = String(arguments[0] || '').trim().toLowerCase();
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden';
                };
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim().toLowerCase();
                const card = Array.from(document.querySelectorAll("main a[href*='/product/']"))
                    .find(element => normalize(element.innerText).includes(productName));
                if (!card) {
                    return false;
                }

                const price = card.querySelector('.product_single_price_generate');
                const mrp = card.querySelector('.product_single_mrp_generate');
                const priceRow = price ? price.closest('[data-pv-id]') || price.parentElement : null;
                const discount = priceRow
                    ? Array.from(priceRow.querySelectorAll('div, span'))
                        .find(element => /[0-9]+%\\s*OFF/i.test(element.innerText || element.textContent || ''))
                    : null;
                if (!price || !mrp || !discount || !visible(price) || !visible(mrp) || !visible(discount)) {
                    return false;
                }

                const priceText = String(price.innerText || price.textContent || '').replace(/\\s+/g, ' ').trim();
                const mrpText = String(mrp.innerText || mrp.textContent || '').replace(/\\s+/g, ' ').trim();
                const discountText = String(discount.innerText || discount.textContent || '').replace(/\\s+/g, ' ').trim();
                const priceFormatValid = /^৳\\s?[0-9]+(\\.[0-9]{1,2})?$/.test(priceText);
                const mrpFormatValid = /^৳\\s?[0-9]+(\\.[0-9]{1,2})?$/.test(mrpText);
                const discountFormatValid = /^[0-9]+%\\s?OFF$/i.test(discountText);

                const priceRect = price.getBoundingClientRect();
                const mrpRect = mrp.getBoundingClientRect();
                const discountRect = discount.getBoundingClientRect();
                const rowAligned = Math.abs(
                    (priceRect.top + priceRect.height / 2) - (mrpRect.top + mrpRect.height / 2)
                ) <= 8
                    && Math.abs(
                        (priceRect.top + priceRect.height / 2) - (discountRect.top + discountRect.height / 2)
                    ) <= 14;

                return priceFormatValid
                    && mrpFormatValid
                    && discountFormatValid
                    && rowAligned
                    && priceRect.right <= mrpRect.left + 12
                    && mrpRect.right <= discountRect.left + 24;
                """, productName));
    }

    public boolean areProductIconsDisplayedCorrectly(String productName) {
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
                const card = Array.from(document.querySelectorAll("main a[href*='/product/']"))
                    .find(element => normalize(element.innerText).includes(productName));
                if (!card) {
                    return false;
                }

                const images = Array.from(card.querySelectorAll('img')).filter(visible);
                const svgs = Array.from(card.querySelectorAll('svg')).filter(visible);
                const imagesLoaded = images.length > 0
                    && images.every(image => image.complete === true
                        && image.naturalWidth > 0
                        && image.naturalHeight > 0);
                const svgsRendered = svgs.every(svg => {
                    const rect = svg.getBoundingClientRect();
                    return rect.width > 0 && rect.height > 0;
                });

                return imagesLoaded && svgsRendered;
                """, productName));
    }

    public boolean hasNoProductCardBrokenAssets(String productName) {
        return getBrokenProductCardAssetDescriptions(productName).isEmpty();
    }

    @SuppressWarnings("unchecked")
    public List<String> getBrokenProductCardAssetDescriptions(String productName) {
        Object result = executeScript("""
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
                const card = Array.from(document.querySelectorAll("main a[href*='/product/']"))
                    .find(element => normalize(element.innerText).includes(productName));
                if (!card) {
                    return ['product card not found'];
                }

                const broken = [];
                for (const image of Array.from(card.querySelectorAll('img')).filter(visible)) {
                    const descriptor = image.currentSrc || image.src || image.alt || '<empty image source>';
                    if (!image.complete || image.naturalWidth === 0 || image.naturalHeight === 0) {
                        broken.push(descriptor);
                    } else if (/placeholder|default-product|no-image/i.test(descriptor)) {
                        broken.push('placeholder image: ' + descriptor);
                    }
                }

                for (const svg of Array.from(card.querySelectorAll('svg')).filter(visible)) {
                    const rect = svg.getBoundingClientRect();
                    if (rect.width === 0 || rect.height === 0) {
                        broken.push('broken svg icon');
                    }
                }

                return broken;
                """, productName);

        if (result instanceof List<?>) {
            return ((List<Object>) result).stream().map(String::valueOf).toList();
        }

        return new ArrayList<>();
    }

    public String getProductDetailsUrl(String productName) {
        Object result = executeScript("""
                const productName = String(arguments[0] || '').trim().toLowerCase();
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim().toLowerCase();
                const card = Array.from(document.querySelectorAll("main a[href*='/product/']"))
                    .find(element => normalize(element.innerText || element.textContent).includes(productName));
                return card ? card.href : '';
                """, productName);

        return result == null ? "" : String.valueOf(result);
    }

    public ProductDetailsPage openProductDetails(String productName) {
        String detailsUrl = getProductDetailsUrl(productName);

        if (detailsUrl.isBlank()) {
            throw new TimeoutException("Product details URL was not found for product: " + productName);
        }

        driver.get(detailsUrl);
        return new ProductDetailsPage(driver).waitUntilLoaded(productName);
    }

    public boolean hasNoProductCardOverlappingUi(String productName) {
        return Boolean.TRUE.equals(executeScript("""
                const productName = String(arguments[0] || '').trim().toLowerCase();
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden';
                };
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim().toLowerCase();
                const card = Array.from(document.querySelectorAll("main a[href*='/product/']"))
                    .find(element => normalize(element.innerText).includes(productName));
                if (!card) {
                    return false;
                }

                const imageBox = card.children[0];
                const action = Array.from(card.querySelectorAll('button'))
                    .find(button => (button.innerText || button.textContent || '').trim().length > 0);
                const title = Array.from(card.querySelectorAll('div, h4, p'))
                    .find(element => normalize(element.textContent) === productName);
                if (!imageBox || !action || !title || !visible(imageBox) || !visible(action) || !visible(title)) {
                    return false;
                }

                const overlaps = (first, second) => first.left < second.right - 2
                    && first.right > second.left + 2
                    && first.top < second.bottom - 2
                    && first.bottom > second.top + 2;

                const imageRect = imageBox.getBoundingClientRect();
                const actionRect = action.getBoundingClientRect();
                const titleRect = title.getBoundingClientRect();

                return !overlaps(imageRect, actionRect)
                    && !overlaps(titleRect, actionRect);
                """, productName));
    }

    public boolean hasNoProductCardTextOverflow(String productName) {
        return Boolean.TRUE.equals(executeScript("""
                const productName = String(arguments[0] || '').trim().toLowerCase();
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden';
                };
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim().toLowerCase();
                const card = Array.from(document.querySelectorAll("main a[href*='/product/']"))
                    .find(element => normalize(element.innerText).includes(productName));
                if (!card) {
                    return false;
                }

                const textElements = Array.from(card.querySelectorAll('div, p, span, h3, h4, del, button'))
                    .filter(visible)
                    .filter(element => (element.innerText || element.textContent || '').trim().length > 0)
                    .filter(element => element.children.length === 0 || element.tagName === 'BUTTON');

                return textElements.every(element => {
                    const className = String(element.className || '');
                    if (/line-clamp|truncate/.test(className)) {
                        return true;
                    }

                    return element.scrollWidth <= element.clientWidth + 2
                        && element.scrollHeight <= element.clientHeight + 4;
                });
                """, productName));
    }

    public boolean isOutOfStockProductDisplayed(String productName) {
        return Boolean.TRUE.equals(executeScript("""
                const productName = String(arguments[0] || '').trim().toLowerCase();
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim().toLowerCase();
                const card = Array.from(document.querySelectorAll("main a[href*='/product/']"))
                    .find(element => normalize(element.innerText).includes(productName));
                if (!card) {
                    return false;
                }

                const cardText = normalize(card.innerText || card.textContent || '');
                const actionButton = Array.from(card.querySelectorAll('button'))
                    .find(button => {
                        const text = normalize(button.innerText || button.textContent || '');
                        return text.includes('notify')
                            || button.querySelector('svg')
                            || button.querySelector('img');
                    });

                return cardText.includes('out of stock') && Boolean(actionButton);
                """, productName));
    }

    public AddProductToCartPage clickNotifyForOutOfStockProduct(String productName) {
        WebElement notifyButton = waitForClickable(outOfStockNotifyButtonByName(productName));
        executeScript("arguments[0].scrollIntoView({block: 'center', inline: 'center'});", notifyButton);

        try {
            notifyButton.click();
        } catch (ElementClickInterceptedException | StaleElementReferenceException exception) {
            executeScript("arguments[0].click();", waitForPresence(outOfStockNotifyButtonByName(productName)));
        }

        waitUntil(TOAST_TIMEOUT, webDriver -> !getLatestVisibleFeedbackMessage().isBlank()
                || isOutOfStockProductDisplayed(productName));
        return this;
    }

    public boolean hasStockNotAvailableToast() {
        return isToastWithTextDisplayed("Stock not available");
    }

    public String getLatestVisibleFeedbackMessage() {
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
                const messages = Array.from(document.querySelectorAll(
                    "section[aria-label*='Notifications'] li, section[aria-label*='Notifications'] *, "
                        + "[class*='toast'], [class*='Toast'], li"
                ))
                    .filter(visible)
                    .map(element => normalize(element.innerText || element.textContent))
                    .filter(text => text.length > 0);

                return messages.length ? messages[messages.length - 1] : '';
                """);

        return result == null ? "" : String.valueOf(result);
    }

    public void waitForToastMessagesToClear() {
        closeVisibleToasts();

        try {
            waitUntil(Duration.ofSeconds(8), webDriver -> !hasVisibleToast());
        } catch (TimeoutException ignored) {
            closeVisibleToasts();
        }
    }

    public void closeVisibleToasts() {
        executeScript("""
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                for (const closeButton of Array.from(document.querySelectorAll("button[aria-label='Close toast']"))) {
                    if (visible(closeButton)) {
                        closeButton.click();
                    }
                }
                """);
    }

    private WebElement waitForSearchResultAddButton(Set<String> excludedProductNames) {
        return waitUntil(UI_TIMEOUT, webDriver -> {
            Object result = executeScript("""
                    const excludedProductNames = new Set(arguments[0] || []);
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
                    const extractProductName = card => {
                        const imageAlt = Array.from(card.querySelectorAll('img[alt]'))
                            .map(image => normalize(image.getAttribute('alt')))
                            .find(alt => alt && !/icon|logo|placeholder|cart|search/i.test(alt));
                        const textLines = String(card.innerText || card.textContent || '')
                            .split(/\\n+/)
                            .map(normalize)
                            .filter(Boolean);
                        const visibleName = textLines.find(line =>
                            !/^ADD$/i.test(line)
                                && !/^Add\\s+to\\s+Cart$/i.test(line)
                                && !/^Notify$/i.test(line)
                                && !/out\\s+of\\s+stock/i.test(line)
                                && !/^৳/.test(line)
                                && !/%\\s*OFF$/i.test(line)
                                && !/^(?:\\d+\\s*-\\s*\\d+|\\d+)\\s*(?:hours?|hrs?)$/i.test(line)
                                && !/^prescription\\s+required$/i.test(line)
                        );
                        return visibleName || imageAlt || '';
                    };
                    const cards = Array.from(document.querySelectorAll("main a[href*='/product/']"))
                        .filter(visible);
                    const addableProducts = cards
                        .map(card => {
                            const addButton = Array.from(card.querySelectorAll('button'))
                                .find(button => visible(button)
                                    && normalize(button.innerText || button.textContent).toUpperCase() === 'ADD');
                            return { card, addButton, productName: extractProductName(card) };
                        })
                        .filter(product => product.addButton)
                        .filter(product => !excludedProductNames.has(canonical(product.productName)));

                    if (!addableProducts.length) {
                        return null;
                    }

                    const preferredProduct = addableProducts[0];

                    preferredProduct.card.scrollIntoView({ block: 'center', inline: 'nearest' });
                    return preferredProduct.addButton;
                    """, new ArrayList<>(excludedProductNames));

            return result instanceof WebElement element ? element : null;
        });
    }

    private ProductCardSelection readProductCardSelection(WebElement actionButton) {
        Object result = executeScript("""
                const button = arguments[0];
                const card = button ? button.closest("a[href*='/product/']") : null;
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();

                if (!card) {
                    return ['', normalize(button ? button.innerText || button.textContent : '')];
                }

                const imageAlt = Array.from(card.querySelectorAll('img[alt]'))
                    .map(image => normalize(image.getAttribute('alt')))
                    .find(alt => alt && !/icon|logo|placeholder/i.test(alt));
                const textLines = String(card.innerText || card.textContent || '')
                    .split(/\\n+/)
                    .map(normalize)
                    .filter(Boolean);
                const productName = imageAlt || textLines.find(line =>
                    !/^ADD$/i.test(line)
                        && !/^Notify$/i.test(line)
                        && !/^৳/.test(line)
                        && !/%\\s*OFF$/i.test(line)
                        && !/^(?:\\d+\\s*-\\s*\\d+|\\d+)\\s*(?:hours?|hrs?)$/i.test(line)
                ) || '';

                return [productName, normalize(button.innerText || button.textContent)];
                """, actionButton);

        if (result instanceof List<?> values && values.size() >= 2) {
            String productName = String.valueOf(values.get(0)).trim();
            String actionText = String.valueOf(values.get(1)).replaceAll("\\s+", " ").trim();

            if (!productName.isBlank()) {
                return new ProductCardSelection(productName, actionText);
            }
        }

        throw new TimeoutException("Could not determine the product name from the selected search result card.");
    }

    private void waitForAddedProductInCart(ProductCardSelection selectedProduct, String selectedQuantity) {
        try {
            new CartPage(driver)
                    .openCartDrawer()
                    .waitForProductInCart(selectedProduct.productName(), selectedQuantity)
                    .closeCartDrawer();
        } catch (TimeoutException exception) {
            throw new TimeoutException("Cart drawer did not confirm added search result product '"
                    + selectedProduct.productName() + "' with quantity '" + selectedQuantity + "'.", exception);
        }
    }

    private AddedProduct getAlreadyAddedProductFromSearchResult() {
        Object result = executeScript("""
                const preferredQuantity = String(arguments[0] || '').trim();
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
                const extractProductName = card => {
                    const imageAlt = Array.from(card.querySelectorAll('img[alt]'))
                        .map(image => normalize(image.getAttribute('alt')))
                        .find(alt => alt && !/icon|logo|placeholder/i.test(alt));
                    const textLines = String(card.innerText || card.textContent || '')
                        .split(/\\n+/)
                        .map(normalize)
                        .filter(Boolean);

                    return imageAlt || textLines.find(line =>
                        !/^ADD$/i.test(line)
                            && !/^Notify$/i.test(line)
                            && !/^৳/.test(line)
                            && !/%\\s*OFF$/i.test(line)
                            && !/^(?:\\d+\\s*-\\s*\\d+|\\d+)\\s*(?:hours?|hrs?)$/i.test(line)
                    ) || '';
                };
                const selectedProducts = Array.from(document.querySelectorAll("main a[href*='/product/']"))
                    .filter(visible)
                    .map(card => {
                        const selectedButton = Array.from(card.querySelectorAll('button'))
                            .find(button => {
                                const text = normalize(button.innerText || button.textContent);
                                return visible(button)
                                    && text
                                    && !/^ADD$/i.test(text)
                                    && !/^Notify$/i.test(text);
                            });
                        return selectedButton
                            ? [extractProductName(card), normalize(selectedButton.innerText || selectedButton.textContent)]
                            : null;
                    })
                    .filter(Boolean);

                const preferredProduct = selectedProducts.find(product => product[1].includes(preferredQuantity))
                    || selectedProducts[0];

                return preferredProduct || [];
                """, DEFAULT_SELECTED_QUANTITY);

        if (result instanceof List<?> values && values.size() >= 2) {
            String productName = String.valueOf(values.get(0)).trim();
            String selectedQuantity = String.valueOf(values.get(1)).replaceAll("\\s+", " ").trim();

            if (!productName.isBlank() && !selectedQuantity.isBlank()) {
                return new AddedProduct(productName, selectedQuantity, true);
            }
        }

        throw new TimeoutException("No visible search result product with an ADD button or already-selected state.");
    }

    private boolean waitForQuantitySelectorIfVisible() {
        try {
            waitUntil(SHORT_UI_TIMEOUT, webDriver -> isQuantitySelectorDisplayedCorrectly());
            return true;
        } catch (TimeoutException exception) {
            return false;
        }
    }

    private String preferredQuantityLabel(String requestedQuantityLabel) {
        List<String> quantityOptions = getVisibleQuantityOptionLabels();
        if (quantityOptions.isEmpty()) {
            throw new TimeoutException("Quantity selector did not expose any purchasable quantity option.");
        }

        String requested = canonicalQuantityLabel(requestedQuantityLabel);
        return quantityOptions.stream()
                .filter(option -> {
                    String optionLabel = canonicalQuantityLabel(option);
                    return !requested.isBlank()
                            && (optionLabel.contains(requested) || requested.contains(optionLabel));
                })
                .findFirst()
                .orElseGet(() -> quantityOptions.stream()
                        .filter(option -> canonicalQuantityLabel(option).contains("1x")
                                || canonicalQuantityLabel(option).startsWith("1"))
                        .findFirst()
                        .orElse(quantityOptions.get(0)));
    }

    private String canonicalQuantityLabel(String label) {
        return label == null ? "" : label
                .replace("×", "x")
                .replaceAll("(?i)\\bbot\\b", "bottle")
                .replaceAll("[^A-Za-z0-9]+", "")
                .toLowerCase();
    }

    private boolean actionTextMatchesQuantity(String actionText, String quantityLabel) {
        String normalizedActionText = normalizeQuantityDisplayText(actionText);
        String normalizedQuantityLabel = normalizeQuantityDisplayText(quantityLabel);

        if (normalizedActionText.isBlank()
                || normalizedQuantityLabel.isBlank()
                || "ADD".equalsIgnoreCase(normalizedActionText)
                || "Notify".equalsIgnoreCase(normalizedActionText)) {
            return false;
        }

        if (normalizedActionText.contains(normalizedQuantityLabel)
                || normalizedQuantityLabel.contains(normalizedActionText)) {
            return true;
        }

        String actionCanonical = canonicalQuantityLabel(normalizedActionText);
        String quantityCanonical = canonicalQuantityLabel(normalizedQuantityLabel);

        if (actionCanonical.isBlank() || quantityCanonical.isBlank()) {
            return false;
        }

        return actionCanonical.equals(quantityCanonical)
                || isMeaningfulQuantityPrefix(actionCanonical, quantityCanonical)
                || isMeaningfulQuantityPrefix(quantityCanonical, actionCanonical);
    }

    private String normalizeQuantityDisplayText(String text) {
        return text == null ? "" : text
                .replace('\u00A0', ' ')
                .replace("…", "...")
                .replaceAll("\\s+", " ")
                .replaceAll("\\.{3,}$", "")
                .trim();
    }

    private boolean isMeaningfulQuantityPrefix(String possiblePrefix, String fullText) {
        return possiblePrefix != null
                && fullText != null
                && possiblePrefix.length() >= 4
                && fullText.startsWith(possiblePrefix);
    }

    private String canonicalProductName(String productName) {
        return productName == null ? "" : productName
                .replaceAll("\\s+", " ")
                .trim()
                .replaceAll("[^A-Za-z0-9]+", "")
                .toLowerCase();
    }

    private Set<String> canonicalProductNameSet(Collection<String> productNames) {
        Set<String> canonicalNames = new HashSet<>();
        if (productNames == null) {
            return canonicalNames;
        }

        productNames.stream()
                .map(this::canonicalProductName)
                .filter(name -> !name.isBlank())
                .forEach(canonicalNames::add);
        return canonicalNames;
    }

    private boolean tryOpenQuantitySelectorFor(String productName) {
        waitForToastMessagesToClear();

        try {
            WebElement addButton = waitForActiveAddButton(productName);
            clickElementWithFallback(addButton);
            waitUntil(PRODUCT_PROBE_TIMEOUT, webDriver -> isQuantitySelectorDisplayedCorrectly()
                    && !getVisibleQuantityOptionLabels().isEmpty());
            return true;
        } catch (ElementClickInterceptedException | StaleElementReferenceException | TimeoutException exception) {
            return false;
        }
    }

    private WebElement waitForActiveAddButton(String productName) {
        return waitForProductActionButton(productName, "ADD", PRODUCT_PROBE_TIMEOUT);
    }

    private WebElement waitForProductActionButton(String productName) {
        return waitForProductActionButton(productName, "", UI_TIMEOUT);
    }

    private WebElement waitForProductActionButton(String productName, String expectedActionText, Duration timeout) {
        return waitUntil(timeout, webDriver -> {
            Object result = executeScript("""
                    const requestedName = canonical(arguments[0]);
                    const expectedActionText = normalize(arguments[1]).toUpperCase();
                    if (!requestedName) {
                        return null;
                    }

                    const productLink = Array.from(document.querySelectorAll("main a[href*='/product/']"))
                        .filter(visible)
                        .find(link => canonical(link.innerText || link.textContent).includes(requestedName));
                    if (!productLink) {
                        return null;
                    }
                    const productTextNode = findProductTextNode() || productLink;
                    const productRect = productTextNode.getBoundingClientRect();
                    const productCenterY = productRect.top + productRect.height / 2;

                    const actionMatches = button => {
                        const text = normalize(button.innerText || button.textContent);
                        return visible(button)
                            && text.length > 0
                            && !button.closest('[role="dialog"]')
                            && !button.disabled
                            && button.getAttribute('aria-disabled') !== 'true'
                            && getComputedStyle(button).pointerEvents !== 'none'
                            && (expectedActionText.length === 0 || text.toUpperCase() === expectedActionText);
                    };

                    for (let scope = productLink; scope && !scope.matches('main'); scope = scope.parentElement) {
                        const scopedButton = Array.from(scope.querySelectorAll('button')).find(actionMatches);
                        if (scopedButton) {
                            return scopedButton;
                        }
                    }

                    const sameRowAction = Array.from(document.querySelectorAll('main button'))
                        .filter(actionMatches)
                        .map(button => ({ button, rect: button.getBoundingClientRect() }))
                        .filter(candidate => candidate.rect.left >= productRect.left - 8)
                        .filter(candidate => Math.abs(
                            (candidate.rect.top + candidate.rect.height / 2) - productCenterY
                        ) <= Math.max(48, productRect.height * 1.25))
                        .sort((first, second) =>
                            Math.abs((first.rect.top + first.rect.height / 2) - productCenterY)
                                - Math.abs((second.rect.top + second.rect.height / 2) - productCenterY)
                                || first.rect.left - second.rect.left
                        )
                        .map(candidate => candidate.button)[0] || null;
                    if (sameRowAction) {
                        return sameRowAction;
                    }

                    return null;

                    function findProductTextNode() {
                        return Array.from(document.querySelectorAll('main h1, main h2, main h3, main h4, main p, main span, main div, main a'))
                            .filter(visible)
                            .filter(element => !element.closest('button'))
                            .map(element => ({
                                element,
                                rect: element.getBoundingClientRect(),
                                text: normalize(element.innerText || element.textContent)
                            }))
                            .filter(candidate => canonical(candidate.text).includes(requestedName))
                            .sort((first, second) =>
                                (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                                    || first.rect.top - second.rect.top
                            )
                            .map(candidate => candidate.element)[0] || null;
                    }

                    function normalize(text) {
                        return String(text || '').replace(/\\s+/g, ' ').trim();
                    }

                    function canonical(text) {
                        return normalize(text).toLowerCase().replace(/[^a-z0-9]+/g, '');
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
                    """, productName, expectedActionText);

            return result instanceof WebElement element ? element : null;
        });
    }

    private void reloadSearchResults() {
        reloadSearchResults(getSubmittedSearchText());
    }

    private String ensureExplicitSearchResultsPage(String searchText) {
        if (searchText == null || searchText.isBlank()) {
            searchText = getSubmittedSearchText();
        }

        String searchUrl = searchText == null || searchText.isBlank()
                ? getCurrentUrl()
                : buildSearchUrl(resolveBaseUrl(), searchText);

        if (searchText != null && !searchText.isBlank() && !getCurrentUrl().contains("/search")) {
            driver.get(searchUrl);
            waitUntilSearchPageLoaded();
            waitForSearchResultsToLoadCompletely(searchText);
        }

        return searchUrl;
    }

    private void reloadSearchResults(String searchText) {
        if (searchText == null || searchText.isBlank()) {
            searchText = getSubmittedSearchText();
        }

        driver.get(buildSearchUrl(resolveBaseUrl(), searchText));
        waitUntilSearchPageLoaded();
        waitForSearchResultsToLoadCompletely(searchText);
        if (!searchText.isBlank() && getSearchInputValue().isBlank()) {
            enterProductName(searchText);
            waitForSearchResultsToLoadCompletely(searchText);
        }
    }

    private QuantityOption waitForPreferredQuantityOption(String quantityLabel) {
        return waitUntil(Duration.ofSeconds(8), webDriver -> {
            Object result = executeScript("""
                    const requestedLabel = String(arguments[0] || '').trim();
                    const dialog = document.querySelector('[role="dialog"][aria-label="Select quantity"]');
                    if (!dialog) {
                        return [];
                    }

                    const visible = element => {
                        const rect = element.getBoundingClientRect();
                        const style = getComputedStyle(element);
                        return rect.width > 0 && rect.height > 0
                            && style.display !== 'none'
                            && style.visibility !== 'hidden'
                            && Number(style.opacity || 1) !== 0;
                    };
                    const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                    const canonical = text => normalize(text)
                        .replace(/[×]/g, 'x')
                        .replace(/\\bbot\\b/ig, 'bottle')
                        .toLowerCase();
                    const requested = canonical(requestedLabel);
                    const options = Array.from(dialog.querySelectorAll('button'))
                        .filter(visible)
                        .map(button => ({ button, label: normalize(button.innerText || button.textContent) }))
                        .filter(option => option.label);

                    const exactOption = options.find(option => canonical(option.label).includes(requested));
                    const singleBottleOption = options.find(option => {
                        const text = canonical(option.label);
                        return /(^|\\s)1\\s*x\\b/.test(text) && /\\bbottle\\b/.test(text);
                    });
                    const selectedOption = exactOption || singleBottleOption;

                    return selectedOption ? [selectedOption.button, selectedOption.label] : [];
                    """, quantityLabel);

            if (result instanceof List<?> values && values.size() >= 2 && values.get(0) instanceof WebElement element) {
                String label = String.valueOf(values.get(1)).replaceAll("\\s+", " ").trim();

                if (!label.isBlank()) {
                    return new QuantityOption(element, label);
                }
            }

            return null;
        });
    }

    private boolean isQuantityOptionSelectedOrFirstVisible(String quantityLabel) {
        return Boolean.TRUE.equals(executeScript("""
                const requestedLabel = String(arguments[0] || '').trim();
                const dialog = document.querySelector('[role="dialog"][aria-label="Select quantity"]');
                if (!dialog) {
                    return false;
                }

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
                const options = Array.from(dialog.querySelectorAll('button'))
                    .filter(visible)
                    .map(button => ({ button, label: normalize(button.innerText || button.textContent) }))
                    .filter(option => option.label);
                const target = options.find(option => canonical(option.label).includes(canonical(requestedLabel)));
                if (!target) {
                    return false;
                }

                const className = String(target.button.className || '');
                const selectedByState = target.button.getAttribute('aria-selected') === 'true'
                    || target.button.getAttribute('aria-checked') === 'true'
                    || target.button.matches('[data-state="checked"], [data-selected="true"]')
                    || Boolean(target.button.querySelector('[checked], [aria-checked="true"], [data-state="checked"]'))
                    || /selected|active|bg-brand|text-brand|text-white|border-brand/i.test(className);
                const firstVisibleDefault = options[0] && options[0].button === target.button;

                return selectedByState || firstVisibleDefault;
                """, quantityLabel));
    }

    private String waitForProductActionToChange(String productName, String previousActionText) {
        return waitUntil(SHORT_UI_TIMEOUT, webDriver -> {
            String actionText = getProductActionText(productName);

            if (!actionText.isBlank()
                    && !"ADD".equalsIgnoreCase(actionText)
                    && !actionText.equalsIgnoreCase(previousActionText)) {
                return actionText;
            }

            return null;
        });
    }

    private void clickElementWithFallback(WebElement element) {
        executeScript("arguments[0].scrollIntoView({block: 'center', inline: 'nearest'});", element);

        try {
            element.click();
        } catch (ElementClickInterceptedException exception) {
            executeScript("arguments[0].click();", element);
        }
    }

    private boolean isToastWithTextDisplayed(String expectedText) {
        return Boolean.TRUE.equals(executeScript("""
                const expectedText = String(arguments[0] || '').trim().toLowerCase();
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
                    "section[aria-label*='Notifications'] *, [class*='toast'], [class*='Toast'], li"
                ))
                    .filter(visible)
                    .some(element => normalize(element.innerText || element.textContent).includes(expectedText));
                """, expectedText));
    }

    private boolean hasVisibleToast() {
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
                    "section[aria-label*='Notifications'] li, [class*='toast'], [class*='Toast']"
                ))
                    .some(visible);
                """));
    }

    private String getProductCardText(String productName) {
        return getText(productCardByName(productName))
                .replaceAll("\\s+", " ")
                .trim();
    }

    private By productCardByName(String productName) {
        return By.xpath(productCardXpath(productName));
    }

    private By productActionButtonByName(String productName) {
        return By.xpath(productCardXpath(productName)
                + "//button[normalize-space() and not(ancestor::*[@role='dialog'])]");
    }

    private By activeAddButtonByName(String productName) {
        return By.xpath(productCardXpath(productName)
                + "//button[normalize-space()='ADD' and not(ancestor::*[@role='dialog'])]");
    }

    private By outOfStockNotifyButtonByName(String productName) {
        return By.xpath(productCardXpath(productName)
                + "//button[normalize-space()='Notify' or .//*[local-name()='svg'] or .//img]");
    }

    private By quantityOptionByLabel(String quantityLabel) {
        return By.xpath("//*[@role='dialog' and @aria-label='Select quantity']"
                + "//button[.//span[normalize-space()=" + xpathText(quantityLabel)
                + "] or normalize-space()=" + xpathText(quantityLabel) + "]");
    }

    private String productCardXpath(String productName) {
        return "//main//a[contains(@href,'/product/') and .//*[normalize-space()="
                + xpathText(productName)
                + "]]";
    }

    private String xpathText(String text) {
        if (text == null) {
            return "''";
        }

        if (!text.contains("'")) {
            return "'" + text + "'";
        }

        if (!text.contains("\"")) {
            return "\"" + text + "\"";
        }

        String[] parts = text.split("'");
        StringBuilder builder = new StringBuilder("concat(");

        for (int index = 0; index < parts.length; index++) {
            if (index > 0) {
                builder.append(",\"'\",");
            }
            builder.append("'").append(parts[index]).append("'");
        }

        builder.append(")");
        return builder.toString();
    }

    private String normalizeCurrencyText(String text) {
        return text == null ? "" : text
                .replaceAll("\\s+", " ")
                .replace("৳ ", "৳")
                .trim();
    }

    private double asDouble(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0;
    }

    public record ProductCardBounds(double left, double top, double width, double height) {
    }

    private record ProductCardSelection(String productName, String actionText) {
    }

    private record QuantityOption(WebElement element, String label) {
    }

    public record AddedProduct(String productName, String selectedQuantity, boolean alreadyAdded) {
    }
}
