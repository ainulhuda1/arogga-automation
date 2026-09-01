# Arogga Automation Framework

## 📌 Project Overview

This is an automated testing framework for the Arogga application.

The framework is built using:

* Java 17
* Selenium WebDriver
* TestNG
* Maven
* Page Object Model (POM)
* WebDriverManager
* Allure Report
* Git & GitHub
* GitHub Actions for CI/CD

---

# 💻 Complete Setup Guide

This guide is intended for a **fresh/clean PC where no development tools are installed**.

Follow the steps below in order.

---

# 1. Install Git

Git is required to clone and manage the automation project.

### Check if Git is already installed

Open Terminal and run:

```bash
git --version
```

If Git is not installed, install it from:

https://git-scm.com/downloads

After installation, verify:

```bash
git --version
```

Example:

```text
git version 2.x.x
```

---

# 2. Install Java 17

This project requires **Java 17**.

### Check Java

```bash
java -version
```

If Java is not installed, install Java 17 from:

https://adoptium.net/temurin/releases/?version=17

Choose:

* Version: 17
* Operating System: Your OS
* Architecture: Your PC architecture
* JDK package

Complete the installation.

### Verify Java

Restart Terminal and run:

```bash
java -version
```

Expected:

```text
openjdk version "17.x.x"
```

Also verify:

```bash
javac -version
```

Expected:

```text
javac 17.x.x
```

---

# 3. Install Maven

Maven is required to build the project and manage dependencies.

### Check Maven

```bash
mvn -version
```

If Maven is not installed, download it from:

https://maven.apache.org/download.cgi

Install Maven according to your operating system.

After installation, restart Terminal and run:

```bash
mvn -version
```

Expected output should contain:

```text
Apache Maven 3.x.x
Java version: 17.x.x
```

---

# 4. Install IntelliJ IDEA

IntelliJ IDEA is recommended for development and running/debugging automation tests.

Download IntelliJ IDEA from:

https://www.jetbrains.com/idea/download/

Install IntelliJ IDEA.

The free/community functionality is sufficient for this project unless your project requires an Ultimate-only feature.

---

# 5. Install Google Chrome

The automation framework uses Google Chrome for browser automation.

Download Chrome from:

https://www.google.com/chrome/

Install Google Chrome.

Verify that Chrome opens successfully.

### Important

You do **not** need to manually download ChromeDriver if the project uses WebDriverManager.

WebDriverManager will automatically manage the appropriate browser driver.

---

# 6. Configure Git

Check Git configuration:

```bash
git config --global user.name
git config --global user.email
``

If they are not configured, set them:

```bash
git config --global user.name "Your Name"
git config --global user.email "your-email@example.com"
```

Example:

```bash
git config --global user.name "QA Engineer"
git config --global user.email "qa@example.com"
```

---

# 7. Clone the Automation Project

Open Terminal and navigate to the location where you want to keep the project.

Example:

```bash
cd ~/Documents
```

Clone the repository:

```bash
git clone https://github.com/ainulhuda1/arogga-automation.git
```

Enter the project directory:

```bash
cd arogga-automation
```

Verify the project files:

```bash
ls
```

You should see files/folders similar to:

```text
src
pom.xml
testng.xml
.github
README.md
```

---

# 8. Open the Project in IntelliJ IDEA

Open IntelliJ IDEA.

Select:

```text
File → Open
```

Select the cloned:

```text
arogga-automation
```

project folder.

IntelliJ should automatically detect:

```text
pom.xml
```

as a Maven project.

Allow IntelliJ to:

* Import Maven project
* Download Maven dependencies
* Index the project

Wait until the Maven dependencies finish downloading.

---

# 9. Configure Project Environment

Before running the automation, configure the required environment settings.

Depending on the project configuration, you may need to provide:

* Application URL
* Test username
* Test password
* API credentials
* Environment name
* Other required configuration values

### ⚠️ Security

Do **NOT** commit passwords, API keys, tokens, or other secrets to GitHub.

Use the project's configured environment variables or local configuration mechanism.

Example:

```text
BASE_URL
USERNAME
PASSWORD
API_TOKEN
```

If the project contains a sample configuration file, copy it and update the values locally.

Example:

```bash
cp .env.example .env
```

Then update the local `.env` file.

> Note: Use the actual configuration method implemented in this project.

---

# 10. Install Project Dependencies

Open Terminal inside the project directory:

```bash
cd arogga-automation
```

Run:

```bash
mvn clean install
```

Maven will:

1. Clean previous build files
2. Download required dependencies
3. Compile the project
4. Run the configured tests
5. Generate the build output

If everything is successful, you should see:

```text
BUILD SUCCESS
```

---

# 11. Run Automation Tests

To run the automation test suite:

```bash
mvn test
```

Or use the configured TestNG suite from IntelliJ IDEA.

---

# 12. Run Tests from IntelliJ IDEA

You can also run tests directly from IntelliJ.

### TestNG

Open the required test class.

Right-click the test class and select:

```text
Run
```

For the complete TestNG suite, open:

```text
testng.xml
```

and run the configured suite.

---

# 13. Run a Specific Test Class

To run a specific Maven test class:

```bash
mvn -Dtest=TestClassName test
```

Example:

```bash
mvn -Dtest=LoginTest test
```

Replace `LoginTest` with the required test class name.

---

# 14. Run a Specific Test Method

To run a specific test method:

```bash
mvn -Dtest=TestClassName#testMethodName test
```

Example:

```bash
mvn -Dtest=LoginTest#verifyGoogleLoginButton test
```

---

# 15. Test Execution Flow

The automation framework follows the application testing flow configured in the project.

Typical flow:

```text
Web Application
      ↓
