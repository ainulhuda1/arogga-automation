package pages.user;

import org.openqa.selenium.WebDriver;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.regex.Pattern;

public class PaymentdetailsPage extends BasePage {

    private static final Duration PAYMENT_TIMEOUT = Duration.ofSeconds(20);

    public PaymentdetailsPage(WebDriver driver) {
        super(driver);
    }

    public PaymentdetailsPage waitUntilDisplayed() {
        waitUntil(PAYMENT_TIMEOUT, webDriver -> isPaymentDetailsSectionDisplayed());
        return this;
    }

    public boolean waitUntilDisplayedIfPresent() {
        try {
            waitUntil(Duration.ofSeconds(5), webDriver -> isPaymentDetailsSectionDisplayed());
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public boolean isPaymentDetailsSectionDisplayed() {
        String text = getSurfaceText();

        return Pattern.compile("(?is)Payment\\s+Details|Payment\\s+Method|Payment\\s+Status")
                .matcher(text)
                .find();
    }

    public boolean isSelectedPaymentMethodDisplayedCorrectly() {
        return isCashOnDeliverySelected() || isAroggaCashPaymentDisplayed();
    }

    public boolean isCashOnDeliverySelected() {
        return Pattern.compile("(?i)(Cash\\s+on\\s+Delivery|\\bCOD\\b)")
                .matcher(getSurfaceText())
                .find();
    }

    public boolean isAroggaCashPaymentDisplayed() {
        return Pattern.compile("(?is)(Arogga\\s+Cash|Arogga\\s+cash\\s+applied|Wallet|Paid|৳\\s*0)")
                .matcher(getSurfaceText())
                .find();
    }

    public boolean isPaymentStatusPendingForCod() {
        String text = getSurfaceText();

        return isCashOnDeliverySelected()
                && (Pattern.compile("(?is)Payment\\s+Status.*(?:Pending|Unpaid|Not\\s+Paid|Due)"
                + "|(?:Pending|Unpaid|Not\\s+Paid|Due).*Payment\\s+Status"
                + "|\\b(?:Pending|Unpaid|Not\\s+Paid|Due)\\b")
                .matcher(text)
                .find()
                || !Pattern.compile("(?is)Payment\\s+Status|Status").matcher(text).find());
    }

    public boolean isAmountPayableMatching(BigDecimal expectedAmountPayable) {
        return containsCurrencyAmount(getSurfaceText(), expectedAmountPayable);
    }

    public boolean isOrderIdMatching(String expectedOrderId) {
        String normalizedExpectedOrderId = normalizeText(expectedOrderId);

        return !normalizedExpectedOrderId.isBlank()
                && normalizeText(getSurfaceText()).contains(normalizedExpectedOrderId);
    }

    public boolean isPaymentInformationProperlyAligned() {
        return Boolean.TRUE.equals(executeScript("""
                const surface = findPaymentSurface();
                if (!surface) {
                    return false;
                }

                const text = normalize(surface.innerText || surface.textContent || '');
                if (!/Payment/i.test(text) || !/(Cash\\s+on\\s+Delivery|\\bCOD\\b|Arogga\\s+Cash|Wallet|Paid|৳\\s*0)/i.test(text)) {
                    return false;
                }

                const leafTextElements = Array.from(surface.querySelectorAll('*'))
                    .filter(visible)
                    .filter(element => {
                        const value = normalize(element.innerText || element.textContent || '');
                        return value.length > 0 && element.children.length === 0;
                    });

                return leafTextElements.every(element =>
                    element.scrollWidth <= element.clientWidth + 4
                        && element.scrollHeight <= element.clientHeight + 6
                );

                function findPaymentSurface() {
                    const candidates = Array.from(document.querySelectorAll('main, section, article, aside, [role="dialog"], body > div'))
                        .filter(visible)
                        .filter(element => /Payment\\s+(Details|Method|Status)|Cash\\s+on\\s+Delivery|\\bCOD\\b/i.test(
                            normalize(element.innerText || element.textContent || '')
                        ))
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        );

                    return candidates[0]?.element || null;
                }

                function normalize(text) {
                    return String(text || '').replace(/\\s+/g, ' ').trim();
                }

                function visible(element) {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                }
                """));
    }

    public boolean hasNoBrokenIcons() {
        return Boolean.TRUE.equals(executeScript("""
                const surface = findPaymentSurface() || document.body;
                const visibleSvgs = Array.from(surface.querySelectorAll('svg')).filter(visible);
                const visibleIconImages = Array.from(surface.querySelectorAll('img'))
                    .filter(visible)
                    .filter(image => /icon|payment|cash|cod|order|status/i.test(
                        `${image.alt || ''} ${image.src || ''} ${image.currentSrc || ''}`
                    ));

                return visibleSvgs.every(svg => {
                    const rect = svg.getBoundingClientRect();
                    return rect.width > 0 && rect.height > 0;
                }) && visibleIconImages.every(image => image.complete === true
                    && image.naturalWidth > 0
                    && image.naturalHeight > 0);

                function findPaymentSurface() {
                    const candidates = Array.from(document.querySelectorAll('main, section, article, aside, [role="dialog"], body > div'))
                        .filter(visible)
                        .filter(element => /Payment\\s+(Details|Method|Status)|Cash\\s+on\\s+Delivery|\\bCOD\\b/i.test(
                            normalize(element.innerText || element.textContent || '')
                        ))
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        );

                    return candidates[0]?.element || null;
                }

                function normalize(text) {
                    return String(text || '').replace(/\\s+/g, ' ').trim();
                }

                function visible(element) {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                }
                """));
    }

    public boolean hasNoUiIssues() {
        return isPaymentInformationProperlyAligned()
                && hasNoBrokenIcons();
    }

    private String getSurfaceText() {
        Object result = executeScript("""
                const surface = findPaymentSurface() || document.body;
                return surface ? normalize(surface.innerText || surface.textContent || '') : '';

                function findPaymentSurface() {
                    const candidates = Array.from(document.querySelectorAll('main, section, article, aside, [role="dialog"], body > div'))
                        .filter(visible)
                        .filter(element => /Payment\\s+(Details|Method|Status)|Cash\\s+on\\s+Delivery|\\bCOD\\b/i.test(
                            normalize(element.innerText || element.textContent || '')
                        ))
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        );

                    return candidates[0]?.element || null;
                }

                function normalize(text) {
                    return String(text || '').replace(/\\s+/g, ' ').trim();
                }

                function visible(element) {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                }
                """);

        return result == null ? "" : String.valueOf(result);
    }

    private boolean containsCurrencyAmount(String text, BigDecimal expectedAmount) {
        if (expectedAmount == null) {
            return false;
        }

        String normalizedText = normalizeCurrencyText(text);
        String roundedAmount = expectedAmount.setScale(0, RoundingMode.HALF_UP).toPlainString();
        String decimalAmount = expectedAmount.setScale(2, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();

        return Pattern.compile("৳\\s*" + Pattern.quote(roundedAmount) + "\\b")
                .matcher(normalizedText)
                .find()
                || Pattern.compile("৳\\s*" + Pattern.quote(decimalAmount) + "\\b")
                .matcher(normalizedText)
                .find();
    }

    private String normalizeCurrencyText(String text) {
        return text == null ? "" : text
                .replaceAll("\\s+", " ")
                .replace("৳ ", "৳")
                .trim();
    }

    private String normalizeText(String text) {
        return text == null ? "" : text.replaceAll("\\s+", " ").trim();
    }
}
