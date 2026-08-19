package utils;

import org.aeonbits.owner.Config;

@Config.Sources("classpath:config/config.properties")
public interface ConfigReader extends Config {

    @Key("browser")
    @DefaultValue("chrome")
    String browser();

    @Key("baseUrl")
    String baseUrl();

    @Key("headless")
    @DefaultValue("false")
    boolean headless();

    @Key("pageLoadTimeoutSeconds")
    @DefaultValue("60")
    long pageLoadTimeoutSeconds();

    @Key("validPhoneNumber")
    String validPhoneNumber();

    @Key("invalidShortPhoneNumber")
    @DefaultValue("01913")
    String invalidShortPhoneNumber();

    @Key("nonNumericPhoneNumber")
    @DefaultValue("01913abc!@#")
    String nonNumericPhoneNumber();

    @Key("validOtp")
    @DefaultValue("1234")
    String validOtp();

    @Key("invalidOtp")
    @DefaultValue("1111")
    String invalidOtp();

    @Key("otpExpirySupported")
    @DefaultValue("false")
    boolean otpExpirySupported();

    @Key("otpExpiryTimeoutSeconds")
    @DefaultValue("90")
    long otpExpiryTimeoutSeconds();

    @Key("visualBaselineEnforced")
    @DefaultValue("false")
    boolean visualBaselineEnforced();

    @Key("visualMismatchThresholdPercent")
    @DefaultValue("0.00")
    double visualMismatchThresholdPercent();

    @Key("cartPersistsAfterLogoutLogin")
    @DefaultValue("true")
    boolean cartPersistsAfterLogoutLogin();

    @Key("dynamicProductSearchKeyword")
    @DefaultValue("Vaseline Lip Therapy Cocoa Butter 20g")
    String dynamicProductSearchKeyword();

    @Key("adminBaseUrl")
    @DefaultValue("https://dev.s.arogga.co/adminv2/staging/#/login")
    String adminBaseUrl();

    @Key("adminApiUrl")
    @DefaultValue("https://dev.s.arogga.co/apiv2/automation-testing/admin")
    String adminApiUrl();

    @Key("adminMobileNumber")
    @DefaultValue("01913321842")
    String adminMobileNumber();

    @Key("adminOtp")
    @DefaultValue("123456")
    String adminOtp();
}
