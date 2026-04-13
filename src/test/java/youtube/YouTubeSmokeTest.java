package youtube;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.*;
import org.openqa.selenium.io.FileHandler;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.StaleElementReferenceException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;


public class YouTubeSmokeTest extends BaseUiTest {

    private static final String SEARCH_TEXT = "Генрих 8";

    @Test
    void searchVideoAndOpenFirstResult() throws IOException {
        try {
            driver.get("https://www.youtube.com/?hl=ru");

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

            acceptCookiesIfPresent();
            waitForPageLoaded();

            searchForVideo(wait, SEARCH_TEXT);

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

            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("ytd-watch-flexy, #movie_player, video")
            ));

            String currentUrl = driver.getCurrentUrl();
            Assertions.assertTrue(
                    currentUrl.contains("watch"),
                    "Ожидали страницу видео, но получили: " + currentUrl
            );
        } finally {
            saveArtifacts();
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

    private void searchForVideo(WebDriverWait wait, String text) {
        By searchInputLocator = By.name("search_query");

        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                WebElement searchInput = wait.until(
                        ExpectedConditions.refreshed(
                                ExpectedConditions.elementToBeClickable(searchInputLocator)
                        )
                );

                ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].scrollIntoView({block: 'center'});",
                        searchInput
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

    private void waitForPageLoaded() {
        new WebDriverWait(driver, Duration.ofSeconds(15)).until(d -> {
            Object state = ((JavascriptExecutor) d).executeScript("return document.readyState");
            return "complete".equals(state);
        });
    }

    private void saveArtifacts() throws IOException {
        Path artifactsDir = Path.of("target", "artifacts");
        Files.createDirectories(artifactsDir);

        File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        FileHandler.copy(screenshot, artifactsDir.resolve("last-page.png").toFile());

        String pageSource = driver.getPageSource();
        Files.writeString(
                artifactsDir.resolve("last-page.html"),
                pageSource,
                StandardCharsets.UTF_8
        );
    }
}