package tests.admin;

import base.BaseTest;
import constants.TestGroups;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;
import pages.admin.AdminLoginPage;
import pages.admin.DashboardPage;
import pages.admin.OrderDetailsPage;
import pages.admin.OrdersPage;
import pages.admin.ShipmentCreationPage;
import pages.admin.ShipmentDetailsPage;
import pages.admin.ShipmentTrackingPage;
import pages.admin.ShipmentVerificationPage;
import utils.TestContext;

import java.util.List;
import java.util.Optional;

public class ShipmentProcessingTest extends BaseTest {

    private static final Logger LOGGER = LogManager.getLogger(ShipmentProcessingTest.class);
    private static final String ADMIN_GROUP = "admin";
    private static final String ADMIN_ORDER_CONFIRMATION_GROUP = "admin-order-confirmation";
    private static final String CREATE_SHIPMENT_CRON = "createShipment";
    private static final String CALL_CONFIRMATION_MESSAGE = "Are you sure you want to call this?";
    private static final String SHIPMENT_CRON_SUCCESS_MESSAGE = "Shipment cron executed successfully.";

    @Test(groups = {ADMIN_GROUP, TestGroups.ORDER, TestGroups.REGRESSION},
            dependsOnGroups = ADMIN_ORDER_CONFIRMATION_GROUP,
            description = "Verify Admin can create, verify, inspect and track shipment for confirmed generated order")
    public void verifyShipmentProcessingForConfirmedGeneratedOrder() {
        String orderId = requiredGeneratedOrderId();
        SoftAssert softAssert = new SoftAssert();

        LOGGER.info("Opening Admin dashboard to process shipment for confirmed order {}", orderId);
        DashboardPage dashboardPage = loginToAdminDashboard();

        LOGGER.info("Executing {} cron from SA Settings", CREATE_SHIPMENT_CRON);
        ShipmentCreationPage shipmentCreationCronPage = dashboardPage.openSASettingsPage();
        softAssert.assertTrue(shipmentCreationCronPage.isSASettingsPageLoaded(), "SA Settings should be loaded");

        shipmentCreationCronPage.openCallSection();
        softAssert.assertTrue(shipmentCreationCronPage.isCallSearchFieldDisplayed(), "CALL search field should be displayed");

        shipmentCreationCronPage.searchCall(CREATE_SHIPMENT_CRON);
        softAssert.assertTrue(shipmentCreationCronPage.isCreateShipmentSearchResultDisplayed(),
                "createShipment should be found in CALL search result"
        );

        shipmentCreationCronPage.selectCreateShipment();
        softAssert.assertTrue(shipmentCreationCronPage.isCallButtonEnabled(), "CALL button should be enabled");

        shipmentCreationCronPage.clickCall();
        softAssert.assertTrue(shipmentCreationCronPage.isConfirmationDialogDisplayed(),
                "CALL confirmation dialog should be displayed");
        softAssert.assertTrue(shipmentCreationCronPage.getConfirmationMessage().contains(CALL_CONFIRMATION_MESSAGE),
                "CALL confirmation message should match expected text");

        shipmentCreationCronPage.confirmCall();
        softAssert.assertTrue(shipmentCreationCronPage.isShipmentCronExecutedSuccessfully(),
                "Shipment cron should execute successfully");
        softAssert.assertTrue(shipmentCreationCronPage.isSuccessMessageDisplayed(SHIPMENT_CRON_SUCCESS_MESSAGE),
                "Success message should be displayed: " + SHIPMENT_CRON_SUCCESS_MESSAGE);

        LOGGER.info("Searching confirmed order {} and loading shipment information", orderId);
        OrdersPage ordersPage = new DashboardPage(driver).openOrdersPage();
        softAssert.assertTrue(ordersPage.isOrdersPageLoaded(), "Orders page should be loaded");

        ordersPage.searchOrder(orderId);
        softAssert.assertTrue(ordersPage.isOrderDisplayed(orderId),
                "Order search should display the generated Order ID: " + orderId);

        OrderDetailsPage orderDetailsPage = ordersPage.openOrderDetails(orderId);
        softAssert.assertTrue(orderDetailsPage.isOrderDetailsPageLoaded(orderId),
                "Order Details should be loaded for Order ID: " + orderId);

        orderDetailsPage.waitUntilOrderedProductsDisplayed();
        List<String> orderedProductRows = orderDetailsPage.getOrderedProductRowTexts();
        String orderShippingAddress = orderDetailsPage.getShippingAddressText();
        softAssert.assertFalse(orderedProductRows.isEmpty(), "Ordered products should be displayed in Order Details");

        ShipmentVerificationPage shipmentVerificationPage = orderDetailsPage.openShipmentVerificationSection(orderId);
        softAssert.assertTrue(shipmentVerificationPage.isLoadShipmentButtonDisplayed(),
                "Load Shipment button should be displayed");

        shipmentVerificationPage.clickLoadShipment();
        shipmentVerificationPage.waitUntilShipmentInformationLoads(orderId);

        softAssert.assertTrue(shipmentVerificationPage.isShipmentSuccessfullyCreated(orderId),
                "Shipment should be created for generated order");
        String shipmentId = shipmentVerificationPage.getShipmentId(orderId);
        softAssert.assertFalse(shipmentId.isBlank(), "Shipment ID should be generated");

        Assert.assertFalse(shipmentId.isBlank(), "Shipment ID is required to continue shipment details verification");
        TestContext.setGeneratedShipmentId(shipmentId);
        softAssert.assertTrue(TestContext.getGeneratedShipmentId().filter(shipmentId::equals).isPresent(),
                "Shipment ID should be captured in TestContext");
        softAssert.assertFalse(shipmentVerificationPage.getShipmentStatus(orderId).isBlank(),
                "Shipment Status should be displayed");
        softAssert.assertTrue(shipmentVerificationPage.doesShipmentBelongToOrder(orderId),
                "Shipment should belong to generated Order ID: " + orderId);

        LOGGER.info("Opening shipment details for shipment {}", shipmentId);
        ShipmentDetailsPage shipmentDetailsPage = shipmentVerificationPage.openShipmentDetailsPage(shipmentId);
        softAssert.assertTrue(shipmentDetailsPage.isShipmentDetailsPageLoaded(shipmentId),
                "Shipment Details should be loaded for Shipment ID: " + shipmentId);
        softAssert.assertTrue(shipmentDetailsPage.isShipmentIdMatching(shipmentId),
                "Shipment Details ID should match captured Shipment ID");
        softAssert.assertTrue(shipmentDetailsPage.isOrderIdMatching(orderId),
                "Shipment Details Order ID should match generated Order ID");
        softAssert.assertFalse(shipmentDetailsPage.getShipmentStatus().isBlank(),
                "Shipment Status should be displayed in Shipment Details");
        softAssert.assertTrue(shipmentDetailsPage.isShipmentCreatedDateDisplayed(),
                "Shipment Created Date should be displayed");
        softAssert.assertTrue(shipmentDetailsPage.isShipmentCreatedTimeDisplayed(),
                "Shipment Created Time should be displayed");
        softAssert.assertTrue(shipmentDetailsPage.isShippingAddressDisplayed(),
                "Shipping Address should be displayed");
        softAssert.assertTrue(shipmentDetailsPage.isShippingAddressMatching(orderShippingAddress),
                "Shipping Address should match the selected order");
        softAssert.assertTrue(shipmentDetailsPage.isCustomerInformationDisplayed(),
                "Customer Information should be displayed");
        softAssert.assertTrue(shipmentDetailsPage.areOrderedProductsDisplayed(),
                "Ordered products should be displayed in Shipment Details");
        softAssert.assertTrue(shipmentDetailsPage.doOrderedProductQuantitiesMatch(orderedProductRows),
                "Shipment item quantities should match the customer order");

        LOGGER.info("Verifying shipment tracking timeline for shipment {}", shipmentId);
        String shipmentStatus = shipmentDetailsPage.getShipmentStatus();
        ShipmentTrackingPage shipmentTrackingPage = shipmentDetailsPage.openShipmentTrackingSection();
        softAssert.assertTrue(shipmentTrackingPage.isTrackingSectionDisplayed(),
                "Tracking section should be displayed");
        softAssert.assertTrue(shipmentTrackingPage.isTrackingTimelineDisplayed(),
                "Tracking timeline should be displayed");
        softAssert.assertTrue(shipmentTrackingPage.isCurrentShipmentStatusHighlighted(shipmentStatus),
                "Current shipment status should be highlighted in tracking timeline");
        softAssert.assertTrue(shipmentTrackingPage.areFutureStatusesPending(shipmentStatus),
                "Future shipment statuses should remain Pending");
        softAssert.assertTrue(shipmentTrackingPage.doesTrackingBelongToShipment(shipmentId),
                "Tracking data should belong to captured Shipment ID");
        softAssert.assertTrue(shipmentTrackingPage.isTimelineDataMatchingShipment(shipmentId, shipmentStatus),
                "Timeline data should match the shipment details");

        softAssert.assertAll();
    }

