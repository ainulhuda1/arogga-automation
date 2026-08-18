package pages.admin;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import pages.user.BasePage;

import java.time.Duration;

public class OrdersPage extends BasePage {

    private static final Duration ORDERS_TIMEOUT = Duration.ofSeconds(30);
    private static final String ORDERS_ROUTE = "/v1/productOrder";

    private static final By ORDERS_MENU = By.xpath(
            "//*[self::a or self::button or self::div or self::span]"
                    + "[contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'orders') "
                    + "or translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')='order' "
                    + "or contains(translate(@href,"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'order')]"
    );
    private static final By ORDERS_SUB_MENU = By.xpath(
            "(//*[self::a or self::button or self::div or self::span]"
                    + "[normalize-space()='Orders' or normalize-space()='Order' or contains(translate(@href,"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'order')]"
                    + ")[last()]"
    );
    private static final By ORDER_ROWS = By.cssSelector(
            "[data-testid='order-row'], table tbody tr, [class*='order-row'], [class*='OrderRow']"
    );
    private static final By EMPTY_STATE = By.xpath(
            "//*[contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'no order') "
                    + "or contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'no data') "
                    + "or contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'not found')]"
    );

    public OrdersPage(WebDriver driver) {
        super(driver);
    }

    public OrdersPage openFromDashboard() {
        if (!isOrdersPageLoaded()) {
            try {
                clickWithFallback(ORDERS_MENU);
                waitForPageLoad();
                waitForAdminLoadingToFinish();
            } catch (RuntimeException exception) {
                openOrdersRoute();
            }
        }

        if (!isOrdersPageLoaded()) {
            try {
                waitUntil(Duration.ofSeconds(10), webDriver -> isOrdersPageLoaded() || clickOrdersSubMenu());
                waitForPageLoad();
                waitForAdminLoadingToFinish();
            } catch (RuntimeException exception) {
                try {
                    clickWithFallback(ORDERS_SUB_MENU);
                    waitForPageLoad();
                    waitForAdminLoadingToFinish();
                } catch (RuntimeException ignored) {
                    openOrdersRoute();
                }
            }
        }

        if (!isOrdersPageLoaded()) {
            openOrdersRoute();
        }

        return waitUntilLoaded();
    }

    public OrdersPage waitUntilLoaded() {
        waitForPageLoad();
        waitUntil(ORDERS_TIMEOUT, webDriver -> isOrdersPageLoaded());
        waitForAdminLoadingToFinish();
        return this;
    }

    public boolean isOrdersPageLoaded() {
        String url = getCurrentUrl().toLowerCase();
        String pageText = getPageText();
        String normalizedPageText = pageText.toLowerCase();
        if (normalizedPageText.contains("e-commerce dashboard")
                || (normalizedPageText.contains("not found") && normalizedPageText.contains("wrong url"))
                || (normalizedPageText.contains("super admin settings") && normalizedPageText.contains("not found"))) {
            return false;
        }

        boolean onOrdersRoute = url.contains(ORDERS_ROUTE.toLowerCase());
        boolean hasOrdersTableText = pageText.matches(
                "(?is).*\\bOrder\\s+List\\b.*\\bID\\b.*\\bUser\\b.*\\bStatus\\b.*"
        ) || pageText.matches(
                "(?is).*\\bOrders\\b.*\\b(ID|User|Customer|Status|Payment|Payable|Delivery|Date).*"
        );

        return (onOrdersRoute || hasOrdersTableText)
                && hasOrderSearchInput();
    }

    public OrdersPage searchOrder(String orderId) {
        enterOrderId(orderId);
        submitSearch();
        waitUntil(ORDERS_TIMEOUT, webDriver -> isOrderDisplayed(orderId) || hasNoResultsMessage());
        return this;
    }

    public OrdersPage enterOrderId(String orderId) {
        WebElement orderSearchInput = waitUntil(ORDERS_TIMEOUT, webDriver -> findOrderSearchInputElement());
        orderSearchInput.clear();
        orderSearchInput.sendKeys(orderId);
        return this;
    }

    public OrdersPage submitSearch() {
        waitUntil(ORDERS_TIMEOUT, webDriver -> findOrderSearchInputElement()).sendKeys(Keys.ENTER);
        waitForAdminLoadingToFinish();
        return this;
    }

    public boolean isOrderDisplayed(String orderId) {
        return orderId != null
                && !orderId.isBlank()
                && displayedElementCount(orderById(orderId)) > 0;
    }

    public boolean isOrderStatusDisplayed(String orderId, String expectedStatus) {
        if (orderId == null || orderId.isBlank() || expectedStatus == null || expectedStatus.isBlank()) {
            return false;
        }

        return Boolean.TRUE.equals(executeScript("""
                const orderId = String(arguments[0] || '').trim();
                const expectedStatus = String(arguments[1] || '').trim().toLowerCase();
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                return Array.from(document.querySelectorAll(
                    'tr, [role="row"], [data-testid*="row"], [class*="row"], [class*="Row"]'
                ))
                    .filter(visible)
                    .some(element => {
                        const text = normalize(element.innerText || element.textContent || '');
                        return text.includes(orderId)
                            && text.toLowerCase().includes(expectedStatus);
                    });
                """, orderId.trim(), expectedStatus.trim()));
    }

    public int getVisibleOrderCount() {
        return (int) displayedElementCount(ORDER_ROWS);
    }

    public boolean hasNoResultsMessage() {
        return isDisplayedNow(EMPTY_STATE);
    }

    public OrderDetailsPage openOrderDetails(String orderId) {
        if (!isOrderDisplayed(orderId)) {
            searchOrder(orderId);
        }

        if (!isOrderDisplayed(orderId)) {
            throw new IllegalStateException("Order " + orderId + " was not displayed on the Admin Orders page. "
                    + "Current URL: " + getCurrentUrl() + ". Page text: " + pageTextSnippet());
        }

        if (!clickOrderIdLink(orderId)) {
            clickWithFallback(orderById(orderId));
        }
        return new OrderDetailsPage(driver).waitUntilLoaded(orderId);
    }

    private By orderById(String orderId) {
        String escapedOrderId = xpathLiteral(orderId);
        return By.xpath("//*[self::tr or self::a or self::button or @data-testid='order-row' "
                + "or contains(@class,'order-row') or contains(@class,'OrderRow')]"
                + "[.//*[normalize-space()=" + escapedOrderId + "] "
                + "or contains(normalize-space()," + escapedOrderId + ")]");
    }

    private void waitForAdminLoadingToFinish() {
        waitUntil(ORDERS_TIMEOUT, webDriver -> Boolean.TRUE.equals(executeScript("""
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };

                return !Array.from(document.querySelectorAll(
                    '[role="progressbar"], [class*="spinner"], [class*="loader"], [class*="loading"], .ant-spin, .MuiCircularProgress-root'
                )).some(visible);
                """)));
    }

    private void openAdminRoute(String route) {
        String normalizedRoute = route.startsWith("/") ? route : "/" + route;
        String currentUrl = getCurrentUrl();
        int hashIndex = currentUrl.indexOf('#');
        String adminBase = hashIndex >= 0
                ? currentUrl.substring(0, hashIndex + 1)
                : currentUrl.replaceAll("/+$", "") + "/#";

        driver.get(adminBase + normalizedRoute);
        waitForPageLoad();
    }

    private void openOrdersRoute() {
        openAdminRoute(ORDERS_ROUTE);
        waitForAdminLoadingToFinish();
    }

    private String xpathLiteral(String value) {
        if (value == null) {
            return "''";
        }

        if (!value.contains("'")) {
            return "'" + value + "'";
        }

        if (!value.contains("\"")) {
            return "\"" + value + "\"";
        }

        String[] parts = value.split("'");
        StringBuilder literal = new StringBuilder("concat(");
        for (int index = 0; index < parts.length; index++) {
            if (index > 0) {
                literal.append(", \"'\", ");
            }
            literal.append("'").append(parts[index]).append("'");
        }
        literal.append(")");
        return literal.toString();
    }

    private boolean clickOrdersSubMenu() {
        return Boolean.TRUE.equals(executeScript("""
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                const candidates = Array.from(document.querySelectorAll('a, button, div, span, li'))
                    .filter(visible)
                    .filter(element => normalize(element.innerText || element.textContent) === 'Orders')
                    .map(element => element.closest('a, button, [role="button"], li, div') || element)
                    .filter(visible)
                    .map(element => ({ element, rect: element.getBoundingClientRect() }))
                    .sort((first, second) =>
                        (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                    );

                const target = candidates[0]?.element || null;
                if (!target) {
                    return false;
                }

                target.scrollIntoView({ block: 'center', inline: 'nearest' });
                target.click();
                return true;
                """));
    }

    private boolean hasOrderSearchInput() {
        return findOrderSearchInputElement() != null;
    }

    private WebElement findOrderSearchInputElement() {
        Object result = executeScript("""
                const visible = element => {
                    if (!element) {
                        return false;
                    }

                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0
                        && !element.disabled;
                };
                const textOf = element => String(element?.innerText || element?.textContent || '')
                    .replace(/\\s+/g, ' ')
                    .trim()
                    .toLowerCase();
                const attrOf = input => [
                        input.getAttribute('name'),
                        input.getAttribute('id'),
                        input.getAttribute('placeholder'),
                        input.getAttribute('aria-label'),
                        input.getAttribute('type')
                    ]
                    .filter(Boolean)
                    .join(' ')
                    .replace(/\\s+/g, ' ')
                    .trim()
                    .toLowerCase();
                const isGlobalMenuSearch = input => {
                    const attrs = attrOf(input);
                    const context = textOf(input.closest('header, [role="banner"], [class*="Header"], [class*="AppBar"], [class*="Toolbar"]'));
                    return attrs.includes('search menu')
                        || (attrs.includes('menu') && (context.includes('dashboard') || context.length < 120));
                };
                const hasOrderContext = input => {
                    const contextRoot = input.closest(
                        'form, [role="search"], [class*="filter"], [class*="Filter"], [class*="list"], [class*="List"], main, [class*="MuiPaper"]'
                    ) || input.parentElement;
                    const context = textOf(contextRoot);
                    return /\\borders?\\b|\\border\\s*id\\b|\\bpayable\\b|\\bpayment\\s*status\\b|\\bcustomer\\b|\\buser\\b|\\bdelivery\\b|\\bstatus\\b|\\bdate\\b/.test(context);
                };
                const onProductOrderRoute = window.location.hash.toLowerCase().includes('/v1/productorder');

                if (onProductOrderRoute) {
                    const routeMatches = Array.from(document.querySelectorAll('input, textarea'))
                        .filter(visible)
                        .filter(input => !isGlobalMenuSearch(input))
                        .filter(input => {
                            const attrs = attrOf(input);
                            return !/password|otp|mobile|phone/.test(attrs)
                                && (/\\bsearch\\b|\\border\\b|\\bid\\b|po[_ -]?id|product\\s*order|order[_ -]?id/.test(attrs)
                                    || input.getBoundingClientRect().top < 180);
                        })
                        .map(input => {
                            const attrs = attrOf(input);
                            const rect = input.getBoundingClientRect();
                            const score = (/\\border\\b|po[_ -]?id|order[_ -]?id/.test(attrs) ? 0 : 10)
                                + (/\\bsearch\\b/.test(attrs) ? 0 : 20)
                                + (rect.left > window.innerWidth * 0.6 ? 20 : 0)
                                + (rect.top < 60 ? 10 : 0);
                            return { input, score };
                        })
                        .sort((first, second) => first.score - second.score);

                    if (routeMatches[0]) {
                        return routeMatches[0].input;
                    }
                }

                const candidates = Array.from(document.querySelectorAll('input, textarea'))
                    .filter(visible)
                    .filter(input => !isGlobalMenuSearch(input))
                    .filter(input => {
                        const attrs = attrOf(input);
                        if (/password|otp|mobile|phone/.test(attrs)) {
                            return false;
                        }

                        if (/\\border\\b|po[_ -]?id|product\\s*order|order[_ -]?id/.test(attrs)) {
                            return true;
                        }

                        if (/\\bsearch\\b|\\bid\\b/.test(attrs)) {
                            return onProductOrderRoute || hasOrderContext(input);
                        }

                        return false;
                    })
                    .map(input => {
                        const attrs = attrOf(input);
                        const rect = input.getBoundingClientRect();
                        const score = (/\\border\\b|po[_ -]?id|order[_ -]?id/.test(attrs) ? 0 : 10)
                            + (/\\bsearch\\b/.test(attrs) ? 1 : 0)
                            + (rect.top < 140 ? 5 : 0);
                        return { input, score };
                    })
                    .sort((first, second) => first.score - second.score);

                return candidates[0]?.input || null;
                """);

        return result instanceof WebElement ? (WebElement) result : null;
    }

    private boolean clickOrderIdLink(String orderId) {
        if (orderId == null || orderId.isBlank()) {
            return false;
        }

        return Boolean.TRUE.equals(executeScript("""
                const orderId = String(arguments[0] || '').trim();
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                const compact = text => normalize(text).replace(/\\s+/g, '');
                const target = Array.from(document.querySelectorAll('a, button, [role="button"]'))
                    .filter(visible)
                    .map(element => ({ element, text: normalize(element.innerText || element.textContent || '') }))
                    .filter(candidate => compact(candidate.text).includes(compact(orderId)))
                    .sort((first, second) =>
                        (first.element.getBoundingClientRect().width * first.element.getBoundingClientRect().height)
                            - (second.element.getBoundingClientRect().width * second.element.getBoundingClientRect().height)
                    )[0]?.element || null;

                if (!target) {
                    return false;
                }

                target.scrollIntoView({ block: 'center', inline: 'nearest' });
                target.click();
                return true;

                function visible(element) {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                }
                """, orderId.trim()));
    }

    private String getPageText() {
        Object result = executeScript("""
                return document.body
                    ? String(document.body.innerText || document.body.textContent || '').replace(/\\s+/g, ' ').trim()
                    : '';
                """);

        return result == null ? "" : String.valueOf(result);
    }

    private String pageTextSnippet() {
        String pageText = getPageText();
        return pageText.length() <= 500 ? pageText : pageText.substring(0, 500);
    }
}
