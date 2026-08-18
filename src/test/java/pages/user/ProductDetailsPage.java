package pages.user;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import utils.TestContext;

import java.time.Duration;
import java.util.List;

public class ProductDetailsPage extends BasePage {

    private static final Duration DETAILS_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration CART_ACTION_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration SHORT_ACTION_TIMEOUT = Duration.ofSeconds(4);
    private static final By QUANTITY_SELECTOR =
            By.cssSelector("[role='dialog'][aria-label='Select quantity']");
    private static final By PRODUCT_DETAILS_ACTION_BUTTONS = By.cssSelector("main button");

    private String currentProductName = "";
    private String selectedQuantity = "";

    public ProductDetailsPage(WebDriver driver) {
        super(driver);
    }

    public ProductDetailsPage waitUntilLoaded(String productName) {
        currentProductName = normalizeText(productName);
        waitForPageLoad();
        waitUntil(DETAILS_TIMEOUT, webDriver -> getCurrentUrl().contains("/product/")
                && pageContainsText(productName));
        return this;
    }

    public ProductDetailsPage addToCart() {
        int cartCountBeforeAdd = new CartPage(driver).getCartCount();
        clickActiveAddToCartButton();

        if (waitForQuantitySelectorIfVisible()) {
            selectedQuantity = selectPreferredQuantityOption();
        } else {
            selectedQuantity = getDisplayedSelectedQuantity();
            if (selectedQuantity.isBlank()) {
                selectedQuantity = "Qty: 1";
            }
        }

        TestContext.setSelectedProductQuantity(selectedQuantity);
        CartPage cartPage = new CartPage(driver);
        if (!currentProductName.isBlank()) {
            cartPage.openCartDrawer();
            CartPage.CartProductData confirmedProduct;
            try {
                confirmedProduct = waitUntil(CART_ACTION_TIMEOUT, webDriver -> {
                    CartPage.CartProductData matchingProduct = cartPage.getCartProductRow(currentProductName);
                    if (matchingProduct != null && matchingProduct.isUsableForCheckout()) {
                        return matchingProduct;
                    }

                    List<CartPage.CartProductData> cartProducts = cartPage.getCartProductRows();
                    return cartProducts.size() == 1 && cartProducts.get(0).isUsableForCheckout()
                            ? cartProducts.get(0)
                            : null;
                });
            } catch (TimeoutException exception) {
                throw new TimeoutException("Cart drawer did not confirm product details Add To Cart for '"
                        + currentProductName + "' with quantity '" + selectedQuantity + "'. Cart drawer text: '"
                        + cartPage.getCartDrawerText() + "'.", exception);
            }

            if (!confirmedProduct.productName().isBlank()) {
                currentProductName = confirmedProduct.productName();
            }
            if (!confirmedProduct.quantityText().isBlank()) {
                selectedQuantity = confirmedProduct.quantityText();
            }
        } else {
            waitUntil(CART_ACTION_TIMEOUT, webDriver -> cartPage.getCartCount() > cartCountBeforeAdd);
        }
        return this;
    }

    public boolean verifyCartAddition() {
        CartPage cartPage = new CartPage(driver);
        if (!currentProductName.isBlank()) {
            try {
                cartPage.openCartDrawer().waitForProductLine(currentProductName);
                return cartPage.verifyProductInCart(currentProductName);
            } catch (TimeoutException exception) {
                return false;
            }
        }

        return isAddedToCartToastDisplayed() || cartPage.getCartCount() > 0;
    }

    public String getSelectedQuantity() {
        if (!selectedQuantity.isBlank()) {
            return selectedQuantity;
        }

        return TestContext.getSelectedProductQuantity()
                .orElseGet(this::getDisplayedSelectedQuantity);
    }

    public String getCurrentProductName() {
        return currentProductName;
    }

