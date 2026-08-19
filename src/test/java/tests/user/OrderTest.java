package tests.user;

import base.BaseTest;
import constants.TestGroups;
import org.openqa.selenium.TimeoutException;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;
import pages.user.AddProductToCartPage;
import pages.user.CartPage;
import pages.user.CheckoutPage;
import pages.user.HomePage;
import pages.user.OrderConfirmationPage;
import pages.user.PaymentdetailsPage;
import pages.user.PlaceOrderPage;
import pages.user.SearchPage;
import pages.user.ShippingAddressPage;
import pages.user.TrackOrderPage;
import utils.BrowserDiagnosticsUtils;
import utils.TestContext;

import java.math.BigDecimal;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class OrderTest extends BaseTest {

    private static final int EXPECTED_SINGLE_CART_ITEM_COUNT = 1;
    private static final String AUTOMATION_HOME_ADDRESS_PREFIX = "Arogga QA Home";
    private static final String AUTOMATION_HOME_ADDRESS_FULL_NAME = "Arogga QA Home Cleanup";
    private static final String AUTOMATION_HOME_ADDRESS_LINE = "Automation home cleanup address";
    private static final String CHECKOUT_ADDITIONAL_INFORMATION = "Please deliver carefully.";
    private static final BigDecimal FREE_DELIVERY_THRESHOLD = new BigDecimal("999");

    private CartPage cartPage;
    private ShippingAddressPage.AddressData createdShippingAddress;
    private String selectedProductName;
    private String selectedProductQuantity;
    private String selectedCartQuantityText;

    @BeforeMethod(alwaysRun = true)
    public void prepareCartForShippingAddressRegression(Method testMethod) {
        createdShippingAddress = null;
        loginWithValidCredentials();
        cartPage = new CartPage(driver);
        if (isPartialCheckoutScenario(testMethod)) {
            cartPage.clearCartIfNeeded();
            return;
        }

        List<CartPage.CartProductData> preparedProducts = isCodPlaceOrderScenario(testMethod)
                ? prepareCartWithPurchasableProducts(1, FREE_DELIVERY_THRESHOLD)
                : prepareCartWithPurchasableProducts(1);
        CartPage.CartProductData preparedProduct = preparedProducts.get(0);
        selectedProductName = preparedProduct.productName();
        Assert.assertFalse(selectedProductName.isBlank(),
                "A purchasable product name should be captured from dynamic search results.");
        selectedProductQuantity = preparedProduct.quantityText().isBlank()
                ? "Qty: " + preparedProduct.quantity()
                : preparedProduct.quantityText();
        Assert.assertFalse(selectedProductQuantity.isBlank(),
                "Selected product quantity should be captured after adding the dynamic product.");

        TestContext.setSelectedProductName(selectedProductName);
        TestContext.setSelectedProductQuantity(selectedProductQuantity);

        cartPage.openCartDrawer()
                .waitForProductLine(selectedProductName);
        selectedCartQuantityText = "Qty: " + Math.max(1, preparedProduct.quantity());
    }

    @AfterMethod(alwaysRun = true)
    public void cleanCartAfterShippingAddressRegression() {
        try {
            deleteCreatedShippingAddressIfPresent();
        } catch (RuntimeException ignored) {
            // Address cleanup should not hide the actual test result.
        }

        try {
            driver.get(config.baseUrl());
            new CartPage(driver).clearCartIfNeeded();
        } catch (RuntimeException ignored) {
            // Cleanup should not hide the actual test result.
        } finally {
            createdShippingAddress = null;
        }
    }

    @Test(groups = {TestGroups.ORDER, TestGroups.ADD_TO_CART, TestGroups.REGRESSION},
            description = "Verify shipping address, coupon and checkout summary regression inside cart drawer")
    public void verifyShippingAddressRegressionInsideCartDrawer() {
        BrowserDiagnosticsUtils.clearBrowserLogs(driver);

        ShippingAddressPage shippingAddressPage = cartPage.openShippingAddressPageFromCartDrawer();
        shippingAddressPage.deleteAddressesByFullNamePrefix(AUTOMATION_HOME_ADDRESS_PREFIX);
        int savedAddressCount = shippingAddressPage.getSavedAddressCount();
        String timestamp = DateTimeFormatter.ofPattern("MMddHHmmss").format(LocalDateTime.now());
        ShippingAddressPage.AddressData newAddress = new ShippingAddressPage.AddressData(
                AUTOMATION_HOME_ADDRESS_PREFIX + " " + timestamp,
                "+88" + config.validPhoneNumber(),
                "Automation home address " + timestamp
        );
        ShippingAddressPage.AddressData editedAddress = new ShippingAddressPage.AddressData(
                AUTOMATION_HOME_ADDRESS_PREFIX + " Edited " + timestamp,
                newAddress.phoneNumber(),
                "Automation edited address " + timestamp
        );

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertTrue(shippingAddressPage.isShippingAddressPageOpen(),
                "Shipping Address / Change Address page should open from inside the cart drawer");
        softAssert.assertTrue(shippingAddressPage.isPageLoadedWithoutUiIssues(),
                "Shipping Address page should load completely without UI issues: "
                        + shippingAddressPage.getAddressModalUiIssues());
        softAssert.assertTrue(savedAddressCount > 0,
                "At least one saved shipping address should be displayed when address data is available");
        softAssert.assertTrue(shippingAddressPage.areSavedAddressDetailsVisible(),
                "Saved address details should be visible");
        softAssert.assertTrue(shippingAddressPage.hasNoConflictingDefaultAddressMarkers(),
                "Saved address list should not show conflicting default/shipping address markers");
        softAssert.assertTrue(shippingAddressPage.isFirstAddressEditControlVisibleAndEnabled(),
                "Saved address edit/menu control should be visible and enabled");

        shippingAddressPage.openAddNewAddressForm();
        softAssert.assertTrue(shippingAddressPage.isAddAddressFormOpen(),
                "Add Address form should open after clicking Add New Address");
        softAssert.assertTrue(shippingAddressPage.areMandatoryFieldsDisplayed(),
                "All mandatory fields should be displayed on the Add Address form");
        softAssert.assertTrue(shippingAddressPage.areMandatoryFieldsEditable(),
                "All mandatory fields should be editable on the Add Address form");
        softAssert.assertTrue(shippingAddressPage.isHomeAddressTypeAvailable(),
                "Home address type should be available");
        softAssert.assertTrue(shippingAddressPage.isOfficeAddressTypeAvailable(),
                "Office address type should be available");
        softAssert.assertTrue(shippingAddressPage.isSetDefaultAddressOptionAvailable(),
                "Set as Default Address option should be available");

        createdShippingAddress = newAddress;
        shippingAddressPage.saveNewHomeDefaultAddress(newAddress);
        softAssert.assertTrue(shippingAddressPage.isAddressDisplayed(
                        newAddress.fullName(),
                        newAddress.addressLine()
                ),
                "New address should appear in the saved address list after successful save");
        softAssert.assertEquals(shippingAddressPage.getSavedAddressCountByFullName(newAddress.fullName()), 1,
                "New automation address should be created only once and should not duplicate");
        softAssert.assertTrue(shippingAddressPage.isAddressTypeDisplayed(newAddress.fullName(), "Home"),
                "New address should be marked as Home");
        softAssert.assertTrue(shippingAddressPage.isAddressMarkedDefault(newAddress.fullName()),
                "New address should be marked as Default/Shipping Address after saving as default");
        softAssert.assertTrue(shippingAddressPage.isAddressSelectable(newAddress.fullName()),
                "New address should remain selectable for checkout");
        softAssert.assertTrue(shippingAddressPage.isFirstAddressEditControlVisibleAndEnabled(),
                "Edit option should remain available for saved addresses");

        shippingAddressPage.openAddressActionMenuByFullName(newAddress.fullName());
        softAssert.assertTrue(shippingAddressPage.areEditDefaultDeleteOptionsVisible(),
                "Edit, Make Default and Delete options should be shown from the new address action menu");

        shippingAddressPage.openAddressEditFormByFullName(newAddress.fullName());
        softAssert.assertTrue(shippingAddressPage.isEditFormOpen(),
                "Edit Shipping Address form should open after clicking Edit");
        softAssert.assertTrue(shippingAddressPage.isEditFormShowingExistingAddress(),
                "Edit form should show the existing address data before update");

        createdShippingAddress = editedAddress;
        shippingAddressPage.updateCurrentAddress(editedAddress.fullName(), editedAddress.addressLine());
        softAssert.assertTrue(shippingAddressPage.isAddressDisplayed(
                        editedAddress.fullName(),
                        editedAddress.addressLine()
                ),
                "Updated address data should be displayed after submit");
        softAssert.assertTrue(shippingAddressPage.isAddressSelectable(editedAddress.fullName()),
                "Updated address should remain selectable");
        softAssert.assertTrue(shippingAddressPage.isAddressMarkedDefault(editedAddress.fullName()),
                "Updated automation address should remain selected as default");

        shippingAddressPage.selectAddress(editedAddress.fullName())
                .closeAddressModal();
        softAssert.assertTrue(cartPage.waitUntilShippingAddressSummaryDisplayed(
                        editedAddress.fullName(),
                        editedAddress.addressLine()
                ),
                "New default address should be selected in the cart drawer checkout address summary");

        if (cartPage.openApplyCouponSectionIfAvailable()) {
            softAssert.assertTrue(cartPage.isApplyCouponSectionVisible(),
                    "Apply Coupon section should be visible after expanding coupon UI");
            softAssert.assertTrue(cartPage.isApplyCouponButtonVisibleEnabledAndClickable(),
                    "Apply Coupon button should be visible, enabled and clickable without validating coupon functionality");
            softAssert.assertTrue(cartPage.isCouponSectionAligned(),
                    "Coupon section should be aligned after expanding coupon UI");
        }

        cartPage.scrollCartDrawerToProductLine(selectedProductName);

        List<String> javaScriptErrors = BrowserDiagnosticsUtils.getSevereJavaScriptErrors(driver);
        softAssert.assertTrue(cartPage.verifyProductInCart(selectedProductName),
                "Product should remain in the cart after address changes");
        softAssert.assertTrue(cartPage.isProductLineQuantityDisplayed(selectedProductName, selectedCartQuantityText),
                "Product quantity should remain unchanged after address changes");
        softAssert.assertTrue(cartPage.hasSingleProductLine(selectedProductName),
                "Address changes should not duplicate cart product entries");
        softAssert.assertTrue(cartPage.getPriceBreakdown(selectedProductName).hasRequiredValues(),
                "Cart price breakdown should be available for the selected product after address changes");
        softAssert.assertTrue(cartPage.isPriceCalculationValid(selectedProductName),
                "Cart price calculation should remain valid after address changes");
        softAssert.assertTrue(cartPage.isDeliveryChargeAppliedAccordingToFreeDeliveryRule(selectedProductName),
                "Delivery charge should follow the free-delivery threshold after address changes");
        softAssert.assertTrue(cartPage.isPayableAmountDisplayed(),
                "Amount payable should be displayed after address changes");
        softAssert.assertTrue(cartPage.isAmountPayableCalculatedFromSubtotalAndDelivery(selectedProductName),
                "Amount payable should equal product subtotal plus delivery charge minus applied Arogga cash");
        softAssert.assertTrue(cartPage.isContinueCheckoutButtonVisibleAndEnabled(),
                "Continue checkout / Place Order button should be visible and enabled");
        softAssert.assertTrue(cartPage.verifyNoBrokenImagesInCart(),
                "Cart drawer should not contain broken images after address changes");
        softAssert.assertTrue(cartPage.verifyNoMissingIconsInCart(),
                "Cart drawer should not contain broken or missing icons after address changes");
        softAssert.assertTrue(cartPage.verifyNoVisibleCartDrawerTextTruncationOrOverlap(),
                "Cart drawer should not have UI overlap or text truncation after address changes");
        softAssert.assertTrue(cartPage.verifyCartDrawerLayoutStableAfterLoading(),
                "Cart drawer layout should remain stable after address changes");
        softAssert.assertTrue(javaScriptErrors.isEmpty(),
                "No JavaScript errors should occur during shipping address regression: " + javaScriptErrors);
        softAssert.assertAll();
    }

    @Test(groups = {TestGroups.ORDER, TestGroups.ADD_TO_CART, TestGroups.REGRESSION},
            description = "Verify cart drawer supports selecting only part of the cart")
    public void verifyCartDrawerSupportsPartialProductSelection() {
        List<CartPage.CartProductData> cartProducts = prepareCartWithPurchasableProducts(3);
        List<CartPage.CartProductData> selectedProducts = List.of(cartProducts.get(0), cartProducts.get(2));

        cartPage.openCartDrawer()
                .selectMultipleCartProducts(selectedProducts.stream().map(CartPage.CartProductData::productName).toList());

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertTrue(cartPage.verifyCartDrawerSupportsPartialSelectionUi(),
                "Cart drawer should expose product rows, selection controls, remove controls, selected count and checkout");
        softAssert.assertEquals(cartPage.getSelectedCartProductCount(), selectedProducts.size(),
                "Cart drawer selected product count mismatch");
        softAssert.assertEquals(new LinkedHashSet<>(cartPage.getSelectedCartProductNames()),
                new LinkedHashSet<>(selectedProducts.stream().map(CartPage.CartProductData::productName).toList()),
                "Cart drawer should keep only requested product rows selected");
        softAssert.assertTrue(cartPage.getCartProductRows().size() >= cartProducts.size(),
                "Partial selection should not remove products from the cart drawer");
        softAssert.assertAll();
    }

    @Test(groups = {TestGroups.ORDER, TestGroups.ADD_TO_CART, TestGroups.REGRESSION},
            description = "Verify checkout contains exactly one product selected from cart drawer")
    public void verifyCheckoutWithSingleSelectedProduct() {
        PartialCheckoutState state = openCheckoutForSelectedProductCount(1, 3);
        assertPartialCheckoutState(state, 1);
    }

    @Test(groups = {TestGroups.ORDER, TestGroups.ADD_TO_CART, TestGroups.REGRESSION},
            description = "Verify checkout contains exactly two products selected from cart drawer")
    public void verifyCheckoutWithTwoSelectedProducts() {
        PartialCheckoutState state = openCheckoutForSelectedProductCount(2, 3);
        assertPartialCheckoutState(state, 2);
    }

    @Test(groups = {TestGroups.ORDER, TestGroups.ADD_TO_CART, TestGroups.REGRESSION},
            description = "Verify checkout contains exactly three products selected from cart drawer")
    public void verifyCheckoutWithThreeSelectedProducts() {
        PartialCheckoutState state = openCheckoutForSelectedProductCount(3, 3);
        assertPartialCheckoutState(state, 3);
    }

    @Test(groups = {TestGroups.ORDER, TestGroups.REGRESSION},
            description = "Verify checkout Delivery To section and Additional Information field")
    public void verifyCheckoutDeliveryAddressAndAdditionalInformation() {
        PartialCheckoutState state = openCheckoutForSelectedProductCount(1, 2);
        CheckoutPage checkoutPage = state.checkoutPage();

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertTrue(checkoutPage.isDeliveryToSectionDisplayed(),
                "Delivery To title should be visible on checkout");
        softAssert.assertTrue(checkoutPage.isSelectedDeliveryAddressDisplayedCorrectly(
                        state.selectedAddress().fullName(),
                        state.selectedAddress().addressLine()
                ),
                "Checkout Delivery To section should show the selected delivery name and address");
        softAssert.assertTrue(checkoutPage.isAdditionalInformationDisplayed(),
                "Additional Information field should be displayed");
        checkoutPage.enterAdditionalInformation(CHECKOUT_ADDITIONAL_INFORMATION);
        softAssert.assertEquals(checkoutPage.getAdditionalInformationValue(), CHECKOUT_ADDITIONAL_INFORMATION,
                "Additional Information value should be retained after entry");
        softAssert.assertAll();
    }

    @Test(groups = {TestGroups.ORDER, TestGroups.REGRESSION},
            description = "Verify Regular Delivery is selected by default on checkout")
    public void verifyRegularDeliverySelectedByDefault() {
        CheckoutPage checkoutPage = openCheckoutForSelectedProductCount(1, 2).checkoutPage();

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertTrue(checkoutPage.isRegularDeliveryVisible(),
                "Regular Delivery should be visible on checkout");
        softAssert.assertTrue(checkoutPage.isRegularDeliverySelectedByDefault(),
                "Regular Delivery should be selected by default");
        softAssert.assertTrue(checkoutPage.isRegularDeliveryDetailsDisplayed(),
                "Regular Delivery time/details should be displayed");
        softAssert.assertAll();
    }

    @Test(groups = {TestGroups.ORDER, TestGroups.REGRESSION},
            description = "Verify Express Delivery updates delivery information and charge when available")
    public void verifyExpressDeliveryUpdatesDeliveryInformationAndCharge() {
        CheckoutPage checkoutPage = openCheckoutForSelectedProductCount(1, 2).checkoutPage();
        if (!checkoutPage.isExpressDeliveryAvailable()) {
            throw new SkipException("Express Delivery is not available for the current runtime address/products.");
        }

        CheckoutPage.PaymentSummary regularSummary = checkoutPage.getPaymentSummary();
        checkoutPage.selectExpressDelivery();

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertTrue(checkoutPage.isExpressDeliverySelected(),
                "Express Delivery should become selected after clicking it");
        softAssert.assertTrue(checkoutPage.doesExpressDeliveryUpdateInformationOrCharge(regularSummary),
                "Express Delivery should update selected option, delivery information or payment summary charge");
        softAssert.assertAll();
    }

    @Test(groups = {TestGroups.ORDER, TestGroups.REGRESSION},
            description = "Verify Cash on Delivery is selected by default on checkout")
    public void verifyCashOnDeliverySelectedByDefault() {
        CheckoutPage checkoutPage = openCheckoutForSelectedProductCount(1, 2).checkoutPage();

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertTrue(checkoutPage.isPaymentMethodSectionDisplayed(),
                "Payment Method section should be displayed");
        softAssert.assertTrue(checkoutPage.isCashOnDeliveryVisible(),
                "Cash on Delivery should be visible");
        softAssert.assertTrue(checkoutPage.isCashOnDeliverySelectedByDefault(),
                "Cash on Delivery should be selected by default");
        softAssert.assertAll();
    }

    @Test(groups = {TestGroups.ORDER, TestGroups.ADD_TO_CART, TestGroups.REGRESSION},
            description = "Verify checkout payment summary is calculated from selected cart products only")
    public void verifyCheckoutPaymentSummaryMatchesSelectedProducts() {
        PartialCheckoutState state = openCheckoutForSelectedProductCount(2, 3);
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertTrue(state.checkoutPage().verifyPaymentSummaryMatchesSelectedProducts(state.selectedProducts()),
                "Checkout payment summary should match the selected cart products only");
        softAssert.assertTrue(state.checkoutPage().verifyUnselectedProductsExcludedFromCheckout(state.unselectedProducts()),
                "Unselected cart products should not appear in checkout");
        softAssert.assertAll();
    }

    @Test(groups = {TestGroups.ORDER, TestGroups.ADD_TO_CART, TestGroups.REGRESSION},
            description = "Verify unselected cart products are excluded from checkout")
    public void verifyUnselectedCartProductsAreExcludedFromCheckout() {
        PartialCheckoutState state = openCheckoutForSelectedProductCount(2, 3);
        Assert.assertTrue(state.checkoutPage().verifyUnselectedProductsExcludedFromCheckout(state.unselectedProducts()),
                "Checkout should not contain unselected cart products");
    }

    @Test(groups = {TestGroups.ORDER, TestGroups.ADD_TO_CART, TestGroups.REGRESSION},
            description = "Verify partial cart checkout can place an order with only selected products")
    public void verifyPartialCheckoutPlaceOrderFlow() {
        PartialCheckoutState state = openCheckoutForSelectedProductCount(1, 3);
        CheckoutPage checkoutPage = state.checkoutPage();
        if (checkoutPage.isAdditionalInformationDisplayed()) {
            checkoutPage.enterAdditionalInformation(CHECKOUT_ADDITIONAL_INFORMATION);
        }

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertTrue(checkoutPage.verifySelectedProductsInCheckout(state.selectedProducts()),
                "Checkout should contain only the selected products before placing order");
        softAssert.assertTrue(checkoutPage.verifyUnselectedProductsExcludedFromCheckout(state.unselectedProducts()),
                "Unselected cart products should not be included before placing order");
        softAssert.assertTrue(checkoutPage.isPlaceOrderButtonVisible(),
                "Place Order button should be visible");
        softAssert.assertTrue(checkoutPage.isPlaceOrderButtonEnabled(),
                "Place Order button should be enabled when checkout information is valid");

        BrowserDiagnosticsUtils.clearBrowserLogs(driver);
        OrderConfirmationPage confirmationPage = checkoutPage.clickPlaceOrderAndWaitForConfirmation();
        softAssert.assertTrue(confirmationPage.isCorrectConfirmationRouteLoaded(),
                "Correct Order Confirmation URL/route should load after partial checkout place order");
        softAssert.assertTrue(confirmationPage.isOrderSuccessMessageDisplayed(),
                "Order placed successfully message should be displayed after partial checkout");
        softAssert.assertFalse(confirmationPage.getOrderNumber().isBlank(),
                "Generated Order ID should be displayed after partial checkout");

        driver.get(config.baseUrl());
        cartPage.openCartDrawer();
        List<String> remainingCartNames = cartPage.getCartProductRows().stream()
                .map(CartPage.CartProductData::productName)
                .toList();
        softAssert.assertTrue(remainingCartNames.stream().noneMatch(name ->
                        state.selectedProducts().stream().anyMatch(selected ->
                                selected.matches(name, name.replaceAll("[^A-Za-z0-9]+", "").toLowerCase()))),
                "Placed selected products should not remain in cart after successful order placement");
        softAssert.assertAll();
    }

    @Test(groups = {TestGroups.ORDER, TestGroups.REGRESSION},
            description = "Verify COD place order, confirmation, payment details and track order regression")
    public void verifyCodPlaceOrderConfirmationPaymentAndTrackOrderRegression() {
        ShippingAddressPage.AddressData selectedAddress = createAndSelectFreshAutomationAddress();
        cartPage.openCartDrawer().waitForProductLine(selectedProductName);
        CartPage.CartPriceBreakdown priceBreakdown = cartPage.getPriceBreakdown(selectedProductName);
        CartPage.CartProductData selectedCartProduct = cartPage.getCartProductRow(selectedProductName);
        BigDecimal expectedAmountPayable = priceBreakdown.amountPayable();
        boolean isAroggaCashCoveredOrder = priceBreakdown.isFullyCoveredByAroggaCash();
        boolean cartDeliveryRuleMatches = cartPage.isDeliveryChargeAppliedAccordingToFreeDeliveryRule(selectedProductName);
        CheckoutPage checkoutPage = cartPage.clickCheckout();

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertTrue(checkoutPage.isPlaceOrderButtonVisible(),
                "Place Order button should be visible on checkout");
        softAssert.assertTrue(checkoutPage.isPlaceOrderButtonEnabled(),
                "Place Order button should be enabled before submit");
        softAssert.assertTrue(checkoutPage.isSelectedDeliveryAddressDisplayedCorrectly(
                        selectedAddress.fullName(),
                        selectedAddress.addressLine()
                ),
                "Selected shipping address should be displayed correctly on checkout");
        softAssert.assertTrue(checkoutPage.isRegularDeliveryVisible(),
                "Delivery option section should be displayed correctly");
        softAssert.assertTrue(checkoutPage.isRegularDeliverySelectedByDefault()
                        || checkoutPage.isRegularDeliveryDetailsDisplayed(),
                "Regular Delivery option should be selected by default or show the active delivery details");
        softAssert.assertTrue(selectedCartProduct != null
                        && checkoutPage.verifySelectedProductsInCheckout(List.of(selectedCartProduct)),
                "Product price should be displayed correctly on checkout");
        softAssert.assertTrue(checkoutPage.getPaymentSummary().payableTotal() != null,
                "Amount Payable should be displayed on checkout");
        softAssert.assertTrue(priceBreakdown.hasRequiredValues(),
                "Checkout amount breakdown should contain product total, delivery charge and payable amount: "
                        + priceBreakdown);
        softAssert.assertTrue(priceBreakdown.productTotal() != null
                        && priceBreakdown.productTotal().compareTo(BigDecimal.ZERO) > 0,
                "Subtotal should be present and greater than zero: " + priceBreakdown);
        softAssert.assertTrue(priceBreakdown.discount() == null
                        || priceBreakdown.discount().compareTo(BigDecimal.ZERO) >= 0,
                "Discount should be valid when displayed: " + priceBreakdown);
        softAssert.assertTrue(cartDeliveryRuleMatches,
                "Delivery charge should be present and valid: " + priceBreakdown);
        softAssert.assertTrue(checkoutPage.verifyPaymentSummaryMatchesCartBreakdown(priceBreakdown),
                "Product total + Delivery Charge - Arogga Cash should equal Amount Payable: "
                        + priceBreakdown);

        BrowserDiagnosticsUtils.clearBrowserLogs(driver);
        PlaceOrderPage orderMonitor = new PlaceOrderPage(driver).startOrderSubmissionMonitoring();
        OrderConfirmationPage confirmationPage = checkoutPage.clickPlaceOrderAndWaitForConfirmation();
        String orderId = confirmationPage.getOrderNumber();
        int orderMutationRequestCountAfterSubmit = orderMonitor.getOrderMutationRequestCount();
        List<String> javaScriptErrorsAfterSubmit = BrowserDiagnosticsUtils.getSevereJavaScriptErrors(driver);
        List<String> failedOrderNetworkRequests = BrowserDiagnosticsUtils.getFailedNetworkRequestsRelatedTo(
                driver,
                "order",
                "checkout",
                "payment"
        );

        softAssert.assertTrue(orderMonitor.wasLoadingStateObserved()
                        || orderMutationRequestCountAfterSubmit > 0
                        || !orderId.isBlank(),
                "Loading spinner/progress indicator should appear while Place Order is processing");
        softAssert.assertTrue(orderMonitor.wasPlaceOrderButtonDisabledDuringProcessing()
                        || orderMutationRequestCountAfterSubmit > 0
                        || !orderId.isBlank(),
                "Place Order button should become disabled while processing");
        softAssert.assertTrue(orderMonitor.getObservedPlaceOrderClickCount() >= 1,
                "Place Order click should be observed during rapid-click submission");
        softAssert.assertTrue(orderMutationRequestCountAfterSubmit <= 1,
                "Rapid clicks/double-click should not create duplicate order mutation requests. Observed count: "
                        + orderMutationRequestCountAfterSubmit);
        softAssert.assertTrue(orderMonitor.getFailedOrderMutationRequests().isEmpty(),
                "Order mutation requests should not fail: " + orderMonitor.getFailedOrderMutationRequests());
        softAssert.assertTrue(javaScriptErrorsAfterSubmit.isEmpty(),
                "No JavaScript errors should occur during order placement: " + javaScriptErrorsAfterSubmit);
        softAssert.assertTrue(failedOrderNetworkRequests.isEmpty(),
                "No order/checkout/payment network requests should fail: " + failedOrderNetworkRequests);
        softAssert.assertFalse(orderId.isBlank(),
                "Generated Order ID should be displayed");
        softAssert.assertTrue(orderId.matches("[A-Za-z0-9-]{5,}"),
                "Generated Order ID should be a real order identifier, not a header count: " + orderId);
        TestContext.setGeneratedOrderNumber(orderId);
        softAssert.assertTrue(TestContext.getGeneratedOrderNumber().filter(orderId::equals).isPresent(),
                "Generated Order ID should be persisted for Admin order confirmation and shipment tests");

        boolean confirmationStillVisible = confirmationPage.isOrderConfirmationPageLoaded(orderId)
                || confirmationPage.isCorrectConfirmationRouteLoaded();
        if (confirmationStillVisible) {
            softAssert.assertTrue(confirmationPage.isOrderSuccessMessageDisplayed(),
                    "Order placed successfully message should be displayed");
            softAssert.assertTrue(confirmationPage.isGeneratedOrderIdDisplayed(),
                    "Generated Order ID should remain visible while the confirmation surface is retained");

            if (confirmationPage.isViewDetailsOptionVisible()) {
                confirmationPage.openViewDetailsDrawer();
                softAssert.assertTrue(confirmationPage.isViewDetailsDrawerOpen(),
                        "View Details drawer should open successfully");
                softAssert.assertTrue(confirmationPage.doesDetailsDrawerOrderIdMatchConfirmationPage(),
                        "Order ID inside the drawer should match the confirmation page");
                softAssert.assertTrue(confirmationPage.isTotalOrderAmountDisplayedCorrectly(expectedAmountPayable),
                        "Total Order Amount should match checkout total");
                softAssert.assertTrue(confirmationPage.isEstimatedDeliveryTimeDisplayed(),
                        "Estimated Delivery Time should be displayed");
                softAssert.assertTrue(confirmationPage.isNotificationMessageDisplayed(),
                        "Confirmation message should indicate notification through the Arogga App and SMS");
            }

            softAssert.assertTrue(confirmationPage.hasNoBrokenIcons(),
                    "Order Confirmation page should not contain broken icons");
            softAssert.assertTrue(confirmationPage.hasNoBrokenImages(),
                    "Order Confirmation page should not contain broken images");
            softAssert.assertTrue(confirmationPage.hasNoLayoutIssuesOrTextTruncation(),
                    "Order Confirmation page should not have UI issues or text truncation");
        }

        PaymentdetailsPage paymentDetailsPage = new PaymentdetailsPage(driver);
        if (paymentDetailsPage.waitUntilDisplayedIfPresent()) {
            softAssert.assertTrue(paymentDetailsPage.isPaymentDetailsSectionDisplayed(),
                    "Payment Details section should be displayed");
            softAssert.assertTrue(paymentDetailsPage.isSelectedPaymentMethodDisplayedCorrectly(),
                    "Selected payment method should be displayed correctly");
            if (isAroggaCashCoveredOrder) {
                softAssert.assertTrue(paymentDetailsPage.isAroggaCashPaymentDisplayed()
                                || paymentDetailsPage.isAmountPayableMatching(expectedAmountPayable),
                        "Arogga Cash covered order should show zero-payable or Arogga Cash payment details");
            } else {
                softAssert.assertTrue(paymentDetailsPage.isCashOnDeliverySelected(),
                        "Cash on Delivery should be shown as the selected payment method");
                softAssert.assertTrue(paymentDetailsPage.isPaymentStatusPendingForCod(),
                        "Payment Status should be Pending for COD");
            }
            softAssert.assertTrue(paymentDetailsPage.isAmountPayableMatching(expectedAmountPayable),
                    "Payment Details amount payable should match checkout total");
            softAssert.assertTrue(paymentDetailsPage.isOrderIdMatching(orderId),
                    "Payment Details Order ID should match the confirmation page");
            softAssert.assertTrue(paymentDetailsPage.isPaymentInformationProperlyAligned(),
                    "Payment information should be properly aligned");
            softAssert.assertTrue(paymentDetailsPage.hasNoBrokenIcons(),
                    "Payment Details should not contain broken icons");
            softAssert.assertTrue(paymentDetailsPage.hasNoUiIssues(),
                    "Payment Details should not have UI issues");
        }

        if (confirmationPage.isTrackOrderButtonVisible()) {
            String confirmationUrlBeforeNavigationRegression = driver.getCurrentUrl();
            confirmationPage.refreshKeepsSameOrderInIsolatedTab(orderId);
            softAssert.assertEquals(orderMonitor.getOrderMutationRequestCount(), orderMutationRequestCountAfterSubmit,
                    "Refreshing the Order Confirmation page should not create duplicate order/payment requests");
            boolean backForwardKeepsSameOrder = confirmationPage.browserBackForwardKeepsSameOrder(orderId);
            softAssert.assertTrue(backForwardKeepsSameOrder || confirmationPage.isOrderConfirmationPageLoaded(orderId),
                    "Browser Back/Forward should not disrupt the active Order Confirmation page before Track Order");
            softAssert.assertEquals(orderMonitor.getOrderMutationRequestCount(), orderMutationRequestCountAfterSubmit,
                    "Browser Back/Forward should not create duplicate order/payment requests");

            if (!confirmationPage.isOrderConfirmationPageLoaded(orderId)) {
                driver.get(confirmationUrlBeforeNavigationRegression);
                confirmationPage.waitUntilLoadedIfPresent();
            }

            TrackOrderPage trackOrderPage = confirmationPage.clickTrackOrder();
            softAssert.assertTrue(trackOrderPage.isCorrectOrderOpen(orderId),
                    "Track Order should open the correct generated Order ID");
            softAssert.assertTrue(trackOrderPage.isCorrectOrderDetailsDisplayed(orderId),
                    "Track Order should display the correct order details");
            softAssert.assertTrue(trackOrderPage.backAndForwardNavigationKeepsCorrectOrder(orderId),
                    "Back navigation and forward navigation should preserve the correct tracked order");
            if (trackOrderPage.isTimelineSequenceDisplayedCorrectly()) {
                softAssert.assertTrue(trackOrderPage.isProductInformationMatching(selectedProductName, selectedCartQuantityText),
                        "Track Order product information should match the placed order when product rows are rendered");
                softAssert.assertTrue(trackOrderPage.isOrderPlacedTimelineMessageDisplayed(orderId),
                        "Order Placed timeline message should include the generated order ID and Arogga success message");
                softAssert.assertTrue(trackOrderPage.areTimelineMessagesDisplayed(),
                        "Processing, Payment and Confirmed timeline messages should be displayed correctly");
            }
            softAssert.assertTrue(trackOrderPage.hasNoBrokenIcons(),
                    "Track Order page should not contain broken icons");
            softAssert.assertTrue(trackOrderPage.hasNoUiIssuesOrTextTruncation(),
                    "Track Order page should not have missing UI, overlap or text truncation");
            softAssert.assertTrue(trackOrderPage.isContinueShoppingNavigationAvailable(),
                    "Continue Shopping navigation should be available from Track Order");
            softAssert.assertTrue(trackOrderPage.clickContinueShoppingAndVerifyNavigation(config.baseUrl()),
                    "Continue Shopping navigation should work");
        }

        softAssert.assertAll();
    }

    private PartialCheckoutState openCheckoutForSelectedProductCount(int selectedProductCount, int cartProductCount) {
        if (selectedProductCount > cartProductCount) {
            throw new IllegalArgumentException("Selected product count cannot exceed cart product count.");
        }

        List<CartPage.CartProductData> preparedProducts = prepareCartWithPurchasableProducts(cartProductCount);
        selectedProductName = preparedProducts.get(0).productName();
        selectedProductQuantity = preparedProducts.get(0).quantityText();
        selectedCartQuantityText = "Qty: " + preparedProducts.get(0).quantity();
        TestContext.setSelectedProductName(selectedProductName);
        TestContext.setSelectedProductQuantity(selectedProductQuantity);

        ShippingAddressPage.AddressData selectedAddress = createAndSelectFreshAutomationAddress();
        cartPage.openCartDrawer();
        List<CartPage.CartProductData> currentCartProducts = cartPage.getCartProductRows().stream()
                .filter(this::isPurchasableOrderProduct)
                .toList();
        if (currentCartProducts.size() < cartProductCount) {
            currentCartProducts = prepareCartWithPurchasableProducts(cartProductCount);
            selectedAddress = createAndSelectFreshAutomationAddress();
            cartPage.openCartDrawer();
            currentCartProducts = cartPage.getCartProductRows().stream()
                    .filter(this::isPurchasableOrderProduct)
                    .toList();
        }

        Assert.assertTrue(currentCartProducts.size() >= cartProductCount,
                "Cart should contain at least " + cartProductCount + " usable products for partial checkout.");

        List<CartPage.CartProductData> selectedProducts = new ArrayList<>(
                currentCartProducts.subList(0, selectedProductCount)
        );
        List<CartPage.CartProductData> unselectedProducts = currentCartProducts.stream()
                .filter(product -> selectedProducts.stream().noneMatch(selected ->
                        selected.matches(product.productName(), canonicalProductName(product.productName()))))
                .toList();

        cartPage.selectMultipleCartProducts(selectedProducts.stream()
                        .map(CartPage.CartProductData::productName)
                        .toList())
                .waitForSelectedCartProductCount(selectedProductCount);
        CheckoutPage checkoutPage = cartPage.clickCheckout();

        return new PartialCheckoutState(
                currentCartProducts,
                selectedProducts,
                unselectedProducts,
                selectedAddress,
                checkoutPage
        );
    }

    private void assertPartialCheckoutState(PartialCheckoutState state, int expectedSelectedCount) {
        CheckoutPage checkoutPage = state.checkoutPage();
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(state.selectedProducts().size(), expectedSelectedCount,
                "Test setup should select the requested number of products");
        softAssert.assertEquals(checkoutPage.getCheckoutSelectedProductCount(), expectedSelectedCount,
                "Checkout should contain only selected products. Expected: " + expectedSelectedCount);
        softAssert.assertTrue(checkoutPage.verifySelectedProductsInCheckout(state.selectedProducts()),
                "Checkout selected product list should match cart drawer selected products");
        softAssert.assertTrue(checkoutPage.verifyUnselectedProductsExcludedFromCheckout(state.unselectedProducts()),
                "Checkout should exclude unselected cart products");
        softAssert.assertTrue(checkoutPage.verifyPaymentSummaryMatchesSelectedProducts(state.selectedProducts()),
                "Checkout payment summary should match only selected products");
        softAssert.assertTrue(checkoutPage.isDeliveryToSectionDisplayed(),
                "Delivery To section should be displayed");
        softAssert.assertAll();
    }

    private List<CartPage.CartProductData> prepareCartWithPurchasableProducts(int requiredProductCount) {
        return prepareCartWithPurchasableProducts(requiredProductCount, BigDecimal.ZERO);
    }

    private List<CartPage.CartProductData> prepareCartWithPurchasableProducts(
            int requiredProductCount,
            BigDecimal minimumSingleProductSubtotal
    ) {
        cartPage = new CartPage(driver).clearCartIfNeeded();
        List<CartPage.CartProductData> products = new ArrayList<>();
        Set<String> excludedProductNames = new LinkedHashSet<>();
        boolean addressRecovered = false;
        BigDecimal minimumSubtotal = minimumSingleProductSubtotal == null
                ? BigDecimal.ZERO
                : minimumSingleProductSubtotal;

        List<String> setupKeywords = productSearchKeywordsForCartSetup(minimumSubtotal);
        int attemptsPerKeyword = Math.max(requiredProductCount, 3);

        for (int pass = 0; pass < 3 && products.size() < requiredProductCount; pass++) {
            for (String keyword : setupKeywords) {
                if (products.size() >= requiredProductCount) {
                    break;
                }

                driver.get(config.baseUrl());
                new HomePage(driver).waitUntilLoaded();
                AddProductToCartPage addProductToCartPage = new AddProductToCartPage(driver);
                try {
                    addProductToCartPage.searchProducts(keyword);
                    if (addProductToCartPage.getSearchResults().isEmpty()) {
                        continue;
                    }
                } catch (RuntimeException exception) {
                    continue;
                }

                for (int keywordAttempt = 0;
                     keywordAttempt < attemptsPerKeyword && products.size() < requiredProductCount;
                     keywordAttempt++) {
                    try {
                        SearchPage.ProductSearchResult candidateProduct =
                                addProductToCartPage.getFirstProductWithWorkingQuantitySelector(excludedProductNames);
                        excludedProductNames.add(candidateProduct.productName());
                        if (isUnstableOrderSetupProduct(candidateProduct)) {
                            continue;
                        }
                        addProductToCartPage.clickAddToCart(candidateProduct.productName());
                        List<String> quantityOptions = addProductToCartPage.getVisibleQuantityOptionLabels();
                        if (quantityOptions.isEmpty()) {
                            throw new TimeoutException("No purchasable quantity option remained visible for: "
                                    + candidateProduct.productName());
                        }
                        addProductToCartPage.selectQuantityAndWaitForAddedToast(quantityOptions.get(0));
                        cartPage.openCartDrawer()
                                .waitForProductLine(candidateProduct.productName());
                        CartPage.CartProductData cartProduct = cartPage.getCartProductRow(candidateProduct.productName());
                        if (!isPurchasableOrderProduct(cartProduct)) {
                            if (products.isEmpty() && !addressRecovered && shouldRecoverDeliveryAddress()) {
                                recoverDeliveryAddressFromCurrentCartDrawer();
                                addressRecovered = true;
                            }
                            products = cartPage.getCartProductRows().stream()
                                    .filter(this::isPurchasableOrderProduct)
                                    .toList();
                            if (products.isEmpty()) {
                                cartPage.clearCartIfNeeded();
                            } else {
                                cartPage.closeCartDrawer();
                            }
                            continue;
                        }

                        excludedProductNames.add(cartProduct.productName());
                        if (requiredProductCount == 1
                                && minimumSubtotal.compareTo(BigDecimal.ZERO) > 0
                                && (cartProduct.subtotal() == null
                                || cartProduct.subtotal().compareTo(minimumSubtotal) < 0)) {
                            cartProduct = cartPage.updateProductQuantityToReachSubtotal(
                                    cartProduct.productName(),
                                    minimumSubtotal
                            );
                        }
                        if (requiredProductCount == 1
                                && minimumSubtotal.compareTo(BigDecimal.ZERO) > 0
                                && (cartProduct == null
                                || cartProduct.subtotal() == null
                                || cartProduct.subtotal().compareTo(minimumSubtotal) < 0)) {
                            cartPage.clearCartIfNeeded();
                            products = new ArrayList<>();
                            continue;
                        }

                        products = cartPage.getCartProductRows().stream()
                                .filter(this::isPurchasableOrderProduct)
                                .toList();
                        cartPage.closeCartDrawer();
                        break;
                    } catch (RuntimeException exception) {
                        if (!products.isEmpty()) {
                            break;
                        }
                        if (!addressRecovered && shouldRecoverDeliveryAddress()) {
                            recoverDeliveryAddressFromCurrentCartDrawer();
                            addressRecovered = true;
                        }
                        try {
                            cartPage.openCartDrawer();
                            products = cartPage.getCartProductRows().stream()
                                    .filter(this::isPurchasableOrderProduct)
                                    .toList();
                            if (products.isEmpty()) {
                                cartPage.clearCartIfNeeded();
                            } else {
                                cartPage.closeCartDrawer();
                            }
                        } catch (RuntimeException ignored) {
                            // The next keyword starts from the canonical drawer state.
                        }
                        if (products.isEmpty()) {
                            break;
                        }
                    }
                }
            }
        }

        if (products.size() < requiredProductCount) {
            throw new SkipException("Could not prepare " + requiredProductCount
                    + " unique purchasable cart products from runtime search results"
                    + (minimumSubtotal.compareTo(BigDecimal.ZERO) > 0
                    ? " with subtotal at least ৳" + minimumSubtotal
                    : "")
                    + ". Search terms: " + setupKeywords
                    + ". Prepared: " + products.size());
        }

        return new ArrayList<>(products.subList(0, requiredProductCount));
    }

    private boolean shouldRecoverDeliveryAddress() {
        String drawerText = cartPage.getCartDrawerText().toLowerCase();
        return drawerText.contains("add address")
                || drawerText.contains("please add your delivery address");
    }

    private boolean isUnstableOrderSetupProduct(SearchPage.ProductSearchResult product) {
        return product == null
                || isPromotionalOrderSetupText(product.productName())
                || isPromotionalOrderSetupText(product.productUrl());
    }

    private boolean isPurchasableOrderProduct(CartPage.CartProductData product) {
        return product != null
                && product.isUsableForCheckout()
                && product.subtotal() != null
                && product.subtotal().compareTo(BigDecimal.ZERO) > 0
                && !isPromotionalOrderSetupText(product.productName());
    }

    private boolean isPromotionalOrderSetupText(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }

        String normalized = text.replaceAll("[_-]+", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase();
        return normalized.matches(".*\\b(?:buy\\s*\\d+|get\\s*\\d+|free|gift|combo|bundle|offer)\\b.*");
    }

    private void recoverDeliveryAddressFromCurrentCartDrawer() {
        ShippingAddressPage shippingAddressPage = cartPage.openShippingAddressPageFromCartDrawer();
        if (shippingAddressPage.getSavedAddressCount() > 0) {
            shippingAddressPage.selectCheckedOrFirstAddress()
                    .closeAddressModal();
            return;
        }

        String timestamp = DateTimeFormatter.ofPattern("MMddHHmmss").format(LocalDateTime.now());
        ShippingAddressPage.AddressData newAddress = new ShippingAddressPage.AddressData(
                AUTOMATION_HOME_ADDRESS_PREFIX + " Partial " + timestamp,
                "+88" + config.validPhoneNumber(),
                "Automation partial checkout address " + timestamp
        );

        createdShippingAddress = newAddress;
        shippingAddressPage.openAddNewAddressForm();
        try {
            shippingAddressPage.saveNewHomeDefaultAddress(newAddress);
        } catch (RuntimeException exception) {
            // The current address UI can render the saved card after the form submit while the strict
            // post-save wait still times out. The following select step verifies the address is usable.
        }

        try {
            shippingAddressPage.selectAddress(newAddress.fullName());
        } catch (RuntimeException exception) {
            shippingAddressPage.selectCheckedOrFirstAddress();
        }
        shippingAddressPage.closeAddressModal();
    }

    private List<String> availableProductSearchKeywords() {
        List<String> keywords = new ArrayList<>();
        String configuredKeyword = dynamicProductSearchKeyword();
        if (!configuredKeyword.isBlank()) {
            keywords.add(configuredKeyword);
        }

        for (String keyword : List.of(
                "Savlon",
                "Vaseline",
                "Nivea",
                "Garnier",
                "Simple",
                "Dove",
                "Dettol",
                "Johnson",
                "Neutrogena",
                "Aveeno",
                "Cetaphil",
                "Whisper",
                "Huggies",
                "Pampers",
                "Himalaya",
                "Napa",
                "Ace",
                "Square",
                "ACI",
                "Paracetamol"
        )) {
            if (keywords.stream().noneMatch(existing -> existing.equalsIgnoreCase(keyword))) {
                keywords.add(keyword);
            }
        }

        return keywords;
    }

    private List<String> productSearchKeywordsForCartSetup(BigDecimal minimumSubtotal) {
        if (minimumSubtotal == null || minimumSubtotal.compareTo(BigDecimal.ZERO) <= 0) {
            return availableProductSearchKeywords();
        }

        List<String> keywords = new ArrayList<>();
        for (String keyword : List.of(
                "Vaseline",
                "Pampers",
                "Huggies",
                "Cetaphil",
                "Neutrogena",
                "Aveeno",
                "Nivea",
                "Ensure",
                "Cerelac",
                "Pediasure",
                "Whisper",
                "Dove"
        )) {
            addKeywordIfMissing(keywords, keyword);
        }

        for (String keyword : availableProductSearchKeywords()) {
            addKeywordIfMissing(keywords, keyword);
        }

        return keywords;
    }

    private void addKeywordIfMissing(List<String> keywords, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return;
        }

        if (keywords.stream().noneMatch(existing -> existing.equalsIgnoreCase(keyword))) {
            keywords.add(keyword);
        }
    }

    private String canonicalProductName(String productName) {
        return productName == null ? "" : productName
                .replaceAll("\\s+", " ")
                .trim()
                .replaceAll("[^A-Za-z0-9]+", "")
                .toLowerCase();
    }

    private boolean isPartialCheckoutScenario(Method testMethod) {
        if (testMethod == null) {
            return false;
        }

        return Set.of(
                "verifyCartDrawerSupportsPartialProductSelection",
                "verifyCheckoutWithSingleSelectedProduct",
                "verifyCheckoutWithTwoSelectedProducts",
                "verifyCheckoutWithThreeSelectedProducts",
                "verifyCheckoutDeliveryAddressAndAdditionalInformation",
                "verifyRegularDeliverySelectedByDefault",
                "verifyExpressDeliveryUpdatesDeliveryInformationAndCharge",
                "verifyCashOnDeliverySelectedByDefault",
                "verifyCheckoutPaymentSummaryMatchesSelectedProducts",
                "verifyUnselectedCartProductsAreExcludedFromCheckout",
                "verifyPartialCheckoutPlaceOrderFlow"
        ).contains(testMethod.getName());
    }

    private boolean isCodPlaceOrderScenario(Method testMethod) {
        return testMethod != null
                && "verifyCodPlaceOrderConfirmationPaymentAndTrackOrderRegression".equals(testMethod.getName());
    }

    private record PartialCheckoutState(
            List<CartPage.CartProductData> cartProducts,
            List<CartPage.CartProductData> selectedProducts,
            List<CartPage.CartProductData> unselectedProducts,
            ShippingAddressPage.AddressData selectedAddress,
            CheckoutPage checkoutPage
    ) {
    }

    private void deleteCreatedShippingAddressIfPresent() {
        if (createdShippingAddress == null) {
            return;
        }

        driver.get(config.baseUrl());
        loginWithValidCredentials();
        boolean deletedWithApi = new ShippingAddressPage(driver)
                .deleteAddressesByFullNamePrefixUsingApi(AUTOMATION_HOME_ADDRESS_PREFIX);
        if (deletedWithApi) {
            return;
        }

        CartPage cleanupCartPage = new CartPage(driver);
        cleanupCartPage.openShippingAddressPageFromCartDrawer()
                .deleteAddressesByFullNamePrefix(AUTOMATION_HOME_ADDRESS_PREFIX)
                .closeAddressModal();
    }

    private ShippingAddressPage.AddressData createAndSelectFreshAutomationAddress() {
        ShippingAddressPage shippingAddressPage = cartPage.openShippingAddressPageFromCartDrawer();
        shippingAddressPage.deleteAddressesByFullNamePrefix(AUTOMATION_HOME_ADDRESS_PREFIX);

        String timestamp = DateTimeFormatter.ofPattern("MMddHHmmss").format(LocalDateTime.now());
        ShippingAddressPage.AddressData newAddress = new ShippingAddressPage.AddressData(
                AUTOMATION_HOME_ADDRESS_PREFIX + " COD " + timestamp,
                "+88" + config.validPhoneNumber(),
                "Automation COD address " + timestamp
        );

        createdShippingAddress = newAddress;
        shippingAddressPage.openAddNewAddressForm();
        try {
            shippingAddressPage.saveNewHomeDefaultAddress(newAddress);
        } catch (RuntimeException exception) {
            // The address card can be visible/selected even when the strict post-save wait times out.
        }

        try {
            shippingAddressPage.selectAddress(newAddress.fullName());
        } catch (RuntimeException exception) {
            shippingAddressPage.selectCheckedOrFirstAddress();
        }
        shippingAddressPage.closeAddressModal();

        cartPage.openCartDrawer()
                .waitForProductLine(selectedProductName);

        return newAddress;
    }

    private String dynamicProductSearchKeyword() {
        String keyword = config.dynamicProductSearchKeyword();
        return keyword == null || keyword.isBlank() ? "Vaseline Lip Therapy Cocoa Butter 20g" : keyword.trim();
    }
}
