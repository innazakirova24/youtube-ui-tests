package youtube;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.*;
import org.openqa.selenium.io.FileHandler;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.io.File;

public class YouTubeSmokeTest extends BaseUiTest {

    private static final String SEARCH_TEXT = "Генрих 8";

    @Test
    void searchVideoAndOpenFirstResult() throws IOException {
        driver.get("https://www.youtube.com/?hl=ru");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        acceptCookiesIfPresent(wait);

        WebElement searchInput = wait.until(
                ExpectedConditions.elementToBeClickable(By.name("search_query"))
        );

        searchInput.click();
        searchInput.clear();
        searchInput.sendKeys(SEARCH_TEXT);
        searchInput.sendKeys(Keys.ENTER);

        wait.until(ExpectedConditions.urlContains("results"));

        List<WebElement> videos = wait.until(
                ExpectedConditions.numberOfElementsToBeMoreThan(
                        By.cssSelector("a#video-title"),
                        0
                )
        );

        WebElement firstVideo = videos.get(0);

        try {
            wait.until(ExpectedConditions.elementToBeClickable(firstVideo));
            firstVideo.click();
        } catch (ElementClickInterceptedException e) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", firstVideo);
        }

        wait.until(ExpectedConditions.urlContains("watch"));
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("ytd-watch-metadata h1, h1.ytd-watch-metadata"))
        );

        String currentUrl = driver.getCurrentUrl();
        Assertions.assertTrue(
                currentUrl.contains("watch"),
                "Ожидали страницу видео, но получили: " + currentUrl
        );

        saveArtifacts();
    }

    private void acceptCookiesIfPresent(WebDriverWait wait) {
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
                wait.until(ExpectedConditions.invisibilityOf(button));
                return;
            } catch (TimeoutException | ElementNotInteractableException ignored) {
            }
        }
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