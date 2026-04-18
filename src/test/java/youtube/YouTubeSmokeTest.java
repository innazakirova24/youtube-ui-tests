package youtube;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.Test;

public class YouTubeSmokeTest extends BaseUiTest {

    private static final String SEARCH_QUERY = "Генрих 8";

    @Test
    void searchVideoAndOpenFirstResult() {
        try {
            System.out.println("➡️ Открываем YouTube");
            page.navigate("https://www.youtube.com/?hl=ru");

            dismissConsentBump();

            // Важная пауза после закрытия consent-бампера
            page.waitForTimeout(3000);

            System.out.println("🔍 Ищем поле поиска...");
            Locator searchInput = page.locator("input#search, ytd-searchbox input, input[name='search_query']")
                    .first();

            searchInput.waitFor(new Locator.WaitForOptions().setTimeout(25000));

            page.evaluate("window.scrollTo(0, 0)");
            page.waitForTimeout(800);

            searchInput.scrollIntoViewIfNeeded();
            searchInput.click();
            searchInput.fill(SEARCH_QUERY);
            System.out.println("✅ Ввели запрос: " + SEARCH_QUERY);

            searchInput.press("Enter");
            System.out.println("✅ Нажали Enter");

            page.waitForSelector("ytd-video-renderer, ytd-search-video-renderer",
                    new Page.WaitForSelectorOptions().setTimeout(18000));

            Locator firstVideo = page.locator("ytd-video-renderer a#video-title, ytd-search-video-renderer a#video-title").first();
            firstVideo.scrollIntoViewIfNeeded();
            firstVideo.click();

            page.waitForTimeout(4000);

            saveScreenshot("youtube-smoke-success");
            System.out.println("✅ Тест успешно пройден в " + (Boolean.parseBoolean(System.getProperty("headless", "false")) ? "headless" : "headful") + " режиме!");

        } catch (Exception e) {
            saveScreenshot("youtube-smoke-FAILED");
            System.err.println("❌ Тест упал: " + e.getMessage());
            throw e;
        }
    }

    private void dismissConsentBump() {
        try {
            System.out.println("🍪 Закрываем consent bump...");

            String[] selectors = {
                    "button:has-text('Принять всё')",
                    "button:has-text('Accept all')",
                    "ytd-consent-bump-v2-lightbox button.yt-spec-button-shape-next--filled",
                    "ytd-button-renderer button",
                    "#ytd-consent-bump-v2-lightbox button"
            };

            for (String sel : selectors) {
                Locator btn = page.locator(sel).first();
                if (btn.count() > 0 && btn.isVisible()) {
                    btn.scrollIntoViewIfNeeded();
                    btn.click();
                    System.out.println("✅ Consent bump закрыт");
                    page.waitForTimeout(2500);
                    return;
                }
            }

            // Запасной вариант через JS
            page.evaluate("document.querySelector('ytd-consent-bump-v2-lightbox')?.remove();");
            System.out.println("✅ Consent bump скрыт через JS");

        } catch (Exception e) {
            System.out.println("⚠️ Не удалось закрыть consent bump: " + e.getMessage());
        }
    }
}