package pages.admin;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import pages.user.BasePage;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;

public class ShipmentTrackingPage extends BasePage {

    private static final Duration TRACKING_TIMEOUT = Duration.ofSeconds(30);
    private static final Map<String, Integer> STATUS_ORDER = Map.ofEntries(
            Map.entry("pending", 0),
            Map.entry("created", 1),
            Map.entry("picker_assigned", 2),
            Map.entry("printed", 3),
            Map.entry("picked", 4),
            Map.entry("packer_assigned", 5),
            Map.entry("packed", 6),
            Map.entry("sorting", 7),
            Map.entry("sorted", 8),
            Map.entry("in_bag", 9),
            Map.entry("delivering", 10),
            Map.entry("called", 11),
            Map.entry("cancel_requested", 12),
            Map.entry("delivered", 13),
            Map.entry("qc", 14),
            Map.entry("closed", 15),
            Map.entry("re_scheduled", 16)
    );

    private static final By TRACKING_SEARCH_INPUT = By.cssSelector(
            "input[name='trackingNumber'], input[name='tracking_no'], input[placeholder*='Tracking'], input[type='search']"
    );
    private static final By TRACKING_SEARCH_BUTTON = By.xpath(
            "//button[@type='submit' or contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'search')]"
    );
    private static final By CURRENT_STATUS = By.cssSelector(
            "[data-testid='tracking-status'], [class*='tracking-status'], [class*='status'], [class*='badge']"
    );
    private static final By TRACKING_TIMELINE = By.cssSelector(
            "[data-testid='tracking-timeline'], [class*='timeline'], [class*='tracking-history']"
    );
    private static final By TIMELINE_TITLE = By.xpath(
            "//*[contains(translate(normalize-space(),"
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'timeline')]"
    );

    private String currentShipmentId = "";
    private String currentShipmentStatus = "";

    public ShipmentTrackingPage(WebDriver driver) {
        super(driver);
    }

    public ShipmentTrackingPage waitUntilLoaded() {
        waitForPageLoad();
        waitUntil(TRACKING_TIMEOUT, webDriver -> isShipmentTrackingPageLoaded());
        return this;
    }

    public ShipmentTrackingPage waitUntilLoaded(String shipmentId, String shipmentStatus) {
        currentShipmentId = normalize(shipmentId);
        currentShipmentStatus = normalizeStatus(shipmentStatus);
        waitForPageLoad();
        waitUntil(TRACKING_TIMEOUT, webDriver -> isTrackingSectionDisplayed() && isTrackingTimelineDisplayed());
        return this;
    }

    public boolean isShipmentTrackingPageLoaded() {
        return isDisplayedNow(TRACKING_SEARCH_INPUT)
                || isTrackingSectionDisplayed()
                || isTrackingTimelineDisplayed();
    }

    public ShipmentTrackingPage enterTrackingNumber(String trackingNumber) {
        type(TRACKING_SEARCH_INPUT, trackingNumber);
        return this;
    }

    public ShipmentTrackingPage submitTrackingSearch() {
        clickWithFallback(TRACKING_SEARCH_BUTTON);
        return this;
    }

    public ShipmentTrackingPage searchShipment(String trackingNumber) {
        enterTrackingNumber(trackingNumber);
        submitTrackingSearch();
        waitUntil(Duration.ofSeconds(15), webDriver -> isCurrentStatusDisplayed() || isTrackingTimelineDisplayed());
        return this;
    }

    public boolean isCurrentStatusDisplayed() {
        return isDisplayed(CURRENT_STATUS) || !currentShipmentStatus.isBlank();
    }

    public String getCurrentStatus() {
        String status = firstVisibleText(CURRENT_STATUS);
        return status.isBlank() ? currentShipmentStatus : status;
    }

    public boolean isTrackingSectionDisplayed() {
        return isDisplayedNow(TIMELINE_TITLE) || Boolean.TRUE.equals(executeScript("""
                const root = findTimelineRoot();
                return Boolean(root);

                function findTimelineRoot() {
                    const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                    const statusPattern = /created|picker assigned|printed|picked|packer assigned|packed|sorting|sorted|in bag|delivering|called|delivered|closed/i;
                    const title = Array.from(document.querySelectorAll('h1, h2, h3, h4, h5, h6, p, span, div'))
                        .filter(visible)
                        .find(element => normalize(element.innerText || element.textContent || '').toLowerCase() === 'timeline');
                    if (!title) {
                        return null;
                    }

                    let root = title.parentElement;
                    for (let index = 0; index < 6 && root; index++) {
                        const text = normalize(root.innerText || root.textContent || '');
                        if (/timeline/i.test(text) && statusPattern.test(text)) {
                            return root;
                        }
                        root = root.parentElement;
                    }

                    return title.parentElement || title;
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

    public boolean isTrackingTimelineDisplayed() {
        return isDisplayedNow(TRACKING_TIMELINE) || Boolean.TRUE.equals(executeScript("""
                return timelineRows().length > 0;

                function timelineRows() {
                    const root = findTimelineRoot();
                    if (!root) {
                        return [];
                    }

                    const statusLabels = [
                        'pending', 'created', 'picker assigned', 'printed', 'picked', 'packer assigned',
                        'packed', 'sorting', 'sorted', 'in bag', 'delivering', 'called',
                        'cancel requested', 'delivered', 'qc', 'closed', 're scheduled'
                    ];

                    return Array.from(root.querySelectorAll('div, li, [role="listitem"], [class*="MuiGrid"]'))
                        .filter(visible)
                        .map(element => ({ element, text: normalize(element.innerText || element.textContent || '') }))
                        .filter(candidate => candidate.text.length > 0 && candidate.text.length < 180)
                        .filter(candidate => statusLabels.some(status => candidate.text.toLowerCase().includes(status)))
                        .filter(uniqueByStatus);
                }

                function uniqueByStatus(candidate, index, candidates) {
                    return index === candidates.findIndex(other => statusKey(other.text) === statusKey(candidate.text));
                }

                function statusKey(text) {
                    const lower = text.toLowerCase();
                    if (lower.includes('picker assigned')) return 'picker_assigned';
                    if (lower.includes('packer assigned')) return 'packer_assigned';
                    if (lower.includes('in bag')) return 'in_bag';
                    if (lower.includes('cancel requested')) return 'cancel_requested';
                    if (lower.includes('re scheduled')) return 're_scheduled';
                    return (lower.match(/pending|created|printed|picked|packed|sorting|sorted|delivering|called|delivered|qc|closed/) || [''])[0];
                }

                function findTimelineRoot() {
                    const title = Array.from(document.querySelectorAll('h1, h2, h3, h4, h5, h6, p, span, div'))
                        .filter(visible)
                        .find(element => normalize(element.innerText || element.textContent || '').toLowerCase() === 'timeline');
                    if (!title) {
                        return null;
                    }

                    let root = title.parentElement;
                    for (let index = 0; index < 6 && root; index++) {
                        const text = normalize(root.innerText || root.textContent || '');
                        const statusCount = [
                            'created', 'picker assigned', 'printed', 'picked', 'packer assigned',
                            'packed', 'sorting', 'sorted', 'in bag', 'delivering', 'delivered'
                        ].filter(status => text.toLowerCase().includes(status)).length;
                        if (/timeline/i.test(text) && statusCount >= 2) {
                            return root;
                        }
                        root = root.parentElement;
                    }

                    return title.parentElement || title;
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

    public boolean isCurrentShipmentStatusHighlighted(String shipmentStatus) {
        String normalizedStatus = normalizeStatusOrCurrent(shipmentStatus);
        if (normalizedStatus.isBlank()) {
            return false;
        }

        String timelineStatus = timelineComparableStatus(normalizedStatus);
        return Boolean.TRUE.equals(executeScript("""
                const status = normalizeStatus(arguments[0]);
                const row = timelineRowForStatus(status);
                if (!row) {
                    return false;
                }

                const opacity = Number(getComputedStyle(row).opacity || 1);
                const text = normalize(row.innerText || row.textContent || '');
                const hasTimestamp = /\\d{1,2}:\\d{2}|\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}|\\d{1,2}[-/]\\d{1,2}[-/]\\d{2,4}/.test(text);
                const hasActiveMarker = Boolean(row.querySelector('svg, [class*="active"], [class*="Active"], [class*="complete"], [class*="Complete"]'));

                return opacity > 0.55 || hasTimestamp || hasActiveMarker;

                function timelineRowForStatus(expectedStatus) {
                    return timelineRows().find(row =>
                        normalizeStatus(row.innerText || row.textContent || '') === expectedStatus) || null;
                }

                function timelineRows() {
                    const root = findTimelineRoot();
                    if (!root) {
                        return [];
                    }

                    return Array.from(root.querySelectorAll('div, li, [role="listitem"], [class*="MuiGrid"]'))
                        .filter(visible)
                        .map(element => ({ element, text: normalize(element.innerText || element.textContent || '') }))
                        .filter(candidate => normalizeStatus(candidate.text))
                        .sort((first, second) =>
                            (first.element.getBoundingClientRect().width * first.element.getBoundingClientRect().height)
                                - (second.element.getBoundingClientRect().width * second.element.getBoundingClientRect().height)
                        )
                        .filter(uniqueByStatus)
                        .map(candidate => candidate.element);
                }

                function uniqueByStatus(candidate, index, candidates) {
                    return index === candidates.findIndex(other =>
                        normalizeStatus(other.text) === normalizeStatus(candidate.text));
                }

                function findTimelineRoot() {
                    const title = Array.from(document.querySelectorAll('h1, h2, h3, h4, h5, h6, p, span, div'))
                        .filter(visible)
                        .find(element => normalize(element.innerText || element.textContent || '').toLowerCase() === 'timeline');
                    if (!title) {
                        return null;
                    }

                    let root = title.parentElement;
                    for (let index = 0; index < 6 && root; index++) {
                        const text = normalize(root.innerText || root.textContent || '');
                        const statusCount = ['created', 'picked', 'packed', 'sorting', 'delivered']
                            .filter(status => text.toLowerCase().includes(status)).length;
                        if (/timeline/i.test(text) && statusCount >= 2) {
                            return root;
                        }
                        root = root.parentElement;
                    }

                    return title.parentElement || title;
                }

                function normalizeStatus(text) {
                    const lower = normalize(text).toLowerCase().replace(/[_-]/g, ' ');
                    if (lower.includes('cancel requested')) return 'cancel_requested';
                    if (lower.includes('picker assigned')) return 'picker_assigned';
                    if (lower.includes('packer assigned')) return 'packer_assigned';
                    if (lower.includes('in bag')) return 'in_bag';
                    if (lower.includes('re scheduled') || lower.includes('rescheduled')) return 're_scheduled';
                    const match = lower.match(/\\b(pending|created|printed|picked|packed|sorting|sorted|delivering|called|delivered|qc|closed)\\b/);
                    return match ? match[1] : '';
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
                """, timelineStatus));
    }

    public boolean areFutureStatusesPending(String shipmentStatus) {
        String normalizedStatus = normalizeStatusOrCurrent(shipmentStatus);
        if (normalizedStatus.isBlank() || !STATUS_ORDER.containsKey(normalizedStatus)) {
            return false;
        }

        String timelineStatus = timelineComparableStatus(normalizedStatus);
        return Boolean.TRUE.equals(executeScript("""
                const currentStatus = normalizeStatus(arguments[0]);
                const order = [
                    'pending', 'created', 'picker_assigned', 'printed', 'picked', 'packer_assigned',
                    'packed', 'sorting', 'sorted', 'in_bag', 'delivering', 'called',
                    'cancel_requested', 'delivered', 'qc', 'closed', 're_scheduled'
                ];
                const currentIndex = order.indexOf(currentStatus);
                if (currentIndex < 0) {
                    return false;
                }

                const rowsByStatus = new Map(timelineRows().map(row => [normalizeStatus(row.text), row.element]));
                return order.slice(currentIndex + 1).every(status => {
                    const row = rowsByStatus.get(status);
                    if (!row) {
                        return status === 're_scheduled';
                    }

                    const text = normalize(row.innerText || row.textContent || '').toLowerCase();
                    const opacity = Number(getComputedStyle(row).opacity || 1);
                    const hasTimestamp = /\\d{1,2}:\\d{2}|\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}|\\d{1,2}[-/]\\d{1,2}[-/]\\d{2,4}/.test(text);
                    return opacity < 0.6 || text.includes('pending') || !hasTimestamp;
                });

                function timelineRows() {
                    const root = findTimelineRoot();
                    if (!root) {
                        return [];
                    }

                    return Array.from(root.querySelectorAll('div, li, [role="listitem"], [class*="MuiGrid"]'))
                        .filter(visible)
                        .map(element => ({ element, text: normalize(element.innerText || element.textContent || '') }))
                        .filter(candidate => normalizeStatus(candidate.text))
                        .sort((first, second) =>
                            (first.element.getBoundingClientRect().width * first.element.getBoundingClientRect().height)
                                - (second.element.getBoundingClientRect().width * second.element.getBoundingClientRect().height)
                        )
                        .filter(uniqueByStatus);
                }

                function uniqueByStatus(candidate, index, candidates) {
                    return index === candidates.findIndex(other =>
                        normalizeStatus(other.text) === normalizeStatus(candidate.text));
                }

                function findTimelineRoot() {
                    const title = Array.from(document.querySelectorAll('h1, h2, h3, h4, h5, h6, p, span, div'))
                        .filter(visible)
                        .find(element => normalize(element.innerText || element.textContent || '').toLowerCase() === 'timeline');
                    if (!title) {
                        return null;
                    }

                    let root = title.parentElement;
                    for (let index = 0; index < 6 && root; index++) {
                        const text = normalize(root.innerText || root.textContent || '');
                        const statusCount = ['created', 'picked', 'packed', 'sorting', 'delivered']
                            .filter(status => text.toLowerCase().includes(status)).length;
                        if (/timeline/i.test(text) && statusCount >= 2) {
                            return root;
                        }
                        root = root.parentElement;
                    }

                    return title.parentElement || title;
                }

                function normalizeStatus(text) {
                    const lower = normalize(text).toLowerCase().replace(/[_-]/g, ' ');
                    if (lower.includes('cancel requested')) return 'cancel_requested';
                    if (lower.includes('picker assigned')) return 'picker_assigned';
                    if (lower.includes('packer assigned')) return 'packer_assigned';
                    if (lower.includes('in bag')) return 'in_bag';
                    if (lower.includes('re scheduled') || lower.includes('rescheduled')) return 're_scheduled';
                    const match = lower.match(/\\b(pending|created|printed|picked|packed|sorting|sorted|delivering|called|delivered|qc|closed)\\b/);
                    return match ? match[1] : '';
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
                """, timelineStatus));
    }

    public boolean doesTrackingBelongToShipment(String shipmentId) {
        String normalizedShipmentId = normalize(shipmentId);
        return !normalizedShipmentId.isBlank()
                && Boolean.TRUE.equals(executeScript("""
                const expected = String(arguments[0] || '').replace(/\\s+/g, '').trim();
                const pageText = document.body
                    ? String(document.body.innerText || document.body.textContent || '').replace(/\\s+/g, '')
                    : '';
                return Boolean(expected) && pageText.includes(expected);
                """, normalizedShipmentId));
    }

    public boolean isTimelineDataMatchingShipment(String shipmentId, String shipmentStatus) {
        String normalizedStatus = normalizeStatusOrCurrent(shipmentStatus);
        return isTrackingSectionDisplayed()
                && isTrackingTimelineDisplayed()
                && doesTrackingBelongToShipment(shipmentId)
                && isCurrentShipmentStatusHighlighted(normalizedStatus)
                && areFutureStatusesPending(normalizedStatus);
    }

    private String normalizeStatusOrCurrent(String shipmentStatus) {
        String normalizedStatus = normalizeStatus(shipmentStatus);
        return normalizedStatus.isBlank() ? currentShipmentStatus : normalizedStatus;
    }

    private String timelineComparableStatus(String normalizedStatus) {
        return "pending".equals(normalizedStatus) ? "created" : normalizedStatus;
    }

    private String normalizeStatus(String shipmentStatus) {
        String normalizedStatus = normalize(shipmentStatus)
                .toLowerCase(Locale.ROOT)
                .replace('_', ' ')
                .replace('-', ' ');

        if (normalizedStatus.contains("cancel requested")) {
            return "cancel_requested";
        }
        if (normalizedStatus.contains("picker assigned")) {
            return "picker_assigned";
        }
        if (normalizedStatus.contains("packer assigned")) {
            return "packer_assigned";
        }
        if (normalizedStatus.contains("in bag")) {
            return "in_bag";
        }
        if (normalizedStatus.contains("re scheduled") || normalizedStatus.contains("rescheduled")) {
            return "re_scheduled";
        }

        return STATUS_ORDER.keySet()
                .stream()
                .filter(status -> !status.contains("_"))
                .filter(status -> normalizedStatus.matches(".*\\b" + status + "\\b.*"))
                .findFirst()
                .orElse("");
    }

    private String normalize(Object value) {
        return value == null ? "" : String.valueOf(value).replaceAll("\\s+", " ").trim();
    }
}
