package tests.user;

import base.BaseTest;
import constants.TestGroups;
import org.openqa.selenium.WebDriverException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.user.HomePage;
import pages.user.SearchPage;
import utils.VisualRegressionUtils;

import java.util.List;

public class SearchPageTest extends BaseTest {

    private static final String SEARCH_GROUP = "search";
    private static final String UNKNOWN_PRODUCT = "zzzxxyyqawer";
    private static final String BROAD_SEARCH_TERM = "medicine";

    private SearchPage searchPage;

    @Override
    @BeforeClass(alwaysRun = true)
    public void setUp() {
        super.setUp();
        loginWithValidCredentials();
    }

    @Override
    @AfterClass(alwaysRun = true)
    public void tearDown() {
        super.tearDown();
    }

    @BeforeMethod(alwaysRun = true)
    public void initializeSearchPage() {
        restartBrowserAtBaseUrl();
        try {
            initializeSearchPageOnce();
        } catch (WebDriverException exception) {
            if (!isRecoverableSessionFailure(exception)) {
                throw exception;
            }
            restartBrowserAtBaseUrl();
            initializeSearchPageOnce();
        }
    }

    private void initializeSearchPageOnce() {
        driver.get(config.baseUrl());
        loginWithValidCredentials();
        searchPage = new SearchPage(driver);
    }

    @Test(groups = {SEARCH_GROUP, TestGroups.REGRESSION}, enabled = false,
            description = "Skipped: Search button entry point is no longer applicable")
    public void verifySearchPageLoadsFromHeaderSearchEntryPoint() {
        searchPage.clickSearchEntryPoint();

        Assert.assertTrue(searchPage.isSearchPageDisplayed(), "Search page should be displayed successfully");
        Assert.assertTrue(searchPage.isSearchInputDisplayed(), "Search input field should be displayed");
        Assert.assertEquals(searchPage.getNormalizedSearchInputPlaceholder(), "Search Products",
                "Search input placeholder should be Search Products");
        Assert.assertTrue(searchPage.getSearchPageContextText().contains("Search Store"),
                "Search page breadcrumb should show Search Store context");
        Assert.assertTrue(searchPage.isSearchIconDisplayed(), "Search icon should be displayed");
        Assert.assertTrue(searchPage.areSearchTabsVisible(), "Store, Lab and Doctor tabs should be visible");
        Assert.assertTrue(searchPage.areCoreSearchUiComponentsDisplayed(),
                "Search page core UI components should load correctly");
    }

   /* @Test(groups = {SEARCH_GROUP, TestGroups.REGRESSION},
            description = "Verify dynamic automatic search results while typing")
    public void verifyDynamicSearchResults() {
        String searchKeyword = dynamicProductSearchKeyword();
        searchPage.searchProducts(searchKeyword);
        SearchPage.ProductSearchResult selectedProduct = searchPage.getFirstAvailableProduct();

        Assert.assertEquals(searchPage.getSearchInputValue(), searchKeyword,
                "Search input should contain only the configured dynamic keyword");
        Assert.assertTrue(searchPage.isProductAvailable(selectedProduct),
                "Search should expose at least one purchasable dynamic product");
    } */

    @Test(groups = {SEARCH_GROUP, TestGroups.REGRESSION},
            description = "Verify typing dynamic keyword displays automatic results")
    public void verifyDynamicSearchShowsAutomaticResultsAfterTyping() {
        String searchKeyword = dynamicProductSearchKeyword();
        searchPage.searchForProduct(searchKeyword);

        Assert.assertTrue(searchPage.isAutomaticSearchResultDisplayed(searchKeyword),
                "Typing the dynamic keyword should display suggestions/results without clicking Search");
    }

    @Test(groups = {SEARCH_GROUP, TestGroups.REGRESSION},
            description = "Verify search suggestions are displayed while typing")
    public void verifySearchSuggestionsAreDisplayedWhileTyping() {
        String searchKeyword = dynamicProductSearchKeyword();
        searchPage.typeSearchTextForSuggestions(searchKeyword);

        Assert.assertTrue(searchPage.areSearchSuggestionsDisplayedFor(searchKeyword),
                "Search suggestions should be displayed while typing");
    }

