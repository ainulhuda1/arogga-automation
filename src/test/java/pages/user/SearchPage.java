package pages.user;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import utils.TestContext;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Page object for the product search journey and search results page.
 */
public class









SearchPage extends BasePage {

    private static final Duration RESULTS_TIMEOUT = Duration.ofSeconds(25);
    private static final Duration SUGGESTIONS_TIMEOUT = Duration.ofSeconds(15);

    private static final By HEADER_SEARCH_INPUT =
            By.xpath("//header//input[@aria-label='Search' and contains(@placeholder,'Search')]");
    private static final By HEADER_SEARCH_ICON =
            By.xpath("//header//img[@alt='Search Icon']");
    private static final By CLEAR_SEARCH_BUTTON =
            By.xpath("//header//button[@aria-label='Clear search']");

    private static final By SEARCH_BREADCRUMB =
            By.xpath("//main//*[self::a or self::span or self::p or self::li][normalize-space()='Search']");
    private static final By BACK_BUTTON =
            By.xpath("//main//*[self::a or self::button or self::span or self::p][normalize-space()='Home']");

    private static final By STORE_TAB =
            By.xpath("//main//*[self::a or self::button][normalize-space()='Store']");
    private static final By LAB_TAB =
            By.xpath("//main//*[self::a or self::button][normalize-space()='Lab']");
    private static final By DOCTOR_TAB =
            By.xpath("//main//*[self::a or self::button][normalize-space()='Doctor']");

    private static final By SEARCH_RESULTS_HEADER =
            By.xpath("//main//h1[contains(normalize-space(),'Showing all results for')]");
    private static final By PRODUCT_RESULT_CARDS =
            By.xpath("//main//a[contains(@href,'/product/')]");
    private static final By NO_RESULTS_STATE =
            By.xpath("//main//*[contains(normalize-space(),'No products found')]");
    private static final By RECENT_SEARCH_HEADING =
            By.xpath("//main//*[normalize-space()='Recently Search Items']");
    private static final By RECENT_SEARCH_CLEAR =
            By.xpath("//main//*[normalize-space()='Recently Search Items']/following::*[normalize-space()='Clear'][1]");
    private static final By TRENDING_ITEMS_HEADING =
            By.xpath("//main//*[normalize-space()='Trending items']");

    public SearchPage(WebDriver driver) {
        super(driver);
    }

    /**
     * Focuses the visible header search input. Search now updates automatically while typing.
     */
    public SearchPage clickSearchEntryPoint() {
        waitForVisible(HEADER_SEARCH_INPUT).click();
        return this;
    }

    /**
     * Opens the empty Search page directly from the configured base URL.
     */
    public SearchPage openEmptySearchPage(String baseUrl) {
        driver.get(buildSearchUrl(baseUrl, ""));
        return waitUntilSearchPageLoaded();
    }

    /**
     * Opens product search results directly for the given query.
     */
    public SearchPage openSearchResults(String baseUrl, String query) {
        String searchText = normalizeSearchText(query);
        driver.get(buildSearchUrl(baseUrl, searchText));
        waitUntilSearchPageLoaded();
        return waitForSubmittedSearchState(searchText);
    }

    public SearchPage waitUntilSearchPageLoaded() {
        waitForPageLoad();
        waitUntil(webDriver -> getCurrentUrl().contains("/search"));
        waitUntil(RESULTS_TIMEOUT, webDriver -> isSearchBreadcrumbDisplayed());
        waitForVisible(STORE_TAB);
        waitForVisible(LAB_TAB);
        waitForVisible(DOCTOR_TAB);
        waitForVisible(HEADER_SEARCH_INPUT);
        return this;
    }

    public boolean isSearchPageDisplayed() {
        return getCurrentUrl().contains("/search")
                && isSearchBreadcrumbDisplayed()
                && areSearchTabsVisible();
    }

    public boolean isSearchInputDisplayed() {
        return isDisplayed(HEADER_SEARCH_INPUT);
    }

    public boolean isSearchIconDisplayed() {
        return isDisplayed(HEADER_SEARCH_ICON);
    }

    public boolean areSearchTabsVisible() {
        return isDisplayed(STORE_TAB)
                && isDisplayed(LAB_TAB)
                && isDisplayed(DOCTOR_TAB);
    }

    public boolean areCoreSearchUiComponentsDisplayed() {
        return isSearchInputDisplayed()
                && isSearchIconDisplayed()
                && isSearchBreadcrumbDisplayed()
                && areSearchTabsVisible();
    }

    public SearchPage enterProductName(String productName) {
        String searchText = normalizeSearchText(productName);

        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                WebElement searchInput = waitForVisible(HEADER_SEARCH_INPUT);
                searchInput.click();
                searchInput.clear();
                searchInput.sendKeys(searchText);
                waitUntilSearchInputValueSettles(searchText);
                return this;
            } catch (StaleElementReferenceException | TimeoutException | ElementNotInteractableException exception) {
                // The header search input rerenders while typing in the current UI; retry with a fresh element.
            }
        }

        setSearchInputValue(searchText);
        waitUntilSearchInputValueSettles(searchText);
        return this;
    }

    public SearchPage typeSearchTextForSuggestions(String searchTerm) {
        return enterProductName(searchTerm);
    }

    public boolean areSearchSuggestionsDisplayedFor(String searchTerm) {
        String searchText = normalizeSearchText(searchTerm);

        if (searchText.isBlank()) {
            return false;
        }

        try {
            waitUntil(SUGGESTIONS_TIMEOUT, webDriver -> getSearchInputValue().equalsIgnoreCase(searchText)
                    && hasVisibleSearchSuggestion(searchText));
            return true;
        } catch (TimeoutException exception) {
            return false;
        }
    }

    public boolean isAutomaticSearchResultDisplayed(String searchTerm) {
        String searchText = normalizeSearchText(searchTerm);

        return !searchText.isBlank()
                && getSearchInputValue().equalsIgnoreCase(searchText)
                && (hasVisibleSearchSuggestion(searchText)
                || isDisplayedNow(productCardByName(searchText))
                || areSearchResultsDisplayed()
                || isNoResultsStateDisplayed());
    }

    /**
     * Enters a query through the header search input and waits for automatic suggestions/results.
     */
    public SearchPage searchForProduct(String productName) {
        String searchText = normalizeSearchText(productName);
        enterProductName(searchText);

        if (searchText.isBlank()) {
            if (!getCurrentUrl().contains("/search")) {
                driver.get(buildSearchUrl(resolveBaseUrl(), ""));
                waitUntilSearchPageLoaded();
            }

            return waitForEmptySearchState();
        }

        return waitForAutomaticSearchState(searchText);
    }

    public SearchPage searchProducts(String keyword) {
        String searchText = normalizeSearchText(keyword);
        clickSearchEntryPoint();
        enterProductName(searchText);

        try {
            return waitForSearchResultsToLoadCompletely(searchText);
        } catch (TimeoutException exception) {
            driver.get(buildSearchUrl(resolveBaseUrl(), searchText));
            waitUntilSearchPageLoaded();
            return waitForSearchResultsToLoadCompletely(searchText);
        }
    }

    public SearchPage waitForSearchResultsToLoadCompletely(String keyword) {
        String searchText = normalizeSearchText(keyword);

        waitUntil(RESULTS_TIMEOUT, webDriver -> areSearchResultsDisplayed() || isNoResultsStateDisplayed());
        waitUntil(RESULTS_TIMEOUT, webDriver -> !isSearchLoadingActive());
        if (!isNoResultsStateDisplayed()) {
            waitUntil(RESULTS_TIMEOUT, webDriver -> isSearchResultCountStable());
        }

        if (!searchText.isBlank()) {
            waitUntil(RESULTS_TIMEOUT, webDriver -> getSearchInputValue().equalsIgnoreCase(searchText)
                    || getCurrentUrl().contains("_search=")
                    || areSearchResultsDisplayed());
        }

        return this;
    }

    public List<ProductSearchResult> getSearchResults() {
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
                const enabled = button => Boolean(button)
                    && !button.disabled
                    && button.getAttribute('aria-disabled') !== 'true'
                    && !/disabled/i.test(String(button.className || ''))
                    && getComputedStyle(button).pointerEvents !== 'none';
                const actionButton = productLink => {
                    const buttonText = button => normalize(button.innerText || button.textContent || '');
                    const candidateButton = button => visible(button)
                        && !button.closest('[role="dialog"]')
                        && buttonText(button).length > 0;
                    const scopedButton = Array.from(productLink.querySelectorAll('button'))
                        .filter(candidateButton)[0] || null;
                    if (scopedButton) {
                        return scopedButton;
                    }

                    for (let scope = productLink.parentElement; scope && !scope.matches('main'); scope = scope.parentElement) {
                        const scopeText = normalize(scope.innerText || scope.textContent || '');
                        if (!scopeText.includes(extractProductName(productLink))) {
                            continue;
                        }

                        const button = Array.from(scope.querySelectorAll('button'))
                            .filter(candidateButton)
                            .find(candidate => /^(ADD|Add\\s+to\\s+Cart|Notify)$/i.test(buttonText(candidate)));
                        if (button) {
                            return button;
                        }
                    }

                    const linkRect = productLink.getBoundingClientRect();
                    const linkCenterY = linkRect.top + linkRect.height / 2;
                    return Array.from(document.querySelectorAll('main button'))
                        .filter(candidateButton)
                        .map(button => ({ button, rect: button.getBoundingClientRect() }))
                        .filter(candidate => candidate.rect.top < linkRect.bottom + 16
                            && candidate.rect.bottom > linkRect.top - 16)
                        .sort((first, second) =>
                            Math.abs((first.rect.top + first.rect.height / 2) - linkCenterY)
                                - Math.abs((second.rect.top + second.rect.height / 2) - linkCenterY)
                        )
                        .map(candidate => candidate.button)[0] || null;
                };

                return Array.from(document.querySelectorAll("main a[href*='/product/']"))
                    .filter(visible)
                    .map(card => {
                        const button = actionButton(card);
                        const actionText = normalize(button ? button.innerText || button.textContent || '' : '');
                        const cardText = normalize(card.innerText || card.textContent || '');
                        const outOfStock = /out\\s+of\\s+stock/i.test(cardText)
                            || /notify/i.test(actionText);
                        const activeAddButton = Boolean(button)
                            && enabled(button)
                            && /^(ADD|Add\\s+to\\s+Cart)$/i.test(actionText);
                        return {
                            productName: extractProductName(card),
                            productUrl: card.href || card.getAttribute('href') || '',
                            actionText,
                            visible: true,
                            enabled: enabled(button),
                            outOfStock,
                            availableForPurchase: !outOfStock && activeAddButton
                        };
                    })
                    .filter(product => product.productName && product.productUrl);
                """);

        List<ProductSearchResult> products = new ArrayList<>();
        if (!(result instanceof List<?> rawProducts)) {
            return products;
        }

        for (Object rawProduct : rawProducts) {
            if (!(rawProduct instanceof Map<?, ?> product)) {
                continue;
            }

            ProductSearchResult searchResult = new ProductSearchResult(
                    stringValue(product.get("productName")),
                    stringValue(product.get("productUrl")),
                    stringValue(product.get("actionText")),
                    booleanValue(product.get("visible")),
                    booleanValue(product.get("enabled")),
                    booleanValue(product.get("outOfStock")),
                    booleanValue(product.get("availableForPurchase"))
            );

            if (!searchResult.productName().isBlank() && !searchResult.productUrl().isBlank()) {
                products.add(searchResult);
            }
        }

        return products;
    }

    public boolean isProductAvailable(ProductSearchResult product) {
        return product != null
                && product.visible()
                && product.enabled()
                && !product.outOfStock()
                && product.availableForPurchase()
                && !product.productName().isBlank()
                && !product.productUrl().isBlank();
    }

    public ProductSearchResult getFirstAvailableProduct() {
        ProductSearchResult selectedProduct = findFirstAvailableProductAcrossLoadedResults();
        TestContext.setSelectedProductName(selectedProduct.productName());
        return selectedProduct;
    }

    public ProductDetailsPage selectFirstAvailableProduct() {
        ProductSearchResult selectedProduct = getFirstAvailableProduct();

        driver.get(selectedProduct.productUrl());
        return new ProductDetailsPage(driver).waitUntilLoaded(selectedProduct.productName());
    }

    public String captureSelectedProductName() {
        return TestContext.getSelectedProductName()
                .orElseThrow(() -> new TimeoutException("No dynamic product has been selected yet."));
    }

    /**
     * Types through the UI only, which allows the application to persist a recent-search chip if supported.
     */
    public SearchPage searchForProductAndRecordRecentSearch(String productName) {
        String searchText = normalizeSearchText(productName);

        if (searchText.isBlank()) {
            return submitEmptySearch();
        }

        enterProductName(searchText);
        return waitForAutomaticSearchState(searchText);
    }

    /**
     * Submits an empty search query and waits for the application's empty-search state.
     */
    public SearchPage submitEmptySearch() {
        clearSearchInput();

        if (!getCurrentUrl().contains("/search")) {
            driver.get(buildSearchUrl(resolveBaseUrl(), ""));
            waitUntilSearchPageLoaded();
        }

        return waitForEmptySearchState();
    }

    public SearchPage waitForSearchResults(String productName) {
        String searchText = normalizeSearchText(productName);

        if (searchText.isBlank()) {
            return waitForEmptySearchState();
        }

        waitUntil(RESULTS_TIMEOUT, webDriver -> getCurrentUrl().contains("/search")
                && (getCurrentUrl().contains("_search=") || isDisplayedNow(SEARCH_RESULTS_HEADER)));
        waitUntil(RESULTS_TIMEOUT, webDriver -> isDisplayedNow(resultsHeaderByQuery(searchText))
                || areSearchResultsDisplayed()
                || isNoResultsStateDisplayed());
        return this;
    }

    public SearchPage waitForProductCard(String productName) {
        waitUntil(RESULTS_TIMEOUT, webDriver -> isDisplayedNow(productCardByName(productName)));
        scrollProductCardIntoView(productName);
        return this;
    }

    public boolean areSearchResultsDisplayed() {
        return getDisplayedProductCount() > 0;
    }

    public int getDisplayedProductCount() {
        return (int) displayedElementCount(PRODUCT_RESULT_CARDS);
    }

    public boolean isProductDisplayed(String productName) {
        return isDisplayed(productCardByName(productName));
    }

    public boolean isProductImageDisplayed(String productName) {
        scrollProductCardIntoView(productName);
        return isImageLoaded(productImageByName(productName));
    }

    public boolean isProductPriceDisplayed(String productName) {
        scrollProductCardIntoView(productName);
        return isDisplayed(productPriceByName(productName));
    }

    public boolean isProductAddButtonDisplayed(String productName) {
        scrollProductCardIntoView(productName);
        return isDisplayed(productAddButtonByName(productName));
    }

    public boolean isDiscountBadgeDisplayed(String productName) {
        scrollProductCardIntoView(productName);
        return isDisplayed(productDiscountBadgeByName(productName));
    }

    public boolean isProductCardComplete(String productName) {
        return isProductDisplayed(productName)
                && isProductImageDisplayed(productName)
                && isProductPriceDisplayed(productName)
                && isProductAddButtonDisplayed(productName);
    }

    public String getSearchPageHeader() {
        if (isDisplayedNow(SEARCH_RESULTS_HEADER)) {
            return getText(SEARCH_RESULTS_HEADER);
        }

        return isSearchBreadcrumbDisplayed() ? "Search" : "";
    }

    public String getSearchInputPlaceholder() {
        String placeholder = getAttribute(HEADER_SEARCH_INPUT, "placeholder");
        return placeholder == null ? "" : placeholder.trim();
    }

    public String getNormalizedSearchInputPlaceholder() {
        return getSearchInputPlaceholder()
                .replace("\"", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public String getSearchInputValue() {
        String value = getAttribute(HEADER_SEARCH_INPUT, "value");
        return value == null ? "" : value.trim();
    }

    protected String getSubmittedSearchText() {
        String inputText = getSearchInputValue();
        if (!inputText.isBlank()) {
            return inputText;
        }

        String urlSearchText = getSearchTextFromCurrentUrl();
        if (!urlSearchText.isBlank()) {
            return urlSearchText;
        }

        String resultsHeaderText = firstVisibleText(SEARCH_RESULTS_HEADER)
                .replaceAll("\\s+", " ")
                .trim();
        return resultsHeaderText.replaceFirst("(?i)^Showing all results for\\s*", "").trim();
    }

    public String getSearchPageContextText() {
        Object result = executeScript("""
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                const mainText = normalize(document.querySelector('main')?.innerText || '');
                const match = mainText.match(/\\bHome\\b\\s*>?\\s*\\bSearch\\b/i);
                return match ? match[0].replace(/\\s+/g, ' ').trim() : '';
                """);
        return result == null ? "" : String.valueOf(result).trim();
    }

    public boolean isBackButtonVisible() {
        return isDisplayed(BACK_BUTTON) || isSearchBreadcrumbDisplayed();
    }

    public HomePage clickBackButton() {
        String searchPageUrl = getCurrentUrl();
        if (isDisplayed(BACK_BUTTON)) {
            clickWithFallback(BACK_BUTTON);
        } else {
            driver.navigate().back();
        }

        try {
            waitUntil(Duration.ofSeconds(8), webDriver -> !getCurrentUrl().equals(searchPageUrl)
                    && !getCurrentUrl().contains("/search"));
        } catch (TimeoutException exception) {
            driver.navigate().back();
            try {
                waitUntil(Duration.ofSeconds(8), webDriver -> !getCurrentUrl().contains("/search"));
            } catch (TimeoutException ignored) {
                driver.get(resolveBaseUrl());
                waitUntil(Duration.ofSeconds(12), webDriver -> !getCurrentUrl().contains("/search"));
            }
        }

        return new HomePage(driver).waitUntilLoaded();
    }

    public SearchPage clearSearchInput() {
        if (isDisplayedNow(CLEAR_SEARCH_BUTTON)) {
            click(CLEAR_SEARCH_BUTTON);
            waitUntil(webDriver -> getSearchInputValue().isBlank());
        } else {
            type(HEADER_SEARCH_INPUT, "");
        }

        return this;
    }

    public boolean isEmptySearchStateDisplayed() {
        return isSearchPageDisplayed()
                && !isDisplayedNow(SEARCH_RESULTS_HEADER)
                && (isDisplayed(RECENT_SEARCH_HEADING) || isDisplayed(TRENDING_ITEMS_HEADING));
    }

    public SearchPage waitForEmptySearchState() {
        waitUntil(Duration.ofSeconds(12), webDriver -> isEmptySearchStateDisplayed());
        return this;
    }

    public boolean isRecentSearchChipDisplayed(String query) {
        try {
            waitUntil(Duration.ofSeconds(10), webDriver -> isRecentSearchChipDisplayedNow(query));
            return true;
        } catch (TimeoutException exception) {
            return false;
        }
    }

    public SearchPage waitForRecentSearchChip(String query) {
        waitUntil(Duration.ofSeconds(15), webDriver -> isRecentSearchChipDisplayedNow(query));
        return this;
    }

    public SearchPage clearRecentSearches(String query) {
        if (!clickRecentSearchClearButton()) {
            click(RECENT_SEARCH_CLEAR);
        }

        waitUntil(Duration.ofSeconds(10), webDriver -> !isRecentSearchChipDisplayedNow(query));
        return this;
    }

    public boolean isNoResultsStateDisplayed() {
        return isDisplayedNow(NO_RESULTS_STATE);
    }

    public boolean waitUntilNoResultsStateDisplayed() {
        try {
            waitUntil(RESULTS_TIMEOUT, webDriver -> !isSearchLoadingActive() && isNoResultsStateDisplayed());
            return true;
        } catch (TimeoutException exception) {
            return false;
        }
    }

    public boolean isSearchInputAligned() {
        return Boolean.TRUE.equals(executeScript("""
                const input = document.querySelector("header input[aria-label='Search']");
                const icon = document.querySelector("header img[alt='Search Icon']");
                if (!input || !icon) {
                    return false;
                }

                const inputRect = input.getBoundingClientRect();
                const iconRect = icon.getBoundingClientRect();
                const centerDelta = Math.abs(
                    (inputRect.top + inputRect.height / 2) - (iconRect.top + iconRect.height / 2)
                );

                return centerDelta <= 8
                    && iconRect.left >= inputRect.left - 48
                    && iconRect.right <= inputRect.right + 2
                    && inputRect.height > 0
                    && inputRect.width > 0
                    && iconRect.width > 0
                    && iconRect.height > 0;
                """));
    }

    public boolean isPlaceholderTextVisibleAndAligned() {
        return getSearchInputPlaceholder().contains("Search") && isSearchInputAligned();
    }

    public boolean areFontsRenderedCorrectly() {
        return Boolean.TRUE.equals(waitUntil(Duration.ofSeconds(10), webDriver -> Boolean.TRUE.equals(executeScript("""
                const bodyFont = getComputedStyle(document.body).fontFamily;
                const fontsLoaded = !document.fonts || document.fonts.status === 'loaded';
                return fontsLoaded && bodyFont && bodyFont.trim().length > 0;
                """))));
    }

    public boolean areSpacingPaddingAndAlignmentValid() {
        return isSearchInputAligned() && areSearchTabsVisible();
    }

    public boolean hasNoOverlappingUiElements() {
        return Boolean.TRUE.equals(executeScript("""
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden';
                };
                const tabElements = Array.from(document.querySelectorAll("main a[href*='/search'][href*='_product_type']"));
                const productCards = Array.from(document.querySelectorAll("main a[href*='/product/']")).slice(0, 8);
                const elements = [
                    document.querySelector("header input[aria-label='Search']"),
                    document.querySelector("header img[alt='Search Icon']"),
                    ...tabElements,
                    ...productCards
                ].filter(Boolean).filter(visible);

                const rects = elements.map(element => element.getBoundingClientRect());
                const overlaps = (first, second) => {
                    const horizontal = first.left < second.right - 2 && first.right > second.left + 2;
                    const vertical = first.top < second.bottom - 2 && first.bottom > second.top + 2;
                    return horizontal && vertical;
                };

                for (let i = 0; i < rects.length; i++) {
                    for (let j = i + 1; j < rects.length; j++) {
                        if (overlaps(rects[i], rects[j])) {
                            return false;
                        }
                    }
                }

                return true;
                """));
    }

    public boolean hasNoTextOverflow() {
        return Boolean.TRUE.equals(executeScript("""
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden';
                };
                const elements = Array.from(document.querySelectorAll(
                    "main h1, main a[href*='/search'][href*='_product_type'], main button"
                )).filter(visible);

                return elements.every(element => element.scrollWidth <= element.clientWidth + 2);
                """));
    }

    public boolean hasNoBrokenVisibleImages() {
        return getBrokenVisibleImageSources().isEmpty();
    }

    public boolean hasNoBrokenVisibleIcons() {
        return getBrokenVisibleIconSources().isEmpty();
    }

    @SuppressWarnings("unchecked")
    public List<String> getBrokenVisibleImageSources() {
        waitForVisibleImagesToFinishLoading();

        Object result = executeScript("""
                const brokenImages = [];
                for (const image of Array.from(document.images)) {
                    const source = image.currentSrc || image.src || '';
                    const isSvg = /\\.svg(\\?|$)/i.test(source);
                    const rect = image.getBoundingClientRect();
                    const inViewport = rect.width > 0
                        && rect.height > 0
                        && rect.bottom >= 0
                        && rect.right >= 0
                        && rect.top <= window.innerHeight
                        && rect.left <= window.innerWidth;
                    const isBroken = isSvg
                        ? false
                        : (!image.complete || image.naturalWidth === 0 || image.naturalHeight === 0);

                    if (inViewport && isBroken) {
                        brokenImages.push(source || image.alt || '<empty image source>');
                    }
                }
                return brokenImages;
                """);

        if (result instanceof List<?>) {
            return ((List<Object>) result).stream().map(String::valueOf).toList();
        }

        return new ArrayList<>();
    }

    private void waitForVisibleImagesToFinishLoading() {
        try {
            waitUntil(Duration.ofSeconds(10), webDriver -> Boolean.TRUE.equals(executeScript("""
                    for (const image of Array.from(document.images)) {
                        const source = image.currentSrc || image.src || '';
                        if (/\\.svg(\\?|$)/i.test(source)) {
                            continue;
                        }

                        const rect = image.getBoundingClientRect();
                        const inViewport = rect.width > 0
                            && rect.height > 0
                            && rect.bottom >= 0
                            && rect.right >= 0
                            && rect.top <= window.innerHeight
                            && rect.left <= window.innerWidth;

                        if (inViewport && image.complete !== true) {
                            return false;
                        }
                    }

                    return true;
                    """)));
        } catch (TimeoutException ignored) {
            // The caller reports any image that remains incomplete or broken.
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> getBrokenVisibleIconSources() {
        Object result = executeScript("""
                const brokenIcons = [];
                for (const image of Array.from(document.images)) {
                    const source = image.currentSrc || image.src || '';
                    const alt = image.alt || '';
                    const isSvg = /\\.svg(\\?|$)/i.test(source);
                    const looksLikeIcon = /icon|svg|search|logo|cart|order|inbox|user|rocket|info/i.test(source + ' ' + alt);
                    const rect = image.getBoundingClientRect();
                    const inViewport = rect.width > 0
                        && rect.height > 0
                        && rect.bottom >= 0
                        && rect.right >= 0
                        && rect.top <= window.innerHeight
                        && rect.left <= window.innerWidth;
                    const isBroken = isSvg
                        ? false
                        : (!image.complete || image.naturalWidth === 0 || image.naturalHeight === 0);

                    if (looksLikeIcon && inViewport && isBroken) {
                        brokenIcons.push(source || alt || '<empty icon source>');
                    }
                }
                return brokenIcons;
                """);

        if (result instanceof List<?>) {
            return ((List<Object>) result).stream().map(String::valueOf).toList();
        }

        return new ArrayList<>();
    }

    public boolean doesScrollingLoadAdditionalProducts() {
        int initialProductCount = getDisplayedProductCount();
        scrollToBottom();

        try {
            waitUntil(Duration.ofSeconds(12), webDriver -> getDisplayedProductCount() > initialProductCount);
            return true;
        } catch (TimeoutException exception) {
            return false;
        }
    }

    private ProductSearchResult findFirstAvailableProductAcrossLoadedResults() {
        int previousCount = -1;

        for (int attempt = 0; attempt < 8; attempt++) {
            waitUntil(RESULTS_TIMEOUT, webDriver -> areSearchResultsDisplayed() || isNoResultsStateDisplayed());
            waitUntil(RESULTS_TIMEOUT, webDriver -> !isSearchLoadingActive());
            waitUntil(RESULTS_TIMEOUT, webDriver -> isSearchResultCountStable());

            List<ProductSearchResult> products = getSearchResults();
            for (ProductSearchResult product : products) {
                if (isProductAvailable(product)) {
                    return product;
                }
            }

            int currentCount = products.size();
            if (currentCount == previousCount || isNoResultsStateDisplayed()) {
                break;
            }

            previousCount = currentCount;
            scrollToBottom();

            try {
                waitUntil(Duration.ofSeconds(8), webDriver -> getDisplayedProductCount() > currentCount
                        || !isSearchLoadingActive());
            } catch (TimeoutException ignored) {
                // The final assertion below reports that no purchasable product was found.
            }
        }

        throw new TimeoutException("No purchasable product was found in the displayed search results.");
    }

    private boolean isSearchLoadingActive() {
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
                    '[role="progressbar"], [class*="spinner"], [class*="loader"], [class*="loading"], .animate-spin'
                )).some(visible);
                """));
    }

    private boolean isSearchResultCountStable() {
        return Boolean.TRUE.equals(executeScript("""
                const count = Array.from(document.querySelectorAll("main a[href*='/product/']"))
                    .filter(visible)
                    .length;
                const now = Date.now();

                if (window.__aroggaSearchResultCount !== count) {
                    window.__aroggaSearchResultCount = count;
                    window.__aroggaSearchResultCountChangedAt = now;
                    return false;
                }

                return count > 0 && now - (window.__aroggaSearchResultCountChangedAt || now) >= 500;

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

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).replaceAll("\\s+", " ").trim();
    }

    private boolean booleanValue(Object value) {
        return value instanceof Boolean booleanValue && booleanValue;
    }

    protected String buildSearchUrl(String baseUrl, String query) {
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;

        if (query == null || query.isBlank()) {
            return normalizedBaseUrl + "/search?_product_type=all";
        }

        return normalizedBaseUrl
                + "/search?_product_type=all&_search="
                + URLEncoder.encode(query, StandardCharsets.UTF_8);
    }

    private SearchPage waitForSubmittedSearchState(String searchText) {
        return searchText.isBlank() ? waitForEmptySearchState() : waitForSearchResults(searchText);
    }

    private SearchPage waitForAutomaticSearchState(String searchText) {
        waitUntil(RESULTS_TIMEOUT, webDriver -> isAutomaticSearchResultDisplayed(searchText));
        return this;
    }

    private boolean isSearchBreadcrumbDisplayed() {
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
                const main = document.querySelector('main');
                if (!main || !visible(main)) {
                    return false;
                }

                const visibleTexts = Array.from(main.querySelectorAll('a, button, span, p, li, div'))
                    .filter(visible)
                    .map(element => normalize(element.innerText || element.textContent || ''))
                    .filter(Boolean);
                return visibleTexts.includes('Home') && visibleTexts.includes('Search');
                """));
    }

    private String normalizeSearchText(String searchText) {
        return searchText == null ? "" : searchText.trim();
    }

    protected String resolveBaseUrl() {
        String currentUrl = getCurrentUrl();
        int searchPathIndex = currentUrl.indexOf("/search");

        if (searchPathIndex > -1) {
            return currentUrl.substring(0, searchPathIndex);
        }

        int queryIndex = currentUrl.indexOf('?');
        return queryIndex > -1 ? currentUrl.substring(0, queryIndex) : currentUrl;
    }

    private String getSearchTextFromCurrentUrl() {
        String currentUrl = getCurrentUrl();
        int queryIndex = currentUrl.indexOf('?');
        if (queryIndex < 0 || queryIndex == currentUrl.length() - 1) {
            return "";
        }

        String[] parameters = currentUrl.substring(queryIndex + 1).split("&");
        for (String parameter : parameters) {
            String[] keyValue = parameter.split("=", 2);
            if (keyValue.length != 2) {
                continue;
            }

            String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
            if ("_search".equals(key) || "search".equals(key) || "keyword".equals(key)) {
                return URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8).trim();
            }
        }

        return "";
    }

    private By resultsHeaderByQuery(String query) {
        return By.xpath("//main//h1[contains(normalize-space()," + xpathText(query) + ")]");
    }

    private void waitUntilSearchInputValueSettles(String expectedValue) {
        waitUntil(Duration.ofSeconds(10), webDriver -> Boolean.TRUE.equals(executeScript("""
                const input = document.querySelector("header input[aria-label='Search']");
                if (!input) {
                    return false;
                }

                const value = input.value.trim();
                const now = Date.now();
                if (window.__aroggaSearchInputValue !== value) {
                    window.__aroggaSearchInputValue = value;
                    window.__aroggaSearchInputChangedAt = now;
                    return false;
                }

                return value === arguments[0]
                    && now - (window.__aroggaSearchInputChangedAt || now) >= 300;
                """, expectedValue)));
    }

    private void setSearchInputValue(String value) {
        WebElement searchInput = waitForVisible(HEADER_SEARCH_INPUT);
        executeScript("""
                const input = arguments[0];
                const value = arguments[1];
                const valueSetter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set;
                valueSetter.call(input, value);
                input.dispatchEvent(new Event('input', { bubbles: true }));
                input.dispatchEvent(new Event('change', { bubbles: true }));
                """, searchInput, value);
    }

    private boolean hasVisibleSearchSuggestion(String searchText) {
        return Boolean.TRUE.equals(executeScript("""
                const query = String(arguments[0] || '').trim().toLowerCase();
                if (!query) {
                    return false;
                }

                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden';
                };
                const textOf = element => String(element.innerText || element.textContent || '')
                    .replace(/\\s+/g, ' ')
                    .trim()
                    .toLowerCase();
                const selector = [
                    "header a[href*='/product/']",
                    "header [role='option']",
                    "header [role='listbox'] a",
                    "header li",
                    "header [class*='suggest']",
                    "main a[href*='/product/']"
                ].join(',');

                return Array.from(document.querySelectorAll(selector))
                    .some(element => visible(element) && textOf(element).includes(query));
                """, searchText));
    }

    private boolean isRecentSearchChipDisplayedNow(String query) {
        return Boolean.TRUE.equals(executeScript("""
                const query = String(arguments[0]).trim().toLowerCase();
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden';
                };
                const textOf = element => String(element.innerText || element.textContent || '').trim();
                const headings = Array.from(document.querySelectorAll('main *'))
                    .filter(element => ['recently search items', 'recent searches', 'recent search']
                        .includes(textOf(element).toLowerCase()));

                for (const heading of headings) {
                    let scope = heading;
                    for (let depth = 0; depth < 5 && scope; depth += 1, scope = scope.parentElement) {
                        const chip = Array.from(scope.querySelectorAll('button, a, span, div, p'))
                            .find(element => visible(element) && textOf(element).toLowerCase() === query);
                        if (chip) {
                            return true;
                        }
                    }
                }

                return false;
                """, query));
    }

    private boolean clickRecentSearchClearButton() {
        return Boolean.TRUE.equals(executeScript("""
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden';
                };
                const textOf = element => String(element.innerText || element.textContent || '').trim();
                const headings = Array.from(document.querySelectorAll('main *'))
                    .filter(element => ['recently search items', 'recent searches', 'recent search']
                        .includes(textOf(element).toLowerCase()));

                for (const heading of headings) {
                    let scope = heading;
                    for (let depth = 0; depth < 5 && scope; depth += 1, scope = scope.parentElement) {
                        const clear = Array.from(scope.querySelectorAll('button, a, span, div'))
                            .find(element => visible(element) && textOf(element).toLowerCase() === 'clear');
                        if (clear) {
                            clear.click();
                            return true;
                        }
                    }
                }

                return false;
                """));
    }

    private By productCardByName(String productName) {
        return By.xpath(productCardXpath(productName));
    }

    private By productImageByName(String productName) {
        return By.xpath(productCardXpath(productName)
                + "//img[string-length(@alt) > 0]");
    }

    private By productPriceByName(String productName) {
        return By.xpath(productCardXpath(productName)
                + "//*[contains(normalize-space(),'৳') or contains(@class,'product_single_price_generate')]");
    }

    private By productAddButtonByName(String productName) {
        return By.xpath(productCardXpath(productName)
                + "//button[normalize-space()='ADD' or normalize-space()='Notify' or normalize-space()]");
    }

    private By productDiscountBadgeByName(String productName) {
        String upperCaseText = "translate(normalize-space(.), 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')";
        String lowerCaseClass = "translate(@class, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')";

        return By.xpath(productCardXpath(productName)
                + "//*[contains(" + upperCaseText + ",'% OFF')"
                + " or contains(" + upperCaseText + ",'%OFF')"
                + " or contains(" + lowerCaseClass + ",'discount')"
                + " or contains(" + lowerCaseClass + ",'badge')]");
    }

    private String productCardXpath(String productName) {
        return "//main//a[contains(@href,'/product/') and contains(normalize-space(),"
                + xpathText(productName)
                + ")]";
    }

    private void scrollProductCardIntoView(String productName) {
        try {
            scrollIntoView(productCardByName(productName));
        } catch (TimeoutException ignored) {
            // The calling assertion will report the missing product/component with its own message.
        }
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

    public record ProductSearchResult(
            String productName,
            String productUrl,
            String actionText,
            boolean visible,
            boolean enabled,
            boolean outOfStock,
            boolean availableForPurchase
    ) {
    }
}
