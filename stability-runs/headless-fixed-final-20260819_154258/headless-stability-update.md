# Headless Stability Update

Generated: 2026-08-19, Asia/Dhaka

## Scope

- Suite: `testng.xml`
- Maven command per run: `mvn -B -Dheadless=true clean test`
- Execution mode: full suite, serial TestNG execution
- TestNG config: `parallel="none"`, `thread-count="1"`, `preserve-order="true"`
- Browser/driver: Chrome `151.0.7922.138`, ChromeDriver `151.0.7922.138`
- Selenium: `4.34.0`

## Final 3-Run Headless Evidence

| Run | Start | End | Duration | Total | Passed | Failed | Skipped | Exit |
| --- | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| 1 | 2026-08-19T15:42:58+06:00 | 2026-08-19T16:03:53+06:00 | 1255s | 71 | 69 | 0 | 2 | 0 |
| 2 | 2026-08-19T16:03:53+06:00 | 2026-08-19T16:24:05+06:00 | 1212s | 71 | 69 | 0 | 2 | 0 |
| 3 | 2026-08-19T16:24:05+06:00 | 2026-08-19T16:44:13+06:00 | 1208s | 71 | 69 | 0 | 2 | 0 |

Skipped tests were consistent and expected from config behavior:

- `tests.user.LoginTest.verifyExpiredOtpShowsProperValidationMessage`
- `tests.user.OtpTest.verifyOtpExpiry`

Failure-signature scan across all 3 final logs:

- `Test failed`: 0
- `BUILD FAILURE`: 0
- `NoSuchSessionException` / `invalid session`: 0
- `No purchasable`: 0
- Selenium timeout/assertion failure signatures: 0

## Stability Result

- Executed test stability: `69/69` passed in all 3 headless runs = `100%`
- Total configured test stability including expected skips: `69 passed / 71 configured`
- Stable tests: `69`
- Flaky tests after fix: `0`
- Consistently failed tests after fix: `0`
- Consistently skipped tests: `2`

Full matrix: `headless-stability-matrix.csv`

## Fixed Root Causes

### 1. Product search add-to-cart flake

Affected test:

- `tests.user.AddProductToCartTest.verifyAddProductToCartFromSearchResultAfterLoginAndSearch`

Original evidence:

- Pre-fix 5-run report had intermittent timeout: `No purchasable product details page was found for search terms: [Vicks, Napa, a]`.

Root cause:

- Test data/catalog instability. The fallback keyword `a` was too broad and could land on unavailable or unsuitable dynamic products.
- Product probing did not preserve enough per-product failure detail, making diagnosis harder.

Permanent fix applied:

- Stable exact product keyword: `Vaseline Lip Therapy Cocoa Butter 20g`.
- Removed broad fallback keyword `a`.
- Product probing now records exact candidate failure reasons.

Evidence after fix:

- Passed in final run 1, run 2, and run 3.

Key files:

- `src/test/resources/config/config.properties`
- `src/test/java/tests/user/AddProductToCartTest.java`
- `src/test/java/pages/user/AddProductToCartPage.java`
- `src/test/java/tests/user/OrderTest.java`
- `src/test/java/tests/user/SearchPageTest.java`

### 2. Cart removal invalid browser session flake

Affected test:

- `tests.user.AddProductToCartTest.verifyQuantitySelectionAndRemoveProduct`

Original evidence:

- Pre-fix 5-run report had intermittent `NoSuchSessionException: invalid session id: session deleted as the browser has closed the connection` during cart-empty wait.

Root cause:

- Automation synchronization/framework issue. The cart-empty wait repeatedly reopened the cart drawer and executed extra JavaScript/page interactions during polling.
- That amplified Chrome session instability in the final removal assertion path.

Permanent fix applied:

- `waitForCartToBecomeEmptyAfterRemoval()` now opens the cart drawer only once if needed, then waits on the actual empty-cart condition.
- Fallback cleanup still exists, but the normal wait path is now stable and lower impact.

Evidence after fix:

- Passed in final run 1, run 2, and run 3.

Key file:

- `src/test/java/pages/user/CartPage.java`

### 3. Third-party diagnostic JavaScript noise

Affected diagnostic failure:

- `tests.user.AddProductToCartTest.verifyDynamicSearchResultLoadsWithAvailableProductAndNoErrors`

Evidence:

- A diagnostic headless run failed only on Microsoft Clarity script noise from `https://scripts.clarity.ms/.../clarity.js`.
- The app search result itself loaded valid product data.

Root cause:

- Environment/third-party telemetry noise, not application behavior.

Permanent fix applied:

- Browser diagnostics now ignores known analytics/telemetry URLs while still reporting application/API JavaScript errors.

Evidence after fix:

- Passed in final run 1, run 2, and run 3.

Key file:

- `src/test/java/utils/BrowserDiagnosticsUtils.java`

## Other Stability Fixes Applied

- Headless execution now respects `-Dheadless=true` in `DriverFactory`.
- Chrome headless run uses `--headless=new` and stable window size.
- Added Chrome background throttling prevention flags.
- Search UI alignment check accepts actual header SVG/search icon variants.
- Search UI overlap check ignores valid parent-child containment, avoiding false overlap reports.

## Remaining Risk

P1:

- Selenium logs repeated warning: `Unable to find CDP implementation matching 151`.
- `selenium-devtools-v151:4.34.0` is not available from Maven Central, so this was not added.
- Recommendation: pin Chrome to a Selenium-supported CDP version in CI, or upgrade Selenium when v151 devtools support is available.

P1:

- The suite still depends on a live app/backend/admin/shipment environment.
- Order ids generated during final validation: `5436517`, `5436539`, `5436543`.
- Recommendation: reserve CI test accounts/data and keep live catalog/admin/shipment state isolated from manual or production-like usage.

P2:

- Two OTP expiry tests are consistently skipped by current config behavior.
- Recommendation: enable only when OTP expiry behavior is supported and deterministic in the environment.

## CI/CD Readiness

Answer: **YES WITH CONDITIONS**

The automation suite is stable enough to run in CI/CD as a controlled serial headless job based on the final 3-run evidence. Before treating it as a hard release gate, fix or control these conditions:

- Pin/align Chrome + Selenium CDP compatibility.
- Keep the test environment and test data isolated.
- Keep the current serial TestNG execution unless the framework is separately validated for parallel execution.
- Do not add retry analyzers as a substitute for real failures.

## Artifacts

- Run 1 log: `run-1/maven.log`
- Run 2 log: `run-2/maven.log`
- Run 3 log: `run-3/maven.log`
- Run 1 Surefire: `run-1/surefire-reports`
- Run 2 Surefire: `run-2/surefire-reports`
- Run 3 Surefire: `run-3/surefire-reports`
- Full matrix CSV: `headless-stability-matrix.csv`
