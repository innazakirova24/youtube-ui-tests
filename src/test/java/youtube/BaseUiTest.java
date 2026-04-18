package youtube;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.nio.file.Paths;

public abstract class BaseUiTest {

    protected Playwright playwright;
    protected Browser browser;
    protected BrowserContext context;
    protected Page page;

    @BeforeEach
    void setUp() {
        playwright = Playwright.create();

        boolean headless = Boolean.parseBoolean(System.getProperty("headless", "false"));

        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setChannel("chrome")
                .setHeadless(headless)
                .setSlowMo(headless ? 0 : 80));

        context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(1920, 1080)
                .setLocale("ru-RU"));

        page = context.newPage();

        page.navigate("https://www.youtube.com");

        // Даём время на загрузку всех cookies
        page.waitForTimeout(6000);

        // Сохраняем состояние
        context.storageState(new BrowserContext.StorageStateOptions()
                .setPath(Paths.get("storage-state.json")));

        System.out.println("✅ storage-state.json создан/обновлён");

        // Теперь используем это состояние для теста
        context = browser.newContext(new Browser.NewContextOptions()
                .setStorageStatePath(Paths.get("storage-state.json"))
                .setViewportSize(1920, 1080)
                .setLocale("ru-RU"));

        page = context.newPage();

        System.out.println("Тест запущен с загруженным storage-state.json (headless = " + headless + ")");
    }

    @AfterEach
    void tearDown() {
        if (page != null) page.close();
        if (context != null) context.close();
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }

    protected void saveScreenshot(String name) {
        try {
            java.nio.file.Files.createDirectories(Paths.get("target/artifacts"));
            page.screenshot(new Page.ScreenshotOptions()
                    .setPath(Paths.get("target/artifacts/" + name + ".png")));
            System.out.println("📸 Скриншот сохранён: " + name + ".png");
        } catch (Exception e) {
            System.out.println("Не удалось сохранить скриншот: " + e.getMessage());
        }
    }
}