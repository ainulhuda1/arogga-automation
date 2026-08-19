# Arogga Automation Stability Summary Report

Generated: 2026-08-19

Evidence root:
`/Users/Ainul/kazimdshimulbillah/Documents/Automation/arogga-automation/stability-runs/20260819_104818`

Execution rule followed:
The complete suite was executed 5 consecutive times through Maven Surefire using the existing `testng.xml` configuration. Test scope, groups, method includes, execution order configuration, and tests were not modified before or during the stability run.

Command used per run:

```bash
mvn -B clean test
```

## Suite Configuration

`testng.xml`

```xml
<suite name="Arogga Web Automation Suite" verbose="1" parallel="none" thread-count="1">
    <test name="Serial Regression" preserve-order="true">
```

Key findings:

- Parallel execution: none.
- Thread count: 1.
- Preserve order: true.
- Retry analyzer: not configured.
- Invocation count: not configured.
- Data providers: none found for the executed suite path.
- Expected configured skips: OTP expiry tests because `otpExpirySupported=false`.
- One XML-included test is disabled in code: `verifyDynamicCartDrawerRegressionAfterAddingProduct`.
- Observed order note: `VisualRegressionTest` executed before `ShipmentProcessingTest` in all 5 runs, although `testng.xml` lists `ShipmentProcessingTest` before `VisualRegressionTest`. Shipment still ran after admin confirmation because it depends on the `admin-order-confirmation` group.

## Runtime Environment

| Item | Value |
|---|---|
| OS | macOS 26.4.1 aarch64 |
| Shell location | `/Users/Ainul/kazimdshimulbillah/Documents/Automation/arogga-automation` |
| Maven | Apache Maven 3.9.16 |
| Maven runtime Java | Java 26.0.1 Homebrew |
| `java -version` on shell | OpenJDK 17.0.19 |
| Selenium | 4.34.0 |
| TestNG | 7.11.0 |
| WebDriverManager | 6.1.0 |
| Browser | Google Chrome 151.0.7922.138 |
| Driver | chromedriver 151.0.7922.138 |
| Driver path | `/Users/Ainul/kazimdshimulbillah/.cache/selenium/chromedriver/mac-arm64/151.0.7922.138/chromedriver` |

Repeated browser/driver warning:

```text
Unable to find CDP implementation matching 151
Unable to find version of CDP to use for 151.0.7922.138
```

Warning counts:

| Run | CDP mismatch warning count |
|---|---:|
| 1 | 76 |
| 2 | 74 |
| 3 | 74 |
| 4 | 74 |
| 5 | 74 |

## Run Summary

| Run | Start | End | Duration | Total | Pass | Fail | Skip | Maven Exit |
|---|---|---|---:|---:|---:|---:|---:|---:|
| 1 | 2026-08-19T10:48:18+06:00 | 2026-08-19T11:20:57+06:00 | 1959s | 71 | 68 | 1 | 2 | 1 |
| 2 | 2026-08-19T11:20:57+06:00 | 2026-08-19T11:42:20+06:00 | 1283s | 71 | 69 | 0 | 2 | 0 |
| 3 | 2026-08-19T11:42:20+06:00 | 2026-08-19T12:11:53+06:00 | 1773s | 71 | 69 | 0 | 2 | 0 |
| 4 | 2026-08-19T12:11:53+06:00 | 2026-08-19T12:32:36+06:00 | 1243s | 71 | 69 | 0 | 2 | 0 |
| 5 | 2026-08-19T12:32:36+06:00 | 2026-08-19T12:51:56+06:00 | 1160s | 71 | 68 | 1 | 2 | 1 |

Summary:

- Full suite pass rate: 3/5 = 60.00%.
- Executable method pass rate, excluding configured skips: 343/345 = 99.42%.
- Pass rate including configured skips: 343/355 = 96.62%.
- Stable executable methods: 67/69 = 97.10%.
- Flaky tests: 2.
- Consistently failed tests: 0.
- Config-skipped tests: 2.

## Per-Run Failure Summary

### Run 1

PASS = 68  
FAIL = 1  
SKIP = 2

Failed test:

`tests.user.AddProductToCartTest.verifyAddProductToCartFromSearchResultAfterLoginAndSearch`

Failure category:
DATA_DEPENDENT / dynamic product availability / product-details probing timeout.

Exact failure:

```text
org.openqa.selenium.TimeoutException:
No purchasable product details page was found for search terms: [Vicks, Napa, a]
```

Root cause from stack:

```text
Caused by: org.openqa.selenium.TimeoutException:
Stopped product-details probing after 6 search-result candidates for keyword 'a'.
Tried products: [Atova 10, Alatrol 10, Alcet, Clopid-AS, Angilock 50, ATV 10]
```

