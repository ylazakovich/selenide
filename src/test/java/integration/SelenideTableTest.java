package integration;

import com.codeborne.selenide.SelenideTable;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.SelenideTable.of;

class SelenideTableTest extends ITest {
  private SelenideTable orders;

  @BeforeEach
  void openTable() {
    openFile("table_navigation.html");
    orders = of($("#orders"));
  }

  @Test
  void readsCellByZeroBasedBodyRowAndCellIndexes() {
    orders.cell(1, 1).shouldHave(exactText("Pending"));
  }

  @Test
  void waitsForDelayedRowsAndSurvivesTableRemount() {
    driver().executeJavaScript("window.delaySecondRow()");
    orders.cell(1, 1).shouldHave(exactText("Pending"), Duration.ofSeconds(2));

    driver().executeJavaScript("window.replaceOrders()");
    orders.cell(0, 0).shouldHave(exactText("1001"));
  }
}