Place Order
      ↓
Admin Panel
      ↓
Process / Approve Order
      ↓
Mobile Application
      ↓
Validate Order
      ↓
API Validation
```

The exact execution flow depends on the configured TestNG suite.

---

# 16. Screenshots

Screenshots for failed tests may be stored under:

```text
target/screenshots/
```

Check this folder when a test fails.

Example:

```text
target/
└── screenshots/
```

---

# 17. Reports

After test execution, check the generated report/output directories.

Common Maven output:

```text
target/
```

TestNG reports may be available under:

```text
test-output/
```

If Allure is configured, generate/view the Allure report using the project's configured Allure commands.

---

# 18. Clean Project

To remove previous build/test output:

```bash
mvn clean
```

Then run:

```bash
mvn test
```

---

# 19. Update the Project

Before starting work, always pull the latest code:

```bash
git pull origin main
```

After making changes:

```bash
git status
```

Then:

```bash
git add .
git commit -m "Update automation tests"
git push origin main
```

---

# 20. GitHub Actions / CI-CD

The project contains GitHub Actions workflow configuration under:

```text
.github/workflows/
```

The automation tests can be executed through GitHub Actions according to the configured workflow.

Typical flow:

```text
Developer pushes code
        ↓
GitHub Repository
        ↓
GitHub Actions
        ↓
Build Project
        ↓
Install Dependencies
        ↓
Run Automation Tests
        ↓
Generate Test Results
```

Check the **Actions** tab of the GitHub repository to view execution status and logs.

---

# 21. Troubleshooting

## Maven command not found

If you see:

```text
command not found: mvn
```

Maven is either not installed or not configured in the system PATH.

Verify:

```bash
mvn -version
```

---

## Java command not found

If you see:

```text
command not found: java
```

Install Java 17 and configure the system PATH/JAVA_HOME.

Verify:

```bash
java -version
```

---

## Wrong Java Version

Check:

```bash
java -version
```

The project requires:

```text
Java 17
```

If another Java version is active, configure Java 17 as the default JDK.

---

## ChromeDriver Error

If ChromeDriver errors occur:

1. Check that Google Chrome is installed.
2. Check the Chrome version.
3. Make sure WebDriverManager is configured correctly.
4. Try:

```bash
mvn clean test
```

Do not manually add a ChromeDriver binary unless the project specifically requires it.

---

## Tests Fail Because of Credentials

Check that all required environment variables/configuration values are correctly configured.

Never commit credentials to GitHub.

---

## Dependency Problems

Try:

```bash
mvn clean install
```

If necessary:

```bash
mvn clean test
```

---

# 22. Quick Setup

For an already configured PC, the basic setup is:

```bash
git clone https://github.com/ainulhuda1/arogga-automation.git

cd arogga-automation

mvn clean install

mvn test
```

---

# 23. Required Software

| Software      | Required                        |
| ------------- | ------------------------------- |
| Git           | Yes                             |
| Java 17 JDK   | Yes                             |
| Maven         | Yes                             |
| IntelliJ IDEA | Recommended                     |
| Google Chrome | Yes                             |
| ChromeDriver  | No, if WebDriverManager is used |

---

# 24. Final Verification

Before considering the setup complete, verify:

```bash
git --version
java -version
javac -version
mvn -version
```

Then:

```bash
cd arogga-automation
mvn clean test
```

Expected result:

```text
BUILD SUCCESS
```

If the project builds successfully and the configured automation tests execute successfully, the local setup is complete.

---

# 👨‍💻 Maintainer

**Arogga Automation Team**

Repository:

`arogga-automation`

For project-specific issues, contact the automation/QA team.