Stack trace head:

```text
at tests.user.AddProductToCartTest.addProductDetailsFromAvailableSearchTerm(AddProductToCartTest.java:685)
at tests.user.AddProductToCartTest.verifyAddProductToCartFromSearchResultAfterLoginAndSearch(AddProductToCartTest.java:580)
Caused by: org.openqa.selenium.TimeoutException
at pages.user.AddProductToCartPage.selectFirstProductWithWorkingQuantitySelector(AddProductToCartPage.java:264)
at tests.user.AddProductToCartTest.addProductDetailsFromAvailableSearchTerm(AddProductToCartTest.java:653)
```

Screenshot:

`/Users/Ainul/kazimdshimulbillah/Documents/Automation/arogga-automation/stability-runs/20260819_104818/run-1/screenshots/verifyAddProductToCartFromSearchResultAfterLoginAndSearch_20260819_111430_182.png`

### Run 2

PASS = 69  
FAIL = 0  
SKIP = 2

No failed tests.

### Run 3

PASS = 69  
FAIL = 0  
SKIP = 2

No failed tests.

### Run 4

PASS = 69  
FAIL = 0  
SKIP = 2

No failed tests.

### Run 5

PASS = 68  
FAIL = 1  
SKIP = 2

Failed test:

`tests.user.AddProductToCartTest.verifyQuantitySelectionAndRemoveProduct`

Failure category:
ENVIRONMENTAL / browser-driver session loss.

Exact failure:

```text
org.openqa.selenium.NoSuchSessionException:
invalid session id: session deleted as the browser has closed the connection
from disconnected: not connected to DevTools
(Session info: chrome=151.0.7922.138)
```

Stack trace head:

```text
at pages.user.BasePage.executeScript(BasePage.java:230)
at pages.user.BasePage.lambda$waitForPageLoad$0(BasePage.java:58)
at pages.user.BasePage.waitForPageLoad(BasePage.java:58)
at pages.user.CartPage.openCartDrawer(CartPage.java:28)
at pages.user.CartPage.lambda$waitForCartToBecomeEmptyAfterRemoval$0(CartPage.java:3482)
at pages.user.CartPage.waitForCartToBecomeEmptyAfterRemoval(CartPage.java:3481)
at pages.user.CartPage.removeProductAndWaitUntilCartIsEmpty(CartPage.java:2061)
at tests.user.AddProductToCartTest.verifyQuantitySelectionAndRemoveProduct(AddProductToCartTest.java:413)
```

Screenshot:
Not available. Screenshot capture was skipped because the WebDriver session was already invalid.

## Configured Skips

These two tests skipped in every run by configuration:

| Test | Reason |
|---|---|
| `tests.user.LoginTest.verifyExpiredOtpShowsProperValidationMessage` | `OTP expiry is disabled for this test environment.` |
| `tests.user.OtpTest.verifyOtpExpiry` | `OTP expiry is disabled for this test environment.` |

Source setting:

```properties
otpExpirySupported=false
```

## Flaky Test Matrix

| Test | Run 1 | Run 2 | Run 3 | Run 4 | Run 5 | Stability |
|---|---|---|---|---|---|---|
| `AddProductToCartTest.verifyAddProductToCartFromSearchResultAfterLoginAndSearch` | FAIL | PASS | PASS | PASS | PASS | FLAKY / DATA_DEPENDENT |
| `AddProductToCartTest.verifyQuantitySelectionAndRemoveProduct` | PASS | PASS | PASS | PASS | FAIL | FLAKY / ENVIRONMENTAL |

All other 67 executed methods passed in all 5 runs.

## Flaky Test Root Causes

### 1. `verifyAddProductToCartFromSearchResultAfterLoginAndSearch`

Actual root cause:
The test depends on live dynamic search results and broad fallback products instead of deterministic fixture-backed test data.

Evidence:

- Configured dynamic search keyword: `dynamicProductSearchKeyword=Vicks`.
- Runtime fallback keywords: `Vicks`, `Napa`, `a`.
- Test data files are empty:
  - `src/test/resources/testdata/products.json`
  - `src/test/resources/testdata/users.json`
- Run 1 failed only after exhausting broad keyword `a`.
- Failed candidates in run 1: `Atova 10`, `Alatrol 10`, `Alcet`, `Clopid-AS`, `Angilock 50`, `ATV 10`.
- Same test passed in runs 2-5 with high duration variance:
  - Run 1: 715.771s, failed.
  - Run 2: 117.219s, passed.
  - Run 3: 593.967s, passed.
  - Run 4: 6.066s, passed.
  - Run 5: 7.149s, passed.

Relevant code:

