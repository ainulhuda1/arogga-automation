package pages.user;

import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class ShippingAddressPage extends BasePage {

    private static final Duration ADDRESS_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration SHORT_ADDRESS_TIMEOUT = Duration.ofSeconds(6);

    public ShippingAddressPage(WebDriver driver) {
        super(driver);
    }

    public ShippingAddressPage waitUntilAddressListOpen() {
        waitUntil(ADDRESS_TIMEOUT, webDriver -> isShippingAddressPageOpen());

        try {
            waitUntil(SHORT_ADDRESS_TIMEOUT, webDriver -> getSavedAddressCount() > 0);
        } catch (TimeoutException ignored) {
            // Some accounts can legitimately have no saved address; assertions decide whether that is acceptable.
        }

        return this;
    }

    public boolean isShippingAddressPageOpen() {
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
                const modal = activeAddressListModal();

                function activeAddressListModal() {
                    return Array.from(document.querySelectorAll('body > div, [role="dialog"]'))
                        .filter(visible)
                        .filter(element => {
                            const text = normalize(element.innerText || element.textContent || '');
                            return /^Address\\b/i.test(text)
                                && /Add\\s+New\\s+Address/i.test(text)
                                && !/Add\\s+Shipping\\s+Address|Update\\s+Shipping\\s+Address/i.test(text);
                        })
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        )
                        .map(candidate => candidate.element)[0] || null;
                }

                return Boolean(modal);
                """));
    }

    public boolean isPageLoadedWithoutUiIssues() {
        return getAddressModalUiIssues().isEmpty();
    }

    public int getSavedAddressCount() {
        Object result = executeScript("""
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                const modal = activeAddressListModal();
                if (!modal) {
                    return 0;
                }

                return Array.from(modal.querySelectorAll('input[type="radio"]')).length;

                function activeAddressListModal() {
                    return Array.from(document.querySelectorAll('body > div, [role="dialog"]'))
                        .filter(visible)
                        .filter(element => {
                            const text = normalize(element.innerText || element.textContent || '');
                            return /^Address\\b/i.test(text)
                                && /Add\\s+New\\s+Address/i.test(text)
                                && !/Add\\s+Shipping\\s+Address|Update\\s+Shipping\\s+Address/i.test(text);
                        })
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        )
                        .map(candidate => candidate.element)[0] || null;
                }

                """);

        return result instanceof Number number ? number.intValue() : 0;
    }

    public boolean areSavedAddressDetailsVisible() {
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
                const modal = activeAddressListModal();
                if (!modal) {
                    return false;
                }

                const text = normalize(modal.innerText || modal.textContent || '');
                return Array.from(modal.querySelectorAll('input[type="radio"]')).length > 0
                    && /\\b(Home|Office|Hometown)\\s+Address\\b/i.test(text)
                    && /(?:\\+?88\\s*)?0?1\\d{9}/.test(text);

                function activeAddressListModal() {
                    return Array.from(document.querySelectorAll('body > div, [role="dialog"]'))
                        .filter(visible)
                        .filter(element => {
                            const text = normalize(element.innerText || element.textContent || '');
                            return /^Address\\b/i.test(text)
                                && /Add\\s+New\\s+Address/i.test(text)
                                && !/Add\\s+Shipping\\s+Address|Update\\s+Shipping\\s+Address/i.test(text);
                        })
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        )
                        .map(candidate => candidate.element)[0] || null;
                }

                """));
    }

    public boolean isDefaultAddressMarkedCorrectly() {
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
                const modal = activeAddressListModal();
                if (!modal) {
                    return false;
                }

                    return /Shipping\\s+Address/i.test(normalize(modal.innerText || modal.textContent || ''))
                    || Array.from(modal.querySelectorAll('input[type="radio"]'))
                        .some(radio => radio.checked);

                function activeAddressListModal() {
                    return Array.from(document.querySelectorAll('body > div, [role="dialog"]'))
                        .filter(visible)
                        .filter(element => {
                            const text = normalize(element.innerText || element.textContent || '');
                            return /^Address\\b/i.test(text)
                                && /Add\\s+New\\s+Address/i.test(text)
                                && !/Add\\s+Shipping\\s+Address|Update\\s+Shipping\\s+Address/i.test(text);
                        })
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        )
                        .map(candidate => candidate.element)[0] || null;
                }

                """));
    }

    public boolean hasNoConflictingDefaultAddressMarkers() {
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
                const modal = activeAddressListModal();
                if (!modal) {
                    return false;
                }

                const checkedRadioCount = Array.from(modal.querySelectorAll('input[type="radio"]'))
                    .filter(radio => radio.checked)
                    .length;
                const shippingMarkerCount = getAddressCards(modal)
                    .filter(card => /Shipping\\s+Address/i.test(normalize(card.innerText || card.textContent || '')))
                    .length;

                return checkedRadioCount <= 1 && shippingMarkerCount <= 1;

                function activeAddressListModal() {
                    return Array.from(document.querySelectorAll('body > div, [role="dialog"]'))
                        .filter(visible)
                        .filter(element => {
                            const text = normalize(element.innerText || element.textContent || '');
                            return /^Address\\b/i.test(text)
                                && /Add\\s+New\\s+Address/i.test(text)
                                && !/Add\\s+Shipping\\s+Address|Update\\s+Shipping\\s+Address/i.test(text);
                        })
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        )
                        .map(candidate => candidate.element)[0] || null;
                }

                function getAddressCards(modal) {
                    return Array.from(modal.querySelectorAll('div'))
                        .filter(visible)
                        .filter(element => {
                            const text = normalize(element.innerText || element.textContent || '');
                            return /\\bAddress\\b/i.test(text)
                                && /(?:\\+?88\\s*)?0?1\\d{9}/.test(text)
                                && element.querySelector('input[type="radio"]');
                        })
                        .filter(element => !Array.from(element.children).some(child => {
                            const text = normalize(child.innerText || child.textContent || '');
                            return visible(child)
                                && /\\bAddress\\b/i.test(text)
                                && /(?:\\+?88\\s*)?0?1\\d{9}/.test(text)
                                && child.querySelector('input[type="radio"]');
                        }));
                }
                """));
    }

    public boolean isFirstAddressEditControlVisibleAndEnabled() {
        return Boolean.TRUE.equals(executeScript("""
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && style.pointerEvents !== 'none'
                        && Number(style.opacity || 1) !== 0;
                };
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                const modal = activeAddressListModal();
                if (!modal) {
                    return false;
                }

                const action = modal.querySelector('svg.lucide-ellipsis')
                    || Array.from(modal.querySelectorAll('button, div, span'))
                        .filter(visible)
                        .find(element => /edit|more|menu/i.test(
                            normalize(element.innerText || element.textContent || element.getAttribute('aria-label') || '')
                        ));

                return Boolean(action);

                function activeAddressListModal() {
                    return Array.from(document.querySelectorAll('body > div, [role="dialog"]'))
                        .filter(visible)
                        .filter(element => {
                            const text = normalize(element.innerText || element.textContent || '');
                            return /^Address\\b/i.test(text)
                                && /Add\\s+New\\s+Address/i.test(text)
                                && !/Add\\s+Shipping\\s+Address|Update\\s+Shipping\\s+Address/i.test(text);
                        })
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        )
                        .map(candidate => candidate.element)[0] || null;
                }

                function getAddressCards(modal) {
                    return Array.from(modal.querySelectorAll('div'))
                        .filter(visible)
                        .filter(element => {
                            const text = normalize(element.innerText || element.textContent || '');
                            return /\\b(Address)\\b/i.test(text)
                                && /(?:\\+?88\\s*)?0?1\\d{9}/.test(text)
                                && element.querySelector('input[type="radio"]');
                        })
                        .filter(element => !Array.from(element.children).some(child => {
                            const text = normalize(child.innerText || child.textContent || '');
                            return visible(child)
                                && /\\b(Address)\\b/i.test(text)
                                && /(?:\\+?88\\s*)?0?1\\d{9}/.test(text)
                                && child.querySelector('input[type="radio"]');
                        }));
                }
                """));
    }

    public ShippingAddressPage openFirstAddressActionMenu() {
        waitUntil(ADDRESS_TIMEOUT, webDriver -> clickFirstAddressActionMenu());
        waitUntil(ADDRESS_TIMEOUT, webDriver -> areEditDefaultDeleteOptionsVisible());
        return this;
    }

    public ShippingAddressPage openAddressActionMenuByFullName(String fullName) {
        String normalizedFullName = normalizeText(fullName);
        if (normalizedFullName.isBlank()) {
            throw new IllegalArgumentException("Address full name must not be blank.");
        }

        waitUntil(ADDRESS_TIMEOUT, webDriver -> clickAddressActionMenuByFullNamePrefix(normalizedFullName));
        waitUntil(ADDRESS_TIMEOUT, webDriver -> areEditDefaultDeleteOptionsVisible());
        return this;
    }

    public boolean areEditDefaultDeleteOptionsVisible() {
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
                const visibleTexts = Array.from(document.querySelectorAll('button, a, div, span, li'))
                    .filter(visible)
                    .map(element => normalize(element.innerText || element.textContent || ''))
                    .filter(Boolean);

                return visibleTexts.some(text => /^Edit$/i.test(text))
                    && visibleTexts.some(text => /^Make\\s+Default$/i.test(text))
                    && visibleTexts.some(text => /^Delete$/i.test(text));
                """));
    }

    public ShippingAddressPage openFirstAddressEditForm() {
        if (!areEditDefaultDeleteOptionsVisible()) {
            openFirstAddressActionMenu();
        }

        waitUntil(ADDRESS_TIMEOUT, webDriver -> clickVisibleExactText("Edit"));
        waitUntil(ADDRESS_TIMEOUT, webDriver -> isEditFormOpen());
        return this;
    }

    public ShippingAddressPage openAddressEditFormByFullName(String fullName) {
        if (!areEditDefaultDeleteOptionsVisible()) {
            openAddressActionMenuByFullName(fullName);
        }

        waitUntil(ADDRESS_TIMEOUT, webDriver -> clickVisibleExactText("Edit"));
        waitUntil(ADDRESS_TIMEOUT, webDriver -> isEditFormOpen());
        return this;
    }

    public boolean isEditFormOpen() {
        return isFormOpen("Update Shipping Address");
    }

    public boolean isAddAddressFormOpen() {
        return isFormOpen("Add Shipping Address");
    }

    public boolean isEditFormShowingExistingAddress() {
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
                const form = Array.from(document.querySelectorAll('body > div, [role="dialog"]'))
                    .filter(visible)
                    .filter(element => /Update\\s+Shipping\\s+Address/i.test(
                        normalize(element.innerText || element.textContent || '')
                    ))
                    .map(element => ({ element, rect: element.getBoundingClientRect() }))
                    .sort((first, second) =>
                        (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                    )
                    .map(candidate => candidate.element)[0] || null;
                if (!form) {
                    return false;
                }

                const textValues = Array.from(form.querySelectorAll('input, textarea'))
                    .filter(element => element.type !== 'hidden')
                    .filter(visible)
                    .map(element => String(element.value || '').trim())
                    .filter(Boolean);
                const formText = normalize(form.innerText || form.textContent || '');
                const hasSelectedDeliveryArea = /Dhaka|City|Adabor|Address/i.test(formText)
                    && !/Select\\s+delivery\\s+area/i.test(formText);

                return textValues.length >= 2 || (textValues.length >= 1 && hasSelectedDeliveryArea);
                """));
    }

    public ShippingAddressPage updateCurrentAddress(String fullName, String addressLine) {
        fillAddressForm(fullName, null, null, addressLine, null, false);
        submitAddressForm();
        waitForSavedAddressOrRecoverFromLabSyncBlock(new AddressData(fullName, null, addressLine));
        return this;
    }

    public ShippingAddressPage openAddNewAddressForm() {
        waitUntilAddressListOpen();
        waitUntil(ADDRESS_TIMEOUT, webDriver -> clickVisibleExactText("Add New Address"));
        waitUntil(ADDRESS_TIMEOUT, webDriver -> isAddAddressFormOpen());
        return this;
    }

    public boolean areMandatoryFieldsDisplayed() {
        return areFormFieldsDisplayed("Full Name", "Phone Number", "Select Delivery Area", "Address", "Address Type");
    }

    public boolean areMandatoryFieldsEditable() {
        return Boolean.TRUE.equals(executeScript("""
                const form = activeFormModal();
                if (!form) {
                    return false;
                }

                const fullName = fieldControl(form, 'Full Name');
                const phone = fieldControl(form, 'Phone Number');
                const deliveryArea = fieldControl(form, 'Select Delivery Area');
                const address = fieldControl(form, 'Address');
                const home = exactButton(form, 'Home');
                const office = exactButton(form, 'Office');

                return [fullName, phone, deliveryArea, address, home, office].every(element =>
                    Boolean(element) && !element.disabled && getComputedStyle(element).pointerEvents !== 'none'
                );

                function activeFormModal() {
                    const visible = element => {
                        const rect = element.getBoundingClientRect();
                        const style = getComputedStyle(element);
                        return rect.width > 0 && rect.height > 0
                            && style.display !== 'none'
                            && style.visibility !== 'hidden'
                            && Number(style.opacity || 1) !== 0;
                    };
                    const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                    return Array.from(document.querySelectorAll('body > div, [role="dialog"]'))
                        .filter(visible)
                        .filter(element => /(?:Add|Update)\\s+Shipping\\s+Address/i.test(
                            normalize(element.innerText || element.textContent || '')
                        ))
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        )
                        .map(candidate => candidate.element)[0] || null;
                }

                function fieldControl(form, labelText) {
                    const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                    const label = Array.from(form.querySelectorAll('label'))
                        .find(candidate => normalize(candidate.innerText || candidate.textContent)
                            .toLowerCase().startsWith(labelText.toLowerCase()));
                    if (!label) {
                        return null;
                    }

                    let scope = label.parentElement;
                    for (let depth = 0; depth < 4 && scope; depth += 1, scope = scope.parentElement) {
                        const control = scope.querySelector('input, textarea, select, button');
                        if (control) {
                            control.scrollIntoView({ block: 'center', inline: 'nearest' });
                            return control;
                        }
                    }

                    return null;
                }

                function exactButton(form, text) {
                    const normalize = value => String(value || '').replace(/\\s+/g, ' ').trim();
                    return Array.from(form.querySelectorAll('button'))
                        .find(button => normalize(button.innerText || button.textContent).toLowerCase()
                            === text.toLowerCase()) || null;
                }
                """));
    }

    public boolean isHomeAddressTypeAvailable() {
        return isAddressTypeAvailable("Home");
    }

    public boolean isOfficeAddressTypeAvailable() {
        return isAddressTypeAvailable("Office");
    }

    public boolean isSetDefaultAddressOptionAvailable() {
        return Boolean.TRUE.equals(executeScript("""
                const form = activeFormModal();
                if (!form) {
                    return false;
                }

                return Array.from(form.querySelectorAll('label, div, span'))
                    .some(element => /Make\\s+default\\s+address|Set\\s+as\\s+Default/i.test(
                        element.innerText || element.textContent || ''
                    ));

                function activeFormModal() {
                    const visible = element => {
                        const rect = element.getBoundingClientRect();
                        const style = getComputedStyle(element);
                        return rect.width > 0 && rect.height > 0
                            && style.display !== 'none'
                            && style.visibility !== 'hidden'
                            && Number(style.opacity || 1) !== 0;
                    };
                    const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                    return Array.from(document.querySelectorAll('body > div, [role="dialog"]'))
                        .filter(visible)
                        .filter(element => /(?:Add|Update)\\s+Shipping\\s+Address/i.test(
                            normalize(element.innerText || element.textContent || '')
                        ))
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        )
                        .map(candidate => candidate.element)[0] || null;
                }
                """));
    }

    public ShippingAddressPage saveNewHomeDefaultAddress(AddressData addressData) {
        fillAddressForm(
                addressData.fullName(),
                addressData.phoneNumber(),
                new String[]{"Dhaka", "Dhaka City", "Adabor"},
                addressData.addressLine(),
                "Home",
                true
        );
        selectSubArea("Adabor 10");
        submitAddressForm();
        waitForSavedAddressOrRecoverFromLabSyncBlock(addressData);
        return this;
    }

    public ShippingAddressPage deleteAddressesByFullNamePrefix(String fullNamePrefix) {
        String normalizedPrefix = normalizeText(fullNamePrefix);
        if (normalizedPrefix.isBlank()) {
            return this;
        }

        waitUntilAddressListOpen();
        int safetyCounter = 0;

        while (safetyCounter < 10 && isAddressWithFullNamePrefixDisplayed(normalizedPrefix)) {
            int countBeforeDelete = getSavedAddressCount();

            waitUntil(ADDRESS_TIMEOUT, webDriver -> clickAddressActionMenuByFullNamePrefix(normalizedPrefix));
            waitUntil(ADDRESS_TIMEOUT, webDriver -> areEditDefaultDeleteOptionsVisible());
            waitUntil(ADDRESS_TIMEOUT, webDriver -> clickVisibleExactText("Delete"));
            confirmAddressDeletionIfPrompted();
            waitUntil(ADDRESS_TIMEOUT, webDriver -> getSavedAddressCount() < countBeforeDelete
                    || !isAddressWithFullNamePrefixDisplayed(normalizedPrefix));
            safetyCounter++;
        }

        return this;
    }

    public boolean deleteAddressesByFullNamePrefixUsingApi(String fullNamePrefix) {
        String normalizedPrefix = normalizeText(fullNamePrefix);
        if (normalizedPrefix.isBlank()) {
            return false;
        }

        Object result = executeAsyncScript("""
                const done = arguments[arguments.length - 1];
                const expectedPrefix = String(arguments[0] || '').trim().toLowerCase();
                const token = findAuthToken();
                const apiBaseUrl = resolveApiBaseUrl();
                const query = new URLSearchParams({
                    _page: '1',
                    _perPage: '100',
                    _return_total: '1',
                    _reload: 'false',
                    f: /Mobi|Android/i.test(navigator.userAgent) ? 'mweb' : 'web',
                    b: detectBrowserName(),
                    v: detectBrowserVersion(),
                    os: detectOsName(),
                    osv: detectOsVersion()
                });

                if (!expectedPrefix || !token) {
                    done(false);
                    return;
                }

                listAddresses()
                    .then(addresses => addresses.filter(address =>
                        normalize(address.ul_name || address.name || address.fullName || '')
                            .startsWith(expectedPrefix)
                    ))
                    .then(async addresses => {
                        for (const address of addresses) {
                            const id = address.ul_id || address.id || address.userLocationId || address.user_location_id;
                            if (!id) {
                                done(false);
                                return;
                            }

                            const response = await fetch(`${apiBaseUrl}/userLocation/v1/${encodeURIComponent(id)}?${clientQuery()}`, {
                                method: 'DELETE',
                                headers: { Authorization: `Bearer ${token}` }
                            });
                            const text = await response.text();
                            if (!response.ok || !/"status"\\s*:\\s*"success"|deleted|removed|success/i.test(text)) {
                                done(false);
                                return;
                            }
                        }

                        const remaining = await listAddresses();
                        done(!remaining.some(address =>
                            normalize(address.ul_name || address.name || address.fullName || '')
                                .startsWith(expectedPrefix)
                        ));
                    })
                    .catch(() => done(false));

                function listAddresses() {
                    return fetch(`${apiBaseUrl}/userLocation/v1?${query}`, {
                        method: 'GET',
                        headers: { Authorization: `Bearer ${token}` }
                    })
                        .then(response => response.ok ? response.json() : Promise.reject(new Error('Address list failed')))
                        .then(payload => Array.isArray(payload.data) ? payload.data : []);
                }

                function clientQuery() {
                    return new URLSearchParams({
                        f: /Mobi|Android/i.test(navigator.userAgent) ? 'mweb' : 'web',
                        b: detectBrowserName(),
                        v: detectBrowserVersion(),
                        os: detectOsName(),
                        osv: detectOsVersion()
                    }).toString();
                }

                function resolveApiBaseUrl() {
                    const pathname = window.location.pathname || '';
                    const environment = (pathname.match(/^\\/web\\/([^/]+)/) || [])[1] || 'automation-testing';
                    return `${window.location.origin}/apiv2/${environment}`;
                }

                function findAuthToken() {
                    const values = [];
                    for (const storage of [window.localStorage, window.sessionStorage]) {
                        if (!storage) {
                            continue;
                        }
                        for (let index = 0; index < storage.length; index += 1) {
                            values.push(storage.getItem(storage.key(index)) || '');
                        }
                    }
                    values.push(document.cookie || '');

                    for (const value of values) {
                        const text = String(value || '');
                        const match = text.match(/authToken["']?\\s*[:=]\\s*["']([^"',} ]+)/i)
                            || text.match(/token["']?\\s*[:=]\\s*["']([^"',} ]+)/i)
                            || text.match(/Bearer\\s+([^"',; ]+)/i);
                        if (match && match[1]) {
                            return decodeURIComponent(match[1]);
                        }
                    }

                    return '';
                }

                function normalize(text) {
                    return String(text || '').replace(/\\s+/g, ' ').trim().toLowerCase();
                }

                function detectBrowserName() {
                    const userAgent = navigator.userAgent || '';
                    if (/Edg\\//.test(userAgent)) {
                        return 'Edge';
                    }
                    if (/Chrome\\//.test(userAgent)) {
                        return 'Chrome';
                    }
                    if (/Safari\\//.test(userAgent) && !/Chrome\\//.test(userAgent)) {
                        return 'Safari';
                    }
                    if (/Firefox\\//.test(userAgent)) {
                        return 'Firefox';
                    }
                    return 'Unknown';
                }

                function detectBrowserVersion() {
                    const userAgent = navigator.userAgent || '';
                    return (userAgent.match(/(?:Edg|Chrome|Version|Firefox)\\/([\\d.]+)/) || [])[1] || '';
                }

                function detectOsName() {
                    const userAgent = navigator.userAgent || '';
                    if (/Windows NT/.test(userAgent)) {
                        return 'Windows';
                    }
                    if (/Mac OS X/.test(userAgent)) {
                        return 'macOS';
                    }
                    if (/Android/.test(userAgent)) {
                        return 'Android';
                    }
                    if (/iPhone|iPad|iPod/.test(userAgent)) {
                        return 'iOS';
                    }
                    if (/Linux/.test(userAgent)) {
                        return 'Linux';
                    }
                    return 'Unknown';
                }

                function detectOsVersion() {
                    const userAgent = navigator.userAgent || '';
                    const match = userAgent.match(/Windows NT ([\\d.]+)/)
                        || userAgent.match(/Mac OS X ([\\d_]+)/)
                        || userAgent.match(/Android ([\\d.]+)/)
                        || userAgent.match(/OS ([\\d_]+)/);
                    return match && match[1] ? match[1].replace(/_/g, '.') : '';
                }
                """, normalizedPrefix);

        return Boolean.TRUE.equals(result);
    }

    public int getSavedAddressCountByFullName(String fullName) {
        Object result = executeScript("""
                const expectedName = String(arguments[0] || '').toLowerCase();
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim().toLowerCase();
                const modal = activeAddressListModal();
                if (!modal) {
                    return 0;
                }

                return getAddressCards(modal)
                    .filter(card => normalize(card.innerText || card.textContent || '').includes(expectedName))
                    .length;

                function activeAddressListModal() {
                    return Array.from(document.querySelectorAll('body > div, [role="dialog"]'))
                        .filter(visible)
                        .filter(element => {
                            const text = normalize(element.innerText || element.textContent || '');
                            return /^address\\b/i.test(text)
                                && /add\\s+new\\s+address/i.test(text)
                                && !/add\\s+shipping\\s+address|update\\s+shipping\\s+address/i.test(text);
                        })
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        )
                        .map(candidate => candidate.element)[0] || null;
                }

                function getAddressCards(modal) {
                    return Array.from(modal.querySelectorAll('div'))
                        .filter(visible)
                        .filter(element => {
                            const text = normalize(element.innerText || element.textContent || '');
                            return /\\baddress\\b/i.test(text)
                                && /(?:\\+?88\\s*)?0?1\\d{9}/.test(text)
                                && element.querySelector('input[type="radio"]');
                        })
                        .filter(element => !Array.from(element.children).some(child => {
                            const text = normalize(child.innerText || child.textContent || '');
                            return visible(child)
                                && /\\baddress\\b/i.test(text)
                                && /(?:\\+?88\\s*)?0?1\\d{9}/.test(text)
                                && child.querySelector('input[type="radio"]');
                        }));
                }
                """, normalizeText(fullName).toLowerCase());

        return result instanceof Number number ? number.intValue() : 0;
    }

    public boolean isAddressDisplayed(String fullName, String addressLine) {
        String normalizedName = normalizeText(fullName).toLowerCase();
        String normalizedAddress = normalizeText(addressLine).toLowerCase();

        return Boolean.TRUE.equals(executeScript("""
                const expectedName = String(arguments[0] || '').toLowerCase();
                const expectedAddress = String(arguments[1] || '').toLowerCase();
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim().toLowerCase();
                const modal = activeAddressListModal();
                if (!modal) {
                    return false;
                }

                return getAddressCards(modal).some(card => {
                    const text = normalize(card.innerText || card.textContent || '');
                    return text.includes(expectedName) && text.includes(expectedAddress);
                });

                function activeAddressListModal() {
                    return Array.from(document.querySelectorAll('body > div, [role="dialog"]'))
                        .filter(visible)
                        .filter(element => {
                            const text = normalize(element.innerText || element.textContent || '');
                            return /^address\\b/i.test(text)
                                && /add\\s+new\\s+address/i.test(text)
                                && !/add\\s+shipping\\s+address|update\\s+shipping\\s+address/i.test(text);
                        })
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        )
                        .map(candidate => candidate.element)[0] || null;
                }

                function getAddressCards(modal) {
                    return Array.from(modal.querySelectorAll('div'))
                        .filter(visible)
                        .filter(element => {
                            const text = normalize(element.innerText || element.textContent || '');
                            return /\\baddress\\b/i.test(text)
                                && /(?:\\+?88\\s*)?0?1\\d{9}/.test(text)
                                && element.querySelector('input[type="radio"]');
                        })
                        .filter(element => !Array.from(element.children).some(child => {
                            const text = normalize(child.innerText || child.textContent || '');
                            return visible(child)
                                && /\\baddress\\b/i.test(text)
                                && /(?:\\+?88\\s*)?0?1\\d{9}/.test(text)
                                && child.querySelector('input[type="radio"]');
                        }));
                }
                """, normalizedName, normalizedAddress));
    }

    public boolean isAddressTypeDisplayed(String fullName, String addressType) {
        return addressCardContains(fullName, addressType + " Address");
    }

    public boolean isAddressMarkedDefault(String fullName) {
        return Boolean.TRUE.equals(executeScript("""
                const expectedName = String(arguments[0] || '').toLowerCase();
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim().toLowerCase();
                const modal = activeAddressListModal();
                if (!modal) {
                    return false;
                }

                if (Array.from(modal.querySelectorAll('div'))
                        .filter(visible)
                        .some(element => {
                            const text = normalize(element.innerText || element.textContent || '');
                            return text.includes(expectedName) && /shipping\\s+address/i.test(text);
                        })) {
                    return true;
                }

                return Array.from(modal.querySelectorAll('input[type="radio"]:checked'))
                    .some(radio => {
                        const card = nearestAddressCard(radio, modal);
                        const text = normalize(card ? card.innerText || card.textContent || '' : '');
                        return text.includes(expectedName);
                    });

                function activeAddressListModal() {
                    return Array.from(document.querySelectorAll('body > div, [role="dialog"]'))
                        .filter(visible)
                        .filter(element => {
                            const text = normalize(element.innerText || element.textContent || '');
                            return /^address\\b/i.test(text)
                                && /add\\s+new\\s+address/i.test(text)
                                && !/add\\s+shipping\\s+address|update\\s+shipping\\s+address/i.test(text);
                        })
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        )
                        .map(candidate => candidate.element)[0] || null;
                }

                function nearestAddressCard(radio, modal) {
                    let scope = radio.parentElement;
                    for (let depth = 0; depth < 8 && scope && scope !== modal; depth += 1, scope = scope.parentElement) {
                        const text = normalize(scope.innerText || scope.textContent || '');
                        if (/\\baddress\\b/i.test(text) && /(?:\\+?88\\s*)?0?1\\d{9}/.test(text)) {
                            return scope;
                        }
                    }

                    return radio.parentElement;
                }
                """, normalizeText(fullName).toLowerCase()));
    }

    public boolean isAddressSelectable(String fullName) {
        return Boolean.TRUE.equals(executeScript("""
                const expectedName = String(arguments[0] || '').toLowerCase();
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim().toLowerCase();
                const modal = activeAddressListModal();
                if (!modal) {
                    return false;
                }

                const card = getAddressCards(modal).find(candidate =>
                    normalize(candidate.innerText || candidate.textContent || '').includes(expectedName)
                ) || null;
                const radio = card ? card.querySelector('input[type="radio"]') : null;

                return Boolean(radio) && !radio.disabled;

                function activeAddressListModal() {
                    return Array.from(document.querySelectorAll('body > div, [role="dialog"]'))
                        .filter(visible)
                        .filter(element => {
                            const text = normalize(element.innerText || element.textContent || '');
                            return /^address\\b/i.test(text)
                                && /add\\s+new\\s+address/i.test(text)
                                && !/add\\s+shipping\\s+address|update\\s+shipping\\s+address/i.test(text);
                        })
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        )
                        .map(candidate => candidate.element)[0] || null;
                }

                function getAddressCards(modal) {
                    return Array.from(modal.querySelectorAll('div'))
                        .filter(visible)
                        .filter(element => {
                            const text = normalize(element.innerText || element.textContent || '');
                            return /\\baddress\\b/i.test(text)
                                && /(?:\\+?88\\s*)?0?1\\d{9}/.test(text)
                                && element.querySelector('input[type="radio"]');
                        })
                        .filter(element => !Array.from(element.children).some(child => {
                            const text = normalize(child.innerText || child.textContent || '');
                            return visible(child)
                                && /\\baddress\\b/i.test(text)
                                && /(?:\\+?88\\s*)?0?1\\d{9}/.test(text)
                                && child.querySelector('input[type="radio"]');
                        }));
                }
                """, normalizeText(fullName).toLowerCase()));
    }

    public ShippingAddressPage selectAddress(String fullName) {
        waitUntil(ADDRESS_TIMEOUT, webDriver -> clickAddressRadio(fullName));
        waitUntil(SHORT_ADDRESS_TIMEOUT, webDriver -> isAddressMarkedDefault(fullName));
        return this;
    }

    public ShippingAddressPage selectCheckedOrFirstAddress() {
        waitUntil(ADDRESS_TIMEOUT, webDriver -> clickCheckedOrFirstAddressRadio());
        return this;
    }

    public ShippingAddressPage closeAddressModal() {
        for (int attempt = 0; attempt < 3; attempt++) {
            if (!isAnyAddressModalOpen()) {
                return this;
            }

            Boolean clicked = (Boolean) executeScript("""
                    const visible = element => {
                        const rect = element.getBoundingClientRect();
                        const style = getComputedStyle(element);
                        return rect.width > 0 && rect.height > 0
                            && style.display !== 'none'
                            && style.visibility !== 'hidden'
                            && style.pointerEvents !== 'none'
                            && Number(style.opacity || 1) !== 0;
                    };
                    const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                    const modal = activeAddressModal();
                    if (!modal) {
                        return true;
                    }

                    const modalRect = modal.getBoundingClientRect();
                    const candidates = Array.from(modal.querySelectorAll('button, [role="button"], svg, img'))
                        .filter(visible)
                        .map(element => {
                            const clickable = element.closest('button, [role="button"]') || element;
                            const rect = clickable.getBoundingClientRect();
                            const label = normalize(
                                clickable.innerText
                                || clickable.textContent
                                || clickable.getAttribute('aria-label')
                                || element.getAttribute('aria-label')
                                || element.getAttribute('alt')
                                || ''
                            );
                            const className = String(element.getAttribute('class') || '')
                                + ' ' + String(clickable.getAttribute('class') || '');
                            return { clickable, rect, label, className };
                        })
                        .filter(candidate =>
                            candidate.rect.top <= modalRect.top + 90
                            && candidate.rect.right >= modalRect.right - 90
                            && candidate.rect.width <= 80
                            && candidate.rect.height <= 80
                        )
                        .sort((first, second) =>
                            (second.rect.right - first.rect.right)
                            || (first.rect.top - second.rect.top)
                        );

                    const closeControl = candidates.find(candidate =>
                        /close/i.test(candidate.label)
                        || /^[x×✕]$/i.test(candidate.label)
                        || /lucide-x|icon-x|close/i.test(candidate.className)
                    ) || candidates[0] || null;
                    if (!closeControl) {
                        return false;
                    }

                    closeControl.clickable.click();
                    return true;

                    function activeAddressModal() {
                        return Array.from(document.querySelectorAll('body > div, [role="dialog"]'))
                            .filter(visible)
                            .filter(element => {
                                const text = normalize(element.innerText || element.textContent || '');
                                return /(?:Add|Update)\\s+Shipping\\s+Address/i.test(text)
                                    || (/^Address\\b/i.test(text)
                                        && /Add\\s+New\\s+Address/i.test(text)
                                        && !/Shopping\\s+Cart/i.test(text));
                            })
                            .map(element => ({ element, rect: element.getBoundingClientRect() }))
                            .sort((first, second) =>
                                (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                            )
                            .map(candidate => candidate.element)[0] || null;
                    }
                    """);

            if (!Boolean.TRUE.equals(clicked)) {
                executeScript("""
                        document.dispatchEvent(new KeyboardEvent('keydown', {
                            key: 'Escape',
                            code: 'Escape',
                            keyCode: 27,
                            which: 27,
                            bubbles: true
                        }));
                        """);
            }

            try {
                waitUntil(SHORT_ADDRESS_TIMEOUT, webDriver -> !isAnyAddressModalOpen());
                return this;
            } catch (TimeoutException ignored) {
                // Retry because the close icon can briefly be covered by nested address form transitions.
            }
        }

        driver.navigate().refresh();
        waitForPageLoad();
        new CartPage(driver).openCartDrawer();
        utils.BrowserDiagnosticsUtils.clearBrowserLogs(driver);
        return this;
    }

    public boolean areAddressCardsAligned() {
        return getAddressModalUiIssues()
                .stream()
                .noneMatch(issue -> issue.toLowerCase().contains("card"));
    }

    public boolean isPageResponsive() {
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
                const modal = Array.from(document.querySelectorAll('body > div, [role="dialog"]'))
                    .filter(visible)
                    .filter(element => /\\bAddress\\b/i.test(normalize(element.innerText || element.textContent || '')))
                    .map(element => ({ element, rect: element.getBoundingClientRect() }))
                    .sort((first, second) =>
                        (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                    )[0];
                if (!modal) {
                    return false;
                }

                const rect = modal.rect;
                return rect.left >= -2
                    && rect.right <= window.innerWidth + 2
                    && rect.top >= -2
                    && rect.bottom <= window.innerHeight + 2
                    && rect.width <= window.innerWidth + 2;
                """));
    }

    @SuppressWarnings("unchecked")
    public List<String> getAddressModalUiIssues() {
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
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                const modal = activeAddressListModal();
                if (!modal) {
                    return ['Address modal is not visible'];
                }

                const modalRect = modal.getBoundingClientRect();
                if (modalRect.left < -2 || modalRect.right > window.innerWidth + 2
                        || modalRect.top < -2 || modalRect.bottom > window.innerHeight + 2) {
                    issues.push('Address modal is outside the viewport');
                }

                const radios = Array.from(modal.querySelectorAll('input[type="radio"]'));
                const cards = radios.map(radio => nearestAddressCard(radio, modal)).filter(Boolean);
                if (radios.length === 0) {
                    issues.push('No saved address card is visible');
                }

                const rects = cards.map(card => card.getBoundingClientRect());
                for (let first = 0; first < rects.length; first += 1) {
                    for (let second = first + 1; second < rects.length; second += 1) {
                        if (overlaps(rects[first], rects[second])) {
                            issues.push('Address cards overlap');
                        }
                    }
                }

                const addButton = Array.from(modal.querySelectorAll('button'))
                    .filter(visible)
                    .find(button => /Add\\s+New\\s+Address/i.test(normalize(button.innerText || button.textContent || '')));
                if (!addButton || addButton.disabled) {
                    issues.push('Add New Address button is missing or disabled');
                }

                for (const image of Array.from(modal.querySelectorAll('img')).filter(visible)) {
                    const descriptor = image.currentSrc || image.src || image.alt || '<empty image>';
                    if (!image.complete || image.naturalWidth === 0 || image.naturalHeight === 0) {
                        issues.push('Broken image: ' + descriptor);
                    } else if (/placeholder|broken|no-image/i.test(descriptor)) {
                        issues.push('Placeholder image: ' + descriptor);
                    }
                }

                for (const icon of Array.from(modal.querySelectorAll('svg')).filter(visible)) {
                    const rect = icon.getBoundingClientRect();
                    if (rect.width === 0 || rect.height === 0) {
                        issues.push('Broken icon in address modal');
                    }
                }

                return issues;

                function overlaps(first, second) {
                    return first.left < second.right - 2
                        && first.right > second.left + 2
                        && first.top < second.bottom - 2
                        && first.bottom > second.top + 2;
                }

                function activeAddressListModal() {
                    return Array.from(document.querySelectorAll('body > div, [role="dialog"]'))
                        .filter(visible)
                        .filter(element => {
                            const text = normalize(element.innerText || element.textContent || '');
                            return /^Address\\b/i.test(text)
                                && /Add\\s+New\\s+Address/i.test(text)
                                && !/Add\\s+Shipping\\s+Address|Update\\s+Shipping\\s+Address/i.test(text);
                        })
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        )
                        .map(candidate => candidate.element)[0] || null;
                }

                function nearestAddressCard(radio, modal) {
                    let scope = radio.parentElement;
                    for (let depth = 0; depth < 8 && scope && scope !== modal; depth += 1, scope = scope.parentElement) {
                        const text = normalize(scope.innerText || scope.textContent || '');
                        if (/\\bAddress\\b/i.test(text) && /(?:\\+?88\\s*)?0?1\\d{9}/.test(text)) {
                            return scope;
                        }
                    }

                    return radio.parentElement;
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

    private void fillAddressForm(
            String fullName,
            String phoneNumber,
            String[] deliveryAreaPath,
            String addressLine,
            String addressType,
            boolean makeDefault
    ) {
        if (fullName != null) {
            waitUntil(ADDRESS_TIMEOUT, webDriver -> setTextFieldValue("Full Name", fullName));
        }
        if (phoneNumber != null) {
            waitUntil(ADDRESS_TIMEOUT, webDriver -> setTextFieldValue("Phone Number", phoneNumber));
        }
        if (deliveryAreaPath != null) {
            selectDeliveryArea(deliveryAreaPath);
        }
        if (addressLine != null) {
            waitUntil(ADDRESS_TIMEOUT, webDriver -> setTextFieldValue("Address", addressLine));
        }
        if (addressType != null) {
            waitUntil(ADDRESS_TIMEOUT, webDriver -> clickAddressType(addressType));
        }
        if (makeDefault) {
            waitUntil(ADDRESS_TIMEOUT, webDriver -> enableDefaultAddressOption());
        }
    }

    private void selectDeliveryArea(String[] deliveryAreaPath) {
        waitUntil(ADDRESS_TIMEOUT, webDriver -> clickDeliveryAreaButton());

        for (int index = 0; index < deliveryAreaPath.length; index++) {
            String option = deliveryAreaPath[index];
            String nextOption = index + 1 < deliveryAreaPath.length ? deliveryAreaPath[index + 1] : null;
            waitUntil(ADDRESS_TIMEOUT, webDriver -> clickDeliveryAreaOption(option));
            waitUntil(ADDRESS_TIMEOUT, webDriver -> isDeliveryAreaSelectionReady(option, nextOption));
        }

        waitUntil(ADDRESS_TIMEOUT, webDriver -> isDeliveryAreaSelected());
    }

    private void selectSubArea(String subArea) {
        if (subArea == null || subArea.isBlank()) {
            return;
        }

        waitUntil(ADDRESS_TIMEOUT, webDriver -> clickSubAreaButton());
        waitUntil(ADDRESS_TIMEOUT, webDriver -> clickSubAreaOption(subArea));
        waitUntil(ADDRESS_TIMEOUT, webDriver -> isSubAreaSelected(subArea));
    }

    private void submitAddressForm() {
        waitUntil(ADDRESS_TIMEOUT, webDriver -> clickSubmitButton());
    }

    private void waitForSavedAddressOrRecoverFromLabSyncBlock(AddressData addressData) {
        try {
            waitUntil(ADDRESS_TIMEOUT, webDriver -> isShippingAddressPageOpen()
                    && isAddressDisplayed(addressData.fullName(), addressData.addressLine()));
            return;
        } catch (TimeoutException exception) {
            if (!isAddressPersistedUsingApi(addressData)) {
                throw exception;
            }
        }

        driver.navigate().refresh();
        waitForPageLoad();
        new CartPage(driver).openShippingAddressPageFromCartDrawer();
        waitUntil(ADDRESS_TIMEOUT, webDriver -> isShippingAddressPageOpen()
                && isAddressDisplayed(addressData.fullName(), addressData.addressLine()));
        utils.BrowserDiagnosticsUtils.clearBrowserLogs(driver);
    }

    private boolean isAddressPersistedUsingApi(AddressData addressData) {
        return Boolean.TRUE.equals(executeAsyncScript("""
                const done = arguments[arguments.length - 1];
                const expectedName = normalize(arguments[0]);
                const expectedAddress = normalize(arguments[1]);
                const token = findAuthToken();
                if (!expectedName || !expectedAddress || !token) {
                    done(false);
                    return;
                }

                const query = new URLSearchParams({
                    _page: '1',
                    _perPage: '100',
                    _return_total: '1',
                    _reload: 'false',
                    f: /Mobi|Android/i.test(navigator.userAgent) ? 'mweb' : 'web',
                    b: detectBrowserName(),
                    v: detectBrowserVersion(),
                    os: detectOsName(),
                    osv: detectOsVersion()
                });

                fetch(`${resolveApiBaseUrl()}/userLocation/v1?${query}`, {
                    method: 'GET',
                    headers: { Authorization: `Bearer ${token}` }
                })
                    .then(response => response.ok ? response.json() : Promise.reject(new Error('Address list failed')))
                    .then(payload => {
                        const addresses = Array.isArray(payload.data) ? payload.data : [];
                        done(addresses.some(address =>
                            normalize(address.ul_name || address.name || address.fullName || '') === expectedName
                            && normalize(address.ul_address || address.address || address.addressLine || '') === expectedAddress
                        ));
                    })
                    .catch(() => done(false));

                function resolveApiBaseUrl() {
                    const pathname = window.location.pathname || '';
                    const environment = (pathname.match(/^\\/web\\/([^/]+)/) || [])[1] || 'automation-testing';
                    return `${window.location.origin}/apiv2/${environment}`;
                }

                function findAuthToken() {
                    const values = [];
                    for (const storage of [window.localStorage, window.sessionStorage]) {
                        if (!storage) {
                            continue;
                        }
                        for (let index = 0; index < storage.length; index += 1) {
                            values.push(storage.getItem(storage.key(index)) || '');
                        }
                    }
                    values.push(document.cookie || '');

                    for (const value of values) {
                        const text = String(value || '');
                        const match = text.match(/authToken["']?\\s*[:=]\\s*["']([^"',} ]+)/i)
                            || text.match(/token["']?\\s*[:=]\\s*["']([^"',} ]+)/i)
                            || text.match(/Bearer\\s+([^"',; ]+)/i);
                        if (match && match[1]) {
                            return decodeURIComponent(match[1]);
                        }
                    }

                    return '';
                }

                function normalize(text) {
                    return String(text || '').replace(/\\s+/g, ' ').trim().toLowerCase();
                }

                function detectBrowserName() {
                    const userAgent = navigator.userAgent || '';
                    if (/Edg\\//.test(userAgent)) {
                        return 'Edge';
                    }
                    if (/Chrome\\//.test(userAgent)) {
                        return 'Chrome';
                    }
                    if (/Safari\\//.test(userAgent) && !/Chrome\\//.test(userAgent)) {
                        return 'Safari';
                    }
                    if (/Firefox\\//.test(userAgent)) {
                        return 'Firefox';
                    }
                    return 'Unknown';
                }

                function detectBrowserVersion() {
                    const userAgent = navigator.userAgent || '';
                    return (userAgent.match(/(?:Edg|Chrome|Version|Firefox)\\/([\\d.]+)/) || [])[1] || '';
                }

                function detectOsName() {
                    const userAgent = navigator.userAgent || '';
                    if (/Windows NT/.test(userAgent)) {
                        return 'Windows';
                    }
                    if (/Mac OS X/.test(userAgent)) {
                        return 'macOS';
                    }
                    if (/Android/.test(userAgent)) {
                        return 'Android';
                    }
                    if (/iPhone|iPad|iPod/.test(userAgent)) {
                        return 'iOS';
                    }
                    if (/Linux/.test(userAgent)) {
                        return 'Linux';
                    }
                    return 'Unknown';
                }

                function detectOsVersion() {
                    const userAgent = navigator.userAgent || '';
                    return (userAgent.match(/Windows NT ([\\d.]+)/)
                        || userAgent.match(/Mac OS X ([\\d_]+)/)
                        || userAgent.match(/Android ([\\d.]+)/)
                        || userAgent.match(/OS ([\\d_]+)/)
                        || [])[1]?.replace(/_/g, '.') || '';
                }
                """, addressData.fullName(), addressData.addressLine()));
    }

    private boolean areFormFieldsDisplayed(String... fieldLabels) {
        for (String fieldLabel : fieldLabels) {
            if (!isFormFieldDisplayed(fieldLabel)) {
                return false;
            }
        }

        return true;
    }

    private boolean isFormFieldDisplayed(String fieldLabel) {
        return Boolean.TRUE.equals(executeScript("""
                const labelText = String(arguments[0] || '').toLowerCase();
                const form = activeFormModal();
                if (!form) {
                    return false;
                }

                const label = Array.from(form.querySelectorAll('label'))
                    .find(candidate => normalize(candidate.innerText || candidate.textContent)
                        .toLowerCase().startsWith(labelText));
                if (!label) {
                    return false;
                }

                label.scrollIntoView({ block: 'center', inline: 'nearest' });
                const rect = label.getBoundingClientRect();
                return rect.width > 0 && rect.height > 0;

                function normalize(text) {
                    return String(text || '').replace(/\\s+/g, ' ').trim();
                }

                function activeFormModal() {
                    const visible = element => {
                        const rect = element.getBoundingClientRect();
                        const style = getComputedStyle(element);
                        return rect.width > 0 && rect.height > 0
                            && style.display !== 'none'
                            && style.visibility !== 'hidden'
                            && Number(style.opacity || 1) !== 0;
                    };
                    return Array.from(document.querySelectorAll('body > div, [role="dialog"]'))
                        .filter(visible)
                        .filter(element => /(?:Add|Update)\\s+Shipping\\s+Address/i.test(
                            normalize(element.innerText || element.textContent || '')
                        ))
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        )
                        .map(candidate => candidate.element)[0] || null;
                }
                """, fieldLabel));
    }

    private String getTextFieldValue(String fieldLabel) {
        Object result = executeScript("""
                const labelText = String(arguments[0] || '').toLowerCase();
                const form = activeFormModal();
                const control = form ? fieldControl(form, labelText) : null;
                return control ? String(control.value || '').trim() : '';

                function normalize(text) {
                    return String(text || '').replace(/\\s+/g, ' ').trim();
                }

                function activeFormModal() {
                    const visible = element => {
                        const rect = element.getBoundingClientRect();
                        const style = getComputedStyle(element);
                        return rect.width > 0 && rect.height > 0
                            && style.display !== 'none'
                            && style.visibility !== 'hidden'
                            && Number(style.opacity || 1) !== 0;
                    };
                    return Array.from(document.querySelectorAll('body > div, [role="dialog"]'))
                        .filter(visible)
                        .filter(element => /(?:Add|Update)\\s+Shipping\\s+Address/i.test(
                            normalize(element.innerText || element.textContent || '')
                        ))
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        )
                        .map(candidate => candidate.element)[0] || null;
                }

                function fieldControl(form, labelText) {
                    const label = Array.from(form.querySelectorAll('label'))
                        .find(candidate => normalize(candidate.innerText || candidate.textContent)
                            .toLowerCase().startsWith(labelText));
                    if (!label) {
                        return null;
                    }

                    let scope = label.parentElement;
                    for (let depth = 0; depth < 4 && scope; depth += 1, scope = scope.parentElement) {
                        const controls = Array.from(scope.querySelectorAll('input, textarea'))
                            .filter(element => element.type !== 'hidden');
                        if (controls.length > 0) {
                            return controls[0];
                        }
                    }

                    return null;
                }
                """, fieldLabel);

        return result == null ? "" : String.valueOf(result).trim();
    }

    private boolean setTextFieldValue(String fieldLabel, String value) {
        return Boolean.TRUE.equals(executeScript("""
                const labelText = String(arguments[0] || '').toLowerCase();
                const value = String(arguments[1] || '');
                const form = activeFormModal();
                const control = form ? fieldControl(form, labelText) : null;
                if (!control || control.disabled) {
                    return false;
                }

                control.scrollIntoView({ block: 'center', inline: 'nearest' });
                const prototype = control.tagName.toLowerCase() === 'textarea'
                    ? HTMLTextAreaElement.prototype
                    : HTMLInputElement.prototype;
                const descriptor = Object.getOwnPropertyDescriptor(prototype, 'value');
                descriptor.set.call(control, value);
                control.dispatchEvent(new Event('input', { bubbles: true }));
                control.dispatchEvent(new Event('change', { bubbles: true }));
                return String(control.value || '') === value;

                function normalize(text) {
                    return String(text || '').replace(/\\s+/g, ' ').trim();
                }

                function activeFormModal() {
                    const visible = element => {
                        const rect = element.getBoundingClientRect();
                        const style = getComputedStyle(element);
                        return rect.width > 0 && rect.height > 0
                            && style.display !== 'none'
                            && style.visibility !== 'hidden'
                            && Number(style.opacity || 1) !== 0;
                    };
                    return Array.from(document.querySelectorAll('body > div, [role="dialog"]'))
                        .filter(visible)
                        .filter(element => /(?:Add|Update)\\s+Shipping\\s+Address/i.test(
                            normalize(element.innerText || element.textContent || '')
                        ))
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        )
                        .map(candidate => candidate.element)[0] || null;
                }

                function fieldControl(form, labelText) {
                    const label = Array.from(form.querySelectorAll('label'))
                        .find(candidate => normalize(candidate.innerText || candidate.textContent)
                            .toLowerCase().startsWith(labelText));
                    if (!label) {
                        return null;
                    }

                    let scope = label.parentElement;
                    for (let depth = 0; depth < 4 && scope; depth += 1, scope = scope.parentElement) {
                        const controls = Array.from(scope.querySelectorAll('input, textarea'))
                            .filter(element => element.type !== 'hidden');
                        if (controls.length > 0) {
                            return controls[0];
                        }
                    }

                    return null;
                }
                """, fieldLabel, value));
    }

    private boolean clickDeliveryAreaButton() {
        return Boolean.TRUE.equals(executeScript("""
                const form = activeFormModal();
                const control = form ? fieldControl(form, 'select delivery area') : null;
                if (!control || control.disabled) {
                    return false;
                }

                control.scrollIntoView({ block: 'center', inline: 'nearest' });
                control.click();
                return true;

                function normalize(text) {
                    return String(text || '').replace(/\\s+/g, ' ').trim();
                }

                function activeFormModal() {
                    const visible = element => {
                        const rect = element.getBoundingClientRect();
                        const style = getComputedStyle(element);
                        return rect.width > 0 && rect.height > 0
                            && style.display !== 'none'
                            && style.visibility !== 'hidden'
                            && Number(style.opacity || 1) !== 0;
                    };
                    return Array.from(document.querySelectorAll('body > div, [role="dialog"]'))
                        .filter(visible)
                        .filter(element => /(?:Add|Update)\\s+Shipping\\s+Address/i.test(
                            normalize(element.innerText || element.textContent || '')
                        ))
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        )
                        .map(candidate => candidate.element)[0] || null;
                }

                function fieldControl(form, labelText) {
                    const label = Array.from(form.querySelectorAll('label'))
                        .find(candidate => normalize(candidate.innerText || candidate.textContent)
                            .toLowerCase().startsWith(labelText));
                    if (!label) {
                        return null;
                    }

                    let scope = label.parentElement;
                    for (let depth = 0; depth < 4 && scope; depth += 1, scope = scope.parentElement) {
                        const control = scope.querySelector('button');
                        if (control) {
                            return control;
                        }
                    }

                    return null;
                }
                """));
    }

    private boolean clickDeliveryAreaOption(String optionLabel) {
        return Boolean.TRUE.equals(executeScript("""
                const optionLabel = String(arguments[0] || '').trim();
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                const form = activeFormModal();
                if (!form) {
                    return false;
                }

                let option = findOption(form, optionLabel);
                if (!option) {
                    const searchInput = Array.from(form.querySelectorAll('input'))
                        .filter(visible)
                        .find(input => /search/i.test(input.getAttribute('placeholder') || ''));
                    if (searchInput) {
                        const descriptor = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value');
                        descriptor.set.call(searchInput, optionLabel);
                        searchInput.dispatchEvent(new Event('input', { bubbles: true }));
                        searchInput.dispatchEvent(new Event('change', { bubbles: true }));
                    }
                    option = findOption(form, optionLabel);
                }

                if (!option) {
                    return false;
                }

                option.scrollIntoView({ block: 'center', inline: 'nearest' });
                option.click();
                return true;

                function activeFormModal() {
                    return Array.from(document.querySelectorAll('body > div, [role="dialog"]'))
                        .filter(visible)
                        .filter(element => /(?:Add|Update)\\s+Shipping\\s+Address/i.test(
                            normalize(element.innerText || element.textContent || '')
                        ))
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        )
                        .map(candidate => candidate.element)[0] || null;
                }

                function findOption(scope, label) {
                    return Array.from(scope.querySelectorAll('button, [role="option"], div, span'))
                        .filter(visible)
                        .filter(element => normalize(element.innerText || element.textContent || '')
                            .toLowerCase() === label.toLowerCase())
                        .sort((first, second) => first.children.length - second.children.length)[0] || null;
                }
                """, optionLabel));
    }

    private boolean isDeliveryAreaSelectionReady(String selectedOption, String nextOption) {
        return Boolean.TRUE.equals(executeScript("""
                const selectedOption = String(arguments[0] || '').trim();
                const nextOption = String(arguments[1] || '').trim();
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                const form = activeFormModal();
                if (!form || hasLoadingIndicator(form)) {
                    return false;
                }

                if (nextOption) {
                    return Boolean(findOption(form, nextOption));
                }

                const control = fieldControl(form, 'select delivery area');
                const controlText = normalize(control ? control.innerText || control.textContent || '' : '');
                return controlText.toLowerCase().includes(selectedOption.toLowerCase())
                    && !/Select\\s+delivery\\s+area/i.test(controlText);

                function activeFormModal() {
                    return Array.from(document.querySelectorAll('body > div, [role="dialog"]'))
                        .filter(visible)
                        .filter(element => /(?:Add|Update)\\s+Shipping\\s+Address/i.test(
                            normalize(element.innerText || element.textContent || '')
                        ))
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        )
                        .map(candidate => candidate.element)[0] || null;
                }

                function fieldControl(formElement, labelText) {
                    const label = Array.from(formElement.querySelectorAll('label'))
                        .find(candidate => normalize(candidate.innerText || candidate.textContent)
                            .toLowerCase().startsWith(labelText));
                    if (!label) {
                        return null;
                    }

                    let scope = label.parentElement;
                    for (let depth = 0; depth < 4 && scope; depth += 1, scope = scope.parentElement) {
                        const control = scope.querySelector('button');
                        if (control) {
                            return control;
                        }
                    }

                    return null;
                }

                function findOption(scope, label) {
                    return Array.from(scope.querySelectorAll('button, [role="option"], div, span'))
                        .filter(visible)
                        .filter(element => normalize(element.innerText || element.textContent || '')
                            .toLowerCase() === label.toLowerCase())
                        .sort((first, second) => first.children.length - second.children.length)[0] || null;
                }

                function hasLoadingIndicator(scope) {
                    return Array.from(scope.querySelectorAll(
                        '[role="progressbar"], [class*="spinner"], [class*="loader"], [class*="loading"], .MuiCircularProgress-root'
                    )).some(visible);
                }
                """, selectedOption, nextOption));
    }

    private boolean isDeliveryAreaSelected() {
        return Boolean.TRUE.equals(executeScript("""
                const form = activeFormModal();
                const control = form ? fieldControl(form, 'select delivery area') : null;
                if (!control) {
                    return false;
                }

                const text = normalize(control.innerText || control.textContent || '');
                return /Dhaka/i.test(text) && !/Select\\s+delivery\\s+area/i.test(text);

                function normalize(text) {
                    return String(text || '').replace(/\\s+/g, ' ').trim();
                }

                function activeFormModal() {
                    const visible = element => {
                        const rect = element.getBoundingClientRect();
                        const style = getComputedStyle(element);
                        return rect.width > 0 && rect.height > 0
                            && style.display !== 'none'
                            && style.visibility !== 'hidden'
                            && Number(style.opacity || 1) !== 0;
                    };
                    return Array.from(document.querySelectorAll('body > div, [role="dialog"]'))
                        .filter(visible)
                        .filter(element => /(?:Add|Update)\\s+Shipping\\s+Address/i.test(
                            normalize(element.innerText || element.textContent || '')
                        ))
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        )
                        .map(candidate => candidate.element)[0] || null;
                }

                function fieldControl(form, labelText) {
                    const label = Array.from(form.querySelectorAll('label'))
                        .find(candidate => normalize(candidate.innerText || candidate.textContent)
                            .toLowerCase().startsWith(labelText));
                    if (!label) {
                        return null;
                    }

                    let scope = label.parentElement;
                    for (let depth = 0; depth < 4 && scope; depth += 1, scope = scope.parentElement) {
                        const control = scope.querySelector('button');
                        if (control) {
                            return control;
                        }
                    }

                    return null;
                }
                """));
    }

    private boolean clickSubAreaButton() {
        return Boolean.TRUE.equals(executeScript("""
                const form = activeFormModal();
                const control = form ? fieldControl(form, 'select sub area') : null;
                if (!control || control.disabled) {
                    return false;
                }

                control.scrollIntoView({ block: 'center', inline: 'nearest' });
                control.click();
                return true;

                function normalize(text) {
                    return String(text || '').replace(/\\s+/g, ' ').trim();
                }

                function activeFormModal() {
                    const visible = element => {
                        const rect = element.getBoundingClientRect();
                        const style = getComputedStyle(element);
                        return rect.width > 0 && rect.height > 0
                            && style.display !== 'none'
                            && style.visibility !== 'hidden'
                            && Number(style.opacity || 1) !== 0;
                    };
                    return Array.from(document.querySelectorAll('body > div, [role="dialog"]'))
                        .filter(visible)
                        .filter(element => /(?:Add|Update)\\s+Shipping\\s+Address/i.test(
                            normalize(element.innerText || element.textContent || '')
                        ))
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        )
                        .map(candidate => candidate.element)[0] || null;
                }

                function fieldControl(formElement, labelText) {
                    const label = Array.from(formElement.querySelectorAll('label'))
                        .find(candidate => normalize(candidate.innerText || candidate.textContent)
                            .toLowerCase().startsWith(labelText));
                    if (!label) {
                        return null;
                    }

                    let scope = label.parentElement;
                    for (let depth = 0; depth < 4 && scope; depth += 1, scope = scope.parentElement) {
                        const control = scope.querySelector('button');
                        if (control) {
                            return control;
                        }
                    }

                    return null;
                }
                """));
    }

    private boolean clickSubAreaOption(String subArea) {
        return Boolean.TRUE.equals(executeScript("""
                const subArea = String(arguments[0] || '').trim();
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                const form = activeFormModal();
                if (!form) {
                    return false;
                }

                let option = findOption(form, subArea);
                if (!option) {
                    const searchInput = Array.from(form.querySelectorAll('input'))
                        .filter(visible)
                        .find(input => /search/i.test(input.getAttribute('placeholder') || ''));
                    if (searchInput) {
                        const descriptor = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value');
                        descriptor.set.call(searchInput, subArea);
                        searchInput.dispatchEvent(new Event('input', { bubbles: true }));
                        searchInput.dispatchEvent(new Event('change', { bubbles: true }));
                    }
                    option = findOption(form, subArea);
                }

                if (!option) {
                    return false;
                }

                option.scrollIntoView({ block: 'center', inline: 'nearest' });
                option.click();
                return true;

                function activeFormModal() {
                    return Array.from(document.querySelectorAll('body > div, [role="dialog"]'))
                        .filter(visible)
                        .filter(element => /(?:Add|Update)\\s+Shipping\\s+Address/i.test(
                            normalize(element.innerText || element.textContent || '')
                        ))
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        )
                        .map(candidate => candidate.element)[0] || null;
                }

                function findOption(scope, label) {
                    const normalizedLabel = label.toLowerCase();
                    const candidates = Array.from(scope.querySelectorAll('button, [role="option"], div, span'))
                        .filter(visible)
                        .map(element => ({
                            element,
                            text: normalize(element.innerText || element.textContent || '')
                        }))
                        .filter(candidate => candidate.text);
                    return candidates
                        .filter(candidate => candidate.text.toLowerCase() === normalizedLabel)
                        .sort((first, second) => first.element.children.length - second.element.children.length)[0]?.element
                        || candidates
                            .filter(candidate => candidate.text.toLowerCase().includes(normalizedLabel))
                            .sort((first, second) => first.element.children.length - second.element.children.length)[0]?.element
                        || null;
                }
                """, subArea));
    }

    private boolean isSubAreaSelected(String subArea) {
        return Boolean.TRUE.equals(executeScript("""
                const subArea = String(arguments[0] || '').trim().toLowerCase();
                const form = activeFormModal();
                const control = form ? fieldControl(form, 'select sub area') : null;
                if (!control) {
                    return false;
                }

                const text = normalize(control.innerText || control.textContent || '').toLowerCase();
                return text.includes(subArea) && !/Select\\s+sub\\s+area/i.test(text);

                function normalize(text) {
                    return String(text || '').replace(/\\s+/g, ' ').trim();
                }

                function activeFormModal() {
                    const visible = element => {
                        const rect = element.getBoundingClientRect();
                        const style = getComputedStyle(element);
                        return rect.width > 0 && rect.height > 0
                            && style.display !== 'none'
                            && style.visibility !== 'hidden'
                            && Number(style.opacity || 1) !== 0;
                    };
                    return Array.from(document.querySelectorAll('body > div, [role="dialog"]'))
                        .filter(visible)
                        .filter(element => /(?:Add|Update)\\s+Shipping\\s+Address/i.test(
                            normalize(element.innerText || element.textContent || '')
                        ))
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        )
                        .map(candidate => candidate.element)[0] || null;
                }

                function fieldControl(formElement, labelText) {
                    const label = Array.from(formElement.querySelectorAll('label'))
                        .find(candidate => normalize(candidate.innerText || candidate.textContent)
                            .toLowerCase().startsWith(labelText));
                    if (!label) {
                        return null;
                    }

                    let scope = label.parentElement;
                    for (let depth = 0; depth < 4 && scope; depth += 1, scope = scope.parentElement) {
                        const control = scope.querySelector('button');
                        if (control) {
                            return control;
                        }
                    }

                    return null;
                }
                """, subArea));
    }

    private boolean clickAddressType(String addressType) {
        return Boolean.TRUE.equals(executeScript("""
                const addressType = String(arguments[0] || '').trim().toLowerCase();
                const form = activeFormModal();
                if (!form) {
                    return false;
                }

                const button = Array.from(form.querySelectorAll('button'))
                    .find(candidate => normalize(candidate.innerText || candidate.textContent)
                        .toLowerCase() === addressType);
                if (!button || button.disabled) {
                    return false;
                }

                button.scrollIntoView({ block: 'center', inline: 'nearest' });
                button.click();
                return true;

                function normalize(text) {
                    return String(text || '').replace(/\\s+/g, ' ').trim();
                }

                function activeFormModal() {
                    const visible = element => {
                        const rect = element.getBoundingClientRect();
                        const style = getComputedStyle(element);
                        return rect.width > 0 && rect.height > 0
                            && style.display !== 'none'
                            && style.visibility !== 'hidden'
                            && Number(style.opacity || 1) !== 0;
                    };
                    return Array.from(document.querySelectorAll('body > div, [role="dialog"]'))
                        .filter(visible)
                        .filter(element => /(?:Add|Update)\\s+Shipping\\s+Address/i.test(
                            normalize(element.innerText || element.textContent || '')
                        ))
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        )
                        .map(candidate => candidate.element)[0] || null;
                }
                """, addressType));
    }

    private boolean isAddressTypeAvailable(String addressType) {
        return Boolean.TRUE.equals(executeScript("""
                const addressType = String(arguments[0] || '').trim().toLowerCase();
                const form = activeFormModal();
                if (!form) {
                    return false;
                }

                return Array.from(form.querySelectorAll('button'))
                    .some(candidate => normalize(candidate.innerText || candidate.textContent)
                        .toLowerCase() === addressType && !candidate.disabled);

                function normalize(text) {
                    return String(text || '').replace(/\\s+/g, ' ').trim();
                }

                function activeFormModal() {
                    const visible = element => {
                        const rect = element.getBoundingClientRect();
                        const style = getComputedStyle(element);
                        return rect.width > 0 && rect.height > 0
                            && style.display !== 'none'
                            && style.visibility !== 'hidden'
                            && Number(style.opacity || 1) !== 0;
                    };
                    return Array.from(document.querySelectorAll('body > div, [role="dialog"]'))
                        .filter(visible)
                        .filter(element => /(?:Add|Update)\\s+Shipping\\s+Address/i.test(
                            normalize(element.innerText || element.textContent || '')
                        ))
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        )
                        .map(candidate => candidate.element)[0] || null;
                }
                """, addressType));
    }

    private boolean enableDefaultAddressOption() {
        return Boolean.TRUE.equals(executeScript("""
                const form = activeFormModal();
                if (!form) {
                    return false;
                }

                const label = Array.from(form.querySelectorAll('label, div, span'))
                    .find(element => /Make\\s+default\\s+address|Set\\s+as\\s+Default/i.test(
                        element.innerText || element.textContent || ''
                    ));
                if (!label) {
                    return false;
                }

                let scope = label.parentElement;
                let input = null;
                for (let depth = 0; depth < 5 && scope; depth += 1, scope = scope.parentElement) {
                    input = scope.querySelector('input[type="checkbox"], input[type="radio"]');
                    if (input) {
                        break;
                    }
                }

                label.scrollIntoView({ block: 'center', inline: 'nearest' });
                if (input && !input.checked) {
                    input.click();
                } else if (!input) {
                    label.click();
                }

                return !input || input.checked;

                function activeFormModal() {
                    const visible = element => {
                        const rect = element.getBoundingClientRect();
                        const style = getComputedStyle(element);
                        return rect.width > 0 && rect.height > 0
                            && style.display !== 'none'
                            && style.visibility !== 'hidden'
                            && Number(style.opacity || 1) !== 0;
                    };
                    const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                    return Array.from(document.querySelectorAll('body > div, [role="dialog"]'))
                        .filter(visible)
                        .filter(element => /(?:Add|Update)\\s+Shipping\\s+Address/i.test(
                            normalize(element.innerText || element.textContent || '')
                        ))
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        )
                        .map(candidate => candidate.element)[0] || null;
                }
                """));
    }

    private boolean clickSubmitButton() {
        return Boolean.TRUE.equals(executeScript("""
                const form = activeFormModal();
                if (!form) {
                    return false;
                }

                const button = Array.from(form.querySelectorAll('button'))
                    .find(candidate => /^Submit$|^Save$/i.test(
                        normalize(candidate.innerText || candidate.textContent || '')
                    ));
                if (!button || button.disabled) {
                    return false;
                }

                button.scrollIntoView({ block: 'center', inline: 'nearest' });
                button.click();
                return true;

                function normalize(text) {
                    return String(text || '').replace(/\\s+/g, ' ').trim();
                }

                function activeFormModal() {
                    const visible = element => {
                        const rect = element.getBoundingClientRect();
                        const style = getComputedStyle(element);
                        return rect.width > 0 && rect.height > 0
                            && style.display !== 'none'
                            && style.visibility !== 'hidden'
                            && Number(style.opacity || 1) !== 0;
                    };
                    return Array.from(document.querySelectorAll('body > div, [role="dialog"]'))
                        .filter(visible)
                        .filter(element => /(?:Add|Update)\\s+Shipping\\s+Address/i.test(
                            normalize(element.innerText || element.textContent || '')
                        ))
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        )
                        .map(candidate => candidate.element)[0] || null;
                }
                """));
    }

    private boolean clickFirstAddressActionMenu() {
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
                const modal = activeAddressListModal();
                if (!modal) {
                    return false;
                }

                const card = getAddressCards(modal)[0] || null;
                if (!card) {
                    return false;
                }

                const action = card.querySelector('svg.lucide-ellipsis')
                    || Array.from(card.querySelectorAll('button, div, span'))
                        .filter(visible)
                        .find(element => /edit|more|menu/i.test(
                            normalize(element.innerText || element.textContent || element.getAttribute('aria-label') || '')
                        ));
                if (!action) {
                    return false;
                }

                action.dispatchEvent(new MouseEvent('click', {
                    bubbles: true,
                    cancelable: true,
                    view: window
                }));
                return true;

                function activeAddressListModal() {
                    return Array.from(document.querySelectorAll('body > div, [role="dialog"]'))
                        .filter(visible)
                        .filter(element => {
                            const text = normalize(element.innerText || element.textContent || '');
                            return /^Address\\b/i.test(text)
                                && /Add\\s+New\\s+Address/i.test(text)
                                && !/Add\\s+Shipping\\s+Address|Update\\s+Shipping\\s+Address/i.test(text);
                        })
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        )
                        .map(candidate => candidate.element)[0] || null;
                }

                function getAddressCards(modal) {
                    return Array.from(modal.querySelectorAll('div'))
                        .filter(visible)
                        .filter(element => {
                            const text = normalize(element.innerText || element.textContent || '');
                            return /\\b(Address)\\b/i.test(text)
                                && /(?:\\+?88\\s*)?0?1\\d{9}/.test(text)
                                && element.querySelector('input[type="radio"]');
                        })
                        .filter(element => !Array.from(element.children).some(child => {
                            const text = normalize(child.innerText || child.textContent || '');
                            return visible(child)
                                && /\\b(Address)\\b/i.test(text)
                                && /(?:\\+?88\\s*)?0?1\\d{9}/.test(text)
                                && child.querySelector('input[type="radio"]');
                        }));
                }
                """));
    }

    private boolean clickAddressActionMenuByFullNamePrefix(String fullNamePrefix) {
        return Boolean.TRUE.equals(executeScript("""
                const expectedPrefix = String(arguments[0] || '').toLowerCase();
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim().toLowerCase();
                const modal = activeAddressListModal();
                if (!modal) {
                    return false;
                }

                const card = findAddressActionCard(modal);
                if (!card) {
                    return false;
                }

                const icon = topRightAction(card);
                const iconButton = icon ? closestClickable(icon, card) : null;
                const fallbackAction = Array.from(card.querySelectorAll('button, div, span'))
                    .filter(visible)
                    .find(element => /edit|more|menu|ellipsis/i.test(
                        normalize(element.innerText || element.textContent || element.getAttribute('aria-label') || '')
                    ) || /^[.⋯…]+$/.test(normalize(element.innerText || element.textContent || '')));
                const action = iconButton || icon || fallbackAction;
                card.scrollIntoView({ block: 'center', inline: 'nearest' });
                if (action) {
                    clickLikeUser(action);
                    return true;
                }

                const cardRect = card.getBoundingClientRect();
                const target = document.elementFromPoint(cardRect.right - 28, cardRect.top + 32);
                if (!target) {
                    return false;
                }

                clickLikeUser(target, cardRect.right - 28, cardRect.top + 32);
                return true;

                function activeAddressListModal() {
                    return Array.from(document.querySelectorAll('body > div, [role="dialog"]'))
                        .filter(visible)
                        .filter(element => {
                            const text = normalize(element.innerText || element.textContent || '');
                            return /^address\\b/i.test(text)
                                && /add\\s+new\\s+address/i.test(text)
                                && !/add\\s+shipping\\s+address|update\\s+shipping\\s+address/i.test(text);
                        })
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        )
                        .map(candidate => candidate.element)[0] || null;
                }

                function findAddressActionCard(modal) {
                    return Array.from(modal.querySelectorAll('div'))
                        .filter(visible)
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .filter(candidate => {
                            const element = candidate.element;
                            const text = normalize(element.innerText || element.textContent || '');
                            return text.includes(expectedPrefix)
                                && /\\b(Home|Office|Hometown)\\s+Address\\b/i.test(text)
                                && /(?:\\+?88\\s*)?0?1\\d{9}/.test(text)
                                && candidate.rect.width >= 300
                                && candidate.rect.height >= 80
                                && candidate.rect.height <= Math.max(280, window.innerHeight * 0.6);
                        })
                        .sort((first, second) => {
                            const firstHasAction = topRightAction(first.element) ? 0 : 1;
                            const secondHasAction = topRightAction(second.element) ? 0 : 1;
                            const firstArea = first.rect.width * first.rect.height;
                            const secondArea = second.rect.width * second.rect.height;
                            return firstHasAction - secondHasAction || firstArea - secondArea;
                        })
                        .map(candidate => candidate.element)[0] || null;
                }

                function topRightAction(scope) {
                    const scopeRect = scope.getBoundingClientRect();
                    return Array.from(scope.querySelectorAll('svg[class*="ellipsis"], svg[class*="more"], button, span, div'))
                        .filter(visible)
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .filter(candidate => {
                            const centerX = candidate.rect.left + candidate.rect.width / 2;
                            const centerY = candidate.rect.top + candidate.rect.height / 2;
                            return centerX > scopeRect.right - 90
                                && centerY < scopeRect.top + 75
                                && candidate.rect.width <= 80
                                && candidate.rect.height <= 60
                                && !candidate.element.querySelector('input[type="radio"]')
                                && !candidate.element.matches('input[type="radio"]');
                        })
                        .sort((first, second) => actionScore(first) - actionScore(second))
                        .map(candidate => candidate.element)[0] || null;
                }

                function actionScore(candidate) {
                    const element = candidate.element;
                    const descriptor = normalize([
                        element.innerText || element.textContent || '',
                        element.getAttribute('aria-label') || '',
                        element.getAttribute('title') || '',
                        element.getAttribute('class') || ''
                    ].join(' '));
                    const tag = String(element.tagName || '').toLowerCase();
                    let score = candidate.rect.width * candidate.rect.height;
                    if (tag === 'button' || element.getAttribute('role') === 'button') {
                        score -= 100000;
                    }
                    if (/edit|more|menu|ellipsis/i.test(descriptor) || /^[.⋯…]+$/.test(descriptor)) {
                        score -= 50000;
                    }
                    return score;
                }

                function closestClickable(element, stopAt) {
                    let current = element;
                    while (current && current !== stopAt) {
                        if (/^(button|a)$/i.test(current.tagName)
                                || current.getAttribute('role') === 'button'
                                || current.onclick) {
                            return current;
                        }
                        current = current.parentElement;
                    }

                    return null;
                }

                function clickLikeUser(element, clientX, clientY) {
                    const rect = element.getBoundingClientRect();
                    const x = clientX || rect.left + rect.width / 2;
                    const y = clientY || rect.top + rect.height / 2;
                    const clickTargets = [element, closestClickable(element, document.body)]
                        .filter(Boolean)
                        .filter((candidate, index, all) => all.indexOf(candidate) === index);
                    for (const target of clickTargets) {
                        ['pointerdown', 'mousedown', 'pointerup', 'mouseup', 'click'].forEach(type => {
                            target.dispatchEvent(new MouseEvent(type, {
                                bubbles: true,
                                cancelable: true,
                                view: window,
                                clientX: x,
                                clientY: y
                            }));
                        });
                        if (typeof target.click === 'function') {
                            target.click();
                        }
                    }
                }
                """, normalizeText(fullNamePrefix).toLowerCase()));
    }

    private boolean isAddressWithFullNamePrefixDisplayed(String fullNamePrefix) {
        return Boolean.TRUE.equals(executeScript("""
                const expectedPrefix = String(arguments[0] || '').toLowerCase();
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim().toLowerCase();
                const modal = activeAddressListModal();
                if (!modal) {
                    return false;
                }

                return getAddressCards(modal).some(card =>
                    normalize(card.innerText || card.textContent || '').includes(expectedPrefix)
                );

                function activeAddressListModal() {
                    return Array.from(document.querySelectorAll('body > div, [role="dialog"]'))
                        .filter(visible)
                        .filter(element => {
                            const text = normalize(element.innerText || element.textContent || '');
                            return /^address\\b/i.test(text)
                                && /add\\s+new\\s+address/i.test(text)
                                && !/add\\s+shipping\\s+address|update\\s+shipping\\s+address/i.test(text);
                        })
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        )
                        .map(candidate => candidate.element)[0] || null;
                }

                function getAddressCards(modal) {
                    return Array.from(modal.querySelectorAll('div'))
                        .filter(visible)
                        .filter(element => {
                            const text = normalize(element.innerText || element.textContent || '');
                            return /\\baddress\\b/i.test(text)
                                && /(?:\\+?88\\s*)?0?1\\d{9}/.test(text)
                                && element.querySelector('input[type="radio"]');
                        })
                        .filter(element => !Array.from(element.children).some(child => {
                            const text = normalize(child.innerText || child.textContent || '');
                            return visible(child)
                                && /\\baddress\\b/i.test(text)
                                && /(?:\\+?88\\s*)?0?1\\d{9}/.test(text)
                                && child.querySelector('input[type="radio"]');
                        }));
                }
                """, normalizeText(fullNamePrefix).toLowerCase()));
    }

    private void confirmAddressDeletionIfPrompted() {
        try {
            waitUntil(SHORT_ADDRESS_TIMEOUT, webDriver -> clickAddressDeleteConfirmationButton()
                    || !isAddressDeleteConfirmationDialogDisplayed());
        } catch (TimeoutException ignored) {
            // Some environments delete immediately without an extra confirmation dialog.
        }
    }

    private boolean clickAddressDeleteConfirmationButton() {
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
                const dialog = Array.from(document.querySelectorAll('[role="dialog"], body > div'))
                    .filter(visible)
                    .filter(element => {
                        const text = normalize(element.innerText || element.textContent || '');
                        return /delete|remove|are\\s+you\\s+sure/i.test(text)
                            && !(/^Address\\b/i.test(text) && /Add\\s+New\\s+Address/i.test(text));
                    })
                    .map(element => ({ element, rect: element.getBoundingClientRect() }))
                    .sort((first, second) =>
                        (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                    )
                    .map(candidate => candidate.element)[0] || null;
                if (!dialog) {
                    return false;
                }

                const button = Array.from(dialog.querySelectorAll('button'))
                    .filter(visible)
                    .find(candidate => /^(Delete|Remove|Yes|Confirm|OK)$/i.test(
                        normalize(candidate.innerText || candidate.textContent || '')
                    ));
                if (!button || button.disabled) {
                    return false;
                }

                button.scrollIntoView({ block: 'center', inline: 'nearest' });
                button.click();
                return true;
                """));
    }

    private boolean isAddressDeleteConfirmationDialogDisplayed() {
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
                return Array.from(document.querySelectorAll('[role="dialog"], body > div'))
                    .filter(visible)
                    .some(element => {
                        const text = normalize(element.innerText || element.textContent || '');
                        return /delete|remove|are\\s+you\\s+sure/i.test(text)
                            && !(/^Address\\b/i.test(text) && /Add\\s+New\\s+Address/i.test(text));
                    });
                """));
    }

    private boolean clickVisibleExactText(String exactText) {
        return Boolean.TRUE.equals(executeScript("""
                const exactText = String(arguments[0] || '').trim().toLowerCase();
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                const element = Array.from(document.querySelectorAll('button, a, div, span, li'))
                    .filter(visible)
                    .filter(candidate => normalize(candidate.innerText || candidate.textContent)
                        .toLowerCase() === exactText)
                    .sort((first, second) => first.children.length - second.children.length)[0] || null;
                if (!element) {
                    return false;
                }

                element.scrollIntoView({ block: 'center', inline: 'nearest' });
                element.click();
                return true;
                """, exactText));
    }

    private boolean clickAddressRadio(String fullName) {
        return Boolean.TRUE.equals(executeScript("""
                const expectedName = String(arguments[0] || '').toLowerCase();
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim().toLowerCase();
                const modal = activeAddressListModal();
                if (!modal) {
                    return false;
                }

                const card = getAddressCards(modal).find(candidate =>
                    normalize(candidate.innerText || candidate.textContent || '').includes(expectedName)
                ) || null;
                const radio = card ? card.querySelector('input[type="radio"]') : null;
                if (!radio || radio.disabled) {
                    return false;
                }

                radio.scrollIntoView({ block: 'center', inline: 'nearest' });
                if (!radio.checked) {
                    radio.click();
                }
                return true;

                function activeAddressListModal() {
                    return Array.from(document.querySelectorAll('body > div, [role="dialog"]'))
                        .filter(visible)
                        .filter(element => {
                            const text = normalize(element.innerText || element.textContent || '');
                            return /^address\\b/i.test(text)
                                && /add\\s+new\\s+address/i.test(text)
                                && !/add\\s+shipping\\s+address|update\\s+shipping\\s+address/i.test(text);
                        })
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        )
                        .map(candidate => candidate.element)[0] || null;
                }

                function getAddressCards(modal) {
                    return Array.from(modal.querySelectorAll('div'))
                        .filter(visible)
                        .filter(element => {
                            const text = normalize(element.innerText || element.textContent || '');
                            return /\\baddress\\b/i.test(text)
                                && /(?:\\+?88\\s*)?0?1\\d{9}/.test(text)
                                && element.querySelector('input[type="radio"]');
                        })
                        .filter(element => !Array.from(element.children).some(child => {
                            const text = normalize(child.innerText || child.textContent || '');
                            return visible(child)
                                && /\\baddress\\b/i.test(text)
                                && /(?:\\+?88\\s*)?0?1\\d{9}/.test(text)
                                && child.querySelector('input[type="radio"]');
                        }));
                }
                """, normalizeText(fullName).toLowerCase()));
    }

    private boolean clickCheckedOrFirstAddressRadio() {
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
                const modal = activeAddressListModal();
                if (!modal) {
                    return false;
                }

                const radios = Array.from(modal.querySelectorAll('input[type="radio"]'))
                    .filter(visible)
                    .filter(radio => !radio.disabled);
                const radio = radios.find(candidate => candidate.checked) || radios[0] || null;
                if (!radio) {
                    return false;
                }

                radio.scrollIntoView({ block: 'center', inline: 'nearest' });
                if (!radio.checked) {
                    radio.click();
                }
                return true;

                function activeAddressListModal() {
                    return Array.from(document.querySelectorAll('body > div, [role="dialog"]'))
                        .filter(visible)
                        .filter(element => {
                            const text = normalize(element.innerText || element.textContent || '');
                            return /^address\\b/i.test(text)
                                && /add\\s+new\\s+address/i.test(text)
                                && !/add\\s+shipping\\s+address|update\\s+shipping\\s+address/i.test(text);
                        })
                        .map(element => ({ element, rect: element.getBoundingClientRect() }))
                        .sort((first, second) =>
                            (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                        )
                        .map(candidate => candidate.element)[0] || null;
                }
                """));
    }

    private boolean addressCardContains(String fullName, String expectedText) {
        return Boolean.TRUE.equals(executeScript("""
                const expectedName = String(arguments[0] || '').toLowerCase();
                const expectedText = String(arguments[1] || '').toLowerCase();
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim().toLowerCase();
                const modal = Array.from(document.querySelectorAll('body > div, [role="dialog"]'))
                    .filter(visible)
                    .filter(element => {
                        const text = normalize(element.innerText || element.textContent || '');
                        return /^address\\b/i.test(text)
                            && /add\\s+new\\s+address/i.test(text)
                            && !/add\\s+shipping\\s+address|update\\s+shipping\\s+address/i.test(text);
                    })
                    .map(element => ({ element, rect: element.getBoundingClientRect() }))
                    .sort((first, second) =>
                        (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                    )
                    .map(candidate => candidate.element)[0] || null;
                if (!modal) {
                    return false;
                }

                return Array.from(modal.querySelectorAll('div'))
                    .filter(visible)
                    .some(element => {
                        const text = normalize(element.innerText || element.textContent || '');
                        return text.includes(expectedName) && text.includes(expectedText);
                    });
                """, normalizeText(fullName).toLowerCase(), normalizeText(expectedText).toLowerCase()));
    }

    private boolean isFormOpen(String formTitle) {
        return Boolean.TRUE.equals(executeScript("""
                const formTitle = String(arguments[0] || '').trim();
                const visible = element => {
                    const rect = element.getBoundingClientRect();
                    const style = getComputedStyle(element);
                    return rect.width > 0 && rect.height > 0
                        && style.display !== 'none'
                        && style.visibility !== 'hidden'
                        && Number(style.opacity || 1) !== 0;
                };
                const normalize = text => String(text || '').replace(/\\s+/g, ' ').trim();
                const form = Array.from(document.querySelectorAll('body > div, [role="dialog"]'))
                    .filter(visible)
                    .filter(element => normalize(element.innerText || element.textContent || '').includes(formTitle))
                    .map(element => ({ element, rect: element.getBoundingClientRect() }))
                    .sort((first, second) =>
                        (first.rect.width * first.rect.height) - (second.rect.width * second.rect.height)
                    )
                    .map(candidate => candidate.element)[0] || null;
                if (!form) {
                    return false;
                }

                const hasSubmitButton = Array.from(form.querySelectorAll('button'))
                    .filter(visible)
                    .some(button => !button.disabled
                        && /^Submit$|^Save$/i.test(normalize(button.innerText || button.textContent || '')));
                const editableTextFields = Array.from(form.querySelectorAll('input, textarea'))
                    .filter(element => element.type !== 'hidden')
                    .filter(visible)
                    .filter(element => !element.disabled);

                return hasSubmitButton && editableTextFields.length >= 2;
                """, formTitle));
    }

    private boolean isAnyAddressModalOpen() {
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
                return Array.from(document.querySelectorAll('body > div, [role="dialog"]'))
                    .filter(visible)
                    .some(element => {
                        const text = normalize(element.innerText || element.textContent || '');
                        return /(?:Add|Update)\\s+Shipping\\s+Address/i.test(text)
                            || (/^Address\\b/i.test(text)
                                && /Add\\s+New\\s+Address/i.test(text)
                                && !/Shopping\\s+Cart/i.test(text));
                    });
                """));
    }

    private String normalizeText(String text) {
        return text == null ? "" : text.replaceAll("\\s+", " ").trim();
    }

    public record AddressData(String fullName, String phoneNumber, String addressLine) {
    }
}