    public boolean isProductDetailsDisplayed(String productName, String strength, String price, String mrp) {
        String pageText = getMainText();
        String normalizedPageText = pageText.toLowerCase();
        boolean strengthDisplayed = true;

        for (String strengthToken : normalizeText(strength).toLowerCase().split("\\s*-\\s*")) {
            if (!strengthToken.isBlank() && !normalizedPageText.contains(strengthToken)) {
                strengthDisplayed = false;
                break;
            }
        }

        return pageText.contains(productName)
                && strengthDisplayed
                && normalizeCurrencyText(pageText).contains(normalizeCurrencyText(price))
                && normalizeCurrencyText(pageText).contains(normalizeCurrencyText(mrp));
    }

    public boolean isSelectedQuantityDisplayed(String quantityLabel) {
        String normalizedQuantity = normalizeText(quantityLabel);

        return !normalizedQuantity.isBlank()
                && normalizeText(getMainText()).toLowerCase().contains(normalizedQuantity.toLowerCase());
    }

    public boolean isCartStateDisplayed(String quantityLabel, int expectedCartCount) {
        return isSelectedQuantityDisplayed(quantityLabel)
                && new CartPage(driver).isHeaderCartBadgeCountDisplayed(expectedCartCount);
    }

    public boolean hasNoBrokenProductImage() {
        return Boolean.TRUE.equals(executeScript("""
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const main = document.querySelector('main') || document.body;
                const images = Array.from(main.querySelectorAll('img'))
                    .filter(visible)
                    .filter(image => !/icon|logo|placeholder/i.test(
                        image.alt || image.src || image.currentSrc || ''
                    ));

                return images.length > 0
                    && images.every(image => image.complete === true
                        && image.naturalWidth > 0
                        && image.naturalHeight > 0);
                """));
    }

    public boolean hasActiveAddToCartButtonForCurrentProduct() {
        try {
            waitForActiveAddToCartButton();
            return true;
        } catch (TimeoutException exception) {
            return false;
        }
    }

    public CartPage openCartDrawer() {
        return new CartPage(driver).openCartDrawer();
    }

    public ProductDetailsPage refreshAndWaitUntilLoaded(String productName) {
        refreshPage();
        return waitUntilLoaded(productName);
    }

    public ProductDetailsPage waitForCartBadgeCount(int expectedCount) {
        try {
            new CartPage(driver).waitForHeaderCartBadgeCount(expectedCount);
        } catch (TimeoutException exception) {
            throw new TimeoutException("Expected cart badge count " + expectedCount
                    + " on product details page.", exception);
        }

        return this;
    }

    private void clickActiveAddToCartButton() {
        WebElement addButton = waitForActiveAddToCartButton();
        executeScript("arguments[0].scrollIntoView({block: 'center', inline: 'nearest'});", addButton);

        try {
            addButton.click();
        } catch (ElementClickInterceptedException | StaleElementReferenceException exception) {
            executeScript("arguments[0].click();", waitForActiveAddToCartButton());
        }
    }

    private WebElement waitForActiveAddToCartButton() {
        return waitUntil(CART_ACTION_TIMEOUT, webDriver -> findAll(PRODUCT_DETAILS_ACTION_BUTTONS)
                .stream()
                .filter(this::isActiveAddToCartButton)
                .findFirst()
                .orElse(null));
    }

    private boolean isActiveAddToCartButton(WebElement button) {
        try {
            String buttonText = button.getText().replaceAll("\\s+", " ").trim();
            return button.isDisplayed()
                    && button.isEnabled()
                    && buttonText.matches("(?i)^(ADD|Add\\s+To\\s+Cart)$")
                    && !buttonText.matches("(?i).*Notify.*")
                    && Boolean.TRUE.equals(executeScript("""
                            const button = arguments[0];
                            const style = getComputedStyle(button);
                            const rect = button.getBoundingClientRect();
                            return rect.width > 0
                                && rect.height > 0
                                && style.display !== 'none'
                                && style.visibility !== 'hidden'
                                && style.pointerEvents !== 'none'
                                && button.getAttribute('aria-disabled') !== 'true'
                                && !button.closest('[role="dialog"]');
                            """, button));
        } catch (StaleElementReferenceException exception) {
            return false;
        }
    }

