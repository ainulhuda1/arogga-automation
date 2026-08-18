package pages.admin;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import pages.user.BasePage;

public class DashboardPage extends BasePage {

    private static final By DASHBOARD_HEADING = By.xpath(
            "//*[self::h1 or self::h2 or @data-testid='dashboard-title']"
                    + "[contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'dashboard')]"
    );
    private static final By ORDER_MENU = By.xpath(
            "//a[contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'order') "
                    + "or contains(translate(@href,"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'order')]"
                    + "|//button[contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'order')]"
    );
    private static final By SHIPMENT_MENU = By.xpath(
            "//a[contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'shipment') "
                    + "or contains(translate(@href,"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'shipment')]"
                    + "|//button[contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'shipment')]"
    );
    private static final By TRACKING_MENU = By.xpath(
            "//a[contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'tracking') "
                    + "or contains(translate(@href,"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'tracking')]"
                    + "|//button[contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'tracking')]"
    );
    private static final By SA_SETTINGS_MENU = By.xpath(
            "//a[contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'sa settings') "
                    + "or contains(translate(@href,"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'sa-settings')]"
                    + "|//button[contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'sa settings')]"
    );
    private static final By USER_MENU = By.cssSelector(
            "[data-testid='admin-user-menu'], button[aria-label*='user'], button[aria-label*='profile']"
    );

    public DashboardPage(WebDriver driver) {
        super(driver);
    }

    public DashboardPage waitUntilLoaded() {
        waitForPageLoad();
        waitUntil(webDriver -> isDashboardLoaded());
        return this;
    }

    public boolean isDashboardLoaded() {
        return isDisplayedNow(DASHBOARD_HEADING)
                || pageContainsText("Dashboard");
    }

    public boolean isUserMenuDisplayed() {
        return isDisplayed(USER_MENU);
    }

    public boolean isOrderMenuDisplayed() {
        return isDisplayed(ORDER_MENU);
    }

    public DashboardPage refreshAndWaitUntilLoaded() {
        refreshPage();
        return waitUntilLoaded();
    }

    public boolean isShipmentMenuDisplayed() {
        return isDisplayed(SHIPMENT_MENU);
    }

    public OrdersPage openOrdersPage() {
        return new OrdersPage(driver).openFromDashboard();
    }

    public OrderSearchPage openOrderSearchPage() {
        clickWithFallback(ORDER_MENU);
        return new OrderSearchPage(driver).waitUntilLoaded();
    }

    public ShipmentDetailsPage openShipmentDetailsPage() {
        clickWithFallback(SHIPMENT_MENU);
        return new ShipmentDetailsPage(driver).waitUntilLoaded();
    }

    public ShipmentTrackingPage openShipmentTrackingPage() {
        clickWithFallback(TRACKING_MENU);
        return new ShipmentTrackingPage(driver).waitUntilLoaded();
    }

    public ShipmentCreationPage openSASettingsPage() {
        if (isDisplayedNow(SA_SETTINGS_MENU)) {
            clickWithFallback(SA_SETTINGS_MENU);
        } else {
            openAdminRoute("/sa-settings");
        }

        return new ShipmentCreationPage(driver).waitUntilSASettingsLoaded();
    }

    private void openAdminRoute(String route) {
        String normalizedRoute = route.startsWith("/") ? route : "/" + route;
        String currentUrl = getCurrentUrl();
        int hashIndex = currentUrl.indexOf('#');
        String adminBase = hashIndex >= 0
                ? currentUrl.substring(0, hashIndex + 1)
                : currentUrl.replaceAll("/+$", "") + "/#";

        driver.get(adminBase + normalizedRoute);
    }
}
