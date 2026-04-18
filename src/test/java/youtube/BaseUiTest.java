package youtube;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.nio.file.Paths;
import java.util.List;

public abstract class BaseUiTest {

    protected Playwright playwright;
    protected Browser browser;
    protected BrowserContext context;
    protected Page page;

    @BeforeEach
    void setUp() {
        playwright = Playwright.create();

        boolean headless = Boolean.parseBoolean(System.getProperty("headless", "false"));

        // Настройки запуска браузера
        BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                .setChannel("chrome")
                .setHeadless(headless)
                .setSlowMo(headless ? 0 : 80);

        // Дополнительные аргументы для headless режима (важно для CI)
        if (headless) {
            launchOptions.setArgs(List.of(
                    "--no-sandbox",
                    "--disable-dev-shm-usage",
                    "--disable-blink-features=AutomationControlled",
                    "--disable-gpu",
                    "--window-size=1920,1080"
            ));
        }

        browser = playwright.chromium().launch(launchOptions);

        // Настройки контекста браузера
        context = browser.newContext(new Browser.NewContextOptions()
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36")
                .setViewportSize(1920, 1080)
                .setLocale("ru-RU")
        );

        // Stealth-патчи против анти-бота YouTube
        context.addInitScript("""
            Object.defineProperty(navigator, 'webdriver', {get: () => undefined});
            Object.defineProperty(navigator, 'plugins', {get: () => [1, 2, 3, 4, 5]});
            Object.defineProperty(navigator, 'languages', {get: () => ['ru-RU', 'ru']});
            
            delete navigator.__proto__.webdriver;
            
            window.chrome = { 
                runtime: {}, 
                loadTimes: () => {}, 
                csi: () => {}, 
                app: {} 
            };
            
            Object.defineProperty(navigator, 'hardwareConcurrency', {get: () => 8});
            Object.defineProperty(navigator, 'deviceMemory', {get: () => 8});
            """);

        page = context.newPage();

        System.out.println("🚀 Браузер запущен (Playwright + Chrome, headless=" + headless + ")");
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