    private boolean waitForQuantitySelectorIfVisible() {
        try {
            waitUntil(SHORT_ACTION_TIMEOUT, webDriver -> isDisplayedNow(QUANTITY_SELECTOR));
            return true;
        } catch (TimeoutException exception) {
            return false;
        }
    }

    private String selectPreferredQuantityOption() {
        Object result = waitUntil(CART_ACTION_TIMEOUT, webDriver -> executeScript("""
                const dialog = document.querySelector('[role="dialog"][aria-label="Select quantity"]');
                if (!dialog || !visible(dialog)) {
                    return [];
                }

                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                const canonical = text => normalize(text)
                    .replace(/[×]/g, 'x')
                    .replace(/\\bbot\\b/ig, 'bottle')
                    .toLowerCase();
                const options = Array.from(dialog.querySelectorAll('button, [role="option"]'))
                    .filter(visible)
                    .map(element => ({ element, label: normalize(element.innerText || element.textContent || '') }))
                    .filter(option => option.label);
                const preferredOption = options.find(option => /^1\\s*x\\b/i.test(canonical(option.label)))
                    || options.find(option => /\\b1\\s+(strip|bottle|piece|pcs|pack|tablet|tablets)\\b/i.test(canonical(option.label)))
                    || options[0]
                    || null;

                if (!preferredOption) {
                    return [];
                }

                preferredOption.element.scrollIntoView({ block: 'center', inline: 'nearest' });
                preferredOption.element.click();
                return [preferredOption.label];

                function visible(element) {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                }
                """));

        String quantity = "";
        if (result instanceof List<?> values && !values.isEmpty()) {
            quantity = normalizeText(String.valueOf(values.get(0)));
        }

        waitUntil(CART_ACTION_TIMEOUT, webDriver -> !isDisplayedNow(QUANTITY_SELECTOR));
        waitUntil(CART_ACTION_TIMEOUT, webDriver -> isAddedToCartToastDisplayed()
                || !getDisplayedSelectedQuantity().isBlank()
                || new CartPage(driver).getCartCount() > 0);

        return quantity.isBlank() ? getDisplayedSelectedQuantity() : quantity;
    }

    private boolean isAddedToCartToastDisplayed() {
        return Boolean.TRUE.equals(executeScript("""
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim().toLowerCase();

                return Array.from(document.querySelectorAll(
                    "section[aria-label*='Notifications'] *, [class*='toast'], [class*='Toast'], li"
                ))
                    .filter(visible)
                    .some(element => normalize(element.innerText || element.textContent).includes('added to cart'));
                """));
    }

    private String getDisplayedSelectedQuantity() {
        Object result = executeScript("""
                const main = document.querySelector('main') || document.body;
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                const selectedButton = Array.from(main.querySelectorAll('button'))
                    .filter(visible)
                    .filter(button => !button.closest('[role="dialog"]'))
                    .map(button => normalize(button.innerText || button.textContent || ''))
                    .find(text => text
                        && !/^(ADD|Add\\s+to\\s+Cart|Notify)$/i.test(text)
                        && /(qty|strip|tablet|bottle|piece|pack|\\d)/i.test(text));

                return selectedButton || '';

                function visible(element) {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                }
                """);

        return result == null ? "" : normalizeText(String.valueOf(result));
    }

    private String getMainText() {
        Object result = executeScript("""
                const main = document.querySelector('main') || document.body;
                return String(main.innerText || main.textContent || '')
                    .replace(/\\s+/g, ' ')
                    .trim();
                """);

        return result == null ? "" : String.valueOf(result);
    }

    private String normalizeText(String text) {
        return text == null ? "" : text.replaceAll("\\s+", " ").trim();
    }

    private String normalizeCurrencyText(String text) {
        return normalizeText(text).replace("৳ ", "৳");
    }
}
