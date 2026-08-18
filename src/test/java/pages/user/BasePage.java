package pages.user;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public abstract class BasePage {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration SHORT_TIMEOUT = Duration.ofSeconds(5);

    protected final WebDriver driver;
    private final WebDriverWait wait;

    protected BasePage(WebDriver driver) {
        this(driver, DEFAULT_TIMEOUT);
    }

    protected BasePage(WebDriver driver, Duration timeout) {
        this.driver = Objects.requireNonNull(driver, "WebDriver must not be null");
        this.wait = new WebDriverWait(driver, timeout);
        this.wait.ignoring(StaleElementReferenceException.class);
    }

    protected WebElement waitForVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected List<WebElement> waitForAllVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
    }

    protected WebElement waitForClickable(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    protected WebElement waitForPresence(By locator) {
        return wait.until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    protected boolean waitForInvisible(By locator) {
        return wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    protected void waitForPageLoad() {
        wait.until(webDriver -> "complete".equals(executeScript("return document.readyState")));
    }

    protected <T> T waitUntil(Function<WebDriver, T> condition) {
        return wait.until(condition);
    }

    protected <T> T waitUntil(Duration timeout, Function<WebDriver, T> condition) {
        WebDriverWait customWait = new WebDriverWait(driver, timeout);
        customWait.ignoring(StaleElementReferenceException.class);
        return customWait.until(condition);
    }

    protected void click(By locator) {
        try {
            waitForClickable(locator).click();
        } catch (StaleElementReferenceException exception) {
            waitForClickable(locator).click();
        }
    }

    protected void clickWithFallback(By locator) {
        try {
            click(locator);
        } catch (StaleElementReferenceException | TimeoutException exception) {
            scrollIntoView(locator);
            jsClick(locator);
        }
    }

    protected void type(By locator, String value) {
        WebElement element = waitForVisible(locator);
        element.clear();
        element.sendKeys(value == null ? "" : value);
    }

    protected String getText(By locator) {
        return waitForVisible(locator).getText().trim();
    }

    protected String getAttribute(By locator, String attributeName) {
        return waitForPresence(locator).getAttribute(attributeName);
    }

    protected boolean isDisplayed(By locator) {
        try {
            return waitForVisible(locator).isDisplayed();
        } catch (NoSuchElementException | StaleElementReferenceException | TimeoutException exception) {
            return false;
        }
    }

    protected boolean isDisplayedNow(By locator) {
        try {
            return driver.findElement(locator).isDisplayed();
        } catch (NoSuchElementException | StaleElementReferenceException exception) {
            return false;
        }
    }

    protected boolean isEnabled(By locator) {
        try {
            return waitForVisible(locator).isEnabled();
        } catch (NoSuchElementException | StaleElementReferenceException | TimeoutException exception) {
            return false;
        }
    }

    protected boolean isEnabledNow(By locator) {
        try {
            return driver.findElement(locator).isEnabled();
        } catch (NoSuchElementException | StaleElementReferenceException exception) {
            return false;
        }
    }

    protected boolean isPresent(By locator) {
        try {
            waitForPresence(locator);
            return true;
        } catch (NoSuchElementException | TimeoutException exception) {
            return false;
        }
    }

    protected List<WebElement> findAll(By locator) {
        try {
            return driver.findElements(locator);
        } catch (NoSuchElementException exception) {
            return Collections.emptyList();
        }
    }

    protected long displayedElementCount(By locator) {
        return findAll(locator).stream().filter(this::isSafelyDisplayed).count();
    }

    protected String firstVisibleText(By locator) {
        return findAll(locator)
                .stream()
                .filter(this::isSafelyDisplayed)
                .map(this::safeText)
                .filter(text -> !text.isBlank())
                .findFirst()
                .orElse("");
    }

    protected boolean isImageLoaded(By locator) {
        WebElement image = waitForVisible(locator);
        return Boolean.TRUE.equals(executeScript(
                "return arguments[0].complete === true "
                        + "&& arguments[0].naturalWidth > 0 "
                        + "&& arguments[0].naturalHeight > 0;",
                image
        ));
    }

    protected boolean areDisplayedImagesLoaded(By locator) {
        List<WebElement> images = findAll(locator).stream().filter(this::isSafelyDisplayed).toList();
        return !images.isEmpty() && images.stream().allMatch(this::isImageLoaded);
    }

    protected boolean isImageLoaded(WebElement image) {
        try {
            return Boolean.TRUE.equals(executeScript(
                    "return arguments[0].complete === true "
                            + "&& arguments[0].naturalWidth > 0 "
                            + "&& arguments[0].naturalHeight > 0;",
                    image
            ));
        } catch (StaleElementReferenceException exception) {
            return false;
        }
    }

    protected void scrollIntoView(By locator) {
        executeScript("arguments[0].scrollIntoView({block: 'center', inline: 'center'});", waitForPresence(locator));
    }

    protected void jsClick(By locator) {
        executeScript("arguments[0].click();", waitForPresence(locator));
    }

    protected void refreshPage() {
        driver.navigate().refresh();
        waitForPageLoad();
    }

    protected void waitForUrlContains(String urlFragment) {
        wait.until(ExpectedConditions.urlContains(urlFragment));
    }

    protected String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    protected boolean pageContainsText(String expectedText) {
        return Boolean.TRUE.equals(executeScript(
                "return document.body && document.body.innerText.includes(arguments[0]);",
                expectedText
        ));
    }

    protected void scrollToBottom() {
        executeScript("window.scrollTo(0, document.body.scrollHeight);");
    }

    protected void scrollToTop() {
        executeScript("window.scrollTo(0, 0);");
    }

    protected Object executeScript(String script, Object... arguments) {
        return ((JavascriptExecutor) driver).executeScript(script, arguments);
    }

    protected Object executeAsyncScript(String script, Object... arguments) {
        return ((JavascriptExecutor) driver).executeAsyncScript(script, arguments);
    }

    protected boolean isSafelyDisplayed(WebElement element) {
        try {
            return element.isDisplayed();
        } catch (StaleElementReferenceException exception) {
            return false;
        }
    }

    protected String safeText(WebElement element) {
        try {
            return element.getText().trim();
        } catch (StaleElementReferenceException exception) {
            return "";
        }
    }

    protected WebDriverWait shortWait() {
        WebDriverWait shortWait = new WebDriverWait(driver, SHORT_TIMEOUT);
        shortWait.ignoring(StaleElementReferenceException.class);
        return shortWait;
    }
}
