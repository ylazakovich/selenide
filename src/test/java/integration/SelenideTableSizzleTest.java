package integration;

import com.codeborne.selenide.SelenideConfig;
import com.codeborne.selenide.SelenideDriver;
import com.codeborne.selenide.SelenideTable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.SelectorMode.Sizzle;

class SelenideTableSizzleTest extends BaseIntegrationTest {
  private final SelenideDriver driver = new SelenideDriver(
    new SelenideConfig().baseUrl(getBaseUrl()).selectorMode(Sizzle));

  @AfterEach
  void closeDriver() {
    driver.close();
  }

  @Test
  void navigatesCellsWithSizzleSelectorMode() {
    driver.open("/table_navigation.html");

    SelenideTable.of(driver.$("#orders")).cell(0, 1).shouldHave(exactText("Ready"));
  }
}
