package pages.admin;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import pages.user.BasePage;

import java.time.Duration;

public class OrderConfirmationPage extends BasePage {

    private static final By ORDER_STATUS = By.cssSelector(
            "[data-testid='order-status'], [class*='status'], [class*='badge']"
    );
    private static final By CONFIRM_ORDER_BUTTON = By.xpath(
            "//button[contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'confirm')]"
    );
    private static final By CREATE_SHIPMENT_BUTTON = By.xpath(
            "//a[contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'shipment')]"
                    + "|//button[contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'shipment')]"
    );

    public OrderConfirmationPage(WebDriver driver) {
        super(driver);
    }

    public OrderConfirmationPage waitUntilLoaded() {
        waitForPageLoad();
        waitUntil(webDriver -> isOrderConfirmationPageLoaded());
        return this;
    }

    public OrderConfirmationPage waitUntilLoaded(String orderId) {
        waitUntilLoaded();

        if (orderId != null && !orderId.isBlank()) {
            waitUntil(Duration.ofSeconds(10), webDriver -> pageContainsText(orderId));
        }

        return this;
    }

    public boolean isOrderConfirmationPageLoaded() {
        return pageContainsText("Order")
                && (isDisplayedNow(ORDER_STATUS) || isDisplayedNow(CONFIRM_ORDER_BUTTON));
    }

    public String getOrderStatus() {
        return firstVisibleText(ORDER_STATUS);
    }

    public boolean isConfirmOrderButtonVisible() {
        return isDisplayed(CONFIRM_ORDER_BUTTON);
    }

    public boolean isConfirmOrderButtonEnabled() {
        return isEnabled(CONFIRM_ORDER_BUTTON);
    }

    public OrderConfirmationPage confirmOrder() {
        clickWithFallback(CONFIRM_ORDER_BUTTON);

        try {
            waitUntil(Duration.ofSeconds(10), webDriver -> !getOrderStatus().isBlank());
        } catch (TimeoutException ignored) {
            // Status text is asserted by the test when the application exposes it.
        }

        return this;
    }

    public boolean isCreateShipmentButtonVisible() {
        return isDisplayed(CREATE_SHIPMENT_BUTTON);
    }

    public ShipmentCreationPage openShipmentCreationPage() {
        clickWithFallback(CREATE_SHIPMENT_BUTTON);
        return new ShipmentCreationPage(driver).waitUntilLoaded();
    }
}
