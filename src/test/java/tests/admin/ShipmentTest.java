package tests.admin;

import base.BaseTest;
import constants.TestGroups;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.admin.AdminLoginPage;
import pages.admin.DashboardPage;
import pages.admin.ShipmentCreationPage;
import pages.admin.ShipmentDetailsPage;
import pages.admin.ShipmentTrackingPage;
import utils.TestContext;

public class ShipmentTest extends BaseTest {

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
            description = "Verify Admin can open shipment creation page from an order")
    public void verifyAdminCanOpenShipmentCreationPageFromOrder() {
        String orderId = requiredValue("adminOrderId", "ADMIN_ORDER_ID");
        ShipmentCreationPage shipmentCreationPage = dashboardPage
                .openOrderSearchPage()
                .searchOrder(orderId)
                .openOrderConfirmationPage(orderId)
                .openShipmentCreationPage();

        Assert.assertTrue(shipmentCreationPage.isShipmentCreationPageLoaded(),
                "Shipment creation page should load from order confirmation");
        Assert.assertTrue(shipmentCreationPage.isCreateShipmentButtonVisible(),
                "Create shipment action should be visible");
    }

    @Test(groups = {ADMIN_GROUP, TestGroups.ORDER, TestGroups.REGRESSION},
            description = "Verify Admin can open shipment details")
    public void verifyAdminCanOpenShipmentDetails() {
        ShipmentDetailsPage shipmentDetailsPage = dashboardPage.openShipmentDetailsPage();

        Assert.assertTrue(shipmentDetailsPage.isShipmentDetailsPageLoaded(),
                "Shipment details page should load successfully");
        Assert.assertTrue(shipmentDetailsPage.isShipmentStatusDisplayed()
                        || !shipmentDetailsPage.getTrackingNumber().isBlank()
                        || !shipmentDetailsPage.getShipmentId().isBlank(),
                "Shipment details should show status, tracking number or shipment id");
    }

    @Test(groups = {ADMIN_GROUP, TestGroups.ORDER, TestGroups.REGRESSION},
            description = "Verify Admin can search shipment tracking")
    public void verifyAdminCanSearchShipmentTracking() {
        String trackingNumber = requiredValue("adminTrackingNumber", "ADMIN_TRACKING_NUMBER");
        ShipmentTrackingPage shipmentTrackingPage = dashboardPage
                .openShipmentTrackingPage()
                .searchShipment(trackingNumber);

        Assert.assertTrue(shipmentTrackingPage.isCurrentStatusDisplayed()
                        || shipmentTrackingPage.isTrackingTimelineDisplayed(),
                "Shipment tracking search should show current status or tracking timeline");
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
