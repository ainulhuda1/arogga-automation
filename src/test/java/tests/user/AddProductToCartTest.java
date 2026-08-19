package tests.user;

import base.BaseTest;
import constants.TestGroups;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriverException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;
import pages.user.AddProductToCartPage;
import pages.user.CartPage;
import pages.user.CheckoutPage;
import pages.user.HomePage;
import pages.user.ProductDetailsPage;
import pages.user.SearchPage;
import utils.BrowserDiagnosticsUtils;
import utils.TestContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AddProductToCartTest extends BaseTest {

    private static final String ADDED_TO_CART_TOAST = "Added to cart!";
    private static final int EXPECTED_SINGLE_CART_ITEM_COUNT = 1;
    private static final int RAPID_ADD_CLICK_COUNT = 5;
    private static final int PRODUCT_DETAILS_ADD_CANDIDATE_LIMIT = 10;
    private static final List<String> STABLE_PRODUCT_SEARCH_KEYWORDS = List.of(
            "Vaseline Lip Therapy Cocoa Butter 20g",
            "Vaseline Lip Therapy Cocoa Butter",
            "Vaseline",
            "Napa"
    );

    private AddProductToCartPage addProductToCartPage;
    private CartPage cartPage;
    private String selectedProductName;
    private String selectedSearchKeyword;

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
    public void initializeAddToCartPage() {
        restartBrowserAtBaseUrl();
        try {
            initializeAddToCartPageOnce();
        } catch (WebDriverException exception) {
            if (!isRecoverableSessionFailure(exception)) {
                throw exception;
            }
            restartBrowserAtBaseUrl();
            initializeAddToCartPageOnce();
        }
    }

    private void initializeAddToCartPageOnce() {
        driver.get(config.baseUrl());
        loginWithValidCredentials();
        cartPage = new CartPage(driver).clearCartIfNeeded();
        driver.get(config.baseUrl());
        new HomePage(driver).waitUntilLoaded();
        addProductToCartPage = new AddProductToCartPage(driver);
        searchAndCaptureFirstAvailableProduct();
    }

    @AfterMethod(alwaysRun = true)
    public void cleanCartAfterTest() {
        try {
            driver.get(config.baseUrl());
            new CartPage(driver).clearCartIfNeeded();
        } catch (RuntimeException ignored) {
            // Cleanup should not hide the actual test result.
        }
    }

    @Test(groups = {TestGroups.ADD_TO_CART, TestGroups.REGRESSION},
            description = "Verify dynamic search result loads with an available product and no JavaScript errors")
    public void verifyDynamicSearchResultLoadsWithAvailableProductAndNoErrors() {
        driver.get(config.baseUrl());
        BrowserDiagnosticsUtils.clearBrowserLogs(driver);
        searchAndCaptureFirstAvailableProduct();
        List<String> javaScriptErrors = BrowserDiagnosticsUtils.getSevereJavaScriptErrors(driver);

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertTrue(addProductToCartPage.isSearchInputDisplayed(), "Search box should be visible");
        softAssert.assertEquals(addProductToCartPage.getSearchInputValue(), selectedSearchKeyword,
                "Search input should retain the submitted dynamic keyword");
        softAssert.assertTrue(addProductToCartPage.areSearchResultsDisplayed(), "Search results should load successfully");
        softAssert.assertTrue(addProductToCartPage.isProductCardDisplayed(selectedProductName),
                "Dynamic search should return the captured available product card");
        softAssert.assertFalse(addProductToCartPage.isNoResultsStateDisplayed(),
                "No empty state should be shown when dynamic search results exist");
        softAssert.assertTrue(javaScriptErrors.isEmpty(), "JavaScript errors found: " + javaScriptErrors);
        softAssert.assertAll();
    }

    @Test(groups = {TestGroups.ADD_TO_CART, TestGroups.REGRESSION},
            description = "Verify dynamic product card content, assets, formatting and alignment")
    public void verifyDynamicProductCardContentAssetsAndAlignment() {
        List<String> brokenAssets =
                addProductToCartPage.getBrokenProductCardAssetDescriptions(selectedProductName);

        Assert.assertTrue(addProductToCartPage.isProductCardDisplayed(selectedProductName),
                "Captured dynamic product card should be displayed");
        Assert.assertTrue(addProductToCartPage.isProductImageDisplayed(selectedProductName),
                "Dynamic product image should be loaded and not broken");
        Assert.assertTrue(addProductToCartPage.isAvailableProductCardSummaryDisplayed(selectedProductName),
                "Product name, image, price and active ADD button should be displayed for the available product");
        Assert.assertTrue(addProductToCartPage.isProductImageNonPlaceholder(selectedProductName),
                "Dynamic product image should not be a placeholder image");
        Assert.assertTrue(addProductToCartPage.isProductCardLayoutAligned(selectedProductName),
                "Dynamic product card layout should be properly aligned");
        Assert.assertTrue(addProductToCartPage.isAddToCartButtonVisibleEnabledAndAligned(selectedProductName),
                "ADD button should be visible, enabled and aligned");
        Assert.assertEquals(addProductToCartPage.getDisplayedProductActionButtonCount(selectedProductName), 1,
                "Product card should not render duplicate ADD/action buttons");
        Assert.assertTrue(addProductToCartPage.areProductIconsDisplayedCorrectly(selectedProductName),
                "Product icons should render correctly");
        Assert.assertTrue(brokenAssets.isEmpty(),
                "Product card should not contain broken images, missing icons or placeholder assets: " + brokenAssets);
    }

    @Test(groups = {TestGroups.ADD_TO_CART, TestGroups.REGRESSION},
            description = "Verify quantity selector options and selected quantity highlighting")
    public void verifyQuantitySelectorOptionsAndSelectedQuantityHighlighting() {
        addProductToCartPage.clickAddToCart(selectedProductName);
        List<String> quantityOptions = getVisibleQuantityOptions();
        String selectedQuantity = quantityOptions.get(0);

        Assert.assertTrue(addProductToCartPage.isQuantitySelectorDisplayedCorrectly(),
                "Quantity selector modal/dropdown should be displayed correctly");
        Assert.assertTrue(addProductToCartPage.isDefaultQuantityOptionDisplayed(selectedQuantity),
                "The default quantity option should be visible and selected or first in the popup");
        Assert.assertTrue(addProductToCartPage.areQuantityOptionsAccessibleWithoutTruncationOrOverlap(),
                "Quantity options should be accessible without truncation or overlap");

        addProductToCartPage.selectQuantityAndWaitForAddedToast(selectedQuantity);
        TestContext.setSelectedProductQuantity(selectedQuantity);

        Assert.assertTrue(addProductToCartPage.isSelectedQuantityHighlighted(selectedProductName, selectedQuantity),
                "Selected quantity should be highlighted correctly");
        Assert.assertTrue(addProductToCartPage.isAddedToCartToastDisplayed(),
                "Added to cart! toast should be displayed");
        cartPage.waitForHeaderCartBadgeCount(EXPECTED_SINGLE_CART_ITEM_COUNT);
        Assert.assertTrue(cartPage.isHeaderCartBadgeCountDisplayed(EXPECTED_SINGLE_CART_ITEM_COUNT),
                "Cart badge should update after selecting a purchasable quantity");
        Assert.assertTrue(addProductToCartPage.isToastMessageDisplayedWithoutUiDistortion(ADDED_TO_CART_TOAST),
                "Added to cart! toast should display without UI distortion");
    }

    @Test(groups = {TestGroups.ADD_TO_CART, TestGroups.REGRESSION},
            description = "Verify dynamic ADD opens quantity selector without reload and toast appears once then dismisses")
    public void verifyDynamicAddButtonSelectorAndToastLifecycle() {
        String urlBeforeClick = driver.getCurrentUrl();

        Assert.assertTrue(addProductToCartPage.isProductCardDisplayed(selectedProductName),
                "Product card should be visible before clicking ADD");
        Assert.assertEquals(addProductToCartPage.getProductActionText(selectedProductName), "ADD",
                "ADD button should show the correct label before click");
        Assert.assertTrue(addProductToCartPage.isProductActionButtonEnabled(selectedProductName),
                "ADD button should be enabled before click");
        Assert.assertEquals(addProductToCartPage.getDisplayedProductActionButtonCount(selectedProductName), 1,
                "Only one ADD button should be shown on the product card");

        addProductToCartPage.clickAddToCart(selectedProductName);
        List<String> quantityOptions = getVisibleQuantityOptions();
        String selectedQuantity = quantityOptions.get(0);

        Assert.assertEquals(driver.getCurrentUrl(), urlBeforeClick,
                "Opening the quantity selector should not reload or redirect the page");
        Assert.assertTrue(addProductToCartPage.isQuantitySelectorDisplayedCorrectly(),
                "Quantity selector should open after clicking ADD");

        addProductToCartPage.selectQuantityAndWaitForAddedToast(selectedQuantity);
        TestContext.setSelectedProductQuantity(selectedQuantity);
        cartPage.waitForHeaderCartBadgeCount(EXPECTED_SINGLE_CART_ITEM_COUNT);

        Assert.assertEquals(addProductToCartPage.getVisibleToastCount(ADDED_TO_CART_TOAST), 1,
                "Added to cart toast should appear only once");
        Assert.assertTrue(addProductToCartPage.isToastMessageDisplayedWithoutUiDistortion(ADDED_TO_CART_TOAST),
                "Added to cart toast should have proper styling and stay within the viewport");

        addProductToCartPage.waitForToastToAutoDismiss(ADDED_TO_CART_TOAST);

        Assert.assertEquals(addProductToCartPage.getVisibleToastCount(ADDED_TO_CART_TOAST), 0,
                "Added to cart toast should auto dismiss");
    }

    @Test(groups = {TestGroups.ADD_TO_CART, TestGroups.REGRESSION},
            description = "Verify dynamic product can be added to cart and quantity state updates")
    public void verifyDynamicProductAddToCartAndQuantityState() {
        AddProductToCartPage.ProductCardBounds beforeAddBounds =
                addProductToCartPage.getProductCardBounds(selectedProductName);

        addProductToCartPage.clickAddToCart(selectedProductName);
        List<String> quantityOptions = getVisibleQuantityOptions();
        String selectedQuantity = quantityOptions.get(0);

        Assert.assertTrue(addProductToCartPage.isQuantitySelectorDisplayedCorrectly(),
                "Quantity selector should be displayed after clicking ADD");

        addProductToCartPage.selectQuantityAndWaitForAddedToast(selectedQuantity);
        addProductToCartPage.waitForProductActionQuantity(selectedProductName, selectedQuantity);
        TestContext.setSelectedProductQuantity(selectedQuantity);

        cartPage.waitForHeaderCartBadgeCount(EXPECTED_SINGLE_CART_ITEM_COUNT);

        Assert.assertTrue(addProductToCartPage.isAddedToCartToastDisplayed(),
                "Added to cart! toast should be shown after selecting a quantity");
        Assert.assertTrue(cartPage.isHeaderCartBadgeCountDisplayed(EXPECTED_SINGLE_CART_ITEM_COUNT),
                "Cart badge should update after adding the selected dynamic product");
        Assert.assertTrue(cartPage.isCartBadgeIconDisplayedAndAligned(),
                "Cart badge icon should be displayed and the badge count should be aligned");

        cartPage.openCartDrawer()
                .waitForProductInCart(selectedProductName, selectedQuantity)
                .closeCartDrawer();

        Assert.assertTrue(addProductToCartPage.isProductActionChangedToQuantity(selectedProductName, selectedQuantity),
                "Product action should change from ADD to selected quantity");
        Assert.assertTrue(addProductToCartPage.isProductCardUpdatedWithoutBrokenOrOverlappingUi(
                        selectedProductName,
                        selectedQuantity
                ),
                "Product card should update without broken or overlapping UI elements");
        Assert.assertTrue(addProductToCartPage.hasNoUnexpectedLayoutShift(selectedProductName, beforeAddBounds),
                "Product card should not shift unexpectedly after adding to cart");

        if (quantityOptions.size() > 1) {
            String updatedQuantity = chooseShortestAlternateQuantity(quantityOptions, selectedQuantity);
            addProductToCartPage
                    .openQuantitySelector(selectedProductName)
                    .selectQuantity(updatedQuantity)
                    .waitForProductActionQuantity(selectedProductName, updatedQuantity);
            TestContext.setSelectedProductQuantity(updatedQuantity);

            Assert.assertTrue(addProductToCartPage.isProductActionChangedToQuantity(selectedProductName, updatedQuantity),
                    "Product action should update to the selected alternate quantity");
            Assert.assertTrue(addProductToCartPage.isUpdatedQuantityDisplayedWithoutOverlapOrTruncation(
                            selectedProductName,
                            updatedQuantity
                    ),
                    "Updated quantity should display without overlap or truncation");
        }

        cartPage.openCartDrawer()
                .waitForProductLine(selectedProductName);

        Assert.assertTrue(cartPage.isPriceCalculationValid(selectedProductName),
                "Cart line price should calculate correctly for the selected dynamic product");
    }

    @Test(groups = {TestGroups.ADD_TO_CART, TestGroups.REGRESSION},
            description = "Verify cart page dynamic product details, price calculation and refresh persistence")
    public void verifyDynamicCartPagePriceCalculationAndRefreshPersistence() {
        String selectedQuantity = addSelectedProductWithFirstQuantityAndClearToast();

        cartPage.openCartDrawer()
                .waitForProductLine(selectedProductName);
        String cartQuantityText = cartQuantityText();

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertTrue(cartPage.isCartProductLineComplete(
                        selectedProductName,
                        "",
                        cartQuantityText
                ),
                "Cart line should show product image, name, quantity, price and remove button");
        softAssert.assertTrue(cartPage.verifyProductInCart(selectedProductName, selectedQuantity),
                "Cart drawer should contain the captured product and selected pack");
        softAssert.assertTrue(cartPage.verifyPriceDetails(),
                "Cart should display product and payable price details");
        softAssert.assertTrue(cartPage.isPriceCalculationValid(selectedProductName),
                "Cart price calculation mismatch. Breakdown: " + cartPage.getPriceBreakdown(selectedProductName));
        softAssert.assertTrue(cartPage.isDeliveryChargeAppliedAccordingToFreeDeliveryRule(selectedProductName),
                "Delivery charge should be added below the free-delivery threshold and should be zero at/above ৳999. Breakdown: "
                        + cartPage.getPriceBreakdown(selectedProductName));
        softAssert.assertTrue(cartPage.verifyNoBrokenImagesInCart(),
                "Cart should not contain broken product images");
        softAssert.assertTrue(cartPage.verifyNoMissingIconsInCart(),
                "Cart should not contain missing action icons");
        softAssert.assertTrue(cartPage.verifyNoTextTruncationOrOverlap(),
                "Cart drawer should not show overlapping or truncated text");
        softAssert.assertAll();

        cartPage.closeCartDrawer()
                .refreshAndWaitForHeaderCartBadgeCount(EXPECTED_SINGLE_CART_ITEM_COUNT)
                .openCartDrawer()
                .waitForProductLine(selectedProductName);

        Assert.assertTrue(cartPage.verifyProductInCart(selectedProductName),
                "Dynamic product should remain in the cart after browser refresh");
        Assert.assertTrue(cartPage.isHeaderCartBadgeCountDisplayed(EXPECTED_SINGLE_CART_ITEM_COUNT)
                        || cartPage.getCartItemCount() == EXPECTED_SINGLE_CART_ITEM_COUNT,
                "Cart badge should remain correct after browser refresh, or drawer should preserve the single cart item when the header badge lags");
    }

    @Test(groups = {TestGroups.ADD_TO_CART, TestGroups.REGRESSION},
            description = "Verify cart drawer price summary matches checkout payment summary")
    public void verifyCartDrawerPriceSummaryMatchesCheckoutPaymentSummary() {
        String selectedQuantity = addSelectedProductWithFirstQuantityAndClearToast();

        cartPage.openCartDrawer()
                .waitForProductInCart(selectedProductName, selectedQuantity)
                .waitForCartItemCount(EXPECTED_SINGLE_CART_ITEM_COUNT);
        CartPage.CartPriceBreakdown cartDrawerBreakdown = cartPage.getPriceBreakdown(selectedProductName);

        Assert.assertTrue(cartDrawerBreakdown.hasRequiredValues(),
                "Cart drawer price summary should contain product total, delivery and payable amount: "
                        + cartDrawerBreakdown);

        CheckoutPage checkoutPage = cartPage.clickCheckout();
        CheckoutPage.PaymentSummary paymentSummary = checkoutPage.getPaymentSummary();

        Assert.assertTrue(checkoutPage.verifyPaymentSummaryMatchesCartBreakdown(cartDrawerBreakdown),
                "Checkout Payment Summary should match the cart drawer price summary beside checkout. Cart drawer: "
                        + cartDrawerBreakdown
                        + ", payment summary: "
                        + paymentSummary);
    }

    @Test(groups = {TestGroups.ADD_TO_CART, TestGroups.REGRESSION},
            description = "Verify rapid ADD clicks do not create duplicate cart items, toasts or failed requests")
    public void verifyRapidAddClicksDoNotDuplicateCartItemToastOrRequests() {
        BrowserDiagnosticsUtils.clearBrowserLogs(driver);

        addProductToCartPage.rapidlyClickAddToCart(selectedProductName, RAPID_ADD_CLICK_COUNT);
        List<String> quantityOptions = getVisibleQuantityOptions();
        String selectedQuantity = quantityOptions.get(0);

        Assert.assertTrue(addProductToCartPage.isQuantitySelectorDisplayedCorrectly(),
                "Quantity selector should open once after rapid ADD clicks");

        addProductToCartPage.selectQuantityAndWaitForAddedToast(selectedQuantity);
        int visibleAddedToastCount = addProductToCartPage.getVisibleToastCount(ADDED_TO_CART_TOAST);
        TestContext.setSelectedProductQuantity(selectedQuantity);
        cartPage.openCartDrawer()
                .waitForProductInCart(selectedProductName, selectedQuantity)
                .waitForCartItemCount(EXPECTED_SINGLE_CART_ITEM_COUNT);

        List<String> javaScriptErrors = BrowserDiagnosticsUtils.getSevereJavaScriptErrors(driver);
        List<String> failedCartRequests = BrowserDiagnosticsUtils.getFailedNonGetNetworkRequestsRelatedTo(
                driver,
                "cart",
                "basket",
                "add-to-cart"
        );
        cartPage
                .waitForProductLine(selectedProductName)
                .waitForCartItemCount(EXPECTED_SINGLE_CART_ITEM_COUNT);

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertTrue(visibleAddedToastCount <= 1,
                "Rapid ADD clicks should not show duplicate success toasts. Visible toast count: "
                        + visibleAddedToastCount);
        softAssert.assertTrue(cartPage.isHeaderCartBadgeCountDisplayed(EXPECTED_SINGLE_CART_ITEM_COUNT)
                        || cartPage.getCartItemCount() == EXPECTED_SINGLE_CART_ITEM_COUNT,
                "Rapid ADD clicks should add only one cart product");
        softAssert.assertTrue(cartPage.hasSingleProductLine(selectedProductName),
                "Cart drawer should contain only one line for the captured dynamic product");
        softAssert.assertEquals(cartPage.getCartItemCount(), EXPECTED_SINGLE_CART_ITEM_COUNT,
                "Cart count should remain one product after rapid ADD clicks");
        softAssert.assertTrue(javaScriptErrors.isEmpty(), "JavaScript errors found: " + javaScriptErrors);
        softAssert.assertTrue(failedCartRequests.isEmpty(),
                "Failed Add To Cart related requests found: " + failedCartRequests);
        softAssert.assertAll();
    }

    @Test(groups = {TestGroups.ADD_TO_CART, TestGroups.REGRESSION},
            description = "Verify quantity selection and product removal update cart correctly")
    public void verifyQuantitySelectionAndRemoveProduct() {
        addProductToCartPage.clickAddToCart(selectedProductName);
        List<String> quantityOptions = getVisibleQuantityOptions();
        String selectedQuantity = quantityOptions.get(0);

        addProductToCartPage.selectQuantity(selectedQuantity)
                .waitForProductActionQuantity(selectedProductName, selectedQuantity);
        TestContext.setSelectedProductQuantity(selectedQuantity);

        cartPage.waitForHeaderCartBadgeCount(EXPECTED_SINGLE_CART_ITEM_COUNT)
                .openCartDrawer()
                .waitForProductLine(selectedProductName);

        Assert.assertTrue(cartPage.isPriceCalculationValid(selectedProductName),
                "Selected quantity should calculate cart totals correctly");

        if (quantityOptions.size() > 1) {
            String updatedQuantity = chooseShortestAlternateQuantity(quantityOptions, selectedQuantity);
            cartPage.closeCartDrawer();
            addProductToCartPage.openQuantitySelector(selectedProductName)
                    .selectQuantity(updatedQuantity)
                    .waitForProductActionQuantity(selectedProductName, updatedQuantity);
            TestContext.setSelectedProductQuantity(updatedQuantity);

            cartPage.waitForHeaderCartBadgeCount(EXPECTED_SINGLE_CART_ITEM_COUNT)
                    .openCartDrawer()
                    .waitForProductLine(selectedProductName);

            Assert.assertTrue(cartPage.isPriceCalculationValid(selectedProductName),
                    "Updated quantity should recalculate cart totals correctly");
        }

        cartPage.removeProductAndWaitUntilCartIsEmpty(selectedProductName);

        Assert.assertTrue(cartPage.isHeaderCartBadgeCountDisplayed(0) || cartPage.isEmptyCartMessageDisplayed(),
                "Removing the final quantity should clear the cart and show an empty cart state");
    }

    @Test(groups = {TestGroups.ADD_TO_CART, TestGroups.REGRESSION},
            description = "Verify cart behavior after logout and login matches configured business rule")
    public void verifyCartPersistenceAfterLogoutAndLoginMatchesBusinessRule() {
        addSelectedProductWithFirstQuantityAndClearToast();
        int cartCountBeforeLogout = Math.max(cartPage.getCartCount(), EXPECTED_SINGLE_CART_ITEM_COUNT);

        HomePage homePage = new HomePage(driver)
                .clickLogout();

        Assert.assertTrue(homePage.isLogoutConfirmationDisplayed(), "Logout confirmation should be displayed");

        homePage.confirmLogout();

        Assert.assertTrue(homePage.isLoggedOut(), "User should be logged out after confirmation");

        loginWithValidCredentials();

        int expectedCartCount = config.cartPersistsAfterLogoutLogin() ? cartCountBeforeLogout : 0;
        cartPage.openCartDrawer();
        if (expectedCartCount > 0) {
            cartPage.waitForProductLine(selectedProductName)
                    .waitForCartItemCount(expectedCartCount);
            Assert.assertTrue(cartPage.isHeaderCartBadgeCountDisplayed(expectedCartCount)
                            || cartPage.getCartItemCount() == expectedCartCount,
                    "Cart count after logout/login should match cartPersistsAfterLogoutLogin business rule");
            Assert.assertTrue(cartPage.verifyProductInCart(selectedProductName),
                    "Persisted cart should still contain the captured dynamic product after login");
        } else {
            Assert.assertTrue(cartPage.isHeaderCartBadgeCountDisplayed(0) || cartPage.isEmptyCartMessageDisplayed(),
                    "Non-persistent cart rule should show an empty cart after login");
        }
    }

    @Test(groups = {TestGroups.ADD_TO_CART, TestGroups.REGRESSION},
            enabled = false,
            description = "Verify Shopping Cart drawer regression state after adding a dynamic product")
    public void verifyDynamicCartDrawerRegressionAfterAddingProduct() {
        addProductToCartPage.clickAddToCart(selectedProductName);
        List<String> quantityOptions = getVisibleQuantityOptions();
        String selectedQuantity = quantityOptions.get(0);

        Assert.assertTrue(addProductToCartPage.isQuantitySelectorDisplayedCorrectly(),
                "Quantity selector should be displayed before selecting product quantity");

        addProductToCartPage
                .selectQuantityAndWaitForAddedToast(selectedQuantity)
                .waitForProductActionQuantity(selectedProductName, selectedQuantity);
        TestContext.setSelectedProductQuantity(selectedQuantity);
        addProductToCartPage.waitForToastMessagesToClear();

        cartPage.openCartDrawer()
                .waitForProductLine(selectedProductName)
                .waitForCartItemCount(EXPECTED_SINGLE_CART_ITEM_COUNT);

        Assert.assertTrue(cartPage.verifyCartDrawerDisplayed(),
                "Shopping Cart drawer should be displayed after clicking the cart icon");
        Assert.assertTrue(cartPage.verifyCartHeader(),
                "Shopping Cart header text and close button should be visible and aligned");
        Assert.assertTrue(cartPage.verifyStoreTabCount(EXPECTED_SINGLE_CART_ITEM_COUNT),
                "Store tab should be visible, aligned and show count 1");
        Assert.assertTrue(cartPage.verifyProductInCart(selectedProductName),
                "Captured dynamic product should be listed in the Shopping Cart drawer");
        Assert.assertTrue(cartPage.waitForProductDetails(selectedProductName),
                "Captured dynamic product image, name, quantity and price should be visible, aligned and not broken");
        Assert.assertTrue(cartPage.verifyProductInCart(selectedProductName, selectedQuantity),
                "Cart line should show the selected product and pack");
        Assert.assertTrue(cartPage.verifyQuantity(cartQuantityText()),
                "Quantity selector control should be visible and cart line should show a quantity");
        Assert.assertTrue(cartPage.hasSingleProductLine(selectedProductName),
                "Cart drawer should contain only one captured dynamic product before quantity update");

        BrowserDiagnosticsUtils.clearBrowserLogs(driver);

        if (quantityOptions.size() > 1) {
            String updatedQuantity = quantityOptions.get(quantityOptions.size() - 1);
            cartPage
                    .updateProductQuantity(selectedProductName, updatedQuantity)
                    .waitForCartItemCount(EXPECTED_SINGLE_CART_ITEM_COUNT);
            TestContext.setSelectedProductQuantity(updatedQuantity);
        }

        List<String> javaScriptErrors = BrowserDiagnosticsUtils.getSevereJavaScriptErrors(driver);
        List<String> failedQuantityUpdateRequests = BrowserDiagnosticsUtils.getFailedNetworkRequestsRelatedTo(
                driver,
                "cart",
                "basket",
                "quantity",
                "add-to-cart"
        );
        int cartItemCountAfterQuantityHandling = cartPage.getCartItemCount();
        int headerCartBadgeCountAfterQuantityHandling = cartPage.getCartCount();
        int cartLineQuantityAfterQuantityHandling = Math.max(
                EXPECTED_SINGLE_CART_ITEM_COUNT,
                cartPage.getPriceBreakdown(selectedProductName).quantity()
        );

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertTrue(cartPage.verifyProductInCart(selectedProductName),
                "Captured dynamic product should remain listed in the Shopping Cart drawer");
        softAssert.assertTrue(cartPage.hasSingleProductLine(selectedProductName),
                "Cart drawer should still contain only one captured dynamic product after quantity handling");
        softAssert.assertEquals(cartItemCountAfterQuantityHandling, EXPECTED_SINGLE_CART_ITEM_COUNT,
                "No duplicate product entry should be created after quantity handling");
        softAssert.assertTrue(
                headerCartBadgeCountAfterQuantityHandling == EXPECTED_SINGLE_CART_ITEM_COUNT
                        || headerCartBadgeCountAfterQuantityHandling == cartLineQuantityAfterQuantityHandling,
                "Cart badge should reflect either the unique product count or the selected line quantity. Actual badge: "
                        + headerCartBadgeCountAfterQuantityHandling
                        + ", unique drawer items: "
                        + cartItemCountAfterQuantityHandling
                        + ", line quantity: "
                        + cartLineQuantityAfterQuantityHandling
        );
        softAssert.assertTrue(cartPage.isCartBadgeIconDisplayedAndAligned(),
                "Cart badge icon should remain visible and aligned after quantity handling");
        softAssert.assertTrue(cartPage.waitForProductDetails(selectedProductName),
                "Captured dynamic product image, name, quantity and price should remain visible and unchanged");
        softAssert.assertTrue(cartPage.verifyPriceDetails(),
                "Cart should show product and payable price details");
        softAssert.assertTrue(cartPage.isProductSubtotalRecalculatedFromUnitPrice(selectedProductName),
                "Product subtotal should equal Unit Price x Quantity. Breakdown: "
                        + cartPage.getPriceBreakdown(selectedProductName));
        softAssert.assertTrue(cartPage.isDeliveryChargeLineDisplayed(),
                "Cart should show Regular Delivery with the current delivery charge. Breakdown: "
                        + cartPage.getPriceBreakdown(selectedProductName));
        softAssert.assertTrue(cartPage.verifyAmountPayable(),
                "Amount Payable should show the current payable total and be highlighted. Breakdown: "
                        + cartPage.getPriceBreakdown(selectedProductName));
        softAssert.assertTrue(cartPage.isAmountPayableCalculatedFromSubtotalAndDelivery(selectedProductName),
                "Amount Payable should equal Product Subtotal + Delivery Charge - Arogga Cash. Breakdown: "
                        + cartPage.getPriceBreakdown(selectedProductName));
        softAssert.assertTrue(cartPage.isAmountPayableRoundedAccordingToBusinessRule(selectedProductName),
                "Amount Payable should be rounded according to the business rule. Breakdown: "
                        + cartPage.getPriceBreakdown(selectedProductName));
        softAssert.assertTrue(cartPage.verifyPlaceOrderButtonVisibleButNotClicked(),
                "Place Order button should be visible, enabled and aligned but must not be clicked");
        softAssert.assertTrue(cartPage.verifyNoBrokenImagesInCart(),
                "Cart drawer should not contain broken images or placeholder assets");
        softAssert.assertTrue(cartPage.verifyNoMissingIconsInCart(),
                "Cart drawer close, delete and quantity control icons should be visible and rendered");
        softAssert.assertTrue(cartPage.verifyNoTextTruncationOrOverlap(),
                "Cart drawer should not have important text truncation, UI overlap or broken layout after quantity handling");
        softAssert.assertTrue(cartPage.verifyCartDrawerLayoutStableAfterLoading(),
                "Cart drawer layout should remain stable after quantity handling");
        softAssert.assertTrue(cartPage.verifyCartDrawerScrollingWorksIfMultipleProductsPresent(),
                "Cart drawer scrolling should work when multiple products are present during debug state");
        softAssert.assertTrue(javaScriptErrors.isEmpty(),
                "JavaScript errors found during quantity handling: " + javaScriptErrors);
        softAssert.assertTrue(failedQuantityUpdateRequests.isEmpty(),
                "Failed cart quantity update related requests found: " + failedQuantityUpdateRequests);
        softAssert.assertAll();
    }

    @Test(groups = {TestGroups.ADD_TO_CART, TestGroups.REGRESSION},
            description = "Verify first available dynamic search product can be opened and added to cart after login")
    public void verifyAddProductToCartFromSearchResultAfterLoginAndSearch() {
        driver.get(config.baseUrl());
        cartPage.clearCartIfNeeded();
        addProductToCartPage = new AddProductToCartPage(driver);

        int cartCountBeforeAdd = cartPage.getCartCount();
        ProductDetailsPage productDetailsPage = addProductDetailsFromAvailableSearchTerm();
        Assert.assertFalse(selectedProductName.isBlank(),
                "Selected product name should be captured from dynamic search results");

        String selectedQuantity = productDetailsPage.getSelectedQuantity();
        TestContext.setSelectedProductName(selectedProductName);
        TestContext.setSelectedProductQuantity(selectedQuantity);

        cartPage.openCartDrawer()
                .waitForProductInCart(selectedProductName, selectedQuantity)
                .waitForCartItemCount(cartCountBeforeAdd + 1);

        Assert.assertTrue(cartPage.isHeaderCartBadgeCountDisplayed(cartCountBeforeAdd + 1)
                        || cartPage.getCartItemCount() == cartCountBeforeAdd + 1,
                "Cart count should increase by 1 after adding the selected dynamic product");
        Assert.assertTrue(productDetailsPage.verifyCartAddition(),
                "Add to Cart should succeed for the selected dynamic product");

        cartPage.openCartDrawer()
                .waitForProductInCart(selectedProductName, selectedQuantity);

        Assert.assertTrue(cartPage.isCartDrawerDisplayed(), "Cart drawer should open after clicking the cart button");
        Assert.assertTrue(cartPage.verifyProductInCart(selectedProductName, selectedQuantity),
                "Cart drawer should contain Shopping Cart, selected product name, selected pack, item count, payable amount and Place Order");
        Assert.assertTrue(cartPage.getCartItemCount() > 0, "Cart drawer should display item count");
        Assert.assertTrue(cartPage.isPayableAmountDisplayed(), "Cart drawer should display payable amount");
        Assert.assertTrue(cartPage.isPlaceOrderButtonVisible(),
                "Place Order should be visible but must not be clicked by this test");
    }

    private SearchPage.ProductSearchResult searchAndCaptureFirstAvailableProduct() {
        TimeoutException lastException = null;

        for (String keyword : availableProductSearchKeywords()) {
            addProductToCartPage.searchProducts(keyword);
            if (addProductToCartPage.getSearchResults().isEmpty()) {
                continue;
            }

            try {
                SearchPage.ProductSearchResult selectedProduct =
                        addProductToCartPage.getFirstProductWithWorkingQuantitySelector();
                Assert.assertTrue(addProductToCartPage.isProductAvailable(selectedProduct),
                        "Selected dynamic product should be available for purchase.");

                selectedSearchKeyword = keyword;
                selectedProductName = selectedProduct.productName();
                TestContext.setSelectedProductName(selectedProductName);
                return selectedProduct;
            } catch (TimeoutException exception) {
                lastException = exception;
            }
        }

        throw new TimeoutException("No purchasable product that opens the quantity selector was found for search terms: "
                + availableProductSearchKeywords(), lastException);
    }

    private ProductDetailsPage addProductDetailsFromAvailableSearchTerm() {
        TimeoutException lastException = null;
        List<String> rejectedProductNames = new ArrayList<>();

        for (String keyword : availableProductSearchKeywords()) {
            addProductToCartPage.searchProducts(keyword);
            if (addProductToCartPage.getSearchResults().isEmpty()) {
                continue;
            }

            for (int attempt = 0; attempt < PRODUCT_DETAILS_ADD_CANDIDATE_LIMIT; attempt++) {
                ProductDetailsPage productDetailsPage;
                String candidateProductName;
                try {
                    productDetailsPage =
                            addProductToCartPage.selectFirstProductWithWorkingQuantitySelector(rejectedProductNames);
                    candidateProductName = addProductToCartPage.captureSelectedProductName();
                } catch (TimeoutException exception) {
                    lastException = exception;
                    break;
                }

                rejectedProductNames.add(candidateProductName);

                try {
                    selectedSearchKeyword = keyword;
                    selectedProductName = candidateProductName;
                    TestContext.setSelectedProductName(selectedProductName);
                    productDetailsPage.addToCart();
                    selectedProductName = productDetailsPage.getCurrentProductName();
                    TestContext.setSelectedProductName(selectedProductName);
                    return productDetailsPage;
                } catch (TimeoutException exception) {
                    lastException = exception;
                    cartPage.clearCartIfNeeded();
                    driver.get(config.baseUrl());
                    new HomePage(driver).waitUntilLoaded();
                    addProductToCartPage = new AddProductToCartPage(driver);
                    addProductToCartPage.searchProducts(keyword);
                    if (addProductToCartPage.getSearchResults().isEmpty()) {
                        break;
                    }
                }
            }
        }

        throw new TimeoutException("No purchasable product details page was found for search terms: "
                + availableProductSearchKeywords(), lastException);
    }

    private String addSelectedProductWithFirstQuantityAndClearToast() {
        try {
            return addSelectedSearchResultProductWithFirstQuantityAndClearToast();
        } catch (TimeoutException exception) {
            cartPage.clearCartIfNeeded();
            driver.get(config.baseUrl());
            addProductToCartPage = new AddProductToCartPage(driver);

            ProductDetailsPage productDetailsPage = addProductDetailsFromAvailableSearchTerm();
            String selectedQuantity = productDetailsPage.getSelectedQuantity();
            TestContext.setSelectedProductQuantity(selectedQuantity);
            cartPage.openCartDrawer()
                    .waitForProductInCart(selectedProductName, selectedQuantity)
                    .waitForCartItemCount(EXPECTED_SINGLE_CART_ITEM_COUNT)
                    .closeCartDrawer();
            return selectedQuantity;
        }
    }

    private String addSelectedSearchResultProductWithFirstQuantityAndClearToast() {
        addProductToCartPage.clickAddToCart(selectedProductName);
        List<String> quantityOptions = addProductToCartPage.getVisibleQuantityOptionLabels();
        if (quantityOptions.isEmpty()) {
            throw new TimeoutException("Search-result ADD did not expose a purchasable quantity option for: "
                    + selectedProductName);
        }
        String selectedQuantity = quantityOptions.get(0);
        addProductToCartPage.selectQuantity(selectedQuantity)
                .waitForProductActionQuantity(selectedProductName, selectedQuantity);
        TestContext.setSelectedProductQuantity(selectedQuantity);
        cartPage.openCartDrawer()
                .waitForProductInCart(selectedProductName, selectedQuantity)
                .waitForCartItemCount(EXPECTED_SINGLE_CART_ITEM_COUNT)
                .closeCartDrawer();
        addProductToCartPage.waitForToastMessagesToClear();
        return selectedQuantity;
    }

    private List<String> getVisibleQuantityOptions() {
        List<String> quantityOptions = addProductToCartPage.getVisibleQuantityOptionLabels();
        Assert.assertFalse(quantityOptions.isEmpty(),
                "Quantity selector should display at least one purchasable quantity option");
        return quantityOptions;
    }

    private String chooseShortestAlternateQuantity(List<String> quantityOptions, String selectedQuantity) {
        String selectedCanonical = canonicalQuantityForComparison(selectedQuantity);

        return quantityOptions.stream()
                .filter(quantity -> !canonicalQuantityForComparison(quantity).equals(selectedCanonical))
                .min(Comparator.comparingInt(quantity -> quantity.replaceAll("\\s+", " ").trim().length()))
                .orElseThrow(() -> new TimeoutException("No alternate quantity option was available. Options: "
                        + quantityOptions + ", selected: " + selectedQuantity));
    }

    private String canonicalQuantityForComparison(String quantity) {
        return quantity == null ? "" : quantity
                .replace("×", "x")
                .replaceAll("(?i)\\bbot\\b", "bottle")
                .replaceAll("[^A-Za-z0-9]+", "")
                .toLowerCase();
    }

    private String cartQuantityText() {
        int quantity = cartPage.getPriceBreakdown(selectedProductName).quantity();
        return "Qty: " + Math.max(1, quantity);
    }

    private String dynamicProductSearchKeyword() {
        String keyword = config.dynamicProductSearchKeyword();
        return keyword == null || keyword.isBlank() ? STABLE_PRODUCT_SEARCH_KEYWORDS.get(0) : keyword.trim();
    }

    private List<String> availableProductSearchKeywords() {
        List<String> keywords = new ArrayList<>();
        addKeywordIfMissing(keywords, dynamicProductSearchKeyword());
        STABLE_PRODUCT_SEARCH_KEYWORDS.forEach(keyword -> addKeywordIfMissing(keywords, keyword));
        return keywords;
    }

    private void addKeywordIfMissing(List<String> keywords, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return;
        }

        String normalizedKeyword = keyword.trim();
        if (keywords.stream().noneMatch(existing -> existing.equalsIgnoreCase(normalizedKeyword))) {
            keywords.add(normalizedKeyword);
        }
    }
}