    private DashboardPage loginToAdminDashboard() {
        String adminBaseUrl = optionalValue("adminBaseUrl", "ADMIN_BASE_URL", config.adminBaseUrl())
                .orElseThrow();
        String adminApiUrl = optionalValue("adminApiUrl", "ADMIN_API_URL", config.adminApiUrl())
                .orElseThrow();
        String adminOtp = optionalValue("adminOtp", "ADMIN_OTP", config.adminOtp())
                .orElseThrow();
        String adminMobileNumber = optionalValue("adminMobileNumber", "ADMIN_MOBILE_NUMBER", config.adminMobileNumber())
                .orElseThrow();

        driver.get(adminBaseUrl);

        AdminLoginPage adminLoginPage = new AdminLoginPage(driver).waitUntilLoaded();
        Assert.assertTrue(adminLoginPage.isAdminLoginPageLoaded(), "Admin login page should load successfully");
        Assert.assertTrue(adminLoginPage.isApiUrlFieldDisplayed(), "Admin API URL field should be displayed");

        adminLoginPage.enterApiUrl(adminApiUrl);
        Assert.assertEquals(adminLoginPage.getApiUrl(), adminApiUrl, "Admin API URL field should accept the API URL");
        Assert.assertTrue(adminLoginPage.isMobileNumberFieldDisplayed(), "Admin mobile number field should be displayed");

        adminLoginPage.enterMobileNumber(adminMobileNumber)
                .clickNextAndWaitForOtp();
        Assert.assertTrue(adminLoginPage.isOtpInputDisplayed(), "Admin OTP field should be displayed after Next");

        DashboardPage dashboardPage = adminLoginPage.enterOtp(adminOtp)
                .clickLoginAfterOtp();
        Assert.assertTrue(dashboardPage.isDashboardLoaded(), "Admin login should open the Dashboard");

        return dashboardPage.refreshAndWaitUntilLoaded();
    }

    private String requiredGeneratedOrderId() {
        return optionalValue("adminOrderId", "ADMIN_ORDER_ID", null)
                .or(() -> TestContext.getGeneratedOrderNumber())
                .filter(orderId -> !orderId.isBlank())
                .orElseThrow(() -> new SkipException(
                        "Generated Order ID is required. Run the user checkout flow first or provide adminOrderId/ADMIN_ORDER_ID."
                ));
    }

    private Optional<String> optionalValue(String propertyName, String environmentName, String defaultValue) {
        String propertyValue = System.getProperty(propertyName);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return Optional.of(propertyValue.trim());
        }

        String environmentValue = System.getenv(environmentName);
        if (environmentValue != null && !environmentValue.isBlank()) {
            return Optional.of(environmentValue.trim());
        }

        return defaultValue == null || defaultValue.isBlank()
                ? Optional.empty()
                : Optional.of(defaultValue.trim());
    }
}
