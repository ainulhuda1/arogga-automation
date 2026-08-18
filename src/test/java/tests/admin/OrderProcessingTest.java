package tests.admin;

import base.BaseTest;
import constants.TestGroups;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.admin.AdminLoginPage;
import pages.admin.DashboardPage;
import pages.admin.OrderConfirmationPage;
import pages.admin.OrderSearchPage;
import utils.TestContext;

public class OrderProcessingTest extends BaseTest {

    private static final String ADMIN_GROUP = "admin";

    private DashboardPage dashboardPage;

    @BeforeMethod(alwaysRun = true)
    public void loginToAdminApplication() {
        driver.get(requiredValue("adminBaseUrl", "ADMIN_BASE_URL"));
        dashboardPage = new AdminLoginPage(driver)
                .waitUntilLoaded()
                .login(
                        requiredValue("adminUsername", "ADMIN_USERNAME"),
                        requiredValue("adminPassword", "ADMIN_PASSWORD")
                );
    }

    @Test(groups = {ADMIN_GROUP, TestGroups.ORDER, TestGroups.REGRESSION},
            description = "Verify Admin can search an order")
    public void verifyAdminCanSearchOrder() {
        String orderId = requiredValue("adminOrderId", "ADMIN_ORDER_ID");
        OrderSearchPage orderSearchPage = dashboardPage
                .openOrderSearchPage()
                .searchOrder(orderId);

        Assert.assertTrue(orderSearchPage.isOrderListed(orderId),
                "Searched admin order should be displayed in the result list");
    }

    @Test(groups = {ADMIN_GROUP, TestGroups.ORDER, TestGroups.REGRESSION},
            description = "Verify Admin can open order confirmation details")
    public void verifyAdminCanOpenOrderConfirmationDetails() {
        String orderId = requiredValue("adminOrderId", "ADMIN_ORDER_ID");
        OrderConfirmationPage orderConfirmationPage = dashboardPage
                .openOrderSearchPage()
                .searchOrder(orderId)
                .openOrderConfirmationPage(orderId);

        Assert.assertTrue(orderConfirmationPage.isOrderConfirmationPageLoaded(),
                "Order confirmation page should load for the selected order");
        Assert.assertTrue(!orderConfirmationPage.getOrderStatus().isBlank()
                        || orderConfirmationPage.isConfirmOrderButtonVisible(),
                "Order confirmation page should show order status or confirmation action");
    }

    @Test(groups = {ADMIN_GROUP, TestGroups.ORDER, TestGroups.REGRESSION},
            description = "Verify Admin order confirmation action is available")
    public void verifyAdminOrderConfirmationActionIsAvailable() {
        String orderId = requiredValue("adminOrderId", "ADMIN_ORDER_ID");
        OrderConfirmationPage orderConfirmationPage = dashboardPage
                .openOrderSearchPage()
                .searchOrder(orderId)
                .openOrderConfirmationPage(orderId);

        Assert.assertTrue(orderConfirmationPage.isConfirmOrderButtonVisible()
                        || orderConfirmationPage.isCreateShipmentButtonVisible(),
                "Order should expose confirmation or shipment processing action");
    }

    private String requiredValue(String propertyName, String environmentName) {
        String value = optionalValue(propertyName, environmentName);

        if (value == null || value.isBlank()) {
            throw new SkipException(propertyName + " or " + environmentName + " is required for admin tests.");
        }

        return value;
    }

    private String optionalValue(String propertyName, String environmentName) {
        String propertyValue = System.getProperty(propertyName);

        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue;
        }

        String environmentValue = System.getenv(environmentName);
        if (environmentValue != null && !environmentValue.isBlank()) {
            return environmentValue;
        }

        if (isAdminOrderId(propertyName, environmentName)) {
            return TestContext.getGeneratedOrderNumber().orElse(null);
        }

        return null;
    }

    private boolean isAdminOrderId(String propertyName, String environmentName) {
        return "adminOrderId".equals(propertyName) || "ADMIN_ORDER_ID".equals(environmentName);
    }
}
