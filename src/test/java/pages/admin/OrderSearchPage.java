package pages.admin;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import pages.user.BasePage;

import java.time.Duration;

public class OrderSearchPage extends BasePage {

    private static final By ORDER_SEARCH_INPUT = By.cssSelector(
            "input[name='orderId'], input[name='order_id'], input[placeholder*='Order'], input[type='search']"
    );
    private static final By ORDER_SEARCH_BUTTON = By.xpath(
            "//button[@type='submit' or contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'search')]"
    );
    private static final By ORDER_ROWS = By.cssSelector(
            "[data-testid='order-row'], table tbody tr, [class*='order-row']"
    );
    private static final By EMPTY_STATE = By.xpath(
            "//*[contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'no order') "
                    + "or contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'no data')]"
    );

    public OrderSearchPage(WebDriver driver) {
        super(driver);
    }

    public OrderSearchPage waitUntilLoaded() {
        waitForPageLoad();
        waitForVisible(ORDER_SEARCH_INPUT);
        return this;
    }

    public boolean isOrderSearchPageLoaded() {
        return isDisplayed(ORDER_SEARCH_INPUT);
    }

    public OrderSearchPage enterOrderId(String orderId) {
        type(ORDER_SEARCH_INPUT, orderId);
        return this;
    }

    public OrderSearchPage submitSearch() {
        clickWithFallback(ORDER_SEARCH_BUTTON);
        return this;
    }

    public OrderSearchPage searchOrder(String orderId) {
        enterOrderId(orderId);
        submitSearch();
        waitUntil(Duration.ofSeconds(15), webDriver -> isOrderListed(orderId) || hasNoResultsMessage());
        return this;
    }

    public boolean isOrderListed(String orderId) {
        if (orderId == null || orderId.isBlank()) {
            return false;
        }

        return displayedElementCount(orderRowById(orderId)) > 0;
    }

    public int getVisibleOrderCount() {
        return (int) displayedElementCount(ORDER_ROWS);
    }

    public boolean hasNoResultsMessage() {
        return isDisplayedNow(EMPTY_STATE);
    }

    public OrderConfirmationPage openOrderConfirmationPage(String orderId) {
        clickWithFallback(orderRowById(orderId));
        return new OrderConfirmationPage(driver).waitUntilLoaded(orderId);
    }

    private By orderRowById(String orderId) {
        return By.xpath("//*[self::tr or @data-testid='order-row' or contains(@class,'order-row')]"
                + "[.//*[normalize-space()='" + orderId + "'] or contains(normalize-space(),'" + orderId + "')]");
    }
}
