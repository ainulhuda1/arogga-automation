package tests.admin;

import base.BaseTest;
import constants.TestGroups;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;
import pages.admin.AdminLoginPage;
import pages.admin.DashboardPage;
import pages.admin.OrderDetailsPage;
import pages.admin.OrdersPage;
import utils.TestContext;

public class AdminOrderConfirmationTest extends BaseTest {

    private static final Logger LOGGER = LogManager.getLogger(AdminOrderConfirmationTest.class);
    private static final String ADMIN_GROUP = "admin";
    private static final String ADMIN_ORDER_CONFIRMATION_GROUP = "admin-order-confirmation";
    @Test(groups = {ADMIN_GROUP, ADMIN_ORDER_CONFIRMATION_GROUP, TestGroups.LOGIN, TestGroups.ORDER, TestGroups.REGRESSION},
            description = "Verify Admin can login, search generated order and confirm it")
    public void verifyAdminCanLoginSearchAndConfirmGeneratedOrder() {
        String orderId = requiredGeneratedOrderId();
        String adminBaseUrl = optionalValue("adminBaseUrl", "ADMIN_BASE_URL", config.adminBaseUrl())
                .orElseThrow();
        String adminApiUrl = optionalValue("adminApiUrl", "ADMIN_API_URL", config.adminApiUrl())
                .orElseThrow();
        String adminOtp = optionalValue("adminOtp", "ADMIN_OTP", config.adminOtp())
                .orElseThrow();
        String adminMobileNumber = optionalValue("adminMobileNumber", "ADMIN_MOBILE_NUMBER", config.adminMobileNumber())
                .orElseThrow();

        LOGGER.info("Opening Admin login page for generated order {}", orderId);
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

        dashboardPage.refreshAndWaitUntilLoaded();
        Assert.assertTrue(dashboardPage.isDashboardLoaded(), "Dashboard should remain loaded after refresh");

        LOGGER.info("Searching generated order {} in Admin Orders page", orderId);
        OrdersPage ordersPage = dashboardPage.openOrdersPage();
        Assert.assertTrue(ordersPage.isOrdersPageLoaded(), "Orders page should load successfully");

        ordersPage.searchOrder(orderId);
        Assert.assertTrue(ordersPage.isOrderDisplayed(orderId),
                "Search result should display the generated Order ID: " + orderId);
        boolean orderAlreadyConfirmed = ordersPage.isOrderStatusDisplayed(orderId, "Confirmed");

        OrderDetailsPage orderDetailsPage = ordersPage.openOrderDetails(orderId);
        Assert.assertTrue(orderDetailsPage.isOrderDetailsPageLoaded(orderId),
                "Order details page should open for generated Order ID: " + orderId);

        orderDetailsPage.scrollToLocationSection();
        Assert.assertTrue(orderDetailsPage.isCustomerLocationDisplayed(),
                "Customer location should be displayed in Order Details");

        if (!orderAlreadyConfirmed) {
            orderDetailsPage.waitForLocationActionState();
            if (!orderDetailsPage.isLocationConfirmed()) {
                Assert.assertTrue(orderDetailsPage.isConfirmLocationButtonVisible(),
                        "Confirm Location button should be visible");
                orderDetailsPage.confirmLocation();
            }
        } else {
            LOGGER.info("Location is already confirmed for generated order {}", orderId);
        }
        Assert.assertTrue(orderAlreadyConfirmed || orderDetailsPage.isLocationConfirmed(),
                "Location should be confirmed successfully");

        if (!orderAlreadyConfirmed) {
            orderDetailsPage.waitForOrderActionState();
            Assert.assertTrue(orderDetailsPage.isConfirmOrderButtonVisible(), "Confirm Order button should be visible");
            orderDetailsPage.confirmOrder();
        } else {
            LOGGER.info("Order status is already confirmed for generated order {}", orderId);
        }
        Assert.assertTrue(orderAlreadyConfirmed || orderDetailsPage.isOrderStatusConfirmed(),
                "Order status should become Confirmed");
    }

    private String requiredGeneratedOrderId() {
        return optionalValue("adminOrderId", "ADMIN_ORDER_ID", null)
                .or(() -> TestContext.getGeneratedOrderNumber())
                .filter(orderId -> !orderId.isBlank())
                .orElseThrow(() -> new SkipException(
                        "Generated Order ID is required. Run the user checkout flow first or provide adminOrderId/ADMIN_ORDER_ID."
                ));
    }

    private java.util.Optional<String> optionalValue(String propertyName, String environmentName, String defaultValue) {
        String propertyValue = System.getProperty(propertyName);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return java.util.Optional.of(propertyValue.trim());
        }

        String environmentValue = System.getenv(environmentName);
        if (environmentValue != null && !environmentValue.isBlank()) {
            return java.util.Optional.of(environmentValue.trim());
        }

        return defaultValue == null || defaultValue.isBlank()
                ? java.util.Optional.empty()
                : java.util.Optional.of(defaultValue.trim());
    }
}