- `AddProductToCartTest.addProductDetailsFromAvailableSearchTerm()`
- `AddProductToCartTest.availableProductSearchKeywords()`
- `AddProductToCartPage.selectFirstProductWithWorkingQuantitySelector()`
- `AddProductToCartPage.tryOpenQuantitySelectorFor()`

Specific issue:
`tryOpenQuantitySelectorFor()` catches `ElementClickInterceptedException`, `StaleElementReferenceException`, and `TimeoutException`, then returns `false`. That preserves test flow but loses the exact candidate-level reason.

Classification:
DATA_DEPENDENT with automation diagnostic gap.

Ownership:
Test data + automation code. Live backend product availability contributes.

### 2. `verifyQuantitySelectionAndRemoveProduct`

Actual root cause:
The browser session was lost mid-test while Selenium was polling page readiness after product removal.

Evidence:

- Failure type: `NoSuchSessionException`.
- Command at failure: `executeScript {script=return document.readyState}`.
- Browser/driver: Chrome 151.0.7922.138, chromedriver 151.0.7922.138.
- Selenium: 4.34.0.
- Repeated CDP mismatch warnings appeared in every run.
- No screenshot was captured because the browser session was already invalid.
- No leftover chromedriver or remote-debugging Chrome process was recorded after each run.

Relevant code:

- `AddProductToCartTest.verifyQuantitySelectionAndRemoveProduct()`
- `CartPage.removeProductAndWaitUntilCartIsEmpty()`
- `CartPage.waitForCartToBecomeEmptyAfterRemoval()`
- `CartPage.openCartDrawer()`
- `BasePage.waitForPageLoad()`

Classification:
ENVIRONMENTAL / browser-driver stability.

Ownership:
Automation environment and browser-driver compatibility. There is no evidence this was an application assertion defect.

## Failure Category Checklist

| Category | Evidence |
|---|---|
| Timeout/wait issue | Yes, run 1 product-details probing timed out. |
| Stale element issue | No final failure caused by stale element. Stale is caught during product probing. |
| Element not found | No final `NoSuchElementException`. |
| Element click intercepted | Not a final failure; caught during candidate probing. |
| Assertion mismatch | None. |
| Test data issue | Yes, run 1 depends on live product availability and empty product fixture data. |
| API/backend issue | Possible contributor for product availability, but no direct failing API response was captured. |
| Environment/network issue | Yes, run 5 browser session loss. |
| Order/dependency issue | Admin/shipment flow is intentionally dependent on generated order state, but did not fail in these 5 runs. |
| Database/state issue | Possible product/order state dependency; no direct DB evidence. |
| Concurrency issue | No. Suite is serial. |
| Parallel execution issue | No. `parallel="none"` and `thread-count="1"`. |

## Test Independence Findings

Browser/session:

- `BaseTest` creates a driver in `@BeforeMethod` and quits in `@AfterMethod`.
- `AddProductToCartTest` overrides lifecycle with class setup and method-level `restartBrowserAtBaseUrl()`.
- Cart cleanup runs before and after AddProductToCart methods.
- Run-to-run independence was preserved with `mvn clean test`.

Shared state:

- `TestContext` uses ThreadLocal plus static shared map plus `target/generated-test-context.properties`.
- `OrderTest` writes `generatedOrderNumber`.
- `AdminOrderConfirmationTest` reads generated order state.
- `ShipmentProcessingTest` reads generated order state and depends on the `admin-order-confirmation` group.

Conclusion:
Most tests are isolated enough for serial execution. The order/admin/shipment chain is intentionally order/state dependent and should be treated as a scenario chain, not independent tests.

## Synchronization Findings

- `Thread.sleep()` count in `src/test/java` and `src/test/resources`: 0.
- Implicit wait count: 0.
- Retry analyzer count: 0.
- `pageLoadStrategy` configuration count: 0.
- Explicit waits are used heavily.
- Main unstable synchronization path is dynamic product probing and cart drawer verification around live product state.

## Data Stability Findings

Unstable data inputs:

- Fixed shared user phone number.
- Fixed admin user.
- Dynamic live product keyword: `Vicks`.
- Fallback keywords: `Napa`, `a`.
- Empty product/user fixture JSON files.
- Generated live orders:
  - Run 1: `5436499`, shipment `5436499A`
  - Run 2: `5436507`, shipment `5436507A`
  - Run 3: `5436510`, shipment `5436510A`
  - Run 4: `5436512`, shipment `5436512A`
  - Run 5: `5436513`, shipment `5436513A`

Generated order/admin/shipment flow passed in all 5 runs.

## Reporting and Screenshots

Reports and logs per run:

- `run-N/maven.log`
- `run-N/surefire-reports/TEST-TestSuite.xml`
- `run-N/surefire-reports/TestSuite.txt`
- `run-N/test-output/ExtentReport.html`, where available
- `run-N/screenshots`, where screenshots were generated
- `run-N/generated-test-context.properties`

