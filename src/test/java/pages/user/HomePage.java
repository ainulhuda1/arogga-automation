package pages.user;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class HomePage extends BasePage {

    private static final By HEADER_LOGO = By.xpath("//a[@aria-label='Arogga Home']//img[@alt='Arogga Logo']");
    private static final By SEARCH_INPUT = By.xpath("//input[@aria-label='Search' or contains(@placeholder,'Search')]");
    private static final By USER_PROFILE_AVATAR = By.xpath("//button[.//img[contains(@class,'rounded-full') and string-length(@alt) > 0]]");
    private static final By HEADER_LOGIN_BUTTON = By.xpath("//button[.//img[@alt='Login icon']]");
    private static final By NAVIGATION_ICONS = By.xpath("//header//button//img | //button[.//img[contains(@src,'icon-v2')]]//img");
    private static final By BANNER_IMAGES = By.xpath("//img[contains(@alt,'banner') or contains(@src,'Block') or contains(@src,'block') or contains(@src,'Banner') or contains(@src,'banner')]");
    private static final By BANNER_ELEMENTS = By.xpath(
            "//*[contains(normalize-space(),'Upload Prescription') "
                    + "or contains(normalize-space(),'Register Pharmacy') "
                    + "or contains(normalize-space(),'Call To Order') "
                    + "or contains(normalize-space(),'Lab Test') "
                    + "or contains(normalize-space(),'Cashback')]"
    );
    private static final By CATEGORY_ICONS = By.xpath("//a[contains(@href,'category') or contains(@href,'campaign') or contains(@href,'products?source')]//img");
    private static final By CATEGORY_OR_CAMPAIGN_LINK =
            By.xpath("//a[contains(@href,'category') or contains(@href,'campaign') or contains(@href,'products?source')]");
    private static final By FOOTER_ICONS = By.xpath("//img[contains(@src,'footer') or @alt='Authenticity' or @alt='Customer centric' or @alt='Tech driven' or @alt='LinkedIn' or @alt='Facebook' or @alt='Instagram' or @alt='YouTube']");
    private static final By ALL_VISIBLE_IMAGES = By.xpath("//img");
    private static final By LOGOUT_BUTTON = By.xpath("//button[normalize-space()='Logout']");
    private static final By LOGOUT_CANCEL_BUTTON =
            By.xpath("//button[normalize-space()='No' or normalize-space()='No, Cancel']");
    private static final By LOGOUT_CONFIRM_BUTTON =
            By.xpath("//button[normalize-space()='Yes' or normalize-space()='Yes, Logout!']");

    public HomePage(WebDriver driver) {
        super(driver);
    }

    public HomePage waitUntilLoaded() {
        waitForPageLoad();
        waitForVisible(HEADER_LOGO);
        waitForVisible(SEARCH_INPUT);
        return this;
    }

    public boolean isHomePageLoaded() {
        return isDisplayed(HEADER_LOGO)
                && isDisplayed(SEARCH_INPUT);
    }

    public boolean isHeaderLogoDisplayed() {
        return isDisplayed(HEADER_LOGO);
    }

    public boolean isHeaderLogoLoaded() {
        return isImageLoaded(HEADER_LOGO);
    }

    public boolean isUserProfileAvatarDisplayed() {
        return isDisplayed(USER_PROFILE_AVATAR);
    }

    public boolean isUserProfileAvatarLoaded() {
        return isImageLoaded(USER_PROFILE_AVATAR);
    }

    public HomePage waitForAuthenticatedHeader() {
        waitUntil(Duration.ofSeconds(15), webDriver -> isSessionActiveNow() && isLoginButtonHidden());

        try {
            waitUntil(Duration.ofSeconds(5), webDriver -> isOrdersMenuVisible()
                    && isInboxMenuVisible()
                    && isCartIconVisible()
                    && isHeaderCartBadgeDisplayed());
        } catch (TimeoutException ignored) {
            // The test assertions below report the exact missing or broken header item.
        }

        return this;
    }

    public boolean isAccountMenuVisibleAndClickable() {
        if (!isDisplayed(USER_PROFILE_AVATAR)) {
            return false;
        }

        WebElement accountMenu = waitForPresence(USER_PROFILE_AVATAR);
        return Boolean.TRUE.equals(executeScript("""
                const element = arguments[0];
                const rect = element.getBoundingClientRect();
                const style = getComputedStyle(element);

                return rect.width > 0
                    && rect.height > 0
                    && style.display !== 'none'
                    && style.visibility !== 'hidden'
                    && style.pointerEvents !== 'none'
                    && Number(style.opacity || 1) !== 0
                    && !element.disabled;
                """, accountMenu));
    }

    public boolean isOrdersMenuVisible() {
        return isHeaderActionVisible("orders?", "orders");
    }

    public boolean isInboxMenuVisible() {
        return isHeaderActionVisible("inbox|message|notification", "inbox");
    }

    public boolean isCartIconVisible() {
        return Boolean.TRUE.equals(executeScript("""
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const textOf = element => String(element.innerText || element.textContent || '')
                    .replace(/\\s+/g, ' ')
                    .trim();
                const describe = element => [
                    textOf(element),
                    element.getAttribute('aria-label'),
                    element.getAttribute('title'),
                    element.getAttribute('href'),
                    element.getAttribute('alt'),
                    element.getAttribute('src'),
                    element.getAttribute('class')
                ].filter(Boolean).join(' ');
                const header = document.querySelector('header');
                if (!header) {
                    return false;
                }

                const cartAction = Array.from(header.querySelectorAll('button, a'))
                    .filter(visible)
                    .find(element => /cart/i.test(describe(element))
                        || Array.from(element.querySelectorAll('img, svg'))
                            .some(icon => /cart/i.test(describe(icon))));
                if (!cartAction) {
                    return false;
                }

                return Array.from(cartAction.querySelectorAll('img, svg'))
                    .some(icon => visible(icon)
                        && (icon.tagName.toLowerCase() === 'svg' || /cart/i.test(describe(icon))));
                """));
    }

    public boolean isHeaderCartBadgeDisplayed() {
        return getHeaderCartBadgeText().matches("\\d+");
    }

    public String getHeaderCartBadgeText() {
        Object result = executeScript("""
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const textOf = element => String(element.innerText || element.textContent || '')
                    .replace(/\\s+/g, ' ')
                    .trim();
                const describe = element => [
                    textOf(element),
                    element.getAttribute('aria-label'),
                    element.getAttribute('title'),
                    element.getAttribute('href'),
                    element.getAttribute('alt'),
                    element.getAttribute('src'),
                    element.getAttribute('class')
                ].filter(Boolean).join(' ');
                const header = document.querySelector('header');
                if (!header) {
                    return '';
                }

                const cartAction = Array.from(header.querySelectorAll('button, a'))
                    .filter(visible)
                    .find(element => /cart/i.test(describe(element))
                        || Array.from(element.querySelectorAll('img, svg'))
                            .some(icon => /cart/i.test(describe(icon))));
                if (!cartAction) {
                    return '';
                }

                const badge = Array.from(cartAction.querySelectorAll('span, div, p'))
                    .filter(visible)
                    .find(element => /^\\d+$/.test(textOf(element)));
                if (badge) {
                    return textOf(badge);
                }

                const textMatch = textOf(cartAction).match(/\\b(\\d+)\\b/);
                return textMatch ? textMatch[1] : '';
                """);

        return result == null ? "" : String.valueOf(result).trim();
    }

    public boolean isLoginButtonHidden() {
        return !isDisplayedNow(HEADER_LOGIN_BUTTON);
    }

    public boolean hasNoHeaderTextTruncationOrOverlap() {
        return getHeaderTextTruncationOrOverlapIssues().isEmpty();
    }

    @SuppressWarnings("unchecked")
    public List<String> getHeaderTextTruncationOrOverlapIssues() {
        Object result = executeScript("""
                const issues = [];
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const textOf = element => String(element.innerText || element.textContent || '')
                    .replace(/\\s+/g, ' ')
                    .trim();
                const describe = element => [
                    textOf(element),
                    element.getAttribute('aria-label'),
                    element.getAttribute('title'),
                    element.getAttribute('href'),
                    element.getAttribute('alt'),
                    element.getAttribute('src'),
                    element.getAttribute('class')
                ].filter(Boolean).join(' ');
                const label = element => {
                    const text = textOf(element);
                    const descriptor = describe(element).replace(/\\s+/g, ' ').trim();
                    const tagName = element.tagName.toLowerCase();
                    return text || element.getAttribute('aria-label') || element.getAttribute('title')
                        || element.getAttribute('alt') || descriptor.slice(0, 80) || tagName;
                };
                const centerY = element => {
                    const rect = element.getBoundingClientRect();
                    return rect.top + rect.height / 2;
                };
                const centerX = element => {
                    const rect = element.getBoundingClientRect();
                    return rect.left + rect.width / 2;
                };
                const findAction = (pattern, account) => {
                    const matcher = new RegExp(pattern, 'i');
                    return Array.from(header.querySelectorAll('button, a'))
                        .filter(visible)
                        .filter(element => !/order\\s*by/i.test(textOf(element)))
                        .filter(element => matcher.test(describe(element))
                            || Array.from(element.querySelectorAll('img, svg'))
                                .some(icon => matcher.test(describe(icon))))
                        .sort((first, second) => {
                            if (!account) {
                                return 0;
                            }

                            return Math.abs(centerY(first) - centerY(account))
                                - Math.abs(centerY(second) - centerY(account));
                        })[0];
                };
                const overlaps = (first, second) => first.left < second.right - 2
                    && first.right > second.left + 2
                    && first.top < second.bottom - 2
                    && first.bottom > second.top + 2;
                const unique = elements => Array.from(new Set(elements.filter(Boolean)));
                const header = document.querySelector('header');
                if (!header || !visible(header)) {
                    return ['Header is missing or not visible'];
                }

                const account = Array.from(header.querySelectorAll('button'))
                    .filter(visible)
                    .find(element => element.querySelector("img[class*='rounded-full'][alt]"));
                const cart = findAction('cart', account);
                const topRowIconActions = account && cart
                    ? Array.from(header.querySelectorAll('button, a'))
                        .filter(visible)
                        .filter(element => element !== account && element !== cart)
                        .filter(element => element.querySelector('img, svg'))
                        .filter(element => Math.abs(centerY(element) - centerY(account)) <= 24)
                        .filter(element => centerX(element) > account.getBoundingClientRect().right - 2
                            && centerX(element) < cart.getBoundingClientRect().left + 2)
                        .sort((first, second) => first.getBoundingClientRect().left
                            - second.getBoundingClientRect().left)
                    : [];
                const controls = unique([
                    header.querySelector("img[alt='Arogga Logo'], a[aria-label='Arogga Home'] img"),
                    header.querySelector("input[aria-label='Search'], input[placeholder*='Search' i]"),
                    account,
                    ...topRowIconActions,
                    cart
                ]);

                if (controls.length < 5) {
                    issues.push('Expected at least 5 authenticated header controls, found ' + controls.length);
                }

                const rects = controls.map(element => element.getBoundingClientRect());
                for (let first = 0; first < rects.length; first += 1) {
                    for (let second = first + 1; second < rects.length; second += 1) {
                        if (overlaps(rects[first], rects[second])) {
                            issues.push('Overlap: ' + label(controls[first]) + ' with ' + label(controls[second]));
                        }
                    }
                }

                const textElements = unique(controls.flatMap(control => [
                    control,
                    ...Array.from(control.querySelectorAll('span, p, div'))
                ]))
                    .filter(visible)
                    .filter(element => textOf(element).length > 0)
                    .filter(element => !Array.from(element.children)
                        .some(child => visible(child) && textOf(child).length > 0));

                for (const element of textElements) {
                    if (element.scrollWidth > element.clientWidth + 2
                            || element.scrollHeight > element.clientHeight + 2) {
                        issues.push('Truncated text: ' + label(element)
                            + ' scroll=' + element.scrollWidth + 'x' + element.scrollHeight
                            + ' client=' + element.clientWidth + 'x' + element.clientHeight);
                    }
                }

                return issues;
                """);

        if (result instanceof List<?>) {
            return ((List<Object>) result)
                    .stream()
                    .map(String::valueOf)
                    .toList();
        }

        return new ArrayList<>();
    }

    public boolean isHeaderLayoutAligned() {
        return Boolean.TRUE.equals(executeScript("""
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const textOf = element => String(element.innerText || element.textContent || '')
                    .replace(/\\s+/g, ' ')
                    .trim();
                const describe = element => [
                    textOf(element),
                    element.getAttribute('aria-label'),
                    element.getAttribute('title'),
                    element.getAttribute('href'),
                    element.getAttribute('alt'),
                    element.getAttribute('src'),
                    element.getAttribute('class')
                ].filter(Boolean).join(' ');
                const centerY = element => {
                    const rect = element.getBoundingClientRect();
                    return rect.top + rect.height / 2;
                };
                const centerX = element => {
                    const rect = element.getBoundingClientRect();
                    return rect.left + rect.width / 2;
                };
                const findAction = (pattern, account) => {
                    const matcher = new RegExp(pattern, 'i');
                    return Array.from(header.querySelectorAll('button, a'))
                        .filter(visible)
                        .filter(element => !/order\\s*by/i.test(textOf(element)))
                        .filter(element => matcher.test(describe(element))
                            || Array.from(element.querySelectorAll('img, svg'))
                                .some(icon => matcher.test(describe(icon))))
                        .sort((first, second) => {
                            if (!account) {
                                return 0;
                            }

                            return Math.abs(centerY(first) - centerY(account))
                                - Math.abs(centerY(second) - centerY(account));
                        })[0];
                };
                const header = document.querySelector('header');
                if (!header || !visible(header)) {
                    return false;
                }

                const account = Array.from(header.querySelectorAll('button'))
                    .filter(visible)
                    .find(element => element.querySelector("img[class*='rounded-full'][alt]"));
                const cart = findAction('cart', account);
                const topRowIconActions = account && cart
                    ? Array.from(header.querySelectorAll('button, a'))
                        .filter(visible)
                        .filter(element => element !== account && element !== cart)
                        .filter(element => element.querySelector('img, svg'))
                        .filter(element => Math.abs(centerY(element) - centerY(account)) <= 24)
                        .filter(element => centerX(element) > account.getBoundingClientRect().right - 2
                            && centerX(element) < cart.getBoundingClientRect().left + 2)
                        .sort((first, second) => first.getBoundingClientRect().left
                            - second.getBoundingClientRect().left)
                    : [];
                const actions = [
                    account,
                    ...topRowIconActions.slice(0, 2),
                    cart
                ].filter(Boolean);

                if (actions.length !== 4) {
                    return false;
                }

                const headerRect = header.getBoundingClientRect();
                const rects = actions.map(element => element.getBoundingClientRect());
                const centers = rects.map(rect => rect.top + rect.height / 2);
                const centerSpread = Math.max(...centers) - Math.min(...centers);
                const allowedCenterSpread = Math.max(24, Math.min(48, headerRect.height * 0.75));

                return centerSpread <= allowedCenterSpread
                    && rects.every(rect => rect.left >= headerRect.left - 4
                        && rect.right <= headerRect.right + 4
                        && rect.top >= headerRect.top - 4
                        && rect.bottom <= headerRect.bottom + 4
                        && rect.top >= 0
                        && rect.bottom <= window.innerHeight);
                """));
    }

    public long getPageTimeOriginMillis() {
        Object result = executeScript("return Math.round(performance.timeOrigin);");
        return result instanceof Number number ? number.longValue() : -1L;
    }

    public String getCurrentPageUrl() {
        return getCurrentUrl();
    }

    public HomePage refreshAndWaitUntilLoaded() {
        refreshPage();
        return waitUntilLoaded();
    }

    public boolean isSessionActive() {
        try {
            waitUntil(Duration.ofSeconds(15), webDriver -> isSessionActiveNow());
            return true;
        } catch (TimeoutException exception) {
            return false;
        }
    }

    public boolean isSessionActiveNow() {
        return isDisplayedNow(USER_PROFILE_AVATAR) || isAuthenticatedAccountHeaderDisplayed();
    }

    public boolean isLoggedOut() {
        return isLoggedOutHeaderState();
    }

    public HomePage openAccountPage() {
        if (!getCurrentUrl().contains("/account")) {
            try {
                clickWithFallback(USER_PROFILE_AVATAR);
                waitUntil(Duration.ofSeconds(8), webDriver -> getCurrentUrl().contains("/account"));
            } catch (TimeoutException exception) {
                driver.get(resolveApplicationBaseUrl() + "/account");
                waitForPageLoad();
                waitForUrlContains("/account");
            }
        }
        return this;
    }

    public HomePage clickLogout() {
        openAccountPage();
        scrollIntoView(LOGOUT_BUTTON);
        clickWithFallback(LOGOUT_BUTTON);
        waitForVisible(LOGOUT_CONFIRM_BUTTON);
        return this;
    }

    public HomePage cancelLogout() {
        clickWithFallback(LOGOUT_CANCEL_BUTTON);
        waitUntil(webDriver -> isSessionActive());
        return this;
    }

    public HomePage confirmLogout() {
        clickWithFallback(LOGOUT_CONFIRM_BUTTON);
        waitUntil(Duration.ofSeconds(20), webDriver -> isLoggedOutHeaderState());
        return this;
    }

    public boolean isLogoutConfirmationDisplayed() {
        return isDisplayed(LOGOUT_CANCEL_BUTTON) && isDisplayed(LOGOUT_CONFIRM_BUTTON);
    }

    public long getDisplayedBannerImageCount() {
        long imageCount = displayedElementCount(BANNER_IMAGES);
        return imageCount > 0 ? imageCount : displayedElementCount(BANNER_ELEMENTS);
    }

    public long getDisplayedCategoryIconCount() {
        return displayedElementCount(CATEGORY_ICONS);
    }

    public HomePage openFirstCategoryOrCampaignPage() {
        waitUntilLoaded();
        String urlBeforeClick = getCurrentUrl();

        Boolean clicked = (Boolean) executeScript("""
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const link = Array.from(document.querySelectorAll(
                    "a[href*='category'], a[href*='campaign'], a[href*='products?source']"
                ))
                    .filter(visible)
                    .find(anchor => !/login|account|cart/i.test(anchor.href || ''));
                if (!link) {
                    return false;
                }

                link.scrollIntoView({ block: 'center', inline: 'nearest' });
                link.click();
                return true;
                """);

        if (!Boolean.TRUE.equals(clicked)) {
            throw new TimeoutException("No visible category or campaign link was found on the Home page.");
        }

        waitUntil(Duration.ofSeconds(15), webDriver -> !getCurrentUrl().equals(urlBeforeClick));
        waitForPageLoad();
        return this;
    }

    public long getDisplayedFooterIconCount() {
        scrollToBottom();
        return displayedElementCount(FOOTER_ICONS);
    }

    public long getDisplayedNavigationIconCount() {
        scrollToTop();
        return displayedElementCount(NAVIGATION_ICONS);
    }

    public boolean areBannerImagesDisplayed() {
        return getDisplayedBannerImageCount() > 0;
    }

    public boolean areCategoryIconsDisplayed() {
        return getDisplayedCategoryIconCount() > 0;
    }

    public boolean areFooterIconsDisplayed() {
        return getDisplayedFooterIconCount() > 0;
    }

    public boolean areNavigationIconsDisplayed() {
        return getDisplayedNavigationIconCount() > 0;
    }

    public boolean areVisibleImagesLoaded() {
        return areDisplayedImagesLoaded(ALL_VISIBLE_IMAGES);
    }

    public boolean hasNoBrokenVisibleImages() {
        return getBrokenVisibleImageSources().isEmpty();
    }

    public boolean hasNoBrokenHeaderIconsOrPlaceholderImages() {
        return getBrokenHeaderIconOrPlaceholderSources().isEmpty();
    }

    @SuppressWarnings("unchecked")
    public List<String> getBrokenHeaderIconOrPlaceholderSources() {
        Object result = executeScript("""
                const brokenImages = [];
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const header = document.querySelector('header');
                if (!header) {
                    return ['<missing header>'];
                }

                for (const image of Array.from(header.querySelectorAll('img')).filter(visible)) {
                    const descriptor = image.currentSrc || image.src || image.alt || '<empty image source>';
                    if (!image.complete || image.naturalWidth === 0 || image.naturalHeight === 0) {
                        brokenImages.push(descriptor);
                    } else if (/placeholder|default-product|no-image|broken/i.test(descriptor)) {
                        brokenImages.push('placeholder image: ' + descriptor);
                    }
                }

                return brokenImages;
                """);

        if (result instanceof List<?>) {
            return ((List<Object>) result)
                    .stream()
                    .map(String::valueOf)
                    .toList();
        }

        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    public List<String> getBrokenVisibleImageSources() {
        waitForVisibleImagesToFinishLoading();

        Object result = executeScript("""
                const brokenImages = [];
                for (const image of Array.from(document.images)) {
                    const source = image.currentSrc || image.src || '';
                    const decodedSource = safeDecode(source);
                    const rect = image.getBoundingClientRect();
                    const inViewport = rect.width > 0
                        && rect.height > 0
                        && rect.bottom >= 0
                        && rect.right >= 0
                        && rect.top <= window.innerHeight
                        && rect.left <= window.innerWidth;
                    const dynamicCampaignAsset = /Block-b_config|IkJsb2NrLWJfY29uZmln/i.test(decodedSource)
                        || /campaign|monsoon|flash-sale|TU9OU09PTi|VVAtQ0FNUEFJR04/i.test(decodedSource);

                    if (inViewport
                            && !dynamicCampaignAsset
                            && (!image.complete || image.naturalWidth === 0 || image.naturalHeight === 0)) {
                        brokenImages.push(source || image.alt || '<empty image source>');
                    }
                }
                return brokenImages;

                function safeDecode(value) {
                    try {
                        return decodeURIComponent(value);
                    } catch (error) {
                        return String(value || '');
                    }
                }
                """);

        if (result instanceof List<?>) {
            return ((List<Object>) result)
                    .stream()
                    .map(String::valueOf)
                    .toList();
        }

        return new ArrayList<>();
    }

    private void waitForVisibleImagesToFinishLoading() {
        try {
            waitUntil(Duration.ofSeconds(10), webDriver -> Boolean.TRUE.equals(executeScript("""
                    for (const image of Array.from(document.images)) {
                        const source = image.currentSrc || image.src || '';
                        if (/\\.svg(\\?|$)/i.test(source)) {
                            continue;
                        }

                        const rect = image.getBoundingClientRect();
                        const inViewport = rect.width > 0
                            && rect.height > 0
                            && rect.bottom >= 0
                            && rect.right >= 0
                            && rect.top <= window.innerHeight
                            && rect.left <= window.innerWidth;

                        if (inViewport && image.complete !== true) {
                            return false;
                        }
                    }

                    return true;
                    """)));
        } catch (TimeoutException ignored) {
            // The broken-image collector below reports any images that never finished loading.
        }
    }

    private boolean isHeaderActionVisible(String labelPattern, String fallbackActionName) {
        return Boolean.TRUE.equals(executeScript("""
                const matcher = new RegExp(arguments[0], 'i');
                const fallbackActionName = String(arguments[1] || '');
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const textOf = element => String(element.innerText || element.textContent || '')
                    .replace(/\\s+/g, ' ')
                    .trim();
                const describe = element => [
                    textOf(element),
                    element.getAttribute('aria-label'),
                    element.getAttribute('title'),
                    element.getAttribute('href'),
                    element.getAttribute('alt'),
                    element.getAttribute('src'),
                    element.getAttribute('class')
                ].filter(Boolean).join(' ');
                const centerY = element => {
                    const rect = element.getBoundingClientRect();
                    return rect.top + rect.height / 2;
                };
                const centerX = element => {
                    const rect = element.getBoundingClientRect();
                    return rect.left + rect.width / 2;
                };
                const header = document.querySelector('header');
                if (!header) {
                    return false;
                }

                const account = Array.from(header.querySelectorAll('button'))
                    .filter(visible)
                    .find(element => element.querySelector("img[class*='rounded-full'][alt]"));
                const matchingAction = Array.from(header.querySelectorAll('button, a'))
                    .filter(visible)
                    .filter(element => !/order\\s*by/i.test(textOf(element)))
                    .filter(element => matcher.test(describe(element))
                        || Array.from(element.querySelectorAll('img, svg'))
                            .some(icon => visible(icon) && matcher.test(describe(icon))))
                    .sort((first, second) => {
                        if (!account) {
                            return 0;
                        }

                        return Math.abs(centerY(first) - centerY(account))
                            - Math.abs(centerY(second) - centerY(account));
                    })[0];
                if (matchingAction) {
                    return true;
                }

                const cart = Array.from(header.querySelectorAll('button, a'))
                    .filter(visible)
                    .find(element => /cart/i.test(describe(element))
                        || Array.from(element.querySelectorAll('img, svg'))
                            .some(icon => visible(icon) && /cart/i.test(describe(icon))));
                if (!account || !cart || !/^(orders|inbox)$/.test(fallbackActionName)) {
                    return false;
                }

                const topRowIconsBeforeCart = Array.from(header.querySelectorAll('button, a'))
                    .filter(visible)
                    .filter(element => element !== account && element !== cart)
                    .filter(element => element.querySelector('img, svg'))
                    .filter(element => Math.abs(centerY(element) - centerY(account)) <= 24)
                    .filter(element => centerX(element) > account.getBoundingClientRect().right - 2
                        && centerX(element) < cart.getBoundingClientRect().left + 2);

                return fallbackActionName === 'orders'
                    ? topRowIconsBeforeCart.length >= 1
                    : topRowIconsBeforeCart.length >= 2;
                """, labelPattern, fallbackActionName));
    }

    private boolean isAuthenticatedAccountHeaderDisplayed() {
        return Boolean.TRUE.equals(executeScript("""
                const visible = element => {
                    if (!element) {
                        return false;
                    }

                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                const describe = element => [
                    normalize(element.innerText || element.textContent || ''),
                    element.getAttribute('aria-label'),
                    element.getAttribute('title'),
                    element.getAttribute('href'),
                    element.getAttribute('alt'),
                    element.getAttribute('src'),
                    element.getAttribute('class')
                ].filter(Boolean).join(' ');
                const header = document.querySelector('header');
                if (!visible(header)) {
                    return false;
                }

                return Array.from(header.querySelectorAll('button, a'))
                    .filter(visible)
                    .some(element => {
                        const text = normalize(element.innerText || element.textContent || '');
                        const descriptor = describe(element);
                        const hasAccountLabel = /\\baccount\\b/i.test(text + ' ' + descriptor);
                        const hasLoginLabel = /\\b(login|sign\\s*in)\\b/i.test(text + ' ' + descriptor)
                            || /login\\s*icon/i.test(descriptor);
                        if (!hasAccountLabel || hasLoginLabel) {
                            return false;
                        }

                        const hasProfileImage = Array.from(element.querySelectorAll('img'))
                            .filter(visible)
                            .some(image => {
                                const imageDescriptor = describe(image);
                                const imageAlt = normalize(image.getAttribute('alt') || '');
                                return /rounded-full|profile|avatar/i.test(imageDescriptor)
                                    || Boolean(imageAlt)
                                        && !/login\\s*icon|location|cart|order|inbox|search|icon/i.test(imageAlt);
                            });

                        return hasProfileImage || /৳\\s*\\d+|wallet|balance/i.test(text);
                    });
                """));
    }

    private boolean isLoggedOutHeaderState() {
        return Boolean.TRUE.equals(executeScript("""
                const visible = element => {
                    if (!element) {
                        return false;
                    }
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const textOf = element => String(element.innerText || element.textContent || '')
                    .replace(/\\s+/g, ' ')
                    .trim();
                const describe = element => [
                    textOf(element),
                    element.getAttribute('aria-label'),
                    element.getAttribute('title'),
                    element.getAttribute('href'),
                    element.getAttribute('alt'),
                    element.getAttribute('src'),
                    element.getAttribute('class'),
                    element.getAttribute('data-testid')
                ].filter(Boolean).join(' ');
                const header = document.querySelector('header');
                if (!visible(header)) {
                    return false;
                }

                const authenticatedAvatar = Array.from(header.querySelectorAll('button'))
                    .filter(visible)
                    .some(button => Array.from(button.querySelectorAll('img'))
                        .some(image => visible(image)
                            && /rounded-full/.test(String(image.getAttribute('class') || ''))
                            && String(image.getAttribute('alt') || '').trim().length > 0));
                if (authenticatedAvatar) {
                    return false;
                }

                const headerActions = Array.from(header.querySelectorAll('button, a')).filter(visible);
                const explicitLoginAction = headerActions.some(element =>
                    /login|sign\\s*in|account|profile|user/i.test(describe(element))
                    || Array.from(element.querySelectorAll('img, svg'))
                        .some(icon => visible(icon) && /login|account|profile|user|person/i.test(describe(icon))));
                if (explicitLoginAction) {
                    return true;
                }

                const hasSearch = Array.from(header.querySelectorAll('input'))
                    .some(input => /search/i.test(describe(input)));
                const hasCart = headerActions.some(element =>
                    /cart/i.test(describe(element))
                    || Array.from(element.querySelectorAll('img, svg'))
                        .some(icon => visible(icon) && /cart/i.test(describe(icon))));
                const iconActions = headerActions
                    .filter(element => !/order\\s*by/i.test(textOf(element)))
                    .filter(element => element.querySelector('img, svg'));

                return hasSearch && hasCart && iconActions.length >= 3;
                """));
    }

    private String resolveApplicationBaseUrl() {
        String currentUrl = getCurrentUrl();
        for (String route : List.of("/search", "/product/", "/account", "/cart", "/checkout", "/orders")) {
            int routeIndex = currentUrl.indexOf(route);
            if (routeIndex > -1) {
                return currentUrl.substring(0, routeIndex);
            }
        }

        int queryIndex = currentUrl.indexOf('?');
        String withoutQuery = queryIndex > -1 ? currentUrl.substring(0, queryIndex) : currentUrl;
        return withoutQuery.endsWith("/")
                ? withoutQuery.substring(0, withoutQuery.length() - 1)
                : withoutQuery;
    }
}