    @Test(groups = {SEARCH_GROUP, TestGroups.REGRESSION},
            description = "Verify Search page back navigation")
    public void verifyBackButtonIsVisibleAndFunctional() {
        searchPage.openSearchResults(config.baseUrl(), dynamicProductSearchKeyword());

        Assert.assertTrue(searchPage.isBackButtonVisible(), "Back navigation should be visible on Search page");

        HomePage homePage = searchPage.clickBackButton();

        Assert.assertTrue(homePage.isHomePageLoaded(), "Back navigation should return user to Home page");
    }

    @Test(groups = {SEARCH_GROUP, TestGroups.REGRESSION},
            description = "Verify Search page UI integrity")
    public void verifySearchPageUiIntegrity() {
        searchPage.openSearchResults(config.baseUrl(), dynamicProductSearchKeyword());
        SearchPage.ProductSearchResult selectedProduct = searchPage.getFirstAvailableProduct();
        searchPage.waitForProductCard(selectedProduct.productName());
        List<String> brokenIcons = searchPage.getBrokenVisibleIconSources();
        VisualRegressionUtils.VisualComparisonResult visualResult =
                VisualRegressionUtils.comparePageWithBaseline(
                        driver,
                        "search_page_dynamic_product",
                        config.visualBaselineEnforced(),
                        config.visualMismatchThresholdPercent()
                );

        Assert.assertTrue(searchPage.isSearchInputAligned(), "Search input should be aligned with the search icon");
        Assert.assertTrue(searchPage.isPlaceholderTextVisibleAndAligned(),
                "Placeholder text should be visible and aligned");
        Assert.assertTrue(brokenIcons.isEmpty(), "Broken visible icons found: " + brokenIcons);
        Assert.assertTrue(searchPage.isProductImageDisplayed(selectedProduct.productName()),
                "Selected dynamic product image should be visible and loaded: " + selectedProduct.productName());
        Assert.assertTrue(searchPage.areFontsRenderedCorrectly(), "Fonts should render correctly");
        Assert.assertTrue(searchPage.areSpacingPaddingAndAlignmentValid(),
                "Spacing, padding and alignment should be valid");
        Assert.assertTrue(searchPage.hasNoOverlappingUiElements(), "Search page UI elements should not overlap");
        Assert.assertTrue(searchPage.hasNoTextOverflow(), "Search page labels should not be truncated or overflow");
        Assert.assertTrue(visualResult.passed(), visualResult.message());
    }

    @Test(groups = {SEARCH_GROUP, TestGroups.REGRESSION},
            description = "Verify empty search behavior and recent search clear")
    public void verifyEmptySearchBehaviorAndRecentSearchClear() {
        String searchKeyword = dynamicProductSearchKeyword();
        searchPage.searchForProductAndRecordRecentSearch(searchKeyword)
                .submitEmptySearch()
                .waitForRecentSearchChip(searchKeyword);

        Assert.assertTrue(searchPage.isEmptySearchStateDisplayed(),
                "Empty search should display recent or trending search content");
        Assert.assertTrue(searchPage.isRecentSearchChipDisplayed(searchKeyword),
                "Recent search chip for the dynamic keyword should appear");

        searchPage.clearRecentSearches(searchKeyword);

        Assert.assertFalse(searchPage.isRecentSearchChipDisplayed(searchKeyword),
                "Clear should remove recent searches");
    }

    @Test(groups = {SEARCH_GROUP, TestGroups.REGRESSION},
            description = "Verify scrolling loads additional products")
    public void verifyScrollingLoadsAdditionalProducts() {
        searchPage.openSearchResults(config.baseUrl(), BROAD_SEARCH_TERM);

        Assert.assertTrue(searchPage.doesScrollingLoadAdditionalProducts(),
                "Scrolling should load additional products; product count did not increase after scrolling");
    }

    @Test(groups = {SEARCH_GROUP, TestGroups.REGRESSION},
            description = "Verify No Results state")
    public void verifyNoResultsStateWhenNoMatchingProductsExist() {
        searchPage.openSearchResults(config.baseUrl(), UNKNOWN_PRODUCT);

        Assert.assertTrue(searchPage.waitUntilNoResultsStateDisplayed(),
                "No Results state should be displayed when no matching products exist");
    }

    private String dynamicProductSearchKeyword() {
        String keyword = config.dynamicProductSearchKeyword();
        return keyword == null || keyword.isBlank() ? "Vaseline Lip Therapy Cocoa Butter 20g" : keyword.trim();
    }
}
