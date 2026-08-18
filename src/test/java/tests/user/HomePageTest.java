package tests.user;

import base.BaseTest;
import constants.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;
import pages.user.HomePage;

import java.util.List;

public class
HomePageTest extends BaseTest {

    @Test(groups = {TestGroups.HOME, TestGroups.REGRESSION},
            description = "Verify Home page loads")
    public void verifyHomePageLoads() {
        HomePage homePage = new HomePage(driver).waitUntilLoaded();

        Assert.assertTrue(homePage.isHomePageLoaded(), "Home page should load successfully");
    }

    @Test(groups = {TestGroups.HOME, TestGroups.REGRESSION},
            description = "Verify header logo")
    public void verifyHeaderLogo() {
        HomePage homePage = new HomePage(driver).waitUntilLoaded();

        Assert.assertTrue(homePage.isHeaderLogoDisplayed(), "Header logo should be displayed");
        Assert.assertTrue(homePage.isHeaderLogoLoaded(), "Header logo image should load");
    }

    @Test(groups = {TestGroups.HOME, TestGroups.REGRESSION},
            description = "Verify profile avatar")
    public void verifyProfileAvatar() {
        HomePage homePage = loginWithValidCredentials();

        Assert.assertTrue(homePage.isUserProfileAvatarDisplayed(), "Profile avatar should be displayed after login");
    }

   /* @Test(groups = {TestGroups.HOME, TestGroups.REGRESSION},
            description = "Verify all banners/images")
    public void verifyAllBannersImages() {
        HomePage homePage = new HomePage(driver).waitUntilLoaded();

        Assert.assertTrue(homePage.areBannerImagesDisplayed(), "Home page should display banner images");
        Assert.assertTrue(homePage.hasNoBrokenVisibleImages(), "Visible home images should not be broken");
    } */

    @Test(groups = {TestGroups.HOME, TestGroups.REGRESSION},
            description = "Verify category icons")
    public void verifyCategoryIcons() {
        HomePage homePage = new HomePage(driver).waitUntilLoaded();

        Assert.assertTrue(homePage.areCategoryIconsDisplayed(), "Category icons/images should be displayed");
    }

    @Test(groups = {TestGroups.HOME, TestGroups.REGRESSION},
            description = "Verify footer icons")
    public void verifyFooterIcons() {
        HomePage homePage = new HomePage(driver).waitUntilLoaded();

        Assert.assertTrue(homePage.areFooterIconsDisplayed(), "Footer icons should be displayed");
    }

    /*@Test(groups = {TestGroups.HOME, TestGroups.REGRESSION},
            description = "Verify no broken images")
    public void verifyNoBrokenImages() {
        HomePage homePage = new HomePage(driver).waitUntilLoaded();
        List<String> brokenImages = homePage.getBrokenVisibleImageSources();

        Assert.assertTrue(brokenImages.isEmpty(), "Broken visible images found: " + brokenImages);
    } */

    @Test(groups = {TestGroups.HOME, TestGroups.REGRESSION},
            description = "Verify all navigation icons")
    public void verifyAllNavigationIcons() {
        HomePage homePage = new HomePage(driver).waitUntilLoaded();

        Assert.assertTrue(homePage.areNavigationIconsDisplayed(), "Navigation icons should be displayed");
    }
}