Failure screenshots:

- Run 1 failure screenshot available.
- Run 5 failure screenshot unavailable due invalid browser session.

## Permanent Stabilization Plan

### P0 - Stabilize product test data

File/class:
`src/test/resources/testdata/products.json`, `AddProductToCartTest`, `OrderTest`, `AddProductToCartPage`

Current problem:
Dynamic product tests rely on live search results and broad fallback keyword `a`.

Why it causes instability:
Search ranking, stock, product type, pack availability, and product-details ADD behavior can change between runs.

Exact recommended change:
Seed or reserve deterministic automation products through API/admin setup, then make cart/order tests use those known product IDs/names. Fail with a clear precondition error or skip only when fixture setup is unavailable.

Expected benefit:
Removes the confirmed data-dependent product flake.

Risk:
Requires coordination with backend/test-data ownership.

### P0 - Preserve candidate-level product probe failure reasons

File/class:
`src/test/java/pages/user/AddProductToCartPage.java`

Current problem:
`tryOpenQuantitySelectorFor()` catches multiple Selenium exceptions and returns `false` without recording the exact reason.

Why it causes instability:
The test eventually fails with a generic product-probing timeout, making root cause slower to diagnose.

Exact recommended change:
Record each rejected candidate with reason: missing ADD button, click intercepted, stale element, quantity dialog missing, detail page inactive, or cart confirmation failure.

Expected benefit:
Future failures identify the exact UI/data condition instead of only listing tried products.

Risk:
Low. Mostly logging/diagnostic change.

### P1 - Align Chrome/Selenium/CDP compatibility

File/class:
`pom.xml`, CI browser image

Current problem:
Every run logs CDP mismatch warnings for Chrome 151 with Selenium 4.34.0.

Why it causes instability:
The run 5 failure was a DevTools/browser-session disconnect. The mismatch is not proven as the only cause, but it weakens browser-session reliability and diagnostics.

Exact recommended change:
Use a Chrome version supported by the Selenium version in CI, or add the matching Selenium DevTools artifact if available for the active Chrome major version.

Expected benefit:
Reduces browser-driver/session instability risk.

Risk:
Medium. Browser pinning must be maintained.

### P1 - Make headless mode actually configurable

File/class:
`src/test/java/drivers/DriverFactory.java`

Current problem:
`DriverFactory` hard-codes `HEADLESS = false`, while `config.properties` also contains `headless=false`.

Why it causes instability:
CI/CD usually needs deterministic headless execution. Current driver options ignore runtime config.

Exact recommended change:
Use `config.headless()` when adding `--headless=new` and `--window-size=1440,1200`.

Expected benefit:
Local and CI browser behavior becomes reproducible.

Risk:
Medium. Visual/layout-sensitive tests may need baseline updates.

### P1 - Treat order/admin/shipment as an explicit scenario chain

File/class:
`OrderTest`, `AdminOrderConfirmationTest`, `ShipmentProcessingTest`, `TestContext`

Current problem:
Admin and shipment tests depend on a generated order from an earlier UI checkout flow.

Why it causes instability:
If checkout order creation fails, downstream admin/shipment tests become dependent skips/failures rather than independent signals.

Exact recommended change:
Document this as a scenario chain. Allow `ADMIN_ORDER_ID` injection for independent admin/shipment execution. Clear shared context at suite boundaries.

Expected benefit:
Cleaner failure attribution and better CI partitioning.

Risk:
Medium. Requires suite design decision.

### P2 - Enforce or reclassify visual regression

File/class:
`VisualRegressionTest`, `VisualRegressionUtils`, `config.properties`

Current problem:
`visualBaselineEnforced=false` and no baseline images exist.

Why it causes instability or false confidence:
The visual test passes when the baseline is missing.

Exact recommended change:
Add approved baselines and enforce them in CI, or rename this as screenshot capture/smoke coverage rather than visual regression.

Expected benefit:
Visual test result becomes meaningful.

Risk:
Medium. Visual baselines require stable environment and viewport.

## CI/CD Readiness

Answer:
NO.

Reason:
The complete suite passed only 3 of 5 consecutive full executions. There are two confirmed flaky tests:

- One data-dependent dynamic product-details failure.
- One environment/browser-driver session loss.

Minimum fixes before merge-blocking CI/CD:

1. Stabilize product test data and product selection.
2. Improve product-probe diagnostics.
3. Align Chrome/Selenium/CDP versions.
4. Make headless execution deterministic for CI.
5. Treat order/admin/shipment as a documented scenario chain or provide injected fixtures.

Retries should not be used as the permanent solution because the observed failures expose real data and environment stability problems.
