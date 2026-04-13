package youtube;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class YouTubeLikeTest {

    private static final String SEARCH_TEXT = "Генрих 8";

    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeEach
    void setUp() {
        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("debuggerAddress", "127.0.0.1:9222");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(2));
    }

    @AfterEach
    void tearDown() {
        // Ничего не делаем специально:
        // браузер был открыт вручную, пусть остаётся открытым после теста.
    }

    @Test
    void searchVideoAndSetLike() {
        driver.get("https://www.youtube.com/?hl=en");

        acceptCookiesIfPresent();
        waitForPageLoaded();
        assertSignedIn();

        searchForVideo(SEARCH_TEXT);

        wait.until(ExpectedConditions.urlContains("results"));

        List<WebElement> videos = wait.until(
                ExpectedConditions.numberOfElementsToBeMoreThan(
                        By.cssSelector("a#video-title[href*='/watch']"),
                        0
                )
        );

        WebElement firstVideo = videos.get(0);
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block: 'center'});",
                firstVideo
        );

        try {
            wait.until(ExpectedConditions.elementToBeClickable(firstVideo)).click();
        } catch (ElementClickInterceptedException e) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", firstVideo);
        }

        wait.until(ExpectedConditions.urlContains("watch"));
        waitForPageLoaded();
        assertSignedIn();

        WebElement likeButton = waitForLikeButton();
        boolean likedInitially = isLiked(likeButton);

        if (likedInitially) {
            clickLikeButton();
            wait.until(d -> !isLiked(waitForLikeButton()));

            driver.navigate().refresh();
            waitForPageLoaded();
            assertSignedIn();

            clickLikeButton();
            wait.until(d -> isLiked(waitForLikeButton()));
        } else {
            clickLikeButton();
            wait.until(d -> isLiked(waitForLikeButton()));
        }

        driver.navigate().refresh();
        waitForPageLoaded();
        assertSignedIn();

        Assertions.assertTrue(
                isLiked(waitForLikeButton()),
                "После обновления страницы лайк должен стоять"
        );
    }

    private void assertSignedIn() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(8)).until(
                    ExpectedConditions.presenceOfElementLocated(
                            By.cssSelector("button#avatar-btn")
                    )
            );
        } catch (TimeoutException e) {
            throw new AssertionError(
                    "В открытом Chrome нет авторизованной сессии YouTube. " +
                            "Открой Chrome вручную с нужным профилем и убедись, что видна аватарка."
            );
        }
    }

    private void acceptCookiesIfPresent() {
        List<By> cookieButtons = List.of(
                By.xpath("//button[.//span[normalize-space()='Accept all']]"),
                By.xpath("//button[.//span[normalize-space()='I agree']]"),
                By.xpath("//button[.//span[normalize-space()='Принять все']]"),
                By.xpath("//button[.//span[normalize-space()='Я принимаю']]"),
                By.xpath("//button[.//*[contains(text(),'Accept all')]]"),
                By.xpath("//button[.//*[contains(text(),'I agree')]]"),
                By.xpath("//button[.//*[contains(text(),'Принять все')]]"),
                By.xpath("//button[.//*[contains(text(),'Я принимаю')]]")
        );

        for (By locator : cookieButtons) {
            try {
                WebElement button = new WebDriverWait(driver, Duration.ofSeconds(5))
                        .until(ExpectedConditions.elementToBeClickable(locator));
                button.click();
                return;
            } catch (TimeoutException | ElementNotInteractableException ignored) {
            }
        }
    }

    private void searchForVideo(String text) {
        By searchInputLocator = By.name("search_query");

        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                WebElement searchInput = wait.until(
                        ExpectedConditions.refreshed(
                                ExpectedConditions.elementToBeClickable(searchInputLocator)
                        )
                );

                searchInput.click();
                searchInput.sendKeys(Keys.chord(Keys.CONTROL, "a"));
                searchInput.sendKeys(Keys.DELETE);
                searchInput.sendKeys(text);
                searchInput.sendKeys(Keys.ENTER);
                return;

            } catch (StaleElementReferenceException | ElementClickInterceptedException e) {
                if (attempt == 3) {
                    throw e;
                }
            }
        }
    }

    private WebElement waitForLikeButton() {
        List<By> locators = List.of(
                By.cssSelector("like-button-view-model button"),
                By.xpath("(//like-button-view-model//button)[1]"),
                By.xpath("(//segmented-like-dislike-button-view-model//button[@aria-pressed])[1]")
        );

        for (By locator : locators) {
            try {
                return new WebDriverWait(driver, Duration.ofSeconds(10))
                        .until(ExpectedConditions.elementToBeClickable(locator));
            } catch (TimeoutException ignored) {
            }
        }

        throw new NoSuchElementException("Не удалось найти кнопку Like");
    }

    private boolean isLiked(WebElement likeButton) {
        String ariaPressed = likeButton.getAttribute("aria-pressed");
        return "true".equalsIgnoreCase(ariaPressed);
    }

    private void clickLikeButton() {
        WebElement likeButton = waitForLikeButton();

        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block: 'center'});",
                likeButton
        );

        try {
            likeButton.click();
        } catch (ElementClickInterceptedException | StaleElementReferenceException e) {
            WebElement freshButton = waitForLikeButton();
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", freshButton);
        }
    }

    private void waitForPageLoaded() {
        new WebDriverWait(driver, Duration.ofSeconds(15)).until(d -> {
            Object state = ((JavascriptExecutor) d).executeScript("return document.readyState");
            return "complete".equals(state);
        });
    }
}